package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DccDirectoryInputValidationTest {
    @Test void storageBoundariesHaveSpecificValidationMessages() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new DccDirectorySaveReqVO();
            for (var entry : Map.of("code", 64, "name", 128, "remark", 255).entrySet()) {
                var field = DccDirectorySaveReqVO.class.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(request, "字".repeat(entry.getValue()));
                assertTrue(validator.validateProperty(request, entry.getKey()).isEmpty());
                field.set(request, "字".repeat(entry.getValue() + 1));
                var violations = validator.validateProperty(request, entry.getKey());
                assertEquals(1, violations.size());
                assertTrue(violations.iterator().next().getMessage().contains("长度不能超过" + entry.getValue()));
            }
        }
    }
}
