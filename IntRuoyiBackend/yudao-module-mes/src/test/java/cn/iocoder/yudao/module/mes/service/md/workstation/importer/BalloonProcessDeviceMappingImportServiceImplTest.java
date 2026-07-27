package cn.iocoder.yudao.module.mes.service.md.workstation.importer;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.BalloonProcessDeviceMappingImportRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkshopMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.Sheet1MachineryProcessExcelParser;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationServiceImpl;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationToolService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationWorkerService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.iocoder.yudao.module.mes.service.wm.warehouse.MesWmWarehouseService;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Import({
        BalloonProcessDeviceMappingImportServiceImpl.class,
        MesMdWorkstationServiceImpl.class,
        Sheet1MachineryProcessExcelParser.class
})
class BalloonProcessDeviceMappingImportServiceImplTest extends BaseDbUnitTest {

    @Resource
    private BalloonProcessDeviceMappingImportServiceImpl importService;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesMdWorkshopMapper workshopMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesDvMachineryMapper machineryMapper;
    @Resource
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Resource
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Resource
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;

    @MockitoBean
    private MesMdProductionLineService productionLineService;
    @MockitoBean
    private MesMdWorkshopService workshopService;
    @MockitoBean
    private MesMdWorkstationMachineService workstationMachineService;
    @MockitoBean
    private MesMdWorkstationToolService workstationToolService;
    @MockitoBean
    private MesMdWorkstationWorkerService workstationWorkerService;
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
    void importMapping_reusesExistingWorkstation_createsMissingWorkstation_and_replacesBindingsOnly() throws Exception {
        seedCommonReferenceData();
        processMapper.insert(MesProProcessDO.builder()
                .id(800001L).code("P001").name("吹球囊成型").status(CommonStatusEnum.ENABLE.getStatus()).build());
        processMapper.insert(MesProProcessDO.builder()
                .id(800002L).code("P002").name("球囊裁剪").status(CommonStatusEnum.ENABLE.getStatus()).build());
        processMapper.insert(MesProProcessDO.builder()
                .id(800003L).code("P003").name("穿显影环").status(CommonStatusEnum.ENABLE.getStatus()).build());
        workstationMapper.insert(MesMdWorkstationDO.builder()
                .id(820001L).code("WS-P001").name("吹球囊成型-工位")
                .workshopId(900011L).processId(800001L).status(CommonStatusEnum.ENABLE.getStatus())
                .singleStandardHourlyCapacity(new BigDecimal("12.5")).build());
        workstationWorkerMapper.insert(MesMdWorkstationWorkerDO.builder()
                .id(870001L).workstationId(820001L).quantity(7).remark("旧人力配置").build());
        machineryMapper.insert(MesDvMachineryDO.builder()
                .id(830001L).code("A03190").name("球囊成型机")
                .workshopId(900010L).status(2).standardHourlyCapacity(new BigDecimal("9.523810")).build());
        machineryMapper.insert(MesDvMachineryDO.builder()
                .id(830002L).code("B09262").name("球囊切管工装")
                .workshopId(900010L).status(2).standardHourlyCapacity(new BigDecimal("147.619048")).build());
        machineryProcessMapper.insert(MesDvMachineryProcessDO.builder()
                .machineryId(830001L).processId(800001L).machineryCode("A03190")
                .processName("吹球囊成型").deviceName("球囊成型机")
                .deviceQuantity(new BigDecimal("1")).standardHourlyCapacity(new BigDecimal("9.523810")).build());
        machineryProcessMapper.insert(MesDvMachineryProcessDO.builder()
                .machineryId(830002L).processId(800002L).machineryCode("B09262")
                .processName("球囊裁剪").deviceName("球囊切管工装")
                .deviceQuantity(new BigDecimal("1")).standardHourlyCapacity(new BigDecimal("147.619048")).build());

        BalloonProcessDeviceMappingImportRespVO respVO = importService.importMapping(buildWorkbook(false, false), 900011L);

        assertEquals(3, respVO.getProcessCount());
        assertEquals(2, respVO.getMachineryCount());
        assertEquals(2, respVO.getMachineryProcessCount());
        assertEquals(1, respVO.getReusedWorkstationCount());
        assertEquals(2, respVO.getCreatedWorkstationCount());
        assertEquals(2, respVO.getMachineryBindingCount());
        assertEquals(1, respVO.getManualProcessCount());
        assertEquals(0, respVO.getCreatedMachineryCount());
        assertEquals(0, respVO.getUpdatedMachineryCount());
        assertEquals(0, respVO.getIgnoredCapacityConflictPairCount());
        assertEquals(3, workstationMapper.selectList().size());
        assertEquals(2, workstationMachineMapper.selectList().size());
        assertEquals(2, machineryMapper.selectList().size());
        assertEquals(2, machineryProcessMapper.selectList().size());

        MesMdWorkstationDO reused = workstationMapper.selectByCode("WS-P001");
        assertEquals(0, reused.getSingleStandardHourlyCapacity().compareTo(new BigDecimal("12.50")));
        assertEquals(0, workstationWorkerMapper.selectListByWorkstationId(reused.getId()).size());
        MesMdWorkstationDO manual = workstationMapper.selectByCode("WS-P003");
        assertEquals(0, manual.getSingleStandardHourlyCapacity().compareTo(new BigDecimal("70.48")));
        assertEquals(0, workstationMachineMapper.selectListByWorkstationId(manual.getId()).size());
        List<MesMdWorkstationWorkerDO> manualWorkers = workstationWorkerMapper.selectListByWorkstationId(manual.getId());
        assertEquals(1, manualWorkers.size());
        assertNull(manualWorkers.get(0).getPostId());
        assertEquals(5, manualWorkers.get(0).getQuantity());
    }

    @Test
    void importMapping_capacityConflictOnly_returnsWarningAndStillBuildsOneBinding() throws Exception {
        seedCommonReferenceData();
        processMapper.insert(MesProProcessDO.builder()
                .id(810001L).code("P101").name("外管拉伸2").status(CommonStatusEnum.ENABLE.getStatus()).build());
        machineryMapper.insert(MesDvMachineryDO.builder()
                .id(840001L).code("A03388").name("球囊导管拉伸机（三工位）")
                .workshopId(900010L).status(2).standardHourlyCapacity(new BigDecimal("61.904762")).build());
        machineryProcessMapper.insert(MesDvMachineryProcessDO.builder()
                .machineryId(840001L).processId(810001L).machineryCode("A03388")
                .processName("外管拉伸2").deviceName("球囊导管拉伸机（三工位）")
                .deviceQuantity(new BigDecimal("1")).standardHourlyCapacity(new BigDecimal("61.904762")).build());

        BalloonProcessDeviceMappingImportRespVO respVO = importService.importMapping(buildWorkbook(true, false), 900011L);

        assertEquals(1, respVO.getProcessCount());
        assertEquals(1, respVO.getMachineryBindingCount());
        assertEquals(1, respVO.getIgnoredCapacityConflictPairCount());
        assertEquals(1, respVO.getIgnoredCapacityConflictPairs().size());
        assertEquals("外管拉伸2", respVO.getIgnoredCapacityConflictPairs().get(0).getProcessName());
        assertEquals("A03388", respVO.getIgnoredCapacityConflictPairs().get(0).getMachineryCode());
        assertEquals(List.of(2, 3), respVO.getIgnoredCapacityConflictPairs().get(0).getSourceRowNos());
        assertEquals(List.of("650", "270"), respVO.getIgnoredCapacityConflictPairs().get(0).getDailyCapacities());
        assertEquals(1, workstationMachineMapper.selectList().size());
    }

    @Test
    void importMapping_duplicateDeviceProcessName_usesMachineryProcessPair() throws Exception {
        seedCommonReferenceData();
        processMapper.insert(MesProProcessDO.builder()
                .id(815001L).code("B010").name("吹球囊成型").status(CommonStatusEnum.ENABLE.getStatus()).build());
        processMapper.insert(MesProProcessDO.builder()
                .id(815002L).code("Z2630").name("吹球囊成型").status(CommonStatusEnum.ENABLE.getStatus()).build());
        workstationMapper.insert(MesMdWorkstationDO.builder()
                .id(835002L).code("TPWS-Z2630").name("吹球囊成型")
                .workshopId(900011L).processId(815002L).status(CommonStatusEnum.ENABLE.getStatus())
                .remark("third-party feedback seed").build());
        machineryMapper.insert(MesDvMachineryDO.builder()
                .id(845001L).code("A03190").name("球囊成型机")
                .workshopId(900010L).status(2).standardHourlyCapacity(new BigDecimal("9.523810")).build());
        machineryProcessMapper.insert(MesDvMachineryProcessDO.builder()
                .machineryId(845001L).processId(815001L).machineryCode("A03190")
                .processName("吹球囊成型").deviceName("球囊成型机")
                .deviceQuantity(new BigDecimal("1")).standardHourlyCapacity(new BigDecimal("9.523810")).build());

        BalloonProcessDeviceMappingImportRespVO respVO = importService.importMapping(
                buildDeviceOnlyWorkbook("吹球囊成型", "A03190", "球囊成型机", 100), 900011L);

        assertEquals(1, respVO.getProcessCount());
        assertEquals(1, respVO.getMachineryBindingCount());
        MesMdWorkstationDO synced = workstationMapper.selectByCode("WS-B010");
        assertNotNull(synced);
        assertEquals(815001L, synced.getProcessId());
        assertEquals(1, workstationMachineMapper.selectListByWorkstationId(synced.getId()).size());
        assertEquals(0, workstationMachineMapper.selectListByWorkstationId(835002L).size());
    }

    @Test
    void importMapping_duplicateManualProcessName_reusesImporterOwnedWorkstation() throws Exception {
        seedCommonReferenceData();
        processMapper.insert(MesProProcessDO.builder()
                .id(818001L).code("PROC-XLSX-00007").name("包套装管").status(CommonStatusEnum.ENABLE.getStatus()).build());
        processMapper.insert(MesProProcessDO.builder()
                .id(818002L).code("Z760").name("包套装管").status(CommonStatusEnum.ENABLE.getStatus()).build());
        workstationMapper.insert(MesMdWorkstationDO.builder()
                .id(838001L).code("WS-PROC-XLSX-00007").name("包套装管-工位")
                .workshopId(900011L).processId(818001L).status(CommonStatusEnum.ENABLE.getStatus())
                .remark("球囊扩张导管工序设备关系同步").build());
        workstationMapper.insert(MesMdWorkstationDO.builder()
                .id(838002L).code("TPWS-Z760").name("包套装管")
                .workshopId(900011L).processId(818002L).status(CommonStatusEnum.ENABLE.getStatus())
                .remark("third-party feedback seed").build());

        BalloonProcessDeviceMappingImportRespVO respVO = importService.importMapping(
                buildManualOnlyWorkbook("包套装管", 5440), 900011L);

        assertEquals(1, respVO.getProcessCount());
        assertEquals(1, respVO.getManualProcessCount());
        MesMdWorkstationDO synced = workstationMapper.selectByCode("WS-PROC-XLSX-00007");
        assertEquals(0, synced.getSingleStandardHourlyCapacity().compareTo(new BigDecimal("518.10")));
        List<MesMdWorkstationWorkerDO> syncedWorkers = workstationWorkerMapper.selectListByWorkstationId(838001L);
        assertEquals(1, syncedWorkers.size());
        assertEquals(5, syncedWorkers.get(0).getQuantity());
        assertEquals(0, workstationWorkerMapper.selectListByWorkstationId(838002L).size());
    }

    @Test
    void importMapping_quantityConflict_stillFailsFast() throws Exception {
        seedCommonReferenceData();
        processMapper.insert(MesProProcessDO.builder()
                .id(820001L).code("P201").name("内管拉伸2").status(CommonStatusEnum.ENABLE.getStatus()).build());
        machineryMapper.insert(MesDvMachineryDO.builder()
                .id(850001L).code("A03388").name("球囊导管拉伸机（三工位）")
                .workshopId(900010L).status(2).build());
        machineryProcessMapper.insert(MesDvMachineryProcessDO.builder()
                .machineryId(850001L).processId(820001L).machineryCode("A03388")
                .processName("内管拉伸2").deviceName("球囊导管拉伸机（三工位）")
                .deviceQuantity(new BigDecimal("1")).standardHourlyCapacity(new BigDecimal("80")).build());

        assertThrows(RuntimeException.class, () -> importService.importMapping(buildWorkbook(false, true), 900011L));
    }

    @Test
    void importMapping_sameProcessWithMachineAndManualRows_failsFast() throws Exception {
        seedCommonReferenceData();
        processMapper.insert(MesProProcessDO.builder()
                .id(830001L).code("P301").name("吹球囊成型").status(CommonStatusEnum.ENABLE.getStatus()).build());
        machineryMapper.insert(MesDvMachineryDO.builder()
                .id(860001L).code("A03190").name("球囊成型机")
                .workshopId(900010L).status(2).build());
        machineryProcessMapper.insert(MesDvMachineryProcessDO.builder()
                .machineryId(860001L).processId(830001L).machineryCode("A03190")
                .processName("吹球囊成型").deviceName("球囊成型机")
                .deviceQuantity(new BigDecimal("1")).standardHourlyCapacity(new BigDecimal("9.523810")).build());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> importService.importMapping(buildMixedResourceWorkbook(), 900011L));

        assertEquals("工序 [吹球囊成型] 同时存在设备映射和纯人工行，无法同步", exception.getMessage());
    }

    private void seedCommonReferenceData() {
        workshopMapper.insert(MesMdWorkshopDO.builder()
                .id(900010L).code("AUTO-WSHOP").name("AutoScheduleWorkshop").status(CommonStatusEnum.ENABLE.getStatus()).build());
        workshopMapper.insert(MesMdWorkshopDO.builder()
                .id(900011L).code("WS-20260513-01").name("车间1").status(CommonStatusEnum.ENABLE.getStatus()).build());

        when(workshopService.getWorkshop(900011L))
                .thenReturn(MesMdWorkshopDO.builder().id(900011L).name("车间1").build());
        doNothing().when(processService).validateProcessExistsAndEnable(anyLong());
        when(warehouseService.ensureWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE))
                .thenReturn(MesWmWarehouseDO.builder().id(840001L).code(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE).build());
        when(locationService.getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION))
                .thenReturn(MesWmWarehouseLocationDO.builder().id(840002L).warehouseId(840001L)
                        .code(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION).build());
        when(areaService.getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA))
                .thenReturn(MesWmWarehouseAreaDO.builder().id(840003L).locationId(840002L)
                        .code(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA).build());
    }

    private MockMultipartFile buildWorkbook(boolean capacityConflictOnly, boolean quantityConflict) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("产品名称");
            header.createCell(1).setCellValue("物料编码");
            header.createCell(2).setCellValue("设备编码");
            header.createCell(3).setCellValue("工序名称");
            header.createCell(4).setCellValue("设备名称");
            header.createCell(5).setCellValue("设备数量");
            header.createCell(6).setCellValue("10.5小时日产能");
            header.createCell(7).setCellValue("人工");

            if (capacityConflictOnly) {
                Row row1 = sheet.createRow(1);
                row1.createCell(0).setCellValue("球囊扩张导管");
                row1.createCell(2).setCellValue("A03388");
                row1.createCell(3).setCellValue("外管拉伸2");
                row1.createCell(4).setCellValue("球囊导管拉伸机（三工位）");
                row1.createCell(5).setCellValue(1);
                row1.createCell(6).setCellValue(650);

                Row row2 = sheet.createRow(2);
                row2.createCell(2).setCellValue("A03388");
                row2.createCell(3).setCellValue("外管拉伸2");
                row2.createCell(4).setCellValue("球囊导管拉伸机（三工位）");
                row2.createCell(5).setCellValue(1);
                row2.createCell(6).setCellValue(270);
            } else if (quantityConflict) {
                Row row1 = sheet.createRow(1);
                row1.createCell(0).setCellValue("球囊扩张导管");
                row1.createCell(2).setCellValue("A03388");
                row1.createCell(3).setCellValue("内管拉伸2");
                row1.createCell(4).setCellValue("球囊导管拉伸机（三工位）");
                row1.createCell(5).setCellValue(1);
                row1.createCell(6).setCellValue(840);

                Row row2 = sheet.createRow(2);
                row2.createCell(2).setCellValue("A03388");
                row2.createCell(3).setCellValue("内管拉伸2");
                row2.createCell(4).setCellValue("球囊导管拉伸机（三工位）");
                row2.createCell(5).setCellValue(2);
                row2.createCell(6).setCellValue(840);
            } else {
                Row row1 = sheet.createRow(1);
                row1.createCell(0).setCellValue("球囊扩张导管");
                row1.createCell(2).setCellValue("A03190");
                row1.createCell(3).setCellValue("吹球囊成型");
                row1.createCell(4).setCellValue("球囊成型机");
                row1.createCell(5).setCellValue(1);
                row1.createCell(6).setCellValue(100);

                Row row2 = sheet.createRow(2);
                row2.createCell(2).setCellValue("B09262");
                row2.createCell(3).setCellValue("球囊裁剪");
                row2.createCell(4).setCellValue("球囊切管工装");
                row2.createCell(5).setCellValue(1);
                row2.createCell(6).setCellValue(1550);

                Row row3 = sheet.createRow(3);
                row3.createCell(2).setCellValue("/");
                row3.createCell(3).setCellValue("穿显影环");
                row3.createCell(4).setCellValue("/");
                row3.createCell(5).setCellValue("/");
                row3.createCell(6).setCellValue("/");
                row3.createCell(7).setCellValue(740);
            }

            workbook.write(outputStream);
            return new MockMultipartFile("file", "balloon.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MockMultipartFile buildMixedResourceWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("产品名称");
            header.createCell(1).setCellValue("物料编码");
            header.createCell(2).setCellValue("设备编码");
            header.createCell(3).setCellValue("工序名称");
            header.createCell(4).setCellValue("设备名称");
            header.createCell(5).setCellValue("设备数量");
            header.createCell(6).setCellValue("10.5小时日产能");
            header.createCell(7).setCellValue("人工");

            Row machineRow = sheet.createRow(1);
            machineRow.createCell(0).setCellValue("球囊扩张导管");
            machineRow.createCell(2).setCellValue("A03190");
            machineRow.createCell(3).setCellValue("吹球囊成型");
            machineRow.createCell(4).setCellValue("球囊成型机");
            machineRow.createCell(5).setCellValue(1);
            machineRow.createCell(6).setCellValue(100);

            Row manualRow = sheet.createRow(2);
            manualRow.createCell(2).setCellValue("/");
            manualRow.createCell(3).setCellValue("吹球囊成型");
            manualRow.createCell(4).setCellValue("/");
            manualRow.createCell(5).setCellValue("/");
            manualRow.createCell(6).setCellValue("/");
            manualRow.createCell(7).setCellValue(740);

            workbook.write(outputStream);
            return new MockMultipartFile("file", "balloon-mixed.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MockMultipartFile buildDeviceOnlyWorkbook(String processName, String machineryCode,
                                                      String deviceName, double dailyCapacity) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            createHeader(sheet.createRow(0));

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("球囊扩张导管");
            row.createCell(2).setCellValue(machineryCode);
            row.createCell(3).setCellValue(processName);
            row.createCell(4).setCellValue(deviceName);
            row.createCell(5).setCellValue(1);
            row.createCell(6).setCellValue(dailyCapacity);

            workbook.write(outputStream);
            return new MockMultipartFile("file", "balloon-device.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MockMultipartFile buildManualOnlyWorkbook(String processName, double manualDailyCapacity) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            createHeader(sheet.createRow(0));

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("球囊扩张导管");
            row.createCell(2).setCellValue("/");
            row.createCell(3).setCellValue(processName);
            row.createCell(4).setCellValue("/");
            row.createCell(5).setCellValue("/");
            row.createCell(6).setCellValue("/");
            row.createCell(7).setCellValue(manualDailyCapacity);

            workbook.write(outputStream);
            return new MockMultipartFile("file", "balloon-manual.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private void createHeader(Row header) {
        header.createCell(0).setCellValue("产品名称");
        header.createCell(1).setCellValue("物料编码");
        header.createCell(2).setCellValue("设备编码");
        header.createCell(3).setCellValue("工序名称");
        header.createCell(4).setCellValue("设备名称");
        header.createCell(5).setCellValue("设备数量");
        header.createCell(6).setCellValue("10.5小时日产能");
        header.createCell(7).setCellValue("人工");
    }
}
