package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

@Import(DccProjectAttributesService.class)
class DccProjectAttributesServiceTest extends BaseDbUnitTest {
    @Resource private DccProjectAttributesService service;
    @Resource private DccProjectCodeMapper projectMapper;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private DccProjectApplicationAttributesMapper attributesMapper;
    @MockitoBean private DccProjectAccessService access;

    private DccProjectAttributes defaults() {
        return new DccProjectAttributes(List.of("NMPA"), null, "Y", "N", "N", null);
    }
    private DccProjectCodeDO project() {
        var project = DccProjectCodeDO.builder().projectCode("B").projectName("属性项目")
                .status("ENABLE").defaultAttributesJson(service.encode(defaults())).build();
        project.setTenantId(1L); projectMapper.insert(project); return project;
    }
    @Test void uploadUsesDefaultsAndSavesManualActual() { threePaths("UPLOAD"); }
    @Test void revisionUsesProjectDefaultsInsteadOfPreviousVersion() { threePaths("REVISION"); }
    @Test void obsoleteHasIndependentApplicationSnapshotAndNoFileWrite() { threePaths("OBSOLETE"); }
    private void threePaths(String type) {
        var project = project();
        assertEquals(defaults(), service.initialize(7L, project.getId(), type));
        var changed = new DccProjectAttributes(List.of("CE", "MADSAP"), null, "N", "Y", "Y", "甲方");
        service.saveDraft(7L, project.getId(), type, 21L, 1, changed);
        var saved = service.readSaved(7L, project.getId(), type, 21L, 1);
        assertEquals(changed, service.readValue(saved.getActualAttributesJson()));
        assertEquals(defaults(), service.readValue(saved.getDefaultSourceJson()));
        assertEquals(defaults(), service.defaults(projectMapper.selectById(project.getId())));
        service.freeze(7L, project.getId(), type, 21L, 1);
        service.freeze(7L, project.getId(), type, 21L, 1);
        assertTrue(attributesMapper.find(type, 21L, 1).getSubmitted());
        assertThrows(RuntimeException.class, () -> service.saveDraft(7L, project.getId(), type, 21L, 1, defaults()));
    }
    @Test void projectChangeCannotOverwriteSavedDraftOrSubmittedHistory() {
        var project = project();
        var draft = service.saveDraft(7L, project.getId(), "UPLOAD", 31L, 1, defaults());
        service.saveDraft(7L, project.getId(), "OBSOLETE", 32L, 1, defaults());
        service.freeze(7L, project.getId(), "OBSOLETE", 32L, 1);
        project.setDefaultAttributesJson(service.encode(new DccProjectAttributes(List.of("FDA"), null, "NA", "Y", "N", null)));
        projectMapper.updateById(project);
        assertEquals(draft.getActualAttributesJson(), service.readSaved(7L, project.getId(), "UPLOAD", 31L, 1).getActualAttributesJson());
        assertEquals(defaults(), service.readValue(service.readSaved(7L, project.getId(), "OBSOLETE", 32L, 1).getActualAttributesJson()));
        assertEquals(List.of("FDA"), service.initialize(7L, project.getId(), "REVISION").targetMarkets());
        // 第二次草稿保存只更新实际值，来源不随项目变
        service.saveDraft(7L, project.getId(), "UPLOAD", 31L, 1, defaults());
        assertEquals(defaults(), service.readValue(attributesMapper.find("UPLOAD", 31L, 1).getDefaultSourceJson()));
    }
    @Test void wrongProjectActionRoundAndDeniedPermissionFailWithoutWrite() {
        var project = project();
        service.saveDraft(7L, project.getId(), "UPLOAD", 40L, 1, defaults());
        assertThrows(RuntimeException.class, () -> service.readSaved(7L, 999L, "UPLOAD", 40L, 1));
        assertThrows(RuntimeException.class, () -> service.saveDraft(7L, project.getId(), "FAKE", 40L, 1, defaults()));
        assertThrows(RuntimeException.class, () -> service.freeze(7L, project.getId(), "UPLOAD", 40L, 0));
        doThrow(fail(LEADER_REQUIRED)).when(access).assertProjectEditorOrOwner(8L, project.getId());
        assertThrows(RuntimeException.class, () -> service.saveDraft(8L, project.getId(), "UPLOAD", 41L, 1, defaults()));
        assertNull(attributesMapper.find("UPLOAD", 41L, 1));
    }
    @Test void missingDefaultsCannotBecomeNoOrNotApplicable() {
        var project = DccProjectCodeDO.builder().projectName("历史项目").projectCode("OLD").status("ENABLE").build();
        project.setTenantId(1L); projectMapper.insert(project);
        assertThrows(RuntimeException.class, () -> service.initialize(7L, project.getId(), "UPLOAD"));
        assertThrows(RuntimeException.class, () -> service.saveDraft(7L, project.getId(), "UPLOAD", 50L, 1, defaults()));
        assertNull(attributesMapper.find("UPLOAD", 50L, 1));
    }
    @Test void conditionalFieldsAndIndependentIdentitiesAreStrict() {
        assertDoesNotThrow(() -> service.encode(new DccProjectAttributes(List.of("NMPA", "OTHER"), "外部市场", "Y", "Y", "Y", "目标")));
        for (var value : List.of(
                new DccProjectAttributes(List.of(), null, "Y", "N", "N", null),
                new DccProjectAttributes(List.of("NA", "FDA"), null, "Y", "N", "N", null),
                new DccProjectAttributes(List.of("OTHER"), null, "Y", "N", "N", null),
                new DccProjectAttributes(List.of("FDA"), null, "Y", "N", "Y", null),
                new DccProjectAttributes(List.of("FDA"), null, "Y", "N", "N", "隐藏目标"),
                new DccProjectAttributes(List.of("FDA"), null, null, "N", "N", null),
                new DccProjectAttributes(List.of("FDA"), null, "Y", null, "N", null),
                new DccProjectAttributes(List.of("FDA"), null, "Y", "N", "NA", null))) {
            assertThrows(RuntimeException.class, () -> service.encode(value));
        }
    }
    @Test void initializationSourceFreezesImmediatelyAndReworkKeepsPreviousValues() {
        var project = project();
        var draft = service.beginDraft(7L, project.getId(), "REVISION", 71L, 1);
        project.setDefaultAttributesJson(service.encode(new DccProjectAttributes(List.of("FDA"), null, "N", "Y", "Y", "新目标")));
        projectMapper.updateById(project);
        assertEquals(defaults(), service.readValue(service.beginDraft(7L, project.getId(), "REVISION", 71L, 1).getDefaultSourceJson()));
        var actual = new DccProjectAttributes(List.of("CE"), null, "Y", "Y", "N", null);
        service.saveDraft(7L, project.getId(), "REVISION", 71L, 1, actual);
        service.freeze(7L, project.getId(), "REVISION", 71L, 1);
        var rework = service.forkForRework(7L, project.getId(), "REVISION", 71L, 1);
        assertEquals(2, rework.getApplicationRound());
        assertEquals(draft.getDefaultSourceJson(), rework.getDefaultSourceJson());
        assertEquals(actual, service.readValue(rework.getActualAttributesJson()));
        assertFalse(rework.getSubmitted());
        assertEquals(rework.getId(), service.forkForRework(7L, project.getId(), "REVISION", 71L, 1).getId());
    }
    @Test void explicitRestoreUpdatesOnlyConfirmedUnsubmittedDraftAndRejectsConcurrentDefaults() throws Exception {
        var project = project();
        service.beginDraft(7L, project.getId(), "UPLOAD", 91L, 1);
        service.beginDraft(7L, project.getId(), "OBSOLETE", 92L, 1);
        service.freeze(7L, project.getId(), "OBSOLETE", 92L, 1);
        var confirmed = new DccProjectAttributes(List.of("FDA"), null, "N", "Y", "Y", "新目标");
        project.setDefaultAttributesJson(service.encode(confirmed)); projectMapper.updateById(project);
        service.restoreDraftDefaults(7L, project.getId(), "UPLOAD", 91L, 1, confirmed);
        var restored = attributesMapper.find("UPLOAD", 91L, 1);
        assertEquals(confirmed, service.readValue(restored.getDefaultSourceJson()));
        assertEquals(confirmed, service.readValue(restored.getActualAttributesJson()));
        assertEquals(defaults(), service.readValue(attributesMapper.find("OBSOLETE", 92L, 1).getDefaultSourceJson()));
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> service.restoreDraftDefaults(7L, project.getId(), "OBSOLETE", 92L, 1, confirmed), SNAPSHOT_FROZEN);
        project.setDefaultAttributesJson(service.encode(defaults())); projectMapper.updateById(project);
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> service.restoreDraftDefaults(7L, project.getId(), "UPLOAD", 91L, 1, confirmed), DEFAULTS_CHANGED);
        assertEquals(confirmed, service.readValue(attributesMapper.find("UPLOAD", 91L, 1).getDefaultSourceJson()));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"UPLOAD", "REVISION", "OBSOLETE"})
    void zeroRowFreezeCannotReportPersistedSubmittedSnapshot(String type) {
        var project = project();
        service.beginDraft(7L, project.getId(), type, 103L, 1);
        doReturn(0).when(attributesMapper).updateById(any(DccProjectApplicationAttributesDO.class));
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> service.freeze(7L, project.getId(), type, 103L, 1), SNAPSHOT_INVALID);
        assertFalse(attributesMapper.find(type, 103L, 1).getSubmitted());
    }
}
