package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryFinalSyncRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryTypeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkshopMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.service.dv.checkplan.MesDvCheckPlanMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.checkrecord.MesDvCheckRecordService;
import cn.iocoder.yudao.module.mes.service.dv.maintenrecord.MesDvMaintenRecordService;
import cn.iocoder.yudao.module.mes.service.dv.repair.MesDvRepairService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({MesDvMachineryFinalSheetSyncServiceImpl.class, Sheet1MachineryProcessExcelParser.class})
class MesDvMachineryFinalSheetSyncServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesDvMachineryFinalSheetSyncServiceImpl syncService;
    @Resource
    private MesDvMachineryMapper machineryMapper;
    @Resource
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Resource
    private MesDvMachineryTypeMapper machineryTypeMapper;
    @Resource
    private MesMdWorkshopMapper workshopMapper;
    @Resource
    private MesProProcessMapper processMapper;

    @MockitoBean
    private MesWmBarcodeService barcodeService;
    @MockitoBean
    private MesDvCheckPlanMachineryService checkPlanMachineryService;
    @MockitoBean
    private MesDvCheckRecordService checkRecordService;
    @MockitoBean
    private MesDvMaintenRecordService maintenRecordService;
    @MockitoBean
    private MesDvRepairService repairService;

    @Test
    void testSyncFinalSheet_exactlyReplaceMainTableAndCaptureLineName() throws Exception {
        workshopMapper.insert(MesMdWorkshopDO.builder()
                .id(900010L)
                .code("AUTO-WSHOP")
                .name("AutoScheduleWorkshop")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        processMapper.insert(MesProProcessDO.builder()
                .id(800001L).code("PROC-001").name("吹球囊成型").status(CommonStatusEnum.ENABLE.getStatus()).build());
        processMapper.insert(MesProProcessDO.builder()
                .id(800002L).code("PROC-002").name("外管与球囊焊接").status(CommonStatusEnum.ENABLE.getStatus()).build());
        processMapper.insert(MesProProcessDO.builder()
                .id(800003L).code("PROC-003").name("焊接圆角").status(CommonStatusEnum.ENABLE.getStatus()).build());

        machineryMapper.insert(MesDvMachineryDO.builder()
                .code("A03190")
                .name("old-name")
                .machineryTypeId(999L)
                .workshopId(999L)
                .status(1)
                .build());
        machineryMapper.insert(MesDvMachineryDO.builder()
                .code("OLD-001")
                .name("to-delete")
                .machineryTypeId(999L)
                .workshopId(999L)
                .status(1)
                .build());

        MockMultipartFile file = buildWorkbook();

        MesDvMachineryFinalSyncRespVO respVO = syncService.syncFinalSheet(file);

        assertEquals(4, respVO.getExcelEffectiveRowCount());
        assertEquals(1, respVO.getIgnoredPlaceholderRowCount());
        assertEquals(2, respVO.getMachineryCount());
        assertEquals(4, respVO.getProcessDetailCount());
        assertEquals(1, respVO.getCreatedCount());
        assertEquals(1, respVO.getUpdatedCount());
        assertEquals(1, respVO.getDeletedCount());
        assertEquals("DEFAULT-MACHINERY-TYPE", respVO.getDefaultMachineryTypeCode());
        assertEquals("AUTO-WSHOP", respVO.getDefaultWorkshopCode());

        List<MesDvMachineryDO> machineries = machineryMapper.selectList();
        assertEquals(2, machineries.size());
        assertFalse(machineries.stream().anyMatch(item -> "OLD-001".equals(item.getCode())));

        MesDvMachineryTypeDO defaultType = machineryTypeMapper.selectByCode("DEFAULT-MACHINERY-TYPE");
        assertEquals("默认设备类型", defaultType.getName());

        MesDvMachineryDO a03190 = machineryMapper.selectByCode("A03190");
        assertEquals("球囊成型机", a03190.getName());
        assertEquals("吹球囊成型", a03190.getProcessName());
        assertEquals(0, a03190.getStandardHourlyCapacity().compareTo(new BigDecimal("9.523810")));
        assertEquals(defaultType.getId(), a03190.getMachineryTypeId());
        assertEquals(900010L, a03190.getWorkshopId());

        MesDvMachineryDO a03196 = machineryMapper.selectByCode("A03196");
        assertEquals("激光焊接机", a03196.getName());
        assertNull(a03196.getProcessName());
        assertNull(a03196.getStandardHourlyCapacity());
        assertEquals(defaultType.getId(), a03196.getMachineryTypeId());
        assertEquals(900010L, a03196.getWorkshopId());

        List<MesDvMachineryProcessDO> processRows = machineryProcessMapper.selectList();
        assertEquals(4, processRows.size());
        assertTrue(processRows.stream().anyMatch(item ->
                "A03190".equals(item.getMachineryCode())
                        && "球囊扩张导管".equals(item.getLineName())
                        && "吹球囊成型".equals(item.getProcessName())));
        assertTrue(processRows.stream().anyMatch(item ->
                "A03190".equals(item.getMachineryCode())
                        && "棘突球囊扩张导管".equals(item.getLineName())
                        && "吹球囊成型".equals(item.getProcessName())));
        assertTrue(processRows.stream().anyMatch(item ->
                "A03196".equals(item.getMachineryCode())
                        && item.getProcessId().equals(800002L)
                        && "球囊扩张导管".equals(item.getLineName())
                        && item.getStandardHourlyCapacity().compareTo(new BigDecimal("55.714286")) == 0));
        assertTrue(processRows.stream().anyMatch(item ->
                "A03196".equals(item.getMachineryCode())
                        && item.getProcessId().equals(800003L)
                        && "球囊扩张导管".equals(item.getLineName())
                        && item.getStandardHourlyCapacity().compareTo(new BigDecimal("28.571429")) == 0));
    }

    @Test
    void syncFinalSheet_rejectsDifferentCapacityForSameMachineryAndProcessAcrossProducts() throws Exception {
        workshopMapper.insert(MesMdWorkshopDO.builder()
                .id(900010L)
                .code("AUTO-WSHOP")
                .name("AutoScheduleWorkshop")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        processMapper.insert(MesProProcessDO.builder()
                .id(800001L).code("PROC-001").name("吹球囊成型").status(CommonStatusEnum.ENABLE.getStatus()).build());

        MockMultipartFile file = buildConflictingCapacityWorkbook();

        ServiceException exception = assertThrows(ServiceException.class, () -> syncService.syncFinalSheet(file));

        assertTrue(exception.getMessage().contains("同一设备同一工序只能配置一个标准小时产能"));
        assertTrue(exception.getMessage().contains("A03190"));
        assertTrue(exception.getMessage().contains("吹球囊成型"));
        assertTrue(exception.getMessage().contains("Excel 行 2"));
        assertTrue(exception.getMessage().contains("Excel 行 3"));
        assertTrue(machineryProcessMapper.selectList().isEmpty());
    }

    private MockMultipartFile buildWorkbook() throws Exception {
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

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("球囊扩张导管");
            row1.createCell(2).setCellValue("A03190");
            row1.createCell(3).setCellValue("吹球囊成型");
            row1.createCell(4).setCellValue("球囊成型机");
            row1.createCell(5).setCellValue(1);
            row1.createCell(6).setCellValue(100);

            Row row2 = sheet.createRow(2);
            row2.createCell(2).setCellValue("A03196");
            row2.createCell(3).setCellValue("外管与球囊焊接");
            row2.createCell(4).setCellValue("激光焊接机");
            row2.createCell(5).setCellValue(1);
            row2.createCell(6).setCellValue(585);

            Row row3 = sheet.createRow(3);
            row3.createCell(2).setCellValue("A03196");
            row3.createCell(3).setCellValue("焊接圆角");
            row3.createCell(4).setCellValue("激光焊接机");
            row3.createCell(5).setCellValue(1);
            row3.createCell(6).setCellValue(300);

            Row row4 = sheet.createRow(4);
            row4.createCell(0).setCellValue("棘突球囊扩张导管");
            row4.createCell(2).setCellValue("A03190");
            row4.createCell(3).setCellValue("吹球囊成型");
            row4.createCell(4).setCellValue("球囊成型机");
            row4.createCell(5).setCellValue(1);
            row4.createCell(6).setCellValue(100);

            Row row5 = sheet.createRow(5);
            row5.createCell(2).setCellValue("/");
            row5.createCell(3).setCellValue("占位");
            row5.createCell(4).setCellValue("/");
            row5.createCell(5).setCellValue("/");
            row5.createCell(6).setCellValue("/");

            workbook.write(outputStream);
            return new MockMultipartFile("file", "final.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MockMultipartFile buildConflictingCapacityWorkbook() throws Exception {
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

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("球囊扩张导管");
            row1.createCell(2).setCellValue("A03190");
            row1.createCell(3).setCellValue("吹球囊成型");
            row1.createCell(4).setCellValue("球囊成型机");
            row1.createCell(5).setCellValue(1);
            row1.createCell(6).setCellValue(100);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("棘突球囊扩张导管");
            row2.createCell(2).setCellValue("A03190");
            row2.createCell(3).setCellValue("吹球囊成型");
            row2.createCell(4).setCellValue("球囊成型机");
            row2.createCell(5).setCellValue(1);
            row2.createCell(6).setCellValue(200);

            workbook.write(outputStream);
            return new MockMultipartFile("file", "final-conflict.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }
}
