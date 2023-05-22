package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * Created by wxh
 * Date 2023/5/11 19:04
 * Description 课程计划查询相关接口
 */
public interface TeachplanService {
    /**
     *
     * @param courseId  课程id
     * @return List<TeachplanDto>
     */
    public List<TeachplanDto> findTeachplanTree(long courseId);

    /**
     *新增/修改/保存课程计划
     * @param saveTeachplanDto 课程计划信息
     */
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);
}
