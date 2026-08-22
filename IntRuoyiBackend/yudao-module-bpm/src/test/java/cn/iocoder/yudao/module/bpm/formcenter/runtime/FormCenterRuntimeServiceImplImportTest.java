package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateImportReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormCenterRuntimeServiceImplImportTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;
    @Mock
    private FormTemplateRecognizer templateRecognizer;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void importDocPersistsRecognizerVisualSchemaOnNewTemplateVersion() {
        TenantContextHolder.setTenantId(122L);
        FormCenterRuntimeServiceImpl service = new FormCenterRuntimeServiceImpl();
        ReflectionTestUtils.setField(service, "templateVersionMapper", templateVersionMapper);
        ReflectionTestUtils.setField(service, "templateRecognizer", templateRecognizer);
        when(templateVersionMapper.selectLatestByTemplateName(122L, "按压式压力泵过程检验记录"))
                .thenReturn(null);
        String visualSchema = "{\"sheetLayoutJson\":\"{\\\"rows\\\":{}}\",\"cellRules\":[]}";
        when(templateRecognizer.recognize(any())).thenReturn(FormTemplateRecognition.success(List.of(
                FormRecognizedField.of("field1", "气密性检测工装：", "input", false)), visualSchema));
        when(templateVersionMapper.insert(any(FormTemplateVersionDO.class))).thenAnswer(invocation -> {
            FormTemplateVersionDO inserted = invocation.getArgument(0);
            inserted.setId(81L);
            return 1;
        });
        FormCenterTemplateImportReqVO reqVO = new FormCenterTemplateImportReqVO();
        reqVO.setTemplateName("按压式压力泵过程检验记录");
        reqVO.setFile(new MockMultipartFile("file", "过程检验记录.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[] {1, 2, 3}));

        service.importDoc(reqVO, 100L);

        ArgumentCaptor<FormTemplateVersionDO> captor = ArgumentCaptor.forClass(FormTemplateVersionDO.class);
        verify(templateVersionMapper).insert(captor.capture());
        assertEquals(visualSchema, captor.getValue().getJimuSchemaJson());
    }

}
