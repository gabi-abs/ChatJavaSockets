package chat.servidor;

import chat.objeto.Mensagem;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ThreadServidor extends Thread {

    private static List<ThreadServidor> clientes = new CopyOnWriteArrayList<>();
    private static ConcurrentHashMap<String, ThreadServidor> mapaClientes = new ConcurrentHashMap<>();

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private String nomeUsuario;

    public ThreadServidor(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            clientes.add(this);

            while (true) {
                Object obj = inputStream.readObject();
                if (obj instanceof Mensagem) {
                    Mensagem mensagem = (Mensagem) obj;

                    switch (mensagem.getAction()) {
                        case CONNECT:
                            handleConnect(mensagem);
                            break;
                        case DISCONNECT:
                            handleDisconnect();
                            return; // encerra thread após disconnect
                        case SEND:
                            handleSend(mensagem);
                            break;
                        case SEND_ONE:
                            handleSendOne(mensagem);
                            break;
                        default:
                            break;
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erro com o cliente " + nomeUsuario + ": " + e.getMessage());
            handleDisconnect(); // em caso de erro, remove cliente
        } finally {
            fecharConexao();
        }
    }

    private void handleConnect(Mensagem mensagem) {
        this.nomeUsuario = mensagem.getRemetente();
        mapaClientes.put(nomeUsuario, this);

        System.out.println(nomeUsuario + " conectou.");

        // Notifica todos que o usuário entrou
        Mensagem msg = new Mensagem();
        msg.setRemetente("Servidor");
        msg.setTexto(nomeUsuario + " entrou no chat.");
        msg.setAction(Mensagem.Action.SEND);

        broadcast(msg);

        // Atualiza lista de usuários online
        enviarUsuariosOnlineParaTodos();
    }

    private void handleDisconnect() {
        if (nomeUsuario != null) {
            System.out.println(nomeUsuario + " desconectou.");
            mapaClientes.remove(nomeUsuario);
            clientes.remove(this);

            // Notifica todos que o usuário saiu
            Mensagem msg = new Mensagem();
            msg.setRemetente("Servidor");
            msg.setTexto(nomeUsuario + " saiu do chat.");
            msg.setAction(Mensagem.Action.SEND);

            broadcast(msg);

            // Atualiza lista de usuários online
            enviarUsuariosOnlineParaTodos();
        }
        fecharConexao();
    }

    private void handleSend(Mensagem mensagem) {
        broadcast(mensagem);
    }

    private void handleSendOne(Mensagem mensagem) {
        String destinatario = mensagem.getDestinatario();
        ThreadServidor threadDestino = mapaClientes.get(destinatario);

        if (threadDestino != null) {
            try {
                threadDestino.outputStream.writeObject(mensagem);
                threadDestino.outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(Mensagem mensagem) {
        for (ThreadServidor cliente : clientes) {
            try {
                cliente.outputStream.writeObject(mensagem);
                cliente.outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enviarUsuariosOnlineParaTodos() {
        Mensagem msg = new Mensagem();
        msg.setAction(Mensagem.Action.USERS_ONLINE);
        msg.setUsuariosOnline(new ArrayList<>(mapaClientes.keySet()));

        for (ThreadServidor cliente : clientes) {
            try {
                cliente.outputStream.writeObject(msg);
                cliente.outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void fecharConexao() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
