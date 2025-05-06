package com.ljh.springbootinit.utils;

import com.ljh.springbootinit.common.ErrorCode;
import com.ljh.springbootinit.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class RedissonLockUtil {

    @Resource
    public RedissonClient redissonClient;

    /**
     * redisson分布式锁
     *
     * @param lockName 锁名称
     * @param supplier 供应商函数（Lambda表达式）
     * @param errorCode 错误代码
     * @param errorMessage 错误消息
     * @param <T>
     * @return
     */
    public <T> T redissonDistributedLocks(String lockName, Supplier<T> supplier, ErrorCode errorCode, String errorMessage) {
        RLock rLock = redissonClient.getLock(lockName);
        try {
            if (rLock.tryLock(0, -1, TimeUnit.MICROSECONDS)) {
                // 立即尝试获取锁，并且持有锁的时间为无限期，如果成功获取到锁，则调用供应商函数并返回其结果。
                return supplier.get();
            }
            throw new BusinessException(errorCode.getCode(), errorMessage);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                log.error("unLock:" + Thread.currentThread().getId());
                rLock.unlock();
            }
        }
    }

    /**
     * redisson分布式锁
     *
     * @param lockName 锁名称
     * @param supplier 供应商
     * @param errorMessage 错误消息
     * @param <T>
     * @return
     */
    public <T> T redissonDistributedLocks(String lockName, Supplier<T> supplier, String errorMessage) {
        return redissonDistributedLocks(lockName, supplier, ErrorCode.OPERATION_ERROR, errorMessage);
    }



}
