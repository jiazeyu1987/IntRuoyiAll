package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorService;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserDirectSupervisorRespDTO;
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_MAPPING_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccApprovalPositionRuntimeResolverTest extends BaseMockitoUnitTest {

    @Mock
    private DccApprovalPositionMapper positionMapper;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private DeptService deptService;
    @Mock private DccPositionAssignmentMapper assignmentMapper;
    @Mock private AdminUserDirectSupervisorService directSupervisorService;

    private DccApprovalPositionRuntimeResolver resolver;

    @BeforeEach
    void setUpResolver() {
        resolver = new DccApprovalPositionRuntimeResolver(positionMapper, adminUserService, deptService);
        ReflectionTestUtils.setField(resolver, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(resolver, "directSupervisorService", directSupervisorService);
    }

    @Test
    void resolveUserIds_directManagerPosition_usesStoredDirectSupervisor() {
        when(positionMapper.selectById(900302L)).thenReturn(position(900302L, "编制人直接主管"));
        when(adminUserService.getUser(99L)).thenReturn(localUser(99L, "uploader", 10L));
        when(directSupervisorService.getDirectSupervisorRelation(99L)).thenReturn(new AdminUserDirectSupervisorRespDTO(1L, 99L, 101L, 1L));
        when(adminUserService.getUser(101L)).thenReturn(localUser(101L, "leader", 10L));

        List<Long> result = resolver.resolveUserIds(900302L, 99L, false);

        assertEquals(List.of(101L), result);
        verifyNoInteractions(deptService);
    }

    @Test
    void resolveUserIds_directManagerPosition_withoutSupervisorDoesNotUseDepartmentLeader() {
        when(positionMapper.selectById(900302L)).thenReturn(position(900302L, "编制人直接主管"));
        when(adminUserService.getUser(99L)).thenReturn(localUser(99L, "uploader", 10L));
        when(directSupervisorService.getDirectSupervisorRelation(99L)).thenReturn(new AdminUserDirectSupervisorRespDTO(1L, 99L, null, 1L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> resolver.resolveUserIds(900302L, 99L, false));

        assertEquals(APPROVAL_POSITION_UPLOADER_MAPPING_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("直属主管未配置"));
        verifyNoInteractions(deptService);
        assertFalse(exception.getMessage().contains("IntAuth"));
    }

    @Test
    void resolveUserIds_departmentScopedPosition_usesLocalSubmitterDepartmentLeader() {
        when(positionMapper.selectById(900333L)).thenReturn(position(900333L, "部门负责人"));
        when(adminUserService.getUser(99L)).thenReturn(localUser(99L, "uploader", 10L));
        when(deptService.getDept(10L)).thenReturn(dept(10L, 102L));
        when(adminUserService.getUser(102L)).thenReturn(localUser(102L, "dept-leader", 10L));

        List<Long> result = resolver.resolveUserIds(900333L, 99L, false);

        assertEquals(List.of(102L), result);
    }

    @Test
    void resolveUserIds_deferredPreviewWithoutSubmitterContext_returnsEmptyForContextDerivedPosition() {
        when(positionMapper.selectById(900302L)).thenReturn(position(900302L, "编制人直接主管"));

        List<Long> result = resolver.resolveUserIds(900302L, null, true);

        assertEquals(List.of(), result);
        verifyNoInteractions(adminUserService, deptService);
    }

    @Test
    void isUploaderDerivedPosition_excludesAuthorizedRepresentativeSoDccAssignmentsCanResolveIt() {
        when(positionMapper.selectById(900334L)).thenReturn(position(900334L, "部门授权代表"));

        assertFalse(resolver.isUploaderDerivedPosition(900334L));
    }

    @Test
    void explicitRelationshipsResolveAgainstEachApplicantAndDeduplicate() {
        when(positionMapper.selectById(1L)).thenReturn(position(1L, "动态审核"));
        when(assignmentMapper.selectActiveListByPositionId(1L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().assignmentType("DIRECT_SUPERVISOR").active(true).build(),
                DccPositionAssignmentDO.builder().assignmentType("DEPT_LEADER").active(true).build(),
                DccPositionAssignmentDO.builder().assignmentType("USER").userId(101L).active(true).build()));
        when(adminUserService.getUser(99L)).thenReturn(localUser(99L, "applicant", 10L));
        when(adminUserService.getUser(101L)).thenReturn(localUser(101L, "supervisor", 20L));
        when(adminUserService.getUser(102L)).thenReturn(localUser(102L, "head", 10L));
        when(deptService.getDept(10L)).thenReturn(dept(10L, 102L));
        when(directSupervisorService.getDirectSupervisorRelation(99L)).thenReturn(new AdminUserDirectSupervisorRespDTO(1L, 99L, 101L, 1L));
        assertTrue(resolver.isUploaderDerivedPosition(1L));
        assertEquals(List.of(101L, 102L), resolver.resolveUserIds(1L, 99L, false));
        when(adminUserService.getUser(98L)).thenReturn(localUser(98L, "another-applicant", 10L));
        when(directSupervisorService.getDirectSupervisorRelation(98L)).thenReturn(new AdminUserDirectSupervisorRespDTO(1L, 98L, 102L, 3L));
        assertEquals(List.of(102L, 101L), resolver.resolveUserIds(1L, 98L, false));
    }

    @Test
    void explicitRelationshipMissingContextBlocksExecutionButAllowsDeferredPreview() {
        when(positionMapper.selectById(1L)).thenReturn(position(1L, "动态审核"));
        when(assignmentMapper.selectActiveListByPositionId(1L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().assignmentType("DIRECT_SUPERVISOR").active(true).build()));
        assertEquals(List.of(), resolver.resolveUserIds(1L, null, true));
        assertThrows(ServiceException.class, () -> resolver.resolveUserIds(1L, null, false));
    }

    @Test
    void missingRelationshipCannotBeHiddenByAnotherFixedAssignee() {
        when(positionMapper.selectById(1L)).thenReturn(position(1L, "动态审核"));
        when(assignmentMapper.selectActiveListByPositionId(1L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().assignmentType("USER").userId(102L).active(true).build(),
                DccPositionAssignmentDO.builder().assignmentType("DIRECT_SUPERVISOR").active(true).build()));
        when(adminUserService.getUser(99L)).thenReturn(localUser(99L, "applicant", 10L));
        when(directSupervisorService.getDirectSupervisorRelation(99L)).thenReturn(new AdminUserDirectSupervisorRespDTO(1L, 99L, null, 1L));
        assertThrows(ServiceException.class, () -> resolver.resolveUserIds(1L, 99L, false));
    }

    private static DccApprovalPositionDO position(Long id, String name) {
        return DccApprovalPositionDO.builder()
                .id(id)
                .code("POS-" + id)
                .name(name)
                .active(Boolean.TRUE)
                .source("LOCAL")
                .remark("seed")
                .build();
    }

    private static AdminUserDO localUser(Long id, String username, Long deptId) {
        return AdminUserDO.builder()
                .id(id)
                .username(username)
                .nickname(username)
                .deptId(deptId)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
    }

    private static DeptDO dept(Long id, Long leaderUserId) {
        DeptDO dept = new DeptDO();
        dept.setId(id);
        dept.setName("部门" + id);
        dept.setLeaderUserId(leaderUserId);
        dept.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return dept;
    }

}
