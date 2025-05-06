package com.ljh.springbootinit.aop;

import com.ljh.springbootinit.common.ErrorCode;
import com.ljh.springbootinit.exception.ThrowUtils;
import com.ljh.springbootinit.model.entity.User;
import com.ljh.springbootinit.service.UserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class ReduceBalanceAOP {

    @Resource
    private UserService userService;

    private static final int REDUCE_BALANCE = 1;

    @Pointcut("execution(* com.ljh.springbootinit.controller.ChartController.genChartByAi(..)) ||" +
            "execution(* com.ljh.springbootinit.controller.ChartController.genChartByAiAsync(..)) ||" +
            "execution(* com.ljh.springbootinit.controller.ChartController.genChartByAiAsyncMq(..))")
    public void reduceBalancePointcut() {}

    @Before("reduceBalancePointcut()")
    public void reduceBalance(JoinPoint joinPoint) {
        // 获取目标方法的参数
        Object[] args = joinPoint.getArgs();
        // 执行积分减少逻辑，假设第一个参数是用户ID，第二个参数是需要减少的积分数
        HttpServletRequest request = (HttpServletRequest) args[2];
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        Integer reduceBalance = REDUCE_BALANCE;
        boolean result = userService.reduceBalance(userId, reduceBalance);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "积分余额不足");
    }

}
