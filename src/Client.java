import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

    public static void main(String[] args)
    {
        // do things
    }

    public void receiveFile(InetAddress adresse, int port, String fichierDistant, String fichierLocal)
    {

    }

    public void sendFile(InetAddress adresse, int port, String fichierLocal)
    {
        FileInputStream file = null;
        DatagramSocket socket;
        //Buffer servant � la lecture
        byte[] buffer = new byte[512];
        //Compteur du nombre d'octets lus
        int compteur = 0;
        //Num�ro du DTG
        int noDTG = 0;
        try
        {
        	//Creation du DatagramSocket
        	socket = new DatagramSocket();
        	//On fixe la dur�e du TimeOut � 3 secondes
        	socket.setSoTimeout(3000);
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
				//Cr�taion du DTG
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

    //Envoi d'un DTG jusqu'� 3 fois, test de r�ception du ACK
    public static void sendBytes(DatagramSocket ds, byte[] tab, InetAddress ia, int port, int numDTG)
    {
    	DatagramPacket dp = new DatagramPacket(tab, tab.length, ia, port);
    	try
    	{
    		int i;
    		//On essaye d'envoyer le DTG jusqu'� 3 fois si on ne re�oit aucun ACK
    		for(i = 0; i < 3; i = ACK(ds, numDTG) ? 5 : (i + 1))
    			ds.send(dp);
    		if(i == 3)
    			System.out.println("Le DTG n�" + numDTG + " n'a pas pu �tre envoy�.");
		}
    	catch (IOException e1)
    	{
			e1.printStackTrace();
		}
    }

    //Retourne "true" � la la r�ception du ACK correspondant au num�ro du DTG,
    //		   "false" si on passe par un Timeout
    public static boolean ACK(DatagramSocket socket, int numBloc)
    {
    	DatagramPacket packet;
    	try
    	{
    		//On re�oit jusqu'� recevoir un ACK pour le DTG voulu ou avoir un Timeout
    		while(true)
    		{
	    		packet = new DatagramPacket(new byte[512], 512);
	    		socket.receive(packet);
	    		//On regarde si on a re�u un ACK
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
