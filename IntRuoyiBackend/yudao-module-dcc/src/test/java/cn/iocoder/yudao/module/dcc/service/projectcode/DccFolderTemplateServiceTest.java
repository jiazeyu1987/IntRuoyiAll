package cn.iocoder.yudao.module.dcc.service.projectcode;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Import({DccFolderTemplateService.class, cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService.class})
class DccFolderTemplateServiceTest extends BaseDbUnitTest {
    @Resource private DccFolderTemplateService service;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean private DccFolderTemplateMapper templates;
    @Resource private DccProjectCodeMapper projects;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean private DccFolderTemplateHistoryMapper history;
    @MockitoBean private PermissionApi permissions;
    @MockitoBean private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccess;
    @MockitoBean private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService audit;
    @Test void unifiedAuditFailureRollsBackTemplateAndChangeHistory() {
        doThrow(new IllegalStateException("统一审计写入失败")).when(audit).append(any());
        assertThrows(RuntimeException.class, () -> service.save(7L, new DccFolderTemplateService.Save(null, "审计原子性", null, true, structure("文件"), "测试操作")));
        assertTrue(templates.selectList().isEmpty());
        assertTrue(history.selectList().isEmpty());
    }
    @BeforeEach void permissions() { when(permissions.hasAnyPermissions(7L, "dcc:project-code:update")).thenReturn(true); }
    @Test void clearingDescriptionPersistsNullAndAuditMatchesSavedRow() {
        Long id = service.save(7L, new DccFolderTemplateService.Save(null, "清空说明", "原说明", true, structure("文件"), "创建模板"));
        service.save(7L, new DccFolderTemplateService.Save(id, "清空说明", null, true, structure("文件"), "清空旧说明"));
        var persisted = templates.selectById(id);
        assertNull(persisted.getDescription(), "必须从数据库真正清除旧说明，不能仅响应/审计显示为空");
        var commands = org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(audit, times(2)).append(commands.capture());
        var after = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                commands.getAllValues().get(1).getAfterState().getCanonicalJson(), DccFolderTemplateDO.class);
        assertEquals(persisted.getDescription(), after.getDescription());
        assertEquals(persisted.getStructureJson(), after.getStructureJson());
    }
    @Test void zeroChangeHistoryInsertCannotSaveTemplateOrAppendSuccessAudit() {
        Long id=service.save(7L,new DccFolderTemplateService.Save(null,"原模板",null,true,structure("旧目录"),"建立模板"));
        clearInvocations(audit);
        doReturn(0).when(history).insert(any(DccFolderTemplateHistoryDO.class));
        var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.save(7L,
                new DccFolderTemplateService.Save(id,"未保存新名称",null,true,structure("新目录"),"修改目录模板")));
        assertEquals(cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE.getCode(),failure.getCode());
        assertEquals("原模板",templates.selectById(id).getName());
        assertTrue(templates.selectById(id).getStructureJson().contains("旧目录"));
        assertEquals(1,history.selectList().size());verifyNoInteractions(audit);
    }
    @Test void zeroTemplateDeleteCannotReportDeletionOrRecordDeletedHistory() {
        Long id=service.save(7L,new DccFolderTemplateService.Save(null,"保留模板",null,true,structure("正式目录"),"建立模板"));
        clearInvocations(audit);
        doReturn(0).when(templates).deleteById(id);
        var failure=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.delete(7L,id,"删除未使用模板"));
        assertEquals(cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE.getCode(),failure.getCode());
        assertNotNull(templates.selectById(id));assertEquals(1,history.selectList().size());verifyNoInteractions(audit);
    }
    private DccFolderTemplateStructure structure(String leaf) {
        return new DccFolderTemplateStructure(List.of(
                new DccFolderTemplateStructure.Node("child", "root", leaf, 1),
                new DccFolderTemplateStructure.Node("root", null, "根目录", 0)));
    }
    private Long project(String code) {
        var row = DccProjectCodeDO.builder().projectName(code).projectCode(code).status("ENABLE").build();
        row.setTenantId(1L); projects.insert(row); return row.getId();
    }
    @Test void permissionOnlySaveHasAuditableChangesNoApproval() {
        assertThrows(RuntimeException.class, () -> service.save(8L, new DccFolderTemplateService.Save(null, "模板", null, true, structure("旧"), "测试新增")));
        assertTrue(templates.selectList().isEmpty());
        Long id = service.save(7L, new DccFolderTemplateService.Save(null, "模板", "说明", true, structure("旧"), "测试新增"));
        service.save(7L, new DccFolderTemplateService.Save(id, "模板", "新说明", true, structure("新"), "测试修改"));
        assertEquals(7L, templates.selectById(id).getEditedByUserId());
        var changes = history.selectList();
        assertEquals(2, changes.size());
        assertTrue(changes.get(1).getBeforeJson().contains("旧"));
        assertTrue(changes.get(1).getAfterJson().contains("新"));
    }
    @Test void generatedFoldersAreIndependentAndSnapshotIsFrozenAtRequest() {
        Long id = service.save(7L, new DccFolderTemplateService.Save(null, "模板", null, true, structure("旧"), "测试新增"));
        String snapshot = service.captureForRequest(id);
        service.save(7L, new DccFolderTemplateService.Save(id, "模板", null, true, structure("新"), "测试修改"));
        Long first = project("P1");
        var rows = service.generate(first, id, snapshot);
        assertEquals(2, rows.size());
        assertEquals(rows.get(0).getId(), rows.get(1).getParentId());
        assertEquals("旧", rows.get(1).getName());
        Long second = project("P2");
        var secondRows = service.generate(second, id, service.captureForRequest(id));
        assertEquals("新", secondRows.get(1).getName());
        assertNotEquals(rows.get(0).getId(), secondRows.get(0).getId());
        assertEquals("旧", service.listProjectFolders(first).get(1).getName());
        assertThrows(RuntimeException.class, () -> service.requireProjectFolder(second, rows.get(1).getId()));
        assertThrows(RuntimeException.class, () -> service.generate(first, id, snapshot));
    }
    @Test void usedTemplateCannotDeleteButCanDisableWithoutTouchingFolders() {
        Long id = service.save(7L, new DccFolderTemplateService.Save(null, "模板", null, true, structure("文件"), "测试操作"));
        String snapshot = service.captureForRequest(id);
        Long project = project("P");
        service.generate(project, id, snapshot);
        assertThrows(RuntimeException.class, () -> service.delete(7L, id, "删除模板"));
        service.save(7L, new DccFolderTemplateService.Save(id, "模板", null, false, structure("文件"), "测试操作"));
        assertThrows(RuntimeException.class, () -> service.captureForRequest(id));
        assertEquals(2, service.listProjectFolders(project).size());
        Long unused = service.save(7L, new DccFolderTemplateService.Save(null, "未用", null, true, structure("文件"), "测试操作"));
        service.delete(7L, unused, "删除未使用模板");
        assertNull(templates.selectById(unused));
    }
    @Test void cycleMissingParentDuplicateSiblingAndEmptyStructureReject() {
        for (var invalid : List.of(
                new DccFolderTemplateStructure(List.of()),
                new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("x", "x", "循环", 0))),
                new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("x", "missing", "缺父", 0))),
                new DccFolderTemplateStructure(List.of(new DccFolderTemplateStructure.Node("a", null, "重复", 0),
                        new DccFolderTemplateStructure.Node("b", null, "重复", 1))))) {
            assertThrows(RuntimeException.class, () -> service.save(7L, new DccFolderTemplateService.Save(null, "错误", null, true, invalid, "非法结构校验")));
        }
        assertTrue(templates.selectList().isEmpty());
    }
}
