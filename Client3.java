//##############################################################################################
//
//  Tyler Paquet, John Marker, Devon Ozoga
//  Project 4: TCP Encrypted Chat Program
//  Chat Client
//  CIS 457
//  Due: 12/04/2017
//
//##############################################################################################

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client3 implements Runnable {

    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static PrintStream os = null;
    // The input stream
    private static DataInputStream is = null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;
    
    public byte[] encrypt(byte[] plaintext, SecretKey secKey, IvParameterSpec iv){
        try{
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE,secKey,iv);
            byte[] ciphertext = c.doFinal(plaintext);
            return ciphertext;
        }catch(Exception e){
            System.out.println("AES Encrypt Exception");
            System.exit(1);
            return null;
        }
    }
    public byte[] RSAEncrypt(byte[] plaintext){
	try{
	    Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
	    c.init(Cipher.ENCRYPT_MODE,pubKey);
	    byte[] ciphertext=c.doFinal(plaintext);
	    return ciphertext;
	}catch(Exception e){
	    System.out.println("RSA Encrypt Exception");
	    System.exit(1);
	    return null;
	}
    }
    public SecretKey generateAESKey(){
	try{
	    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	    keyGen.init(128);
	    SecretKey secKey = keyGen.generateKey();
	    return secKey;
	}catch(Exception e){
	    System.out.println("Key Generation Exception");
	    System.exit(1);
	    return null;
	}
    }
    public void setPublicKey(String filename){
	try{
	    File f = new File(filename);
	    FileInputStream fs = new FileInputStream(f);
	    byte[] keybytes = new byte[(int)f.length()];
	    fs.read(keybytes);
	    fs.close();
	    X509EncodedKeySpec keyspec = new X509EncodedKeySpec(keybytes);
	    KeyFactory rsafactory = KeyFactory.getInstance("RSA");
	    pubKey = rsafactory.generatePublic(keyspec);
	}catch(Exception e){
	    System.out.println("Public Key Exception");
	    System.exit(1);
	}
    }
    public byte[] encrypt(byte[] plaintext, SecretKey secKey, IvParameterSpec iv){
	try{
	    Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	    c.init(Cipher.ENCRYPT_MODE,secKey,iv);
	    byte[] ciphertext = c.doFinal(plaintext);
	    return ciphertext;
	}catch(Exception e){
	    System.out.println("AES Encrypt Exception");
	    System.exit(1);
	    return null;
	}
    }

    public static void main(String[] args) {

        // The default port.
        int portNumber = 2222;
        // The default host.
        String host = "localhost";


        //open socket for sending data and create input and output streams for socket
        try
        {
            clientSocket = new Socket(host, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new DataInputStream(clientSocket.getInputStream());
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
                new Thread(new Client2()).start();

                //send messages
                while (!closed)
                {
                    os.println(inputLine.readLine().trim());
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
                System.out.println(responseLine);
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
}