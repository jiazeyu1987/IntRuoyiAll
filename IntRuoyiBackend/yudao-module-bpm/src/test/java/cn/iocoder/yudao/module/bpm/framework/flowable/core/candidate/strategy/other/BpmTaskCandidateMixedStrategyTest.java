package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.other;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BpmTaskCandidateMixedStrategyTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BpmTaskCandidateMixedStrategy strategy;

    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private BpmProcessInstanceService processInstanceService;

    @Test
    public void testGetStrategy() {
        assertEquals(BpmTaskCandidateStrategyEnum.MIXED, strategy.getStrategy());
    }

    @Test
    public void testCalculateUsersByTask_mixedApprovalObjects() {
        // 准备参数：用户、权限角色、部门、发起对象直属主管混选
        String param = JsonUtils.toJsonString(asList(
                new BpmTaskCandidateMixedStrategy.Entry().setStrategy(BpmTaskCandidateStrategyEnum.USER.getStrategy()).setParam("10,11"),
                new BpmTaskCandidateMixedStrategy.Entry().setStrategy(BpmTaskCandidateStrategyEnum.ROLE.getStrategy()).setParam("20"),
                new BpmTaskCandidateMixedStrategy.Entry().setStrategy(BpmTaskCandidateStrategyEnum.DEPT_MEMBER.getStrategy()).setParam("30"),
                new BpmTaskCandidateMixedStrategy.Entry().setStrategy(BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy()).setParam("1")
        ));
        // mock 用户
        // mock 角色
        when(permissionApi.getUserRoleIdListByRoleIds(eq(asSet(20L)))).thenReturn(asSet(201L, 202L));
        // mock 部门
        when(adminUserApi.getUserListByDeptIds(eq(asSet(30L)))).thenReturn(asList(
                randomPojo(AdminUserRespDTO.class, o -> o.setId(301L)),
                randomPojo(AdminUserRespDTO.class, o -> o.setId(302L))));
        // mock 发起对象直属主管
        DelegateExecution execution = mock(DelegateExecution.class);
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(execution.getProcessInstanceId()).thenReturn("process-instance-id");
        when(processInstanceService.getProcessInstance(eq("process-instance-id"))).thenReturn(processInstance);
        when(processInstance.getStartUserId()).thenReturn("1");
        when(adminUserApi.getUser(eq(1L))).thenReturn(
                randomPojo(AdminUserRespDTO.class, o -> o.setId(1L).setDeptId(100L)));
        when(deptApi.getDept(eq(100L))).thenReturn(
                randomPojo(DeptRespDTO.class, o -> o.setId(100L).setLeaderUserId(1001L)));

        Set<Long> userIds = strategy.calculateUsersByTask(execution, param);

        assertEquals(asSet(10L, 11L, 201L, 202L, 301L, 302L, 1001L), userIds);
    }

    @Test
    public void testCalculateUsersByActivity_directLeader() {
        String param = JsonUtils.toJsonString(List.of(
                new BpmTaskCandidateMixedStrategy.Entry()
                        .setStrategy(BpmTaskCandidateStrategyEnum.START_USER_DEPT_LEADER.getStrategy())
                        .setParam("1")
        ));
        when(adminUserApi.getUser(eq(1L))).thenReturn(
                randomPojo(AdminUserRespDTO.class, o -> o.setId(1L).setDeptId(100L)));
        when(deptApi.getDept(eq(100L))).thenReturn(
                randomPojo(DeptRespDTO.class, o -> o.setId(100L).setLeaderUserId(1001L)));

        Set<Long> userIds = strategy.calculateUsersByActivity(null, null, param, 1L, null, null);

        assertEquals(asSet(1001L), userIds);
    }

    @Test
    public void testValidateParam_empty() {
        assertThrows(IllegalArgumentException.class, () -> strategy.validateParam("[]"));
    }

    @Test
    public void testValidateParam_unsupportedStrategy() {
        String param = JsonUtils.toJsonString(List.of(
                new BpmTaskCandidateMixedStrategy.Entry().setStrategy(BpmTaskCandidateStrategyEnum.EXPRESSION.getStrategy()).setParam("${1}")
        ));

        assertThrows(IllegalArgumentException.class, () -> strategy.validateParam(param));
    }

}
