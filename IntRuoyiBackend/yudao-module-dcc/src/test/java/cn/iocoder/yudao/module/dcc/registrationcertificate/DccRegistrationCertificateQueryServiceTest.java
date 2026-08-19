package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateQueryMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateDetail;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateOldIndexItem;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageItem;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageQuery;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateQueryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateQueryServiceImpl;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateQueryServiceImpl.class,
        DccRegistrationCertificateReadAuditService.class,
        DccRegistrationCertificateAccessPolicyService.class,
        DccRegistrationCertificateBusinessClock.class
})
class DccRegistrationCertificateQueryServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateQueryService queryService;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Resource
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;
    @Resource
    private DccRegistrationCertificateFileMapper dbFileMapper;
    @Resource
    private DccRegistrationCertificateGrantMapper grantMapper;
    @Resource
    private DccRegistrationCertificateAuditMapper auditMapper;
    @Resource
    private DccRegistrationCertificateQueryMapper queryMapper;

    @MockitoBean
    private MdmCompanyScopeApi companyScopeApi;
    @MockitoBean
    private MdmEnterpriseApi enterpriseApi;

    @BeforeEach
    void setUp() {
        reset(companyScopeApi, enterpriseApi);
    }

    @Test
    void detailContractExposesFormalRegistrationBusinessFileId() {
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("registrationFileId"),
                "detail response must expose the formal registration business-file id");
    }

    @Test
    void detailReturnsFormalRegistrationBusinessFileId() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-FILE-ID", true, 20L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        DccRegistrationCertificateDetail detail = queryService.getDetail(
                1L, 99L, visible.certificateId(), context("REQ-DETAIL-FILE-ID"));

        assertEquals(visible.registrationFileId(), detail.getRegistrationFileId(),
                "detail must return the formal business-file id selected by the query mapper");
    }

    @Test
    void pageAppliesCompanyScopeBeforeCountAndListAndAuditsReturnedObjects() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-VISIBLE", true, 20L);
        FormalFixture hidden = seedFormal(1L, 11L, "ACTIVE", "CURRENT", "CERT-HIDDEN", true, 21L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1).pageSize(10).status("CURRENT").missingFile(false).build(),
                context("REQ-PAGE-001"));

        assertEquals(1L, page.getTotal());
        assertEquals(List.of(visible.certificateId()),
                page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals("Owner A", page.getList().get(0).getOwnerCompanyName());
        assertEquals(1, auditMapper.selectListByCertificateId(visible.certificateId()).size());
        assertEquals(0, auditMapper.selectListByCertificateId(hidden.certificateId()).size());
        assertEquals(1L, queryMapper.countPage(1L, List.of(10L),
                DccRegistrationCertificatePageQuery.builder().status("CURRENT").missingFile(false).build()));
    }

    @Test
    void structuredAndClosedDateFiltersShareTheSameCountAndListPredicate() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-VISIBLE", true, 20L);
        seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-OTHER", false, null);
        seedFormal(1L, 11L, "ACTIVE", "CURRENT", "CERT-HIDDEN", true, 20L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));
        DccRegistrationCertificatePageQuery query = DccRegistrationCertificatePageQuery.builder()
                .pageNo(1).pageSize(10)
                .ownerCompanyId(10L)
                .productMasterId(20L)
                .status("CURRENT")
                .certificateNo("VISIBLE")
                .missingProjectCode(false)
                .missingFile(false)
                .firstObtainedStart(LocalDate.of(2026, 1, 1))
                .firstObtainedEnd(LocalDate.of(2026, 1, 1))
                .approvalStart(LocalDate.of(2026, 2, 1))
                .approvalEnd(LocalDate.of(2026, 2, 1))
                .effectiveStart(LocalDate.of(2026, 9, 1))
                .effectiveEnd(LocalDate.of(2026, 9, 1))
                .expiryStart(LocalDate.of(2031, 9, 1))
                .expiryEnd(LocalDate.of(2031, 9, 1))
                .build();

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, query, context("REQ-FILTER-001"));

        assertEquals(1L, page.getTotal());
        assertEquals(List.of(visible.certificateId()),
                page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals(1L, queryMapper.countPage(1L, List.of(10L), query));
    }

    @Test
    void detailOutsideCompanyScopeReturnsNotFoundAndRecordsFailureWithoutSensitiveFields() {
        FormalFixture hidden = seedFormal(1L, 11L, "ACTIVE", "CURRENT", "CERT-HIDDEN", true, 21L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));

        ServiceException error = assertThrows(ServiceException.class,
                () -> queryService.getDetail(1L, 99L, hidden.certificateId(), context("REQ-HIDDEN-001")));

        assertEquals(REGISTRATION_CERTIFICATE_NOT_EXISTS.getCode(), error.getCode());
        DccRegistrationCertificateAuditDO audit = auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-HIDDEN-001:DETAIL:REQUESTED:" + hidden.certificateId() + ":FAILURE");
        assertNotNull(audit);
        assertNull(audit.getOwnerCompanyId());
        assertNull(audit.getCertificateId());
        assertEquals(hidden.certificateId(), audit.getRequestedCertificateId());
        assertEquals("FAILURE", audit.getResult());
    }

    @Test
    void oldIndexUsesOnlyD004WhitelistAndNeverContainsDetailOrFileFields() {
        FormalFixture old = seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-OLD", true, null);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificateOldIndexItem> page = queryService.getOldIndexPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10).build(),
                context("REQ-OLD-001"));

        assertEquals(1L, page.getTotal());
        DccRegistrationCertificateOldIndexItem item = page.getList().get(0);
        assertEquals(old.certificateId(), item.getCertificateId());
        assertEquals(old.versionId(), item.getVersionId());
        assertEquals("Owner A", item.getOwnerCompanyName());
        assertEquals("Product CERT-OLD", item.getProductName());
        assertEquals("CERT-OLD", item.getCertificateNo());
        assertEquals(LocalDate.of(2031, 9, 1), item.getExpiryDate());

        Set<String> fields = Arrays.stream(DccRegistrationCertificateOldIndexItem.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("certificateId", "versionId", "ownerCompanyId", "ownerCompanyName",
                "productName", "certificateNo", "versionNo", "expiryDate", "status"), fields);
        assertFalse(fields.contains("projectCodeId"));
        assertFalse(fields.contains("registrantName"));
        assertFalse(fields.contains("productionAddress"));
        assertFalse(fields.contains("fileId"));
        assertFalse(fields.contains("previewUrl"));
    }

    @Test
    void currentPageNeverReturnsOldVersionFacts() {
        FormalFixture current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-CURRENT", true, 20L);
        seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-OLD", true, null);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10).build(),
                context("REQ-CURRENT-PAGE-NO-OLD"));

        assertEquals(1L, page.getTotal());
        assertEquals(List.of(current.certificateId()), page.getList().stream()
                .map(DccRegistrationCertificatePageItem::getCertificateId).toList());
    }

    @Test
    void oldDetailRequiresAStillValidOldViewGrant() {
        FormalFixture old = seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-OLD-DETAIL", true, null);
        seedOldViewGrant(old.certificateId(), LocalDateTime.of(2026, 8, 17, 9, 0),
                LocalDateTime.of(2026, 8, 18, 9, 0));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        ServiceException error = assertThrows(ServiceException.class,
                () -> queryService.getDetail(1L, 99L, old.certificateId(), context("REQ-OLD-EXPIRED")));

        assertEquals(REGISTRATION_CERTIFICATE_ACCESS_GRANT_EXPIRED.getCode(), error.getCode());
    }

    @Test
    void oldDetailReturnsAfterValidOldViewGrant() {
        FormalFixture old = seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-OLD-GRANTED", true, null);
        seedOldViewGrant(old.certificateId(), LocalDateTime.of(2026, 8, 19, 8, 0),
                LocalDateTime.of(2100, 1, 1, 0, 0));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        DccRegistrationCertificateDetail detail = queryService.getDetail(
                1L, 99L, old.certificateId(), context("REQ-OLD-GRANTED"));

        assertEquals(old.certificateId(), detail.getCertificateId());
        assertEquals("OLD", detail.getStatus());
    }

    @Test
    void missingEnabledCompanyFactFailsWithoutIdOrSnapshotNameFallback() {
        seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-VISIBLE", true, 20L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any())).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class,
                () -> queryService.getPage(1L, 99L, DccRegistrationCertificatePageQuery.builder().build(),
                        context("REQ-COMPANY-MISSING")));

        assertEquals(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED.getCode(), error.getCode());
    }

    @Test
    void auditFailureBlocksSuccessfulDetailResponse() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-VISIBLE", true, 20L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        DccRegistrationCertificateDetail first = assertDoesNotThrow(() -> queryService.getDetail(
                1L, 99L, visible.certificateId(), context("REQ-DETAIL-DUP")));
        assertEquals(visible.certificateId(), first.getCertificateId());

        ServiceException duplicateAudit = assertThrows(ServiceException.class,
                () -> queryService.getDetail(1L, 99L, visible.certificateId(), context("REQ-DETAIL-DUP")));
        assertEquals(1_080_000_219, duplicateAudit.getCode());
    }

    private FormalFixture seedFormal(Long tenantId, Long ownerCompanyId, String masterStatus, String versionStatus,
                                     String certificateNo, boolean hasFile, Long projectCodeId) {
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(ownerCompanyId)
                .productMasterId(20L)
                .projectCodeId(projectCodeId)
                .firstObtainedDate(LocalDate.of(2026, 1, 1))
                .status(masterStatus)
                .rowVersion(2)
                .build();
        certificate.setTenantId(tenantId);
        assertEquals(1, certificateMapper.insert(certificate));

        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo(certificateNo)
                .approvalDate(LocalDate.of(2026, 2, 1))
                .effectiveDate(LocalDate.of(2026, 9, 1))
                .expiryDate(LocalDate.of(2031, 9, 1))
                .classification("II")
                .categoryChanged(false)
                .status(versionStatus)
                .formalizedAt(java.time.LocalDateTime.of(2026, 8, 17, 9, 0))
                .formalizedBy(99L)
                .build();
        version.setTenantId(tenantId);
        assertEquals(1, versionMapper.insert(version));

        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(version.getId())
                .revisionNo(1)
                .productName("Product " + certificateNo)
                .registrantName("Sensitive Registrant")
                .modelSpecification("Sensitive Model")
                .structureComposition("Sensitive Structure")
                .intendedUse("Sensitive Use")
                .technicalRequirements("Sensitive Requirement")
                .residenceAddress("Sensitive Residence")
                .productionAddress("Sensitive Production")
                .entrustedProduction(true)
                .selfProduction(false)
                .entrustedEnterprisesJson("[{\"enterpriseId\":30,\"enterpriseName\":\"Factory A\"}]")
                .effectiveAt(java.time.LocalDateTime.of(2026, 9, 1, 0, 0))
                .build();
        snapshot.setTenantId(tenantId);
        assertEquals(1, snapshotMapper.insert(snapshot));

        Long registrationFileId = null;
        if (hasFile) {
            DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                    .ownerType("VERSION")
                    .ownerId(version.getId())
                    .fileKind("REGISTRATION_CERTIFICATE")
                    .infraFileId(7000L + version.getId())
                    .originalName("sensitive-" + certificateNo + ".pdf")
                    .mimeType("application/pdf")
                    .fileSize(128L)
                    .sha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                    .status("BOUND")
                    .boundAt(java.time.LocalDateTime.of(2026, 8, 17, 9, 5))
                    .boundBy(99L)
                    .build();
            file.setTenantId(tenantId);
            assertEquals(1, dbFileMapper.insert(file));
            registrationFileId = file.getId();
        }

        certificate.setCurrentVersionId("CURRENT".equals(versionStatus) ? version.getId() : null);
        certificate.setPendingVersionId("PENDING_EFFECTIVE".equals(versionStatus) ? version.getId() : null);
        certificate.setCurrentSnapshotId("CURRENT".equals(versionStatus) ? snapshot.getId() : null);
        assertEquals(1, certificateMapper.updateById(certificate));
        return new FormalFixture(certificate.getId(), version.getId(), snapshot.getId(), registrationFileId);
    }

    private void seedOldViewGrant(Long certificateId, LocalDateTime grantedAt, LocalDateTime expiresAt) {
        DccRegistrationCertificateGrantDO grant = DccRegistrationCertificateGrantDO.builder()
                .requestId(9000L + certificateId)
                .ownerCompanyId(10L)
                .certificateId(certificateId)
                .granteeUserId(99L)
                .grantType("VIEW_OLD_CERTIFICATE")
                .grantKey("query-old:" + certificateId)
                .status("ACTIVE")
                .grantedAt(grantedAt)
                .expiresAt(expiresAt)
                .detailJson("{}")
                .build();
        grant.setTenantId(1L);
        assertEquals(1, grantMapper.insert(grant));
    }

    private static MdmEnterpriseRespDTO owner(Long id, String name) {
        return MdmEnterpriseRespDTO.builder()
                .id(id)
                .tenantId(1L)
                .type("OWNED_COMPANY")
                .status("ENABLED")
                .name(name)
                .build();
    }

    private static DccRequestAuditContext context(String requestId) {
        return new DccRequestAuditContext("10.0.0.1", "JUnit", requestId);
    }

    private record FormalFixture(Long certificateId, Long versionId, Long snapshotId, Long registrationFileId) {
    }
}
