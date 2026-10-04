package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class DccWorkflowDistributionDirectoryTest extends BaseMockitoUnitTest {
    @Mock private DccControlledFileMapper controlledFileMapper;
    @Mock private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock private DeptApi deptApi;
    @Mock private AdminUserApi adminUserApi;
    @Mock private PermissionApi permissionApi;
    @InjectMocks private DccWorkflowDistributionService service;
    private DccControlledFileDO file;
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        file=DccControlledFileDO.builder().id(42L).tenantId(1L).categoryId(10L).status("CONTROLLED_PENDING_EFFECTIVE")
                .controlledTime(LocalDateTime.now()).build();
        when(controlledFileMapper.selectById(42L)).thenAnswer(ignored->file);
        when(permissionSupport.hasCategoryPermission(10L,99L,DccFileCategoryPermissionActionEnum.DISTRIBUTE)).thenReturn(true);
        when(permissionApi.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        when(adminUserApi.getUserListByDeptIds(List.of(51L))).thenReturn(List.of(user(100L,0),user(101L,1)));
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }
    @Test void enabledSelectedDepartmentAccountsAreProjectedWithExactStringIds() {
        long exactId=9007199254740993L;
        when(adminUserApi.getUserListByDeptIds(List.of(51L))).thenReturn(List.of(user(exactId,0),user(101L,1)));
        var options=service.recipientOptions(99L,42L,51L);
        assertEquals(1,options.size());assertEquals(exactId,options.get(0).id());
        assertTrue(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(options).contains("\"9007199254740993\""));
        verify(deptApi).validateDeptList(List.of(51L));
    }
    @Test void duplicateOrNullDirectoryIdentityCannotBeShownAsACompleteRecipientRoster() {
        when(adminUserApi.getUserListByDeptIds(List.of(51L))).thenReturn(List.of(user(100L,0),user(100L,0)));
        assertThrows(IllegalStateException.class,()->service.recipientOptions(99L,42L,51L));
        when(adminUserApi.getUserListByDeptIds(List.of(51L))).thenReturn(java.util.Arrays.asList(user(100L,0),null));
        assertThrows(IllegalStateException.class,()->service.recipientOptions(99L,42L,51L));
    }
    @Test void wrongTenantStateOrPermissionDoesNotReadTheUserDirectory() {
        TenantContextHolder.setTenantId(122L);
        assertThrows(IllegalArgumentException.class,()->service.recipientOptions(99L,42L,51L));
        TenantContextHolder.setTenantId(1L);file.setStatus("OBSOLETE");
        assertThrows(IllegalArgumentException.class,()->service.recipientOptions(99L,42L,51L));
        file.setStatus("ACTIVE");when(permissionSupport.hasCategoryPermission(any(),any(),any())).thenReturn(false);
        assertThrows(IllegalArgumentException.class,()->service.recipientOptions(99L,42L,51L));
        verifyNoInteractions(adminUserApi,deptApi);
    }
    @Test void wrongDepartmentOrMissingDirectoryResponseFailsExplicitly() {
        when(adminUserApi.getUserListByDeptIds(List.of(51L))).thenReturn(List.of(user(100L,0).setDeptId(52L)));
        assertThrows(IllegalStateException.class,()->service.recipientOptions(99L,42L,51L));
        when(adminUserApi.getUserListByDeptIds(List.of(51L))).thenReturn(null);
        assertThrows(IllegalStateException.class,()->service.recipientOptions(99L,42L,51L));
    }
    @Test void menuPermissionWithoutDocumentControlRoleCannotReadOrCreateDistribution() {
        when(permissionApi.hasAnyRoles(99L,"doc_control")).thenReturn(false);
        assertThrows(IllegalArgumentException.class,()->service.recipientOptions(99L,42L,51L));
        when(controlledFileMapper.selectByIdAndTenantForUpdate(1L,42L)).thenReturn(file);
        var scope=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkflowDistributionReqVO.Scope();
        scope.setDepartmentId(51L);scope.setDistributionMedium("PUBLIC_FOLDER");scope.setRecipientUserIds(List.of(100L));
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkflowDistributionReqVO();request.setScopes(List.of(scope));
        assertThrows(IllegalArgumentException.class,()->service.distribute(99L,42L,request));
        verifyNoInteractions(adminUserApi,deptApi);
    }
    private AdminUserRespDTO user(long id,int status) { return new AdminUserRespDTO().setId(id).setStatus(status).setDeptId(51L).setNickname("接收人"+id); }
}
