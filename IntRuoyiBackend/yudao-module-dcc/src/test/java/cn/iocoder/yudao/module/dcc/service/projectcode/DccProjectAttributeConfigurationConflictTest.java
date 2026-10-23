package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectAttributesController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({DccProjectAttributesController.class, DccProjectAttributesService.class,
        DccProjectLeaderService.class, DccProjectConfigurationAuditService.class})
class DccProjectAttributeConfigurationConflictTest extends BaseDbUnitTest {
    @Resource private DccProjectAttributesController controller;
    @Resource private DccProjectAttributesService attributes;
    @Resource private DccProjectCodeMapper projects;
    @MockitoBean private AdminUserApi users;
    @MockitoBean private GxpAuditService audit;
    @MockitoBean private DccProjectAccessService access;
    @MockitoBean private DccFolderTemplateService folders;
    @MockitoBean private DccProjectFilePlacementService placements;

    @BeforeEach void accounts() {
        for (Long id : List.of(7L, 8L)) {
            var user = new AdminUserRespDTO(); user.setId(id); user.setStatus(0); user.setNickname("负责人" + id);
            when(users.getUser(id)).thenReturn(user);
        }
    }
    private DccProjectAttributes value(String market) {
        return new DccProjectAttributes(List.of(market), null, "Y", "N", "N", null);
    }
    private Long project() {
        var row = DccProjectCodeDO.builder().projectName("并发属性项目").projectCode("ATTR-CONFLICT")
                .projectLeaderUserId(7L).projectLeader("负责人7").status("ENABLE")
                .defaultAttributesJson(attributes.encode(value("NMPA"))).build();
        row.setTenantId(1L); projects.insert(row); return row.getId();
    }
    private DccProjectAttributesController.ConfigurationView read(Long id) {
        return controller.configuration(id).getData();
    }
    private String token(Long id) {
        return read(id).configurationToken();
    }
    private void save(Long id, Long leader, DccProjectAttributes actual, String expected) {
        controller.configure(id, new DccProjectAttributesController.Configuration(leader, actual, "明确修改配置", expected));
    }
    @Test void staleAttributesOrLeaderCannotOverwriteCurrentConfiguration() {
        Long id = project(); String baseline = token(id);
        attributes.saveDraft(7L, id, "UPLOAD", 901L, 1, value("CE"));
        save(id, 8L, value("FDA"), baseline);
        assertNotEquals(baseline, token(id));
        var stale = assertThrows(ServiceException.class, () -> save(id, 7L, value("CE"), baseline));
        assertTrue(stale.getMessage().contains("已变化"));
        assertEquals(8L, projects.selectById(id).getProjectLeaderUserId());
        assertEquals(value("FDA"), attributes.defaults(projects.selectById(id)));
        assertEquals(value("CE"), attributes.readValue(attributes.readSaved(7L,id,"UPLOAD",901L,1).getActualAttributesJson()));
        verify(audit, times(1)).append(any());
    }
    @Test void missingOrInvalidBaselineFailsBeforeWriting() {
        Long id = project(); String token = token(id);
        assertTrue(token.matches("[a-f0-9]{64}"));
        for (String invalid : new String[]{null, "", "a".repeat(64)}) {
            assertThrows(ServiceException.class, () -> save(id, 8L, value("FDA"), invalid));
        }
        assertEquals(value("NMPA"), attributes.defaults(projects.selectById(id)));
        verifyNoInteractions(audit);
    }
    @Test void malformedStoredAttributesCanBeReadAndExplicitlyRepaired() {
        Long id = project(); var row = projects.selectById(id);
        row.setDefaultAttributesJson("{invalid"); row.setProjectLeaderUserId(99L); projects.updateById(row);
        assertEquals("{invalid", read(id).defaultAttributesJson());
        save(id, 7L, value("FDA"), token(id));
        assertEquals(value("FDA"), attributes.defaults(projects.selectById(id)));
        assertEquals(7L, projects.selectById(id).getProjectLeaderUserId());
    }
    @Test void auditFailureDoesNotCommitValuesOrInvalidateExistingBaseline() {
        Long id = project(); String baseline = token(id);
        doThrow(new IllegalStateException("审计失败")).when(audit).append(any());
        assertThrows(IllegalStateException.class, () -> save(id,8L,value("FDA"),baseline));
        assertEquals(baseline,token(id));
        assertEquals(value("NMPA"),attributes.defaults(projects.selectById(id)));
    }
    @Test void leaderOnlyChangeStillInvalidatesPreviouslyReadAttributes() {
        Long id = project(); String baseline = token(id);
        save(id, 8L, value("NMPA"), baseline);
        assertThrows(ServiceException.class, () -> save(id,7L,value("FDA"),baseline));
        assertEquals(8L,projects.selectById(id).getProjectLeaderUserId());
        verify(audit,times(1)).append(any());
    }
    @Test void anotherProjectsTokenCannotAuthorizeThisProjectsSave() {
        Long id = project(); var other = projects.selectById(id);
        other.setId(null); other.setProjectCode("ATTR-CONFLICT-OTHER"); projects.insert(other);
        assertNotEquals(token(id),token(other.getId()));
        assertThrows(ServiceException.class, () -> save(id,8L,value("FDA"),token(other.getId())));
        assertEquals(value("NMPA"),attributes.defaults(projects.selectById(id)));
        verifyNoInteractions(audit);
    }
    @Test void repairStillRejectsInvalidNewValuesAndUnavailableNewAccount() {
        Long id = project(); String baseline = token(id);
        assertThrows(ServiceException.class, () -> save(id,99L,value("FDA"),baseline));
        var incomplete = new DccProjectAttributes(List.of("FDA"),null,"Y","N","Y",null);
        assertThrows(ServiceException.class, () -> save(id,7L,incomplete,baseline));
        assertEquals(baseline,token(id)); verifyNoInteractions(audit);
    }
    @Test void formalProjectAccessIsRequiredForBothReadAndSave() {
        Long id = project(); String baseline = token(id);
        doThrow(new IllegalArgumentException("无项目访问资格")).when(access).assertProjectViewerOrAbove(any(),eq(id));
        assertThrows(IllegalArgumentException.class, () -> read(id));
        assertThrows(IllegalArgumentException.class, () -> save(id,8L,value("FDA"),baseline));
        assertEquals(value("NMPA"),attributes.defaults(projects.selectById(id))); verifyNoInteractions(audit);
    }
    @Test void simultaneousSavesFromSameBaselineCommitOnlyOneDecision() throws Exception {
        Long id = project(); String baseline = token(id);
        var barrier = new CyclicBarrier(2);
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> concurrentSave(id,7L,"CE",baseline,barrier));
            var second = workers.submit(() -> concurrentSave(id,8L,"FDA",baseline,barrier));
            String firstResult = first.get(20,TimeUnit.SECONDS), secondResult = second.get(20,TimeUnit.SECONDS);
            assertEquals(1,List.of(firstResult,secondResult).stream().filter("CONFLICT"::equals).count());
            String winner = "CONFLICT".equals(firstResult) ? secondResult : firstResult;
            assertEquals(value(winner),attributes.defaults(projects.selectById(id)));
            assertEquals("CE".equals(winner)?7L:8L,projects.selectById(id).getProjectLeaderUserId());
            verify(audit,times(1)).append(any());
        } finally { workers.shutdownNow(); }
    }
    private String concurrentSave(Long id,Long leader,String market,String baseline,CyclicBarrier barrier) throws Exception {
        TenantContextHolder.setTenantId(1L); TenantContextHolder.setIgnore(false);
        try {
            barrier.await(10,TimeUnit.SECONDS);
            save(id,leader,value(market),baseline);
            return market;
        } catch(ServiceException failure) {
            if(!failure.getCode().equals(DccProjectAttributeErrors.CONFIGURATION_CHANGED.getCode()))throw failure;
            return "CONFLICT";
        } finally { TenantContextHolder.clear(); }
    }
}
