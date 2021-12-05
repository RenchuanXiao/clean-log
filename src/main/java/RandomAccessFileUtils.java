import java.io.*;

public class RandomAccessFileUtils {


    /**
     * @param fileName 文件名称
     * @param oldstr   要修改的字符串
     * @param newStr   新的字符串
     * @return
     */
    //  文件修改 文件内容出现换行就会有错误
    private static boolean modifyFileContent(String path, String oldstr, String newStr) {
        ///定义一个随机访问文件类的对象
        RandomAccessFile raf = null;
        try {
            //初始化对象,以"rw"(读写方式)访问文件
            raf = new RandomAccessFile(path, "rw");
            //临时变量,存放每次读出来的文件内容
            String line = null;
            // 记住上一次的偏移量
            long lastPoint = 0;
            //循环读出文件内容
            while ((line = raf.readLine()) != null) {
                // 文件当前偏移量 返回文件记录指针的当前位置
                final long point = raf.getFilePointer();
                // 查找要替换的内容
                if (line.contains(oldstr)) {
                    //修改内容,line读出整行数据
                    String str = line.replace(oldstr, newStr);
                    //文件节点移动到文件开始
                    System.out.println(str);
                    raf.seek(lastPoint);
                    raf.writeBytes(str);
                }
                lastPoint = point;//如果文件出现换行,则修改后的节点需要移动到下一行的开头,所以此处会出现错误
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
     *文件修改
     * @param filePath 文件名称
     * @param oldstr   要修改的字符串
     * @param newStr   新的字符串
     */
    private static void autoReplace(String filePath, String oldstr, String newStr) {
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
