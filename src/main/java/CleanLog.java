import java.nio.file.Paths;
import java.util.HashMap;


public class CleanLog {

    public static void main(String[] args) {
        System.setProperty("line.separator", "\n");
        HashMap<String, Object> filenames = null;
        FileUtils.cleanLog("D:\\Etix-Repository\\etix\\ticketing");
        //FileUtils.checkFiles(Paths.get("D:\\Etix-Repository\\etix\\common\\src\\main\\java\\com\\etix\\log\\RequestInfo.java"));

    }
}
