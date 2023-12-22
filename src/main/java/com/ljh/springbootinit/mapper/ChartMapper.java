package com.ljh.springbootinit.mapper;

import com.ljh.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author ljh
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2023-10-30 12:31:41
* @Entity com.ljh.springbootinit.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

    List<Map<String,Object>> queryChartData(String querySql);
}




