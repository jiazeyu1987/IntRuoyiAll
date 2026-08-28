package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormTemplateFillRuleAutoDetectRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormTemplateFillRuleCandidateVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormTemplateFillRuleAutoDetectServiceTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;

    @InjectMocks
    private FormTemplateFillRuleAutoDetectService service;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void detect_createsDraftFromPublishedVersionAndExtractsCandidates() {
        TenantContextHolder.setTenantId(122L);
        FormTemplateVersionDO sourceVersion = templateVersion(101L, 122L, "生产报工模板", "V1.0",
                FormTemplateStatus.PUBLISHED, templateSchema());
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(200L, "V1.0")).thenReturn(sourceVersion);
        when(templateVersionMapper.selectDraftByTemplateId(122L, 200L)).thenReturn(null);
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(200L, "V2.0")).thenReturn(null);
        when(templateVersionMapper.insert((FormTemplateVersionDO) any())).thenAnswer(invocation -> {
            FormTemplateVersionDO insert = invocation.getArgument(0);
            insert.setId(301L);
            return 1;
        });

        FormTemplateFillRuleAutoDetectRespVO response = service.detect(200L, "V1.0");

        assertEquals(200L, response.getTemplateId());
        assertEquals("生产报工模板", response.getTemplateName());
        assertEquals("V1.0", response.getSourceVersionNo());
        assertEquals("V2.0", response.getVersionNo());
        assertEquals(FormTemplateStatus.DRAFT.name(), response.getTargetStatus());
        assertTrue(response.getDraftCreated());
        assertEquals(2, response.getCandidateCount());
        assertEquals(2, response.getCandidates().size());

        FormTemplateFillRuleCandidateVO first = response.getCandidates().get(0);
        assertEquals(0, first.getRowIndex());
        assertEquals(1, first.getColumnIndex());
        assertEquals("生产批号", first.getLabel());
        assertEquals("STRING", first.getValueType());
        assertEquals("input-text", first.getComponentFlag());
        assertEquals("请输入生产批号", first.getPlaceholder());
        assertNotNull(first.getReason());
        assertTrue(first.getReason().contains("生产批号"));

        FormTemplateFillRuleCandidateVO second = response.getCandidates().get(1);
        assertEquals(0, second.getRowIndex());
        assertEquals(3, second.getColumnIndex());
        assertEquals("操作日期", second.getLabel());
        assertEquals("DATE", second.getValueType());
        assertEquals("date", second.getComponentFlag());
        assertEquals("请选择操作日期", second.getPlaceholder());

        ArgumentCaptor<FormTemplateVersionDO> captor = ArgumentCaptor.forClass(FormTemplateVersionDO.class);
        verify(templateVersionMapper).insert(captor.capture());
        FormTemplateVersionDO inserted = captor.getValue();
        assertEquals(200L, inserted.getTemplateId());
        assertEquals(122L, inserted.getTenantId());
        assertEquals("生产报工模板", inserted.getTemplateName());
        assertEquals("V2.0", inserted.getVersionNo());
        assertEquals(FormTemplateStatus.DRAFT.name(), inserted.getStatus());
        assertEquals(templateSchema(), inserted.getJimuSchemaJson());
        verify(templateVersionMapper).selectDraftByTemplateId(122L, 200L);
    }

    @Test
    void detect_reusesExistingDraftWithoutCreatingNewVersion() {
        TenantContextHolder.setTenantId(122L);
        FormTemplateVersionDO sourceVersion = templateVersion(101L, 122L, "生产报工模板", "V1.0",
                FormTemplateStatus.PUBLISHED, templateSchema());
        FormTemplateVersionDO existingDraft = templateVersion(102L, 122L, "生产报工模板", "V1.1",
                FormTemplateStatus.DRAFT, templateSchema());
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(200L, "V1.0")).thenReturn(sourceVersion);
        when(templateVersionMapper.selectDraftByTemplateId(122L, 200L)).thenReturn(existingDraft);

        FormTemplateFillRuleAutoDetectRespVO response = service.detect(200L, "V1.0");

        assertFalse(response.getDraftCreated());
        assertEquals("V1.1", response.getVersionNo());
        assertEquals(2, response.getCandidateCount());
        verify(templateVersionMapper, never()).insert((FormTemplateVersionDO) any());
    }

    @Test
    void detect_keepsSourceDraftWithoutCreatingNewVersion() {
        TenantContextHolder.setTenantId(122L);
        FormTemplateVersionDO sourceVersion = templateVersion(103L, 122L, "生产报工模板", "V2.0",
                FormTemplateStatus.DRAFT, templateSchema());
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(200L, "V2.0")).thenReturn(sourceVersion);

        FormTemplateFillRuleAutoDetectRespVO response = service.detect(200L, "V2.0");

        assertFalse(response.getDraftCreated());
        assertEquals("V2.0", response.getVersionNo());
        assertEquals(2, response.getCandidateCount());
        verify(templateVersionMapper, never()).selectDraftByTemplateId(122L, 200L);
        verify(templateVersionMapper, never()).insert((FormTemplateVersionDO) any());
    }

    @Test
    void detectReadsImportedSheetLayoutJsonAndExistingCellRules() throws Exception {
        Path fixture = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001", "过程检验记录.docx")
                .toAbsolutePath().normalize();
        String jimuSchemaJson;
        try (InputStream input = Files.newInputStream(fixture);
             XWPFDocument document = new XWPFDocument(input)) {
            jimuSchemaJson = WordTableVisualSchemaBuilder.build(document.getTables().get(0));
        }

        TenantContextHolder.setTenantId(1L);
        FormTemplateVersionDO version = FormTemplateVersionDO.builder()
                .id(8001L)
                .templateId(33L)
                .tenantId(1L)
                .templateName("按压式压力泵过程检验记录")
                .versionNo("V8.0")
                .status(FormTemplateStatus.DRAFT.name())
                .jimuSchemaJson(jimuSchemaJson)
                .build();
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(33L, "V8.0")).thenReturn(version);

        FormTemplateFillRuleAutoDetectRespVO response = service.detect(33L, "V8.0");
        List<FormTemplateFillRuleCandidateVO> candidates = response.getCandidates();

        assertTrue(candidates.stream().anyMatch(candidate -> "NUMBER".equals(candidate.getValueType())
                && "input-number".equals(candidate.getComponentFlag())));
        assertTrue(candidates.stream().anyMatch(candidate -> "DATE".equals(candidate.getValueType())
                && "date".equals(candidate.getComponentFlag())));
        assertTrue(candidates.stream().anyMatch(candidate -> "STRING".equals(candidate.getValueType())
                && "radio-group".equals(candidate.getComponentFlag())
                && JsonUtils.toJsonString(candidate.getConstraints()).contains("符合要求")
                && JsonUtils.toJsonString(candidate.getConstraints()).contains("不符合要求")));
        assertTrue(candidates.stream().anyMatch(candidate -> "SIGNATURE".equals(candidate.getValueType())
                && "signature".equals(candidate.getComponentFlag())));
    }

    private FormTemplateVersionDO templateVersion(Long id, Long tenantId, String templateName, String versionNo,
            FormTemplateStatus status, String jimuSchemaJson) {
        return FormTemplateVersionDO.builder()
                .id(id)
                .templateId(200L)
                .tenantId(tenantId)
                .templateName(templateName)
                .versionNo(versionNo)
                .status(status.name())
                .sourceFileName("生产报工模板.docx")
                .sourceFileContent("source-content")
                .recognizedSchemaJson("{}")
                .jimuSchemaJson(jimuSchemaJson)
                .remark("识别备注")
                .build();
    }

    private String templateSchema() {
        return """
                {
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"生产批号"},
                        "1":{"text":"","fillForm":{"field":"tpl_r0_c1","component":"Input","componentFlag":"input-text","placeholder":"请输入生产批号"}},
                        "2":{"text":"操作日期"},
                        "3":{"text":"","fillForm":{"field":"tpl_r0_c3","component":"Input","componentFlag":"input-text"}}
                      }
                    }
                  }
                }
                """;
    }

}
