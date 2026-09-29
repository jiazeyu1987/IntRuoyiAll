package cn.iocoder.yudao.module.system.service.gxpaudit.maintenance;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Explicit maintenance command; never discovered as an application component. */
public final class GxpAuditPolicyActivateMain {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Set<String> BOUNDARY_DECISIONS = Set.of("REGISTERED", "PRIVATE_HELPER",
            "APPROVED_EXCLUSION", "REVIEWED_READ_ONLY", "REVIEWED_OUT_OF_RELEASE_SCOPE", "REVIEWED_MIXED_SCOPE",
            "REVIEWED_DELEGATED_EFFECT");

    private GxpAuditPolicyActivateMain() { }

    public static void main(String[] args) {
        try {
            require(args.length == 0, "use-explicit-environment-inputs");
            GxpAuditPolicyActivationResult result = run(System.getenv());
            System.out.println("GXP activation committed: id=" + result.activationId()
                    + " policyHash=" + result.policyHash() + " replayed=" + result.replayed());
        } catch (RequirementFailure failure) {
            System.err.println("GXP activation refused: " + failure.getMessage());
            System.exit(1);
        } catch (Exception failure) {
            // JDBC/driver exceptions can contain connection details. Never print credentials or SQL.
            System.err.println("GXP activation failed: database/context/transaction error ("
                    + failure.getClass().getSimpleName() + "). No successful activation is reported.");
            System.exit(1);
        }
    }

    static GxpAuditPolicyActivationResult run(Map<String, String> env) throws IOException {
        String url = input(env, "DB_URL");
        String dbUser = input(env, "DB_USER");
        String dbPassword = input(env, "DB_PASSWORD");
        String operator = input(env, "OPERATOR");
        String password = input(env, "OPERATOR_PASSWORD");
        String request = input(env, "REQUEST_ID");
        require(request.length() <= 96, "request-id-too-long");
        long tenant = positiveLong(input(env, "TENANT_ID"));
        require("true".equals(input(env, "WRITERS_STOPPED")), "maintenance-window-required");
        require(TenantContextHolder.getTenantId() == null && !TenantContextHolder.isIgnore()
                && SecurityContextHolder.getContext().getAuthentication() == null, "standalone-context-required");
        GxpAuditPolicyBundle bundle = new GxpAuditPolicyBundleLoader().load();
        String approval = input(env, "APPROVAL_REFERENCE");
        require(bundle.approved() && approval.equals(bundle.approvalReference()), "approved-bundle-reference-mismatch");
        String coverageHash = validateEvidence(env, bundle, approval);
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, dbUser, dbPassword);
        try (AnnotationConfigApplicationContext context = openContext(dataSource)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            return transaction.execute(status -> {
                LoginUser actor = authenticate(jdbc, tenant, operator, password);
                TenantContextHolder.setTenantId(tenant);
                var security = SecurityContextHolder.createEmptyContext();
                security.setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
                SecurityContextHolder.setContext(security);
                try {
                    validateLedger(jdbc, tenant);
                    validateExistingVersion(jdbc, tenant, bundle, coverageHash);
                    return context.getBean(GxpAuditPolicyActivationService.class).activate(
                            new GxpAuditPolicyActivationCommand(tenant, actor.getId(), approval, request, coverageHash));
                } finally {
                    TenantContextHolder.clear();
                    SecurityContextHolder.clearContext();
                }
            });
        }
    }

    /** Registers an explicit allowlist. No Boot auto-configuration, scanning, runners or scheduling. */
    static AnnotationConfigApplicationContext openContext(DataSource dataSource) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        try {
            context.register(TransactionSupport.class);
            context.registerBean(DataSource.class, () -> dataSource);
            context.registerBean(PlatformTransactionManager.class, () -> new DataSourceTransactionManager(dataSource));
            context.registerBean(SqlSessionFactory.class, () -> sessionFactory(dataSource));
            context.registerBean(SqlSessionTemplate.class,
                    () -> new SqlSessionTemplate(context.getBean(SqlSessionFactory.class)));
            registerMapper(context, TenantMapper.class);
            registerMapper(context, GxpAuditPolicyVersionMapper.class);
            registerMapper(context, GxpAuditPolicyOperationMapper.class);
            registerMapper(context, GxpAuditPolicyActivationMapper.class);
            registerMapper(context, GxpAuditLedgerSequenceMapper.class);
            registerMapper(context, GxpAuditEventMapper.class);
            registerMapper(context, GxpAuditEventRelationMapper.class);
            context.registerBean(GxpAuditPolicyBundleLoader.class);
            context.registerBean(GxpAuditPolicyActivationServiceImpl.class);
            context.registerBean(GxpAuditServiceImpl.class);
            context.refresh();
            return context;
        } catch (RuntimeException failure) {
            context.close();
            throw failure;
        }
    }

    // Not @Configuration/@Component: the normal server must not discover this separate context.
    @EnableTransactionManagement
    static class TransactionSupport { }

    private static <T> void registerMapper(AnnotationConfigApplicationContext context, Class<T> mapper) {
        context.registerBean(mapper, () -> {
            SqlSessionTemplate session = context.getBean(SqlSessionTemplate.class);
            if (!session.getConfiguration().hasMapper(mapper)) {
                session.getConfiguration().addMapper(mapper);
            }
            return session.getMapper(mapper);
        });
    }

    private static SqlSessionFactory sessionFactory(DataSource dataSource) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        GlobalConfig global = new GlobalConfig();
        global.setBanner(false);
        global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
        global.setMetaObjectHandler(new DefaultDBFieldHandler());
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(global);
        try {
            return Objects.requireNonNull(factory.getObject());
        } catch (Exception failure) {
            throw new IllegalStateException("maintenance-mapper-initialization-failed", failure);
        }
    }

    private static String validateEvidence(Map<String, String> env, GxpAuditPolicyBundle bundle,
                                          String approval) throws IOException {
        byte[] reportBytes = Files.readAllBytes(Path.of(input(env, "COVERAGE_FILE")));
        String reportHash = DigestUtil.sha256Hex(reportBytes);
        require(reportHash.equals(input(env, "COVERAGE_SHA256")), "coverage-report-digest-mismatch");
        JsonNode report = JSON.readTree(reportBytes);
        require(report != null && report.isObject(), "coverage-report-object-required");
        require("gxp-coverage-report.v1".equals(text(report, "schemaVersion")), "coverage-schema-invalid");
        require("PASS".equals(text(report, "status")), "coverage-not-passing");
        require(bundle.policyVersion().equals(text(report, "policyVersion")), "coverage-policy-version-mismatch");
        require(bundle.policyHash().equals(text(report, "policyHash")), "coverage-policy-hash-mismatch");
        require(bundle.artifactHash().equals(text(report, "artifactHash")), "coverage-artifact-hash-mismatch");
        require(approval.equals(text(report, "approvalReference")), "coverage-approval-reference-mismatch");
        String mode = text(report, "coverageMode");
        require(Set.of("R1", "FULL_COVERAGE").contains(mode), "coverage-mode-invalid");
        require(mode.equals(text(bundle.policyNode().path("coverageScope"), "mode")), "coverage-policy-scope-mismatch");
        require(nonnegativeInteger(report, "unresolvedCount") == 0, "coverage-unresolved-boundaries");
        int excluded = nonnegativeInteger(report, "reviewedOutOfScopeCount");
        require(!"FULL_COVERAGE".equals(mode) || excluded == 0, "full-coverage-has-out-of-scope-boundaries");
        byte[] boundaries = Files.readAllBytes(Path.of(input(env, "BOUNDARY_FILE")));
        require(DigestUtil.sha256Hex(boundaries).equals(text(report, "boundarySha256")), "coverage-boundary-digest-mismatch");
        int count = 0;
        int observedExcluded = 0;
        for (String line : new String(boundaries, StandardCharsets.UTF_8).lines().toList()) {
            require(!line.isBlank(), "coverage-boundary-empty-row");
            JsonNode boundary = JSON.readTree(line);
            String decision = text(boundary, "decision");
            require(BOUNDARY_DECISIONS.contains(decision), "coverage-boundary-not-reviewed");
            if (Set.of("REVIEWED_OUT_OF_RELEASE_SCOPE", "REVIEWED_MIXED_SCOPE").contains(decision)) observedExcluded++;
            count++;
        }
        require(count > 0, "coverage-boundaries-empty");
        require(observedExcluded == excluded, "coverage-exclusion-count-mismatch");
        return reportHash;
    }

    private static LoginUser authenticate(JdbcTemplate jdbc, long tenant, String operator, String password) {
        var tenants = jdbc.query("SELECT status, expire_time FROM system_tenant WHERE id=? AND deleted=false FOR UPDATE",
                (row, index) -> row.getInt("status") == 0
                        && row.getTimestamp("expire_time").toLocalDateTime().isAfter(LocalDateTime.now()), tenant);
        require(tenants.size() == 1 && tenants.get(0), "tenant-missing-disabled-or-expired");
        var users = jdbc.query("SELECT id, username, nickname, password, status, login_locked, password_credential_status "
                        + "FROM system_users WHERE tenant_id=? AND username=? AND deleted=false FOR UPDATE",
                (row, index) -> {
                    require(row.getInt("status") == 0 && !row.getBoolean("login_locked")
                            && "ACTIVE".equals(row.getString("password_credential_status")), "operator-not-active");
                    require(operator.equals(row.getString("username"))
                            && new BCryptPasswordEncoder().matches(password, row.getString("password")), "operator-authentication-failed");
                    require(row.getString("nickname") != null && !row.getString("nickname").isBlank(), "operator-display-name-missing");
                    LoginUser user = new LoginUser();
                    user.setId(row.getLong("id"));
                    user.setTenantId(tenant);
                    user.setUserType(UserTypeEnum.ADMIN.getValue());
                    user.setInfo(Map.of("username", row.getString("username"), LoginUser.INFO_KEY_NICKNAME, row.getString("nickname")));
                    return user;
                }, tenant, operator);
        require(users.size() == 1, "operator-not-found-or-ambiguous");
        LoginUser actor = users.get(0);
        Integer roles = jdbc.queryForObject("SELECT COUNT(*) FROM system_user_role ur JOIN system_role r ON r.id=ur.role_id "
                        + "AND r.tenant_id=ur.tenant_id WHERE ur.tenant_id=? AND ur.user_id=? "
                        + "AND ur.deleted=false AND r.deleted=false AND r.status=0 AND r.code IN ('super_admin','tenant_admin')",
                Integer.class, tenant, actor.getId());
        require(roles != null && roles > 0, "tenant-administrator-role-required");
        return actor;
    }

    /** Verify current ledger facts without changing, importing or rebuilding historical evidence. */
    private static void validateExistingVersion(JdbcTemplate jdbc, long tenant,
                                                GxpAuditPolicyBundle bundle, String coverageHash) {
        List<Boolean> versions = jdbc.query("SELECT policy_hash, artifact_hash, coverage_report_hash, approval_reference, "
                        + "schema_version, canonical_policy_json FROM gxp_audit_policy_version "
                        + "WHERE tenant_id=? AND policy_version=? FOR UPDATE",
                (row, index) -> bundle.policyHash().equals(row.getString("policy_hash"))
                        && bundle.artifactHash().equals(row.getString("artifact_hash"))
                        && coverageHash.equals(row.getString("coverage_report_hash"))
                        && bundle.approvalReference().equals(row.getString("approval_reference"))
                        && bundle.schemaVersion().equals(row.getString("schema_version"))
                        && bundle.canonicalPolicyJson().equals(row.getString("canonical_policy_json")), tenant, bundle.policyVersion());
        require(versions.isEmpty() || versions.size() == 1 && versions.get(0), "existing-version-evidence-mismatch");
    }

    private static void validateLedger(JdbcTemplate jdbc, long tenant) {
        List<Long> watermarks = jdbc.queryForList("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence "
                + "WHERE tenant_id=? FOR UPDATE", Long.class, tenant);
        long[] expectedSequence = {1L};
        String[] previousHash = {null};
        jdbc.query("SELECT ledger_sequence, canonical_event_json, event_hash, previous_event_hash FROM gxp_audit_event "
                + "WHERE tenant_id=? ORDER BY ledger_sequence", row -> {
            require(row.getLong("ledger_sequence") == expectedSequence[0], "ledger-sequence-gap");
            require(Objects.equals(previousHash[0], row.getString("previous_event_hash")), "ledger-chain-link-mismatch");
            require(DigestUtil.sha256Hex(row.getString("canonical_event_json")).equals(row.getString("event_hash")), "ledger-event-hash-mismatch");
            previousHash[0] = row.getString("event_hash");
            expectedSequence[0]++;
        }, tenant);
        require(watermarks.size() == 1 && watermarks.get(0) == expectedSequence[0]
                || watermarks.isEmpty() && expectedSequence[0] == 1L, "ledger-watermark-mismatch");
        String[] previousActivation = {null};
        jdbc.query("SELECT canonical_activation_json, activation_hash, previous_activation_hash FROM gxp_audit_policy_activation "
                + "WHERE tenant_id=? ORDER BY id", row -> {
            require(Objects.equals(previousActivation[0], row.getString("previous_activation_hash")), "activation-chain-link-mismatch");
            require(DigestUtil.sha256Hex(row.getString("canonical_activation_json")).equals(row.getString("activation_hash")), "activation-hash-mismatch");
            previousActivation[0] = row.getString("activation_hash");
        }, tenant);
    }

    private static int nonnegativeInteger(JsonNode node, String field) {
        JsonNode value = node.get(field);
        require(value != null && value.isIntegralNumber() && value.canConvertToInt() && value.intValue() >= 0,
                "coverage-invalid-count-" + field);
        return value.intValue();
    }

    private static String text(JsonNode node, String field) {
        require(node != null && node.isObject(), "coverage-invalid-object");
        JsonNode value = node.get(field);
        require(value != null && value.isTextual() && !value.textValue().isBlank(), "coverage-missing-field-" + field);
        return value.textValue();
    }

    private static String input(Map<String, String> env, String key) {
        String value = env.get("GXP_ACTIVATION_" + key);
        require(value != null && !value.isBlank(), "missing-environment-GXP_ACTIVATION_" + key);
        return value;
    }

    private static long positiveLong(String value) {
        require(value.matches("[1-9][0-9]{0,17}"), "tenant-id-invalid");
        return Long.parseLong(value);
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new RequirementFailure(reason);
    }

    private static final class RequirementFailure extends IllegalArgumentException {
        private RequirementFailure(String reason) { super(reason); }
    }
}
