package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateParseJsonReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateParseJsonRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormCenterRuntimeServiceImplParseJsonTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;
    @Mock
    private FormTemplateRecognizer templateRecognizer;
    @Mock
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;

    @InjectMocks
    private FormCenterRuntimeServiceImpl runtimeService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void parseProductionBatchRecordJsonUsesFormCenterRecognizerWithoutPersistingTemplateVersion() {
        TenantContextHolder.setTenantId(122L);
        String jimuSchemaJson = "{\"sheetLayoutJson\":\"{\\\"rows\\\":{\\\"0\\\":{\\\"cells\\\":{\\\"0\\\":{\\\"text\\\":\\\"批号\\\"}}}},\\\"cols\\\":{},\\\"merges\\\":[]}\","
                + "\"cellRules\":[{\"rowIndex\":1,\"columnIndex\":2,\"valueType\":\"STRING\"}]}";
        when(templateRecognizer.recognize(any())).thenReturn(FormTemplateRecognition.success(List.of(
                FormRecognizedField.required("batchNo", "批号", "input")), jimuSchemaJson));

        FormCenterTemplateParseJsonRespVO result = runtimeService.parseProductionBatchRecordJson(req("清洗工序生产记录.docx"));

        ArgumentCaptor<FormTemplateImportCommand> commandCaptor = ArgumentCaptor.forClass(FormTemplateImportCommand.class);
        verify(templateRecognizer).recognize(commandCaptor.capture());
        FormTemplateImportCommand command = commandCaptor.getValue();
        assertEquals("清洗工序生产记录", command.getTemplateName());
        assertEquals("PARSE_ONLY", command.getVersionNo());
        assertEquals("清洗工序生产记录.docx", command.getSourceFileName());
        assertArrayEquals(new byte[]{1, 2, 3}, command.getSourceBytes());
        assertEquals("生产批记录解析", command.getRemark());

        assertEquals("PRODUCTION_BATCH_RECORD", result.getParseType());
        assertEquals("生产批记录", result.getParseTypeName());
        assertEquals("清洗工序生产记录.docx", result.getSourceFileName());
        assertEquals("[{\"fieldCode\":\"batchNo\",\"label\":\"批号\",\"fieldType\":\"input\",\"required\":true}]",
                result.getRecognizedSchemaJson());
        assertEquals(jimuSchemaJson, result.getJimuSchemaJson());
        assertEquals("批号", result.getRecognizedFields().get(0).getLabel());
        assertTrue(result.getWarnings().isEmpty());
        verify(templateVersionMapper, never()).insert(any(FormTemplateVersionDO.class));
        verify(templateVersionMapper, never()).updateById(any(FormTemplateVersionDO.class));
        verify(businessApprovalOrchestrator, never()).submit(any());
    }

    @Test
    void parseProductionBatchRecordJsonParsesLegacyDocWithRealFormCenterRecognizer() throws Exception {
        TenantContextHolder.setTenantId(122L);
        Path sample = findRepoResource("按压式球囊扩充压力泵IDI-001",
                "RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc");
        assertTrue(Files.exists(sample), "pressure pump production record DOC fixture is required");
        ReflectionTestUtils.setField(runtimeService, "templateRecognizer", new DefaultWordFormTemplateRecognizer());

        FormCenterTemplateParseJsonRespVO result = runtimeService.parseProductionBatchRecordJson(
                req(sample.getFileName().toString(), Files.readAllBytes(sample)));

        assertEquals("PRODUCTION_BATCH_RECORD", result.getParseType());
        assertEquals("生产批记录", result.getParseTypeName());
        assertEquals(sample.getFileName().toString(), result.getSourceFileName());
        assertFalse(result.getRecognizedFields().isEmpty());
        assertTrue(result.getJimuSchemaJson().contains("\"sheetLayoutJson\""));
        assertTrue(result.getJimuSchemaJson().contains("\"cellRules\""));
        verify(templateVersionMapper, never()).insert(any(FormTemplateVersionDO.class));
        verify(templateVersionMapper, never()).updateById(any(FormTemplateVersionDO.class));
        verify(businessApprovalOrchestrator, never()).submit(any());
    }

    @Test
    void parseProductionBatchRecordJsonFailsWhenRecognizerDoesNotProduceVisualSchema() {
        TenantContextHolder.setTenantId(122L);
        when(templateRecognizer.recognize(any())).thenReturn(FormTemplateRecognition.success(List.of(
                FormRecognizedField.required("batchNo", "批号", "input"))));

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.parseProductionBatchRecordJson(req("清洗工序生产记录.docx")));

        assertEquals(FormCenterErrorCode.TEMPLATE_RECOGNITION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("schema rows"));
        verify(templateVersionMapper, never()).insert(any(FormTemplateVersionDO.class));
        verify(businessApprovalOrchestrator, never()).submit(any());
    }

    @Test
    void parseProductionBatchRecordJsonRejectsUnsupportedWordSourceType() {
        TenantContextHolder.setTenantId(122L);

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.parseProductionBatchRecordJson(req("清洗工序生产记录.xlsx")));

        assertEquals(FormCenterErrorCode.TEMPLATE_SOURCE_TYPE_UNSUPPORTED, exception.getErrorCode());
        verify(templateRecognizer, never()).recognize(any());
        verify(templateVersionMapper, never()).insert(any(FormTemplateVersionDO.class));
    }

    private FormCenterTemplateParseJsonReqVO req(String filename) {
        return req(filename, new byte[]{1, 2, 3});
    }

    private FormCenterTemplateParseJsonReqVO req(String filename, byte[] bytes) {
        FormCenterTemplateParseJsonReqVO reqVO = new FormCenterTemplateParseJsonReqVO();
        reqVO.setFile(new MockMultipartFile("file", filename, "application/octet-stream", bytes));
        return reqVO;
    }

    private static Path findRepoResource(String directoryName, String fileName) {
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; cursor != null && depth < 8; depth++) {
            Path candidate = cursor.resolve("resource").resolve(directoryName).resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
        }
        return Path.of("resource").resolve(directoryName).resolve(fileName);
    }

}
