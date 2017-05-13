import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Client {

    private static final byte[] RRQ = ByteBuffer.allocate(2).put((byte) 0).put((byte) 1).slice().array();
    private static final byte[] WRQ = ByteBuffer.allocate(2).put((byte) 0).put((byte) 2).slice().array();
    private static final byte[] DATA = ByteBuffer.allocate(2).put((byte) 0).put((byte) 3).slice().array();
    private static final byte[] ACK = ByteBuffer.allocate(2).put((byte) 0).put((byte) 4).slice().array();
    //private static final byte[] ERROR = ByteBuffer.allocate(2).put((byte) 0).put((byte) 5).slice().array();
    private static final int taille = 516;

    public static void main(String[] args) throws IOException {
        // receiveFile
        new Client().receiveFile(InetAddress.getByName("127.0.0.1"), 69, "fichier.txt", "fichier.txt");

        // sendFile
        //new Client().sendFile(InetAddress.getByName("127.0.0.1"), 69, "fichier.txt");
    }

    private int receiveFile(InetAddress adresse, int port, String fichierDistant, String fichierLocal) throws IOException {
        // Ouverture du port
        DatagramSocket soc = new DatagramSocket();
        soc.setSoTimeout(3000);

        // Ouverture du fichierLocal
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("data/"+fichierLocal);
        } catch (IOException e) {
            return -1; // CrRv < 0, erreur locale (impossible d'accéder ou répertoire pour créer un fichier, le flchîer existe déjà..,).
        }
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        boolean fileOpen = true;

        // Envoi de RRQ fichierDistant
        ByteBuffer bufSend = ByteBuffer.allocate(taille).put(RRQ).put(fichierDistant.getBytes()).put((byte) 0).put("octet".getBytes()).put((byte) 0).slice();
        DatagramPacket dpSend = new DatagramPacket(bufSend.array(), bufSend.capacity(), adresse, port);
        soc.send(dpSend);

        while (true) {
            // Attente réponse
            ByteBuffer bufReceive = ByteBuffer.allocate(taille);
            DatagramPacket dpReceive = new DatagramPacket(bufReceive.array(), bufReceive.capacity());

            try {
                soc.receive(dpReceive);
            } catch (SocketTimeoutException e) {
                if (fileOpen) {
                    return 1; // CrRv > 0 erreur de tronsfert intervenue sur le serveur
                }
                break;
            }

            //boolean erreurFaite = false;

            // On récupère l'opcode
            byte[] opcode = Arrays.copyOfRange(bufReceive.array(), 0, 2);

            if (opcode[1] == DATA[1]) {
                // On récupère le numéro de bloc
                byte[] num = Arrays.copyOfRange(bufReceive.array(), 2, 4);

                if (fileOpen) {

                    int i = bufReceive.capacity() - 1;
                    while (i >= 0 && bufReceive.get(i) == 0) --i;
                    byte[] data = Arrays.copyOfRange(bufReceive.array(), 4, i + 1);

                    // On écrit les données dans le fichier local
                    bufferedWriter.write(new String(data, StandardCharsets.UTF_8).trim());
                    bufferedWriter.flush();

                    if (data.length < 512) {
                        // Fermeture du fichier
                        bufferedWriter.close();
                        fileWriter.close();
                        fileOpen = false;
                        System.out.println("Le fichier '" + fichierDistant + "' a bien été reçu");
                    }
                }

                // On envoi un ACK avec le bon numéro de bloc
                bufSend = ByteBuffer.allocate(taille).put(ACK).put(num).slice();
                dpSend = new DatagramPacket(bufSend.array(), bufSend.capacity(), dpReceive.getAddress(), dpReceive.getPort());

                /*if (num[1] == (byte) 2 && !erreurFaite) { // ACK(2) perdu
                    dpSend.setPort(2000); // mauvais port
                    erreurFaite = true;
                }*/

                soc.send(dpSend);
            }
        }

        // Fermeture du socket
        soc.close();

        return 0; // CrRv = 0, le transfert s'est bien déroulé
    }

    public void sendFile(InetAddress adresse, int port, String fichierLocal) {
        FileInputStream file = null;
        DatagramSocket socket;
        //Buffer servant à la lecture
        byte[] buffer = new byte[512];
        //Compteur du nombre d'octets lus
        int compteur = 0;
        //Numéro du DTG
        int noDTG = 0;
        try {
            //Creation du DatagramSocket
            socket = new DatagramSocket();
            //On fixe la durée du TimeOut à 3 secondes
            socket.setSoTimeout(3000);

            //Emission du WRQ fichier local
            ByteBuffer bBuffer = ByteBuffer.allocate(taille).put(WRQ).put(fichierLocal.getBytes()).put((byte) 0).put("octet".getBytes()).put((byte) 0).slice();
            sendBytes(socket, bBuffer.array(), adresse, port, 0);

            //Ouverture du fichier local
            file = new FileInputStream(new File("data/"+fichierLocal));
            //Lecture du fichier
            while ((compteur = file.read(buffer)) >= 0) {
                //Lecture du fichier
                if (compteur != buffer.length) {
                    byte[] buf = new byte[compteur];
                    for (int i = 0; i < compteur; i++)
                        buf[i] = buffer[i];
                    buffer = buf;
                }
                //Création du DTG
                byte[] DTG = new byte[compteur + 4];
                DTG[0] = 0;
                DTG[1] = 3;
                DTG[2] = (byte) (noDTG / 256);
                DTG[3] = (byte) (noDTG % 256);
                for (int i = 0; i < compteur; i++)
                    DTG[4 + i] = buffer[i];
                //Envoi du fichier
                sendBytes(socket, buffer, adresse, port, noDTG);
                noDTG++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Envoi d'un DTG jusqu'à 3 fois, test de réception du ACK
    public static void sendBytes(DatagramSocket ds, byte[] tab, InetAddress ia, int port, int numDTG) {
        DatagramPacket dp = new DatagramPacket(tab, tab.length, ia, port);
        try {
            int i;
            //On essaye d'envoyer le DTG jusqu'à 3 fois si on ne reçoit aucun ACK
            for (i = 0; i < 3; i = ACK(ds, numDTG) ? 5 : (i + 1))
                ds.send(dp);
            if (i == 3)
                System.out.println("Le DTG n°" + numDTG + " n'a pas pu être envoyé.");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    //Retourne "true" à la la réception du ACK correspondant au numéro du DTG,
    //		   "false" si on passe par un Timeout
    public static boolean ACK(DatagramSocket socket, int numBloc) {
        DatagramPacket packet;
        try {
            //On reçoit jusqu'à recevoir un ACK pour le DTG voulu ou avoir un Timeout
            while (true) {
                packet = new DatagramPacket(new byte[512], 512);
                socket.receive(packet);
                //On regarde si on a reçu un ACK
                byte[] buffer = packet.getData();
                int opCode = 0;
                int no = 0;
                for (int i = 0; i < 4; i++) {
                    opCode *= 2;
                    no *= 2;
                    opCode += buffer[i];
                    no += buffer[4 + i];
                }
                //Si on a un ACK pour le bon DTG, on retourne "true"
                if ((opCode == 4) && (no == numBloc))
                    return true;
            }
        } catch (IOException e) {
            //Timeout : on retourne "false"
            return false;
        }
    }
}
