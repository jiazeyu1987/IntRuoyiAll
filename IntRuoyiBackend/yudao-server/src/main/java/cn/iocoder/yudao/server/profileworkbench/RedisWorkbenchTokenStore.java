package cn.iocoder.yudao.server.profileworkbench;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
@Component
public class RedisWorkbenchTokenStore implements WorkbenchTokenStore {
    private final StringRedisTemplate redis;
    public RedisWorkbenchTokenStore(StringRedisTemplate redis) { this.redis = redis; }
    private static String key(String token) { return "system:workbench:query:v3:" + token; }
    public boolean create(String token, String record, Duration lifetime) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(token), record, lifetime));
    }
    public String read(String token) { return redis.opsForValue().get(key(token)); }
}
