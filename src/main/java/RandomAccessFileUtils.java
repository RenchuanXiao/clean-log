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
            replace(raf);
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
     *
     */
    public static void replace(RandomAccessFile raf) throws IOException {
        //临时变量,存放每次读出来的文件内容
        String line = null;
        raf.seek(42);
        raf.writeBytes("Adadad\n");


        //外层循环
     /*   while ((line = raf.readLine()) != null) {

            //findCatch(line, raf);
        }*/
    }

    public static void findCatch(String line, RandomAccessFile raf) throws IOException {

        // 记住上一次的偏移量
        long lastPoint = 0;
        //确认当前catch是否结束
        int findCatchBlock = 0;
        //写文件时的偏移量
        long writePoint = 0;

        //找到catch 代码块
        if (line.contains("catch (") | line.contains("catch(")) {
            lastPoint = raf.getFilePointer();
            //截取catch后的字符串
            String catchLine = StringUtils.substring(line, line.indexOf("catch"));
            line = catchLine;
            String backLine = "";
            while (true) {
                writePoint = raf.getFilePointer();
                System.out.println(line + backLine);
                if (line.contains("{")) {
                    findCatchBlock = findCatchBlock + 1;
                }
                if (line.contains("}")) {
                    findCatchBlock = findCatchBlock - 1;
                }
                if (line.contains("e.printStackTrace();")) {
                    String newline = line.replace("e.printStackTrace();", "afawfawf");
                    raf.writeBytes(newline);
                }
                if (findCatchBlock == 0 && !(catchLine.equals(line))) {
                    raf.seek(lastPoint);
                    break;
                } else {
                    if (backLine.contains("{")) {
                        findCatchBlock = findCatchBlock + 1;
                    }
                }
                backLine = "";
                line = raf.readLine();
                if (line.contains("catch (") | line.contains("catch(")) {
                    backLine = StringUtils.substring(line, line.indexOf("catch"));
                    line = StringUtils.substring(line, 0, line.indexOf("catch"));
                }
            }
        }
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
