//##############################################################################################
//
//  Tyler Paquet, John Marker, Devon Ozoga
//  Project 4: TCP Encrypted Chat Program
//  Chat Server
//  CIS 457
//  Due: 12/04/2017
//
//##############################################################################################

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import javax.xml.bind.DatatypeConverter;

public class Server3 {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;
    
    static CryptoChat ce;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 10;
    private static final clientThread[] threads = new clientThread[maxClientsCount];

    public static void main(String args[]) {

        int portNumber = 2222;

        //Open a server socket on portNumber
        try
        {
            serverSocket = new ServerSocket(portNumber);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        
        System.out.println("Connected to port number = " + portNumber);

        //Create a client socket for every connection and pass the socket to the client
        while (true)
        {
            try
            {
                clientSocket = serverSocket.accept();
                int i = 0;
                for (i = 0; i < maxClientsCount; i++)
                {
                    if (threads[i] == null)
                    {
                        (threads[i] = new clientThread(clientSocket, threads)).start();
                        break;
                    }
                }
                if (i == maxClientsCount)
                {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            }
            catch (IOException e)
            {
                System.out.println(e);
            }
        }
    }
}

class clientThread extends Thread
{

    private String clientName = null;
    private DataInputStream is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final clientThread[] threads;
    private int maxClientsCount;
    int kicked = 0;
    static CryptoChat ce;

    public clientThread(Socket clientSocket, clientThread[] threads)
    {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }

    public void run()
    {
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;

        try
        {
            //Create input and output streams for this client
            is = new DataInputStream(clientSocket.getInputStream());
            //is = new DataInputStream(CypherInputStream(clientSocket.getInputStream()), c);
            os = new PrintStream(clientSocket.getOutputStream());
            //os = new PrintStream(CypherOutputStream(clientSocket.getOutputStream()), c);
            String name;
            
            // Setting private key
			ce = new CryptoChat();
			ce.setPrivateKey("RSApriv.der");
            
            // Receive the encrypted secret key & decrypt
			byte encryptedsecret[] = new byte[256];
			is.read(encryptedsecret);
			byte decryptedsecret[] = ce.RSADecrypt(encryptedsecret); 
			SecretKey s = new SecretKeySpec(decryptedsecret, "AES");
			
			// Receive IV
			byte ivbytes[] = new byte[16];
			is.read(ivbytes);
			IvParameterSpec iv = new IvParameterSpec(ivbytes);
			
			// Open new OutStream with CypherOutputStream
			try{
			
				Cipher c_enc = Cipher.getInstance("AES/CBC/PKCS5Padding");
	    		c_enc.init(Cipher.ENCRYPT_MODE,s,iv);
	    		os = new PrintStream(new CipherOutputStream(clientSocket.getOutputStream(), c_enc));
	    	}catch(Exception e){
	    		System.out.println("AES Encrypt Exception");
	    		System.exit(1);
			}
			
			// Open new InputStream with CypherInputStream
	    	try{
			
				Cipher c_dec = Cipher.getInstance("AES/CBC/PKCS5Padding");
	    		c_dec.init(Cipher.DECRYPT_MODE,s,iv);
	    		is = new DataInputStream(new CipherInputStream(clientSocket.getInputStream(), c_dec));
	    	}catch(Exception e){
	    		System.out.println("AES Encrypt Exception");
	    		System.exit(1);
			}
            
            while (true)
            {	
                int duplicate = 0;
                os.println(addSpaces("\nEnter your username:"));
                os.flush();
                name = is.readLine().trim();

                //Check to see if entered username is currently being used
                synchronized (this)
                {
                    for (int i = 0; i < maxClientsCount; i++)
                    {
                        if (threads[i] != null && threads[i].clientName != null)
                        {
                            if(name.equals(threads[i].clientName.substring(1, threads[i].clientName.length())))
                            {
                                duplicate = 1;
                            }
                        }
                    }
                }

                //Alert the user if they entered invalid username, else continue
                if(duplicate == 1)
                {
                    os.println(addSpaces("This username is already taken."));
                    os.flush();
                }
                else if (name.indexOf('@') != -1)
                {
                    os.println(addSpaces("The name should not contain '@' character."));
                    os.flush();
                }
                else if (name.indexOf(' ') != -1)
                {
                    os.println(addSpaces("The name should not contain spaces."));
                    os.flush();
                }
                else
                {
                    break;
                }
            }

            os.println("");

            //Welcome the new client
            os.println(addSpaces("<Server> Welcome " + name));
            os.flush();
            synchronized (this)
            {
                for (int i = 0; i < maxClientsCount; i++)
                {
                    if (threads[i] != null && threads[i] == this)
                    {
                        clientName = "@" + name;
                        break;
                    }
                }

                //Alert other clients that the new client has joined
                for (int i = 0; i < maxClientsCount; i++)
                {
                    if (threads[i] != null && threads[i] != this && threads[i].clientName != null)
                    {
                        threads[i].os.println(addSpaces("<Server> " + name + " has joined the chat room"));
                        threads[i].os.flush();
                    }
                }
            }

            //Prints total connected clients to server
            synchronized (this)
            {
                int openClients = 0;
                for (int i = 0; i < maxClientsCount; i++)
                {
                    if (threads[i] != null && threads[i].clientName != null)
                    {
                        openClients++;
                    }
                }
                System.out.println("Number of clients connected: " + openClients);
            }

            //start conversation (break when the client is kicked)
            while (this.kicked == 0)
            {
                String line = is.readLine().trim();
                if(this.kicked == 1)
                {
                    break;
                }

                //If client typed /q then break from conversation loop
                if (line.startsWith("/quit"))
                {
                    synchronized (this)
                    {
                        int openClients = 0;
                        for (int i = 0; i < maxClientsCount; i++)
                        {
                            if (threads[i] != null && threads[i].clientName != null)
                            {
                                openClients++;
                            }
                        }
                        openClients--;
                        System.out.println("Number of clients connected: " + openClients);
                    }
                    break;
                }

                //Send Private Message only to correct client
                if (line.startsWith("@"))
                {
                    String[] words = line.split("\\s", 2);
                    if (words.length > 1 && words[1] != null)
                    {
                        words[1] = words[1].trim();
                        if (!words[1].isEmpty())
                        {
                            synchronized (this)
                            {
                                for (int i = 0; i < maxClientsCount; i++)
                                {
                                    if (threads[i] != null && threads[i] != this
                                            && threads[i].clientName != null
                                            && threads[i].clientName.equals(words[0]))
                                    {
                                        threads[i].os.println(addSpaces("{from " + name + "} " + words[1]));
                                        threads[i].os.flush();
                                        this.os.println(addSpaces("{to " + threads[i].clientName.substring(1,
                                                threads[i].clientName.length()) + "} " + words[1]));
                                                this.os.flush();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                //Send client list back to client
                else if(line.startsWith("/clients"))
                {
                    synchronized (this)
                    {
                        this.os.println(addSpaces("\nClient List:"));
                        this.os.flush();
                        this.os.println(addSpaces("-----------------------"));
                        this.os.flush();
                        for (int i = 0; i < maxClientsCount; i++)
                        {
                            if (threads[i] != null && threads[i] != this
                                    && threads[i].clientName != null)
                            {
                                this.os.println(addSpaces(threads[i].clientName.substring(1, threads[i].clientName.length())));
                                os.flush();
                            }
                        }
                        this.os.println(addSpaces("-----------------------\n"));
                        this.os.flush();
                    }
                }

                //Kick the specified user
                else if(line.startsWith("/kick @"))
                {
                    //System.out.println(line.substring(5, line.length()));
                    String kickedName;
                    synchronized (this)
                    {
                        for (int i = 0; i < maxClientsCount; i++)
                        {
                            if (threads[i] != null && threads[i].clientName.equals(line.substring(6, line.length())))
                            {
                                kickedName = threads[i].clientName;
                                System.out.println(this.clientName.substring(1, this.clientName.length()) + " kicked " +
                                        kickedName.substring(1, kickedName.length()));
                                threads[i].kicked = 1;
                                threads[i].os.println(addSpaces("You have been kicked by " +
                                        this.clientName.substring(1, this.clientName.length())));
                                        threads[i].os.flush();
                                threads[i] = null;

                                synchronized (this)
                                {
                                    int openClients = 0;
                                    for (int j = 0; j < maxClientsCount; j++)
                                    {
                                        if (threads[j] != null && threads[j].clientName != null)
                                        {
                                            openClients++;
                                        }
                                    }
                                    System.out.println("Number of clients connected: " + openClients);
                                }

                                synchronized (this)
                                {
                                    for (int k = 0; k < maxClientsCount; k++)
                                    {
                                        if (threads[k] != null && threads[k].clientName != null)
                                        {
                                            threads[k].os.println(addSpaces("<Server> " +
                                                    this.clientName.substring(1, this.clientName.length()) + " kicked " +
                                                    kickedName.substring(1, kickedName.length())));
                                                    os.flush();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if(!line.matches(".*\\w.*"))
                {
                }

                //Message must be public so broadcast to all clients
                else
                {
                    synchronized (this)
                    {
                        for (int i = 0; i < maxClientsCount; i++)
                        {
                            if (threads[i] != null && threads[i].clientName != null)
                            {
                            	if (threads[i].clientName == this.clientName)
                            	{
                            		threads[i].os.println(addSpaces("<You> " + line));
                                	threads[i].os.flush();
                            	}
                            	else
                            	{
                            		threads[i].os.println(addSpaces("<" + name + "> " + line));
                                	threads[i].os.flush();
                            	}
                            }
                        }
                    }

                }
            }

            //Only print if the user wasn't kicked
            if(kicked == 0)
            {
                synchronized (this)
                {
                    for (int i = 0; i < maxClientsCount; i++)
                    {
                        if (threads[i] != null && threads[i] != this
                                && threads[i].clientName != null)
                        {

                            threads[i].os.println(addSpaces("<Server> " + name + " has left the chat room"));
                            threads[i].os.flush();
                        }
                    }
                }
            }
            os.println(addSpaces("See Ya Later " + name));
            os.flush();

            //Clean up. Set Variable to null so server can accept another client
            synchronized (this)
            {
                for (int i = 0; i < maxClientsCount; i++)
                {
                    if (threads[i] == this)
                    {
                        threads[i] = null;
                    }
                }
            }

            //Close streams and socket
            is.close();
            os.close();
            clientSocket.close();
        }
        catch (IOException e)
        {
        }
    }
    
    public static String addSpaces(String str)
    {
    	return str + "\n                                                                            ";
    }
}
