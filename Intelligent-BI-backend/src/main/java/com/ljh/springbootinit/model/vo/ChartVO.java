package com.ljh.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ChartVO implements Serializable {

    private Long id;

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chatType;

    /**
     * 生成的图表数据
     */
    private String genChart;

    /**
     * 图表标签 Json 列表
     */
    private String tags;

    /**
     * 生成的分析结论
     */
    private String genResult;
    /**
     * 执行状态
     */
    private String status;
    /**
     * 执行信息
     */
    private String execMessage;

    /**
     * 创建人头像
     */
    private String userAvatar;

    /**
     * 创建时间
     */
    private Date createTime;


    private static final long serialVersionUID = 1L;
}