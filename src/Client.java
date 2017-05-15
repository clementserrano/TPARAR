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
    private static final int taille = 516;

    private static InetAddress m_add;
    private static int m_port;

    public int receiveFile(InetAddress adresse, int port, String fichierDistant, String fichierLocal) throws IOException {
        // Ouverture du port
        DatagramSocket soc = new DatagramSocket();
        soc.setSoTimeout(3000);

        // Ouverture du fichierLocal
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("data/" + fichierLocal);
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
                    return 1; // CrRv > 0 erreur de transfert intervenue sur le serveur
                }
                break;
            }

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
                    }
                }

                // On envoi un ACK avec le bon numéro de bloc
                bufSend = ByteBuffer.allocate(taille).put(ACK).put(num).slice();
                dpSend = new DatagramPacket(bufSend.array(), bufSend.capacity(), dpReceive.getAddress(), dpReceive.getPort());

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
        int noDTG = 1;
        try {
            //Creation du DatagramSocket
            socket = new DatagramSocket();
            //On fixe la durée du TimeOut à 3 secondes
            socket.setSoTimeout(3000);

            //Emission du WRQ fichier local
            ByteBuffer bBuffer = ByteBuffer.allocate(taille).put(WRQ).put(fichierLocal.getBytes()).put((byte) 0).put("octet".getBytes()).put((byte) 0).slice();
            sendBytes(socket, bBuffer.array(), adresse, port, 0);

            //Ouverture du fichier local
            file = new FileInputStream(new File("data/" + fichierLocal));
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
                ByteBuffer DTG = ByteBuffer.allocate(compteur + 4);
                DTG.put(DATA).put((byte) 0).put((byte) noDTG).put(buffer).slice();
                //Envoi du fichier
                sendBytes(socket, DTG.array(), m_add, m_port, noDTG);
                noDTG++;
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Fichier envoy�");
    }

    //Envoi d'un DTG jusqu'à 3 fois, test de réception du ACK
    public static void sendBytes(DatagramSocket ds, byte[] tab, InetAddress ia, int port, int numDTG) {
        DatagramPacket dp = new DatagramPacket(tab, tab.length, ia, port);
        try {
            int i;
            //On essaye d'envoyer le DTG jusqu'à 3 fois si on ne reçoit aucun ACK
            for (i = 0; i < 3; i = (ACK(ds, numDTG) ? 5 : (i + 1)))
                ds.send(dp);
            if (i == 3)
                System.out.println("Le DTG num " + numDTG + " n'a pas pu etre envoye.");
            else System.out.println("Le DTG num " + numDTG + " a ete envoye.");
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
                //On r�cup�re l'adresse et le port de l'emetteur
                m_add = packet.getAddress();
                m_port = packet.getPort();
                //On regarde si on a reçu un ACK
                byte[] buffer = packet.getData();
                int opCode = (buffer[0] * 128) + buffer[1];
                int no = (buffer[2] * 128) + buffer[3];
                //Si on a un ACK pour le bon DTG, on retourne "true"
                if ((opCode == 4) && (no == numBloc))
                    return true;
                else System.out.println("Erreur :\n>OP Code : " + opCode + "\n>Num : " + no);
            }
        } catch (IOException e) {
            //Si ce n'est pas un Timeout, on affiche la trace
            if (!(e instanceof SocketTimeoutException))
                e.printStackTrace();
            else System.out.println("Timeout");
            //Timeout : on retourne "false"
            return false;
        }
    }
}
