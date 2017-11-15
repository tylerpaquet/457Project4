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

public class Server2 {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 10;
    private static final clientThread[] threads = new clientThread[maxClientsCount];

    public static void main(String args[]) {

        // The default port number.
        int portNumber = 2222;

        System.out.println("Connected to port number = " + portNumber);

        //Open a server socket on portNumber
        try
        {
            serverSocket = new ServerSocket(portNumber);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

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
            os = new PrintStream(clientSocket.getOutputStream());
            String name;
            while (true)
            {
                os.println("\nEnter your username:");
                name = is.readLine().trim();
                if (name.indexOf('@') != -1)
                {
                    os.println("The name should not contain '@' character.");
                }
                else
                {
                    break;
                }
            }

            os.println("");

            //Welcome the new client
            os.println("<Server> Welcome " + name);
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
                for (int i = 0; i < maxClientsCount; i++)
                {
                    if (threads[i] != null && threads[i] != this)
                    {
                        threads[i].os.println("<Server> " + name + " has joined the chat room");
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
            //start conversation
            while (true)
            {
                String line = is.readLine();
                if (line.startsWith("/q"))
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

                //Send Private Message only to right client
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
                                        threads[i].os.println("{from " + name + "} " + words[1]);
                                        this.os.println("{to " + threads[i].clientName.substring(1,
                                                threads[i].clientName.length()) + "} " + words[1]);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                else if(line.startsWith("ClientList"))
                {
                    synchronized (this)
                    {
                        this.os.println("\nClient List:");
                        this.os.println("-----------------------");
                        for (int i = 0; i < maxClientsCount; i++)
                        {
                            if (threads[i] != null && threads[i] != this
                                    && threads[i].clientName != null)
                            {
                                this.os.println(threads[i].clientName.substring(1, threads[i].clientName.length()));
                            }
                        }
                        this.os.println("-----------------------\n");
                    }
                }
                else
                {
                    if(true)
                    {
                        //Message must be public so broadcast to all clients
                        synchronized (this)
                        {
                            for (int i = 0; i < maxClientsCount; i++)
                            {
                                if (threads[i] != null && threads[i].clientName != null)
                                {
                                    threads[i].os.println("<" + name + "> " + line);
                                }
                            }
                        }
                    }

                }
            }

            synchronized (this)
            {
                for (int i = 0; i < maxClientsCount; i++)
                {
                    if (threads[i] != null && threads[i] != this
                            && threads[i].clientName != null)
                    {
                        threads[i].os.println("<Server> " + name + " has left the chat room");
                    }
                }
            }
            os.println("See Ya Later " + name);

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
}
