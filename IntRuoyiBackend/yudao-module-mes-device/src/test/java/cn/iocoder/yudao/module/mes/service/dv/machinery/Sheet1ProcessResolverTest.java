package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Sheet1ProcessResolverTest {
    @Mock private MesProProcessMapper processMapper;
    @InjectMocks private Sheet1ProcessResolver resolver;

    @Test
    void sameNamesResolveToIndependentProcessesByCode() {
        when(processMapper.selectListByCodes(anyCollection())).thenReturn(List.of(
                process(1L, "IDI-GX-0001", 0), process(2L, "ID-GX-0001", 0)));
        var sheet = new Sheet1MachineryProcessExcelParser.ParsedSheet(List.of(
                device("IDI-GX-0001"), device("ID-GX-0001")), List.of(), 0);
        var result = resolver.resolve(sheet);
        assertEquals(1L, result.get("IDI-GX-0001").getId());
        assertEquals(2L, result.get("ID-GX-0001").getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "disabled", "duplicate", "wrongName", "manualWrongName"})
    void rejectsInvalidIdentityWithoutUsingSameName(String scenario) {
        var target = process(1L, "GX-1", "disabled".equals(scenario) ? 1 : 0);
        if (scenario.endsWith("Name")) target.setName("精洗工序");
        var rows = switch (scenario) {
            case "missing" -> List.of(process(2L, "OTHER", 0));
            case "duplicate" -> List.of(target, process(2L, "GX-1", 0));
            default -> List.of(target);
        };
        when(processMapper.selectListByCodes(anyCollection())).thenReturn(rows);
        var sheet = "manualWrongName".equals(scenario)
                ? new Sheet1MachineryProcessExcelParser.ParsedSheet(List.of(), List.of(
                    new Sheet1MachineryProcessExcelParser.ManualRow(2, "产线", "GX-1", "粗洗工序", BigDecimal.TEN, BigDecimal.ONE)), 0)
                : new Sheet1MachineryProcessExcelParser.ParsedSheet(List.of(device("GX-1")), List.of(), 0);
        var error = assertThrows(ServiceException.class, () -> resolver.resolve(sheet));
        assertTrue(error.getMessage().contains("GX-1"));
        if (!"duplicate".equals(scenario)) assertTrue(error.getMessage().contains("第 2 行"));
    }

    private MesProProcessDO process(Long id, String code, int status) {
        return MesProProcessDO.builder().id(id).code(code).name("粗洗工序").status(status).build();
    }

    private Sheet1MachineryProcessExcelParser.DeviceRow device(String code) {
        return new Sheet1MachineryProcessExcelParser.DeviceRow(2, "产线", "B09393", code,
                "粗洗工序", "清洗机", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE);
    }
}
