package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Reuses the immutable registration fixture; new lane calls real Spring proxy/Gxp, not a mocked write.
 */
@Import({ DccLegacySourceNameRegistrationService.class, cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl.class, DccLegacyMaintenanceAuthAdapter.class, DccLegacyMaintenanceExecutor.class })
class DccLegacyMaintenanceKernelTest extends DccLegacySourceNameRegistrationTest {

    @Resource
    DccLegacyMaintenanceAuthAdapter adapter;

    @Resource
    DccLegacyMaintenanceExecutor executor;

    @MockBean
    DccLegacyMaintenanceGate maintenanceGate;

    @MockBean
    OAuth2TokenCommonApi tokenApi;

    @MockBean
    OAuth2AccessTokenMapper tokenRows;

    final java.util.Map<ch.qos.logback.classic.Logger, ch.qos.logback.classic.Level> previousLogs = new java.util.LinkedHashMap<>();

    @AfterEach
    void restoreLogging() {
        previousLogs.forEach(ch.qos.logback.classic.Logger::setLevel);
    }

    void actualToken() {
        var logging = org.springframework.boot.logging.LoggingSystem.get(getClass().getClassLoader());
        var prefixes = java.util.List.of("cn.iocoder.yudao.module.system.service.oauth2.", "cn.iocoder.yudao.module.system.dal.mysql.oauth2.", "cn.iocoder.yudao.module.system.dal.redis.oauth2.", "org.springframework.data.redis.", "io.lettuce.core.protocol.", "org.mybatis.spring.", "org.apache.ibatis.", "org.springframework.jdbc.core.");
        for (var configuration : logging.getLoggerConfigurations()) if (prefixes.stream().anyMatch(configuration.getName()::startsWith)) {
            var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(configuration.getName());
            previousLogs.put(logger, logger.getLevel());
            logger.setLevel(ch.qos.logback.classic.Level.OFF);
        }
        for (String name : java.util.List.of("cn.iocoder.yudao.module.system.service.oauth2", "cn.iocoder.yudao.module.system.dal.mysql.oauth2", "cn.iocoder.yudao.module.system.dal.redis.oauth2")) {
            var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(name);
            if (!previousLogs.containsKey(logger))
                previousLogs.put(logger, logger.getLevel());
            logger.setLevel(ch.qos.logback.classic.Level.OFF);
        }
        for (String name : java.util.List.of("cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenServiceImpl", "cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper", "cn.iocoder.yudao.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO", "org.springframework.data.redis", "io.lettuce.core.protocol", "org.mybatis.spring", "org.apache.ibatis", "org.springframework.jdbc.core")) {
            var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(name);
            if (!previousLogs.containsKey(logger))
                previousLogs.put(logger, logger.getLevel());
            logger.setLevel(ch.qos.logback.classic.Level.OFF);
        }
        var checked = new OAuth2AccessTokenCheckRespDTO();
        checked.setUserId(account.getId());
        checked.setTenantId(account.getTenantId());
        checked.setUserType(2);
        checked.setUserInfo(login.getInfo());
        checked.setExpiresTime(LocalDateTime.now().plusMinutes(2));
        var actual = new OAuth2AccessTokenDO();
        actual.setId(5L);
        actual.setAccessToken("OFFLINE-REAL-PORT-TOKEN");
        actual.setUserId(checked.getUserId());
        actual.setTenantId(checked.getTenantId());
        actual.setUserType(checked.getUserType());
        actual.setExpiresTime(checked.getExpiresTime());
        when(tokenApi.checkAccessToken("OFFLINE-REAL-PORT-TOKEN")).thenReturn(checked);
        when(tokenRows.selectByAccessToken("OFFLINE-REAL-PORT-TOKEN")).thenReturn(actual);
    }

    org.apache.ibatis.session.SqlSessionFactory guardedAuthReadFactory() {
        // The official token read port is isolated in this fixture; the real registration/H2/Gxp transaction is unchanged.
        var factory = mock(org.apache.ibatis.session.SqlSessionFactory.class);
        var configuration = new org.apache.ibatis.session.Configuration();
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
        configuration.addMappedStatement(new org.apache.ibatis.mapping.MappedStatement.Builder(configuration, "cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper.selectList", new org.apache.ibatis.builder.StaticSqlSource(configuration, "SELECT 1"), org.apache.ibatis.mapping.SqlCommandType.SELECT).build());
        when(factory.getConfiguration()).thenReturn(configuration);
        return factory;
    }

    @Test
    void maintenanceCallsActualRegistrationAndOfficialKernelWithDerivedTokenActor() throws Exception {
        policy();
        actualToken();
        var verifiedScope = seal();
        var input = mock(DccLegacyMaintenanceGate.Input.class);
        when(maintenanceGate.read(any(), any(), any())).thenReturn(input);
        when(maintenanceGate.verifiedScope(input)).thenReturn(verifiedScope);
        var command = new DccLegacyNameRegistrationMaintenanceCommand(maintenanceGate, adapter, executor, guardedAuthReadFactory());
        var receipt = command.execute(temp, temp.resolve("request.json"), "a".repeat(64), new ByteArrayInputStream("OFFLINE-REAL-PORT-TOKEN".getBytes(StandardCharsets.UTF_8)));
        assertNotNull(receipt.eventId());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_legacy_source_name_scope", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event", Integer.class));
        assertEquals(account.getUsername(), jdbc.queryForObject("SELECT actor_username FROM gxp_audit_event", String.class));
        verify(maintenanceGate).currentDevelopmentPolicy(input);
        assertThrows(IllegalArgumentException.class, () -> command.execute(temp, temp.resolve("request.json"), "a".repeat(64), new ByteArrayInputStream(new byte[0])));
    }

    @Test
    void maintenanceActualGxpFailureRollsBackSameScopeRegistryTransactionAndRestoresPrincipal() throws Exception {
        actualToken();
        var verifiedScope = seal();
        var input = mock(DccLegacyMaintenanceGate.Input.class);
        when(maintenanceGate.read(any(), any(), any())).thenReturn(input);
        when(maintenanceGate.verifiedScope(input)).thenReturn(verifiedScope);
        var previous = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var command = new DccLegacyNameRegistrationMaintenanceCommand(maintenanceGate, adapter, executor, guardedAuthReadFactory());
        var error = assertThrows(IllegalStateException.class, () -> command.execute(temp, temp.resolve("request.json"), "a".repeat(64), new ByteArrayInputStream("OFFLINE-REAL-PORT-TOKEN".getBytes(StandardCharsets.UTF_8))));
        zero();
        assertSame(previous, org.springframework.security.core.context.SecurityContextHolder.getContext());
        assertFalse(error.getMessage().contains("TOKEN"));
        assertNull(error.getCause());
    }
}
