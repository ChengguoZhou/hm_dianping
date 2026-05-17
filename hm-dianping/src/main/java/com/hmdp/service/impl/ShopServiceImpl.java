package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 根据id查询店铺
     * @param id 店铺id
     * @return Result实体类，封装了查询状态和Shop实体类
     */
    @Override
    public Result queryShopById(Long id) {
        // 缓存穿透
        // lambda表达式简写 id2 -> getById(id2)为this::getByid
//         Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById,
//                CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, LOCK_SHOP_KEY, id,
                Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null){
            return Result.fail("店铺不存在！");
        }
        // 返回
        return Result.ok(shop);

    }



    /**
     * 避免缓存击穿的测试（用JMeter进行测试）
     * @param id 店铺id
     * @return Shop实体类
     */
    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断Redis查询到的商铺信息是否存在
        if (StrUtil.isNotBlank(shopJson)){
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断Redis命中的商铺信息是否是空值/空字符串
        if (shopJson != null){
            // Redis命中的商铺信息是空字符串（避免缓存穿透手动添加的），返回一个null
            return null;
        }
        // 4.实现缓存重建
        // 4.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = cacheClient.tryLock(lockKey);
            // 4.2 判断是否获取成功
            if (!isLock){
                // 4.3 如果失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            // 4.4 如果获取锁成功，则根据id查询数据库
            shop = getById(id);
            // 模拟重建的延迟
            Thread.sleep(200);
            // 5.查询数据库不存在，保存空字符串到Redis并返回错误
            if (shop == null){
                // 将空值写入Redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 6.查询数据库存在，写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 7.释放互斥锁
        cacheClient.unlock(lockKey);
        // 8.返回
        return shop;
    }

    /**
     * 避免缓存穿透的查询
     * @param id
     * @return
     */
    public Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断Redis查询到的商铺信息是否存在
        if (StrUtil.isNotBlank(shopJson)){
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断Redis命中的商铺信息是否是空值/空字符串
        if (shopJson != null){
            // Redis命中的商铺信息是空字符串（避免缓存穿透手动添加的），返回一个null
            return null;
        }
        // 4.Redis未命中，根据id查询数据库
        Shop shop = getById(id);
        // 5.查询数据库不存在，保存空字符串到Redis并返回错误
        if (shop == null){
            // 将空值写入Redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 6.查询数据库存在，写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7.返回
        return shop;
    }


    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺id不能为空！");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }



    /**
     * 采用逻辑有效期方式解决缓存击穿问题
     * 通过Redis保存店铺信息
     * @param id 店铺id
     * @param expireSeconds 逻辑有效期（秒）
     */
    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        // 1.查询店铺数据
        Shop shop = getById(id);
        // 模拟复杂业务延迟的情况
        Thread.sleep(200);
        // 2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusMinutes(expireSeconds));
        // 3.写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }
}
