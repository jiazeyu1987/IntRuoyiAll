package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateItemSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateSaveReqVO;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccProjectFileTemplateRequestValidationTest {

    @Test
    void nestedRequestAllowsMissingClassificationAndRetainsNameConstraints() {
        // Validate the request envelope used by @Valid on the save endpoint.
        // Calling the controller or service directly does not exercise this boundary.
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var item = new DccProjectFileTemplateItemSaveReqVO();
            item.setSortOrder(1);
            var request = new DccProjectFileTemplateSaveReqVO();
            request.setItems(List.of(item));

            item.setFileName("模拟文件名称必填验证20261117");
            assertTrue(validator.validate(request).isEmpty());
            item.setFileName("文".repeat(255));
            assertTrue(validator.validate(request).isEmpty());

            for (String invalidName : new String[]{null, "", "   ", "文".repeat(256)}) {
                item.setFileName(invalidName);
                var violations = validator.validate(request);
                assertEquals(1, violations.size());
                assertEquals("items[0].fileName", violations.iterator().next().getPropertyPath().toString());
            }
        }
    }
}
