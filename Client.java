import java.io.*;
import java.nio.*;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.nio.channels.*;

public class Client {

    public static void main(String[] args) throws IOException {

        boolean exit = false;
        String sentence;
        String serverSentence;
        InputStream is = null;
        DataOutputStream outToServer = null;
        Socket sock = null;
        BufferedReader inFromUser = null;
        BufferedReader inFromServer = null;

	inFromUser = new BufferedReader(new InputStreamReader(System.in));
	System.out.println("Enter a port number: ");
        int port = Integer.parseInt(inFromUser.readLine());
	System.out.println("Enter an ip address: ");
	String ip = inFromUser.readLine();

        try {
            sock = new Socket(ip, port);
            System.out.println("Connection to server: " + sock);
            while (!exit) {
                outToServer = new DataOutputStream(sock.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                inFromUser = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Commands: exit, send <filename>, listFiles");
                sentence = inFromUser.readLine();
                
                //Check user entry for exit
                if (sentence.equals("exit")) {
                    exit = true;
                }
                outToServer.writeBytes(sentence + '\n');
                serverSentence = inFromServer.readLine();
                System.out.println("Server Respone: " + serverSentence);
                sock = new Socket(ip, port);
            }
        //Clean up and close outputstreams and sockets
        } finally {
            if (sock != null)
                sock.close();
        }
    }

} 
