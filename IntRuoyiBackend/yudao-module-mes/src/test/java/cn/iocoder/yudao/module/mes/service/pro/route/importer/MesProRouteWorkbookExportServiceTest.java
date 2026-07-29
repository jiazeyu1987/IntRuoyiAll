package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowBoundaryEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowLayoutDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMachineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationWorkerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowLayoutMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteWorkbookExportServiceTest {

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private MesProRouteProcessFlowBoundaryEdgeMapper routeProcessFlowBoundaryEdgeMapper;
    @Mock
    private MesProRouteProcessFlowLayoutMapper routeProcessFlowLayoutMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Mock
    private MesMdWorkstationWorkerMapper workstationWorkerMapper;
    @Mock
    private MesDvMachineryMapper machineryMapper;
    @Mock
    private MesDvMachineryProcessMapper machineryProcessMapper;

    @InjectMocks
    private MesProRouteWorkbookExportServiceImpl exportService;

    @Test
    void exportWorkbook_ignoresListFiltersAndWritesFullRouteDataSheets() throws Exception {
        MesProRouteDO route = MesProRouteDO.builder()
                .id(10L).code("ROUTE-ALL").name("全量路线").status(0).build();
        MesProRoutePageReqVO reqVO = new MesProRoutePageReqVO();
        reqVO.setCode("SCREEN-FILTER");
        when(routeMapper.selectPage(any(MesProRoutePageReqVO.class))).thenAnswer(invocation -> {
            MesProRoutePageReqVO actual = invocation.getArgument(0);
            assertNull(actual.getCode());
            assertNull(actual.getName());
            assertNull(actual.getStatus());
            return new PageResult<>(List.of(route), 1L);
        });
        when(routeProcessMapper.selectListByRouteIds(eq(Set.of(10L)))).thenReturn(List.of(routeProcess(20L, 30L, 1)));
        when(routeProcessFlowEdgeMapper.selectListByRouteId(10L)).thenReturn(List.of(edge(20L, 20L, 1)));
        when(routeProcessFlowBoundaryEdgeMapper.selectListByRouteId(10L)).thenReturn(List.of(
                MesProRouteProcessFlowBoundaryEdgeDO.builder()
                        .routeId(10L).routeProcessId(20L).boundaryType("START").sort(1).build()));
        when(routeProcessFlowLayoutMapper.selectListByRouteId(10L)).thenReturn(List.of(
                MesProRouteProcessFlowLayoutDO.builder()
                        .routeId(10L).routeProcessId(20L).x(10).y(20).width(180).height(80).build()));
        when(routeVersionMapper.selectListByRouteIds(eq(Set.of(10L)))).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(500L).routeId(10L).versionNo("V21")
                        .active(true).lifecycleStatus("ACTIVE").build()));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(500L)).thenReturn(List.of(
                MesProRouteScheduleConfigDO.builder().routeVersionId(500L).routeProcessId(20L)
                        .capacityMode("RESOURCE_CALCULATED").configVersion("S1").build()));
        when(routeFlowConfigMapper.selectListByRouteIds(eq(Set.of(10L)))).thenReturn(List.of(
                MesProRouteFlowConfigDO.builder().id(600L).routeId(10L).useType("BATCH")
                        .enabled(true).configVersion("B1").build()));
        when(routeFlowProcessConfigMapper.selectListByRouteIds(eq(Set.of(10L)))).thenReturn(List.of(
                MesProRouteFlowProcessConfigDO.builder().id(700L).routeFlowConfigId(600L)
                        .routeId(10L).routeProcessId(20L).useType("BATCH").enabled(true)
                        .executionMode("SEQUENTIAL").batchRecordReportId("BR-001").build()));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIds(eq(Set.of(10L)))).thenReturn(List.of(
                MesProRouteFlowProcessBatchRecordDO.builder().routeFlowProcessConfigId(700L)
                        .routeId(10L).routeProcessId(20L).useType("BATCH")
                        .batchRecordReportId("BR-001").formBindingKey("slot-a")
                        .formTemplateNameSnapshot("过程记录").reportSort(1).build()));
        when(routeProductMapper.selectListByRouteIds(eq(Set.of(10L)))).thenReturn(List.of());
        when(routeProductBomMapper.selectList(eq(10L), eq(null), eq(null))).thenReturn(List.of());
        when(processMapper.selectListByIds(eq(Set.of(30L)))).thenReturn(List.of(process(30L, "PROC-A")));

        byte[] bytes = exportService.exportWorkbook(reqVO);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertNotNull(workbook.getSheet("工艺路线"));
            assertNotNull(workbook.getSheet("路线工序"));
            assertNotNull(workbook.getSheet("流转关系"));
            assertNotNull(workbook.getSheet("边界关系"));
            assertNotNull(workbook.getSheet("流转布局"));
            assertNotNull(workbook.getSheet("产品绑定"));
            assertNotNull(workbook.getSheet("工序BOM"));
            assertNotNull(workbook.getSheet("路线排产配置"));
            assertNotNull(workbook.getSheet("流程用途配置"));
            assertNotNull(workbook.getSheet("工序用途配置"));
            assertNotNull(workbook.getSheet("工序表单绑定"));
            assertEquals("START", workbook.getSheet("边界关系").getRow(1).getCell(1).getStringCellValue());
            assertEquals("PROC-A", workbook.getSheet("流转布局").getRow(1).getCell(1).getStringCellValue());
            assertEquals("RESOURCE_CALCULATED", workbook.getSheet("路线排产配置").getRow(1).getCell(2).getStringCellValue());
            assertEquals("BATCH", workbook.getSheet("流程用途配置").getRow(1).getCell(1).getStringCellValue());
            assertEquals("BR-001", workbook.getSheet("工序表单绑定").getRow(1).getCell(3).getStringCellValue());
        }
    }

    @Test
    void exportWorkbook_writesBranchRelationshipsFromFlowEdges() throws Exception {
        MesProRouteDO route = MesProRouteDO.builder()
                .id(10L).code("ROUTE-001").name("标准路线").status(0)
                .description("路线说明").remark("[owner]张三[/owner]\n路线备注").build();
        List<MesProRouteProcessDO> routeProcesses = List.of(
                routeProcess(20L, 30L, 1),
                routeProcess(21L, 31L, 2),
                routeProcess(22L, 32L, 3),
                routeProcess(23L, 33L, 4));
        List<MesProProcessDO> processes = List.of(
                process(30L, "PROC-A"),
                process(31L, "PROC-B"),
                process(32L, "PROC-C"),
                process(33L, "PROC-D"));
        List<MesProRouteProcessFlowEdgeDO> flowEdges = List.of(
                edge(20L, 21L, 1),
                edge(20L, 22L, 2),
                edge(22L, 23L, 3));
        MesMdItemDO product = MesMdItemDO.builder()
                .id(40L).code("ITEM-P").name("产品").specification("P-SPEC").build();
        MesMdItemDO bom = MesMdItemDO.builder()
                .id(41L).code("ITEM-B").name("BOM物料").specification("B-SPEC").build();
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder()
                .id(50L).routeId(10L).itemId(40L).quantity(100)
                .productionTime(new BigDecimal("60.50")).timeUnitType("MINUTE").remark("产品备注").build();
        MesProRouteProductBomDO routeProductBom = MesProRouteProductBomDO.builder()
                .id(60L).routeId(10L).processId(30L).productId(40L).itemId(41L)
                .quantity(new BigDecimal("1.25")).remark("BOM备注").build();
        when(routeMapper.selectPage(any(MesProRoutePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(route), 1L));
        when(routeProcessMapper.selectListByRouteIds(eq(Set.of(10L)))).thenReturn(routeProcesses);
        when(routeProcessFlowEdgeMapper.selectListByRouteId(10L)).thenReturn(flowEdges);
        when(routeProductMapper.selectListByRouteIds(eq(Set.of(10L)))).thenReturn(List.of(routeProduct));
        when(routeProductBomMapper.selectList(eq(10L), eq(null), eq(null))).thenReturn(List.of(routeProductBom));
        when(processMapper.selectListByIds(eq(Set.of(30L, 31L, 32L, 33L)))).thenReturn(processes);
        when(itemMapper.selectListByIds(eq(Set.of(40L, 41L)))).thenReturn(List.of(product, bom));
        byte[] bytes = exportService.exportWorkbook(new MesProRoutePageReqVO());

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals(11, workbook.getNumberOfSheets());
            assertEquals("工艺路线", workbook.getSheetName(0));
            assertEquals("路线工序", workbook.getSheetName(1));
            assertEquals("流转关系", workbook.getSheetName(2));
            assertEquals("边界关系", workbook.getSheetName(3));
            assertEquals("流转布局", workbook.getSheetName(4));
            assertEquals("产品绑定", workbook.getSheetName(5));
            assertEquals("工序BOM", workbook.getSheetName(6));
            assertEquals("路线编码", workbook.getSheet("工艺路线").getRow(0).getCell(0).getStringCellValue());
            assertEquals("ROUTE-001", workbook.getSheet("工艺路线").getRow(1).getCell(0).getStringCellValue());
            assertEquals("PROC-A", workbook.getSheet("路线工序").getRow(1).getCell(2).getStringCellValue());
            assertEquals("源工序编码", workbook.getSheet("流转关系").getRow(0).getCell(1).getStringCellValue());
            assertEquals("PROC-A", workbook.getSheet("流转关系").getRow(1).getCell(1).getStringCellValue());
            assertEquals("PROC-B", workbook.getSheet("流转关系").getRow(1).getCell(2).getStringCellValue());
            assertEquals("PROC-A", workbook.getSheet("流转关系").getRow(2).getCell(1).getStringCellValue());
            assertEquals("PROC-C", workbook.getSheet("流转关系").getRow(2).getCell(2).getStringCellValue());
            assertEquals("PROC-C", workbook.getSheet("流转关系").getRow(3).getCell(1).getStringCellValue());
            assertEquals("PROC-D", workbook.getSheet("流转关系").getRow(3).getCell(2).getStringCellValue());
            assertEquals("ITEM-P", workbook.getSheet("产品绑定").getRow(1).getCell(1).getStringCellValue());
            assertEquals("ITEM-B", workbook.getSheet("工序BOM").getRow(1).getCell(3).getStringCellValue());
            assertEquals("备注", workbook.getSheet("路线工序").getRow(0).getCell(9).getStringCellValue());
        }
    }

    private MesProRouteProcessDO routeProcess(Long id, Long processId, int sort) {
        return MesProRouteProcessDO.builder()
                .id(id).routeId(10L).processId(processId).sort(sort).prepareTime(5)
                .waitTime(3).colorCode("#1677ff").keyFlag(sort == 1).checkFlag(false)
                .remark("工序备注").build();
    }

    private MesProProcessDO process(Long id, String code) {
        return MesProProcessDO.builder().id(id).code(code).name(code).build();
    }

    private MesProRouteProcessFlowEdgeDO edge(Long sourceId, Long targetId, int sort) {
        return MesProRouteProcessFlowEdgeDO.builder()
                .routeId(10L).sourceRouteProcessId(sourceId).targetRouteProcessId(targetId)
                .relationType("NORMAL").sort(sort).build();
    }
}
