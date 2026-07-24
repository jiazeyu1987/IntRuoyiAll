package cn.iocoder.yudao.module.mes.service.wm.warehouse;

import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.warehouse.MesWmWarehouseMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import cn.iocoder.yudao.module.mes.service.wm.materialstock.MesWmMaterialStockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesWmWarehouseServiceImplReadOnlyTest {

    @InjectMocks
    private MesWmWarehouseServiceImpl warehouseService;

    @Mock
    private MesWmWarehouseMapper warehouseMapper;
    @Mock
    private MesWmWarehouseLocationService locationService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesWmMaterialStockService materialStockService;
    @Mock
    private MesWmBarcodeService barcodeService;

    @Test
    void getWarehouseByCodeShouldNotCreateMissingVirtualWarehouse() {
        when(warehouseMapper.selectByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE)).thenReturn(null);

        MesWmWarehouseDO warehouse =
                warehouseService.getWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE);

        assertNull(warehouse);
        verify(warehouseMapper, never()).insert(any(MesWmWarehouseDO.class));
    }

    @Test
    void serviceShouldExposeExplicitGetOrCreateCommand() throws Exception {
        assertNotNull(MesWmWarehouseService.class.getMethod("ensureWarehouseByCode", String.class));
    }
}
