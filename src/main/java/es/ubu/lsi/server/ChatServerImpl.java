package es.ubu.lsi.server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Chat server. Based on code available at: 
 * http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/
 * 
 * Modified by Raúl Marticorena y Joaquín P- Seco
 * 
 * @author http://www.dreamincode.net
 * @author Raúl Marticorena
 * @author Joaquin P. Seco
 * @author Josué Granados
 *
 */
public class ChatServerImpl implements ChatServer {

	private static final int DEFAULT_PORT = 1500;
    private static int clientId = 0;
    private List<ServerThreadForClient> clients = new ArrayList<>();
    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private int port;
    private boolean alive;
    private ServerSocket serverSocket;
    private int[] blacklist = {5001, 5002, 9999, 56046}; // Puertos bloqueados

    public ChatServerImpl(int port) {
        this.port = port;
    }

    @Override
    public void startup() {
        alive = true;
        try {
            serverSocket = new ServerSocket(port);
            while (alive) {
                show("Servidor esperando en puerto " + port);
                Socket socket = serverSocket.accept();
                if (!alive) break;
                // --- LÓGICA DE BLACKLIST ---
            int clientPort = socket.getPort(); // Obtenemos el puerto de origen
            boolean blocked = false;
            for (int bPort : blacklist) {
                if (bPort == clientPort) {
                    blocked = true;
                    break;
                }
            }

            if (blocked) {
                show("Conexión bloqueada desde el puerto origen: " + clientPort);
                socket.close(); // Cerramos sin crear el hilo
                continue; // Saltamos al siguiente cliente
            }
            // ---------------------------
                ServerThreadForClient t = new ServerThreadForClient(socket);
                clients.add(t);
                t.start();
            }
        } catch (IOException e) {
            show("Error en ServerSocket: " + e);
        }
    }

    @Override
    public synchronized void broadcast(ChatMessage message) {
        String time = sdf.format(new Date());
        // REQUISITO OBLIGATORIO DE AUTORIA 
        String prefix = "Josué Granados patrocina el mensaje: ";
        String formatted = time + " " + prefix + message.getMessage();
        
        System.out.println(formatted);
        message.setMessage(formatted);

        for (int i = clients.size() - 1; i >= 0; i--) {
            if (!clients.get(i).sendMessage(message)) {
                clients.remove(i);
            }
        }
    }

    @Override
    public synchronized void remove(int id) {
        clients.removeIf(it -> it.id == id);
    }

    @Override
    public void shutdown() {
        alive = false;
        try {
            serverSocket.close();
            for (ServerThreadForClient ct : clients) ct.close();
        } catch (IOException e) {
            show("Error al cerrar: " + e);
        }
    }

    private void show(String event) {
        System.out.println(sdf.format(new Date()) + " " + event);
    }

    public static void main(String[] args) {
        new ChatServerImpl(DEFAULT_PORT).startup();
    }

    /** Hilo interno para cada cliente [cite: 85, 121] */
    private class ServerThreadForClient extends Thread {
        private Socket socket;
        private ObjectInputStream sInput;
        private ObjectOutputStream sOutput;
        private int id;
        private String username;

        private ServerThreadForClient(Socket socket) {
            this.id = ++clientId;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                // Cambia la línea de 'show' por esta:
show(username + " conectado con ID " + id + " desde el puerto " + socket.getPort());
            } catch (Exception e) {
                show("Error en streams: " + e);
            }
        }

        public void run() {
            boolean running = true;
            while (running && alive) {
                try {
                    ChatMessage msg = (ChatMessage) sInput.readObject();
                    running = processMessage(msg);
                } catch (Exception e) {
                    break;
                }
            }
            remove(id);
            close();
        }

        private boolean processMessage(ChatMessage msg) {
            if (msg.getType() == MessageType.LOGOUT) {
                broadcast(new ChatMessage(id, MessageType.MESSAGE, username + " sale del chat."));
                return false;
            }
            broadcast(new ChatMessage(id, MessageType.MESSAGE, username + ": " + msg.getMessage()));
            return true;
        }

        private boolean sendMessage(ChatMessage msg) {
            try {
                sOutput.writeObject(msg);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        private void close() {
            try {
                if (sOutput != null) sOutput.close();
                if (sInput != null) sInput.close();
                if (socket != null) socket.close();
            } catch (Exception e) {}
        }
    }
}