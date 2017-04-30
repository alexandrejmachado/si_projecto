import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
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
    public RepoPathTypeEnum whohasit;

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
                        for (File f : new File[]{file,new File(file.getAbsolutePath()+".sig"),new File(file.getAbsolutePath()+".key.server")}) {
                            RepoManager.manageVersions(file);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (file.isFile() & file.lastModified() < dataManifest.getModifiedData(file.getName())) {
                    files.add(file.getName());
                    for (File f : new File[]{file,new File(file.getAbsolutePath()+".sig"),new File(file.getAbsolutePath()+".key.server")}) {
                        RepoManager.manageVersions(file);
                    }
                    System.out.println("File modified: " + file.getName());
                } else if (file.isFile() & file.lastModified() > dataManifest.getModifiedData(file.getName())){
                    System.out.println("Outdated local repository: Pull first do update");
//                    break;
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
                else if (file.isFile() & (file.lastModified() < dataManifest.getModifiedData(file.getName()))) {
                    files.add(file.getName());
                    System.out.println("File modified: " + file.getName());
                } else if (file.isFile() & (file.lastModified() > dataManifest.getModifiedData(file.getName()))){
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
                    if(tempName.matches(".*\\.[1-2]$") || tempName.matches("^\\..*") || tempName.matches(".*\\.sig$") || tempName.matches(".*\\.key.server$")){
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

    static File generateSignature(String path,String username){
        try {
            FileInputStream kfile = null;
            byte[] data= Files.readAllBytes(Paths.get(path));
            kfile = new FileInputStream("cliente.jks");
            FileOutputStream fos = new FileOutputStream(path+".sig");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            KeyStore kstore = KeyStore.getInstance("JKS");
            kstore.load(kfile, "bolachas".toCharArray());
            PrivateKey myPrivateKey=(PrivateKey) kstore.getKey(username, "bolachas".toCharArray());
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(myPrivateKey);
            s.update(data);
            oos.writeObject(s.sign( ));
            fos.close();
            return new File(path+".sig");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void CipherKey(String fileName) {
        try {

            // TODO Muda isto!
            FileInputStream fileInputStream = new FileInputStream("servidor.jks");
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fileInputStream, "bolachas".toCharArray());
            Certificate certificate = keyStore.getCertificate("servidor");

            Cipher ckey = Cipher.getInstance("RSA");
            ckey.init(Cipher.WRAP_MODE, certificate);
            FileInputStream fisKey = new FileInputStream(fileName);
            ObjectInputStream oisKey = new ObjectInputStream(fisKey);

            byte[] key = (byte[]) oisKey.readObject();

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            byte[] cipheredKey = ckey.wrap(keySpec);

            FileOutputStream kos = new FileOutputStream(fileName + ".server");
            //ObjectOutputStream oos = new ObjectOutputStream(kos);
            kos.write(cipheredKey);
            kos.close();
            new File(fileName).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static boolean checkSignature(String path,String username){
        boolean answer=false;
        try {
            FileInputStream kfile = new FileInputStream("cliente.jks"); //keystore
            KeyStore kstore = KeyStore.getInstance("JKS");
            kstore.load(kfile, "bolachas".toCharArray()); //password
            Certificate cert = kstore.getCertificate(username);
            FileInputStream fis = new FileInputStream(path+".sig");
            ObjectInputStream ois = new ObjectInputStream(fis);
            byte[] data= Files.readAllBytes(Paths.get(path)); //não fiz verificação de erro
            byte signature[] = (byte[]) ois.readObject(); //não fiz verificação de erro
            Certificate c = cert; //obtém um certificado de alguma forma (ex., de um ficheiro)
            PublicKey pk = c.getPublicKey();
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(pk);
            s.update(data);
            answer=s.verify(signature);
            if (answer)
                System.out.println("Message is valid");
            else
                System.out.println("Message was corrupted");

            fis.close();
            new File(path+".sig").delete();
            return answer;
        }
        catch(Exception e){

            e.printStackTrace();
        }
        return false;
    }


    public static File decipherKey(String fileName) {

        try {
            File file = new File(fileName + ".key.server");
            FileInputStream kos = new FileInputStream(file);
            //ObjectInputStream  bos = new ObjectInputStream(kos);

            byte[] chaveCifrada = new byte[256];
            int i = kos.read(chaveCifrada);

            // TODO Muda isto
            FileInputStream fileInputStream = new FileInputStream("servidor.jks");
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fileInputStream, "bolachas".toCharArray());
            PrivateKey privateKey = (PrivateKey) keyStore.getKey("servidor", "bolachas".toCharArray());

            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.UNWRAP_MODE, privateKey );


            Key decipheredKey = c.unwrap(chaveCifrada, "AES", Cipher.SECRET_KEY);

            FileOutputStream fos   = new FileOutputStream(fileName + ".key");
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.write(decipheredKey.getEncoded());

            oos.flush();
            oos.close();
            fos.flush();
            fos.close();
            kos.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return new File(fileName + ".key");
    }

    public static File generateCipherKey(String fileName) throws NoSuchAlgorithmException, IOException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        SecretKey key = kg.generateKey();

        FileOutputStream fos = new FileOutputStream(fileName + ".key");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        byte[] keyBytes = key.getEncoded();

        oos.writeObject(keyBytes);

        oos.flush();
        oos.close();
        fos.flush();
        fos.close();

        return new File(fileName + ".key");
    }

    public static File cipherFile(File secretKeyFile, File fileToCipher) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, ClassNotFoundException, InvalidKeyException {

        FileInputStream fisKey;
        ObjectInputStream oisKey;
        FileInputStream fisFile;
        FileOutputStream fos;
        CipherOutputStream cos;

        fisKey = new FileInputStream(secretKeyFile);
        oisKey = new ObjectInputStream(fisKey);

        fisFile = new FileInputStream(fileToCipher);

        byte[] key = (byte[]) oisKey.readObject();

        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, keySpec);


        fos = new FileOutputStream(fileToCipher.getName()+".cif");
        cos = new CipherOutputStream(fos, c);

        byte[] b = new byte[16];
        int i = fisFile.read(b);
        while (i != -1) {
            cos.write(b, 0, i);
            i = fisFile.read(b);
        }
        cos.close();

        return new File(fileToCipher.getName()+".cif");
    }

    public static File decipherFile(File secretKeyFile, File fileToDecipher, long date) throws Exception {
        try {
            FileInputStream fis = new FileInputStream(secretKeyFile);

            byte[] keyEncoded = new byte[16];

            ObjectInputStream ois = new ObjectInputStream(fis);

            ois.read(keyEncoded);
            ois.close();

            SecretKeySpec keySpec2 = new SecretKeySpec(keyEncoded, "AES");
            //SecretKeySpec é subclasse de secretKey
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, keySpec2);
            FileInputStream fis2;
            FileOutputStream fos;
            fis2 = new FileInputStream(fileToDecipher);
            fos = new FileOutputStream(fileToDecipher.getAbsoluteFile() + ".temp");
            CipherInputStream cis = new CipherInputStream(fis2, c);
            byte[] b = new byte[16];
            int i = cis.read(b);
            while (i != -1) {
                fos.write(b, 0, i);
                i = cis.read(b);
            }
            fos.close();
            cis.close();
            String namefile = fileToDecipher.getName();
            fileToDecipher.delete();
            secretKeyFile.delete();
            new File(fileToDecipher.getAbsolutePath() + ".temp").renameTo(new File(fileToDecipher.getAbsolutePath()));
            new File(fileToDecipher.getAbsolutePath()).setLastModified(date);
            if(new File(fileToDecipher.getAbsolutePath()+".old").exists()){
                new File(fileToDecipher.getAbsolutePath()+".old").delete();
            }

        }
        catch(Exception e)
        {
            if(new File(fileToDecipher.getAbsolutePath()+".old").exists()){
                new File(fileToDecipher.getAbsolutePath()+".old").renameTo(new File(fileToDecipher.getAbsolutePath()));
            }
            else{
                new File(fileToDecipher.getAbsolutePath()).delete();
            }
            throw new Exception("Ciphered file is corrupted or was manipulated");
        }
        return null;
    }

    public static void cleanup(String repo) {
        File clean = new File(repo);
        if (clean.isFile()) {
            for (String s : new String[]{".sig", ".key", ".temp"})
                new File(clean.getAbsolutePath() + s).delete();
        }
        else {
            for (File f : new File(repo).listFiles()) {
                if (f.getName().endsWith(".sig") || f.getName().endsWith(".key") || f.getName().endsWith(".temp")) {
                    f.delete();
                }
            }
        }
    }

}