package cn.iocoder.yudao.module.mes.service.pro.frontline;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class MesFrontlineRedisSessionSnapshotStore implements MesFrontlineSessionSnapshotStore {

    private static final String KEY_PREFIX = "mes:frontline:session-snapshot:";

    private final StringRedisTemplate stringRedisTemplate;

    public MesFrontlineRedisSessionSnapshotStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void save(Long tenantId, String snapshotId, String snapshotJson, Duration ttl) {
        stringRedisTemplate.opsForValue().set(buildKey(tenantId, snapshotId), snapshotJson, ttl);
    }

    @Override
    public String get(Long tenantId, String snapshotId) {
        return stringRedisTemplate.opsForValue().get(buildKey(tenantId, snapshotId));
    }

    private static String buildKey(Long tenantId, String snapshotId) {
        return KEY_PREFIX + tenantId + ":" + snapshotId;
    }

}
