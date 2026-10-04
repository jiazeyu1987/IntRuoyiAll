package cn.iocoder.yudao.module.dcc.service.file;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.iocoder.yudao.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccLegacyMaintenanceLoggingTest {

    static final String SERVICE = "cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenServiceImpl";

    static final String MAPPER = "cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper";

    final Map<Logger, Level> previous = new LinkedHashMap<>();

    Logger level(String name, Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(name);
        if (!previous.containsKey(logger))
            previous.put(logger, logger.getLevel());
        logger.setLevel(level);
        return logger;
    }

    void guardedParentsOff() {
        for (String name : List.of(SERVICE, MAPPER, "cn.iocoder.yudao.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO", "org.springframework.data.redis", "io.lettuce.core.protocol", "org.mybatis.spring", "org.apache.ibatis", "org.springframework.jdbc.core")) level(name, Level.OFF);
        for (String name : List.of("cn.iocoder.yudao.module.system.service.oauth2", "cn.iocoder.yudao.module.system.dal.mysql.oauth2", "cn.iocoder.yudao.module.system.dal.redis.oauth2")) level(name, Level.OFF);
        for (var configuration : org.springframework.boot.logging.LoggingSystem.get(getClass().getClassLoader()).getLoggerConfigurations()) {
            if (List.of("cn.iocoder.yudao.module.system.service.oauth2.", "cn.iocoder.yudao.module.system.dal.mysql.oauth2.", "cn.iocoder.yudao.module.system.dal.redis.oauth2.", "org.springframework.data.redis.", "io.lettuce.core.protocol.", "org.mybatis.spring.", "org.apache.ibatis.", "org.springframework.jdbc.core.").stream().anyMatch(configuration.getName()::startsWith))
                level(configuration.getName(), Level.OFF);
        }
    }

    @AfterEach
    void restore() {
        previous.forEach(Logger::setLevel);
    }

    @Test
    void actualOAuthServiceWarnMustBeOffBeforeReadingAnyToken() {
        guardedParentsOff();
        level(SERVICE, Level.WARN);
        assertThrows(IllegalArgumentException.class, DccLegacyNameRegistrationMaintenanceCommand::requireSecretLoggersDisabled);
    }

    @Test
    void underlyingBaseMapperSelectListChildCannotOverrideOffParent() {
        guardedParentsOff();
        level(MAPPER + ".selectList", Level.DEBUG);
        assertThrows(IllegalArgumentException.class, DccLegacyNameRegistrationMaintenanceCommand::requireSecretLoggersDisabled);
    }

    @Test
    void stdoutAndAutoSelectedMyBatisLoggingRejectBeforeCredentialLookup() {
        for (Class<? extends org.apache.ibatis.logging.Log> unsafe : List.of(org.apache.ibatis.logging.stdout.StdOutImpl.class, org.apache.ibatis.logging.nologging.NoLoggingImpl.class)) {
            var factory = mock(org.apache.ibatis.session.SqlSessionFactory.class);
            var configuration = new org.apache.ibatis.session.Configuration();
            configuration.setLogImpl(unsafe);
            when(factory.getConfiguration()).thenReturn(configuration);
            assertThrows(IllegalArgumentException.class, () -> DccLegacyNameRegistrationMaintenanceCommand.requireSafeMyBatisLogging(factory));
        }
    }

    @Test
    void actualFormalServiceRedisFaultCannotEmitTokenWhenGuardedOff() {
        guardedParentsOff();
        DccLegacyNameRegistrationMaintenanceCommand.requireSecretLoggersDisabled();
        Logger logger = (Logger) LoggerFactory.getLogger(SERVICE);
        var events = new ListAppender<ILoggingEvent>();
        events.start();
        logger.addAppender(events);
        try {
            var service = new OAuth2TokenServiceImpl();
            var redis = mock(OAuth2AccessTokenRedisDAO.class);
            var access = mock(OAuth2AccessTokenMapper.class);
            var refresh = mock(OAuth2RefreshTokenMapper.class);
            ReflectionTestUtils.setField(service, "oauth2AccessTokenRedisDAO", redis);
            ReflectionTestUtils.setField(service, "oauth2AccessTokenMapper", access);
            ReflectionTestUtils.setField(service, "oauth2RefreshTokenMapper", refresh);
            when(redis.get("ISOLATED-LOGGER-BOUNDARY-TOKEN")).thenThrow(new IllegalStateException("isolated Redis fault"));
            assertNull(service.getAccessToken("ISOLATED-LOGGER-BOUNDARY-TOKEN"));
            verify(redis).get("ISOLATED-LOGGER-BOUNDARY-TOKEN");
            assertTrue(events.list.isEmpty(), "actual service WARN path must emit no credential event");
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }
}
