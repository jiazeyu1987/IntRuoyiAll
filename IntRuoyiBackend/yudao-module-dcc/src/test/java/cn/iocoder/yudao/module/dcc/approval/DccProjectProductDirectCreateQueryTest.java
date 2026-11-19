package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** Executes the production query predicates against real rows, including SQL NULL semantics. */
class DccProjectProductDirectCreateQueryTest {
    JdbcTemplate jdbc;
    DccProjectProductTaskDelegate delegate;
    DccProjectProductCreateRequestMapper requests;
    DccProjectProductActorSupport actors;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:direct_query_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE dcc_project_product_create_request(id BIGINT, tenant_id BIGINT, status VARCHAR(32),"
                + " review_contract VARCHAR(64), reviewer_role_id BIGINT, reviewer_role_code VARCHAR(64),"
                + " configured_reviewer_user_id BIGINT, reviewer_user_id BIGINT, approver_user_id BIGINT,"
                + " reviewed_time TIMESTAMP, approved_time TIMESTAMP, applicant_user_id BIGINT, submitted_time TIMESTAMP, project_name VARCHAR(128), product_name VARCHAR(128), project_code VARCHAR(64))");
        requests = mock(DccProjectProductCreateRequestMapper.class);
        when(requests.selectCount(any())).thenAnswer(invocation -> count(invocation.getArgument(0)));
        actors = mock(DccProjectProductActorSupport.class);
        when(actors.canQuery(7L)).thenReturn(true);
        when(actors.canUpdate(7L)).thenReturn(true);
        delegate = new DccProjectProductTaskDelegate(requests, actors, mock(DccProjectProductCreateService.class));
        ReflectionTestUtils.setField(delegate, "reviewerRoles", mock(DccProjectReviewerRoleService.class));
        jdbc.update("INSERT INTO dcc_project_product_create_request(id,tenant_id,status,configured_reviewer_user_id)"
                + " VALUES(1,1,'PENDING_REVIEW',7)");
        insertDirect(2, 1);
    }

    private void insertDirect(long id, long tenant) {
        jdbc.update("INSERT INTO dcc_project_product_create_request(id,tenant_id,status,review_contract) VALUES(?,?,?,?)",
                id, tenant, "COMPLETED", DccProjectProductCreateService.DIRECT_CREATE_CONTRACT);
    }

    private long count(QueryWrapper<DccProjectProductCreateRequestDO> query) {
        var values = new ArrayList<Object>();
        var matcher = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}").matcher(query.getSqlSegment());
        var sql = new StringBuilder();
        while (matcher.find()) {
            values.add(query.getParamNameValuePairs().get(matcher.group(1)));
            matcher.appendReplacement(sql, "?");
        }
        matcher.appendTail(sql);
        return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_product_create_request WHERE " + sql,
                Long.class, values.toArray());
    }

    private long total(ApprovalTaskViewType view) {
        return delegate.page(ApprovalTaskQueryContext.of(7L, view, ApprovalModuleCode.DCC, null, 1, 20)
                .withWindow(new ApprovalQueryWindow(0, 0))).getTotal();
    }

    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void initiatedIncludesOwnSubmittedHistoryAndDirectCreatesButExcludesOtherApplicantsAndTenants() {
        jdbc.update("UPDATE dcc_project_product_create_request SET applicant_user_id=7,submitted_time=CURRENT_TIMESTAMP WHERE id=1");
        for (String status : new String[]{"PENDING_APPROVAL", "REJECTED", "WRITING", "WRITE_FAILED", "COMPLETED"}) {
            jdbc.update("INSERT INTO dcc_project_product_create_request(id,tenant_id,status,applicant_user_id,submitted_time) VALUES(10,1,?,7,CURRENT_TIMESTAMP)", status);
            assertEquals(2L, total(ApprovalTaskViewType.MY_INITIATED), status);
            jdbc.update("DELETE FROM dcc_project_product_create_request WHERE id=10");
        }
        jdbc.update("UPDATE dcc_project_product_create_request SET applicant_user_id=7,submitted_time=CURRENT_TIMESTAMP WHERE id=2");
        assertEquals(2L, total(ApprovalTaskViewType.MY_INITIATED));
        jdbc.update("UPDATE dcc_project_product_create_request SET applicant_user_id=8");
        assertEquals(0L, total(ApprovalTaskViewType.MY_INITIATED));
        var global = ApprovalTaskQueryContext.of(7L, ApprovalTaskViewType.MY_INITIATED, ApprovalModuleCode.DCC, null, 1, 20, true)
                .withWindow(new ApprovalQueryWindow(0, 0));
        assertEquals(2L, delegate.page(global).getTotal());
        jdbc.update("UPDATE dcc_project_product_create_request SET tenant_id=2 WHERE id=1");
        assertEquals(1L, delegate.page(global).getTotal());
        jdbc.update("UPDATE dcc_project_product_create_request SET tenant_id=1,applicant_user_id=7,submitted_time=NULL WHERE id=1");
        assertEquals(0L, total(ApprovalTaskViewType.MY_INITIATED));
    }

    @Test void initiatedPaginatesAndKeepsExistingDetailAndTimelineIdentityWithoutReviewActions() {
        jdbc.update("UPDATE dcc_project_product_create_request SET applicant_user_id=7,submitted_time=CURRENT_TIMESTAMP,project_name='小马宝莉云宝',product_name='产品',project_code='XM001' WHERE id=1");
        var row = DccProjectProductCreateRequestDO.builder().id(1L).projectName("小马宝莉云宝").productName("产品")
                .projectCode("XM001").applicantUserId(7L).reviewContract(DccProjectProductCreateService.DIRECT_CREATE_CONTRACT).status("COMPLETED").submittedTime(java.time.LocalDateTime.now()).build();
        row.setTenantId(1L);
        when(requests.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(java.util.List.of(row));
        var context = ApprovalTaskQueryContext.of(7L, ApprovalTaskViewType.MY_INITIATED, ApprovalModuleCode.DCC, "小马宝莉", 1, 1);
        var page = delegate.page(context);
        assertEquals(1L, page.getTotal());
        var summary = page.getList().get(0);
        assertEquals("小马宝莉云宝 · 产品", summary.getBusinessTitle());
        assertEquals(7L, summary.getInitiatorUserId());
        assertEquals("DCC_PROJECT_PRODUCT:1:1:DCC_PROJECT_PRODUCT_REQUEST", summary.getSourceTaskId());
        assertEquals("/mdm/product-catalog", summary.getDetailRoute());
        assertEquals("1", summary.getDetailQuery().get("requestId"));
        assertTrue(summary.getAvailableActions().isEmpty());
        assertFalse(summary.getRequiresSignature());
        verify(actors, never()).requireApprover();
        @SuppressWarnings("rawtypes") var queries = org.mockito.ArgumentCaptor.forClass(QueryWrapper.class);
        verify(requests).selectList(queries.capture());
        assertTrue(queries.getValue().getSqlSegment().contains("LIMIT 1 OFFSET 0"));
        assertEquals(0L, delegate.page(ApprovalTaskQueryContext.of(7L, ApprovalTaskViewType.MY_INITIATED, ApprovalModuleCode.DCC, "不存在", 1, 20)
                .withWindow(new ApprovalQueryWindow(0, 0))).getTotal());
    }

    @Test void validDirectCreatesDoNotBreakCountsOrBecomeApprovalTasks() {
        assertEquals(1L, total(ApprovalTaskViewType.TODO));
        assertEquals(0L, total(ApprovalTaskViewType.DONE));
        jdbc.update("DELETE FROM dcc_project_product_create_request WHERE id=1");
        assertEquals(0L, total(ApprovalTaskViewType.TODO));
        assertEquals(0L, total(ApprovalTaskViewType.DONE));
    }

    @Test void directCreateWithForgedReviewerOrApproverIdentityStillFails() {
        for (String column : new String[]{"reviewer_role_id", "configured_reviewer_user_id", "reviewer_user_id", "approver_user_id"}) {
            jdbc.update("UPDATE dcc_project_product_create_request SET " + column + "=7 WHERE id=2");
            assertEquals(DccProjectReviewerRoleService.CONTRACT_INVALID.getCode(),
                    assertThrows(ServiceException.class, () -> total(ApprovalTaskViewType.TODO), column).getCode());
            jdbc.update("UPDATE dcc_project_product_create_request SET " + column + "=NULL WHERE id=2");
        }
        jdbc.update("UPDATE dcc_project_product_create_request SET reviewer_role_code='dcc_project_reviewer' WHERE id=2");
        assertEquals(DccProjectReviewerRoleService.CONTRACT_INVALID.getCode(),
                assertThrows(ServiceException.class, () -> total(ApprovalTaskViewType.DONE)).getCode());
    }

    @Test void unknownOrIncompleteRoleContractsRemainErrorsButOtherTenantsAreIsolated() {
        jdbc.update("UPDATE dcc_project_product_create_request SET review_contract='UNKNOWN' WHERE id=2");
        assertEquals(DccProjectReviewerRoleService.CONTRACT_INVALID.getCode(),
                assertThrows(ServiceException.class, () -> total(ApprovalTaskViewType.TODO)).getCode());
        jdbc.update("UPDATE dcc_project_product_create_request SET review_contract='ROLE_SINGLE_REVIEW_V1' WHERE id=2");
        assertEquals(DccProjectReviewerRoleService.CONTRACT_INVALID.getCode(),
                assertThrows(ServiceException.class, () -> total(ApprovalTaskViewType.TODO)).getCode());
        jdbc.update("UPDATE dcc_project_product_create_request SET tenant_id=2 WHERE id=2");
        assertEquals(1L, total(ApprovalTaskViewType.TODO));
    }
}
