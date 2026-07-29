package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchFullConfigImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchManualReplanDataImportRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.PostConfigPackageService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleConfigPackageService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProSchedulerWorkbenchFullConfigPackageServiceTest {

    @InjectMocks
    private MesProSchedulerWorkbenchFullConfigPackageServiceImpl service;

    @Mock
    private PostConfigPackageService postConfigPackageService;
    @Mock
    private RoleConfigPackageService roleConfigPackageService;
    @Mock
    private MesProSchedulerWorkbenchRouteConfigPackageService routeConfigPackageService;
    @Mock
    private MesProSchedulerWorkbenchManualReplanDataPackageService manualReplanDataPackageService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RoleService roleService;
    @Mock
    private AdminUserService adminUserService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exportPackage_shouldContainAllSubPackagesAndUserRoleBindings() throws Exception {
        when(postConfigPackageService.exportPackage()).thenReturn("{\"packageVersion\":\"1\",\"posts\":[]}"
                .getBytes(StandardCharsets.UTF_8));
        when(roleConfigPackageService.exportPackage()).thenReturn("{\"packageVersion\":\"1\",\"roles\":[]}"
                .getBytes(StandardCharsets.UTF_8));
        when(routeConfigPackageService.exportPackage()).thenReturn("{\"packageVersion\":\"scheduler-route-config.v1\",\"routes\":[]}"
                .getBytes(StandardCharsets.UTF_8));
        when(manualReplanDataPackageService.exportPackage()).thenReturn("{\"packageVersion\":\"scheduler-manual-replan-data.v1\"}"
                .getBytes(StandardCharsets.UTF_8));
        when(permissionService.getUserRoleIdListByRoleId(Set.of(11L, 12L))).thenReturn(Set.of(101L));
        when(roleService.getRoleList()).thenReturn(List.of(
                role(11L, "super_admin", "超级管理员"),
                role(12L, "mes_scheduler", "排产员")
        ));
        when(adminUserService.getUserList(Set.of(101L))).thenReturn(List.of(
                AdminUserDO.builder().id(101L).username("smokeplan1").nickname("排产冒烟员").build()
        ));
        when(permissionService.getUserRoleIdListByUserId(101L)).thenReturn(Set.of(11L, 12L));

        byte[] exported = service.exportPackage();
        JsonNode root = objectMapper.readTree(new String(exported, StandardCharsets.UTF_8));

        assertEquals("scheduler-workbench-full-config.v1", root.path("packageVersion").asText());
        assertEquals("1", root.path("postConfigPackage").path("packageVersion").asText());
        assertEquals("1", root.path("roleConfigPackage").path("packageVersion").asText());
        assertEquals("scheduler-route-config.v1", root.path("routeConfigPackage").path("packageVersion").asText());
        assertEquals("scheduler-manual-replan-data.v1",
                root.path("manualReplanDataPackage").path("packageVersion").asText());
        assertEquals(1, root.path("userRoleBindings").size());
        assertEquals("smokeplan1", root.path("userRoleBindings").get(0).path("username").asText());
        assertEquals(2, root.path("userRoleBindings").get(0).path("roleCodes").size());
    }

    @Test
    void importPackage_shouldFailFastWhenUserRoleBindingsMissing() {
        byte[] invalid = """
                {
                  "packageVersion":"scheduler-workbench-full-config.v1",
                  "postConfigPackage":{"packageVersion":"1","posts":[]},
                  "roleConfigPackage":{"packageVersion":"1","roles":[]},
                  "routeConfigPackage":{"packageVersion":"scheduler-route-config.v1","routes":[]},
                  "manualReplanDataPackage":{"packageVersion":"scheduler-manual-replan-data.v1"}
                }
                """.getBytes(StandardCharsets.UTF_8);

        assertServiceException(() -> service.importPackage(invalid),
                CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包缺少 userRoleBindings");
    }

    @Test
    void importPackage_shouldFailFastWhenManualReplanDataPackageMissing() {
        byte[] invalid = """
                {
                  "packageVersion":"scheduler-workbench-full-config.v1",
                  "postConfigPackage":{"packageVersion":"1","posts":[]},
                  "roleConfigPackage":{"packageVersion":"1","roles":[]},
                  "routeConfigPackage":{"packageVersion":"scheduler-route-config.v1","routes":[]},
                  "userRoleBindings":[]
                }
                """.getBytes(StandardCharsets.UTF_8);

        assertServiceException(() -> service.importPackage(invalid),
                CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包缺少手动重排数据包");
    }

    @Test
    void importPackage_shouldExposeSpecificErrorWhenSubPackageBase64Invalid() {
        byte[] invalid = """
                {
                  "packageVersion":"scheduler-workbench-full-config.v1",
                  "postConfigPackageBase64":"not-base64",
                  "roleConfigPackage":{"packageVersion":"2","roles":[]},
                  "routeConfigPackage":{"packageVersion":"scheduler-route-config.v1","routes":[{"routeId":10,"routeCode":"ROUTE-001","routeVersionId":100,"useProcessConfigs":[],"scheduleConfigs":[],"resources":[]}]},
                  "manualReplanDataPackage":{"packageVersion":"scheduler-manual-replan-data.v1"},
                  "userRoleBindings":[]
                }
                """.getBytes(StandardCharsets.UTF_8);

        assertServiceException(() -> service.importPackage(invalid),
                CONFIG_PACKAGE_CONTENT_INVALID,
                "排产员工作台全量配置包中的岗位配置包 Base64 非法");
    }

    @Test
    void importPackage_shouldExposeSpecificErrorWhenUserMissing() {
        when(adminUserService.getUserByUsername("smokeplan1")).thenReturn(null);
        when(roleService.getRoleList()).thenReturn(List.of(role(11L, "super_admin", "超级管理员")));
        when(manualReplanDataPackageService.importPackage(any())).thenReturn(manualReplanImportResp(3, 4, 5));

        byte[] payload = """
                {
                  "packageVersion":"scheduler-workbench-full-config.v1",
                  "postConfigPackage":{"packageVersion":"1","posts":[]},
                  "roleConfigPackage":{"packageVersion":"1","roles":[]},
                  "routeConfigPackage":{"packageVersion":"scheduler-route-config.v1","routes":[{"routeId":10,"routeCode":"ROUTE-001","routeVersionId":100,"useProcessConfigs":[],"scheduleConfigs":[],"resources":[]}]},
                  "manualReplanDataPackage":{"packageVersion":"scheduler-manual-replan-data.v1"},
                  "userRoleBindings":[
                    {
                      "username":"smokeplan1",
                      "roleCodes":["super_admin"]
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);

        assertServiceException(() -> service.importPackage(payload),
                CONFIG_PACKAGE_REFERENCE_MISSING,
                "用户【smokeplan1】在目标环境不存在");
    }

    @Test
    void importPackage_shouldReplaySubPackagesAndAssignUserRolesByUsername() throws Exception {
        when(adminUserService.getUserByUsername("smokeplan1"))
                .thenReturn(AdminUserDO.builder().id(101L).username("smokeplan1").build());
        when(roleService.getRoleList()).thenReturn(List.of(
                role(11L, "super_admin", "超级管理员"),
                role(12L, "mes_scheduler", "排产员")
        ));
        when(manualReplanDataPackageService.importPackage(any())).thenReturn(manualReplanImportResp(3, 4, 5));

        byte[] payload = """
                {
                  "packageVersion":"scheduler-workbench-full-config.v1",
                  "postConfigPackage":{"packageVersion":"1","posts":[]},
                  "roleConfigPackage":{"packageVersion":"1","roles":[]},
                  "routeConfigPackage":{"packageVersion":"scheduler-route-config.v1","routes":[]},
                  "manualReplanDataPackage":{"packageVersion":"scheduler-manual-replan-data.v1"},
                  "userRoleBindings":[
                    {
                      "username":"smokeplan1",
                      "roleCodes":["super_admin","mes_scheduler"]
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);

        MesProSchedulerWorkbenchFullConfigImportRespVO result = service.importPackage(payload);

        verify(postConfigPackageService).importPackage(anyBytesCaptor().capture());
        verify(roleConfigPackageService).importPackage(anyBytesCaptor().capture());
        verify(manualReplanDataPackageService).importPackage(anyBytesCaptor().capture());
        verify(routeConfigPackageService).importPackage(anyBytesCaptor().capture());
        ArgumentCaptor<Set<Long>> roleIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(permissionService).assignUserRole(org.mockito.ArgumentMatchers.eq(101L), roleIdsCaptor.capture());
        assertEquals(Set.of(11L, 12L), roleIdsCaptor.getValue());
        assertEquals(1, result.getUserRoleBindingCount());
        assertEquals(2, result.getAssignedRoleCount());
        assertEquals(3, result.getReplanMasterDataCount());
        assertEquals(4, result.getReplanScheduleOrderDataCount());
        assertEquals(5, result.getReplanRuntimeDataCount());
    }

    @Test
    void importPackage_shouldExposeSpecificErrorWhenRoleCodeMissingAfterImport() {
        when(adminUserService.getUserByUsername("smokeplan1"))
                .thenReturn(AdminUserDO.builder().id(101L).username("smokeplan1").build());
        when(roleService.getRoleList()).thenReturn(List.of(role(11L, "super_admin", "超级管理员")));
        when(manualReplanDataPackageService.importPackage(any())).thenReturn(manualReplanImportResp(3, 4, 5));

        byte[] payload = """
                {
                  "packageVersion":"scheduler-workbench-full-config.v1",
                  "postConfigPackage":{"packageVersion":"1","posts":[]},
                  "roleConfigPackage":{"packageVersion":"1","roles":[]},
                  "routeConfigPackage":{"packageVersion":"scheduler-route-config.v1","routes":[{"routeId":10,"routeCode":"ROUTE-001","routeVersionId":100,"useProcessConfigs":[],"scheduleConfigs":[],"resources":[]}]},
                  "manualReplanDataPackage":{"packageVersion":"scheduler-manual-replan-data.v1"},
                  "userRoleBindings":[
                    {
                      "username":"smokeplan1",
                      "roleCodes":["super_admin","mes_scheduler"]
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);

        assertServiceException(() -> service.importPackage(payload),
                CONFIG_PACKAGE_REFERENCE_MISSING,
                "角色编码【mes_scheduler】在目标环境不存在，无法为用户【smokeplan1】回放角色");
    }

    private static ArgumentCaptor<byte[]> anyBytesCaptor() {
        return ArgumentCaptor.forClass(byte[].class);
    }

    private static MesProSchedulerWorkbenchManualReplanDataImportRespVO manualReplanImportResp(
            int masterDataCount, int scheduleOrderDataCount, int runtimeDataCount) {
        MesProSchedulerWorkbenchManualReplanDataImportRespVO respVO =
                new MesProSchedulerWorkbenchManualReplanDataImportRespVO();
        respVO.setMasterDataCount(masterDataCount);
        respVO.setScheduleOrderDataCount(scheduleOrderDataCount);
        respVO.setRuntimeDataCount(runtimeDataCount);
        return respVO;
    }

    private static RoleDO role(Long id, String code, String name) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        return role;
    }
}
