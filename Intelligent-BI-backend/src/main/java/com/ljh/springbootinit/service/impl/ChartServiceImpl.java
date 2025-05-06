package com.ljh.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.springbootinit.mapper.ChartMapper;
import com.ljh.springbootinit.model.entity.Chart;
import com.ljh.springbootinit.service.ChartService;
import org.springframework.stereotype.Service;

/**
* @author ljh13
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2023-10-30 12:31:41
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService {

}




