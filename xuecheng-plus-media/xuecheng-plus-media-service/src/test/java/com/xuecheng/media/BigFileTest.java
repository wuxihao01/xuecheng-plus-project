package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by wxh
 * Date 2023/5/25 15:20
 * Description测试大文件上传方法
 */
public class BigFileTest {

    //文件分块功能测试
    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("D:\\upload\\test.mp4");

        String chunkFilePath="D:\\upload\\chunk\\";
        //1MB
        int chunkSize=1024*1024*1;
        //分块文件个数
        int chunkNum= (int) Math.ceil(sourceFile.length()*1.0/chunkSize);

        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
        byte[] bytes=new byte[1024];
        for(int i=0;i<chunkNum;i++){
            File chunkFile = new File(chunkFilePath + i);
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len=-1;
            while((len=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
                if(chunkFile.length()>=chunkSize){
                    break;
                }
            }
            raf_rw.close();

        }
        raf_r.close();
    }


    //将分块文件合并功能
    @Test
    public void testMerge() throws IOException {
        File chunkFolder = new File("D:\\upload\\chunk\\");
        File sourceFile = new File("D:\\upload\\test.mp4");
        File mergeFile = new File("D:\\upload\\test_2.mp4");

        //取出所有分块文件
        File[] files = chunkFolder.listFiles();
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        byte[] bytes=new byte[1024];
        for(File file:fileList){
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len=-1;
            while((len=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
            }
            raf_r.close();
        }

        raf_rw.close();

        //对合并的文件进行校验
        FileInputStream inputStream_merge = new FileInputStream(mergeFile);
        FileInputStream inputStream_source = new FileInputStream(sourceFile);
        String md5_merge = DigestUtils.md5Hex(inputStream_merge);
        String md5_source = DigestUtils.md5Hex(inputStream_source);
        if(md5_merge.equals(md5_source)){
            System.out.println("文件合并成功");
        }
    }
}
