
import org.omg.CORBA.TRANSACTION_MODE;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.toString());


    public static HashMap<String, Object> cleanLog(String folderPath) {
        HashMap<String, Object> map = new HashMap<>();
        File f = new File(folderPath);
        if (f.exists()) {
            File fa[] = f.listFiles();
            for (int i = 0; i < fa.length; i++) {
                File fs = fa[i];
                if (fs.isDirectory()) {
                    cleanLog(fs.getAbsolutePath());
                } else {
                    if (FileUtils.getFileExt(fs.getName()).equals("java")) {
                        checkFiles(Paths.get(fs.getAbsolutePath()));
                    }
                }
            }
        }
        return map;
    }


    public static void checkFiles(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            File file = new File(path.toString());
            FileReader fileReader = new FileReader(file);
            LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
            String line = null;
            String catchLine = null;
            String exceptionType = null;
            String exceptionName = null;
            Boolean editFlag = false;
            Boolean julFlag = false;
            while ((line = lineNumberReader.readLine()) != null) {
                if (line.contains("logger") || line.contains("LOGGER") || line.contains("logger")) {
                    julFlag = true;
                }
                if (line.contains("catch (") | line.contains("catch(")) {
                    catchLine = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
                    try {
                        exceptionType = catchLine.substring(0, catchLine.lastIndexOf(" "));
                        exceptionName = catchLine.substring(catchLine.lastIndexOf(" "));
                    } catch (Exception e) {
                        LOGGER.info("          " + file.getName() + "         " + lineNumberReader.getLineNumber() + "行读取失败");
                    }
                    if (exceptionType.equals("Throwable")) {
                        exceptionType = "Exception";
                    }
                }
                if (line.contains((exceptionName + ".printStackTrace").trim())) {
                    String resultString;
                    if (julFlag) {
                        resultString = line.trim().replace((exceptionName + ".printStackTrace()").trim(), "logger.log(Level.WARNING," + "\"" + exceptionType + "\"" + "," + exceptionName + ")".trim());
                    } else {
                        resultString = line.trim().replace((exceptionName + ".printStackTrace()").trim(), "LOGGER.log(Level.WARN," + "\"" + exceptionType + "\"" + "," + exceptionName + ")".trim());
                    }
                    lines.set(lineNumberReader.getLineNumber() - 1, "\t\t\t" + resultString);
                    if (editFlag == false) {
                        editFlag = true;
                    }
                }
            }
            lines = editFiles(path, lines, editFlag, julFlag);
            if (editFlag == true) {
                Files.write(path, lines, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
    }

    public static List<String> editFiles(Path path, List<String> lines, Boolean editFlag, Boolean julFlag) {
        try {
            File file = new File(path.toString());
            FileReader fileReader = new FileReader(file);
            LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
            int importIndex = 0;
            int publicClassIndex = 0;
            Boolean improtIndextFlag = false;
            String line = null;
            while ((line = lineNumberReader.readLine()) != null) {
                if (line.contains("import ")) {
                    importIndex = lineNumberReader.getLineNumber();
                }
                if (line.contains("import java.util.logging.Level;".trim()) || line.contains("import java.util.logging.*;")) {
                    improtIndextFlag = true;
                }
                if (line.contains("public abstract class".trim()) || line.contains("public class".trim())) {
                    publicClassIndex = lineNumberReader.getLineNumber();
                }
            }
            if (editFlag && julFlag && improtIndextFlag) {
                lines.add(importIndex, "import java.util.logging.Level;");
            }
            if (editFlag && !julFlag && !improtIndextFlag) {
                lines.add(importIndex, "import org.apache.logging.log4j.Logger;");
                lines.add(importIndex, "import org.apache.logging.log4j.LogManager;");
                lines.add(importIndex, "import org.apache.logging.log4j.Level;");
                String checkline = lines.get(publicClassIndex+2);
                if (checkline.contains("}")) {
                    lines.add(publicClassIndex + 2, "private static final Logger LOGGER = LogManager.getLogger();");
                } else {
                    lines.add(publicClassIndex + 3, "private static final Logger LOGGER = LogManager.getLogger();");
                }
            }
            return lines;
        } catch (Exception e) {
            LOGGER.info("edit Files Failed" + path.toString());
            return null;
        }


    }

    public static String getFileExt(String fileName) {
        int point = fileName.lastIndexOf('.');
        int length = fileName.length();
        if (point == -1 || point == length - 1) {
            return "";
        } else {
            return fileName.substring(point + 1, length);
        }
    }


}
