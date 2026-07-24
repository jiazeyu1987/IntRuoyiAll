package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Clears permission-related caches on startup so direct SQL role/menu changes
 * are visible after the next backend restart.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheStartupRefreshRunner implements ApplicationRunner {

    private static final List<String> CACHE_NAMES = List.of(
            RedisKeyConstants.ROLE,
            RedisKeyConstants.USER_ROLE_ID_LIST,
            RedisKeyConstants.MENU_ROLE_ID_LIST,
            RedisKeyConstants.PERMISSION_MENU_ID_LIST
    );

    private final CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        CACHE_NAMES.forEach(this::clearCacheIfPresent);
        log.info("[run][启动阶段已清理权限缓存: {}]", CACHE_NAMES);
    }

    private void clearCacheIfPresent(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.debug("[clearCacheIfPresent][缓存未注册，跳过: {}]", cacheName);
            return;
        }
        cache.clear();
    }

}
