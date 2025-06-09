package chat.servidor;

import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

    public static void main(String[] args) {
        int porta = 54321;
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("Servidor iniciado na porta " + porta);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + socket.getInetAddress());
                ThreadServidor threadServidor = new ThreadServidor(socket);
                threadServidor.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
