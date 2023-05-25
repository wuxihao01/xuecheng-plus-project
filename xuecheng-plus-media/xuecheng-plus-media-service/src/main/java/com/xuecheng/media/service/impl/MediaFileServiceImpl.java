package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

 @Autowired
 MediaFilesMapper mediaFilesMapper;
 @Autowired
 MinioClient minioClient;

 @Autowired
 MediaFileService currentProxy;

 //普通文件桶
 @Value("${minio.bucket.files}")
 private String bucket_mediafiles;

 @Value("${minio.bucket.videofiles}")
 private String bucket_video;


 //获取文件默认存储目录路径 年/月/日
 private String getDefaultFolderPath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/") + "/";
  return folder;
 }

 //获取文件的md5
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
   e.printStackTrace();
   return null;
  }
 }


 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }

 //根据拓展名获取mimeType
 private String getMimeType(String extension) {
  if (extension == null) {
   extension = "";
  }
  //根据扩展名取出mimeType
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
  if (extensionMatch != null) {
   mimeType = extensionMatch.getMimeType();
  }
  return mimeType;
 }


 /**
  * @param localFilePath 文件地址
  * @param bucket        桶
  * @param objectName    对象名称
  * @description 将文件写入minIO
  */

 public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {
  UploadObjectArgs testBucket = null;
  try {
   testBucket = UploadObjectArgs.builder()
           .bucket(bucket)
           .filename(localFilePath)
           .contentType(mimeType)
           .object(objectName)
           .build();
   minioClient.uploadObject(testBucket);
   log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
   System.out.println("上传成功");
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
   XueChengPlusException.cast("上传文件到文件系统失败");
  }
  return false;
 }

 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

  String fileName = uploadFileParamsDto.getFilename();
  String extention = fileName.substring(fileName.lastIndexOf("."));
  String mimeType = getMimeType(extention);

  //日期作为子目录
  String defaultFolderPath = getDefaultFolderPath();
  //文件md5值作为文件名
  String fileMd5 = getFileMd5(new File(localFilePath));
  String objectName = defaultFolderPath + fileMd5;

  boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_mediafiles, objectName);
  if (!result) {
   XueChengPlusException.cast("上传文件失败");
  }


  //将文件信息保存到数据库
  MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
  if(mediaFiles == null){
   XueChengPlusException.cast("文件信息上传失败");
  }
  //准备返回对象
  UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);

  return uploadFileResultDto;
 }

 /**
  * @param companyId           机构id
  * @param fileMd5             文件md5值
  * @param uploadFileParamsDto 上传文件的信息
  * @param bucket              桶
  * @param objectName          对象名称
  * @return com.xuecheng.media.model.po.MediaFiles
  * @description 将文件信息添加到文件表
  */
 @Transactional
 public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {

  //从数据库查询文件
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles == null) {
   mediaFiles = new MediaFiles();
   //拷贝基本信息
   BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
   mediaFiles.setId(fileMd5);
   mediaFiles.setFileId(fileMd5);
   mediaFiles.setCompanyId(companyId);
   mediaFiles.setUrl("/" + bucket + "/" + objectName);
   mediaFiles.setBucket(bucket);
   mediaFiles.setFilePath(objectName);
   mediaFiles.setCreateDate(LocalDateTime.now());
   mediaFiles.setAuditStatus("002003");
   mediaFiles.setStatus("1");
   //保存文件信息到文件表
   int insert = mediaFilesMapper.insert(mediaFiles);
   if (insert < 0) {
    log.error("保存文件信息到数据库失败,{}",mediaFiles.toString());
    XueChengPlusException.cast("保存文件信息失败");
   }
   log.debug("保存文件信息到数据库成功,{}",mediaFiles.toString());

  }
  return mediaFiles;


 }
}