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

public class SimpleWebServer{

    //timeout for persistent connections. 15 seconds.
    public static final int TIMEOUT = 15000;

    public static void main(String[] args) throws IOException{

        boolean bRedirect = false;
        String strRedirect = "";

        //get value passed in from port flag
        String port = System.getProperty("port");

        //validate port flag, show usage if incorrect
        if (port == null || port.isEmpty() || port == ""){
            System.err.println("Usage: java -Dport=<port number> ServerEcho");
            System.exit(1);
        }

        //set port string from flag to int
        int portNum = Integer.parseInt(port);

        //init socket to listen on port passed in
        ServerSocket serverSocket = new ServerSocket(portNum);

        //flag and "dummy" value for persistent connections
        boolean holding = false;
        Socket hold = null;

        while(true){

            try{

                Socket clientSocket;

                //check to see if we're holding on persistent connection
                if(holding){
                    System.out.println("Holding");
                    clientSocket = hold;
                    clientSocket.setSoTimeout(TIMEOUT);
                    holding = false;
                }
                else {
                    System.out.println("Cutting");
                    clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(0);
                }

                //init PrintWriter and DataOutputStream to receive requests and send responses
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine = in.readLine();

                //this check is necessary on the uchicago cs linux machines
                //was getting null pointer exception without it
                if(inputLine != null){
                    //handle get requests
                    if (inputLine.startsWith("GET")){
                        int nHTTPStart = inputLine.indexOf("HTTP/");
                        File fin = new File(inputLine.substring(5, nHTTPStart - 1));

                        //build header
                        String header = getHeader(inputLine.substring(5, nHTTPStart - 1), in);

                        //blank line between header and body
                        out.writeBytes(header + "\r\n");

                        //good request
                        if (header.contains("200 OK")){
                            FileInputStream fin2 = new FileInputStream(fin);

                            //get contents of requested file and output it
                            int numBytes = (int) fin.length();
                            //System.out.println(numBytes);
                            byte[] fileInBytes = new byte[numBytes];
                            fin2.read(fileInBytes);
                            out.write(fileInBytes, 0, numBytes);

                        //redirect
                        } else if (header.contains("301 Mo")){
                            String site = header.substring(header.lastIndexOf(": ") + 2);
                            URL urlSite = new URL(site);
                            BufferedReader urlin = new BufferedReader(new InputStreamReader(urlSite.openStream()));
                            String example;
                            while((example = urlin.readLine()) != null){
                                out.writeBytes(example);
                            }
                        } else if (header.contains("404")){

                            out.writeBytes("<html><head><title>Not Found</title></head><body><h1><b><font color='red'>Sorry, the object you requested was not found.</font></b></h1></body><html>");

                        }

                        //check for persistent connections
                        if(header.contains("Connection: Keep-Alive")){
                            holding = true;
                            hold = clientSocket;
                            //System.out.println("Still Alive");
                            continue;

                        } else {
                            holding = false;
                            clientSocket.close();
                            //System.out.println("Connection Closed");
                            continue;

                        }

                    //head request
                    } else if (inputLine.startsWith("HEAD")){
                        int nHTTPStart = inputLine.indexOf("HTTP/");
                        out.writeBytes(getHeader(inputLine.substring(6, nHTTPStart - 1), in));

                        holding = false;
                        clientSocket.close();
                        continue;

                    //invalid request type (we only support head and get)
                    } else {
                        out.writeBytes("HTTP/ 403 Invalid Request\r\nConnection: close\r\n");
                        holding = false;
                        clientSocket.close();
                    }
                }
            } catch(IOException e){
                //catch errors listening for requests and is used to catch persistent connection timeouts
                System.out.println("Error listening on port " + portNum + " or listening for a connection");
                System.out.println(e.getMessage());
                holding = false;
                hold = null;
            }
        }

    }

    /**
     * Builds out header to return with response to recived HTTP request
     * @param inputLine
     * @param in
     * @return
     * @throws IOException
     */
    public static String getHeader(String inputLine, BufferedReader in) throws IOException{
        File F = new File(inputLine);
        String fullRequest = "";
        String line;

        //capture full request to look for keep alive
        while((line = in.readLine()) != null){
            if(line.length() == 0){break;}
            fullRequest += line + "\n";
        }

        //check for request type from request header
        if (F.exists()){
            //System.out.println("Exists");
            //Scanner s = new Scanner(F);
            String type = null;
            if (inputLine.endsWith(".defs")){
                return "HTTP/1.1 403 Forbidden \r\nConnection: close\r\n";
            } else if (inputLine.endsWith(".html") || inputLine.endsWith(".htm")){
                type = "text/html";
            } else if (inputLine.endsWith(".png")){
                type = "image/png";
            } else if (inputLine.endsWith(".txt")){
                type = "text/plain";
            } else if (inputLine.endsWith(".jpeg") || inputLine.endsWith("jpg")){
                type = "image/jpeg";
            } else if (inputLine.endsWith(".pdf")){
                type = "application/pdf";
            } else {
                type = "application/octet-stream";
            }

            //put together response  header
            String header = "HTTP/1.1 200 OK \r\n";
            header += "Content-Length: " + F.length() + "\r\n";
            header += "Content-Type: " + type + "\r\n";
            //System.out.println(fullRequest);
            header += fullRequest.contains("Connection: keep-alive") || fullRequest.contains("Connection: Keep-Alive") ?
            "Connection: Keep-Alive\r\n" : "Connection: close\r\n";
            if (header.contains("Connection: Keep-Alive")){
               header += "Keep-Alive: timeout=15, max=100\r\n";
            }
            return header;
        } else {
            //System.out.println("DNE");
            String path = inputLine;

            //check redirect.defs to see if request is listed in redirects file
            try{
                File redir = new File("www/redirect.defs");
                Scanner s = new Scanner(redir);

                while(s.hasNextLine()){
                    String[] stuff = s.nextLine().split(" ");
                    if (stuff[0].equals("/" + path)){
                        String header = "HTTP/1.1 301 Redirect\r\n";
                        header += "Location: " + stuff[1] + "\r\n";
                        header += "Connection: close\r\n";
                        return header;
                    }
                }

            } catch(FileNotFoundException e2){}

            //requested file not found, return 404
            String header = "HTTP/1.1 404 Not Found\r\n";
            header += "Connection: close\r\n";
            return header;
        }
    }
}

