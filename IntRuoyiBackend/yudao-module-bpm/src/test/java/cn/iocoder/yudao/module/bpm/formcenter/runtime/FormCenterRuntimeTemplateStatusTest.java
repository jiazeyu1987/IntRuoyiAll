package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormCenterRuntimeTemplateStatusTest extends BaseMockitoUnitTest {

    @Mock
    private FormTemplateVersionMapper templateVersionMapper;

    @InjectMocks
    private FormCenterRuntimeServiceImpl runtimeService;

    @Test
    void enableTemplateRestoresDisabledVersionToPublished() {
        FormTemplateVersionDO version = version(FormTemplateStatus.DISABLED);
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(200L, "V1.0")).thenReturn(version);

        runtimeService.enableTemplate(200L, "V1.0");

        assertEquals(FormTemplateStatus.PUBLISHED.name(), version.getStatus());
        verify(templateVersionMapper).updateById(version);
    }

    @Test
    void enableTemplateRejectsNonDisabledVersion() {
        FormTemplateVersionDO version = version(FormTemplateStatus.PUBLISHED);
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(200L, "V1.0")).thenReturn(version);

        FormCenterException exception = assertThrows(FormCenterException.class,
                () -> runtimeService.enableTemplate(200L, "V1.0"));

        assertEquals(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE, exception.getErrorCode());
    }

    private FormTemplateVersionDO version(FormTemplateStatus status) {
        return FormTemplateVersionDO.builder()
                .templateId(200L)
                .tenantId(122L)
                .templateName("损耗单")
                .versionNo("V1.0")
                .status(status.name())
                .build();
    }

}
