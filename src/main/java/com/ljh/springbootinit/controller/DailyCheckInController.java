package com.ljh.springbootinit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljh.springbootinit.common.BaseResponse;
import com.ljh.springbootinit.common.ErrorCode;
import com.ljh.springbootinit.common.ResultUtils;
import com.ljh.springbootinit.exception.BusinessException;
import com.ljh.springbootinit.exception.ThrowUtils;
import com.ljh.springbootinit.mapper.DailyCheckInMapper;
import com.ljh.springbootinit.model.entity.DailyCheckIn;
import com.ljh.springbootinit.model.entity.User;
import com.ljh.springbootinit.model.vo.LoginUserVO;
import com.ljh.springbootinit.model.vo.UserVO;
import com.ljh.springbootinit.service.DailyCheckInService;
import com.ljh.springbootinit.service.UserService;
import com.ljh.springbootinit.utils.RedissonLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 每日签到接口
 */
@RestController
@RequestMapping("/dailyCheckIn")
@Slf4j
public class DailyCheckInController {

    @Resource
    private DailyCheckInService dailyCheckInService;

    @Resource
    private DailyCheckInMapper dailyCheckInMapper;

    @Resource
    private UserService userService;

    @Resource
    private RedissonLockUtil redissonLockUtil;

    @PostMapping("/doCheckIn")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> doDailyCheckIn(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);
        // 当前操作时的时间
        LocalDate currentDate = LocalDate.now();
        LocalDate updateTime = dailyCheckInMapper.getLatestUpdateByUserId(String.valueOf(loginUser.getId()));

        String redissonLock = ("doDailyCheckIn_" + loginUserVO.getUserAccount().intern());
        return redissonLockUtil.redissonDistributedLocks(redissonLock, () -> {
            LambdaQueryWrapper<DailyCheckIn> dailCheckInLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dailCheckInLambdaQueryWrapper.eq(DailyCheckIn::getUserId, loginUser.getId());
            ThrowUtils.throwIf(currentDate.equals(updateTime), ErrorCode.OPERATION_ERROR, "签到失败，今日已签到");
            DailyCheckIn dailyCheckIn = new DailyCheckIn();
            dailyCheckIn.setUserId(loginUserVO.getId());
            dailyCheckIn.setAddPoints(10);
            boolean dailyCheckInResult = dailyCheckInService.save(dailyCheckIn);
            boolean addBalance = userService.addBalance(loginUser.getId(), dailyCheckIn.getAddPoints());
            boolean result = dailyCheckInResult & addBalance;
            if (!result) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            return ResultUtils.success(true);
        }, "签到失败");
    }

    @PostMapping("/doCheckIn1")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> doDailyCheckIn1(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String redissonLock = ("doDailyCheckIn_" + loginUser.getUserAccount()).intern();
        return redissonLockUtil.redissonDistributedLocks(redissonLock, () -> {
            LambdaQueryWrapper<DailyCheckIn> dailyCheckInLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dailyCheckInLambdaQueryWrapper.eq(DailyCheckIn::getUserId, loginUser.getId());
            DailyCheckIn dailyCheckIn = dailyCheckInService.getOne(dailyCheckInLambdaQueryWrapper);
            if (ObjectUtils.isNotEmpty(dailyCheckIn)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "签到失败,今日已签到");
            }
            dailyCheckIn = new DailyCheckIn();
            dailyCheckIn.setUserId(loginUser.getId());
            dailyCheckIn.setAddPoints(10);
            boolean dailyCheckInResult = dailyCheckInService.save(dailyCheckIn);
            boolean addWalletBalance = userService.addBalance(loginUser.getId(), dailyCheckIn.getAddPoints());
            boolean result = dailyCheckInResult & addWalletBalance;
            if (!result) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            return ResultUtils.success(true);
        }, "签到失败");
    }



}
