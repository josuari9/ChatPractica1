package es.ubu.lsi.client;

import java.net.*;
import java.io.*;
import java.util.*;

import es.ubu.lsi.client.ChatClientImpl.ChatClientListener;
import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Client.
 * 
 * @author http://www.dreamincode.net
 * @author Raúl Marticorena
 * @author Joaquin P. Seco
 * @author Josué Granados
 *
 */
public class ChatClientImpl implements ChatClient {
	
	private String server;
    private String username;
    private int port = 1500;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private Set<String> bannedUsers = new HashSet<>();

    public ChatClientImpl(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    @Override
    public boolean start() {
        try {
            socket = new Socket(server, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(username); // Enviar nick al servidor [cite: 10]
            new Thread(new ChatClientListener()).start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void sendMessage(ChatMessage msg) {
        try {
            out.writeObject(msg);
        } catch (IOException e) {}
    }

    @Override
    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        String nick = (args.length > 1) ? args[1] : "Anonimo";
        
        ChatClientImpl client = new ChatClientImpl(host, 1500, nick);
        if (!client.start()) return;

        Scanner sc = new Scanner(System.in);
        while (true) {
            String text = sc.nextLine();
            if (text.equalsIgnoreCase("logout")) {
                client.sendMessage(new ChatMessage(0, MessageType.LOGOUT, ""));
                break;
            } else if (text.startsWith("ban ")) {
                handleBan(client, text, nick);
            } else if (text.startsWith("unban ")) {
                client.bannedUsers.remove(text.substring(6));
            } else {
                client.sendMessage(new ChatMessage(0, MessageType.MESSAGE, text));
            }
        }
        client.disconnect();
    }

    private static void handleBan(ChatClientImpl client, String text, String nick) {
        String target = text.substring(4);
        client.bannedUsers.add(target);
        // Notificar al servidor del baneo [cite: 17]
        client.sendMessage(new ChatMessage(0, MessageType.MESSAGE, nick + " ha baneado a " + target));
    }

    /** Hilo para recibir mensajes [cite: 45, 65] */
    private class ChatClientListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    ChatMessage msg = (ChatMessage) in.readObject();
                    // Lógica de baneo local [cite: 15]
                    boolean isBanned = bannedUsers.stream().anyMatch(u -> msg.getMessage().contains(u + ":"));
                    if (!isBanned) {
                        System.out.println(msg.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Conexión finalizada.");
            }
        }
    }
}