package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_FIELD_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_SUBMIT_TIME_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrontlineTemplatePayloadContractTest {

    private final FrontlineTemplateService service = new FrontlineTemplateServiceImpl();

    @Test
    void shouldKeepRawOutOfLimitValuesWithoutTemplateStageClamping() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(FrontlineTemplateFieldCodes.DEVICE, "EQ-01");
        values.put(FrontlineTemplateFieldCodes.DEVICE_PARAMETERS, Map.of("temperature", new BigDecimal("50")));
        values.put(FrontlineTemplateFieldCodes.OUTPUT_QUANTITY, new BigDecimal("12"));
        values.put(FrontlineTemplateFieldCodes.SCRAP_QUANTITY, new BigDecimal("0.5"));

        FrontlineTemplatePayload payload = service.buildPayload(
                new FrontlineTemplatePayloadCommand(
                        10L, 20L, 30L, 40L, 50L, FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED, values));

        assertEquals(new BigDecimal("50"),
                ((Map<?, ?>) payload.fieldValues().get(FrontlineTemplateFieldCodes.DEVICE_PARAMETERS)).get("temperature"));
        assertEquals(10L, payload.workOrderId());
        assertEquals(30L, payload.processId());
        assertEquals(40L, payload.routeProcessId());
        assertEquals(50L, payload.actualEmployeeId());
    }

    @Test
    void shouldRejectEditableSubmitTimeAndUnknownFields() {
        ServiceException submitTime = assertThrows(ServiceException.class, () -> service.buildPayload(
                new FrontlineTemplatePayloadCommand(
                        10L, 20L, 30L, 40L, 50L, FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED,
                        Map.of(
                                FrontlineTemplateFieldCodes.DEVICE, "EQ-01",
                                FrontlineTemplateFieldCodes.DEVICE_PARAMETERS, Map.of("temperature", new BigDecimal("50")),
                                FrontlineTemplateFieldCodes.OUTPUT_QUANTITY, BigDecimal.ONE,
                                FrontlineTemplateFieldCodes.SCRAP_QUANTITY, BigDecimal.ZERO,
                                "submitTime", "2026-07-30T01:00:00"))));

        ServiceException unknownField = assertThrows(ServiceException.class, () -> service.buildPayload(
                new FrontlineTemplatePayloadCommand(
                        10L, 20L, 30L, 40L, 50L, FrontlineTemplateCodes.PQC_SIMPLIFIED,
                        Map.of(FrontlineTemplateFieldCodes.PQC_RESULT, FrontlinePqcResults.DETECTION_SUCCESS,
                                "batchRecordFreeText", "not allowed"))));

        assertEquals(PRO_FRONTLINE_TEMPLATE_SUBMIT_TIME_FORBIDDEN.getCode(), submitTime.getCode());
        assertEquals(PRO_FRONTLINE_TEMPLATE_FIELD_INVALID.getCode(), unknownField.getCode());
    }

    @Test
    void shouldRejectPreviousProcessInputQuantityAsUnknownProductionField() {
        ServiceException exception = assertThrows(ServiceException.class, () -> service.buildPayload(
                new FrontlineTemplatePayloadCommand(
                        10L, 20L, 30L, 40L, 50L, FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED,
                        Map.of(
                                "PREVIOUS_PROCESS_INPUT_QUANTITY", BigDecimal.ONE,
                                FrontlineTemplateFieldCodes.DEVICE, "EQ-01",
                                FrontlineTemplateFieldCodes.DEVICE_PARAMETERS, Map.of("temperature", new BigDecimal("50")),
                                FrontlineTemplateFieldCodes.OUTPUT_QUANTITY, BigDecimal.ONE,
                                FrontlineTemplateFieldCodes.SCRAP_QUANTITY, BigDecimal.ZERO))));

        assertEquals(PRO_FRONTLINE_TEMPLATE_FIELD_INVALID.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectPayloadWithoutProcessPoolContext() {
        ServiceException exception = assertThrows(ServiceException.class, () -> service.buildPayload(
                new FrontlineTemplatePayloadCommand(
                        10L, 20L, 30L, null, 50L, FrontlineTemplateCodes.PQC_SIMPLIFIED,
                        Map.of(FrontlineTemplateFieldCodes.PQC_RESULT, FrontlinePqcResults.DETECTION_SUCCESS))));

        assertEquals(PRO_FRONTLINE_TEMPLATE_CONTEXT_REQUIRED.getCode(), exception.getCode());
    }
}
