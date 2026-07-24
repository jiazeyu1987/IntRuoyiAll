package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionCacheStartupRefreshRunnerTest extends BaseMockitoUnitTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache roleCache;
    @Mock
    private Cache userRoleCache;
    @Mock
    private Cache menuRoleCache;

    @Test
    void run_clearsRegisteredPermissionCaches() throws Exception {
        when(cacheManager.getCache(RedisKeyConstants.ROLE)).thenReturn(roleCache);
        when(cacheManager.getCache(RedisKeyConstants.USER_ROLE_ID_LIST)).thenReturn(userRoleCache);
        when(cacheManager.getCache(RedisKeyConstants.MENU_ROLE_ID_LIST)).thenReturn(menuRoleCache);
        when(cacheManager.getCache(RedisKeyConstants.PERMISSION_MENU_ID_LIST)).thenReturn(null);
        PermissionCacheStartupRefreshRunner runner = new PermissionCacheStartupRefreshRunner(cacheManager);

        runner.run(null);

        verify(roleCache).clear();
        verify(userRoleCache).clear();
        verify(menuRoleCache).clear();
        verify(cacheManager).getCache(RedisKeyConstants.PERMISSION_MENU_ID_LIST);
    }

    @Test
    void run_skipsMissingCaches() throws Exception {
        List.of(
                RedisKeyConstants.ROLE,
                RedisKeyConstants.USER_ROLE_ID_LIST,
                RedisKeyConstants.MENU_ROLE_ID_LIST,
                RedisKeyConstants.PERMISSION_MENU_ID_LIST
        ).forEach(cacheName -> when(cacheManager.getCache(cacheName)).thenReturn(null));
        PermissionCacheStartupRefreshRunner runner = new PermissionCacheStartupRefreshRunner(cacheManager);

        runner.run(null);

        verify(roleCache, never()).clear();
        verify(userRoleCache, never()).clear();
        verify(menuRoleCache, never()).clear();
    }

}
