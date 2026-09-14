package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FrontlineTemplateCatalogTest {

    private final FrontlineTemplateService service = new FrontlineTemplateServiceImpl();

    @Test
    void shouldExposeOnlyProductionAndPqcSimplifiedTemplates() {
        List<FrontlineTemplateDefinition> templates = service.listCatalog();

        assertEquals(2, templates.size());
        assertEquals(
                Set.of(FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED, FrontlineTemplateCodes.PQC_SIMPLIFIED),
                templates.stream().map(FrontlineTemplateDefinition::code).collect(Collectors.toSet()));
        assertEquals("生产简化模板", service.getTemplate(FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED).name());
        assertEquals("PQC 简化模板", service.getTemplate(FrontlineTemplateCodes.PQC_SIMPLIFIED).name());
        assertFalse(service.getTemplate(FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED).editableSubmitTime());
        assertFalse(service.getTemplate(FrontlineTemplateCodes.PQC_SIMPLIFIED).editableSubmitTime());
    }
}
