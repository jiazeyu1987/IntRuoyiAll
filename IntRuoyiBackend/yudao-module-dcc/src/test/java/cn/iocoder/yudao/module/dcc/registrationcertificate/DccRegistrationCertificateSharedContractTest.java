package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DccRegistrationCertificateSharedContractTest extends BaseDbUnitTest {

    private static final List<String> COMMAND_ERROR_NAMES = List.of(
            "REGISTRATION_CERTIFICATE_REVISION_CONFLICT",
            "REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED",
            "REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT",
            "REGISTRATION_CERTIFICATE_TENANT_MISMATCH",
            "REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED",
            "REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED",
            "REGISTRATION_CERTIFICATE_PRODUCT_REQUIRED",
            "REGISTRATION_CERTIFICATE_PRODUCT_INVALID",
            "REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID",
            "REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED",
            "REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH",
            "REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH",
            "REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID",
            "REGISTRATION_CERTIFICATE_FIRST_OBTAINED_DATE_INVALID",
            "REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID",
            "REGISTRATION_CERTIFICATE_FILE_REQUIRED",
            "REGISTRATION_CERTIFICATE_FILE_NOT_STAGED",
            "REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH",
            "REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT",
            "REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT");

    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Resource
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;
    @Resource
    private DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    @Resource
    private DccRegistrationCertificateAuditMapper auditMapper;

    @Test
    void sharedMapperSurfaceAndCommandErrorsShouldBeExplicit() throws Exception {
        requiredMethod(DccRegistrationCertificateSnapshotMapper.class, "updateDraftByIdAndRevision",
                DccRegistrationCertificateSnapshotDO.class, Long.class, Integer.class);
        requiredMethod(DccRegistrationCertificateSnapshotMapper.class, "deleteDraftByIdAndRevision",
                Long.class, Long.class, Integer.class);
        requiredMethod(DccRegistrationCertificateSnapshotEntrustedMapper.class,
                "deleteDraftBySnapshotIdAndRevision", Long.class, Long.class, Integer.class);
        requiredMethod(DccRegistrationCertificateAuditMapper.class, "selectByTenantIdAndEventKey",
                Long.class, String.class);

        String errors = Files.readString(findBackendRoot().resolve(
                        "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java"),
                StandardCharsets.UTF_8);
        for (int index = 0; index < COMMAND_ERROR_NAMES.size(); index++) {
            assertEquals(1, count(errors, COMMAND_ERROR_NAMES.get(index)),
                    "command error name must be allocated exactly once");
            assertEquals(1, count(errors, "1_080_000_" + (220 + index)),
                    "command error code must be allocated exactly once");
        }
    }

    @Test
    void draftSnapshotUpdateShouldRequireTenantRevisionAndLinkedDraft() {
        DccRegistrationCertificateVersionDO version = insertVersion(1L, "DRAFT", 1);
        DccRegistrationCertificateSnapshotDO snapshot = insertSnapshot(
                1L, version.getId(), 1, "Before", 30L);
        Method update = requiredMethod(DccRegistrationCertificateSnapshotMapper.class,
                "updateDraftByIdAndRevision", DccRegistrationCertificateSnapshotDO.class,
                Long.class, Integer.class);
        Method deleteProjection = requiredMethod(DccRegistrationCertificateSnapshotEntrustedMapper.class,
                "deleteDraftBySnapshotIdAndRevision", Long.class, Long.class, Integer.class);

        DccRegistrationCertificateSnapshotDO change = snapshotChange(snapshot.getId(), "After");
        assertEquals(0, invokeInt(update, snapshotMapper, change, 2L, 1));
        assertEquals(0, invokeInt(update, snapshotMapper, change, 1L, 2));
        assertEquals("Before", snapshotMapper.selectById(snapshot.getId()).getProductName());

        insertEntrusted(1L, snapshot.getId(), 30L);
        assertEquals(0, invokeInt(update, snapshotMapper, change, 1L, 1),
                "snapshot update must not drift an existing entrusted projection");
        assertEquals("Before", snapshotMapper.selectById(snapshot.getId()).getProductName());
        assertEquals(1, invokeInt(deleteProjection, entrustedMapper, snapshot.getId(), 1L, 1));

        assertEquals(1, invokeInt(update, snapshotMapper, change, 1L, 1));
        DccRegistrationCertificateSnapshotDO updated = snapshotMapper.selectById(snapshot.getId());
        assertEquals("After", updated.getProductName());
        assertEquals(2, updated.getRevisionNo());

        version.setStatus("CURRENT");
        assertEquals(1, versionMapper.updateById(version));
        DccRegistrationCertificateSnapshotDO forbidden = snapshotChange(snapshot.getId(), "Forbidden");
        assertEquals(0, invokeInt(update, snapshotMapper, forbidden, 1L, 2));
        assertEquals("After", snapshotMapper.selectById(snapshot.getId()).getProductName());
    }

    @Test
    void crossTenantEntrustedProjectionShouldBlockSnapshotMutation() {
        DccRegistrationCertificateVersionDO version = insertVersion(1L, "DRAFT", 4);
        DccRegistrationCertificateSnapshotDO snapshot = insertSnapshot(
                1L, version.getId(), 1, "Cross tenant", 40L);
        insertEntrusted(2L, snapshot.getId(), 40L);
        Method update = requiredMethod(DccRegistrationCertificateSnapshotMapper.class,
                "updateDraftByIdAndRevision", DccRegistrationCertificateSnapshotDO.class,
                Long.class, Integer.class);
        Method deleteSnapshot = requiredMethod(DccRegistrationCertificateSnapshotMapper.class,
                "deleteDraftByIdAndRevision", Long.class, Long.class, Integer.class);

        assertEquals(0, invokeInt(update, snapshotMapper,
                snapshotChange(snapshot.getId(), "Forbidden"), 1L, 1));
        assertEquals(0, invokeInt(deleteSnapshot, snapshotMapper, snapshot.getId(), 1L, 1));
        assertEquals("Cross tenant", snapshotMapper.selectById(snapshot.getId()).getProductName());
    }

    @Test
    void draftSnapshotAndProjectionDeleteShouldRejectFormalOrDriftedRows() {
        Method deleteProjection = requiredMethod(DccRegistrationCertificateSnapshotEntrustedMapper.class,
                "deleteDraftBySnapshotIdAndRevision", Long.class, Long.class, Integer.class);
        Method deleteSnapshot = requiredMethod(DccRegistrationCertificateSnapshotMapper.class,
                "deleteDraftByIdAndRevision", Long.class, Long.class, Integer.class);

        DccRegistrationCertificateVersionDO formalVersion = insertVersion(1L, "CURRENT", 1);
        DccRegistrationCertificateSnapshotDO formalSnapshot = insertSnapshot(
                1L, formalVersion.getId(), 1, "Formal", 10L);
        insertEntrusted(1L, formalSnapshot.getId(), 10L);
        assertEquals(0, invokeInt(deleteProjection, entrustedMapper, formalSnapshot.getId(), 1L, 1));
        assertEquals(0, invokeInt(deleteSnapshot, snapshotMapper, formalSnapshot.getId(), 1L, 1));
        assertNotNull(snapshotMapper.selectById(formalSnapshot.getId()));
        assertEquals(1, entrustedMapper.selectListBySnapshotId(formalSnapshot.getId()).size());

        DccRegistrationCertificateVersionDO draftVersion = insertVersion(1L, "DRAFT", 2);
        DccRegistrationCertificateSnapshotDO draftSnapshot = insertSnapshot(
                1L, draftVersion.getId(), 3, "Draft", 20L);
        insertEntrusted(1L, draftSnapshot.getId(), 20L);
        assertEquals(0, invokeInt(deleteProjection, entrustedMapper, draftSnapshot.getId(), 2L, 3));
        assertEquals(0, invokeInt(deleteProjection, entrustedMapper, draftSnapshot.getId(), 1L, 2));
        assertEquals(1, entrustedMapper.selectListBySnapshotId(draftSnapshot.getId()).size());
        assertEquals(0, invokeInt(deleteSnapshot, snapshotMapper, draftSnapshot.getId(), 1L, 3),
                "snapshot deletion must not orphan an entrusted projection");
        assertEquals(1, invokeInt(deleteProjection, entrustedMapper, draftSnapshot.getId(), 1L, 3));
        assertTrue(entrustedMapper.selectListBySnapshotId(draftSnapshot.getId()).isEmpty());
        assertEquals(0, invokeInt(deleteSnapshot, snapshotMapper, draftSnapshot.getId(), 1L, 2));
        assertEquals(1, invokeInt(deleteSnapshot, snapshotMapper, draftSnapshot.getId(), 1L, 3));
        assertNull(snapshotMapper.selectById(draftSnapshot.getId()));
    }

    @Test
    void auditReplayLookupShouldRequireTenantAndExactEventKey() {
        Method lookup = requiredMethod(DccRegistrationCertificateAuditMapper.class,
                "selectByTenantIdAndEventKey", Long.class, String.class);
        DccRegistrationCertificateAuditDO tenantOne = insertAudit(1L, "draft:create:key", 101L);
        DccRegistrationCertificateAuditDO tenantTwo = insertAudit(2L, "draft:create:key", 202L);

        assertEquals(tenantOne.getId(), invokeAudit(lookup, 1L, "draft:create:key").getId());
        assertEquals(tenantTwo.getId(), invokeAudit(lookup, 2L, "draft:create:key").getId());
        assertNull(invokeAudit(lookup, 1L, "draft:create:missing"));
    }

    private DccRegistrationCertificateVersionDO insertVersion(Long tenantId, String status, int versionNo) {
        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(100L + versionNo)
                .versionNo(versionNo)
                .versionType("INITIAL_CERTIFICATE")
                .categoryChanged(false)
                .status(status)
                .build();
        version.setTenantId(tenantId);
        assertEquals(1, versionMapper.insert(version));
        return version;
    }

    private DccRegistrationCertificateSnapshotDO insertSnapshot(
            Long tenantId, Long versionId, int revisionNo, String productName, Long enterpriseId) {
        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(versionId)
                .revisionNo(revisionNo)
                .productName(productName)
                .registrantName("Registrant")
                .entrustedProduction(true)
                .selfProduction(false)
                .entrustedEnterprisesJson("[{\"enterpriseId\":" + enterpriseId
                        + ",\"enterpriseName\":\"Enterprise " + enterpriseId + "\"}]")
                .effectiveAt(LocalDateTime.of(2026, 8, 17, 9, 0))
                .build();
        snapshot.setTenantId(tenantId);
        assertEquals(1, snapshotMapper.insert(snapshot));
        return snapshot;
    }

    private void insertEntrusted(Long tenantId, Long snapshotId, Long enterpriseId) {
        DccRegistrationCertificateSnapshotEntrustedDO entrusted =
                DccRegistrationCertificateSnapshotEntrustedDO.builder()
                        .snapshotId(snapshotId)
                        .enterpriseId(enterpriseId)
                        .enterpriseNameSnapshot("Enterprise " + enterpriseId)
                        .sortOrder(1)
                        .build();
        entrusted.setTenantId(tenantId);
        assertEquals(1, entrustedMapper.insert(entrusted));
    }

    private DccRegistrationCertificateAuditDO insertAudit(Long tenantId, String eventKey, Long actorId) {
        DccRegistrationCertificateAuditDO audit = DccRegistrationCertificateAuditDO.builder()
                .tenantId(tenantId)
                .ownerCompanyId(10L)
                .certificateId(100L)
                .eventKey(eventKey)
                .eventType("DRAFT_CREATED")
                .actorId(actorId)
                .result("SUCCESS")
                .resultCode("OK")
                .requestTraceId("trace-" + tenantId)
                .detailJson("{}")
                .occurredAt(LocalDateTime.of(2026, 8, 17, 9, 1))
                .creator(String.valueOf(actorId))
                .build();
        assertEquals(1, auditMapper.insert(audit));
        return audit;
    }

    private static DccRegistrationCertificateSnapshotDO snapshotChange(Long id, String productName) {
        return DccRegistrationCertificateSnapshotDO.builder()
                .id(id)
                .productName(productName)
                .registrantName("Registrant Updated")
                .modelSpecification("Model")
                .structureComposition("Structure")
                .intendedUse("Use")
                .technicalRequirements("Requirements")
                .residenceAddress("Residence")
                .productionAddress("Production")
                .entrustedProduction(true)
                .selfProduction(false)
                .entrustedEnterprisesJson("[{\"enterpriseId\":30,"
                        + "\"enterpriseName\":\"Enterprise 30\"}]")
                .effectiveAt(LocalDateTime.of(2026, 8, 18, 9, 0))
                .build();
    }

    private DccRegistrationCertificateAuditDO invokeAudit(Method method, Long tenantId, String eventKey) {
        return (DccRegistrationCertificateAuditDO) invoke(method, auditMapper, tenantId, eventKey);
    }

    private static int invokeInt(Method method, Object target, Object... arguments) {
        return (Integer) invoke(method, target, arguments);
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError("mapper invocation failed", cause);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("mapper invocation failed", exception);
        }
    }

    private static Method requiredMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            fail("missing shared mapper contract: " + type.getSimpleName() + "." + name);
            return null;
        }
    }

    private static int count(String text, String fragment) {
        int result = 0;
        for (int index = 0; (index = text.indexOf(fragment, index)) >= 0; index += fragment.length()) {
            result++;
        }
        return result;
    }

    private static Path findBackendRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("sql/mysql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("IntRuoyiBackend root not found");
    }
}
