import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Locale;

public class RandomAccessFileUtils {

    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(FileUtils.class);

    /**
     * @param path   文件路径
     * @param oldstr 要修改的字符串
     * @param newStr 新的字符串
     * @return
     */
    //  文件修改 文件内容出现换行就会有错误
    public static boolean modifyFileContent(String path, String oldstr, String newStr) {
        ///定义一个随机访问文件类的对象
        RandomAccessFile raf = null;
        try {
            //初始化对象,以"rw"(读写方式)访问文件
            raf = new RandomAccessFile(path, "rw");
            //临时变量,存放每次读出来的文件内容
            String line = null;
            // 记住上一次的偏移量
            long lastPoint = 0;
            // 记录当前catch代码块的偏移量
            long catchPoint = 0;
            //确认当前catch是否结束
            int findCatchBlock = 1;
            boolean replaceFlag = true;
            while ((line = raf.readLine()) != null) {
                // 查找catch代码块
                if (line.contains("catch (")) {
                    while (line != null) {
                        //截取catch后的字符串
                        line = StringUtils.substring(line, line.indexOf("catch"));
                        // 文件当前偏移量 返回文件记录指针的当前位置
                        catchPoint = raf.getFilePointer();
                        if (line.contains(("{"))) {
                            findCatchBlock = findCatchBlock + 1;
                        }
                        if (line.contains(("}"))) {
                            findCatchBlock = findCatchBlock - 1;
                        }
                        if (line.contains("e.printStackTrace();")) {
                            String str = line.replace(oldstr, newStr);
                            replaceFlag = false;
                            LOGGER.info("Replace e.printStackTrace() to " + str);
                        }
                        if (line.toLowerCase(Locale.ROOT).contains("log")) {
                            replaceFlag = false;
                        }
                        if (findCatchBlock == 0) {
                            lastPoint=raf.getFilePointer();
                        }
                        if (findCatchBlock == 0 && replaceFlag) {
                            lastPoint = raf.getFilePointer();
                            raf.seek(catchPoint);
                            raf.readLine();
                            raf.writeBytes("logger.log(Level.WARNING,\"\",e)");
                            //修改内容,line读出整行数据
                        }
                        line=raf.readLine();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 文件修改
     *
     * @param filePath 文件名称
     * @param oldstr   要修改的字符串
     * @param newStr   新的字符串
     */
    public static void autoReplace(String filePath, String oldstr, String newStr) {
        //创建文件
        File file = new File(filePath);
        //记录文件长度
        Long fileLength = file.length();
        //记录读出来的文件的内容
        byte[] fileContext = new byte[fileLength.intValue()];
        FileInputStream in = null;
        PrintWriter out = null;
        try {
            in = new FileInputStream(filePath);
            //读出文件全部内容(内容和文件中的格式一致,含换行)
            in.read(fileContext);
            // 避免出现中文乱码
            String str = new String(fileContext, "utf-8");
            //修改对应字符串内容
            str = str.replace(oldstr, newStr);
            //再把新的内容写入文件
            out = new PrintWriter(filePath);
            out.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.flush();
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
