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

public class Client2 implements Runnable {

    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static PrintStream os = null;
    // The input stream
    private static DataInputStream is = null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;

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
        System.out.println("Welcome to the TCP Encrypted Chat Program. There are a few things you should know " +
                "before you get started. There are two types of messages. Private {} and Public <>. " +
                "A private message can be sent by starting the message " +
                "with '@username'. All other messages will be sent as public. Enjoy!");

        System.out.println("\nList of current commands:");
        System.out.println("--------------------------------------------------");
        System.out.println("/q: quits the program");
        System.out.println("clientlist: returns a list of conncected clients");
        System.out.println("@username message: sends a private message if the username " +
                "is a connected client");
        System.out.println("kick @username: if the user is connected, they will be " +
                "kicked");
        System.out.println("--------------------------------------------------");

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
