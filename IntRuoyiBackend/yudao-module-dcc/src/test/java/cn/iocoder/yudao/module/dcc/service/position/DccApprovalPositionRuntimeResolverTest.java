package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccApprovalPositionRuntimeResolverTest extends BaseMockitoUnitTest {

    @Mock
    private DccApprovalPositionMapper positionMapper;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private DeptService deptService;

    private DccApprovalPositionRuntimeResolver resolver;

    @BeforeEach
    void setUpResolver() {
        resolver = new DccApprovalPositionRuntimeResolver(positionMapper, adminUserService, deptService);
    }

    @Test
    void resolveUserIds_directManagerPosition_usesLocalSubmitterDepartmentLeader() {
        when(positionMapper.selectById(900302L)).thenReturn(position(900302L, "编制人直接主管"));
        when(adminUserService.getUser(99L)).thenReturn(localUser(99L, "uploader", 10L));
        when(deptService.getDept(10L)).thenReturn(dept(10L, 101L));
        when(adminUserService.getUser(101L)).thenReturn(localUser(101L, "leader", 10L));

        List<Long> result = resolver.resolveUserIds(900302L, 99L, false);

        assertEquals(List.of(101L), result);
    }

    @Test
    void resolveUserIds_directManagerPosition_withoutLocalLeader_returnsPreciseLocalBlocker() {
        when(positionMapper.selectById(900302L)).thenReturn(position(900302L, "编制人直接主管"));
        when(adminUserService.getUser(99L)).thenReturn(localUser(99L, "uploader", 10L));
        when(deptService.getDept(10L)).thenReturn(dept(10L, null));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> resolver.resolveUserIds(900302L, 99L, false));

        assertEquals(APPROVAL_POSITION_UPLOADER_MAPPING_INVALID.getCode(), exception.getCode());
        assertEquals(
                "Approval position runtime mapping failed: 编制人直接主管 requires a local department leader for the submitter",
                exception.getMessage());
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
