package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_PQC_RESULT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PqcSimpleTemplateContractTest {

    private final FrontlineTemplateService service = new FrontlineTemplateServiceImpl();

    @Test
    void shouldAllowOnlyPqcSuccessOrFailure() {
        FrontlineTemplateDefinition template = service.getTemplate(FrontlineTemplateCodes.PQC_SIMPLIFIED);

        assertEquals(List.of(FrontlineTemplateFieldCodes.PQC_RESULT),
                template.fields().stream().map(FrontlineTemplateField::code).toList());
        assertEquals(List.of(FrontlinePqcResults.DETECTION_SUCCESS, FrontlinePqcResults.DETECTION_FAILED),
                template.fields().get(0).options());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.buildPayload(
                new FrontlineTemplatePayloadCommand(
                        10L, 20L, 30L, 40L, 50L, FrontlineTemplateCodes.PQC_SIMPLIFIED,
                        Map.of(FrontlineTemplateFieldCodes.PQC_RESULT, "RECHECK_REQUIRED"))));
        assertEquals(PRO_FRONTLINE_TEMPLATE_PQC_RESULT_INVALID.getCode(), exception.getCode());
    }
}
