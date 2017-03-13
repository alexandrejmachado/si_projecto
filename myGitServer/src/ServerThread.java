import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Melancias on 03/03/2017.
 */
class ServerThread extends Thread {

    private Socket socket = null;
    private AuthManager auth= new AuthManager();
    private DataTransferUtils util;
    ServerThread(Socket inSoc) throws IOException {
        socket = inSoc;
        util = new DataTransferUtils(socket);
        System.out.println("thread do server para cada cliente");

    }

    public void run(){
        try {
            //noinspection Since15
            String[] crends=util.getCredentials();
            String user = crends[0];
            String passwd = crends[1];
            System.out.println("thread: depois de receber a password e o user");
            if (auth.authenticate(user,passwd)){
                util.sendHandshake();
                System.out.println("A ir buscar manifest");
                DataManifest manifest = util.getManifest();
                System.out.println("done");
                ArrayList<String> c= DataManifest.processManifest(manifest);
                util.sendRequestList(c);
                if (manifest.action.equals("push")){
                    for(String s : c){
                        System.out.println("A receber ficheiros LOLOLOL");
                        util.pullFile(new File(manifest.repo+"/"+s));
                    }
                }

            }
            else {
                util.sendCloseHandshake();
            }

            //este codigo apenas exemplifica a comunicacao entre o cliente e o servidor
            //nao faz qualquer tipo de autenticacao

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }








}
