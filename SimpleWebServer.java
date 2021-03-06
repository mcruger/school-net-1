/**
 * Created with IntelliJ IDEA.
 * User: LEWIS
 * Date: 2/7/14
 * Time: 7:56 PM
 * To change this template use File | Settings | File Templates.
 */

import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * ****************************************
 * <p/>
 * NOTE:
 * I used the -D option to pass my flags as -Dkey=value so I didn't have to parse them from args.
 * example usage below. You can use either FQDN or ip for host flag.
 * <p/>
 * example usage:
 * java -Dport=6789 ServerEcho
 * <p/>
 * ******************************************
 */

public class SimpleWebServer {
    public static void main(String[] args) throws IOException {

        boolean bRedirect = false;
        String strRedirect = "";

        //get value passed in from port flag
        String port = System.getProperty("port");

        //validate port flag, show usage if incorrect
        if (port == null || port.isEmpty() || port == "") {
            System.err.println("Usage: java -Dport=<port number> ServerEcho");
            System.exit(1);
        }

        //set port string from flag to int
        int portNum = Integer.parseInt(port);
        //boolean thinking = true;

       // while (true) {
        try {

            //init ServerSocket and Socket to send/receive messages from client
            ServerSocket serverSocket = new ServerSocket(portNum);
            Socket clientSocket = serverSocket.accept();

            //init PrintWriter and BufferedReader to send/receive messages to/from client socket
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            //capture input sent from client
            String inputLine;
            //String inputLine = in.readLine();
            //while ((inputLine = in.readLine()) != null) {
            //echo the message received from client back out to the client
            //while((inputLine = in.readLine()) == null) ;
            //thinking = true;
            while ((inputLine = in.readLine()) != null) {

                //inputLine = in.readLine();

                if (inputLine.isEmpty() != true || inputLine != "") {

                    if (bRedirect) {

                        bRedirect = false;
                        continue;
                    } else if (inputLine.startsWith("GET")) {
                        int nHTTPStart = inputLine.indexOf("HTTP/");
                        File fin = new File(inputLine.substring(5, nHTTPStart - 1));

                        String header = getHeader(inputLine.substring(5, nHTTPStart - 1));
                        out.writeBytes(header + "\r\n");
                        if (header.contains("200 OK")) {
                            FileInputStream fin2 = new FileInputStream(fin);

                            //out.writeBytes("\r\n");
                            int numBytes = (int) fin.length();
                            //System.out.println(numBytes);
                            byte[] fileInBytes = new byte[numBytes];
                            fin2.read(fileInBytes);
                            out.write(fileInBytes, 0, numBytes);
                        } else if (header.contains("301 Re")) {
                            String site = header.substring(header.lastIndexOf(": ") + 2);
                            URL urlSite = new URL(site);
                            BufferedReader urlin = new BufferedReader(new InputStreamReader(urlSite.openStream()));
                            String example;
                            while ((example = urlin.readLine()) != null) {
                                out.writeBytes(example);
                            }
                        }

                        continue;

                    } else if (inputLine.startsWith("HEAD")) {
                        int nHTTPStart = inputLine.indexOf("HTTP/");
                        out.writeBytes(getHeader(inputLine.substring(6, nHTTPStart - 1)));

                        continue;
                    } else {
                        out.writeBytes("HTTP/ 403 Invalid Request");
                    }
                    //out.println("Echo" + inputLine);
                    //output the message to console received from the client
               }
           }
        } catch (IOException e) {
            //catch errors listening on port used for server/client socket connections
            System.out.println("Error listening on port " + portNum + " or listening for a connection");
            System.out.println(e.getMessage());
            //break;
        }
       // }

    }

    public static String getHeader(String inputLine) {
        try {
            File F = new File(inputLine);
            Scanner s = new Scanner(F);
            String type = null;
            if (inputLine.endsWith(".html")) {
                type = "text/html";
            }
            if (inputLine.endsWith(".png")) {
                type = "image/png";
            }
            if (inputLine.endsWith(".txt")) {
                type = "text/plain";
            }
            if (inputLine.endsWith(".jpeg") || inputLine.endsWith("jpg")) {
                type = "image/jpeg";
            }
            if (inputLine.endsWith(".pdf")) {
                type = "application/pdf";
            }
            String header = "HTTP/1.1 200 OK \r\n";
            header += "Content-Length: " + F.length() + "\r\n";
            header += "Content-Type: " + type + "\r\n";
            return header;
        } catch (FileNotFoundException e) {

            String path = inputLine.substring(3);

            try {
                File redir = new File("www/redirect.defs");
                Scanner s = new Scanner(redir);

                while (s.hasNextLine()) {
                    String[] stuff = s.nextLine().split(" ");
                    if (stuff[0].equals(path)) {
                        String header = "HTTP/1.1 301 Redirect \r\n";
                        header += "Location: " + stuff[1] + "\r\n";
                        return header;
                    }
                }

            } catch (FileNotFoundException e2) {
            }

            String header = "HTTP/1.1 404 Not Found\r\n";
            header += "Content-type: text/html\r\n";
            header += "Content-length: 135\r\n\r\n";

            header += "<html><head><title>Not Found</title></head><body>Sorry, the object you requested was not found.</body><html>";

            return header;

        }
    }
}

