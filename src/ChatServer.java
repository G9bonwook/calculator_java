import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ChatServer {
    private static final int PORT = 1234;
    private static HashSet<String> names = new HashSet<>();
    private static HashSet<PrintWriter> writers = new HashSet<>();
    private static Map<String, PrintWriter> userMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) { // Support for multiple threads by using while statements.
                new Handler(listener.accept()).start(); // The thread starts when the client connects the socket.
            }
        } finally {
            listener.close(); // It ends by closing the socket.
        }
    }

    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        // When you create and start a thread, the run function is executed.
        // The run function consists of logic about client-server communication.
        public void run() {
            try {
                /*
                The contents to be received from the client to the server are entered through "in".
                The content to be sent from the server to the client is done through "out".
                */
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    /*
                    After requesting "SUBMITNAME" to the clint,
                    the name value entered from the client is read and stored in the NAME variable
                    */
                    out.println("SUBMITNAME");
                    name = in.readLine();

                    if (name == null) {
                        return;
                    }

                    // Using synchronized doese not create problems related to shared resuource such as race conditions.
                    synchronized (names) {
                        if (!names.contains(name)) { // Save the name in names and userMap HashSet.
                            names.add(name);
                            userMap.put(name, out);
                            break;
                        }
                    }
                }
                // Send "NAMEACCEPTED" to the client and store the "out" variable of the client in "writers" HashSet.
                out.println("NAMEACCEPTED");
                writers.add(out);

                // A message is sent to all clients through the broadcast function to inform them of the new client's entrance.
                broadcastMessage("MESSAGE " + name + " has entered the chatroom.");

                // Send update lists to all existing clients when new clients come in.
                sendClientList();

                while (true) {
                    // Save message sent by clients to input
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }

                    // Sent senderName to "name", default to full destination message, and targetUser to null.
                    String senderName = name;
                    String targetUser = null;

                    // If the Whisper message is input, extract the targetUser and content from the input and sent the message only to the senderUser and targetUser.
                    if (input.startsWith("<") && input.contains("/>")) {
                        String[] part = input.split("/", 2);
                        targetUser = part[0].replace("<", "").replace(">", "").trim();
                        String whisperMessage = part[1].replace(">", "").trim();

                        // Find the PrintWriter for each client through userMap under the client's name.
                        PrintWriter targetWriter = userMap.get(targetUser);
                        PrintWriter senderWriter = userMap.get(senderName);

                        if (targetWriter != null && senderWriter != null) {
                            targetWriter.println("MESSAGE " + "<Whisper> " + senderName + ": " + whisperMessage);
                            senderWriter.println("MESSAGE " + "<Whisper> " + senderName + " to " + targetUser + ": " + whisperMessage);
                        }
                    } else {
                        // If it is not a Whisper message, input is sent to all clients.
                        broadcastMessage("MESSAGE " + senderName + ": " + input);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally { // When the client completes the connection, it deletes the client's information and sents the client's exit message to all clients.
                if (name != null) {
                    names.remove(name);
                    userMap.remove(name);
                    broadcastMessage("MESSAGE " + name + " has left the chatroom.");
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close(); // Close the socket.
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Send a message to all clients.
        private void broadcastMessage(String message) {
            for (PrintWriter writer : writers) {
                writer.println(message);
            }
        }

        /*
        * All clients sotred in names are appended after "CLIENTLIST" and sent to all clients.
        * EX) CLIENTLIST,BONWOOK,JEONGHOON,JUNGHUN
        */
        private void sendClientList() {
            StringBuilder clientList = new StringBuilder("CLIENTLIST");
            for (String client : names) {
                clientList.append(",").append(client);
            }
            broadcastMessage(clientList.toString());
        }
    }
}
