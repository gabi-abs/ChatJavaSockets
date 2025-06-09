package chat.cliente;

import chat.objeto.Mensagem;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ThreadCliente extends Thread {

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Consumer<Mensagem> onMessageReceived;
    private String nomeUsuario;
    private String ip;
    private int porta;

    private volatile boolean running = true;

    public ThreadCliente(Consumer<Mensagem> onMessageReceived, String nomeUsuario, String ip, int porta) {
        this.onMessageReceived = onMessageReceived;
        this.nomeUsuario = nomeUsuario;
        this.ip = ip;
        this.porta = porta;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(ip, porta);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            // Envia mensagem CONNECT
            Mensagem mensagemConectar = new Mensagem();
            mensagemConectar.setRemetente(nomeUsuario);
            mensagemConectar.setAction(Mensagem.Action.CONNECT);

            outputStream.writeObject(mensagemConectar);
            outputStream.flush();

            // Loop de recebimento
            while (running) {
                Object obj = inputStream.readObject();
                if (obj instanceof Mensagem) {
                    Mensagem mensagem = (Mensagem) obj;
                    onMessageReceived.accept(mensagem);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Mensagem erro = new Mensagem();
            erro.setRemetente("Sistema");
            erro.setTexto("Erro de conexão: " + e.getMessage());
            erro.setAction(Mensagem.Action.SEND);
            onMessageReceived.accept(erro);
        } finally {
            fecharConexao();
        }
    }

    public void enviarMensagem(String texto, String destinatario, Mensagem.Action action) {
        try {
            Mensagem mensagem = new Mensagem();
            mensagem.setRemetente(nomeUsuario);
            mensagem.setTexto(texto);
            mensagem.setAction(action);
            mensagem.setDestinatario(destinatario);

            outputStream.writeObject(mensagem);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Mensagem erro = new Mensagem();
            erro.setRemetente("Sistema");
            erro.setTexto("Erro ao enviar mensagem: " + e.getMessage());
            erro.setAction(Mensagem.Action.SEND);
            onMessageReceived.accept(erro);
        }
    }

    public void desconectar() {
        try {
            running = false;
            Mensagem mensagem = new Mensagem();
            mensagem.setRemetente(nomeUsuario);
            mensagem.setAction(Mensagem.Action.DISCONNECT);

            outputStream.writeObject(mensagem);
            outputStream.flush();

            fecharConexao();
        } catch (IOException e) {
            e.printStackTrace();
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
