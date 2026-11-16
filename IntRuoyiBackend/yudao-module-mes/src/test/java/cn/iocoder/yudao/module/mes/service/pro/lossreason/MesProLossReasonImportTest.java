package cn.iocoder.yudao.module.mes.service.pro.lossreason;

import cn.iocoder.yudao.module.mes.controller.admin.pro.lossreason.MesProLossReasonController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import static org.mockito.Mockito.*;

class MesProLossReasonImportTest {
    @Test void sparseWorksheetPreservesBlankPhysicalRowsForAtomicValidation() throws Exception {
        var service = mock(MesProLossReasonService.class);
        var controller = new MesProLossReasonController();
        ReflectionTestUtils.setField(controller, "service", service);
        var descriptions = Arrays.asList("裁切损耗", null, "调机损耗");
        when(service.create(descriptions)).thenReturn(List.of());
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/loss-reasons/sparse-import.xlsx"))) {
            controller.importFile(new MockMultipartFile("file", "import.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", input));
        }
        verify(service).create(descriptions);
    }
}
