import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class Server {

    public static void main(String[] args) throws IOException {

        ServerSocket servsock = null;
        Socket sock = null;
	BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter a port number: ");
	int portNum = Integer.parseInt(input.readLine());

        try {
           
            servsock = new ServerSocket(portNum);
            System.out.println("Waiting for connection");
            
            //Every time a new connection from a client is made, a new thread is created by calling ServerThread method
            while (true) {
                try {
                    sock = servsock.accept();
                    new ServerThread(sock).start();
                } catch (IOException e) {
                }
            }
        } finally {
            if (servsock != null)
                servsock.close();
        }
    }
}

//New class to implement threading (allows for server to service clients in any order)
class ServerThread extends Thread {
    Socket sock = null;
    public ServerThread(Socket clientSocket) {
        this.sock = clientSocket;
    }
    //Executes actions requested from the client
    public void run() {
        //System.out.println("Connection established: " + sock);
        String clientSentence;
	BufferedReader inFromClient=null;
	DataOutputStream outToClient=null;
        try {
            try {
                inFromClient = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                outToClient = new DataOutputStream(sock.getOutputStream());
                clientSentence = inFromClient.readLine();
                System.out.println("Client said: " + clientSentence);
                outToClient.writeBytes(clientSentence + '\n');
                outToClient.flush();
		          
            } finally {
		if (outToClient != null)
                    outToClient.close();
		if (inFromClient != null)
                    inFromClient.close();
                if (sock != null)
                    sock.close();
              }
            } catch (Exception e) {}
    }
}   
