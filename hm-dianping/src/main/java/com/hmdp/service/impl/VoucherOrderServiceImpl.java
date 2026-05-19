package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 代金券订单服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 抢购秒杀优惠券
     * @param voucherId 优惠券Id
     * @return Result实体类
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
            // 已经结束
            return Result.fail("秒杀已经结束！");
        }
        // 4.库存判断是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }

        Long userId = UserHolder.getUser().getId();

        // 创建锁对象
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        // 获取锁
        boolean isLock = lock.tryLock(1200);
        // 判断是否获取锁成功
        if (!isLock){
            // 获取锁失败，返回错误或重试
            return Result.fail("一个人只允许下一单！");
        }
        // 获取代理对象（事务）
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }
//        synchronized (userId.toString().intern()) {
//            // 获取代理对象（事务）
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }
    }

    // 由于涉及到tb_seckill_voucher和tb_voucher_order两张表的操作，加上事务@Transactional
    // synchronized最好不要加在方法上，因为不同用户之间执行创建订单的操作不应该受到影响(具体看项目中markdown笔记)
    /**
     * 创建优惠券订单
     * @param voucherId 优惠券id
     * @return Result实体类
     */
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 5.一人一单
        Long userId = UserHolder.getUser().getId();
        // 5.1 查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2 判断是否存在
        if (count > 0){
            // 用户已经购买过了
            return Result.fail("用户已经购买过一次了！");
        }
        // 6.扣减库存
        // 乐观锁：更新时的库存值等于之前查询到的库存值，说明更新期间没有线程修改过库存值 eq("stock", voucher.getStock())
        // 缺点：只要不匹配就更新失败，导致失败率高
        // 因此逻辑上，只要库存数量大于0就可以执行扣减库存操作
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").
                eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!success){
            // 扣除失败
            return Result.fail("库存不足！");
        }
        // 7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1 订单id
        long orderId = redisIdWorker.nextId(RedisConstants.ORDER);
        voucherOrder.setId(orderId);
        // 7.2 用户id
        voucherOrder.setUserId(userId);
        // 7.3 优惠券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 8.返回订单信息
        return Result.ok(orderId);

    }
}
