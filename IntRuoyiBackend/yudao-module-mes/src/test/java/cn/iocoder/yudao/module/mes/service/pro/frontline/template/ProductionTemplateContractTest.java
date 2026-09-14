package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductionTemplateContractTest {

    private final FrontlineTemplateService service = new FrontlineTemplateServiceImpl();

    @Test
    void shouldExposeOnlyProductionSimplifiedFields() {
        FrontlineTemplateDefinition template = service.getTemplate(FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED);

        assertEquals(List.of(
                FrontlineTemplateFieldCodes.DEVICE,
                FrontlineTemplateFieldCodes.DEVICE_PARAMETERS,
                FrontlineTemplateFieldCodes.OUTPUT_QUANTITY,
                FrontlineTemplateFieldCodes.SCRAP_QUANTITY
        ), template.fields().stream().map(FrontlineTemplateField::code).toList());
        assertFalse(template.fields().stream()
                .anyMatch(field -> "PREVIOUS_PROCESS_INPUT_QUANTITY".equals(field.code())));
        assertFalse(template.fields().stream().anyMatch(field -> field.code().toLowerCase().contains("time")));
    }
}
