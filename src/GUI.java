import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class GUI extends JFrame {

    private JTextField address;
    private JTextField fichierLocal;
    private JTextField fichierDistant;

    public GUI() {
        super("Client");
        JPanel root = new JPanel(new BorderLayout());

        // Création du champ : Entrer une adresse
        JPanel north = new JPanel(new BorderLayout());
        JLabel label = new JLabel("IP Pumpkin : ");
        address = new JTextField();
        north.add(label, BorderLayout.WEST);
        north.add(address, BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);

        // Champ des fichiers
        JPanel files = new JPanel(new BorderLayout());
        fichierLocal = new JTextField();
        fichierDistant = new JTextField();
        files.add(fichierLocal, BorderLayout.NORTH);
        files.add(fichierDistant, BorderLayout.SOUTH);
        root.add(files, BorderLayout.CENTER);

        // Champ des boutons
        JPanel buttons = new JPanel(new BorderLayout());

        // Création du bouton : recevoir un fichier
        JButton receive = new JButton("Recevoir");
        receive.addActionListener(e -> {
            try {
                new Client().receiveFile(InetAddress.getByName(address.getText()), 69, fichierDistant.getText(), fichierLocal.getText());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        buttons.add(receive, BorderLayout.EAST);

        // Création du champ : envoyer un fichier
        JButton send = new JButton("Envoyer");
        send.addActionListener(e -> {
            try {
                new Client().sendFile(InetAddress.getByName(address.getText()), 69, fichierLocal.getText());
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }
        });
        buttons.add(send, BorderLayout.WEST);

        root.add(buttons, BorderLayout.SOUTH);

        this.getContentPane().add(root, BorderLayout.CENTER);
        this.setSize(200, 200);
        this.setLocationByPlatform(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    public static void main(String[] args) {
        new GUI();
    }
}
