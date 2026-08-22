package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormCenterTemplateVersionQueryTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;

    @InjectMocks
    private FormCenterRuntimeServiceImpl service;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getTemplateVersionReturnsExactCurrentTenantVersion() {
        TenantContextHolder.setTenantId(122L);
        FormTemplateVersionDO version = FormTemplateVersionDO.builder()
                .templateId(9001L)
                .tenantId(122L)
                .templateName("设备点检表")
                .versionNo("V2.0")
                .status("DRAFT")
                .recognizedSchemaJson("[]")
                .jimuSchemaJson("{\"sheetLayoutJson\":\"{}\"}")
                .build();
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(9001L, "V2.0")).thenReturn(version);

        FormCenterTemplateRespVO result = service.getTemplateVersion(9001L, "V2.0");

        assertEquals(9001L, result.getTemplateId());
        assertEquals("V2.0", result.getVersionNo());
        assertEquals("设备点检表", result.getTemplateName());
        assertEquals(version.getJimuSchemaJson(), result.getJimuSchemaJson());
    }

    @Test
    void getTemplateVersionRejectsOtherTenantVersion() {
        TenantContextHolder.setTenantId(122L);
        FormTemplateVersionDO version = FormTemplateVersionDO.builder()
                .templateId(9001L)
                .tenantId(123L)
                .templateName("其他租户表单")
                .versionNo("V1.0")
                .status("PUBLISHED")
                .recognizedSchemaJson("[]")
                .build();
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(9001L, "V1.0")).thenReturn(version);

        assertThrows(FormCenterException.class, () -> service.getTemplateVersion(9001L, "V1.0"));
    }

}
