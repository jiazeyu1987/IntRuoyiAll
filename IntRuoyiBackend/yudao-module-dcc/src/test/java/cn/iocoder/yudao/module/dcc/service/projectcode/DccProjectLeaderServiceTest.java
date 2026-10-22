package cn.iocoder.yudao.module.dcc.service.projectcode;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectLeaderService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Import(DccProjectLeaderService.class)
class DccProjectLeaderServiceTest extends BaseDbUnitTest {
    @Resource private DccProjectLeaderService service;
    @Resource private DccProjectCodeMapper mapper;
    @MockitoBean private AdminUserApi users;
    @Test void formalLeaderCannotAuthorizeOtherTenantProject() {
        var project = DccProjectCodeDO.builder().projectName("其他租户项目").projectCode("OTHER")
                .projectLeaderUserId(7L).status("ENABLE").build();
        project.setTenantId(2L); mapper.insert(project);
        var user = new AdminUserRespDTO(); user.setId(7L); user.setStatus(0);
        when(users.getUser(7L)).thenReturn(user);
        assertThrows(RuntimeException.class, () -> service.assertProjectLeader(7L, project.getId()));
    }
    @Test void onlyFormalLeaderMatchesNoAdminOrOwnerBypass() {
        var project = DccProjectCodeDO.builder().projectName("负责人项目").projectCode("L")
                .projectLeader("admin").projectLeaderUserId(7L).status("ENABLE").build();
        project.setTenantId(1L); mapper.insert(project);
        var user = new AdminUserRespDTO(); user.setId(7L); user.setStatus(0); user.setNickname("负责人");
        when(users.getUser(7L)).thenReturn(user);
        assertDoesNotThrow(() -> service.assertProjectLeader(7L, project.getId()));
        for (Long other : new Long[] {1L, 8L, 99L, null}) {
            assertThrows(RuntimeException.class, () -> service.assertProjectLeader(other, project.getId()));
        }
        verify(users, never()).getUser(1L);
        user.setStatus(1);
        assertThrows(RuntimeException.class, () -> service.assertProjectLeader(7L, project.getId()));
    }
    @Test void legacyLeaderTextAndDisabledProjectAreNotAuthority() {
        var project = DccProjectCodeDO.builder().projectName("旧项目").projectCode("OLD")
                .projectLeader("负责人").status("ENABLE").build();
        project.setTenantId(1L); mapper.insert(project);
        assertThrows(RuntimeException.class, () -> service.assertProjectLeader(7L, project.getId()));
        project.setProjectLeaderUserId(7L); project.setStatus("DISABLE"); mapper.updateById(project);
        assertThrows(RuntimeException.class, () -> service.assertProjectLeader(7L, project.getId()));
        assertThrows(RuntimeException.class, () -> service.assertProjectLeader(7L, 999L));
        assertThrows(RuntimeException.class, () -> service.requireEnabledAccount(8L));
    }
}
