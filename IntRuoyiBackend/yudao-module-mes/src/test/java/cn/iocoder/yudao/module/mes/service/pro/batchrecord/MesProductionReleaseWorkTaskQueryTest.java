package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProductionReleaseWorkTaskQueryTest {

    @Test
    void candidateQueryUsesRequestedFillAndNodeFiltersWithoutForcingReview() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), MesProEdhrWorkTaskDO.class.getName()),
                MesProEdhrWorkTaskDO.class);
        MesProEdhrWorkTaskMapper mapper = mock(
                MesProEdhrWorkTaskMapper.class, Answers.CALLS_REAL_METHODS);
        MesProEdhrWorkTaskPageReqVO request = new MesProEdhrWorkTaskPageReqVO();
        request.setPageNo(1);
        request.setPageSize(10);
        request.setTaskType("FILL");
        request.setBatchExecutionId(9001L);
        request.setNodeTypes(List.of("INCOMING_INSPECTION_REPORT", "STERILIZATION_REPORT"));
        AtomicReference<Wrapper<MesProEdhrWorkTaskDO>> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(1));
            return new PageResult<MesProEdhrWorkTaskDO>(List.of(), 0L);
        }).when(mapper).selectPage(eq((PageParam) request), any(Wrapper.class));

        mapper.selectCandidateTodoPage(request, 7101L, MesProEdhrWorkTaskStatus.TODO);

        LambdaQueryWrapper<MesProEdhrWorkTaskDO> wrapper =
                (LambdaQueryWrapper<MesProEdhrWorkTaskDO>) captured.get();
        assertTrue(wrapper.getSqlSegment().contains("batch_task_id IN"));
        assertTrue(wrapper.getSqlSegment().contains("batch_execution_id IS NULL"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("FILL"));
        assertFalse(wrapper.getParamNameValuePairs().containsValue("REVIEW"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(9001L));
    }

    @Test
    void candidateProjectionReturnsFrozenNodeAndApplicationVersion() {
        MesProEdhrWorkTaskMapper workTaskMapper = mock(MesProEdhrWorkTaskMapper.class);
        MesProEdhrBatchExecutionTaskMapper batchTaskMapper = mock(MesProEdhrBatchExecutionTaskMapper.class);
        MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper =
                mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        MesProEdhrWorkTaskServiceImpl service = new MesProEdhrWorkTaskServiceImpl();
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "batchTaskMapper", batchTaskMapper);
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", applicationMapper);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "roleApi", mock(RoleApi.class));
        ReflectionTestUtils.setField(service, "deptApi", mock(DeptApi.class));

        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setId(9201L)
                .setTaskType("FILL")
                .setBatchExecutionId(9001L)
                .setBatchTaskId(9101L)
                .setBusinessScopeType("RELEASE_REPORT_NODE")
                .setBusinessScopeId(9101L)
                .setAssigneeUserId(7101L)
                .setCandidateSourceType("FROZEN_REPORT_OWNER")
                .setCandidateUserSnapshot("7101")
                .setStatus(MesProEdhrWorkTaskStatus.TODO);
        MesProEdhrBatchExecutionTaskDO batchTask = new MesProEdhrBatchExecutionTaskDO()
                .setId(9101L)
                .setBatchExecutionId(9001L)
                .setNodeType("INCOMING_INSPECTION_REPORT")
                .setProcessName("来料检报告");
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setBatchExecutionId(9001L)
                        .setVersion(4);
        when(workTaskMapper.selectCandidateTodoPage(any(), eq(7101L), eq(MesProEdhrWorkTaskStatus.TODO)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(batchTaskMapper.selectByIds(any())).thenReturn(List.of(batchTask));
        when(applicationMapper.selectListByBatchExecutionIds(any())).thenReturn(List.of(application));
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(7101L);
        user.setNickname("来料检负责人");
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(7101L, user));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7101L);
            PageResult<MesProEdhrWorkTaskRespVO> result = service.getCandidateSignatureTodoPage(
                    new MesProEdhrWorkTaskPageReqVO());

            assertEquals("INCOMING_INSPECTION_REPORT", result.getList().get(0).getNodeType());
            assertEquals("来料检报告", result.getList().get(0).getNodeName());
            assertEquals(4, result.getList().get(0).getVersion());
        }
        verify(applicationMapper).selectListByBatchExecutionIds(
                argThat(ids -> ids != null && Set.copyOf(ids).equals(Set.of(9001L))));
    }

    @Test
    void pqcCandidateWithoutBatchExecutionStillProjectsApplicationVersion() {
        MesProEdhrWorkTaskMapper workTaskMapper = mock(MesProEdhrWorkTaskMapper.class);
        MesProEdhrBatchExecutionTaskMapper batchTaskMapper = mock(MesProEdhrBatchExecutionTaskMapper.class);
        MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper =
                mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        MesProEdhrWorkTaskServiceImpl service = new MesProEdhrWorkTaskServiceImpl();
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "batchTaskMapper", batchTaskMapper);
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", applicationMapper);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "roleApi", mock(RoleApi.class));
        ReflectionTestUtils.setField(service, "deptApi", mock(DeptApi.class));

        MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO()
                .setId(8201L)
                .setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(7001L)
                .setAssigneeUserId(7101L)
                .setCandidateUserSnapshot("7101")
                .setStatus(MesProEdhrWorkTaskStatus.TODO);
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO().setId(7001L).setVersion(1);
        when(workTaskMapper.selectCandidateTodoPage(any(), eq(7101L), eq(MesProEdhrWorkTaskStatus.TODO)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(applicationMapper.selectByIds(any())).thenReturn(List.of(application));
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(7101L);
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(7101L, user));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7101L);
            PageResult<MesProEdhrWorkTaskRespVO> result = service.getCandidateSignatureTodoPage(
                    new MesProEdhrWorkTaskPageReqVO());

            assertEquals(1, result.getList().get(0).getVersion());
            assertEquals("PQC_PRODUCTION_RELEASE", result.getList().get(0).getTaskType());
        }
    }
}
