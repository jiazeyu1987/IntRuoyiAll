package cn.iocoder.yudao.module.dcc.service.file;

import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Explicit one-time local configuration invocation. No controller or scheduler.
 */
@Service
public class DccLegacyNameRegistrationMaintenanceCommand {

    final DccLegacyMaintenanceGate gate;

    final DccLegacyMaintenanceAuthAdapter auth;

    final DccLegacyMaintenanceExecutor executor;

    final org.apache.ibatis.session.SqlSessionFactory sqlSessions;

    final AtomicBoolean invoked = new AtomicBoolean();

    public DccLegacyNameRegistrationMaintenanceCommand(DccLegacyMaintenanceGate gate, DccLegacyMaintenanceAuthAdapter auth, DccLegacyMaintenanceExecutor executor, org.apache.ibatis.session.SqlSessionFactory sqlSessions) {
        this.gate = gate;
        this.auth = auth;
        this.executor = executor;
        this.sqlSessions = java.util.Objects.requireNonNull(sqlSessions);
    }

    public DccLegacySourceNameRegistrationService.Receipt execute(Path root, Path request, String requestHash, InputStream tokenInput) {
        if (!invoked.compareAndSet(false, true))
            throw DccLegacyMaintenanceGate.invalid("ONCE_ONLY_NO_RETRY");
        try {
            gate.requireDevelopmentEnvironment();
            var input = gate.read(root, request, requestHash);
            // Immutable authorized development facts and complete proof precede stdin; current rows are rechecked in the authenticated transaction.
            var verifiedScope = gate.verifiedScope(input);
            gate.requireDevelopmentPolicy(input);
            requireSecretLoggersDisabled();
            requireSafeMyBatisLogging(sqlSessions);
            byte[] bytes = tokenInput.readNBytes(65537);
            String token;
            try {
                if (bytes.length == 0 || bytes.length > 65536)
                    throw DccLegacyMaintenanceGate.invalid("TOKEN_INPUT_BOUND");
                token = StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            } finally {
                java.util.Arrays.fill(bytes, (byte) 0);
            }
            return auth.withVerifiedActor(token, () -> executor.execute(input, verifiedScope));
        } catch (Exception error) {
            String code = error.getMessage();
            if (code == null || !code.matches("DCC_MAINTENANCE_[A-Z0-9_]+"))
                code = "DCC_MAINTENANCE_FAILED_" + error.getClass().getSimpleName().toUpperCase(java.util.Locale.ROOT);
            // No raw nested exception/message/source key/token can reach startup error logs.
            throw new IllegalStateException(code);
        }
    }

    static void requireSecretLoggersDisabled() {
        for (String name : java.util.List.of("cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenServiceImpl", "cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper", "cn.iocoder.yudao.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO", "org.springframework.data.redis", "io.lettuce.core.protocol", "org.mybatis.spring", "org.apache.ibatis", "org.springframework.jdbc.core")) {
            var logger = org.slf4j.LoggerFactory.getLogger(name);
            if (logger.isErrorEnabled() || logger.isWarnEnabled() || logger.isInfoEnabled() || logger.isDebugEnabled() || logger.isTraceEnabled())
                throw DccLegacyMaintenanceGate.invalid("TOKEN_LOGGER_MUST_BE_OFF");
        }
        var logging = org.springframework.boot.logging.LoggingSystem.get(DccLegacyNameRegistrationMaintenanceCommand.class.getClassLoader());
        var protectedPrefixes = java.util.List.of("cn.iocoder.yudao.module.system.service.oauth2.", "cn.iocoder.yudao.module.system.dal.mysql.oauth2.", "cn.iocoder.yudao.module.system.dal.redis.oauth2.", "org.springframework.data.redis.", "io.lettuce.core.protocol.", "org.mybatis.spring.", "org.apache.ibatis.", "org.springframework.jdbc.core.");
        for (var configuration : logging.getLoggerConfigurations()) if (protectedPrefixes.stream().anyMatch(configuration.getName()::startsWith) && configuration.getEffectiveLevel() != org.springframework.boot.logging.LogLevel.OFF)
            throw DccLegacyMaintenanceGate.invalid("TOKEN_CHILD_LOGGER_MUST_BE_OFF");
    }

    static void requireSafeMyBatisLogging(org.apache.ibatis.session.SqlSessionFactory factory) {
        var configuration = factory.getConfiguration();
        if (configuration == null || configuration.getLogImpl() != org.apache.ibatis.logging.slf4j.Slf4jImpl.class)
            throw DccLegacyMaintenanceGate.invalid("TOKEN_MYBATIS_SLF4J_REQUIRED");
        int statements = 0;
        for (String id : configuration.getMappedStatementNames()) if (id.startsWith("cn.iocoder.yudao.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper.")) {
            statements++;
            if (!(configuration.getMappedStatement(id).getStatementLog() instanceof org.apache.ibatis.logging.slf4j.Slf4jImpl))
                throw DccLegacyMaintenanceGate.invalid("TOKEN_MYBATIS_STATEMENT_LOGGER_UNSAFE");
        }
        if (statements == 0)
            throw DccLegacyMaintenanceGate.invalid("TOKEN_MYBATIS_STATEMENTS_REQUIRED");
    }
}
