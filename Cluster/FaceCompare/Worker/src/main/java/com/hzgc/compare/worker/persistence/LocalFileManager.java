package com.hzgc.compare.worker.persistence;


import com.hzgc.compare.worker.common.tuple.Triplet;
import com.hzgc.compare.worker.conf.Config;
import com.hzgc.compare.worker.persistence.task.TimeToCheckFile;
import com.hzgc.compare.worker.persistence.task.TimeToCheckTask;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class LocalFileManager implements FileManager {
    private static Logger log = Logger.getLogger(LocalFileManager.class);
    private String path; //文件保存目录
    private Long fileSize;
    private Long timeToCheckFile;
    private String work_id;
    //    private static Logger logger = Logger.getLogger(LocalFileManager.class);
    private LocalStreamCache streamCache;

    public LocalFileManager() {
        init();
    }

    public void init() {
        path = Config.WORKER_FILE_PATH;
        work_id = Config.WORKER_ID;
        timeToCheckFile = Config.WORKER_FILE_CHECK_TIME;
        fileSize = Config.WORKER_FILE_SIZE;
        streamCache = LocalStreamCache.getInstance();
    }

    public void flush() {

    }

    /*
     *文件存储，大小默认为256MB
     * 根据workID和月份进行存储
     */
    @Override
    public void flush(List<Triplet<String, String, byte[]>> buffer) {
        log.info("Flush records to Local, the num is " + buffer.size());
        Map<String , List<Triplet<String, String, String>>> temp = new Hashtable<>();
        for(Triplet<String, String, byte[]> quintuple : buffer){
            if(quintuple.getThird() == null){
                log.warn("Irregular data.");
                continue;
            }
            byte[] fea = quintuple.getThird();
            String feature = Base64.getEncoder().encodeToString(fea);
            String dateYMD = quintuple.getFirst();
            String[] strings = dateYMD.split("-");
            String dateYM = strings[0] + "-" + strings[1];
            List<Triplet<String, String, String>> list = temp.get(dateYM);
            if(list == null){
                list = new ArrayList<>();
                temp.put(dateYM, list);
            }
            list.add(new Triplet<>(dateYMD, quintuple.getSecond(), feature));
        }

        for(Map.Entry<String , List<Triplet<String, String, String>>> entry : temp.entrySet()){
            String dateYM = entry.getKey();
            List<Triplet<String, String, String>> datas = entry.getValue();
            try {
                flushForMonth(dateYM, datas);
            } catch (IOException e) {
                log.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 文件存储，大小默认为256MB
     * 根据workID和月份进行存储
     * @param month 要写的月份
     * @param datas 数据
     * @throws IOException
     */
    private void flushForMonth(String month, List<Triplet<String, String, String>>  datas) throws IOException {
        log.info("Flush data for month : " + month + ". The path is : " + path + "/" + Config.DIR_PATH_TO_USE + "/" + month);

        File workFile = new File(Config.DIR_PATH_TO_USE);

        //寻找目标月的文件夹
        File dirMonth = new File(path + "/" + Config.DIR_PATH_TO_USE + "/" + month);
        File[] ymFiles = workFile.listFiles();
        if (ymFiles != null && ymFiles.length > 0){
            for (File f : ymFiles){
                if (f.isDirectory() && f.getName().equals(month)){
                    dirMonth = f;
                }
            }
        }
        //若没有该文件夹，创建
        if(!dirMonth.exists() || !dirMonth.isDirectory()){
            boolean res = dirMonth.mkdir();
            if(!res){
                throw new IOException("创建文件夹 " + dirMonth.getAbsolutePath() + " 失败");
            }
        }
        //寻找目标文件
        File[] files = dirMonth.listFiles();
        File fileToWrite;
        Comparator<File> comparator = new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.valueOf(o1.getName().split("\\.")[0]) - Integer.valueOf(o2.getName().split("\\.")[0]);
            }
        };
        if(files == null || files.length == 0){
            fileToWrite = new File(dirMonth, 0 + ".txt");
            boolean res = fileToWrite.createNewFile();
            if(!res){
                throw new IOException("创建文件" + fileToWrite.getName()  + "失败。");
            }
        } else{
            fileToWrite = files[0];
            for(File f : files){
                if(comparator.compare(f, fileToWrite) > 0){
                    fileToWrite = f;
                }
            }
        }
        long theFileSize = fileToWrite.length();
        for(Triplet<String, String, String> data : datas) {
            BufferedWriter bufferedWriter = null;
            String info = data.getFirst() + "_" + data.getSecond() + "_" + data.getThird();
            Integer fileName = Integer.valueOf(fileToWrite.getName().split("\\.")[0]);
            //判断文件大小
            if (theFileSize + info.length() + 2 >= fileSize) {
                bufferedWriter = streamCache.getWriterStream(fileToWrite);
                bufferedWriter.flush();
                bufferedWriter.close();
                fileName = fileName + 1;
                fileToWrite = new File(dirMonth, String.valueOf(fileName) + ".txt");
                boolean res = fileToWrite.createNewFile();
                if(!res){
                    throw new IOException("创建文件" + fileToWrite.getName()  + "失败。");
                }
                theFileSize = 0L;
            }

            bufferedWriter = streamCache.getWriterStream(fileToWrite);
            bufferedWriter.write(info, 0, info.length());
            bufferedWriter.newLine();
            theFileSize += (info.length() + 2);
            if(theFileSize > 1024 * 64) {
                bufferedWriter.flush();
                theFileSize = fileToWrite.length();
            }
        }
        streamCache.getWriterStream(fileToWrite).flush();

    }

    public void checkFile() {
        new Timer().schedule(new TimeToCheckFile(), timeToCheckFile, timeToCheckFile);
    }

    public void checkTaskTodo() {
        new Timer().schedule(new TimeToCheckTask(this), 1000, 1000);
    }
}
