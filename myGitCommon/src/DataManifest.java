import javax.xml.crypto.Data;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Melancias on 03/03/2017.
 */

public class DataManifest implements Serializable{

    private HashMap<String,Long> dataManifest = new HashMap<String,Long>();
    public String user;
    public String repo;
    public String action;
    public boolean autoGenerated=false;

    public DataManifest(String user,String repo,String action){
        this.user=user;
        this.repo=repo;
        this.action=action;
    }


    public void addFileManifestManual(String file){
        File temp =new File(file);
        dataManifest.put(temp.getName(),temp.lastModified());
    }

    public void autoGenerateManifest(String path) throws Exception {

        File[] files = filterHistory(path);

        for (File file : files) {
            if (file.isFile()) {
                dataManifest.put(file.getName(),file.lastModified());
            }
            else{
                System.out.println("Found a directory: Ignoring...");
            }
        }
        autoGenerated=true;
    }

    public Long getModifiedData(String file){return dataManifest.get(file);}


    public static ArrayList<String> processManifest(DataManifest data) throws Exception {
        if (data.action.equals("push"))
            return processPushManifest(data);
        else{
            return processPullManifest(data);
        }

    }

    private static ArrayList<String> processPullManifest(DataManifest data) throws IOException {
        String[] structure=data.repo.split("/");
        ArrayList<String> files=new ArrayList<String>();
        File dirTest= new File(data.repo);
        if(!dirTest.exists() & data.autoGenerated==true & (structure.length<3)){
            System.out.println("First Pull: Creating local repository");
            dirTest.mkdirs();
        }
        else if (!new File(structure[0]).exists() & data.autoGenerated==false & structure.length==2 ){
            System.out.println("First Pull: Creating Repository ");
            new File(structure[0]).mkdirs();
        }
        else if (structure.length==3 && !new File(structure[0]+"/"+structure[1]).exists() & data.autoGenerated==false ) {
            System.out.println("First Pull: Creating Repository");
            new File(structure[0] + "/" + structure[1]).mkdirs();
        }

        if(data.autoGenerated==false){files=processFile(false,data,data.action);}
        else {
            files = processFolder(false,data, data.action);

        }
        return files;
    }

    private static ArrayList<String> processPushManifest(DataManifest data) throws Exception {
        String[] structure=data.repo.split("/");
        ArrayList<String> files=new ArrayList<String>();;
        File dirTest= new File(data.repo);
        if(!dirTest.exists() & data.autoGenerated==true & structure.length==3){
            System.out.println("First Push: Creating Repository " + data.repo);
            RepoManager.createRepo(data.repo,data.user);
        }
        else if(!new File(data.user+"/"+data.repo).exists() & data.autoGenerated==true & structure.length==1){
            System.out.println("First Push: Creating Repository " + data.user+"/"+data.repo);
            RepoManager.createRepo(data.user+"/"+data.repo,data.user);
        }
        else if (!new File(data.user+"/"+structure[0]).exists() & data.autoGenerated==false & structure.length==2 ){
            System.out.println("First Push: Creating Repository " + data.user+"/"+structure[0]);
            RepoManager.createRepo(data.user+"/"+structure[0],data.user);
        }

        if(data.autoGenerated==false){
            if(structure.length<3){files=processFile(true,data,data.action);}
            else{files=processFile(false,data,data.action);}
        }
        else {
            if(structure.length==1)
                files = processFolder(true,data, data.action);

            else
                files = processFolder(false,data, data.action);

        }
        return files;
    }

    private static ArrayList<String> processFolder(boolean userappend,DataManifest dataManifest, String action) throws IOException {
        ArrayList<String> files=new ArrayList<String>();;
        String repo=dataManifest.repo;
        if (userappend){
            repo=dataManifest.user+"/"+repo;
        }
        if(action.equals("push")){
        for(String s:dataManifest.dataManifest.keySet()) {
            if (!new File("./" + repo + "/" + s).exists()) {
                files.add(s);
            }
        }

        File[] localFiles = filterHistory(repo);
            for (File file : localFiles) {
                if (file.exists() & !dataManifest.dataManifest.containsKey(file.getName()) & dataManifest.autoGenerated==true){
                    try {
                        RepoManager.manageVersions(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (file.isFile() & file.lastModified() < dataManifest.getModifiedData(file.getName())) {
                    files.add(file.getName());
                    RepoManager.manageVersions(file);
                    System.out.println("File modified: " + file.getName());
                } else if (file.isFile() & file.lastModified() > dataManifest.getModifiedData(file.getName())){
                    System.out.println("Outdated local repository: Pull first do update");
                    break;
                }
                else{ System.out.println("File not modified"); }
            }
        }

        if(action.equals("pull/server")){
            for(String s:dataManifest.dataManifest.keySet()) {
                if (!new File("./" + repo + "/" + s).exists()) {
                    files.add(s);
                }
            }

            File[] localFiles = filterHistory(repo);
            for (File file : localFiles) {
                if (file.exists() & !dataManifest.dataManifest.containsKey(file.getName()) & dataManifest.autoGenerated==true){
                    try {
                        file.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if (file.isFile() & file.lastModified() > dataManifest.getModifiedData(file.getName())) {
                    files.add(file.getName());
                    System.out.println("File modified: " + file.getName());
                } else if (file.isFile() & file.lastModified() < dataManifest.getModifiedData(file.getName())){
                    System.out.println("Outdated repository: Push first do update");
                    break;
                }
                else{ System.out.println("File not modified"); }
            }
        }

        return files;
    }

    private static ArrayList<String> processFile(boolean b, DataManifest dataManifest, String action) throws IOException {

        String repo="";
        if(b){
            repo= dataManifest.user+"/"+dataManifest.repo;
        }
        else{
            repo=dataManifest.repo;
        }
        ArrayList<String> files=new ArrayList<String>();
        String filename=repo.split("/")[repo.split("/").length-1];
        if (action.equals("push")){
            File testCase=new File("./"+repo);
            if(!testCase.exists()){files.add(filename);}
            else if (pushChecker(testCase,dataManifest)){
                files.add(filename);
            }
        }
        if (action.equals("pull/server")){
            File testCase=new File("./"+repo);
            if(!testCase.exists()){files.add(filename);}
            else if (pullChecker(testCase,dataManifest)){
                files.add(filename);
            }
        }

        return files;
    }

    private static boolean pullChecker(File file, DataManifest data) {
        if (file.exists() & !data.dataManifest.containsKey(file.getName())){
            try {
                file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (file.isFile() & file.lastModified() < data.getModifiedData(file.getName())) {

            System.out.println("Outdated file: " + file.getName());
            return true;

        } else if (file.isFile() & file.lastModified() > data.getModifiedData(file.getName())){
            System.out.println("Outdated repository: Pull first to update locally");
            return false;
        }
        else{ System.out.println("File already up to date: " + file.getName()); return false;}
        return false;
    }



    public static File[] filterHistory(String pathname){

        File[] files = new File(pathname).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String tempName= pathname.getName();
                try{
                    if(tempName.matches(".*\\.[1-2]$") || tempName.matches("^\\..*")){
                        return false;}
                    else
                    {
                        return true;
                    }
                }
                catch(Exception e) {
                    return true;
                }
            }});

        return files;
    }

    public static boolean pushChecker(File file, DataManifest data) throws IOException {
        if (file.exists() & !data.dataManifest.containsKey(file.getName())){
                    try {
                        RepoManager.manageVersions(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (file.isFile() & file.lastModified() < data.getModifiedData(file.getName())) {
                    RepoManager.manageVersions(file);
                    System.out.println("Updated File: " + file.getName());
                    return true;

                } else if (file.isFile() & file.lastModified() > data.getModifiedData(file.getName())){
                    System.out.println("Outdated repository: Pull first to update locally");
                    return false;
                }
                else{ System.out.println("File already up to date: " + file.getName()); return false;}
                return false;
    }

    static void generateSignature(String path){
        try {
            FileInputStream kfile = null;
            byte[] data= Files.readAllBytes(Paths.get(path));
            kfile = new FileInputStream("cliente.jks");
            FileOutputStream fos = new FileOutputStream(path+".sig");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            KeyStore kstore = KeyStore.getInstance("JKS");
            kstore.load(kfile, "bolachas".toCharArray());
            PrivateKey myPrivateKey=(PrivateKey) kstore.getKey("cliente", "bolachas".toCharArray());
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(myPrivateKey);
            s.update(data);
            oos.writeObject(data);
            oos.writeObject(s.sign( ));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }





}

