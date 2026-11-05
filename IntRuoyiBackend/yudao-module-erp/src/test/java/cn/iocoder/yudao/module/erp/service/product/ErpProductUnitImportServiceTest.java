package cn.iocoder.yudao.module.erp.service.product;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitImportVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpProductUnitImportServiceTest {

    @Mock
    private ErpProductUnitMapper mapper;
    @InjectMocks
    private ErpProductUnitImportService service;

    @Test
    void importsBothStatusesAndNormalizesNames() {
        assertEquals(2, service.importUnits(List.of(row(" 盒 ", "0"), row("箱", "关闭"))));
        ArgumentCaptor<ErpProductUnitDO> units = ArgumentCaptor.forClass(ErpProductUnitDO.class);
        verify(mapper, times(2)).insert(units.capture());
        assertEquals("盒", units.getAllValues().get(0).getName());
        assertEquals(0, units.getAllValues().get(0).getStatus());
        assertEquals(1, units.getAllValues().get(1).getStatus());
        assertNull(units.getAllValues().get(0).getId());
    }

    @Test
    void skipsDuplicateRowsAndKeepsFirstStatus() {
        assertEquals(2, service.importUnits(List.of(row("盒", "0"), row(" 盒 ", "1"), row("张", "开启"))));
        ArgumentCaptor<ErpProductUnitDO> units = ArgumentCaptor.forClass(ErpProductUnitDO.class);
        verify(mapper, times(2)).insert(units.capture());
        assertEquals("盒", units.getAllValues().get(0).getName());
        assertEquals(0, units.getAllValues().get(0).getStatus());
        assertEquals("张", units.getAllValues().get(1).getName());
    }

    @Test
    void skipsExistingUnitWithoutUpdatingIt() {
        when(mapper.selectByName("盒")).thenReturn(ErpProductUnitDO.builder().id(1L).name("盒").build());
        assertEquals(0, service.importUnits(List.of(row("盒", "0"), row(" 盒 ", "1"))));
        verify(mapper, never()).insert(any(ErpProductUnitDO.class));
        verify(mapper, never()).updateById(any(ErpProductUnitDO.class));
    }

    @Test
    void importsNewUnitsAlongsideExistingAndFileDuplicates() {
        when(mapper.selectByName("盒")).thenReturn(ErpProductUnitDO.builder().id(1L).name("盒").build());
        assertEquals(1, service.importUnits(List.of(row("盒", "0"), row("张", "1"), row(" 张 ", "0"))));
        ArgumentCaptor<ErpProductUnitDO> unit = ArgumentCaptor.forClass(ErpProductUnitDO.class);
        verify(mapper).insert(unit.capture());
        assertEquals("张", unit.getValue().getName());
        assertEquals(1, unit.getValue().getStatus());
        verify(mapper, never()).updateById(any(ErpProductUnitDO.class));
    }

    @Test
    void rejectsInvalidLaterRowWithoutPartialImport() {
        for (String status : List.of("", "2", "启用")) {
            assertThrows(ServiceException.class,
                    () -> service.importUnits(List.of(row("盒", "开启"), row("箱", status))));
        }
        assertThrows(ServiceException.class, () -> service.importUnits(List.of(row(" ", "0"))));
        assertThrows(ServiceException.class, () -> service.importUnits(List.of()));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.importUnits(List.of(row("盒", "0"), row("盒", "1"), row("张", "2"))));
        assertTrue(error.getMessage().contains("第 4 行"));
        verify(mapper, never()).insert(any(ErpProductUnitDO.class));
    }

    @Test
    void exportedWorkbookCanBeReadForImportWithoutReusingIds() throws Exception {
        ErpProductUnitRespVO exported = new ErpProductUnitRespVO();
        exported.setId(123L);
        exported.setName("盒");
        exported.setStatus(0);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.write(response, "产品单位.xlsx", "数据", ErpProductUnitRespVO.class, List.of(exported));
        List<ErpProductUnitImportVO> rows = ExcelUtils.read(
                new ByteArrayInputStream(response.getContentAsByteArray()), ErpProductUnitImportVO.class);
        assertEquals(1, service.importUnits(rows));
        ArgumentCaptor<ErpProductUnitDO> unit = ArgumentCaptor.forClass(ErpProductUnitDO.class);
        verify(mapper).insert(unit.capture());
        assertEquals("盒", unit.getValue().getName());
        assertEquals(0, unit.getValue().getStatus());
        assertNull(unit.getValue().getId());
    }

    private static ErpProductUnitImportVO row(String name, String status) {
        ErpProductUnitImportVO row = new ErpProductUnitImportVO();
        row.setName(name);
        row.setStatus(status);
        return row;
    }
}
