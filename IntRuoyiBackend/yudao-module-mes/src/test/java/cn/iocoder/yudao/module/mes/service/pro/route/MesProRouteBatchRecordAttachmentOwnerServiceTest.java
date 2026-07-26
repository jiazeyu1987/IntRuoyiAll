package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerInitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionGateService;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleCategoryMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteBatchRecordAttachmentOwnerServiceTest {

    @InjectMocks
    private MesProRouteFlowConfigServiceImpl service;

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProEdhrPermissionGateService permissionGateService;
    @Mock
    private MesProRouteCandidateConfigService routeCandidateConfigService;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private RoleService roleService;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RoleCategoryMapper roleCategoryMapper;
    @Mock
    private PermissionService permissionService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void initializeBatchRecordAttachmentOwners_shouldCreateDefaultRolesAndAssignEnabledTenantUsers() {
        TenantContextHolder.setTenantId(11L);
        stubDraftCandidate();
        when(roleCategoryMapper.selectByCode("batch-record")).thenReturn(enabledCategory());
        when(adminUserService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of(
                enabledUser(101L), enabledUser(102L), enabledUser(103L), enabledUser(104L), enabledUser(105L)));
        when(roleMapper.selectByCode(any())).thenReturn(null);
        AtomicLong roleId = new AtomicLong(900L);
        when(roleService.createRole(any(RoleSaveReqVO.class), isNull())).thenAnswer(invocation -> roleId.incrementAndGet());
        when(permissionService.getUserRoleIdListByRoleId(anySet())).thenReturn(Collections.emptySet());
        when(permissionService.getUserRoleIdListByUserId(any())).thenReturn(Collections.emptySet());

        List<MesProRouteBatchRecordAttachmentOwnerRespVO> result = service.initializeBatchRecordAttachmentOwners(
                new MesProRouteBatchRecordAttachmentOwnerInitReqVO().setRouteId(10L).setRouteVersionId(1002L));

        assertEquals(4, result.size());
        assertEquals("来料检报告", result.get(0).getAttachmentName());
        assertEquals("来料检报告上传1", result.get(0).getDefaultRoleName());
        assertEquals("ROLE", result.get(0).getCandidateSourceType());
        assertEquals(1, result.get(0).getCandidateSourceIds().size());
        assertTrue(result.stream().allMatch(owner -> owner.getAssignedUserIds().size() >= 2
                && owner.getAssignedUserIds().size() <= 4));
        assertTrue(result.stream().allMatch(owner -> owner.getAssignedUserNames().size() >= 2
                && owner.getAssignedUserNames().size() <= 4));

        ArgumentCaptor<RoleSaveReqVO> roleCaptor = ArgumentCaptor.forClass(RoleSaveReqVO.class);
        verify(roleService, times(4)).createRole(roleCaptor.capture(), isNull());
        assertEquals("来料检报告上传1", roleCaptor.getAllValues().get(0).getName());
        assertEquals(77L, roleCaptor.getAllValues().get(0).getCategoryId());

        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Set<Long>> roleSetCaptor = ArgumentCaptor.forClass(Set.class);
        verify(permissionService, atLeast(8)).assignUserRole(userCaptor.capture(), roleSetCaptor.capture());
        verify(permissionService, atMost(16)).assignUserRole(any(), anySet());
        assertTrue(userCaptor.getAllValues().stream().allMatch(userId -> userId >= 101L && userId <= 105L));
        assertTrue(roleSetCaptor.getAllValues().stream().allMatch(roleIds -> roleIds.stream().allMatch(id -> id >= 901L && id <= 904L)));
        assertTrue(roleSetCaptor.getAllValues().size() >= 8);
        assertTrue(roleSetCaptor.getAllValues().size() <= 16);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("batchRecordAttachmentOwners"), any());
    }

    @Test
    void initializeBatchRecordAttachmentOwners_shouldFailWhenCurrentTenantHasLessThanTwoEnabledUsers() {
        stubDraftCandidate();
        when(roleCategoryMapper.selectByCode("batch-record")).thenReturn(enabledCategory());
        when(adminUserService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of(enabledUser(101L)));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.initializeBatchRecordAttachmentOwners(
                new MesProRouteBatchRecordAttachmentOwnerInitReqVO().setRouteId(10L).setRouteVersionId(1002L)));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_ENABLED_USER_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    void saveBatchRecordAttachmentOwners_shouldRejectUserOutsideCurrentTenantEnabledUsers() {
        stubDraftCandidate();
        when(adminUserService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus())).thenReturn(List.of(
                enabledUser(101L), enabledUser(102L)));
        MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO item =
                new MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO()
                        .setAttachmentCode("INCOMING_INSPECTION_REPORT")
                        .setCandidateSourceType("USERS")
                        .setCandidateSourceIds(List.of(101L, 999L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBatchRecordAttachmentOwners(
                new MesProRouteBatchRecordAttachmentOwnerSaveReqVO()
                        .setRouteId(10L)
                        .setRouteVersionId(1002L)
                        .setItems(List.of(item))));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID.getCode(), ex.getCode());
    }

    private void stubDraftCandidate() {
        when(routeMapper.selectById(10L)).thenReturn(MesProRouteDO.builder().id(10L).code("R-001").name("路线").build());
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {"routeId":10,"routeCode":"R-001","routeName":"路线","configSnapshots":{"flowGraph":{"nodes":[{"routeProcessId":100,"processId":200,"sort":1}]}}}
                        """)
                .build());
    }

    private RoleCategoryDO enabledCategory() {
        RoleCategoryDO category = new RoleCategoryDO();
        category.setId(77L);
        category.setCode("batch-record");
        category.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return category;
    }

    private AdminUserDO enabledUser(Long id) {
        AdminUserDO user = new AdminUserDO();
        user.setId(id);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        user.setNickname("用户" + id);
        return user;
    }
}
