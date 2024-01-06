import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MultiThreadServerGUI extends JFrame {
    private static final int PORT = 12345;
    private static List<Socket> clientSockets = new ArrayList<>();
    private static JTextArea consoleArea;
    private static JTextField messageField;
    private static JButton sendButton;
    private static ExecutorService executorService;
    private static ServerThread serverThread;

    /**
     * Konstruktor untuk membuat objek MultiThreadServerGUI.
     */
    public MultiThreadServerGUI() {
        setTitle("MultiThread Server");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel titleLabel = new JLabel("MultiThread Server", SwingConstants.CENTER);
        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        messageField = new JTextField();
        sendButton = new JButton("Send");

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().add(titleLabel, BorderLayout.NORTH);
        getContentPane().add(consoleScrollPane, BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        // ActionListener untuk tombol "Send"
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // WindowListener untuk memulai dan menghentikan server saat jendela dibuka atau ditutup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                startServer();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });
    }

    /**
     * Metode untuk memulai server dan thread server.
     */
    private static void startServer() {
        serverThread = new ServerThread();
        serverThread.start();
    }

    /**
     * Metode untuk menghentikan server dan menutup executorService dan thread server.
     */
    private static void stopServer() {
        showMessage("Stopping the server...\n");
        if (executorService != null) {
            executorService.shutdown();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    /**
     * Metode untuk menampilkan pesan pada area konsol server.
     * @param message Pesan yang akan ditampilkan.
     */
    private static void showMessage(String message) {
        SwingUtilities.invokeLater(() -> consoleArea.append(message));
    }

    /**
     * Metode untuk mengirim pesan dari server ke semua klien yang terhubung.
     */
    private static void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            broadcastMessage("Server: " + message + "\n");
            messageField.setText("");
        }
    }

    /**
     * Metode untuk mengirim pesan ke semua klien yang terhubung.
     */
    private static void broadcastMessage(String message) {
        showMessage("Broadcasting message: " + message);
        List<Socket> disconnectedClients = new ArrayList<>();

        for (Socket socket : clientSockets) {
            try {
                if (socket.isConnected() && !socket.isClosed()) {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write((message).getBytes());
                    outputStream.flush();
                } else {
                    disconnectedClients.add(socket);
                }
            } catch (IOException e) {
                showMessage("Error broadcasting message to a client.\n");
                disconnectedClients.add(socket);
            }
        }

        clientSockets.removeAll(disconnectedClients);
    }

    /**
     * Kelas inner ClientHandler, mengelola thread untuk setiap klien yang terhubung.
     */
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        /**
         * Konstruktor untuk membuat objek ClientHandler.
         */
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (InputStream inputStream = clientSocket.getInputStream();
                 Scanner scanner = new Scanner(inputStream)) {

                while (!Thread.interrupted()) {
                    if (scanner.hasNextLine()) {
                        String message = scanner.nextLine();
                        showMessage("Client: " + message + "\n");
                    }
                }
            } catch (IOException e) {
                showMessage("Client disconnected: " + clientSocket.getInetAddress().getHostAddress() + "\n");
                clientSockets.remove(clientSocket);
            }
        }
    }

    /**
     * Kelas inner ServerThread, mengelola thread utama untuk server.
     */
    static class ServerThread extends Thread {
        @Override
        public void run() {
            executorService = Executors.newFixedThreadPool(10);

            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                showMessage("Server is listening on port " + PORT + "\n");

                while (!Thread.interrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    clientSockets.add(clientSocket);
                    showMessage("Client connected: " + clientSocket.getInetAddress().getHostAddress() + "\n");

                    executorService.execute(new ClientHandler(clientSocket));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Metode utama untuk menjalankan aplikasi server GUI.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MultiThreadServerGUI server = new MultiThreadServerGUI();
            server.setVisible(true);
        });
    }
}
