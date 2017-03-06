import java.io.File;
import java.io.Serializable;
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
    private boolean autoGenerated=false;

    public DataManifest(String user,String repo,String action){
        this.user=user;
        this.repo=repo;
        this.action=action;
    }


    public void addFileManifestManual(String file){
        dataManifest.put(file,new File("./"+file).lastModified());
    }

    public void autoGenerateManifest(String path) throws Exception {

        File[] files = new File(path).listFiles();

        for (File file : files) {
            if (file.isFile()) {
                dataManifest.put(file.getName(),file.lastModified());
            }
            else{
                System.out.println("Encontrei uma pasta, ignorando");
            }
        }
        autoGenerated=true;
    }

    public Long getModifiedData(String file){return dataManifest.get(file);}

    public static ArrayList<String> processManifest(DataManifest data){
        ArrayList<String> requestedFiles = new ArrayList<String>();
        if(data.action.equals("push")) {
            //Se push e dir
            if (data.autoGenerated) {
                File[] files = new File("./" + data.repo).listFiles();
                //push e single
                if(files==null){
                    files = new File[]{new File("./" + data.repo)};
                    if(files==null){
                        requestedFiles.add(data.repo);
                        return requestedFiles;
                    }
                }
                for (File file : files) {
                    if (file.isFile() & file.lastModified() < data.getModifiedData(file.getName())) {
                        requestedFiles.add(file.getName());
                    } else {
                        System.out.println("Ficheiro " + file.getName() + " não modificado");
                    }
                }
                System.out.println(requestedFiles);
                return requestedFiles;
            }
        }
        else if(data.action.equals("pull")){
            //pull e dir
            if(data.autoGenerated){
                File[] files = new File("./" + data.repo + "/").listFiles();
                for (File file : files) {
                    if (file.isFile() & file.lastModified() > data.getModifiedData(file.getName())) {
                        requestedFiles.add(file.getName());
                        System.out.println("Ficheiro " + file.getName() + " tem nova versao");
                    } else {
                        System.out.println("Ficheiro " + file.getName() + " não modificado");
                    }
                }
                System.out.println(requestedFiles);
                return requestedFiles;
            }
            //pull e single
            else{
                return null;
            }

        }
        return null;
    }

}
