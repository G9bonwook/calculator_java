import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(20);
    JTextArea messageArea = new JTextArea(8, 40);
    JButton whisperButton = new JButton("Whisper");
    JComboBox<String> clientListComboBox = new JComboBox<>();

    public ChatClient() {
        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);

        // Use FlowLayout for the content pane
        frame.setLayout(new FlowLayout());

        frame.getContentPane().add(textField);
        frame.getContentPane().add(new JScrollPane(messageArea));
        frame.getContentPane().add(clientListComboBox);
        frame.getContentPane().add(whisperButton);

        // Add Listeners
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });

        // When the whisper button is pressed, Create WhisperWorker class and execute "execute" method.
        whisperButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Whisper button clicked");
                new WhisperWorker().execute();
            }
        });

        frame.setPreferredSize(new Dimension(500, 250));

        frame.pack();
    }

    // Gui to float when you enter a name
    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    // Delete existing content and add it to the clientListComboBox based on the content of the list taken by the argument.
    private void updateClientList(String[] clients) {
        clientListComboBox.removeAllItems();
        for (String client : clients) {
            clientListComboBox.addItem(client);
        }
    }

    private class WhisperWorker extends SwingWorker<Void, Void> {

        private String selectedClient;

        @Override
        protected Void doInBackground() throws Exception {
            // Floats a list of clients and selects a client to send a Whisper message
            selectedClient = (String) clientListComboBox.getSelectedItem(); //
            if (selectedClient != null) {
                // Receive the message content to be sent through gui and send it to the client through "out".
                String message = JOptionPane.showInputDialog(frame, "Enter Whisper message:");
                if (message != null && !message.isEmpty()) {
                    out.println("<" + selectedClient + "/>" + message);
                }
            }
            return null;
        }
    }

    private void run() throws IOException {
        String fileName = "ServerInfo.dat";
        String host = "";
        int port = 0;

        // Read the contents of the server in the file and connect to it
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(fileName));
            host = inputStream.readUTF();
            port = inputStream.readInt();
        } catch (IOException e) {
            host = "localhost";
            port = 1234;
        }

        Socket socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            } else if (line.startsWith("SUBMITNAME")) {
                // When you receive a message from the server to submit a name, you call the getName function to receive the name and send it to the server.
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                // Separates message received from the server, and display on messageArea.
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("CLIENTLIST")) {
                // Separates message received from the server by "," list them, and forward them to the updateClientist function
                String[] clients = line.substring(11).split(",");
                updateClientList(clients);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}