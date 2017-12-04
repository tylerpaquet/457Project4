//##############################################################################################
//
//  Tyler Paquet, John Marker, Devon Ozoga
//  Project 4: TCP Encrypted Chat Program
//  Chat Client
//  CIS 457
//  Due: 12/04/2017
//
//##############################################################################################

import java.io.*;
import java.net.*;
import java.nio.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import javax.xml.bind.DatatypeConverter;

public class Client3 implements Runnable {

    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static PrintStream os = null;
    // The input stream
    private static DataInputStream is = null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;
    
    static CryptoChat ce;
	static SecretKey s;

    public static void main(String[] args) {

        // The default port.
        int portNumber = 2222;
        
        // The default host.
        String host = "localhost";
        
        // Set Public Key
		ce = new CryptoChat();
		ce.setPublicKey("RSApub.der");
		SecureRandom r = new SecureRandom();


        //open socket for sending data and create input and output streams for socket
        try
        {
            clientSocket = new Socket(host, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new DataInputStream(clientSocket.getInputStream());
            
            // Generate Symmetric Key
			s = ce.generateAESKey();
			
			// Send Symmetric Key to server encrypted with public key
			byte encryptedsecret[] = ce.RSAEncrypt(s.getEncoded());
			os.write(encryptedsecret);
			
			// Create and send IV
			byte ivbytes[] = new byte[16];
			r.nextBytes(ivbytes);
			IvParameterSpec iv = new IvParameterSpec(ivbytes);
			os.write(ivbytes);
			
			
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
			
			
        }
        catch (UnknownHostException e)
        {
            System.err.println("Don't know about host " + host);
        }
        catch (IOException e)
        {
            System.err.println("Couldn't get I/O for the connection to the host " + host);
        }


        //User information and instrucitons
        System.out.println("\nWelcome to our TCP Encrypted Chat Program. Enjoy!");

        System.out.println("\nList of current commands:");
        System.out.println("--------------------------------------------------------------");
        System.out.println("/quit\t\t\t| quits the program");
        System.out.println("--------------------------------------------------------------");
        System.out.println("/clients\t\t| returns a list of connected clients");
        System.out.println("--------------------------------------------------------------");
        System.out.println("/kick @client\t\t| kicks client");
        System.out.println("--------------------------------------------------------------");
        System.out.println("@client message\t\t| sends a private message to client");
        System.out.println("--------------------------------------------------------------");
        
        System.out.println("\nOther useful info:");
        System.out.println("--------------------------------------------------------------");
        System.out.println("Anything not in command list will be sent as a public message");
        System.out.println("--------------------------------------------------------------");
        System.out.println("Public messages <>\tPrivate messages {}");
        System.out.println("--------------------------------------------------------------");
        

        if (clientSocket != null && os != null && is != null)
        {
            try
            {
                //create new thread to read messages from server
                new Thread(new Client3()).start();

                //send messages
                while (!closed)
                {
                    os.println(addSpaces(inputLine.readLine()));
                    os.flush();
                }

                //Close streams and socket
                os.close();
                is.close();
                clientSocket.close();
            }
            catch (IOException e)
            {
                System.err.println("IOException:  " + e);
            }
        }
    }

    public void run()
    {
        //Keep reading from server until we receive a message that starts with 'See' or 'You'.
        String responseLine;
        try
        {
            while ((responseLine = is.readLine()) != null)
            {
                System.out.println(responseLine.trim());
                if (responseLine.startsWith("See"))
                {
                    break;
                }
                if (responseLine.startsWith("You"))
                {
                    break;
                }
            }
            closed = true;
        }
        catch (IOException e)
        {
            System.err.println("IOException:  " + e);
        }
    }
    
    public static String addSpaces(String str)
    {
    	return str + "\n                                                                            ";
    }
}
