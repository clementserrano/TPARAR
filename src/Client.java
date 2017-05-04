import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

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
        FileInputStream file = null;
        DatagramSocket socket;
        //Buffer servant à la lecture
        byte[] buffer = new byte[512];
        //Compteur du nombre d'octets lus
        int compteur = 0;
        //Numéro du DTG
        int noDTG = 0;
        try
        {
        	//Creation du DatagramSocket
        	socket = new DatagramSocket();
        	//On fixe la durée du TimeOut à 3 secondes
        	socket.setSoTimeout(3000);

        	//Emission du WRQ fichier local
            ByteBuffer bBuffer = ByteBuffer.allocate(taille);
            bBuffer.put((byte)0);
            bBuffer.put((byte)2);
            bBuffer.put(fichierLocal.getBytes());
            bBuffer.put((byte)0);
            bBuffer.put("octet".getBytes());
            bBuffer.put((byte)0);
            bBuffer = bBuffer.slice();
            sendBytes(socket, bBuffer.array(), adresse, port, 0);

        	//Ouverture du fichier local
			file = new FileInputStream(new File(fichierLocal));
			//Lecture du fichier
			while((compteur = file.read(buffer)) >= 0)
			{
				//Lecture du fichier
				if(compteur != buffer.length)
				{
					byte[] buf = new byte[compteur];
					for(int i = 0; i < compteur; i++)
						buf[i] = buffer[i];
					buffer = buf;
				}
				//Crétaion du DTG
				byte[] DTG = new byte[compteur + 4];
				DTG[0] = 0;
				DTG[1] = 3;
				DTG[2] = (byte)(noDTG / 256);
				DTG[3] = (byte)(noDTG % 256);
				for(int i = 0; i < compteur; i++)
					DTG[4 + i] = buffer[i];
				//Envoie du fichier
				sendBytes(socket, buffer, adresse, port, noDTG);
				noDTG++;
			}
		}
        catch (IOException e)
        {
			e.printStackTrace();
		}

    }

    //Envoi d'un DTG jusqu'à 3 fois, test de réception du ACK
    public static void sendBytes(DatagramSocket ds, byte[] tab, InetAddress ia, int port, int numDTG)
    {
    	DatagramPacket dp = new DatagramPacket(tab, tab.length, ia, port);
    	try
    	{
    		int i;
    		//On essaye d'envoyer le DTG jusqu'à 3 fois si on ne reçoit aucun ACK
    		for(i = 0; i < 3; i = ACK(ds, numDTG) ? 5 : (i + 1))
    			ds.send(dp);
    		if(i == 3)
    			System.out.println("Le DTG n°" + numDTG + " n'a pas pu être envoyé.");
		}
    	catch (IOException e1)
    	{
			e1.printStackTrace();
		}
    }

    //Retourne "true" à la la réception du ACK correspondant au numéro du DTG,
    //		   "false" si on passe par un Timeout
    public static boolean ACK(DatagramSocket socket, int numBloc)
    {
    	DatagramPacket packet;
    	try
    	{
    		//On reçoit jusqu'à recevoir un ACK pour le DTG voulu ou avoir un Timeout
    		while(true)
    		{
	    		packet = new DatagramPacket(new byte[512], 512);
	    		socket.receive(packet);
	    		//On regarde si on a reçu un ACK
	    		byte[] buffer = packet.getData();
	    		int opCode = 0;
	    		int no = 0;
	    		for(int i = 0; i < 4; i++)
	    		{
	    			opCode *= 2;
	    			no *= 2;
	    			opCode += buffer[i];
	    			no += buffer[4 + i];
	    		}
	    		//Si on a un ACK pour le bon DTG, on retourne "true"
	    		if((opCode == 4) && (no == numBloc))
	    			return true;
    		}
    	}
    	catch(IOException e)
    	{
    		//Timeout : on retourne "false"
    		return false;
    	}
    }
}
