import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class MultiThreadClientGUI extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private JTextArea chatArea;
    private JTextField messageField;

    public MultiThreadClientGUI() {
        setTitle("MultiThread Client");  // Setel judul JFrame
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Inisialisasi komponen GUI
        JLabel titleLabel = new JLabel("MultiThread Client", SwingConstants.CENTER);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        messageField = new JTextField();
        JButton sendButton = new JButton("Send");

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().add(titleLabel, BorderLayout.NORTH);
        getContentPane().add(chatScrollPane, BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        // Menambahkan action listener untuk tombol "Send"
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Menambahkan window listener untuk menangani peristiwa penutupan jendela
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        connectToServer();  // Menghubungkan ke server
        startThreads();  // Memulai thread-thread
    }

    // Metode untuk menghubungkan ke server
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            showMessage("Terhubung ke server di " + SERVER_ADDRESS + ":" + SERVER_PORT + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Gagal terhubung ke server.\n");
        }
    }

    // Metode untuk memulai thread-thread
    private void startThreads() {
        Thread readThread = new Thread(new ReadThread());
        readThread.start();

        // Write thread akan dimulai saat pengguna mengirim pesan pertama.
    }

    // Metode untuk mengirim pesan ke server
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write((message + "\n").getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error mengirim pesan ke server.\n");
            }
            messageField.setText("");
        }
    }

    // Metode untuk menampilkan pesan di area chat
    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message));
    }

    // Kelas ReadThread untuk membaca pesan dari server
    private class ReadThread implements Runnable {
        @Override
        public void run() {
            try (InputStream inputStream = socket.getInputStream();
                 Scanner scanner = new Scanner(inputStream)) {

                while (!socket.isClosed()) {
                    if (scanner.hasNextLine()) {
                        String message = scanner.nextLine();
                        showMessage("Server: " + message + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MultiThreadClientGUI clientGUI = new MultiThreadClientGUI();
            clientGUI.setVisible(true);
        });
    }
}

