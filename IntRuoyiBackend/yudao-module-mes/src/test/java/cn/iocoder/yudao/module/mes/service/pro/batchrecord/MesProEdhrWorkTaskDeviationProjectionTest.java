package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.approval.MesEdhrAuthorizedTaskProjection;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** Exercises the real service projection used by approval-center count/window reads. */
class MesProEdhrWorkTaskDeviationProjectionTest {
    private MesProEdhrWorkTaskServiceImpl service;
    private MesProEdhrWorkTaskMapper mapper;
    private MockedStatic<SecurityFrameworkUtils> login;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        login = mockStatic(SecurityFrameworkUtils.class);
        login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
        service = new MesProEdhrWorkTaskServiceImpl();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "work"),
                MesProEdhrWorkTaskDO.class);
        mapper = mock(MesProEdhrWorkTaskMapper.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "workTaskMapper", mapper);
        AdminUserApi users = mock(AdminUserApi.class);
        when(users.getUserMap(anyCollection())).thenReturn(Map.of(12L,
                new AdminUserRespDTO().setId(12L).setNickname("偏差责任人")));
        ReflectionTestUtils.setField(service, "adminUserApi", users);
    }

    @AfterEach
    void cleanup() {
        login.close();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEVIATION_RECEIVE", "DEVIATION_CORRECT", "DEVIATION_VERIFY",
            "DEVIATION_SIGN", "DEVIATION_APPROVE", "DEVIATION_CLOSE"})
    void productionOrderDeviationWithoutBatchProjectsInAuthorizedWindowAndPrefix(String taskType) {
        MesProEdhrWorkTaskDO task = task(taskType, "EDHR_DEVIATION").setBusinessScopeId(27L);
        MesProEdhrWorkTaskPageReqVO filters = new MesProEdhrWorkTaskPageReqVO();
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(6L);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(task));

        assertDeviationProjection(service.getApprovalCenterAuthorizedWindow(
                filters, false, false, 0L, 1L), task);
        assertDeviationProjection(service.getApprovalCenterAuthorizedPrefix(
                filters, false, false, 1L), task);
        verify(mapper, times(2)).selectApprovalCenterWindow(filters, 12L, 1L, false, "TODO", 0L, 1L);
        verify(mapper).selectApprovalCenterPrefix(filters, 12L, 1L, false, "TODO", 1L);
        verify(mapper, times(2)).selectCount(any(Wrapper.class));
        verify(mapper, times(2)).selectList(any(Wrapper.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RELEASE_APPLICATION", "RELEASE_REPORT_NODE", "RELEASE_TRANSACTION"})
    void absentReleaseAssociationDoesNotLookUpNullKeys(String scopeType) {
        var page = project(task("RELEASE_APPROVE", scopeType));
        assertEquals(1L, page.getTotal());
        assertNull(page.getList().get(0).display().getVersion());
        assertNull(page.getList().get(0).display().getBatchExecutionId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RELEASE_APPLICATION", "RELEASE_REPORT_NODE", "RELEASE_TRANSACTION"})
    void formalReleaseAssociationStillProjectsItsVersion(String scopeType) {
        MesProEdhrWorkTaskDO task = task("RELEASE_APPROVE", scopeType).setBusinessScopeId(42L);
        if ("RELEASE_TRANSACTION".equals(scopeType)) {
            var transactions = mock(MesProEdhrReleaseTransactionMapper.class);
            when(transactions.selectListByIds(anyCollection())).thenReturn(List.of(
                    new MesProEdhrReleaseTransactionDO().setId(42L).setVersion(3)));
            ReflectionTestUtils.setField(service, "releaseTransactionMapper", transactions);
        } else {
            var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
            var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(42L).setVersion(3);
            if ("RELEASE_REPORT_NODE".equals(scopeType)) {
                task.setBatchExecutionId(5L);
                application.setBatchExecutionId(5L);
                var batches = mock(MesProEdhrBatchExecutionMapper.class);
                when(batches.selectBatchIds(anyCollection())).thenReturn(List.of());
                ReflectionTestUtils.setField(service, "batchExecutionMapper", batches);
                when(applications.selectListByBatchExecutionIds(anyCollection())).thenReturn(List.of(application));
            } else {
                when(applications.selectByIds(anyCollection())).thenReturn(List.of(application));
            }
            ReflectionTestUtils.setField(service, "releaseApplicationMapper", applications);
        }
        assertEquals(3, project(task).getList().get(0).display().getVersion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_GROUP", "DEPT_GROUP"})
    void absentCandidateSourceRemainsUnassociated(String candidateType) {
        var page = project(task("DEVIATION_APPROVE", "EDHR_DEVIATION")
                .setBusinessScopeId(27L).setCandidateSourceType(candidateType));
        assertNull(page.getList().get(0).display().getCandidatePoolName());
        assertNull(page.getList().get(0).display().getCandidateSourceId());
        assertTrue(page.getList().get(0).display().getActionable());
    }

    private PageResult<MesEdhrAuthorizedTaskProjection> project(MesProEdhrWorkTaskDO task) {
        MesProEdhrWorkTaskPageReqVO filters = new MesProEdhrWorkTaskPageReqVO();
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(task));
        return service.getApprovalCenterAuthorizedWindow(filters, false, false, 0L, 200L);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"DEVIATION_RECEIVE,,开展偏差调查", "DEVIATION_CORRECT,,完成偏差整改",
            "DEVIATION_VERIFY,CORRECTIVE_VERIFIER,完成偏差调查结果验证", "DEVIATION_SIGN,PREPARER,完成偏差处理记录编制",
            "DEVIATION_APPROVE,QUALITY_OWNER,完成质量负责人批准", "DEVIATION_CLOSE,CLOSE,完成偏差正式关闭"})
    void overduePreservesFormalDutyWithoutInboxNotice(String type, String node, String expectedTitle) {
        var task = task(type, "EDHR_DEVIATION").setBusinessScopeId(27L).setSignatureCellKey(node)
                .setReason("退回原因：请补充证据").setCandidateUserSnapshot("12")
                .setDueTime(java.time.LocalDateTime.of(2026, 11, 17, 9, 0));
        var handlingMapper = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationHandlingMapper.class);
        when(handlingMapper.selectByTenantAndDeviationId(1L, 27L)).thenReturn(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO());
        ReflectionTestUtils.setField(service, "deviationHandlingMapper", handlingMapper);
        var users = mock(AdminUserApi.class);
        when(users.getUser(12L)).thenReturn(new AdminUserRespDTO().setId(12L).setTenantId(1L).setStatus(0));
        ReflectionTestUtils.setField(service, "adminUserApi", users);
        var notify = mock(cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi.class);
        doReturn(List.of(task)).when(mapper).selectDueActiveTasks(any(), anyInt());
        doReturn(1).when(mapper).updateToOverdueIfActive(any(), any(), any());
        assertEquals(1, service.processOverdueTasks(java.time.LocalDateTime.of(2026, 11, 17, 10, 0), 20));
        verifyNoInteractions(notify);
        assertEquals("OVERDUE", task.getStatus());
        assertEquals("退回原因：请补充证据", task.getReason());
    }

    private MesProEdhrWorkTaskDO task(String taskType, String scopeType) {
        return new MesProEdhrWorkTaskDO().setId(31L).setTaskType(taskType)
                .setBusinessScopeType(scopeType).setWorkOrderId(20L).setWorkOrderCode("WO-20")
                .setAssigneeUserId(12L).setStatus("TODO")
                .setActionUrl("/mes/pro/edhr/deviation-detail?id=27");
    }

    private void assertDeviationProjection(PageResult<MesEdhrAuthorizedTaskProjection> page,
                                            MesProEdhrWorkTaskDO task) {
        assertEquals(6L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertSame(task, page.getList().get(0).task());
        var display = page.getList().get(0).display();
        assertEquals(task.getId(), display.getId());
        assertEquals(task.getTaskType(), display.getTaskType());
        assertEquals(20L, display.getWorkOrderId());
        assertEquals(27L, display.getBusinessScopeId());
        assertEquals(task.getActionUrl(), display.getActionUrl());
        assertEquals("偏差责任人", display.getAssigneeUserName());
        assertTrue(display.getActionable());
        assertNull(display.getBatchExecutionId());
        assertNull(display.getBatchTaskId());
        assertNull(display.getNodeType());
        assertNull(display.getVersion());
        assertNull(display.getBlockedReason());
    }
}
