package top.sharehome.springbootinittemplate.utils.redisson.rateLimit;

import lombok.AllArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import top.sharehome.springbootinittemplate.common.base.ReturnCode;
import top.sharehome.springbootinittemplate.config.bean.SpringContextHolder;
import top.sharehome.springbootinittemplate.config.redisson.condition.RedissonCondition;
import top.sharehome.springbootinittemplate.config.redisson.properties.RedissonProperties;
import top.sharehome.springbootinittemplate.exception.customize.CustomizeReturnException;
import top.sharehome.springbootinittemplate.utils.redisson.KeyPrefixConstants;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 限流工具类
 *
 * @author AntonyCheng
 */
@Component
@AllArgsConstructor
@Conditional(RedissonCondition.class)
public class RateLimitUtils {

    /**
     * 被封装的redisson客户端对象
     */
    private static final RedissonClient REDISSON_CLIENT = SpringContextHolder.getBean(RedissonClient.class);

    private final RedissonProperties redissonProperties;

    /**
     * 限流单位时间，单位：秒
     */
    private static long rate;

    @PostConstruct
    public void setRate() {
        rate = redissonProperties.getLimitRate();
    }

    /**
     * 限流单位时间内访问次数，也能看做单位时间内系统分发的令牌数
     */
    private static long rateInterval;

    @PostConstruct
    public void setRateInterval() {
        rateInterval = redissonProperties.getLimitRateInterval();
    }

    /**
     * 每个操作所要消耗的令牌数
     */
    private static long permit;

    @PostConstruct
    public void setPermit() {
        permit = redissonProperties.getLimitPermits();
    }

    /**
     * 限流操作
     *
     * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
     */
    public static void doRateLimit(String key) {
        // 创建一个名称为user_limiter的限流器，默认每秒最多访问 2 次
        RRateLimiter rateLimiter = REDISSON_CLIENT.getRateLimiter(KeyPrefixConstants.RATE_LIMIT_PREFIX + key);
        rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS);
        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(permit);
        if (!canOp) {
            throw new CustomizeReturnException(ReturnCode.TOO_MANY_REQUESTS);
        }
    }

    /**
     * 限流并且为限流键值设定过期时间
     *
     * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
     */
    public static void doRateLimitAndExpire(String key) {
        // 创建一个名称为user_limiter的限流器，每秒最多访问 2 次
        RRateLimiter rateLimiter = REDISSON_CLIENT.getRateLimiter(KeyPrefixConstants.RATE_LIMIT_PREFIX + key);
        rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS);
        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(permit);
        if (!canOp) {
            throw new CustomizeReturnException(ReturnCode.TOO_MANY_REQUESTS);
        }
        String baseKey = KeyPrefixConstants.RATE_LIMIT_PREFIX + key;
        List<String> uselessCacheKeys = new ArrayList<String>() {
            {
                add(baseKey);
                add("{" + baseKey + "}:permits");
                add("{" + baseKey + "}:value");
            }
        };
        for (String uselessCacheKey : uselessCacheKeys) {
            REDISSON_CLIENT.getBucket(uselessCacheKey).expire(Duration.ofSeconds(rate * 3));
        }
    }

}