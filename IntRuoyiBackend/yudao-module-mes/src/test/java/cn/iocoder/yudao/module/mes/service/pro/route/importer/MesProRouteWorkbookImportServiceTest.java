package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.productbom.MesProRouteProductBomSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
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
    void importWorkbook_createsBaseRouteChildrenWithoutResourceOrBatchBindingSheets() throws Exception {
        when(routeMapper.selectByCode("ROUTE-NEW")).thenReturn(null);
        when(processMapper.selectByCode("PROC-001"))
                .thenReturn(MesProProcessDO.builder().id(30L).code("PROC-001").name("裁切").build());
        when(itemMapper.selectByCode("ITEM-P"))
                .thenReturn(MesMdItemDO.builder().id(40L).code("ITEM-P").name("产品").build());
        when(itemMapper.selectByCode("ITEM-B"))
                .thenReturn(MesMdItemDO.builder().id(41L).code("ITEM-B").name("BOM物料").build());
        when(routeService.createRoute(any(MesProRouteSaveReqVO.class))).thenReturn(100L);
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
    void importWorkbook_rejectsMultipleIncomingBeforeAnyWrite() throws Exception {
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
                "multiple-incoming.xlsx", List.of(
                        List.of("ROUTE-NEW", "PROC-A", "PROC-C", "NORMAL"),
                        List.of("ROUTE-NEW", "PROC-B", "PROC-C", "NORMAL"),
                        List.of("ROUTE-NEW", "PROC-C", "PROC-D", "NORMAL")))));

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
            workbook.write(outputStream);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MockMultipartFile branchWorkbookFile(String filename, List<List<String>> flowRows) throws Exception {
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
            sheet(workbook, "产品绑定", List.of("路线编码", "产品编码", "产品名称", "规格", "生产数量",
                    "生产用时", "时间单位", "备注"), List.of());
            sheet(workbook, "工序BOM", List.of("路线编码", "工序编码", "产品编码", "BOM物料编码",
                    "BOM物料名称", "规格", "用料比例", "备注"), List.of());
            workbook.write(outputStream);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
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
