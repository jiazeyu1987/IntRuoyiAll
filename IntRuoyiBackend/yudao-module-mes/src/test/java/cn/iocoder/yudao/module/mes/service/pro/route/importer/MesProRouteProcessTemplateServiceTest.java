package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowValidationRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteCandidateConfigService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessFlowService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionWorkflowService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.IMPORT_MODE_REBUILD;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.IMPORT_MODE_UPGRADE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteProcessTemplateServiceTest {

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Mock
    private MesDvMachineryMapper machineryMapper;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Mock
    private MesProRouteVersionWorkflowService routeVersionWorkflowService;
    @Mock
    private MesProRouteCandidateConfigService routeCandidateConfigService;
    @InjectMocks
    private MesProRouteProcessTemplateServiceImpl service;

    @Test
    void exportTemplate_containsOnlyEmployeeProcessFieldsInRouteOrder() throws Exception {
        MesProRouteDO route = route();
        MesProRouteProcessDO first = routeProcess(20L, 30L, 100L, 1, true);
        MesProRouteProcessDO second = routeProcess(21L, 31L, 101L, 2, false);
        MesProRouteVersionDO activeVersion = MesProRouteVersionDO.builder().id(500L).routeId(10L)
                .active(true).lifecycleStatus("ACTIVE").versionNo("V1").build();

        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(first, second));
        when(processMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                MesProProcessDO.builder().id(30L).name("裁切").build(),
                MesProProcessDO.builder().id(31L).name("包装").build()));
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(activeVersion);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(500L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder().routeVersionId(500L).routeProcessId(20L)
                        .hourlyCapacity(new BigDecimal("12.5")).build(),
                MesProRouteScheduleConfigDO.builder().routeVersionId(500L).routeProcessId(21L)
                        .hourlyCapacity(new BigDecimal("8")).build()));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(100L).processId(30L).build(),
                MesMdWorkstationDO.builder().id(101L).processId(31L).build()));
        when(workstationMachineMapper.selectListByWorkstationIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationMachineDO.builder().workstationId(100L).machineryId(1000L).quantity(1).build(),
                MesMdWorkstationMachineDO.builder().workstationId(101L).machineryId(1001L).quantity(1).build()));
        when(machineryMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesDvMachineryDO.builder().id(1000L).code("EQ-001").build(),
                MesDvMachineryDO.builder().id(1001L).code("EQ-002").build()));

        byte[] bytes = service.exportTemplate(10L);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals(1, workbook.getNumberOfSheets());
            assertEquals("工序模板", workbook.getSheetAt(0).getSheetName());
            assertEquals("路线编码", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("ROUTE-001", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            Row header = workbook.getSheetAt(0).getRow(4);
            assertEquals("工序名称", header.getCell(0).getStringCellValue());
            assertEquals("产能", header.getCell(1).getStringCellValue());
            assertEquals("设备编号", header.getCell(2).getStringCellValue());
            assertEquals("是否关键工序", header.getCell(3).getStringCellValue());
            assertEquals("裁切", workbook.getSheetAt(0).getRow(5).getCell(0).getStringCellValue());
            assertEquals("包装", workbook.getSheetAt(0).getRow(6).getCell(0).getStringCellValue());
            assertFalse(workbook.getSheetAt(0).getRow(4).toString().contains("批记录表单"));
            assertFalse(workbook.getSheetAt(0).getRow(4).toString().contains("表单槽位"));
        }
    }

    @Test
    void importRebuild_replacesProcessGraphAndWritesActiveCapacity() throws Exception {
        arrangeImportMasters(true);
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(10L);
        graph.setGraphVersion(7L);
        when(routeProcessFlowService.getGraph(10L)).thenReturn(graph);
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(activeVersion());
        MesProRouteProcessFlowValidationRespVO saved = new MesProRouteProcessFlowValidationRespVO();
        saved.setValid(true);
        when(routeProcessFlowService.saveGraph(any(MesProRouteProcessFlowSaveReqVO.class))).thenReturn(saved);
        when(routeProcessMapper.updateById(any(MesProRouteProcessDO.class))).thenReturn(1);

        MesProRouteProcessTemplateImportResult result = service.importTemplate(
                workbookFile("工序名称", "产能", "设备编号", "是否关键工序",
                        new Object[][]{{"包装", 8, "EQ-002", "否"}, {"裁切", 12.5, "EQ-001", "是"}}),
                IMPORT_MODE_REBUILD);

        assertEquals(2, result.getRouteProcessCount());
        assertEquals(List.of("包装", "裁切"), result.getProcessNames());
        ArgumentCaptor<MesProRouteProcessFlowSaveReqVO> graphCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowSaveReqVO.class);
        verify(routeProcessFlowService).saveGraph(graphCaptor.capture());
        MesProRouteProcessFlowSaveReqVO graphRequest = graphCaptor.getValue();
        assertTrue(graphRequest.getRouteProcessDeletes().isEmpty());
        assertTrue(graphRequest.getRouteProcessCreates().isEmpty());
        assertEquals(1, graphRequest.getEdges().size());
        assertEquals(21L, graphRequest.getEdges().get(0).getSourceRouteProcessId());
        assertEquals(20L, graphRequest.getEdges().get(0).getTargetRouteProcessId());
        assertEquals(2, graphRequest.getBoundaryEdges().size());

        ArgumentCaptor<MesProRouteProcessDO> processCaptor = ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper, org.mockito.Mockito.times(2)).updateById(processCaptor.capture());
        assertTrue(processCaptor.getAllValues().stream().anyMatch(process -> Objects.equals(process.getId(), 21L)
                && Objects.equals(process.getSort(), 1) && Objects.equals(process.getWorkstationId(), 101L)));
        assertTrue(processCaptor.getAllValues().stream().anyMatch(process -> Objects.equals(process.getId(), 20L)
                && Objects.equals(process.getSort(), 2) && Objects.equals(process.getWorkstationId(), 100L)));

        verify(routeScheduleConfigMapper).deleteByRouteVersionId(500L);
        ArgumentCaptor<MesProRouteScheduleConfigDO> configCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper, org.mockito.Mockito.times(2)).insert(configCaptor.capture());
        assertEquals(List.of(new BigDecimal("8"), new BigDecimal("12.5")), configCaptor.getAllValues().stream()
                .map(MesProRouteScheduleConfigDO::getHourlyCapacity).toList());
        assertEquals(List.of(21L, 20L), configCaptor.getAllValues().stream()
                .map(MesProRouteScheduleConfigDO::getRouteProcessId).toList());
        verify(routeService).ensureDefaultScheduleArtifacts(10L, 21L);
        verify(routeService).ensureDefaultScheduleArtifacts(10L, 20L);
    }

    @Test
    void importUpgrade_updatesDraftSnapshotWithoutTouchingActiveScheduleRows() throws Exception {
        arrangeImportMasters(false);
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder().id(600L).routeId(10L)
                .versionNo("V2").active(false).lifecycleStatus("DRAFT").build();
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(activeVersion());
        when(routeVersionWorkflowService.createCandidate(any())).thenReturn(candidate);
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(10L);
        graph.setGraphVersion(3L);
        graph.setNodes(List.of(
                node(20L, 30L, 100L, 1), node(21L, 31L, 101L, 2)));
        when(routeProcessFlowService.getGraph(10L, 600L)).thenReturn(graph);
        MesProRouteProcessFlowValidationRespVO saved = new MesProRouteProcessFlowValidationRespVO();
        saved.setValid(true);
        when(routeProcessFlowService.saveGraph(any(MesProRouteProcessFlowSaveReqVO.class))).thenReturn(saved);

        MesProRouteProcessTemplateImportResult result = service.importTemplate(
                workbookFile("工序名称", "产能", "设备编号", "是否关键工序",
                        new Object[][]{{"裁切", 12.5, "EQ-001", "是"}, {"包装", 8, "EQ-002", "否"}}),
                IMPORT_MODE_UPGRADE);

        assertEquals(600L, result.getRouteVersionId());
        assertEquals("V2", result.getRouteVersionNo());
        ArgumentCaptor<MesProRouteProcessFlowSaveReqVO> graphCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowSaveReqVO.class);
        verify(routeProcessFlowService).saveGraph(graphCaptor.capture());
        assertEquals(600L, graphCaptor.getValue().getRouteVersionId());
        assertEquals(List.of(20L, 21L), graphCaptor.getValue().getRouteProcessDeletes());
        verify(routeCandidateConfigService).saveConfigSnapshot(eq(600L), eq("scheduleConfigs"), any());
        org.mockito.Mockito.verify(routeScheduleConfigMapper, org.mockito.Mockito.never())
                .deleteByRouteVersionId(any());
    }

    @Test
    void import_rejectsForbiddenConfigurationColumns() throws Exception {
        assertThrows(RuntimeException.class, () -> service.importTemplate(
                workbookFile("工序名称", "产能", "设备编号", "批记录表单",
                        new Object[][]{{"裁切", 12.5, "EQ-001", "批记录"}}),
                IMPORT_MODE_REBUILD));
        org.mockito.Mockito.verifyNoInteractions(routeProcessFlowService);
    }

    @Test
    void importRebuild_rejectsRemovingProcessWithBatchConfiguration() throws Exception {
        arrangeImportMasters(true);
        MesProRouteProcessDO batchConfigured = routeProcess(22L, 32L, 102L, 3, false);
        batchConfigured.setBatchRecordReportId("BR-001");
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(
                routeProcess(20L, 30L, 100L, 1, true),
                routeProcess(21L, 31L, 101L, 2, false),
                batchConfigured));
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(10L);
        graph.setGraphVersion(7L);
        when(routeProcessFlowService.getGraph(10L)).thenReturn(graph);
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(activeVersion());

        assertThrows(RuntimeException.class, () -> service.importTemplate(
                workbookFile("工序名称", "产能", "设备编号", "是否关键工序",
                        new Object[][]{{"裁切", 12.5, "EQ-001", "是"}, {"包装", 8, "EQ-002", "否"}}),
                IMPORT_MODE_REBUILD));

        org.mockito.Mockito.verify(routeProcessFlowService, org.mockito.Mockito.never()).saveGraph(any());
        org.mockito.Mockito.verify(routeProcessMapper, org.mockito.Mockito.never()).deleteById(any());
    }

    private void arrangeImportMasters(boolean rebuild) {
        MesProRouteDO route = route();
        when(routeMapper.selectByCode("ROUTE-001")).thenReturn(route);
        when(processMapper.selectByName("裁切")).thenReturn(
                MesProProcessDO.builder().id(30L).name("裁切").status(0).build());
        when(processMapper.selectByName("包装")).thenReturn(
                MesProProcessDO.builder().id(31L).name("包装").status(0).build());
        when(machineryMapper.selectListByCodes(anyCollection())).thenReturn(List.of(
                MesDvMachineryDO.builder().id(1000L).code("EQ-001").status(0).build(),
                MesDvMachineryDO.builder().id(1001L).code("EQ-002").status(0).build()));
        when(workstationMachineMapper.selectListByMachineryIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationMachineDO.builder().workstationId(100L).machineryId(1000L).quantity(1).build(),
                MesMdWorkstationMachineDO.builder().workstationId(101L).machineryId(1001L).quantity(1).build()));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(100L).processId(30L).status(0).build(),
                MesMdWorkstationDO.builder().id(101L).processId(31L).status(0).build()));
        if (rebuild) {
            when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(
                    routeProcess(20L, 30L, 100L, 1, true), routeProcess(21L, 31L, 101L, 2, false)));
            doNothing().when(routeService).validateRouteNotEnable(10L);
        }
    }

    private MesProRouteDO route() {
        return MesProRouteDO.builder().id(10L).code("ROUTE-001").name("标准路线").status(1).build();
    }

    private MesProRouteVersionDO activeVersion() {
        return MesProRouteVersionDO.builder().id(500L).routeId(10L).versionNo("V1")
                .active(true).lifecycleStatus("ACTIVE").build();
    }

    private MesProRouteProcessDO routeProcess(Long id, Long processId, Long workstationId,
                                              int sort, boolean keyFlag) {
        return MesProRouteProcessDO.builder().id(id).routeId(10L).processId(processId)
                .workstationId(workstationId).sort(sort).keyFlag(keyFlag).checkFlag(false).build();
    }

    private cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowNodeRespVO node(
            Long routeProcessId, Long processId, Long workstationId, int sort) {
        cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowNodeRespVO node =
                new cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowNodeRespVO();
        node.setRouteProcessId(routeProcessId);
        node.setProcessId(processId);
        node.setRouteProcessWorkstationId(workstationId);
        node.setSort(sort);
        return node;
    }

    private MockMultipartFile workbookFile(String h1, String h2, String h3, String h4,
                                            Object[][] rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("工序模板");
            sheet.createRow(0).createCell(0).setCellValue("路线编码");
            sheet.getRow(0).createCell(1).setCellValue("ROUTE-001");
            sheet.createRow(1).createCell(0).setCellValue("路线名称");
            sheet.getRow(1).createCell(1).setCellValue("标准路线");
            sheet.createRow(2).createCell(0).setCellValue("填写说明");
            sheet.getRow(2).createCell(1).setCellValue("按行顺序填写工序基础配置");
            Row header = sheet.createRow(4);
            header.createCell(0).setCellValue(h1);
            header.createCell(1).setCellValue(h2);
            header.createCell(2).setCellValue(h3);
            header.createCell(3).setCellValue(h4);
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(5 + rowIndex);
                for (int columnIndex = 0; columnIndex < rows[rowIndex].length; columnIndex++) {
                    Object value = rows[rowIndex][columnIndex];
                    if (value instanceof Number number) {
                        row.createCell(columnIndex).setCellValue(number.doubleValue());
                    } else {
                        row.createCell(columnIndex).setCellValue(String.valueOf(value));
                    }
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", "process-template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
