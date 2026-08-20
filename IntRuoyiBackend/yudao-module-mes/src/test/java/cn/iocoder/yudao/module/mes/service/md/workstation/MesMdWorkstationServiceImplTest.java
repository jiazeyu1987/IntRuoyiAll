package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(MesMdWorkstationServiceImpl.class)
class MesMdWorkstationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesMdWorkstationServiceImpl workstationService;
    @Resource
    private MesMdWorkstationMapper workstationMapper;

    @MockitoBean
    private MesMdWorkstationMachineService workstationMachineService;
    @MockitoBean
    private MesMdWorkstationToolService workstationToolService;
    @MockitoBean
    private MesMdWorkstationWorkerService workstationWorkerService;
    @MockitoBean
    private MesMdWorkshopService workshopService;
    @MockitoBean
    private MesMdProductionLineService productionLineService;
    @MockitoBean
    private MesWmWarehouseService warehouseService;
    @MockitoBean
    private MesWmWarehouseLocationService locationService;
    @MockitoBean
    private MesWmWarehouseAreaService areaService;
    @MockitoBean
    private MesWmBarcodeService barcodeService;
    @MockitoBean
    private MesProProcessService processService;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;

    @Test
    void createWorkstation_persistCapacityFields() {
        MesMdWorkstationSaveReqVO reqVO = new MesMdWorkstationSaveReqVO();
        reqVO.setCode("WS-CAP-01");
        reqVO.setName("产能测试工位");
        reqVO.setAddress("车间1");
        reqVO.setWorkshopId(10L);
        reqVO.setProcessId(20L);
        reqVO.setWarehouseId(30L);
        reqVO.setLocationId(31L);
        reqVO.setAreaId(32L);
        reqVO.setSingleStandardHourlyCapacity(new BigDecimal("16.5"));
        reqVO.setShiftHours(new BigDecimal("7.5"));
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setRemark("capacity");

        when(workshopService.getWorkshop(10L)).thenReturn(MesMdWorkshopDO.builder().id(10L).build());
        doNothing().when(processService).validateProcessExistsAndEnable(20L);
        when(warehouseService.validateWarehouseExists(30L)).thenReturn(MesWmWarehouseDO.builder().id(30L).build());
        when(locationService.validateWarehouseLocationExists(31L))
                .thenReturn(MesWmWarehouseLocationDO.builder().id(31L).warehouseId(30L).build());
        when(areaService.validateWarehouseAreaExists(32L))
                .thenReturn(MesWmWarehouseAreaDO.builder().id(32L).locationId(31L).build());

        Long id = workstationService.createWorkstation(reqVO);

        MesMdWorkstationDO workstation = workstationMapper.selectById(id);
        assertNotNull(workstation);
        assertEquals(0, workstation.getSingleStandardHourlyCapacity().compareTo(new BigDecimal("16.5")));
        assertEquals(0, workstation.getShiftHours().compareTo(new BigDecimal("7.5")));
        verify(barcodeService).autoGenerateBarcode(any(), any(), any(), any());
    }

    @Test
    void createWorkstation_withoutWarehouseHierarchy_keepsFieldsEmpty() {
        MesMdWorkstationSaveReqVO reqVO = new MesMdWorkstationSaveReqVO();
        reqVO.setCode("WS-NO-WAREHOUSE-01");
        reqVO.setName("无仓储配置工作站");
        reqVO.setWorkshopId(10L);
        reqVO.setProcessId(20L);
        reqVO.setShiftHours(new BigDecimal("8"));
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        when(workshopService.getWorkshop(10L)).thenReturn(MesMdWorkshopDO.builder().id(10L).build());
        doNothing().when(processService).validateProcessExistsAndEnable(20L);
        when(warehouseService.ensureWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE))
                .thenReturn(MesWmWarehouseDO.builder().id(30L).build());
        when(locationService.getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION))
                .thenReturn(MesWmWarehouseLocationDO.builder().id(31L).warehouseId(30L).build());
        when(areaService.getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA))
                .thenReturn(MesWmWarehouseAreaDO.builder().id(32L).locationId(31L).build());

        Long id = workstationService.createWorkstation(reqVO);

        MesMdWorkstationDO workstation = workstationMapper.selectById(id);
        assertNotNull(workstation);
        assertNull(workstation.getWarehouseId());
        assertNull(workstation.getLocationId());
        assertNull(workstation.getAreaId());
    }

    @Test
    void updateWorkstation_persistShiftHours() {
        MesMdWorkstationDO existing = MesMdWorkstationDO.builder()
                .code("WS-CAP-02")
                .name("产能测试工位2")
                .address("车间1")
                .workshopId(10L)
                .processId(20L)
                .warehouseId(30L)
                .locationId(31L)
                .areaId(32L)
                .singleStandardHourlyCapacity(new BigDecimal("12"))
                .shiftHours(new BigDecimal("8"))
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        workstationMapper.insert(existing);

        MesMdWorkstationSaveReqVO reqVO = new MesMdWorkstationSaveReqVO();
        reqVO.setId(existing.getId());
        reqVO.setCode("WS-CAP-02");
        reqVO.setName("产能测试工位2");
        reqVO.setAddress("车间1");
        reqVO.setWorkshopId(10L);
        reqVO.setProcessId(20L);
        reqVO.setWarehouseId(30L);
        reqVO.setLocationId(31L);
        reqVO.setAreaId(32L);
        reqVO.setSingleStandardHourlyCapacity(new BigDecimal("12"));
        reqVO.setShiftHours(new BigDecimal("6.5"));
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        when(workshopService.getWorkshop(10L)).thenReturn(MesMdWorkshopDO.builder().id(10L).build());
        doNothing().when(processService).validateProcessExistsAndEnable(20L);
        when(warehouseService.validateWarehouseExists(30L)).thenReturn(MesWmWarehouseDO.builder().id(30L).build());
        when(locationService.validateWarehouseLocationExists(31L))
                .thenReturn(MesWmWarehouseLocationDO.builder().id(31L).warehouseId(30L).build());
        when(areaService.validateWarehouseAreaExists(32L))
                .thenReturn(MesWmWarehouseAreaDO.builder().id(32L).locationId(31L).build());

        workstationService.updateWorkstation(reqVO);

        MesMdWorkstationDO workstation = workstationMapper.selectById(existing.getId());
        assertEquals(0, workstation.getShiftHours().compareTo(new BigDecimal("6.5")));
    }

    @Test
    void updateWorkstation_withClearedWarehouseHierarchy_persistsNulls() {
        MesMdWorkstationDO existing = MesMdWorkstationDO.builder()
                .code("WS-CLEAR-WAREHOUSE-01")
                .name("清空仓储配置工作站")
                .workshopId(10L)
                .processId(20L)
                .warehouseId(30L)
                .locationId(31L)
                .areaId(32L)
                .shiftHours(new BigDecimal("8"))
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        workstationMapper.insert(existing);

        MesMdWorkstationSaveReqVO reqVO = new MesMdWorkstationSaveReqVO();
        reqVO.setId(existing.getId());
        reqVO.setCode(existing.getCode());
        reqVO.setName(existing.getName());
        reqVO.setWorkshopId(10L);
        reqVO.setProcessId(20L);
        reqVO.setShiftHours(new BigDecimal("8"));
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        when(workshopService.getWorkshop(10L)).thenReturn(MesMdWorkshopDO.builder().id(10L).build());
        doNothing().when(processService).validateProcessExistsAndEnable(20L);
        when(warehouseService.ensureWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE))
                .thenReturn(MesWmWarehouseDO.builder().id(40L).build());
        when(locationService.getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION))
                .thenReturn(MesWmWarehouseLocationDO.builder().id(41L).warehouseId(40L).build());
        when(areaService.getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA))
                .thenReturn(MesWmWarehouseAreaDO.builder().id(42L).locationId(41L).build());

        workstationService.updateWorkstation(reqVO);

        MesMdWorkstationDO workstation = workstationMapper.selectById(existing.getId());
        assertNull(workstation.getWarehouseId());
        assertNull(workstation.getLocationId());
        assertNull(workstation.getAreaId());
    }

    @Test
    void getWorkstationListByProcessIds_mapsLegacyProcessBindingToCurrentIdentity() {
        MesMdWorkstationDO legacyWorkstation = MesMdWorkstationDO.builder()
                .code("WS-LEGACY-01")
                .name("历史工序工作站")
                .processId(900L)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        workstationMapper.insert(legacyWorkstation);
        when(routeProcessService.getProcessIdentityMap(List.of(901L)))
                .thenReturn(Map.of(900L, 901L, 901L, 901L));

        List<MesMdWorkstationDO> result = workstationService.getWorkstationListByProcessIds(List.of(901L));

        assertEquals(1, result.size());
        assertEquals(legacyWorkstation.getId(), result.get(0).getId());
        assertEquals(901L, result.get(0).getProcessId());
    }
}
