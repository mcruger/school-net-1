/**
 * Created with IntelliJ IDEA.
 * User: LEWIS
 * Date: 2/7/14
 * Time: 7:56 PM
 * To change this template use File | Settings | File Templates.
 */

import java.net.*;
import java.io.*;

/*******************************************

 NOTE:
 I used the -D option to pass my flags as -Dkey=value so I didn't have to parse them from args.
 example usage below. You can use either FQDN or ip for host flag.

 example usage:
 java -Dport=6789 ServerEcho

 ********************************************/

public class SimpleWebServer {
    public static void main(String[] args) throws IOException {

        //get value passed in from port flag
        String port = System.getProperty("port");

        //validate port flag, show usage if incorrect
        if (port == null || port.isEmpty() || port == "") {
            System.err.println("Usage: java -Dport=<port number> ServerEcho");
            System.exit(1);
        }

        //set port string from flag to int
        int portNum = Integer.parseInt(port);

        try {
            //init ServerSocket and Socket to send/receive messages from client
            ServerSocket serverSocket = new ServerSocket(portNum);
            Socket clientSocket = serverSocket.accept();

            //init PrintWriter and BufferedReader to send/receive messages to/from client socket
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            //capture input sent from client
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                //echo the message received from client back out to the client
                out.println(inputLine);
                //output the message to console received from the client
                System.out.println(inputLine);
            }
        } catch (IOException e) {
            //catch errors listening on port used for server/client socket connections
            System.out.println("Error listening on port "+ portNum + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}

