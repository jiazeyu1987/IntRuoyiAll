package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowBoundaryEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowLayoutReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.productbom.MesProRouteProductBomSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessFlowService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductBomService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteWorkbookImportServiceTest {

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesMdItemMapper itemMapper;
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
    private MesProRouteService routeService;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Mock
    private MesProRouteProductService routeProductService;
    @Mock
    private MesProRouteProductBomService routeProductBomService;

    @InjectMocks
    private MesProRouteWorkbookImportServiceImpl importService;

    @Test
    void importWorkbook_replaysFullRouteGraphAndConfigSheets() throws Exception {
        when(routeMapper.selectByCode("ROUTE-NEW")).thenReturn(null);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(MesProProcessDO.builder().id(30L).code("PROC-001").name("裁切").build());
        when(routeService.createRoute(any(MesProRouteSaveReqVO.class))).thenReturn(100L);
        when(routeProcessService.createRouteProcess(any(MesProRouteProcessSaveReqVO.class))).thenReturn(200L);
        when(routeVersionMapper.selectActiveByRouteId(100L))
                .thenReturn(MesProRouteVersionDO.builder().id(500L).routeId(100L).versionNo("V1").build());

        importService.importWorkbook(fullWorkbookFile("full-route.xlsx"));

        ArgumentCaptor<MesProRouteProcessFlowSaveReqVO> flowCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowSaveReqVO.class);
        verify(routeProcessFlowService).saveGraph(flowCaptor.capture());
        assertEquals(2, flowCaptor.getValue().getBoundaryEdges().size());
        MesProRouteProcessFlowBoundaryEdgeReqVO start = flowCaptor.getValue().getBoundaryEdges().get(0);
        assertEquals("START", start.getBoundaryType());
        assertEquals(200L, start.getRouteProcessId());
        MesProRouteProcessFlowLayoutReqVO layout = flowCaptor.getValue().getLayouts().get(0);
        assertEquals(200L, layout.getRouteProcessId());
        assertEquals(120, layout.getX());

        verify(routeScheduleConfigMapper).insert(argThat((MesProRouteScheduleConfigDO config) ->
                config.getRouteVersionId().equals(500L)
                        && config.getRouteProcessId().equals(200L)
                        && "RESOURCE_CALCULATED".equals(config.getCapacityMode())));
        verify(routeFlowConfigMapper).insert(argThat((MesProRouteFlowConfigDO config) ->
                config.getRouteId().equals(100L)
                        && "BATCH".equals(config.getUseType())
                        && Boolean.TRUE.equals(config.getEnabled())));
        verify(routeFlowProcessConfigMapper).insert(argThat((MesProRouteFlowProcessConfigDO config) ->
                config.getRouteId().equals(100L)
                        && config.getRouteProcessId().equals(200L)
                        && "BR-001".equals(config.getBatchRecordReportId())));
        verify(routeFlowProcessBatchRecordMapper).insert(argThat((MesProRouteFlowProcessBatchRecordDO binding) ->
                binding.getRouteId().equals(100L)
                        && configBindingMatches(binding)));
    }

    @Test
    void importWorkbook_createsBaseRouteChildrenWithoutResourceOrBatchBindingSheets() throws Exception {
        when(routeMapper.selectByCode("ROUTE-NEW")).thenReturn(null);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(MesProProcessDO.builder().id(30L).code("PROC-001").name("裁切").build());
        when(itemMapper.selectByCode("ITEM-P"))
                .thenReturn(MesMdItemDO.builder().id(40L).code("ITEM-P").name("产品").build());
        when(itemMapper.selectByCode("ITEM-B"))
                .thenReturn(MesMdItemDO.builder().id(41L).code("ITEM-B").name("BOM物料").build());
        when(routeService.createRoute(any(MesProRouteSaveReqVO.class))).thenReturn(100L);
        when(routeVersionMapper.selectActiveByRouteId(100L))
                .thenReturn(MesProRouteVersionDO.builder().id(500L).routeId(100L).versionNo("V1").build());
        when(routeProcessService.createRouteProcess(any(MesProRouteProcessSaveReqVO.class))).thenReturn(200L);
        when(routeProductService.createRouteProduct(any(MesProRouteProductSaveReqVO.class))).thenReturn(300L);
        when(routeProductBomService.createRouteProductBom(any(MesProRouteProductBomSaveReqVO.class))).thenReturn(400L);

        MesProRouteWorkbookImportResult result = importService.importWorkbook(workbookFile("route.xlsx"));

        assertEquals(1, result.getRouteCount());
        assertEquals(1, result.getRouteProcessCount());
        assertEquals(1, result.getRouteProductCount());
        assertEquals(1, result.getRouteProductBomCount());
        assertEquals(List.of("ROUTE-NEW"), result.getRouteCodes());

        ArgumentCaptor<MesProRouteSaveReqVO> routeCaptor = ArgumentCaptor.forClass(MesProRouteSaveReqVO.class);
        verify(routeService).createRoute(routeCaptor.capture());
        assertEquals("ROUTE-NEW", routeCaptor.getValue().getCode());
        assertEquals("标准路线", routeCaptor.getValue().getName());
        assertEquals("张三", routeCaptor.getValue().getOwnerName());

        ArgumentCaptor<MesProRouteProcessSaveReqVO> processCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessSaveReqVO.class);
        verify(routeProcessService).createRouteProcess(processCaptor.capture());
        assertEquals(100L, processCaptor.getValue().getRouteId());
        assertEquals(30L, processCaptor.getValue().getProcessId());
        assertEquals(1, processCaptor.getValue().getSort());
        assertEquals(true, processCaptor.getValue().getKeyFlag());

        ArgumentCaptor<MesProRouteProcessFlowSaveReqVO> flowCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowSaveReqVO.class);
        verify(routeProcessFlowService).saveGraph(flowCaptor.capture());
        assertEquals(100L, flowCaptor.getValue().getRouteId());
        assertEquals(0L, flowCaptor.getValue().getGraphVersion());
        assertEquals(List.of(), flowCaptor.getValue().getEdges());

        verify(routeService).updateRouteStatus(100L, CommonStatusEnum.ENABLE.getStatus());
    }

    @Test
    void importWorkbook_duplicateRouteCodeFailsBeforeAnyWrite() throws Exception {
        when(routeMapper.selectByCode("ROUTE-NEW")).thenReturn(MesProRouteDO.builder().id(9L).code("ROUTE-NEW").build());

        assertThrows(ServiceException.class, () -> importService.importWorkbook(workbookFile("duplicate.xlsx")));

        verify(routeService, never()).createRoute(any());
        verify(routeProcessService, never()).createRouteProcess(any());
        verify(routeProductService, never()).createRouteProduct(any());
        verify(routeProductBomService, never()).createRouteProductBom(any());
    }

    @Test
    void importWorkbook_missingProductMasterFailsBeforeAnyWrite() throws Exception {
        when(routeMapper.selectByCode("ROUTE-NEW")).thenReturn(null);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(MesProProcessDO.builder().id(30L).code("PROC-001").build());
        when(itemMapper.selectByCode("ITEM-P")).thenReturn(null);

        assertThrows(ServiceException.class, () -> importService.importWorkbook(workbookFile("missing-product.xlsx")));

        verify(routeService, never()).createRoute(any());
    }

    private MockMultipartFile workbookFile(String filename) throws Exception {
        return workbookFile(filename, "");
    }

    @Test
    void importWorkbook_preservesMultipleSuccessors() throws Exception {
        when(routeMapper.selectByCode("ROUTE-NEW")).thenReturn(null);
        when(processMapper.selectByCode("PROC-A"))
                .thenReturn(MesProProcessDO.builder().id(30L).code("PROC-A").build());
        when(processMapper.selectByCode("PROC-B"))
                .thenReturn(MesProProcessDO.builder().id(31L).code("PROC-B").build());
        when(processMapper.selectByCode("PROC-C"))
                .thenReturn(MesProProcessDO.builder().id(32L).code("PROC-C").build());
        when(processMapper.selectByCode("PROC-D"))
                .thenReturn(MesProProcessDO.builder().id(33L).code("PROC-D").build());
        when(routeService.createRoute(any(MesProRouteSaveReqVO.class))).thenReturn(100L);
        when(routeVersionMapper.selectActiveByRouteId(100L))
                .thenReturn(MesProRouteVersionDO.builder().id(500L).routeId(100L).versionNo("V1").build());
        when(routeProcessService.createRouteProcess(any(MesProRouteProcessSaveReqVO.class)))
                .thenReturn(201L, 202L, 203L, 204L);

        importService.importWorkbook(branchWorkbookFile("branch.xlsx", List.of(
                List.of("ROUTE-NEW", "PROC-A", "PROC-B", "NORMAL"),
                List.of("ROUTE-NEW", "PROC-A", "PROC-C", "NORMAL"),
                List.of("ROUTE-NEW", "PROC-C", "PROC-D", "NORMAL"))));

        ArgumentCaptor<MesProRouteProcessFlowSaveReqVO> flowCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowSaveReqVO.class);
        verify(routeProcessFlowService).saveGraph(flowCaptor.capture());
        assertEquals(3, flowCaptor.getValue().getEdges().size());
        assertEquals(201L, flowCaptor.getValue().getEdges().get(0).getSourceRouteProcessId());
        assertEquals(202L, flowCaptor.getValue().getEdges().get(0).getTargetRouteProcessId());
        assertEquals(201L, flowCaptor.getValue().getEdges().get(1).getSourceRouteProcessId());
        assertEquals(203L, flowCaptor.getValue().getEdges().get(1).getTargetRouteProcessId());
        assertEquals(203L, flowCaptor.getValue().getEdges().get(2).getSourceRouteProcessId());
        assertEquals(204L, flowCaptor.getValue().getEdges().get(2).getTargetRouteProcessId());
    }

    @Test
    void importWorkbook_preservesMultipleStartsAndIncomingJoin() throws Exception {
        when(routeMapper.selectByCode("ROUTE-NEW")).thenReturn(null);
        when(processMapper.selectByCode("PROC-A"))
                .thenReturn(MesProProcessDO.builder().id(30L).code("PROC-A").build());
        when(processMapper.selectByCode("PROC-B"))
                .thenReturn(MesProProcessDO.builder().id(31L).code("PROC-B").build());
        when(processMapper.selectByCode("PROC-C"))
                .thenReturn(MesProProcessDO.builder().id(32L).code("PROC-C").build());
        when(processMapper.selectByCode("PROC-D"))
                .thenReturn(MesProProcessDO.builder().id(33L).code("PROC-D").build());
        when(routeService.createRoute(any(MesProRouteSaveReqVO.class))).thenReturn(100L);
        when(routeVersionMapper.selectActiveByRouteId(100L))
                .thenReturn(MesProRouteVersionDO.builder().id(500L).routeId(100L).versionNo("V1").build());
        when(routeProcessService.createRouteProcess(any(MesProRouteProcessSaveReqVO.class)))
                .thenReturn(201L, 202L, 203L, 204L);

        importService.importWorkbook(branchWorkbookFile(
                "multiple-incoming.xlsx",
                List.of(
                        List.of("ROUTE-NEW", "PROC-A", "PROC-C", "NORMAL"),
                        List.of("ROUTE-NEW", "PROC-B", "PROC-C", "NORMAL"),
                        List.of("ROUTE-NEW", "PROC-C", "PROC-D", "NORMAL")),
                List.of(
                        List.of("ROUTE-NEW", "START", "PROC-A", "1"),
                        List.of("ROUTE-NEW", "START", "PROC-B", "2"),
                        List.of("ROUTE-NEW", "END", "PROC-D", "1"))));

        ArgumentCaptor<MesProRouteProcessFlowSaveReqVO> flowCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowSaveReqVO.class);
        verify(routeProcessFlowService).saveGraph(flowCaptor.capture());
        assertEquals(3, flowCaptor.getValue().getEdges().size());
        assertEquals(3, flowCaptor.getValue().getBoundaryEdges().size());
        assertEquals(201L, flowCaptor.getValue().getBoundaryEdges().get(0).getRouteProcessId());
        assertEquals(202L, flowCaptor.getValue().getBoundaryEdges().get(1).getRouteProcessId());
        assertEquals(204L, flowCaptor.getValue().getBoundaryEdges().get(2).getRouteProcessId());
    }

    @Test
    void importWorkbook_rejectsCyclicOrUnreachableFlowBeforeAnyWrite() throws Exception {
        when(routeMapper.selectByCode("ROUTE-NEW")).thenReturn(null);
        when(processMapper.selectByCode("PROC-A"))
                .thenReturn(MesProProcessDO.builder().id(30L).code("PROC-A").build());
        when(processMapper.selectByCode("PROC-B"))
                .thenReturn(MesProProcessDO.builder().id(31L).code("PROC-B").build());
        when(processMapper.selectByCode("PROC-C"))
                .thenReturn(MesProProcessDO.builder().id(32L).code("PROC-C").build());
        when(processMapper.selectByCode("PROC-D"))
                .thenReturn(MesProProcessDO.builder().id(33L).code("PROC-D").build());

        assertThrows(ServiceException.class, () -> importService.importWorkbook(branchWorkbookFile(
                "cycle.xlsx", List.of(
                        List.of("ROUTE-NEW", "PROC-A", "PROC-B", "NORMAL"),
                        List.of("ROUTE-NEW", "PROC-B", "PROC-C", "NORMAL"),
                        List.of("ROUTE-NEW", "PROC-C", "PROC-A", "NORMAL")))));

        verify(routeService, never()).createRoute(any());
        verify(routeProcessFlowService, never()).saveGraph(any());
    }

    private MockMultipartFile workbookFile(String filename, String ignored) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            sheet(workbook, "工艺路线", List.of("路线编码", "路线名称", "状态", "负责人", "说明", "备注"),
                    List.of("ROUTE-NEW", "标准路线", "0", "张三", "路线说明", "路线备注"));
            sheet(workbook, "路线工序", List.of("路线编码", "序号", "工序编码", "工序名称",
                            "准备时间", "等待时间", "颜色", "关键工序", "质检工序", "备注"),
                    List.of("ROUTE-NEW", "1", "PROC-001", "裁切", "5", "3", "#1677ff",
                            "true", "false", "工序备注"));
            sheet(workbook, "流转关系", List.of("路线编码", "源工序编码", "目标工序编码", "关系类型"), List.of());
            sheet(workbook, "产品绑定", List.of("路线编码", "产品编码", "产品名称", "规格", "生产数量",
                            "生产用时", "时间单位", "备注"),
                    List.of("ROUTE-NEW", "ITEM-P", "产品", "P-SPEC", "100", "60.5", "MINUTE", "产品备注"));
            sheet(workbook, "工序BOM", List.of("路线编码", "工序编码", "产品编码", "BOM物料编码",
                            "BOM物料名称", "规格", "用料比例", "备注"),
                    List.of("ROUTE-NEW", "PROC-001", "ITEM-P", "ITEM-B", "BOM物料", "B-SPEC", "1.25", "BOM备注"));
            emptyFullDataSheets(workbook);
            workbook.write(outputStream);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MockMultipartFile branchWorkbookFile(String filename, List<List<String>> flowRows) throws Exception {
        return branchWorkbookFile(filename, flowRows, List.of());
    }

    private MockMultipartFile branchWorkbookFile(String filename, List<List<String>> flowRows,
                                                 List<List<String>> boundaryRows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            sheet(workbook, "工艺路线", List.of("路线编码", "路线名称", "状态", "负责人", "说明", "备注"),
                    List.of("ROUTE-NEW", "分支路线", "1", "", "", ""));
            sheetRows(workbook, "路线工序", List.of("路线编码", "序号", "工序编码", "工序名称",
                            "准备时间", "等待时间", "颜色", "关键工序", "质检工序", "备注"),
                    List.of(
                            List.of("ROUTE-NEW", "1", "PROC-A", "A", "", "", "", "false", "false", ""),
                            List.of("ROUTE-NEW", "2", "PROC-B", "B", "", "", "", "false", "false", ""),
                            List.of("ROUTE-NEW", "3", "PROC-C", "C", "", "", "", "false", "false", ""),
                            List.of("ROUTE-NEW", "4", "PROC-D", "D", "", "", "", "false", "false", "")));
            sheetRows(workbook, "流转关系", List.of("路线编码", "源工序编码", "目标工序编码", "关系类型"), flowRows);
            sheetRows(workbook, "边界关系", List.of("路线编码", "边界类型", "工序编码", "序号"), boundaryRows);
            sheet(workbook, "产品绑定", List.of("路线编码", "产品编码", "产品名称", "规格", "生产数量",
                    "生产用时", "时间单位", "备注"), List.of());
            sheet(workbook, "工序BOM", List.of("路线编码", "工序编码", "产品编码", "BOM物料编码",
                    "BOM物料名称", "规格", "用料比例", "备注"), List.of());
            emptyFullDataSheetsExceptBoundary(workbook);
            workbook.write(outputStream);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MockMultipartFile fullWorkbookFile(String filename) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            sheet(workbook, "工艺路线", List.of("路线编码", "路线名称", "状态", "负责人", "说明", "备注"),
                    List.of("ROUTE-NEW", "全量路线", "0", "张三", "路线说明", "路线备注"));
            sheet(workbook, "路线工序", List.of("路线编码", "序号", "工序编码", "工序名称",
                            "准备时间", "等待时间", "颜色", "关键工序", "质检工序", "备注"),
                    List.of("ROUTE-NEW", "1", "PROC-001", "裁切", "5", "3", "#1677ff",
                            "true", "false", "工序备注"));
            sheet(workbook, "流转关系", List.of("路线编码", "源工序编码", "目标工序编码", "关系类型"), List.of());
            sheetRows(workbook, "边界关系", List.of("路线编码", "边界类型", "工序编码", "序号"),
                    List.of(
                            List.of("ROUTE-NEW", "START", "PROC-001", "1"),
                            List.of("ROUTE-NEW", "END", "PROC-001", "1")));
            sheetRows(workbook, "流转布局", List.of("路线编码", "工序编码", "横坐标", "纵坐标", "宽度", "高度"),
                    List.of(List.of("ROUTE-NEW", "PROC-001", "120", "240", "180", "80")));
            sheet(workbook, "产品绑定", List.of("路线编码", "产品编码", "产品名称", "规格", "生产数量",
                    "生产用时", "时间单位", "备注"), List.of());
            sheet(workbook, "工序BOM", List.of("路线编码", "工序编码", "产品编码", "BOM物料编码",
                    "BOM物料名称", "规格", "用料比例", "备注"), List.of());
            sheet(workbook, "路线排产配置", List.of("路线编码", "工序编码", "产能模式", "小时产能",
                            "无限产能数量系数", "无限产能基准分钟", "夜班启用", "日历规则ID", "配置版本", "备注"),
                    List.of("ROUTE-NEW", "PROC-001", "RESOURCE_CALCULATED", "", "", "", "true", "", "S1", "排产备注"));
            sheet(workbook, "流程用途配置", List.of("路线编码", "用途类型", "启用", "配置版本", "备注"),
                    List.of("ROUTE-NEW", "BATCH", "true", "B1", "批记录用途"));
            sheet(workbook, "工序用途配置", List.of("路线编码", "用途类型", "工序编码", "启用", "执行模式",
                            "生产数量系数", "批记录表单ID", "备注"),
                    List.of("ROUTE-NEW", "BATCH", "PROC-001", "true", "SEQUENTIAL", "1.5", "BR-001", "工序用途"));
            sheet(workbook, "工序表单绑定", List.of("路线编码", "用途类型", "工序编码", "批记录表单ID",
                            "批记录定义ID", "批记录版本ID", "表单槽位", "表单绑定Key", "表单模板ID",
                            "表单模板名称快照", "最后发布模板版本ID", "最后发布模板版本号", "实例范围",
                            "共享表单Key", "填写范围JSON", "记录分类", "校验档案", "记录本启用",
                            "权限范围ID", "记录分类快照Hash", "必填策略", "必填条件JSON",
                            "负责人角色Key", "归档可见性", "槽位配置快照Hash", "候选来源类型",
                            "候选来源ID", "候选来源名称", "报表序号", "备注"),
                    List.of("ROUTE-NEW", "BATCH", "PROC-001", "BR-001", "11", "12", "MAIN",
                            "slot-a", "21", "过程记录", "22", "V3", "ROUTE_PROCESS", "shared-a",
                            "{}", "BATCH_RECORD", "CONTROLLED_BATCH", "true", "31", "hash-a",
                            "ALWAYS", "{}", "QA", "ALL", "slot-hash", "ROLE", "1,2",
                            "生产,质量", "1", "绑定备注"));
            workbook.write(outputStream);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private void emptyFullDataSheets(Workbook workbook) {
        sheet(workbook, "边界关系", List.of("路线编码", "边界类型", "工序编码", "序号"), List.of());
        emptyFullDataSheetsExceptBoundary(workbook);
    }

    private void emptyFullDataSheetsExceptBoundary(Workbook workbook) {
        sheet(workbook, "流转布局", List.of("路线编码", "工序编码", "横坐标", "纵坐标", "宽度", "高度"), List.of());
        sheet(workbook, "路线排产配置", List.of("路线编码", "工序编码", "产能模式", "小时产能",
                "无限产能数量系数", "无限产能基准分钟", "夜班启用", "日历规则ID", "配置版本", "备注"), List.of());
        sheet(workbook, "流程用途配置", List.of("路线编码", "用途类型", "启用", "配置版本", "备注"), List.of());
        sheet(workbook, "工序用途配置", List.of("路线编码", "用途类型", "工序编码", "启用", "执行模式",
                "生产数量系数", "批记录表单ID", "备注"), List.of());
        sheet(workbook, "工序表单绑定", List.of("路线编码", "用途类型", "工序编码", "批记录表单ID",
                "批记录定义ID", "批记录版本ID", "表单槽位", "表单绑定Key", "表单模板ID",
                "表单模板名称快照", "最后发布模板版本ID", "最后发布模板版本号", "实例范围",
                "共享表单Key", "填写范围JSON", "记录分类", "校验档案", "记录本启用",
                "权限范围ID", "记录分类快照Hash", "必填策略", "必填条件JSON",
                "负责人角色Key", "归档可见性", "槽位配置快照Hash", "候选来源类型",
                "候选来源ID", "候选来源名称", "报表序号", "备注"), List.of());
    }

    private boolean configBindingMatches(MesProRouteFlowProcessBatchRecordDO binding) {
        return "BR-001".equals(binding.getBatchRecordReportId())
                && "MAIN".equals(binding.getFormSlotType())
                && "slot-a".equals(binding.getFormBindingKey())
                && "过程记录".equals(binding.getFormTemplateNameSnapshot());
    }

    private void sheet(Workbook workbook, String name, List<String> headers, List<String> values) {
        sheetRows(workbook, name, headers, values.isEmpty() ? List.of() : List.of(values));
    }

    private void sheetRows(Workbook workbook, String name, List<String> headers, List<List<String>> rows) {
        var sheet = workbook.createSheet(name);
        var headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            headerRow.createCell(i).setCellValue(headers.get(i));
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            var valueRow = sheet.createRow(rowIndex + 1);
            List<String> values = rows.get(rowIndex);
            for (int i = 0; i < values.size(); i++) {
                valueRow.createCell(i).setCellValue(values.get(i));
            }
        }
    }
}
