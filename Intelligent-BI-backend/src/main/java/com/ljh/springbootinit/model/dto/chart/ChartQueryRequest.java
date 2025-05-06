package com.ljh.springbootinit.model.dto.chart;

import com.ljh.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
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
     * 图表类型
     */
    private String chartType;

    /**
     * 创建人 id
     */
    private Long userId;

    /**
     * 图表标签 Json 列表
     */
    private String tags;

    /**
     * 创建人头像
     */
    private String userAvatar;

    /**
     * 状态
     */
    private String status;

    private static final long serialVersionUID = 1L;
}