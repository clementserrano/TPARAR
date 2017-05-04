import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class Client {

    private final static int taille = 512;

    public static void main(String[] args) {
        // receiveFile
        try {
            receiveFile(InetAddress.getByName("127.0.0.1"),69,"fichier.txt","fichier.txt");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(InetAddress adresse, int port, String fichierDistant, String fichierLocal){
        // Ouverture du port
        DatagramSocket soc = null;
        try {
            soc = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // Envoi de RRQ fichierDistant
        ByteBuffer buffer = ByteBuffer.allocate(taille);
        buffer.put((byte)0);
        buffer.put((byte)1); // RRQ
        buffer.put(fichierDistant.getBytes());
        buffer.put((byte)0);
        buffer.put("octet".getBytes());
        buffer.put((byte)0);
        buffer = buffer.slice();
        DatagramPacket dpSend = new DatagramPacket(buffer.array(),buffer.capacity(),adresse, port);
        System.out.println(buffer.capacity());

        try {
            soc.send(dpSend);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fermeture du socket
        soc.close();
    }

    public void sendFile(InetAddress adresse, int port, String fichierLocal)
    {

    }

    public static void sendBytes(DatagramSocket ds, byte[] tab, InetAddress ia, int port)
    {
    	DatagramPacket dp = new DatagramPacket(tab, tab.length, ia, port);
    	try
    	{
			ds.send(dp);
		}
    	catch (IOException e1)
    	{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
}
