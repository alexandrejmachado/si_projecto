import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**

 * Created by Melancias on 25/02/2017.
 */
public class myGitServer {
    //TODO: mudar a introdução da password de argumento para scanner, mas não mudem entretanto pq +facil de testar assim
    public static void main(String[] args) {;
    int port=23456;
    String passwd = "";
    try{
        port=Integer.parseInt(args[0]);
        passwd=args[1];
        if (Integer.parseInt(args[0]) <= 1024){
            throw new Exception();
        }
    }
    catch(Exception e){
        System.err.println("Error: Port must be an integer above 1024");
        System.exit(-1);
    }
        System.out.println("Server started");
        myGitServer server = new myGitServer();
        server.startServer(port,passwd);

    }

     public void startServer (int port,String passwd){
//     System.setProperty("javax.net.ssl.keyStore", "myServer.keyStore");
//     System.setProperty("javax.net.ssl.keyStorePassword", "123456");
     System.setProperty("javax.net.ssl.keyStore", "myServer.jks");
     System.setProperty("javax.net.ssl.keyStorePassword", "batatas");
     ServerSocketFactory sf = SSLServerSocketFactory.getDefault( );
         ServerSocket sSoc = null;

        try {
            sSoc = sf.createServerSocket(port);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        while(true) {
            try {
                Socket inSoc = sSoc.accept();
                ServerThread newServerThread = new ServerThread(inSoc,passwd);
                newServerThread.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
