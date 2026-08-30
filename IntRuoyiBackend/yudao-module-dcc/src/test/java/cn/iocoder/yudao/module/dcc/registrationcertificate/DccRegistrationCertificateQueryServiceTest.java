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
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderService;
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
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

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
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_STATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_SORT_INVALID;
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
        DccRegistrationCertificateBusinessClock.class,
        DccRegistrationCertificateReminderService.class,
        DccRegistrationCertificateQueryServiceTest.JdbcTestConfiguration.class
})
class DccRegistrationCertificateQueryServiceTest extends BaseDbUnitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class JdbcTestConfiguration {
        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }

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
    @Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MdmCompanyScopeApi companyScopeApi;
    @MockitoBean
    private MdmEnterpriseApi enterpriseApi;
    @MockitoBean
    private PermissionApi permissionApi;

    @BeforeEach
    void setUp() {
        reset(companyScopeApi, enterpriseApi, permissionApi);
    }

    @Test
    void detailContractExposesFormalRegistrationBusinessFileId() {
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("registrationFileId"),
                "detail response must expose the formal registration business-file id");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("registrationFileName"),
                "detail response must expose the original name of the formal registration file");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("rowVersion"),
                "detail response must expose the server row version for automatic concurrency control");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("snapshotRevision"),
                "detail response must expose the server snapshot revision for automatic concurrency control");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("projectCode"),
                "detail response must expose the formal DCC project code business value");
        assertDoesNotThrow(() -> DccRegistrationCertificatePageItem.class.getDeclaredField("projectCode"),
                "current page response must expose the formal DCC project code business value");
        assertDoesNotThrow(() -> DccRegistrationCertificatePageItem.class.getDeclaredField("classification"),
                "current page response must expose the formal registration certificate category");
        assertDoesNotThrow(() -> DccRegistrationCertificateOldIndexItem.class.getDeclaredField("classification"),
                "old page response must expose the formal registration certificate category");
        assertDoesNotThrow(() -> DccRegistrationCertificatePageItem.class.getDeclaredField("reminderColor"),
                "current page response must expose the registration reminder color");
        assertDoesNotThrow(() -> DccRegistrationCertificatePageItem.class.getDeclaredField("visualState"),
                "current page response must expose the registration reminder state");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("reminderColor"),
                "detail response must expose the registration reminder color");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("visualState"),
                "detail response must expose the registration reminder state");
    }

    @Test
    void detailReturnsFormalRegistrationBusinessFileId() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-FILE-ID", true, 20L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        DccRegistrationCertificateDetail detail = queryService.getDetail(
                1L, 99L, visible.certificateId(), null, context("REQ-DETAIL-FILE-ID"));

        assertEquals(visible.registrationFileId(), detail.getRegistrationFileId(),
                "detail must return the formal business-file id selected by the query mapper");
        assertEquals("sensitive-CERT-FILE-ID.pdf", detail.getRegistrationFileName(),
                "detail must return the original name from the same formal registration file");
        assertEquals(2, detail.getRowVersion(),
                "detail must return the server row version instead of asking the user to type it");
        assertEquals(1, detail.getSnapshotRevision(),
                "detail must return the server snapshot revision instead of asking the user to type it");
        assertEquals("DCC-PROJ-20", detail.getProjectCode(),
                "detail must return the formal project code instead of the project-code database id");
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
        assertEquals("DCC-PROJ-20", page.getList().get(0).getProjectCode());
        assertEquals("II", page.getList().get(0).getClassification());
        assertEquals("Remark CERT-VISIBLE", page.getList().get(0).getRemark());
        assertEquals(1, auditMapper.selectListByCertificateId(visible.certificateId()).size());
        assertEquals(0, auditMapper.selectListByCertificateId(hidden.certificateId()).size());
        assertEquals(1L, queryMapper.countPage(1L, List.of(10L),
                DccRegistrationCertificatePageQuery.builder().status("CURRENT").missingFile(false).build()));
    }

    @Test
    void pageAndDetailExposeReminderVisualStateFromFormalExpiry() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-REMINDER", true, 20L);
        LocalDate expiryDate = LocalDate.now(DccRegistrationCertificateBusinessClock.BUSINESS_ZONE)
                .plusMonths(3);
        assertEquals(1, jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_version
                   SET expiry_date = ?
                 WHERE id = ?
                """, expiryDate, visible.versionId()));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1).pageSize(10).certificateNo("CERT-REMINDER").build(),
                context("REQ-REMINDER-PAGE"));
        DccRegistrationCertificateDetail detail = queryService.getDetail(
                1L, 99L, visible.certificateId(), null, context("REQ-REMINDER-DETAIL"));

        assertEquals("LIGHT", page.getList().get(0).getReminderColor());
        assertEquals("T_8", page.getList().get(0).getVisualState());
        assertEquals("LIGHT", detail.getReminderColor());
        assertEquals("T_8", detail.getVisualState());
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
                .ownerCompanyName("Owner")
                .productName("Product CERT-VISIBLE")
                .classification("II")
                .registrantName("Sensitive Registrant")
                .modelSpecification("Sensitive Model")
                .productionAddress("Sensitive Production")
                .entrustedEnterpriseName("Factory A")
                .projectCode("DCC-PROJ-20")
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
    void currentPageAppliesServerSortFieldAndDirection() {
        FormalFixture lower = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-A-SORT", true, 20L);
        FormalFixture higher = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-Z-SORT", true, 21L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1)
                        .pageSize(10)
                        .sortField("certificateNo")
                        .sortOrder("desc")
                        .build(),
                context("REQ-CURRENT-SORT-CERTIFICATE-NO-DESC"));

        assertEquals(2L, page.getTotal());
        assertEquals(List.of(higher.certificateId(), lower.certificateId()),
                page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
    }

    @Test
    void currentPageAppliesReminderSortAndFilterInFormalQuery() {
        LocalDate businessDate = LocalDate.now(DccRegistrationCertificateBusinessClock.BUSINESS_ZONE);
        FormalFixture normal = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-NONE-REMINDER", true, 20L);
        FormalFixture warning = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-T8-REMINDER", true, 21L);
        FormalFixture urgent = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-T1-REMINDER", true, 22L);
        updateExpiryDate(normal.versionId(), businessDate.plusMonths(40));
        updateExpiryDate(warning.versionId(), businessDate.plusMonths(3));
        updateExpiryDate(urgent.versionId(), businessDate.plusDays(10));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> sortedPage = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1)
                        .pageSize(10)
                        .sortField("reminder")
                        .sortOrder("desc")
                        .build(),
                context("REQ-CURRENT-SORT-REMINDER-DESC"));

        assertEquals(3L, sortedPage.getTotal());
        assertEquals(List.of(urgent.certificateId(), warning.certificateId(), normal.certificateId()),
                sortedPage.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals(List.of("T_1", "T_8", "NONE"),
                sortedPage.getList().stream().map(DccRegistrationCertificatePageItem::getVisualState).toList());

        DccRegistrationCertificatePageQuery filterQuery = DccRegistrationCertificatePageQuery.builder()
                .pageNo(1)
                .pageSize(10)
                .reminderState("T_8")
                .build();
        PageResult<DccRegistrationCertificatePageItem> filteredPage = queryService.getPage(
                1L, 99L, filterQuery, context("REQ-CURRENT-FILTER-REMINDER-T8"));

        assertEquals(1L, filteredPage.getTotal());
        assertEquals(List.of(warning.certificateId()),
                filteredPage.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals(1L, queryMapper.countPage(1L, List.of(10L), filterQuery));
    }

    @Test
    void currentPageReminderNormalFilterIncludesNoneAndClearedStates() {
        LocalDate businessDate = LocalDate.now(DccRegistrationCertificateBusinessClock.BUSINESS_ZONE);
        FormalFixture cleared = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-CLEARED-REMINDER", true, 20L);
        FormalFixture normal = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-NONE-REMINDER", true, 21L);
        FormalFixture warning = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-T8-REMINDER", true, 22L);
        updateExpiryDate(cleared.versionId(), businessDate.plusMonths(3));
        updateExpiryDate(normal.versionId(), businessDate.plusMonths(40));
        updateExpiryDate(warning.versionId(), businessDate.plusMonths(3));
        seedSupportingDocument(cleared, "RENEWAL_ACCEPTANCE_RECEIPT");
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        DccRegistrationCertificatePageQuery query = DccRegistrationCertificatePageQuery.builder()
                .pageNo(1)
                .pageSize(10)
                .reminderState("NORMAL")
                .sortField("certificateNo")
                .sortOrder("asc")
                .build();
        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, query, context("REQ-CURRENT-FILTER-REMINDER-NORMAL"));

        assertEquals(2L, page.getTotal());
        assertEquals(List.of(cleared.certificateId(), normal.certificateId()),
                page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals(List.of("CLEARED", "NONE"),
                page.getList().stream().map(DccRegistrationCertificatePageItem::getVisualState).toList());
        assertEquals(2L, queryMapper.countPage(1L, List.of(10L), query));
    }

    @Test
    void oldIndexAppliesServerSortFieldAndDirection() {
        FormalFixture lower = seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-A-OLD-SORT", true, 20L);
        FormalFixture higher = seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-Z-OLD-SORT", true, 21L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificateOldIndexItem> page = queryService.getOldIndexPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1)
                        .pageSize(10)
                        .sortField("certificateNo")
                        .sortOrder("asc")
                        .build(),
                context("REQ-OLD-SORT-CERTIFICATE-NO-ASC"));

        assertEquals(2L, page.getTotal());
        assertEquals(List.of(lower.certificateId(), higher.certificateId()),
                page.getList().stream().map(DccRegistrationCertificateOldIndexItem::getCertificateId).toList());
    }

    @Test
    void currentPageExecutesEveryDisplayedServerSortFieldAndDirection() {
        seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-A-SORT-BRANCH", true, 20L);
        seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-Z-SORT-BRANCH", false, null);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        List<String> fields = List.of("certificateNo", "ownerCompanyName", "productName", "classification",
                "projectCode", "versionNo", "status", "hasProjectCode", "hasRegistrationFile", "approvalDate",
                "effectiveDate", "expiryDate", "remark");
        for (String field : fields) {
            for (String order : List.of("asc", "desc")) {
                PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                        1L, 99L, DccRegistrationCertificatePageQuery.builder()
                                .pageNo(1)
                                .pageSize(10)
                                .sortField(field)
                                .sortOrder(order)
                                .build(),
                        context("REQ-CURRENT-SORT-" + field + "-" + order));

                assertEquals(2L, page.getTotal(), field + " " + order);
            }
        }
    }

    @Test
    void oldIndexExecutesEveryDisplayedServerSortFieldAndDirection() {
        seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-A-OLD-SORT-BRANCH", true, 20L);
        seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-Z-OLD-SORT-BRANCH", false, 21L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        List<String> fields = List.of("certificateNo", "ownerCompanyName", "productName", "classification",
                "versionNo", "status", "expiryDate");
        for (String field : fields) {
            for (String order : List.of("asc", "desc")) {
                PageResult<DccRegistrationCertificateOldIndexItem> page = queryService.getOldIndexPage(
                        1L, 99L, DccRegistrationCertificatePageQuery.builder()
                                .pageNo(1)
                                .pageSize(10)
                                .sortField(field)
                                .sortOrder(order)
                                .build(),
                        context("REQ-OLD-SORT-" + field + "-" + order));

                assertEquals(2L, page.getTotal(), field + " " + order);
            }
        }
    }

    @Test
    void invalidSortFailsFastWithoutDefaultOrderFallback() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> queryService.getPage(1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1)
                        .pageSize(10)
                        .sortField("visualState")
                        .sortOrder("asc")
                        .build(), context("REQ-CURRENT-SORT-INVALID")));

        assertEquals(REGISTRATION_CERTIFICATE_SORT_INVALID.getCode(), error.getCode());
    }

    @Test
    void invalidReminderStateFailsFastWithoutIgnoringFilter() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> queryService.getPage(1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1)
                        .pageSize(10)
                        .reminderState("UNKNOWN")
                        .build(), context("REQ-CURRENT-REMINDER-INVALID")));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_STATE_INVALID.getCode(), error.getCode());
    }

    @Test
    void oldIndexRejectsReminderStateWithoutSilentIgnore() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> queryService.getOldIndexPage(1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1)
                        .pageSize(10)
                        .reminderState("T_8")
                        .build(), context("REQ-OLD-REMINDER-UNSUPPORTED")));

        assertEquals(REGISTRATION_CERTIFICATE_REMINDER_STATE_INVALID.getCode(), error.getCode());
    }

    @Test
    void detailOutsideCompanyScopeReturnsNotFoundAndRecordsFailureWithoutSensitiveFields() {
        FormalFixture hidden = seedFormal(1L, 11L, "ACTIVE", "CURRENT", "CERT-HIDDEN", true, 21L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));

        ServiceException error = assertThrows(ServiceException.class,
                () -> queryService.getDetail(1L, 99L, hidden.certificateId(), null, context("REQ-HIDDEN-001")));

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
        assertEquals("II", item.getClassification());
        assertEquals("CERT-OLD", item.getCertificateNo());
        assertEquals(LocalDate.of(2031, 9, 1), item.getExpiryDate());

        Set<String> fields = Arrays.stream(DccRegistrationCertificateOldIndexItem.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("certificateId", "versionId", "ownerCompanyId", "ownerCompanyName",
                "productMasterId", "productName", "projectCodeId", "projectCode",
                "classification", "certificateNo", "versionNo", "expiryDate", "status"), fields);
        assertFalse(fields.contains("registrantName"));
        assertFalse(fields.contains("productionAddress"));
        assertFalse(fields.contains("fileId"));
        assertFalse(fields.contains("previewUrl"));
    }

    @Test
    void oldIndexAppliesFirstObtainedEffectiveAndExpiryDateRangeFilters() {
        FormalFixture visible = seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-OLD-DATE-VISIBLE", true, 20L);
        FormalFixture outside = seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-OLD-DATE-OUTSIDE", true, 20L);
        assertEquals(1, jdbcTemplate.update("""
                UPDATE dcc_registration_certificate
                   SET first_obtained_date = ?
                 WHERE tenant_id = 1 AND id = ?
                """, LocalDate.of(2025, 1, 1), outside.certificateId()));
        assertEquals(1, jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_version
                   SET effective_date = ?, expiry_date = ?
                 WHERE tenant_id = 1 AND id = ?
                """, LocalDate.of(2027, 9, 1), LocalDate.of(2031, 9, 1), outside.versionId()));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));
        DccRegistrationCertificatePageQuery query = DccRegistrationCertificatePageQuery.builder()
                .pageNo(1).pageSize(10)
                .firstObtainedStart(LocalDate.of(2026, 1, 1))
                .firstObtainedEnd(LocalDate.of(2026, 1, 1))
                .effectiveStart(LocalDate.of(2026, 9, 1))
                .effectiveEnd(LocalDate.of(2026, 9, 1))
                .expiryStart(LocalDate.of(2031, 9, 1))
                .expiryEnd(LocalDate.of(2031, 9, 1))
                .build();

        PageResult<DccRegistrationCertificateOldIndexItem> page = queryService.getOldIndexPage(
                1L, 99L, query, context("REQ-OLD-DATE-FILTER"));

        assertEquals(1L, page.getTotal());
        assertEquals(List.of(visible.certificateId()), page.getList().stream()
                .map(DccRegistrationCertificateOldIndexItem::getCertificateId).toList());
        assertEquals(1L, queryMapper.countOldIndex(1L, List.of(10L), query));
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
    void currentPageShowsApprovedPendingRenewalAsTheSingleCurrentRow() {
        FormalFixture current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-CURRENT", true, 20L);
        FormalFixture renewal = seedPendingRenewal(current.certificateId(), current.snapshotId());
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10).build(),
                context("REQ-CURRENT-PAGE-PENDING-RENEWAL"));

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        DccRegistrationCertificatePageItem item = page.getList().get(0);
        assertEquals(current.certificateId(), item.getCertificateId());
        assertEquals(renewal.versionId(), item.getVersionId());
        assertEquals("PENDING_EFFECTIVE", item.getStatus());
        assertEquals(LocalDate.of(2026, 8, 1), item.getApprovalDate());
        assertEquals(LocalDate.of(2026, 10, 1), item.getEffectiveDate());
        assertEquals(LocalDate.of(2031, 10, 1), item.getExpiryDate());
    }

    @Test
    void currentPageShowsApprovedInitialUploadWaitingForFirstEffectiveDate() {
        FormalFixture pendingInitial = seedFormal(
                1L, 10L, "PENDING_FIRST_EFFECTIVE", "PENDING_EFFECTIVE",
                "CERT-UPLOAD-FUTURE", true, null);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10).build(),
                context("REQ-CURRENT-PAGE-PENDING-FIRST"));

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        DccRegistrationCertificatePageItem item = page.getList().get(0);
        assertEquals(pendingInitial.certificateId(), item.getCertificateId());
        assertEquals(pendingInitial.versionId(), item.getVersionId());
        assertEquals("PENDING_EFFECTIVE", item.getStatus());
        assertEquals(LocalDate.of(2026, 9, 1), item.getEffectiveDate());
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
                () -> queryService.getDetail(1L, 99L, old.certificateId(), null, context("REQ-OLD-EXPIRED")));

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
                1L, 99L, old.certificateId(), null, context("REQ-OLD-GRANTED"));

        assertEquals(old.certificateId(), detail.getCertificateId());
        assertEquals("OLD", detail.getStatus());
    }

    @Test
    void detailReturnsRequestedOldVersionWhenVersionIdIsProvided() {
        FormalFixture current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-CURRENT-WITH-OLD", true, 20L);
        FormalFixture old = seedOldVersionOnCertificate(current, "CERT-OLD-SPECIFIC-VERSION");
        seedOldViewGrant(current.certificateId(), LocalDateTime.of(2026, 8, 19, 8, 0),
                LocalDateTime.of(2100, 1, 1, 0, 0));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        DccRegistrationCertificateDetail defaultDetail = queryService.getDetail(
                1L, 99L, current.certificateId(), null, context("REQ-DETAIL-DEFAULT-CURRENT"));
        DccRegistrationCertificateDetail oldDetail = queryService.getDetail(
                1L, 99L, current.certificateId(), old.versionId(), context("REQ-DETAIL-SPECIFIC-OLD"));

        assertEquals(current.versionId(), defaultDetail.getVersionId());
        assertEquals("CURRENT", defaultDetail.getStatus());
        assertEquals(old.versionId(), oldDetail.getVersionId());
        assertEquals("OLD", oldDetail.getStatus());
        assertEquals("CERT-OLD-SPECIFIC-VERSION", oldDetail.getCertificateNo());
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
                1L, 99L, visible.certificateId(), null, context("REQ-DETAIL-DUP")));
        assertEquals(visible.certificateId(), first.getCertificateId());

        ServiceException duplicateAudit = assertThrows(ServiceException.class,
                () -> queryService.getDetail(1L, 99L, visible.certificateId(), null, context("REQ-DETAIL-DUP")));
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
        seedProjectCode(tenantId, projectCodeId);
        seedMdmEnterprise(tenantId, ownerCompanyId);

        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo(certificateNo)
                .approvalDate(LocalDate.of(2026, 2, 1))
                .effectiveDate(LocalDate.of(2026, 9, 1))
                .expiryDate(LocalDate.of(2031, 9, 1))
                .classification("II")
                .remark("Remark " + certificateNo)
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
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_snapshot_entrusted
                    (snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id, deleted)
                VALUES (?, ?, ?, ?, ?, 0)
                """, snapshot.getId(), 30L, "Factory A", 1, tenantId));

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

    private void updateExpiryDate(Long versionId, LocalDate expiryDate) {
        assertEquals(1, jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_version
                   SET expiry_date = ?
                 WHERE tenant_id = 1 AND id = ?
                """, expiryDate, versionId));
    }

    private void seedSupportingDocument(FormalFixture fixture, String documentType) {
        Long ownerCompanyId = jdbcTemplate.queryForObject("""
                SELECT owner_company_id
                  FROM dcc_registration_certificate
                 WHERE tenant_id = 1 AND id = ?
                """, Long.class, fixture.certificateId());
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_supporting_document
                    (tenant_id, owner_company_id, certificate_id, version_id, business_file_id,
                     document_type, status, uploaded_at, uploaded_by, confirmed_at, confirmed_by, creator, deleted)
                VALUES (1, ?, ?, ?, ?, ?, 'EFFECTIVE', ?, 99, ?, 99, 'junit', 0)
                """, ownerCompanyId, fixture.certificateId(), fixture.versionId(),
                900000L + fixture.versionId(), documentType,
                LocalDateTime.of(2026, 8, 17, 9, 0), LocalDateTime.of(2026, 8, 17, 9, 5)));
    }

    private FormalFixture seedPendingRenewal(Long certificateId, Long baseSnapshotId) {
        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificateId)
                .versionNo(2)
                .versionType("RENEWAL_CERTIFICATE")
                .certificateNo("CERT-RENEWED")
                .approvalDate(LocalDate.of(2026, 8, 1))
                .effectiveDate(LocalDate.of(2026, 10, 1))
                .expiryDate(LocalDate.of(2031, 10, 1))
                .classification("II")
                .remark("Renewal remark")
                .categoryChanged(false)
                .baseSnapshotId(baseSnapshotId)
                .status("PENDING_EFFECTIVE")
                .formalizedAt(java.time.LocalDateTime.of(2026, 8, 20, 9, 0))
                .formalizedBy(200L)
                .build();
        version.setTenantId(1L);
        assertEquals(1, versionMapper.insert(version));

        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(version.getId())
                .revisionNo(1)
                .productName("Product CERT-RENEWED")
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
                .effectiveAt(java.time.LocalDateTime.of(2026, 10, 1, 0, 0))
                .build();
        snapshot.setTenantId(1L);
        assertEquals(1, snapshotMapper.insert(snapshot));
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_snapshot_entrusted
                    (snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id, deleted)
                VALUES (?, ?, ?, ?, ?, 0)
                """, snapshot.getId(), 30L, "Factory A", 1, 1L));
        assertEquals(1, jdbcTemplate.update("""
                UPDATE dcc_registration_certificate
                   SET pending_version_id = ?, row_version = row_version + 1
                 WHERE tenant_id = 1 AND id = ?
                """, version.getId(), certificateId));
        return new FormalFixture(certificateId, version.getId(), snapshot.getId(), null);
    }

    private FormalFixture seedOldVersionOnCertificate(FormalFixture current, String certificateNo) {
        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(current.certificateId())
                .versionNo(2)
                .versionType("RENEWAL_CERTIFICATE")
                .certificateNo(certificateNo)
                .approvalDate(LocalDate.of(2021, 8, 25))
                .effectiveDate(LocalDate.of(2021, 8, 25))
                .expiryDate(LocalDate.of(2026, 8, 25))
                .classification("II")
                .remark("Old remark " + certificateNo)
                .categoryChanged(false)
                .status("OLD")
                .baseSnapshotId(current.snapshotId())
                .formalizedAt(java.time.LocalDateTime.of(2021, 8, 25, 9, 0))
                .formalizedBy(99L)
                .build();
        version.setTenantId(1L);
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
                .effectiveAt(java.time.LocalDateTime.of(2021, 8, 25, 0, 0))
                .build();
        snapshot.setTenantId(1L);
        assertEquals(1, snapshotMapper.insert(snapshot));
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_snapshot_entrusted
                    (snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id, deleted)
                VALUES (?, ?, ?, ?, ?, 0)
                """, snapshot.getId(), 30L, "Factory A", 1, 1L));
        return new FormalFixture(current.certificateId(), version.getId(), snapshot.getId(), null);
    }

    private void seedProjectCode(Long tenantId, Long projectCodeId) {
        if (projectCodeId == null || exists("dcc_project_code", tenantId, projectCodeId)) {
            return;
        }
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_project_code
                    (id, product_master_id, project_name, project_code, status, tenant_id, deleted)
                VALUES (?, ?, ?, ?, 'ENABLED', ?, 0)
                """, projectCodeId, 20L, "Project " + projectCodeId, "DCC-PROJ-" + projectCodeId, tenantId));
    }

    private void seedMdmEnterprise(Long tenantId, Long ownerCompanyId) {
        if (exists("mdm_enterprise", tenantId, ownerCompanyId)) {
            return;
        }
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO mdm_enterprise
                    (id, enterprise_code, name, type, status, tenant_id, deleted)
                VALUES (?, ?, ?, 'OWNED_COMPANY', 'ENABLED', ?, 0)
                """, ownerCompanyId, "OWNER-" + ownerCompanyId, "Owner A", tenantId));
    }

    private boolean exists(String tableName, Long tenantId, Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE tenant_id = ? AND id = ?",
                Integer.class, tenantId, id);
        return count != null && count > 0;
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
