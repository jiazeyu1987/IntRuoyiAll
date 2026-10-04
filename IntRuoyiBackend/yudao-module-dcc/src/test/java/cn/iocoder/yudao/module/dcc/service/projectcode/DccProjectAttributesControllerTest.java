package cn.iocoder.yudao.module.dcc.service.projectcode;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectAttributesController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.*;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Import({DccProjectAttributesController.class, DccProjectAttributesService.class, DccProjectLeaderService.class, DccProjectConfigurationAuditService.class})
class DccProjectAttributesControllerTest extends BaseDbUnitTest {
    @Resource private DccProjectAttributesController controller;
    @Resource private DccProjectAttributesService attributes;
    @Resource private DccProjectCodeMapper mapper;
    @MockitoBean private AdminUserApi users;
    @MockitoBean private GxpAuditService audit;
    @MockitoBean private DccProjectAccessService access;
    @MockitoBean private DccFolderTemplateService folders;
    @MockitoBean private cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService placementService;
    private DccProjectAttributes defaults(String market) { return new DccProjectAttributes(List.of(market), null, "Y", "N", "N", null); }
    private Long project() {
        var project = DccProjectCodeDO.builder().projectName("配置API项目").projectCode("B-CONFIG").status("ENABLE")
                .defaultAttributesJson(attributes.encode(defaults("NMPA"))).build();
        project.setTenantId(1L); mapper.insert(project); return project.getId();
    }
    @BeforeEach void account() {
        var user = new AdminUserRespDTO(); user.setId(7L); user.setStatus(0); user.setNickname("负责人");
        when(users.getUser(7L)).thenReturn(user);
    }
    @Test void configurePersistsFormalAccountAndDefaultsButKeepsSavedApplication() {
        Long id = project();
        attributes.saveDraft(7L, id, "OBSOLETE", 81L, 1, defaults("CE"));
        controller.configure(id, new DccProjectAttributesController.Configuration(7L, defaults("FDA"), "修改项目默认"));
        var saved = mapper.selectById(id);
        assertEquals(7L, saved.getProjectLeaderUserId());
        assertEquals("负责人", saved.getProjectLeader());
        assertEquals(defaults("FDA"), attributes.defaults(saved));
        assertEquals(defaults("CE"), attributes.readValue(attributes.readSaved(7L, id, "OBSOLETE", 81L, 1).getActualAttributesJson()));
        var command = ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(audit).append(command.capture());
        assertEquals("dcc.project-attributes.configure", command.getValue().getOperationId());
        assertTrue(command.getValue().getBeforeState().getCanonicalJson().contains("NMPA"));
        assertTrue(command.getValue().getAfterState().getCanonicalJson().contains("FDA"));
    }
    @Test void auditFailureRollsBackConfiguration() {
        Long id = project();
        doThrow(new IllegalStateException("统一审计失败")).when(audit).append(any());
        assertThrows(RuntimeException.class, () -> controller.configure(id,
                new DccProjectAttributesController.Configuration(7L, defaults("FDA"), "修改项目默认")));
        assertEquals(defaults("NMPA"), attributes.defaults(mapper.selectById(id)));
        assertNull(mapper.selectById(id).getProjectLeaderUserId());
    }
    @Test void missingReasonInvalidAccountAndUnknownProjectReject() {
        Long id = project();
        assertThrows(RuntimeException.class, () -> controller.configure(id,
                new DccProjectAttributesController.Configuration(7L, defaults("FDA"), " ")));
        assertThrows(RuntimeException.class, () -> controller.configure(id,
                new DccProjectAttributesController.Configuration(99L, defaults("FDA"), "修改")));
        assertThrows(RuntimeException.class, () -> controller.configure(999L,
                new DccProjectAttributesController.Configuration(7L, defaults("FDA"), "修改")));
        verifyNoInteractions(audit);
    }
    @Test void configurationAndInitializationEndpointsKeepExplicitPermissions() throws Exception {
        assertEquals("@ss.hasPermission('dcc:project-code:update')", DccProjectAttributesController.class
                .getMethod("configure", Long.class, DccProjectAttributesController.Configuration.class).getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasAnyPermissions('dcc:project-code:query','dcc:controlled-file:submit','dcc:controlled-file:query')", DccProjectAttributesController.class
                .getMethod("defaults", Long.class, String.class).getAnnotation(PreAuthorize.class).value());
    }
}
