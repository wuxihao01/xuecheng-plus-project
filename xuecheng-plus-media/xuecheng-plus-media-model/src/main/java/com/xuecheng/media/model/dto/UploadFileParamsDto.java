package com.xuecheng.media.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * Created by wxh
 * Date 2023/5/24 16:00
 * Description 文件信息
 */
@Data
@ToString
public class UploadFileParamsDto {

    /**
     * 文件名称
     */
    private String filename;


    /**
     * 文件类型（文档，音频，视频）
     */
    private String fileType;
    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 标签
     */
    private String tags;

    /**
     * 上传人
     */
    private String username;

    /**
     * 备注
     */
    private String remark;



}

