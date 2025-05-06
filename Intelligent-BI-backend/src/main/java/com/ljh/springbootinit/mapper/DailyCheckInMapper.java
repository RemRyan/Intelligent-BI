package com.ljh.springbootinit.mapper;

import com.ljh.springbootinit.model.entity.DailyCheckIn;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * @author ljh13
 * @description 针对表【daily_check_in(每日签到表)】的数据库操作Mapper
 * @createDate 2023-12-10 13:52:36
 * @Entity com.ljh.springbootinit.model.entity.DailyCheckIn
 */
public interface DailyCheckInMapper extends BaseMapper<DailyCheckIn> {

    LocalDate getLatestUpdateByUserId(String userId);
}




