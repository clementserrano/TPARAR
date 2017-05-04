import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

    public static void main(String[] args) {
        // do things
    }

    public void receiveFile(InetAddress adresse, int port, String fichierDistant, String fichierLocal){

    }

    public void sendFile(InetAddress adresse, int port, String fichierLocal){

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
