package com.ljh.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.springbootinit.mapper.DailyCheckInMapper;
import com.ljh.springbootinit.model.entity.DailyCheckIn;
import com.ljh.springbootinit.service.DailyCheckInService;
import org.springframework.stereotype.Service;

/**
* @author ljh13
* @description 针对表【daily_check_in(每日签到表)】的数据库操作Service实现
* @createDate 2023-12-10 13:52:36
*/
@Service
public class DailyCheckInServiceImpl extends ServiceImpl<DailyCheckInMapper, DailyCheckIn>
    implements DailyCheckInService{

}




