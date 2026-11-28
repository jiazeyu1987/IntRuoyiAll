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
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateOperationAuditService;
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
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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
import java.util.Collection;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        DccRegistrationCertificateQueryServiceImpl.class,
        DccRegistrationCertificateOperationAuditService.class,
        DccRegistrationCertificateReadAuditService.class,
        DccRegistrationCertificateAccessPolicyService.class,
        DccRegistrationCertificateBusinessClock.class,
        DccRegistrationCertificateReminderService.class,
        DccRegistrationCertificateQueryServiceTest.JdbcTestConfiguration.class
})
public class DccRegistrationCertificateQueryServiceTest extends BaseDbUnitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class JdbcTestConfiguration {
        @Bean
        H2TextCastInterceptor h2TextCastInterceptor() { return new H2TextCastInterceptor(); }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        org.apache.ibatis.plugin.Interceptor mysqlTextCasts() {
            return new MysqlTextCasts();
        }
    }

    /** H2 truncates unqualified CHAR casts to one character; MySQL preserves the full text. */
    @org.apache.ibatis.plugin.Intercepts(@org.apache.ibatis.plugin.Signature(
            type = org.apache.ibatis.executor.statement.StatementHandler.class, method = "prepare",
            args = {java.sql.Connection.class, Integer.class}))
    static class MysqlTextCasts implements org.apache.ibatis.plugin.Interceptor {
        @Override
        public Object intercept(org.apache.ibatis.plugin.Invocation invocation) throws Throwable {
            var handler = (org.apache.ibatis.executor.statement.StatementHandler) invocation.getTarget();
            var bound = handler.getBoundSql();
            org.apache.ibatis.reflection.SystemMetaObject.forObject(bound).setValue("sql",
                    bound.getSql().replace(" AS CHAR)", " AS VARCHAR)"));
            return invocation.proceed();
        }
    }

    // H2 CHAR defaults to length 1, unlike MySQL's unbounded CAST AS CHAR.
    @org.apache.ibatis.plugin.Intercepts(@org.apache.ibatis.plugin.Signature(
            type = org.apache.ibatis.executor.statement.StatementHandler.class,
            method = "prepare", args = {java.sql.Connection.class, Integer.class}))
    public static class H2TextCastInterceptor implements org.apache.ibatis.plugin.Interceptor {
        @Override
        public Object intercept(org.apache.ibatis.plugin.Invocation invocation) throws Throwable {
            var handler = (org.apache.ibatis.executor.statement.StatementHandler) invocation.getTarget();
            var sql = handler.getBoundSql();
            org.apache.ibatis.reflection.SystemMetaObject.forObject(sql).setValue("sql",
                    sql.getSql().replace(" AS CHAR)", " AS VARCHAR)"));
            return invocation.proceed();
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
    @MockitoBean
    private AdminUserApi adminUserApi;

    @Test
    void rejectsAllReversedDateRangesAsBusinessParameterErrors() throws Exception {
        for (String field : List.of("CeIssue", "Listing", "FirstObtained", "Approval", "Effective", "Expiry")) {
            DccRegistrationCertificatePageQuery query = DccRegistrationCertificatePageQuery.builder().build();
            query.getClass().getMethod("set" + field + "Start", LocalDate.class)
                    .invoke(query, LocalDate.of(2026, 2, 2));
            query.getClass().getMethod("set" + field + "End", LocalDate.class)
                    .invoke(query, LocalDate.of(2026, 2, 1));
            ServiceException error = assertThrows(ServiceException.class,
                    () -> queryService.getPage(1L, 99L, query, context("REVERSED-DATE")));
            assertEquals(400, error.getCode());
            assertEquals("日期起始不能晚于截止", error.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP ALIAS IF EXISTS JSON_CONTAINS");
        jdbcTemplate.execute("CREATE ALIAS JSON_CONTAINS FOR 'cn.iocoder.yudao.module.dcc.registrationcertificate.DccRegistrationCertificateQueryServiceTest.jsonContains'");
        jdbcTemplate.execute("DROP ALIAS IF EXISTS JSON_QUOTE");
        jdbcTemplate.execute("CREATE ALIAS JSON_QUOTE FOR 'cn.iocoder.yudao.module.dcc.registrationcertificate.DccRegistrationCertificateQueryServiceTest.jsonQuote'");
        jdbcTemplate.execute("DROP ALIAS IF EXISTS JSON_EXTRACT");
        jdbcTemplate.execute("CREATE ALIAS JSON_EXTRACT FOR 'cn.iocoder.yudao.module.dcc.registrationcertificate.DccRegistrationCertificateQueryServiceTest.jsonExtract'");
        jdbcTemplate.execute("CREATE ALIAS IF NOT EXISTS JSON_LENGTH FOR 'cn.iocoder.yudao.module.dcc.registrationcertificate.DccRegistrationCertificateQueryServiceTest.jsonLength'");
        jdbcTemplate.execute("CREATE ALIAS IF NOT EXISTS JSON_TYPE FOR 'cn.iocoder.yudao.module.dcc.registrationcertificate.DccRegistrationCertificateQueryServiceTest.jsonType'");
        reset(companyScopeApi, enterpriseApi, permissionApi, adminUserApi);
        when(adminUserApi.getUserList(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.stream().map(DccRegistrationCertificateQueryServiceTest::user).toList();
        });
    }

    @Test
    void certificateTypeColumnFiltersCurrentOldIndexAndExportByDisplayedLabel() {
        var currentIds = new java.util.HashMap<String, Long>();
        var oldIds = new java.util.HashMap<String, Long>();
        for (String type : List.of("CE", "FDA", "DOMESTIC")) {
            var current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "TYPE-" + type, true, 20L);
            var old = seedOldVersionOnCertificate(current, "OLD-TYPE-" + type);
            for (var fixture : List.of(current, old)) {
                var snapshot = snapshotMapper.selectById(fixture.snapshotId());
                var facts = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(snapshot.getFactsJson(),
                        cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.class);
                facts.setCertificateType(type);
                jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE id=?",
                        facts.toJson(), fixture.snapshotId());
            }
            currentIds.put(type, current.versionId());
            oldIds.put(type, old.versionId());
        }
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));
        for (String type : List.of("CE", "FDA", "DOMESTIC")) {
            String label = "DOMESTIC".equals(type) ? "国内" : type;
            var query = DccRegistrationCertificatePageQuery.builder()
                    .listFilters("[{\"key\":\"certificateType\",\"operator\":\"contains\",\"value\":\"" + label + "\"}]")
                    .build();
            var page = queryService.getPage(1L, 99L, query, context("TYPE-CURRENT-" + type));
            assertEquals(1L, page.getTotal());
            assertEquals(List.of(currentIds.get(type)), page.getList().stream().map(DccRegistrationCertificatePageItem::getVersionId).toList());
            var oldPage = queryService.getOldIndexPage(1L, 99L, query, context("TYPE-OLD-" + type));
            assertEquals(1L, oldPage.getTotal());
            assertEquals(List.of(oldIds.get(type)), oldPage.getList().stream().map(DccRegistrationCertificateOldIndexItem::getVersionId).toList());
            assertEquals(type, oldPage.getList().get(0).getCertificateType());
            assertEquals(List.of(currentIds.get(type)), queryService.getExportRows(1L, 99L, query, null, 10)
                    .stream().map(DccRegistrationCertificatePageItem::getVersionId).toList());
        }
    }

    @Test
    void exportUsesListSortSelectedVersionIdsAndPersistsBatchAudit() throws Exception {
        var b = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "EXPORT-B", false, 20L);
        var a = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "EXPORT-A", false, 20L);
        seedFormal(2L, 12L, "ACTIVE", "CURRENT", "EXPORT-OTHER-TENANT", false, 22L);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any())).thenReturn(List.of(owner(10L,"Owner A")));
        var query = DccRegistrationCertificatePageQuery.builder().certificateNo("EXPORT-").sortField("certificateNo").sortOrder("asc").build();
        var rows = queryService.getExportRows(1L,99L,query,List.of(b.versionId(),a.versionId()),10);
        assertEquals(List.of(a.versionId(),b.versionId()),rows.stream().map(DccRegistrationCertificatePageItem::getVersionId).toList());
        var desc = DccRegistrationCertificatePageQuery.builder().sortField("certificateNo").sortOrder("desc").build();
        assertEquals(List.of(b.versionId(),a.versionId()),queryService.getExportRows(1L,99L,desc,null,10).stream().map(DccRegistrationCertificatePageItem::getVersionId).toList());
        assertThrows(DccRegistrationCertificateQueryService.ExportLimitExceeded.class,()->queryService.getExportRows(1L,99L,DccRegistrationCertificatePageQuery.builder().build(),null,1));
        when(permissionApi.hasAnyPermissions(eq(99L),any(String[].class))).thenReturn(true);
        var exports = new cn.iocoder.yudao.module.dcc.registrationcertificate.service.export.DccRegistrationCertificateExportService(
                queryService,auditMapper,permissionApi,new DccRegistrationCertificateBusinessClock());
        var req = new cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.query.vo.DccRegistrationCertificatePageReqVO();
        req.setSortField("certificateNo");req.setSortOrder("asc");
        var p=exports.preview(1L,99L,new cn.iocoder.yudao.module.dcc.registrationcertificate.service.export.DccRegistrationCertificateExportService.Scope("current","SELECTED",req,List.of(b.versionId(),a.versionId())));
        exports.generate(1L,99L,p.snapshotId(),p.requestId());
        exports.generate(1L,99L,p.snapshotId(),p.requestId());
        var audits=auditMapper.selectListByCertificateId(a.certificateId());
        assertEquals(1,audits.size());assertEquals("EXCEL_GENERATED",audits.get(0).getEventType());
        assertTrue(audits.get(0).getDetailJson().contains("exported_record_ids"));
        try(var stream=exports.open(1L,99L,p.requestId())){assertEquals(2,stream.status().exported());}
        jdbcTemplate.update("UPDATE dcc_registration_certificate SET deleted=1 WHERE id=?",a.certificateId());
        assertThrows(ServiceException.class,()->exports.open(1L,99L,p.requestId()));
        assertEquals("INVALID",exports.status(1L,99L,p.requestId()).state());
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
        assertDoesNotThrow(() -> DccRegistrationCertificatePageItem.class.getDeclaredField("hasPendingRenewal"),
                "current page response must expose whether renewal submission is currently blocked");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("reminderColor"),
                "detail response must expose the registration reminder color");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("visualState"),
                "detail response must expose the registration reminder state");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("uploadOperatorName"),
                "detail response must expose the formal initial upload operator name");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("uploadedAt"),
                "detail response must expose the formal initial upload time");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("uploadApproverName"),
                "detail response must expose the formal initial upload approver name");
        assertDoesNotThrow(() -> DccRegistrationCertificateDetail.class.getDeclaredField("uploadApprovedAt"),
                "detail response must expose the formal initial upload approval time");
    }

    @Test
    void ceNewAndOldVersionIdentityUsesTheirOwnLatestRevWhenGenericNumberIsEmpty() {
        var fixture=seedFormal(1L,10L,"ACTIVE","CURRENT","CE-EMPTY",true,20L);
        var facts=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(snapshotMapper.selectById(fixture.snapshotId()).getFactsJson());
        facts.setCertificateType("CE");facts.setCertificateNo(null);
        var rev=new cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.CeCertificate();
        rev.setRevision("R04");rev.setCertificateNo("SIMULATION-CE-R04");facts.setCeCertificates(List.of(rev));
        jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE id=?",facts.toJson(),fixture.snapshotId());
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)),any())).thenReturn(List.of(owner(10L,"Owner A")));
        assertEquals("SIMULATION-CE-R04",queryService.getDetail(1L,99L,fixture.certificateId(),null,context("CE-IDENTITY")).getCertificateNo());
        var query=DccRegistrationCertificatePageQuery.builder().certificateNo("SIMULATION-CE-R04").build();
        assertEquals(1,queryService.getPage(1L,99L,query,context("CE-IDENTITY-PAGE")).getTotal());
        jdbcTemplate.update("UPDATE dcc_registration_certificate_version SET status='OLD' WHERE id=?",fixture.versionId());
        assertEquals("SIMULATION-CE-R04",queryService.getOldIndexPage(1L,99L,query,context("CE-IDENTITY-OLD")).getList().get(0).getCertificateNo());
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
    void sharedCertificateCanBeQueriedByItsSecondProjectAndProductMaster() {
        var fixture=seedFormal(1L,10L,"ACTIVE","CURRENT","SHARED-QUERY",true,20L);
        seedProjectCode(1L,21L);jdbcTemplate.update("UPDATE dcc_project_code SET product_master_id=99 WHERE id=21 AND tenant_id=1");
        var facts=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(snapshotMapper.selectById(fixture.snapshotId()).getFactsJson());
        var second=new cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProjectProduct(21L,664L,"P664","产品乙",List.of());
        facts.setProjectProducts(List.of(facts.getProjectProduct(),second));
        jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE id=?",facts.toJson(),fixture.snapshotId());
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)),any())).thenReturn(List.of(owner(10L,"Owner A")));
        for(var query:List.of(DccRegistrationCertificatePageQuery.builder().projectCodeId(21L).build(),DccRegistrationCertificatePageQuery.builder().productMasterId(99L).build())){
            var page=queryService.getPage(1L,99L,query,context("SHARED-"+query.hashCode()));
            assertEquals(1L,page.getTotal());assertEquals(fixture.certificateId(),page.getList().get(0).getCertificateId());
            assertEquals("DCC-PROJ-20、DCC-PROJ-21",page.getList().get(0).getProjectCode());
        }
    }

    @Test
    void pageListsTenantCurrentCertificatesWithoutCompanyScopeAndAuditsReturnedObjects() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-VISIBLE", true, 20L);
        FormalFixture otherCompany = seedFormal(1L, 11L, "ACTIVE", "CURRENT", "CERT-OTHER-COMPANY", true, 21L);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L, 11L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A"), owner(11L, "Owner B")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1).pageSize(10).status("CURRENT").missingFile(false).build(),
                context("REQ-PAGE-001"));

        assertEquals(2L, page.getTotal());
        assertEquals(List.of(visible.certificateId(), otherCompany.certificateId()),
                page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals("Owner A", page.getList().get(0).getOwnerCompanyName());
        assertEquals("DCC-PROJ-20", page.getList().get(0).getProjectCode());
        assertEquals("II", page.getList().get(0).getClassification());
        assertEquals("Remark CERT-VISIBLE", page.getList().get(0).getRemark());
        assertFalse(page.getList().get(0).getHasPendingRenewal(),
                "a current certificate without an open renewal must remain available for renewal submission");
        assertEquals(1, auditMapper.selectListByCertificateId(visible.certificateId()).size());
        assertEquals(1, auditMapper.selectListByCertificateId(otherCompany.certificateId()).size());
        assertEquals(2L, queryMapper.countPage(1L, List.of(),
                DccRegistrationCertificatePageQuery.builder().status("CURRENT").missingFile(false).build()));
        verify(companyScopeApi, never()).getEnabledCompanyIdsForUser(99L);
    }

    @Test
    void currentPageUsesCurrentSnapshotAndDoesNotExpandHistoricalSnapshots() throws Exception {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-SNAPSHOT-UNIQUE", true, 20L);
        DccRegistrationCertificateSnapshotDO historical = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(visible.versionId())
                .revisionNo(2)
                .productName("Historical Product")
                .registrantName("Historical Registrant")
                .modelSpecification("Historical Model")
                .structureComposition("Historical Structure")
                .intendedUse("Historical Use")
                .technicalRequirements("Historical Requirement")
                .residenceAddress("Historical Residence")
                .productionAddress("Historical Production")
                .entrustedProduction(false)
                .selfProduction(true)
                .entrustedEnterprisesJson("[]")
                .effectiveAt(LocalDateTime.of(2026, 9, 2, 0, 0))
                .build();
        historical.setTenantId(1L);
        historical.setFactsJson(DccRegistrationCertificateTestFacts.domestic(jdbcTemplate, historical.getVersionId()));
        assertEquals(1, snapshotMapper.insert(historical));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1).pageSize(10).certificateNo("CERT-SNAPSHOT-UNIQUE").build(),
                context("REQ-SNAPSHOT-UNIQUE"));

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(visible.certificateId(), page.getList().get(0).getCertificateId());
        assertEquals(visible.snapshotId(), page.getList().get(0).getSnapshotId());
        assertEquals("Product CERT-SNAPSHOT-UNIQUE", page.getList().get(0).getProductName());
        Object pendingState = page.getList().get(0).getClass()
                .getMethod("getHasPendingChange").invoke(page.getList().get(0));
        assertEquals(Boolean.FALSE, pendingState);
        assertEquals(1L, queryMapper.countPage(1L, List.of(10L),
                DccRegistrationCertificatePageQuery.builder()
                        .certificateNo("CERT-SNAPSHOT-UNIQUE").build()));
    }

    @Test
    void currentPageMarksCertificateWithPendingChange() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-PENDING-CHANGE", true, 20L);
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_lifecycle_event
                  (tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                   source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                   baseline_row_version, baseline_snapshot_revision, actor_id, detail_json, occurred_at, creator)
                VALUES (1, 10, ?, ?, ?, ?, ?, 'pending-change-query', 'CHANGE_SUBMITTED', 1,
                        2, 1, 99, '{}', ?, '99')
                """, visible.certificateId(), visible.versionId(), visible.versionId(),
                visible.snapshotId(), visible.snapshotId(), LocalDateTime.of(2026, 9, 2, 10, 0)));
        Long eventId = jdbcTemplate.queryForObject(
                "SELECT id FROM dcc_registration_certificate_lifecycle_event WHERE event_key = 'pending-change-query'",
                Long.class);
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_change
                  (tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                   resulting_snapshot_id, event_id, approval_date, selected_change_types_json,
                   selected_item_count, status, actor_id)
                VALUES (1, 10, ?, ?, ?, NULL, ?, DATE '2026-09-02', '[\"PRODUCT_NAME\"]',
                        1, 'PENDING_APPROVAL', 99)
                """, visible.certificateId(), visible.versionId(), visible.snapshotId(), eventId));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder()
                        .pageNo(1).pageSize(10).certificateNo("CERT-PENDING-CHANGE").build(),
                context("REQ-PENDING-CHANGE"));

        assertEquals(1, page.getList().size());
        assertTrue(page.getList().get(0).getHasPendingChange());
    }

    @Test
    void currentDetailUsesCertificateCurrentSnapshotWhenVersionHasHistory() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-DETAIL-SNAPSHOT", true, 20L);
        DccRegistrationCertificateSnapshotDO historical = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(visible.versionId())
                .revisionNo(2)
                .productName("Historical Detail Product")
                .registrantName("Historical Detail Registrant")
                .modelSpecification("Historical Detail Model")
                .structureComposition("Historical Detail Structure")
                .intendedUse("Historical Detail Use")
                .technicalRequirements("Historical Detail Requirement")
                .residenceAddress("Historical Detail Residence")
                .productionAddress("Historical Detail Production")
                .entrustedProduction(false)
                .selfProduction(true)
                .entrustedEnterprisesJson("[]")
                .effectiveAt(LocalDateTime.of(2026, 9, 2, 0, 0))
                .build();
        historical.setTenantId(1L);
        historical.setFactsJson(DccRegistrationCertificateTestFacts.domestic(jdbcTemplate, historical.getVersionId()));
        assertEquals(1, snapshotMapper.insert(historical));
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        DccRegistrationCertificateDetail detail = queryService.getDetail(
                1L, 99L, visible.certificateId(), null, context("REQ-DETAIL-SNAPSHOT"));

        assertEquals(visible.snapshotId(), detail.getSnapshotId());
        assertEquals("Product CERT-DETAIL-SNAPSHOT", detail.getProductName());
        assertEquals("Sensitive Registrant", detail.getRegistrantName());
    }

    @Test
    void pageReadAuditDuplicateDoesNotBlockRepeatedListRefresh() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-REFRESH-AUDIT", true, 20L);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));
        DccRegistrationCertificatePageQuery query = DccRegistrationCertificatePageQuery.builder()
                .pageNo(1).pageSize(10).certificateNo("CERT-REFRESH-AUDIT").build();
        DccRequestAuditContext auditContext = context("REQ-PAGE-REFRESH-DUP");

        PageResult<DccRegistrationCertificatePageItem> first = queryService.getPage(
                1L, 99L, query, auditContext);
        PageResult<DccRegistrationCertificatePageItem> second = assertDoesNotThrow(() -> queryService.getPage(
                1L, 99L, query, auditContext));

        assertEquals(List.of(visible.certificateId()),
                first.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals(List.of(visible.certificateId()),
                second.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals(1, auditMapper.selectListByCertificateId(visible.certificateId()).size(),
                "重复列表刷新应复用同一读审计事件，不应创建重复事件或阻断页面");
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
        refreshFacts(visible.versionId());
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
    void ceReminderFiltersAndDetailsCannotBeClearedBySupportingMaterials() {
        LocalDate today = LocalDate.now(DccRegistrationCertificateBusinessClock.BUSINESS_ZONE);
        FormalFixture warning = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CE-WARNING", true, 20L);
        FormalFixture urgent = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CE-URGENT", true, 21L);
        FormalFixture notice = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CE-NOTICE", true, 22L);
        for (FormalFixture fixture : List.of(warning, urgent, notice)) {
            updateExpiryDate(fixture.versionId(), today.plusMonths(fixture == warning ? 10 : fixture == urgent ? 2 : 15));
            var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts
                    .parse(snapshotMapper.selectById(fixture.snapshotId()).getFactsJson());
            facts.setCertificateType("CE");
            jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE id=?",
                    facts.toJson(), fixture.snapshotId());
        }
        FormalFixture oldWarningVersion = seedOldVersionOnCertificate(warning, "CE-WARNING-OLD");
        seedSupportingDocument(warning, "RENEWAL_ACCEPTANCE_RECEIPT");
        jdbcTemplate.update("UPDATE dcc_registration_certificate_supporting_document SET version_id=? WHERE certificate_id=?",
                oldWarningVersion.versionId(), warning.certificateId());
        seedSupportingDocument(urgent, "RENEWAL_SUPPLEMENT_NOTICE");
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));
        var page = queryService.getPage(1L, 99L, DccRegistrationCertificatePageQuery.builder()
                .sortField("reminder").sortOrder("desc").build(), context("CE-REMINDER-SORT"));
        assertEquals(List.of("CE_T_3", "CE_T_12", "CE_T_18"),
                page.getList().stream().map(DccRegistrationCertificatePageItem::getVisualState).toList());
        var filter = DccRegistrationCertificatePageQuery.builder().reminderState("CE_T_12").build();
        assertEquals(1L, queryService.getPage(1L, 99L, filter, context("CE-REMINDER-FILTER")).getTotal());
        assertEquals("CE_T_12", queryService.getDetail(1L, 99L, warning.certificateId(), null,
                context("CE-REMINDER-DETAIL")).getVisualState());
        jdbcTemplate.update("UPDATE dcc_registration_certificate_supporting_document SET version_id=? WHERE certificate_id=?",
                warning.versionId(), warning.certificateId());
        assertEquals(1L, queryService.getPage(1L, 99L, filter, context("CE-REMINDER-MATERIAL-FILTER")).getTotal());
        assertEquals("CE_T_12", queryService.getDetail(1L, 99L, warning.certificateId(), null,
                context("CE-REMINDER-MATERIAL-DETAIL")).getVisualState());
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
    void detailReturnsTenantCurrentCertificateWithoutCompanyScopeFilter() {
        FormalFixture visible = seedFormal(1L, 11L, "ACTIVE", "CURRENT", "CERT-CROSS-COMPANY", true, 21L);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(11L)), any()))
                .thenReturn(List.of(owner(11L, "Owner B")));

        DccRegistrationCertificateDetail detail = queryService.getDetail(
                1L, 99L, visible.certificateId(), null, context("REQ-CROSS-COMPANY-DETAIL"));

        assertEquals(visible.certificateId(), detail.getCertificateId());
        assertEquals("Owner B", detail.getOwnerCompanyName());
        verify(companyScopeApi, never()).getEnabledCompanyIdsForUser(99L);
    }

    @Test
    void oldIndexReadsTypeFromOldVersionSnapshotEvenWhenCurrentPointerDiffers() {
        FormalFixture current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CURRENT-TYPE", true, null);
        jdbcTemplate.update("UPDATE dcc_registration_certificate_version SET version_no=2 WHERE id=?", current.versionId());
        var oldVersion = versionMapper.selectById(current.versionId());
        oldVersion.setId(null);
        oldVersion.setVersionNo(1);
        oldVersion.setStatus("OLD");
        assertEquals(1, versionMapper.insert(oldVersion));
        var oldSnapshot = snapshotMapper.selectById(current.snapshotId());
        oldSnapshot.setId(null);
        oldSnapshot.setVersionId(oldVersion.getId());
        var oldFacts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts
                .parse(oldSnapshot.getFactsJson());
        oldFacts.setCertificateType("FDA");
        oldSnapshot.setFactsJson(oldFacts.toJson());
        assertEquals(1, snapshotMapper.insert(oldSnapshot));
        seedFdaFacts(new FormalFixture(current.certificateId(), oldVersion.getId(), oldSnapshot.getId(), null),
                "Historical holder", LocalDate.of(2025, 1, 1), false);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));
        var page = queryService.getOldIndexPage(1L, 99L,
                DccRegistrationCertificatePageQuery.builder().build(), context("OLD-SNAPSHOT-TYPE"));
        var item = page.getList().stream().filter(row -> row.getVersionId().equals(oldVersion.getId())).findFirst().orElseThrow();
        assertEquals("FDA", item.getCertificateType());
        assertThrows(ServiceException.class, () -> queryService.getDetail(1L, 99L,
                current.certificateId(), oldVersion.getId(), context("OLD-TYPE-NO-GRANT")));
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
        assertEquals("DOMESTIC", item.getCertificateType());
        assertEquals(LocalDate.of(2031, 9, 1), item.getExpiryDate());

        Set<String> fields = Arrays.stream(DccRegistrationCertificateOldIndexItem.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("certificateId", "versionId", "ownerCompanyId", "ownerCompanyName",
                "productMasterId", "productName", "projectCodeId", "projectCode",
                "classification", "certificateNo", "certificateType", "versionNo", "expiryDate", "status"), fields);
        assertFalse(fields.contains("factsJson"));
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
        refreshFacts(outside.versionId());
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
    void currentPageShowsCurrentAndApprovedPendingRenewal() {
        FormalFixture current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-CURRENT", true, 20L);
        FormalFixture renewal = seedPendingRenewal(current.certificateId(), current.snapshotId());
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10).build(),
                context("REQ-CURRENT-PAGE-PENDING-RENEWAL"));

        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getList().size());
        assertTrue(page.getList().stream().anyMatch(row -> row.getVersionId().equals(current.versionId()) && "CURRENT".equals(row.getStatus())));
        DccRegistrationCertificatePageItem item = page.getList().stream().filter(row -> row.getVersionId().equals(renewal.versionId())).findFirst().orElseThrow();
        assertEquals(current.certificateId(), item.getCertificateId());
        assertEquals(renewal.versionId(), item.getVersionId());
        assertEquals("PENDING_EFFECTIVE", item.getStatus());
        assertEquals(LocalDate.of(2026, 8, 1), item.getApprovalDate());
        assertEquals(LocalDate.of(2026, 10, 1), item.getEffectiveDate());
        assertEquals(LocalDate.of(2031, 10, 1), item.getExpiryDate());
        assertTrue(item.getHasPendingRenewal(),
                "an approved renewal that is pending effective date must block duplicate renewal submission");
    }

    @Test
    void currentPageMarksBpmBoundRenewalApprovalAsPending() {
        FormalFixture current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-BPM-BOUND-RENEWAL", true, 20L);
        seedBpmBoundRenewalApproval(current);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        PageResult<DccRegistrationCertificatePageItem> page = queryService.getPage(
                1L, 99L, DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10).build(),
                context("REQ-CURRENT-PAGE-BPM-BOUND-RENEWAL"));

        assertEquals(1L, page.getTotal());
        DccRegistrationCertificatePageItem item = page.getList().get(0);
        assertEquals(current.certificateId(), item.getCertificateId());
        assertEquals(current.versionId(), item.getVersionId(),
                "a pending approval must not replace the current effective version in the list");
        assertTrue(item.getHasPendingRenewal(),
                "a BPM-bound renewal approval must block duplicate renewal submission from the row action");
    }

    @Test
    void detailReturnsInitialUploadOperatorAndApproverFromFormalWorkflowFacts() {
        FormalFixture visible = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-UPLOAD-AUDIT", true, 20L);
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 8, 16, 15, 30);
        seedApprovedUploadRequest(visible, 88L, uploadedAt);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));

        DccRegistrationCertificateDetail detail = queryService.getDetail(
                1L, 99L, visible.certificateId(), null, context("REQ-DETAIL-UPLOAD-AUDIT"));

        assertEquals("用户-88", detail.getUploadOperatorName());
        assertEquals(uploadedAt, detail.getUploadedAt());
        assertEquals("用户-99", detail.getUploadApproverName());
        assertEquals(LocalDateTime.of(2026, 8, 17, 9, 0), detail.getUploadApprovedAt());
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
    void oldDetailRejectsScopedUserWithoutOldViewGrant() {
        FormalFixture old = seedFormal(1L, 10L, "EXPIRED_UNRENEWED", "OLD", "CERT-OLD-DETAIL", true, null);
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        ServiceException error = assertThrows(ServiceException.class, () -> queryService.getDetail(
                1L, 99L, old.certificateId(), old.versionId(), context("REQ-OLD-SCOPED-DETAIL")));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID.getCode(), error.getCode());
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
    void defaultDetailKeepsCurrentWhilePendingVersionCanBeSelectedExplicitly() {
        FormalFixture current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CERT-PENDING-FILE", true, 20L);
        FormalFixture pending = seedPendingRenewal(current.certificateId(), current.snapshotId());
        Long pendingFileId = seedRegistrationFile(1L, pending.versionId(), "CERT-PENDING-FILE", "BOUND");
        when(companyScopeApi.getEnabledCompanyIdsForUser(99L)).thenReturn(Set.of(10L));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any())).thenReturn(List.of(owner(10L, "Owner A")));
        var detail = queryService.getDetail(1L, 99L, current.certificateId(), null, context("REQ-DEFAULT-CURRENT"));
        assertEquals(current.versionId(), detail.getVersionId());
        var selected = queryService.getDetail(1L, 99L, current.certificateId(), pending.versionId(), context("REQ-SELECT-PENDING"));
        assertEquals(pending.versionId(), selected.getVersionId());
        assertEquals(pendingFileId, selected.getRegistrationFileId());
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

    @Test
    void fdaFactsCombinedFiltersUseTypedListingDatesAndAllDisplayedSortFields() {
        var a=seedFormal(1L,10L,"ACTIVE","CURRENT","FDA-A",true,20L);
        var b=seedFormal(1L,10L,"ACTIVE","CURRENT","FDA-B",true,20L);
        var excluded=seedFormal(1L,10L,"ACTIVE","CURRENT","FDA-EXCLUDED",true,20L);
        seedFdaFacts(a,"B holder",LocalDate.of(2026,10,1),true);
        seedFdaFacts(b,"A holder",LocalDate.of(2026,10,2),true);
        seedFdaFacts(excluded,"C holder",LocalDate.of(2025,1,1),false);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)),any())).thenReturn(List.of(owner(10L,"Owner A")));
        var query=DccRegistrationCertificatePageQuery.builder().certificateType("FDA")
                .fieldFilters(java.util.Map.of("ownProduct","true","customerHeld","true","kpNumber","K001"))
                .listingStart(LocalDate.of(2026,10,1)).listingEnd(LocalDate.of(2026,10,2))
                .sortField("certificateHolder").sortOrder("asc").build();
        var page=queryService.getPage(1L,99L,query,context("FDA-TYPED-FILTER"));
        assertEquals(List.of(b.certificateId(),a.certificateId()),page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        for(String field:List.of("certificateHolder","registrationLink","productNameEn","submissionType","kpNumber","regulationCode","productCode","approvalDate","listingNumber","listingDate","customerHeld","approvalLetterStatus","factoryRegistrationStatus","ownProduct","changeDescription","remark")) {
            query.setSortField(field);
            assertEquals(2L,queryService.getPage(1L,99L,query,context("FDA-SORT-"+field)).getTotal());
        }
    }

    @Test
    void productionAndSupplementalFiltersCombineWithCanonicalCertificateFacts() {
        var record=seedFormal(1L,10L,"ACTIVE","CURRENT","DOMESTIC-FILTER",true,20L);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)),any())).thenReturn(List.of(owner(10L,"Owner A")));
        var query=DccRegistrationCertificatePageQuery.builder().certificateType("DOMESTIC")
                .fieldFilters(java.util.Map.of("ownProduct","true","entrustedProduction","true","selfProduction","false","intendedUse","Sensitive","entrustedEnterpriseName","Factory A"))
                .certificateNo("DOMESTIC").sortField("intendedUse").sortOrder("desc").build();
        var page=queryService.getPage(1L,99L,query,context("PRODUCTION-TYPED-FILTER"));
        assertEquals(List.of(record.certificateId()),page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        query.setFieldFilters(java.util.Map.of("selfProduction","true"));
        assertEquals(0L,queryService.getPage(1L,99L,query,context("PRODUCTION-NEGATIVE")).getTotal());
    }

    @Test
    void expiredDisplayFiltersMatchExpiryWithoutChangingVersionLifecycle() {
        var expired = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "EXPIRED-FILTER", true, 20L);
        var current = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "CURRENT-FILTER", true, 20L);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any())).thenReturn(List.of(owner(10L, "Owner A")));
        var today = LocalDate.now(DccRegistrationCertificateBusinessClock.BUSINESS_ZONE);
        updateExpiryDate(expired.versionId(), today.minusDays(1));
        updateExpiryDate(current.versionId(), today);
        for (String key : List.of("status", "reminder")) {
            var query = DccRegistrationCertificatePageQuery.builder().build();
            query.setListFilters("[{\"key\":\"" + key + "\",\"operator\":\"eq\",\"value\":\"已过期\"}]");
            var page = queryService.getPage(1L, 99L, query, context("EXPIRED-DISPLAY-FILTER"));
            assertEquals(List.of(expired.certificateId()), page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
            assertEquals("CURRENT", page.getList().get(0).getStatus());
            assertEquals("EXPIRED", page.getList().get(0).getVisualState());
        }
        var query = DccRegistrationCertificatePageQuery.builder().reminderState("EXPIRED").build();
        assertEquals(1L, queryService.getPage(1L, 99L, query, context("EXPIRED-STATE-FILTER")).getTotal());
    }

    @Test
    void entrustedNameFiltersUseTheDisplayedSnapshotAfterAChange() {
        var record = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "ENTERPRISE-CHANGE", true, 20L);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any())).thenReturn(List.of(owner(10L, "Owner A")));
        jdbcTemplate.update("DELETE FROM dcc_registration_certificate_snapshot_entrusted WHERE snapshot_id=?", record.snapshotId());
        jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET entrusted_enterprises_json=? WHERE id=?",
                "[{\"enterpriseId\":30,\"enterpriseName\":\"山东瑛泰医疗器械有限公司\"}]", record.snapshotId());
        for (String name : List.of("山东瑛泰医疗器械有限公司", "山东瑛泰")) {
            var query = DccRegistrationCertificatePageQuery.builder().entrustedEnterpriseName(name).build();
            assertEquals(1L, queryService.getPage(1L, 99L, query, context("ENTERPRISE-DIRECT")).getTotal());
            query.setEntrustedEnterpriseName(null);
            query.setListFilters("[{\"key\":\"modelSpecification\",\"operator\":\"contains\",\"value\":\"Sensitive Model\"},{\"key\":\"entrustedEnterpriseName\",\"operator\":\"contains\",\"value\":\"" + name + "\"}]");
            assertEquals(1L, queryService.getPage(1L, 99L, query, context("ENTERPRISE-LIST-FILTER")).getTotal());
            query.setListFilters(null);
            query.setFieldFilters(java.util.Map.of("entrustedEnterpriseName", name));
            assertEquals(1L, queryService.getPage(1L, 99L, query, context("ENTERPRISE-FIELD")).getTotal());
        }
        var query = DccRegistrationCertificatePageQuery.builder().entrustedEnterpriseName("Factory A").build();
        assertEquals(0L, queryService.getPage(1L, 99L, query, context("ENTERPRISE-OLD-NAME")).getTotal());
    }

    @Test
    void ceTypedRevisionFiltersSurviveRealTenantAndPaginationParsing() {
        var query = DccRegistrationCertificatePageQuery.builder().certificateType("CE")
                .ceRevision("Rev.2").ceCertificateNo("CE-2")
                .ceIssueStart(LocalDate.of(2026, 1, 1)).ceIssueEnd(LocalDate.of(2026, 12, 31))
                .sortField("ceIssueDate").sortOrder("asc").build();
        // This parser-only fixture must register the real tenant entities, not ignore unknown tables.
        for (Class<?> entity : List.of(DccRegistrationCertificateDO.class,
                DccRegistrationCertificateSnapshotDO.class, DccRegistrationCertificateVersionDO.class)) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                    new org.apache.ibatis.builder.MapperBuilderAssistant(
                            new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ce-parser-fixture"), entity);
        }
        var tenantHandler = new cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor(
                new cn.iocoder.yudao.framework.tenant.config.TenantProperties());
        assertFalse(tenantHandler.ignoreTable("dcc_registration_certificate_snapshot"));
        assertFalse(tenantHandler.ignoreTable("dcc_registration_certificate"));
        var tenant = new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(tenantHandler);
        Long previousTenant = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getTenantId();
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(7L);
        try {
            for (String provider : List.of(DccRegistrationCertificateQueryMapper.SqlProvider.selectPage(),
                    DccRegistrationCertificateQueryMapper.SqlProvider.countPage(),
                    DccRegistrationCertificateQueryMapper.SqlProvider.selectOldIndexPage(),
                    DccRegistrationCertificateQueryMapper.SqlProvider.countOldIndex())) {
                var source = new org.apache.ibatis.scripting.xmltags.XMLLanguageDriver().createSqlSource(
                        new org.apache.ibatis.session.Configuration(), provider, java.util.Map.class);
                var boundSql = source.getBoundSql(java.util.Map.of("tenantId", 1L, "companyIds", List.of(),
                        "query", query, "limit", 10, "offset", 0));
                String rewritten = assertDoesNotThrow(() -> tenant.parserSingle(boundSql.getSql(), null));
                assertTrue(rewritten.contains("SET_VAR(cte_max_recursion_depth=4294967295)"));
                assertTrue(rewritten.contains("s.tenant_id = 7"), rewritten);
                assertFalse(rewritten.contains("ce.tenant_id")); // ce is a CTE over the already scoped snapshot.
                assertFalse(rewritten.contains("JSON_TABLE"));
                assertTrue(rewritten.contains("WITH RECURSIVE ce"));
                assertTrue(rewritten.contains("ce.idx"));
                assertTrue(boundSql.getParameterMappings().stream()
                        .anyMatch(mapping -> mapping.getProperty().equals("query.ceIssueStart")));
                var pagination = new com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor();
                String countSql = pagination.autoCountSql(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10), rewritten);
                assertTrue(countSql.contains("SET_VAR(cte_max_recursion_depth=4294967295)"));
                assertDoesNotThrow(() -> net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(countSql));
                String pageSql = new com.baomidou.mybatisplus.extension.plugins.pagination.dialects.MySqlDialect()
                        .buildPaginationSql(rewritten, 0, 10).getDialectSql();
                assertTrue(pageSql.contains("SET_VAR(cte_max_recursion_depth=4294967295)"));
                assertDoesNotThrow(() -> net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(pageSql));
            }
        } finally {
            if (previousTenant == null) cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
            else cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(previousTenant);
        }
    }

    @Test
    void nonCeQueriesKeepTheirOriginalRecursionSetting() {
        var source = new org.apache.ibatis.scripting.xmltags.XMLLanguageDriver().createSqlSource(
                new org.apache.ibatis.session.Configuration(),
                DccRegistrationCertificateQueryMapper.SqlProvider.countPage(), java.util.Map.class);
        var query = DccRegistrationCertificatePageQuery.builder().certificateType("DOMESTIC").build();
        String sql = source.getBoundSql(java.util.Map.of("tenantId", 1L, "companyIds", List.of(), "query", query)).getSql();
        assertFalse(sql.contains("SET_VAR"));
        assertFalse(sql.contains("WITH RECURSIVE"));
    }

    @Test
    void otherDomesticTextProjectCodeIsQueryableSortableAndNeverCreatesAssociation() {
        var a=seedFormal(1L,10L,"ACTIVE","CURRENT","OTHER-A",true,null);
        var b=seedFormal(1L,10L,"ACTIVE","CURRENT","OTHER-B",true,null);
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)),any())).thenReturn(List.of(owner(10L,"Owner A")));
        for(var fixture:List.of(a,b)) {
            var snapshot=snapshotMapper.selectById(fixture.snapshotId());
            var facts=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(snapshot.getFactsJson());
            facts.setOwnProduct(false);facts.setProjectCode(fixture==a?"EXT-B":"EXT-A");snapshot.setFactsJson(facts.toJson());jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE id=?",facts.toJson(),fixture.snapshotId());
            jdbcTemplate.update("UPDATE dcc_registration_certificate SET product_master_id=NULL,project_code_id=NULL WHERE id=?",fixture.certificateId());
        }
        var query=DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10).certificateType("DOMESTIC")
                .fieldFilters(java.util.Map.of("projectCode","EXT-")).sortField("projectCode").sortOrder("asc").build();
        var page=queryService.getPage(1L,99L,query,context("REQ-OTHER-PROJECT-CODE"));
        assertEquals(List.of(b.certificateId(),a.certificateId()),page.getList().stream().map(DccRegistrationCertificatePageItem::getCertificateId).toList());
        assertEquals("EXT-A",page.getList().get(0).getProjectCode());assertEquals(null,page.getList().get(0).getProjectCodeId());assertEquals(null,page.getList().get(0).getProductMasterId());
        query.setProjectCode("EXT-B");assertEquals(1L,queryService.getPage(1L,99L,query,context("REQ-OTHER-PROJECT-CODE-EXACT")).getTotal());
    }

    @Test
    void snapshotIsoDatesRemainTypedForProjectionFilteringSortingAndReminder() {
        FormalFixture fixture = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "ISO-DATES", true, 20L);
        var snapshot = snapshotMapper.selectById(fixture.snapshotId());
        var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts
                .parse(snapshot.getFactsJson());
        facts.setFirstObtainedDate(LocalDate.of(2025, 1, 2));
        facts.setApprovalDate(LocalDate.of(2025, 2, 3));
        facts.setEffectiveDate(LocalDate.of(2025, 3, 4));
        facts.setExpiryDate(LocalDate.of(2031, 10, 5));
        jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE id=?",
                facts.toJson(), fixture.snapshotId());
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));
        var query = DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10)
                .expiryStart(LocalDate.of(2031, 10, 1)).expiryEnd(LocalDate.of(2031, 10, 31))
                .sortField("expiryDate").sortOrder("asc").build();

        var projected = queryMapper.selectPage(1L, List.of(), query, 10, 0);
        assertEquals(1, projected.size());
        assertEquals(facts.getFirstObtainedDate(), projected.get(0).getFirstObtainedDate());
        assertEquals(facts.getApprovalDate(), projected.get(0).getApprovalDate());
        assertEquals(facts.getEffectiveDate(), projected.get(0).getEffectiveDate());
        assertEquals(facts.getExpiryDate(), projected.get(0).getExpiryDate());
        var page = queryService.getPage(1L, 99L, query, context("REQ-ISO-DATE-REMINDER"));
        assertEquals(1L, page.getTotal());
        assertEquals(facts.getExpiryDate(), page.getList().get(0).getExpiryDate());
        assertNotNull(page.getList().get(0).getVisualState());
    }

    @Test
    void fdaJsonNullDatesStayAbsentAndDoNotRequireAnExpiryReminder() {
        FormalFixture fixture = seedFormal(1L, 10L, "ACTIVE", "CURRENT", "FDA-NULL-DATES", true, 20L);
        seedFdaFacts(fixture, "FDA holder", LocalDate.of(2026, 10, 1), false);
        var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts
                .parse(snapshotMapper.selectById(fixture.snapshotId()).getFactsJson());
        facts.setApprovalDate(null);
        jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE id=?",
                facts.toJson(), fixture.snapshotId());
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(owner(10L, "Owner A")));
        var query = DccRegistrationCertificatePageQuery.builder().pageNo(1).pageSize(10)
                .certificateType("FDA").sortField("expiryDate").sortOrder("asc").build();

        var projected = queryMapper.selectPage(1L, List.of(), query, 10, 0).get(0);
        assertNull(projected.getFirstObtainedDate());
        assertNull(projected.getApprovalDate());
        assertNull(projected.getEffectiveDate());
        assertNull(projected.getExpiryDate());
        var item = queryService.getPage(1L, 99L, query, context("REQ-FDA-NULL-DATE-REMINDER"))
                .getList().get(0);
        assertNull(item.getExpiryDate());
        assertEquals("NONE", item.getVisualState());
        assertEquals("NORMAL", item.getReminderColor());
    }

    private void seedFdaFacts(FormalFixture fixture,String holder,LocalDate listed,boolean customerHeld) {
        var facts=new cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts();
        facts.setCertificateType("FDA");facts.setOwnProduct(true);facts.setClassification("II");facts.setCertificateHolder(holder);
        facts.setSubmissionType("TRADITIONAL_510K");facts.setCustomerHeld(customerHeld);facts.setKpNumber("K001");
        facts.setApprovalLetterStatus("VALID");facts.setFactoryRegistrationStatus("ACTIVE");facts.setListingDate(listed);facts.setApprovalDate(LocalDate.of(2026,1,1));
        facts.validate();jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE id=?",facts.toJson(),fixture.snapshotId());
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
        snapshot.setFactsJson(DccRegistrationCertificateTestFacts.domestic(jdbcTemplate, snapshot.getVersionId()));
        assertEquals(1, snapshotMapper.insert(snapshot));
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_snapshot_entrusted
                    (snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id, deleted)
                VALUES (?, ?, ?, ?, ?, 0)
        """, snapshot.getId(), 30L, "Factory A", 1, tenantId));

        Long registrationFileId = null;
        if (hasFile) {
            registrationFileId = seedRegistrationFile(tenantId, version.getId(), certificateNo, "BOUND");
        }

        certificate.setCurrentVersionId("CURRENT".equals(versionStatus) ? version.getId() : null);
        certificate.setPendingVersionId("PENDING_EFFECTIVE".equals(versionStatus) ? version.getId() : null);
        certificate.setCurrentSnapshotId("CURRENT".equals(versionStatus) ? snapshot.getId() : null);
        assertEquals(1, certificateMapper.updateById(certificate));
        return new FormalFixture(certificate.getId(), version.getId(), snapshot.getId(), registrationFileId);
    }

    private Long seedRegistrationFile(Long tenantId, Long versionId, String certificateNo, String status) {
        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION")
                .ownerId(versionId)
                .fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(7000L + versionId)
                .originalName("sensitive-" + certificateNo + ".pdf")
                .mimeType("application/pdf")
                .fileSize(128L)
                .sha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .status(status)
                .boundAt(java.time.LocalDateTime.of(2026, 8, 17, 9, 5))
                .boundBy(99L)
                .build();
        file.setTenantId(tenantId);
        assertEquals(1, dbFileMapper.insert(file));
        return file.getId();
    }

    private void refreshFacts(Long versionId) {
        jdbcTemplate.update("UPDATE dcc_registration_certificate_snapshot SET facts_json=? WHERE version_id=?",
                DccRegistrationCertificateTestFacts.domestic(jdbcTemplate,versionId),versionId);
    }

    private void updateExpiryDate(Long versionId, LocalDate expiryDate) {
        assertEquals(1, jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_version
                   SET expiry_date = ?
                 WHERE tenant_id = 1 AND id = ?
                """, expiryDate, versionId));
        refreshFacts(versionId);
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
        snapshot.setFactsJson(DccRegistrationCertificateTestFacts.domestic(jdbcTemplate, snapshot.getVersionId()));
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
        snapshot.setFactsJson(DccRegistrationCertificateTestFacts.domestic(jdbcTemplate, snapshot.getVersionId()));
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

    private void seedApprovedUploadRequest(FormalFixture fixture, Long requesterUserId, LocalDateTime requestedAt) {
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request
                    (id, tenant_id, owner_company_id, certificate_id, requester_user_id,
                     request_type, request_key, purpose, status, requested_at, completed_at,
                     detail_json, creator, deleted)
                VALUES (8001, 1, 10, ?, ?, 'UPLOAD_CERTIFICATE', 'upload-audit-8001',
                        '注册证上传审批', 'APPROVED', ?, ?, '{}', 'junit', 0)
                """, fixture.certificateId(), requesterUserId, requestedAt,
                LocalDateTime.of(2026, 8, 17, 9, 0)));
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request_file
                    (id, tenant_id, request_id, business_file_id, file_kind,
                     download_requested, status, detail_json, creator, deleted)
                VALUES (8101, 1, 8001, ?, 'REGISTRATION_CERTIFICATE',
                        FALSE, 'APPROVED', '{}', 'junit', 0)
                """, fixture.registrationFileId()));
    }

    private void seedBpmBoundRenewalApproval(FormalFixture fixture) {
        assertEquals(1, jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request
                    (id, tenant_id, owner_company_id, certificate_id, requester_user_id,
                     request_type, request_key, bpm_process_instance_id, purpose, status, requested_at,
                     detail_json, creator, deleted)
                VALUES (8002, 1, 10, ?, 99, 'UPLOAD_CERTIFICATE', 'renewal-bpm-bound-8002', 'BPM-8002',
                        '上传延续注册证，待注册部经理审批', 'BPM_BOUND', ?, '{}', 'junit', 0)
                """, fixture.certificateId(), LocalDateTime.of(2026, 9, 3, 9, 0)));
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

    private static AdminUserRespDTO user(Long id) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname("用户-" + id);
        return user;
    }

    private static DccRequestAuditContext context(String requestId) {
        return new DccRequestAuditContext("10.0.0.1", "JUnit", requestId);
    }

    public static String jsonExtract(String json, String path) {
        if (json == null || path == null) return null;
        var node = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json);
        if ("$[*].enterpriseName".equals(path)) {
            var names = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
            node.forEach(item -> { if (item.has("enterpriseName")) names.add(item.get("enterpriseName")); });
            return names.isEmpty() ? null : names.toString();
        }
        if (path.equals("$.projectProducts[*].projectCodeId")) {
            var result = cn.iocoder.yudao.framework.common.util.json.JsonUtils.getObjectMapper().createArrayNode();
            var products = node.get("projectProducts");
            if (products == null || !products.isArray()) return null;
            products.forEach(product -> { if (product.has("projectCodeId")) result.add(product.get("projectCodeId")); });
            return result.toString();
        }
        var tokens = java.util.regex.Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*)|\\[([0-9]+)\\]").matcher(path.substring(1));
        while (tokens.find() && node != null) node = tokens.group(1) != null ? node.get(tokens.group(1)) : node.get(Integer.parseInt(tokens.group(2)));
        return node == null ? null : node.toString();
    }
    public static Integer jsonContains(String json, String candidate) {
        if (json == null || candidate == null) return null;
        var node = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json);
        var value = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(candidate);
        for (var element : node) if (element.equals(value)) return 1;
        return 0;
    }
    public static String jsonQuote(String value) {
        return value == null ? null : cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(value);
    }
    public static Integer jsonLength(String json) {
        if (json == null) return null;
        var node = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json);
        return node.isContainerNode() ? node.size() : 1;
    }
    public static String jsonType(String json) {
        if (json == null) return null;
        var node = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json);
        return node.isNull() ? "NULL" : node.isArray() ? "ARRAY" : node.isObject() ? "OBJECT" : node.isTextual() ? "STRING" : node.isBoolean() ? "BOOLEAN" : "INTEGER";
    }

    private record FormalFixture(Long certificateId, Long versionId, Long snapshotId, Long registrationFileId) {
    }
}
