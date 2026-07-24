package cn.iocoder.yudao.module.mes.dal.redis.md.autocode;

import cn.iocoder.yudao.module.mes.dal.redis.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * MES 编码规则的 Redis DAO
 *
 * @author 瑛泰源码
 */
@Repository
public class MesMdAutoCodeRedisDAO {

    private static final RedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            local floor = tonumber(ARGV[1])
            local step = tonumber(ARGV[2])
            local ttl = tonumber(ARGV[3])
            local nextValue

            if current == false then
                nextValue = floor
                redis.call('SET', KEYS[1], nextValue)
            else
                local currentNumber = tonumber(current)
                if currentNumber == nil then
                    return redis.error_reply('AUTO_CODE_COUNTER_NOT_NUMERIC')
                end
                if currentNumber < floor then
                    nextValue = floor
                    redis.call('SET', KEYS[1], nextValue)
                else
                    nextValue = redis.call('INCRBY', KEYS[1], step)
                end
            end

            if ttl ~= nil and ttl > 0 then
                redis.call('PEXPIRE', KEYS[1], ttl)
            end
            return nextValue
            """, Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 递增序号（带初始值和循环）
     *
     * @param keySuffix key 后缀（不包含 prefix）
     * @param duration 过期时间
     * @param step 步长
     * @param start 起始值（仅在 key 不存在时使用）
     * @return 递增后的值
     */
    public Long increment(String keySuffix, Duration duration, Supplier<Long> initialValueSupplier, Integer step) {
        Assert.notNull(keySuffix, "AUTO_CODE_COUNTER_KEY_SUFFIX_REQUIRED");
        Assert.notNull(initialValueSupplier, "AUTO_CODE_COUNTER_INITIAL_VALUE_SUPPLIER_REQUIRED");
        Assert.notNull(step, "AUTO_CODE_COUNTER_STEP_REQUIRED");
        Assert.isTrue(step > 0, "AUTO_CODE_COUNTER_STEP_MUST_BE_POSITIVE");
        if (duration != null) {
            Assert.isTrue(!duration.isZero() && !duration.isNegative(), "AUTO_CODE_COUNTER_TTL_MUST_BE_POSITIVE");
        }

        String key = RedisKeyConstants.AUTO_CODE + keySuffix;
        Long floorValue = initialValueSupplier.get();
        Assert.notNull(floorValue, "AUTO_CODE_COUNTER_FLOOR_VALUE_REQUIRED");

        Long value = stringRedisTemplate.execute(INCREMENT_SCRIPT, List.of(key), String.valueOf(floorValue),
                String.valueOf(step), String.valueOf(duration == null ? 0 : duration.toMillis()));
        Assert.notNull(value, "AUTO_CODE_COUNTER_INCREMENT_FAILED");
        return value;
    }

}
