package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_EXPORT_WORKBOOK_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.BOM_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.BOM_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.FLOW_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.FLOW_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.PROCESS_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.PROCESS_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.PRODUCT_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.PRODUCT_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.ROUTE_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.ROUTE_SHEET;

@Service
@Validated
public class MesProRouteWorkbookExportServiceImpl implements MesProRouteWorkbookExportService {

    private static final String OWNER_PREFIX = "[owner]";
    private static final String OWNER_SUFFIX = "[/owner]";

    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesMdItemMapper itemMapper;

    @Override
    public byte[] exportWorkbook(MesProRoutePageReqVO pageReqVO) {
        PageResult<MesProRouteDO> pageResult = routeMapper.selectPage(pageReqVO);
        List<MesProRouteDO> routes = pageResult.getList() == null ? Collections.emptyList() : pageResult.getList();
        WorkbookData data = loadWorkbookData(routes);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeRoutes(workbook, routes);
            writeProcesses(workbook, data);
            writeFlows(workbook, data);
            writeProducts(workbook, data);
            writeBoms(workbook, data);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw exception(PRO_ROUTE_EXPORT_WORKBOOK_FAILED, ex.getMessage());
        }
    }

    private WorkbookData loadWorkbookData(List<MesProRouteDO> routes) {
        if (routes.isEmpty()) {
            return new WorkbookData(routes, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        }
        Set<Long> routeIds = routes.stream().map(MesProRouteDO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProRouteProcessDO> routeProcesses = new ArrayList<>(safeList(routeProcessMapper.selectListByRouteIds(routeIds)));
        routeProcesses.sort(routeProcessComparator());
        List<MesProRouteProcessFlowEdgeDO> flowEdges = new ArrayList<>();
        for (Long routeId : routeIds) {
            flowEdges.addAll(safeList(routeProcessFlowEdgeMapper.selectListByRouteId(routeId)));
        }
        List<MesProRouteProductDO> routeProducts = new ArrayList<>(safeList(routeProductMapper.selectListByRouteIds(routeIds)));
        routeProducts.sort(Comparator.comparing(MesProRouteProductDO::getId, Comparator.nullsLast(Long::compareTo)));
        List<MesProRouteProductBomDO> routeProductBoms = new ArrayList<>();
        for (Long routeId : routeIds) {
            routeProductBoms.addAll(routeProductBomMapper.selectList(routeId, null, null));
        }
        routeProductBoms.sort(Comparator.comparing(MesProRouteProductBomDO::getId, Comparator.nullsLast(Long::compareTo)));

        Set<Long> processIds = new LinkedHashSet<>();
        routeProcesses.forEach(routeProcess -> addIfNotNull(processIds, routeProcess.getProcessId()));
        routeProductBoms.forEach(bom -> addIfNotNull(processIds, bom.getProcessId()));
        Map<Long, MesProProcessDO> processMap = processIds.isEmpty() ? Collections.emptyMap()
                : toMap(processMapper.selectListByIds(processIds), MesProProcessDO::getId);

        Set<Long> itemIds = new LinkedHashSet<>();
        routeProducts.forEach(product -> addIfNotNull(itemIds, product.getItemId()));
        routeProductBoms.forEach(bom -> {
            addIfNotNull(itemIds, bom.getProductId());
            addIfNotNull(itemIds, bom.getItemId());
        });
        Map<Long, MesMdItemDO> itemMap = itemIds.isEmpty() ? Collections.emptyMap()
                : toMap(itemMapper.selectListByIds(itemIds), MesMdItemDO::getId);
        return new WorkbookData(routes, routeProcesses, flowEdges, routeProducts, routeProductBoms, processMap, itemMap);
    }

    private void writeRoutes(Workbook workbook, List<MesProRouteDO> routes) {
        Sheet sheet = createSheet(workbook, ROUTE_SHEET, ROUTE_HEADERS);
        int rowIndex = 1;
        for (MesProRouteDO route : routes) {
            RouteRemarkParts remarkParts = splitRouteRemark(route.getRemark());
            writeRow(sheet.createRow(rowIndex++), route.getCode(), route.getName(), route.getStatus(),
                    remarkParts.ownerName(), route.getDescription(), remarkParts.visibleRemark());
        }
    }

    private void writeProcesses(Workbook workbook, WorkbookData data) {
        Sheet sheet = createSheet(workbook, PROCESS_SHEET, PROCESS_HEADERS);
        int rowIndex = 1;
        Map<Long, MesProRouteDO> routeMap = toMap(data.routes(), MesProRouteDO::getId);
        for (MesProRouteProcessDO routeProcess : data.routeProcesses()) {
            MesProRouteDO route = routeMap.get(routeProcess.getRouteId());
            MesProProcessDO process = data.processMap().get(routeProcess.getProcessId());
            writeRow(sheet.createRow(rowIndex++), routeCode(route), routeProcess.getSort(), code(process), name(process),
                    routeProcess.getPrepareTime(), routeProcess.getWaitTime(),
                    routeProcess.getColorCode(), routeProcess.getKeyFlag(), routeProcess.getCheckFlag(), routeProcess.getRemark());
        }
    }

    private void writeFlows(Workbook workbook, WorkbookData data) {
        Sheet sheet = createSheet(workbook, FLOW_SHEET, FLOW_HEADERS);
        int rowIndex = 1;
        Map<Long, MesProRouteDO> routeMap = toMap(data.routes(), MesProRouteDO::getId);
        Map<Long, MesProRouteProcessDO> routeProcessMap = toMap(data.routeProcesses(), MesProRouteProcessDO::getId);
        for (MesProRouteProcessFlowEdgeDO edge : data.flowEdges()) {
            MesProRouteProcessDO sourceRouteProcess = routeProcessMap.get(edge.getSourceRouteProcessId());
            MesProRouteProcessDO targetRouteProcess = routeProcessMap.get(edge.getTargetRouteProcessId());
            if (sourceRouteProcess == null || targetRouteProcess == null
                    || !sourceRouteProcess.getRouteId().equals(targetRouteProcess.getRouteId())
                    || !sourceRouteProcess.getRouteId().equals(edge.getRouteId())) {
                throw new IllegalStateException("流转关系引用了无效的路线工序");
            }
            MesProRouteDO route = routeMap.get(edge.getRouteId());
            MesProProcessDO sourceProcess = data.processMap().get(sourceRouteProcess.getProcessId());
            MesProProcessDO targetProcess = data.processMap().get(targetRouteProcess.getProcessId());
            if (route == null || sourceProcess == null || targetProcess == null) {
                throw new IllegalStateException("流转关系缺少路线或工序主数据");
            }
            writeRow(sheet.createRow(rowIndex++), route.getCode(), sourceProcess.getCode(),
                    targetProcess.getCode(), edge.getRelationType());
        }
    }

    private void writeProducts(Workbook workbook, WorkbookData data) {
        Sheet sheet = createSheet(workbook, PRODUCT_SHEET, PRODUCT_HEADERS);
        int rowIndex = 1;
        Map<Long, MesProRouteDO> routeMap = toMap(data.routes(), MesProRouteDO::getId);
        for (MesProRouteProductDO routeProduct : data.routeProducts()) {
            MesMdItemDO item = data.itemMap().get(routeProduct.getItemId());
            writeRow(sheet.createRow(rowIndex++), routeCode(routeMap.get(routeProduct.getRouteId())), code(item), name(item),
                    item == null ? null : item.getSpecification(), routeProduct.getQuantity(), routeProduct.getProductionTime(),
                    routeProduct.getTimeUnitType(), routeProduct.getRemark());
        }
    }

    private void writeBoms(Workbook workbook, WorkbookData data) {
        Sheet sheet = createSheet(workbook, BOM_SHEET, BOM_HEADERS);
        int rowIndex = 1;
        Map<Long, MesProRouteDO> routeMap = toMap(data.routes(), MesProRouteDO::getId);
        for (MesProRouteProductBomDO bom : data.routeProductBoms()) {
            MesProProcessDO process = data.processMap().get(bom.getProcessId());
            MesMdItemDO product = data.itemMap().get(bom.getProductId());
            MesMdItemDO item = data.itemMap().get(bom.getItemId());
            writeRow(sheet.createRow(rowIndex++), routeCode(routeMap.get(bom.getRouteId())), code(process), code(product),
                    code(item), name(item), item == null ? null : item.getSpecification(), bom.getQuantity(), bom.getRemark());
        }
    }

    private Sheet createSheet(Workbook workbook, String sheetName, List<String> headers) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeRow(sheet.createRow(0), headers.toArray());
        return sheet;
    }

    private void writeRow(Row row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value == null) {
                row.createCell(i).setCellValue("");
            } else if (value instanceof Number number) {
                row.createCell(i).setCellValue(number.doubleValue());
            } else if (value instanceof Boolean bool) {
                row.createCell(i).setCellValue(bool);
            } else {
                row.createCell(i).setCellValue(String.valueOf(value));
            }
        }
    }

    private Comparator<MesProRouteProcessDO> routeProcessComparator() {
        return Comparator.comparing(MesProRouteProcessDO::getRouteId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MesProRouteProcessDO::getId, Comparator.nullsLast(Long::compareTo));
    }

    private <T, K> Map<K, T> toMap(Collection<T> items, java.util.function.Function<T, K> keyGetter) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        return items.stream().filter(item -> keyGetter.apply(item) != null)
                .collect(Collectors.toMap(keyGetter, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

    private void addIfNotNull(Set<Long> values, Long value) {
        if (value != null) {
            values.add(value);
        }
    }

    private String routeCode(MesProRouteDO route) {
        return route == null ? null : route.getCode();
    }

    private String code(MesProProcessDO process) {
        return process == null ? null : process.getCode();
    }

    private String name(MesProProcessDO process) {
        return process == null ? null : process.getName();
    }

    private String code(MesMdItemDO item) {
        return item == null ? null : item.getCode();
    }

    private String name(MesMdItemDO item) {
        return item == null ? null : item.getName();
    }

    private RouteRemarkParts splitRouteRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            return new RouteRemarkParts(null, null);
        }
        String normalized = remark.trim();
        int prefixIndex = normalized.indexOf(OWNER_PREFIX);
        int suffixIndex = normalized.indexOf(OWNER_SUFFIX);
        if (prefixIndex >= 0 && suffixIndex > prefixIndex) {
            String ownerName = normalized.substring(prefixIndex + OWNER_PREFIX.length(), suffixIndex).trim();
            String visibleRemark = (normalized.substring(0, prefixIndex)
                    + normalized.substring(suffixIndex + OWNER_SUFFIX.length())).trim();
            return new RouteRemarkParts(blankToNull(ownerName), blankToNull(visibleRemark));
        }
        return new RouteRemarkParts(null, normalized);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record RouteRemarkParts(String ownerName, String visibleRemark) {
    }

    private record WorkbookData(List<MesProRouteDO> routes,
                                List<MesProRouteProcessDO> routeProcesses,
                                List<MesProRouteProcessFlowEdgeDO> flowEdges,
                                List<MesProRouteProductDO> routeProducts,
                                List<MesProRouteProductBomDO> routeProductBoms,
                                Map<Long, MesProProcessDO> processMap,
                                Map<Long, MesMdItemDO> itemMap) {
    }
}
