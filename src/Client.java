import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
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

        // Ouverture du fichierLocal
        FileOutputStream file = null;
        try {
            file = new FileOutputStream("data/" + fichierLocal);
        } catch (IOException e) {
            soc.close();
            return -1; // CrRv < 0, erreur locale (impossible d'accéder ou répertoire pour créer un fichier, le flchîer existe déjà..,).
        }
        boolean fileOpen = true;

        // Envoi de RRQ fichierDistant
        ByteBuffer bufSend = ByteBuffer.allocate(taille).put(RRQ).put(fichierDistant.getBytes()).put((byte) 0).put("octet".getBytes()).put((byte) 0).slice();
        DatagramPacket dpSend = new DatagramPacket(bufSend.array(), bufSend.capacity(), adresse, port);
        soc.send(dpSend);

        boolean once = true;

        while (fileOpen) {
            // Attente réponse
            byte[] bufReceive = new byte[taille];
            DatagramPacket dpReceive = new DatagramPacket(bufReceive, bufReceive.length);

            try {
                soc.receive(dpReceive);
            } catch (SocketTimeoutException e) {
                if (fileOpen) {
                    file.close();
                    soc.close();
                    return 1; // CrRv > 0 erreur de transfert intervenue sur le serveur
                }
                break;
            }

            // On récupère l'opcode
            byte[] opcode = Arrays.copyOfRange(bufReceive, 0, 2);

            if (opcode[1] == DATA[1]) {
                // On récupère le numéro de bloc
                byte[] num = Arrays.copyOfRange(bufReceive, 2, 4);

                if (fileOpen) {

                    //while (i >= 0 && bufReceive.get(i) == 0) --i;
                    byte[] data = Arrays.copyOfRange(bufReceive, 4, dpReceive.getLength());

                    // On écrit les données dans le fichier local
                    file.write(data);
                    file.flush();

                    if (dpReceive.getLength() < taille) {
                        // Fermeture du fichier
                        file.close();
                        fileOpen = false;
                    }
                }

                // On envoi un ACK avec le bon numéro de bloc
                bufSend = ByteBuffer.allocate(taille).put(ACK).put(num).slice();
                dpSend = new DatagramPacket(bufSend.array(), bufSend.capacity(), dpReceive.getAddress(), dpReceive.getPort());

                soc.send(dpSend);

                if (once) {
                    soc.setSoTimeout(3000);
                    once = false;
                }
            }
        }

        // Fermeture du socket
        soc.close();

        return 0; // CrRv = 0, le transfert s'est bien déroulé
    }

    public int sendFile(InetAddress adresse, int port, String fichierLocal) {
        FileInputStream file = null;
        DatagramSocket socket;
        //Buffer servant à la lecture
        byte[] buffer = new byte[512];
        //Compteur du nombre d'octets lus
        int compteur = 0;
        //Numéro du DTG
        int noDTG = 1;

        //Ouverture du fichier local
        try {
            file = new FileInputStream(new File("data/" + fichierLocal));
        } catch (FileNotFoundException e1) {
            //Erreur en local
            return -1;
        }

        try {
            //Creation du DatagramSocket
            socket = new DatagramSocket();
            //On fixe la durée du TimeOut à 3 secondes
            socket.setSoTimeout(3000);

            //Emission du WRQ fichier local
            ByteBuffer bBuffer = ByteBuffer.allocate(taille).put(WRQ).put(fichierLocal.getBytes()).put((byte) 0).put("octet".getBytes()).put((byte) 0).slice();
            if (!sendBytes(socket, bBuffer.array(), adresse, port, 0))
                return 1;
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
                byte[] num = Conv(noDTG);
                DTG.put(DATA).put(num[0]).put(num[1]).put(buffer).slice();
                //Envoi du fichier
                if (!sendBytes(socket, DTG.array(), m_add, m_port, noDTG)) {
                    return 2;
                }
                if (noDTG == 65536)
                    noDTG = 0;
                else noDTG++;

            }
            file.close();
            socket.close();
        } catch (IOException e) {
            //Erreur en ligne
            return 3;
        }
        return 0;
    }

    protected byte[] Conv(int i) {
        byte[] res = new byte[2];
        res[1] = (byte) (i & 0x000000FF);
        res[0] = (byte) ((i & 0x0000FF00) / 256);
        return res;
    }

    //Envoi d'un DTG jusqu'à 3 fois, test de réception du ACK
    public static boolean sendBytes(DatagramSocket ds, byte[] tab, InetAddress ia, int port, int numDTG) {
        DatagramPacket dp = new DatagramPacket(tab, tab.length, ia, port);
        int i;
        //On essaye d'envoyer le DTG jusqu'à 3 fois si on ne reçoit aucun ACK
        for (i = 0; i < 3; i = (ACK(ds, numDTG) ? 5 : (i + 1))) {
            try {
                ds.send(dp);
            } catch (Exception e) {
                return false;
            }
        }
        if (i == 3)
            return false;
        return true;
    }

    //Retourne "true" à la la réception du ACK correspondant au numéro du DTG,
    //		   "false" si on passe par un Timeout
    public static boolean ACK(DatagramSocket socket, int numBloc) {
        DatagramPacket packet;
        boolean ack = false;
        try {
            //On reçoit jusqu'à recevoir un ACK pour le DTG voulu ou avoir un Timeout
            while (!ack) {
                packet = new DatagramPacket(new byte[512], 512);
                socket.receive(packet);
                //On récupère l'adresse et le port de l'emetteur
                m_add = packet.getAddress();
                m_port = packet.getPort();
                //On regarde si on a reçu un ACK
                byte[] buffer = packet.getData();
                int LSB = buffer[3];
                int MSB = buffer[2];
                if (LSB < 0)
                    LSB = 256 + LSB;
                if (MSB < 0)
                    MSB = 256 + MSB;
                int opCode = (buffer[0] * 256) + buffer[1];
                int no = (MSB * 256) + LSB;
                if (no < 0)
                    no *= -1;
                //System.out.println(no + ", " + numBloc);
                //Si on a un ACK pour le bon DTG, on retourne "true"
                ack = ((opCode == 4) && (no == numBloc));
            }
        } catch (IOException e) {
            //Timeout : on retourne "false"
        }
        return ack;
    }
}
