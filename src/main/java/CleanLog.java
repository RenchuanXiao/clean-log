import org.apache.commons.lang.StringUtils;

import java.util.Locale;

public class CleanLog {

    public static void main(String[] args) {
        String str = "FAFAFCATCH ()";
        String str1 = "cfaftacfh";
        Boolean flag =str.contains("catch ()");
        int i = StringUtils.indexOf(str.toLowerCase(Locale.ROOT),"catch ()");
        int b = StringUtils.indexOf("catch", str1);
         RandomAccessFileUtils.modifyFileContent("D:\\Etix-Repository\\etix\\web\\src\\main\\java\\com\\etix\\ticketing\\accountManager\\AccountManager.java","e.printStackTrace();","logger.log(Level.WARNING,\"\",e); \n");

    }
}
