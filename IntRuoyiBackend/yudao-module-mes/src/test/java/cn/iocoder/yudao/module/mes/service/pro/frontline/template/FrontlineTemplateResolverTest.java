package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_BINDING_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrontlineTemplateResolverTest {

    private final FrontlineTemplateService service = new FrontlineTemplateServiceImpl();

    @Test
    void shouldResolveTemplateByActualEmployeeAndCurrentProcess() {
        FrontlineTemplateDefinition template = service.resolveTemplate(
                new FrontlineTemplateResolveCommand(1001L, 2001L, 3001L, FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED));

        assertEquals(FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED, template.code());
        assertEquals(FrontlineTemplateTypes.PRODUCTION, template.type());
    }

    @Test
    void shouldRejectMissingTemplateBindingWithoutDefaultFallback() {
        ServiceException exception = assertThrows(ServiceException.class, () -> service.resolveTemplate(
                new FrontlineTemplateResolveCommand(1001L, 2001L, 3001L, null)));

        assertEquals(PRO_FRONTLINE_TEMPLATE_BINDING_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectUnsupportedTemplateCode() {
        ServiceException exception = assertThrows(ServiceException.class, () -> service.resolveTemplate(
                new FrontlineTemplateResolveCommand(1001L, 2001L, 3001L, "FREE_FORM_BATCH_RECORD")));

        assertEquals(PRO_FRONTLINE_TEMPLATE_UNSUPPORTED.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectMissingActualEmployeeOrProcessContext() {
        ServiceException missingEmployee = assertThrows(ServiceException.class, () -> service.resolveTemplate(
                new FrontlineTemplateResolveCommand(null, 2001L, 3001L, FrontlineTemplateCodes.PQC_SIMPLIFIED)));
        ServiceException missingProcess = assertThrows(ServiceException.class, () -> service.resolveTemplate(
                new FrontlineTemplateResolveCommand(1001L, null, null, FrontlineTemplateCodes.PQC_SIMPLIFIED)));

        assertEquals(PRO_FRONTLINE_TEMPLATE_CONTEXT_REQUIRED.getCode(), missingEmployee.getCode());
        assertEquals(PRO_FRONTLINE_TEMPLATE_CONTEXT_REQUIRED.getCode(), missingProcess.getCode());
    }
}
