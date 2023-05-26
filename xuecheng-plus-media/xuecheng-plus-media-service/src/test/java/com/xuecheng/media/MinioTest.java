package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by wxh
 * Date 2023/5/22 15:03
 * Description 测试minio的sdk
 */
public class MinioTest {

    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test_upload() throws Exception {
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp3");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }


        UploadObjectArgs testBucket = UploadObjectArgs.builder()
                .bucket("testbucket")
                .filename("D:\\download-complete.mp3")
                .contentType(mimeType)
                .object("1.mp3")
                .build();
        minioClient.uploadObject(testBucket);
    }

    @Test
    public void test_delete() throws Exception {
        RemoveObjectArgs deleteObject = RemoveObjectArgs.builder().bucket("testbucket").object("1.mp3").build();
        minioClient.removeObject(deleteObject);
    }

    @Test
    public void test_getFile() throws Exception {
        GetObjectArgs getObject = GetObjectArgs.builder().bucket("testbucket").object("1.mp3").build();
        FilterInputStream inputStream = minioClient.getObject(getObject);
        FileOutputStream outputStream=new FileOutputStream(new File("D:\\work\\1a.mp3"));
        IOUtils.copy(inputStream,outputStream);

        //md5检验文件完整性
        String source_md5 = DigestUtils.md5Hex(new FileInputStream(new File("D:\\download-complete.mp3")));
        String local_md5 = DigestUtils.md5Hex(new FileInputStream(new File("D:\\work\\1a.mp3")));
        if(source_md5.equals(local_md5)){
            System.out.println("下载成功");
        }
    }

    @Test
    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        for(int i=0;i<=3;i++){
            UploadObjectArgs testBucket = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .filename("D:\\upload\\chunk\\"+i)
                    .object("chunk/"+i)
                    .build();
            minioClient.uploadObject(testBucket);
            System.out.println("上传分块"+i+"成功");
        }
    }

    @Test
    public void testMerge() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//        List<ComposeSource> sources = new ArrayList<>();
//        for (int i = 0; i < 16; i++) {
//            ComposeSource build = ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build();
//            sources.add(build);
//        }
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(4).map(i -> ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build()).collect(Collectors.toList());
        ComposeObjectArgs objectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sources)
                .build();
        minioClient.composeObject(objectArgs);
    }
}
