package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowBoundaryEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowEdgeReqVO;
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
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_FILE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_INVALID_EXCEL;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_ROUTE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_ROUTE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_WORKBOOK_CELL_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_WORKBOOK_HEADERS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_WORKBOOK_MASTER_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_WORKBOOK_SHEET_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_WORKBOOK_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.BATCH_RECORD_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.BATCH_RECORD_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.BOM_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.BOM_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.BOUNDARY_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.BOUNDARY_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.FLOW_CONFIG_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.FLOW_CONFIG_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.FLOW_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.FLOW_PROCESS_CONFIG_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.FLOW_PROCESS_CONFIG_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.FLOW_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.LAYOUT_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.LAYOUT_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.PROCESS_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.PROCESS_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.PRODUCT_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.PRODUCT_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.ROUTE_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.ROUTE_SHEET;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.SCHEDULE_CONFIG_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookConstants.SCHEDULE_CONFIG_SHEET;

@Service
@Validated
public class MesProRouteWorkbookImportServiceImpl implements MesProRouteWorkbookImportService {

    private static final List<String> ROUTE_FLOW_USE_TYPES = List.of("SCHEDULE", "BATCH");

    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Resource
    private MesProRouteProductService routeProductService;
    @Resource
    private MesProRouteProductBomService routeProductBomService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteWorkbookImportResult importWorkbook(MultipartFile file) {
        validateFile(file);
        ParsedWorkbook workbook = parse(file);
        ResolvedWorkbook resolved = resolveAndValidate(workbook);

        Map<String, Long> routeIdByCode = new LinkedHashMap<>();
        Map<String, Long> activeRouteVersionIdByRouteCode = new HashMap<>();
        Map<String, Long> processIdByCode = new HashMap<>();
        Map<String, Long> routeProcessIdByKey = new HashMap<>();
        Map<String, Long> flowConfigIdByKey = new HashMap<>();
        Map<String, Long> flowProcessConfigIdByKey = new HashMap<>();
        for (RouteProcessRow processRow : workbook.processes()) {
            processIdByCode.put(processRow.processCode(), resolved.processByCode().get(processRow.processCode()).getId());
        }

        for (RouteRow routeRow : workbook.routes()) {
            MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
            reqVO.setCode(routeRow.code());
            reqVO.setName(routeRow.name());
            reqVO.setOwnerName(routeRow.ownerName());
            reqVO.setDescription(routeRow.description());
            reqVO.setRemark(routeRow.remark());
            Long routeId = routeService.createRoute(reqVO);
            routeIdByCode.put(routeRow.code(), routeId);
            MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(routeId);
            if (activeVersion == null || activeVersion.getId() == null) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "路线 " + routeRow.code() + " 创建后缺少当前生效版本");
            }
            activeRouteVersionIdByRouteCode.put(routeRow.code(), activeVersion.getId());
        }
        for (RouteProcessRow row : workbook.processes()) {
            MesProRouteProcessSaveReqVO reqVO = new MesProRouteProcessSaveReqVO();
            reqVO.setRouteId(routeIdByCode.get(row.routeCode()));
            reqVO.setProcessId(processIdByCode.get(row.processCode()));
            reqVO.setSort(row.sort());
            reqVO.setPrepareTime(row.prepareTime());
            reqVO.setWaitTime(row.waitTime());
            reqVO.setColorCode(row.colorCode());
            reqVO.setKeyFlag(row.keyFlag());
            reqVO.setCheckFlag(row.checkFlag());
            reqVO.setRemark(row.remark());
            Long routeProcessId = routeProcessService.createRouteProcess(reqVO);
            routeProcessIdByKey.put(routeProcessKey(row.routeCode(), row.processCode()), routeProcessId);
        }
        for (RouteRow routeRow : workbook.routes()) {
            MesProRouteProcessFlowSaveReqVO reqVO = new MesProRouteProcessFlowSaveReqVO();
            reqVO.setRouteId(routeIdByCode.get(routeRow.code()));
            reqVO.setGraphVersion(0L);
            reqVO.setEdges(workbook.flows().stream()
                    .filter(row -> row.routeCode().equals(routeRow.code()))
                    .map(row -> {
                        MesProRouteProcessFlowEdgeReqVO edge = new MesProRouteProcessFlowEdgeReqVO();
                        edge.setSourceRouteProcessId(routeProcessIdByKey.get(
                                routeProcessKey(row.routeCode(), row.sourceProcessCode())));
                        edge.setTargetRouteProcessId(routeProcessIdByKey.get(
                                routeProcessKey(row.routeCode(), row.targetProcessCode())));
                        edge.setRelationType(row.relationType());
                        return edge;
                    }).toList());
            reqVO.setBoundaryEdges(workbook.boundaries().stream()
                    .filter(row -> row.routeCode().equals(routeRow.code()))
                    .map(row -> {
                        MesProRouteProcessFlowBoundaryEdgeReqVO boundaryEdge =
                                new MesProRouteProcessFlowBoundaryEdgeReqVO();
                        boundaryEdge.setBoundaryType(row.boundaryType());
                        boundaryEdge.setRouteProcessId(routeProcessIdByKey.get(
                                routeProcessKey(row.routeCode(), row.processCode())));
                        boundaryEdge.setSort(row.sort());
                        return boundaryEdge;
                    }).toList());
            reqVO.setLayouts(workbook.layouts().stream()
                    .filter(row -> row.routeCode().equals(routeRow.code()))
                    .map(row -> {
                        MesProRouteProcessFlowLayoutReqVO layout = new MesProRouteProcessFlowLayoutReqVO();
                        layout.setRouteProcessId(routeProcessIdByKey.get(
                                routeProcessKey(row.routeCode(), row.processCode())));
                        layout.setX(row.x());
                        layout.setY(row.y());
                        layout.setWidth(row.width());
                        layout.setHeight(row.height());
                        return layout;
                    }).toList());
            routeProcessFlowService.saveGraph(reqVO);
        }
        for (RouteProductRow row : workbook.products()) {
            MesProRouteProductSaveReqVO reqVO = new MesProRouteProductSaveReqVO();
            reqVO.setRouteId(routeIdByCode.get(row.routeCode()));
            reqVO.setItemId(resolved.itemByCode().get(row.productCode()).getId());
            reqVO.setQuantity(row.quantity());
            reqVO.setProductionTime(row.productionTime());
            reqVO.setTimeUnitType(row.timeUnitType());
            reqVO.setRemark(row.remark());
            routeProductService.createRouteProduct(reqVO);
        }
        for (RouteBomRow row : workbook.boms()) {
            MesProRouteProductBomSaveReqVO reqVO = new MesProRouteProductBomSaveReqVO();
            reqVO.setRouteId(routeIdByCode.get(row.routeCode()));
            reqVO.setProcessId(resolved.processByCode().get(row.processCode()).getId());
            reqVO.setProductId(resolved.itemByCode().get(row.productCode()).getId());
            reqVO.setItemId(resolved.itemByCode().get(row.bomItemCode()).getId());
            reqVO.setQuantity(row.quantity());
            reqVO.setRemark(row.remark());
            routeProductBomService.createRouteProductBom(reqVO);
        }
        clearGeneratedDefaultConfigs(routeIdByCode, activeRouteVersionIdByRouteCode);
        for (RouteScheduleConfigRow row : workbook.scheduleConfigs()) {
            routeScheduleConfigMapper.insert(MesProRouteScheduleConfigDO.builder()
                    .routeVersionId(activeRouteVersionIdByRouteCode.get(row.routeCode()))
                    .routeProcessId(routeProcessIdByKey.get(routeProcessKey(row.routeCode(), row.processCode())))
                    .capacityMode(row.capacityMode())
                    .hourlyCapacity(row.hourlyCapacity())
                    .infiniteDurationQuantityFactor(row.infiniteDurationQuantityFactor())
                    .infiniteDurationBaseMinutes(row.infiniteDurationBaseMinutes())
                    .nightShiftEnabled(row.nightShiftEnabled())
                    .calendarRuleId(row.calendarRuleId())
                    .configVersion(row.configVersion())
                    .remark(row.remark())
                    .build());
        }
        for (RouteFlowConfigRow row : workbook.flowConfigs()) {
            MesProRouteFlowConfigDO config = MesProRouteFlowConfigDO.builder()
                    .routeId(routeIdByCode.get(row.routeCode()))
                    .useType(row.useType())
                    .enabled(row.enabled())
                    .configVersion(row.configVersion())
                    .remark(row.remark())
                    .build();
            routeFlowConfigMapper.insert(config);
            flowConfigIdByKey.put(routeUseKey(row.routeCode(), row.useType()), config.getId());
        }
        for (RouteFlowProcessConfigRow row : workbook.flowProcessConfigs()) {
            String routeUseKey = routeUseKey(row.routeCode(), row.useType());
            if (!flowConfigIdByKey.containsKey(routeUseKey)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "工序用途配置缺少流程用途配置：" + row.routeCode() + " " + row.useType());
            }
            MesProRouteFlowProcessConfigDO config = MesProRouteFlowProcessConfigDO.builder()
                    .routeFlowConfigId(flowConfigIdByKey.get(routeUseKey))
                    .routeId(routeIdByCode.get(row.routeCode()))
                    .routeProcessId(routeProcessIdByKey.get(routeProcessKey(row.routeCode(), row.processCode())))
                    .useType(row.useType())
                    .enabled(row.enabled())
                    .executionMode(row.executionMode())
                    .productionQuantityFactor(row.productionQuantityFactor())
                    .batchRecordReportId(row.batchRecordReportId())
                    .remark(row.remark())
                    .build();
            routeFlowProcessConfigMapper.insert(config);
            flowProcessConfigIdByKey.put(routeUseProcessKey(row.routeCode(), row.useType(), row.processCode()),
                    config.getId());
        }
        for (RouteBatchRecordRow row : workbook.batchRecords()) {
            String useProcessKey = routeUseProcessKey(row.routeCode(), row.useType(), row.processCode());
            if (!flowProcessConfigIdByKey.containsKey(useProcessKey)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "工序表单绑定缺少工序用途配置：" + row.routeCode() + " " + row.useType()
                                + " " + row.processCode());
            }
            routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                    .routeFlowProcessConfigId(flowProcessConfigIdByKey.get(useProcessKey))
                    .routeId(routeIdByCode.get(row.routeCode()))
                    .routeProcessId(routeProcessIdByKey.get(routeProcessKey(row.routeCode(), row.processCode())))
                    .useType(row.useType())
                    .batchRecordReportId(row.batchRecordReportId())
                    .batchRecordDefinitionId(row.batchRecordDefinitionId())
                    .batchRecordVersionId(row.batchRecordVersionId())
                    .formSlotType(row.formSlotType())
                    .formBindingKey(row.formBindingKey())
                    .formTemplateId(row.formTemplateId())
                    .formTemplateNameSnapshot(row.formTemplateNameSnapshot())
                    .lastPublishedTemplateVersionId(row.lastPublishedTemplateVersionId())
                    .lastPublishedTemplateVersionNo(row.lastPublishedTemplateVersionNo())
                    .instanceScope(row.instanceScope())
                    .sharedFormKey(row.sharedFormKey())
                    .fillableScopeJson(row.fillableScopeJson())
                    .recordCategory(row.recordCategory())
                    .validationProfile(row.validationProfile())
                    .recordbookEnabled(row.recordbookEnabled())
                    .permissionScopeId(row.permissionScopeId())
                    .recordCategorySnapshotHash(row.recordCategorySnapshotHash())
                    .requiredPolicy(row.requiredPolicy())
                    .requiredConditionJson(row.requiredConditionJson())
                    .ownerRoleKey(row.ownerRoleKey())
                    .archiveVisibility(row.archiveVisibility())
                    .slotConfigSnapshotHash(row.slotConfigSnapshotHash())
                    .candidateSourceType(row.candidateSourceType())
                    .candidateSourceIds(row.candidateSourceIds())
                    .candidateSourceNames(row.candidateSourceNames())
                    .reportSort(row.reportSort())
                    .remark(row.remark())
                    .build());
        }
        for (RouteRow routeRow : workbook.routes()) {
            if (CommonStatusEnum.ENABLE.getStatus().equals(routeRow.status())) {
                routeService.updateRouteStatus(routeIdByCode.get(routeRow.code()), routeRow.status());
            }
        }

        MesProRouteWorkbookImportResult result = new MesProRouteWorkbookImportResult();
        result.setRouteCount(workbook.routes().size());
        result.setRouteProcessCount(workbook.processes().size());
        result.setRouteProductCount(workbook.products().size());
        result.setRouteProductBomCount(workbook.boms().size());
        result.setRouteCodes(new ArrayList<>(routeIdByCode.keySet()));
        return result;
    }

    private void clearGeneratedDefaultConfigs(Map<String, Long> routeIdByCode,
                                              Map<String, Long> activeRouteVersionIdByRouteCode) {
        for (Long activeRouteVersionId : activeRouteVersionIdByRouteCode.values()) {
            routeScheduleConfigMapper.deleteByRouteVersionId(activeRouteVersionId);
        }
        for (Long routeId : routeIdByCode.values()) {
            for (String useType : ROUTE_FLOW_USE_TYPES) {
                routeFlowProcessBatchRecordMapper.deleteByRouteIdAndUseType(routeId, useType);
                routeFlowProcessConfigMapper.deleteByRouteIdAndUseType(routeId, useType);
                routeFlowConfigMapper.deleteByRouteIdAndUseType(routeId, useType);
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(PRO_ROUTE_IMPORT_FILE_EMPTY);
        }
    }

    private ParsedWorkbook parse(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            return new ParsedWorkbook(
                    parseRoutes(requiredSheet(workbook, ROUTE_SHEET), formatter),
                    parseProcesses(requiredSheet(workbook, PROCESS_SHEET), formatter),
                    parseFlows(requiredSheet(workbook, FLOW_SHEET), formatter),
                    parseBoundaries(requiredSheet(workbook, BOUNDARY_SHEET), formatter),
                    parseLayouts(requiredSheet(workbook, LAYOUT_SHEET), formatter),
                    parseProducts(requiredSheet(workbook, PRODUCT_SHEET), formatter),
                    parseBoms(requiredSheet(workbook, BOM_SHEET), formatter),
                    parseScheduleConfigs(requiredSheet(workbook, SCHEDULE_CONFIG_SHEET), formatter),
                    parseFlowConfigs(requiredSheet(workbook, FLOW_CONFIG_SHEET), formatter),
                    parseFlowProcessConfigs(requiredSheet(workbook, FLOW_PROCESS_CONFIG_SHEET), formatter),
                    parseBatchRecords(requiredSheet(workbook, BATCH_RECORD_SHEET), formatter));
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(PRO_ROUTE_IMPORT_INVALID_EXCEL);
        }
    }

    private Sheet requiredSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_SHEET_MISSING, sheetName);
        }
        return sheet;
    }

    private List<RouteRow> parseRoutes(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, ROUTE_SHEET, ROUTE_HEADERS, formatter);
        List<RouteRow> rows = new ArrayList<>();
        Set<String> routeCodes = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, ROUTE_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            if (!routeCodes.add(routeCode)) {
                throw exception(PRO_ROUTE_IMPORT_ROUTE_DUPLICATE, routeCode);
            }
            Integer status = parseStatus(requiredCell(sheet, row, 2, "状态", formatter));
            rows.add(new RouteRow(routeCode, requiredCell(sheet, row, 1, "路线名称", formatter), status,
                    cell(row, 3, formatter), cell(row, 4, formatter), cell(row, 5, formatter)));
        }
        return rows;
    }

    private List<RouteProcessRow> parseProcesses(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, PROCESS_SHEET, PROCESS_HEADERS, formatter);
        List<RouteProcessRow> rows = new ArrayList<>();
        Set<String> routeSortKeys = new LinkedHashSet<>();
        Set<String> routeProcessKeys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, PROCESS_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            Integer sort = parseInteger(requiredCell(sheet, row, 1, "序号", formatter), PROCESS_SHEET, row);
            String processCode = requiredCell(sheet, row, 2, "工序编码", formatter);
            if (!routeSortKeys.add(routeCode + ":" + sort)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, routeCode + " 工序序号 " + sort);
            }
            if (!routeProcessKeys.add(routeCode + ":" + processCode)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, routeCode + " 工序 " + processCode);
            }
            rows.add(new RouteProcessRow(routeCode, sort, processCode,
                    parseIntegerOrNull(cell(row, 4, formatter), PROCESS_SHEET, row),
                    parseIntegerOrNull(cell(row, 5, formatter), PROCESS_SHEET, row),
                    cell(row, 6, formatter), parseBoolean(cell(row, 7, formatter)),
                    parseBoolean(cell(row, 8, formatter)), cell(row, 9, formatter)));
        }
        return rows;
    }

    private List<RouteFlowRow> parseFlows(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, FLOW_SHEET, FLOW_HEADERS, formatter);
        List<RouteFlowRow> rows = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, FLOW_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String sourceProcessCode = requiredCell(sheet, row, 1, "源工序编码", formatter);
            String targetProcessCode = requiredCell(sheet, row, 2, "目标工序编码", formatter);
            String edgeKey = routeCode + ":" + sourceProcessCode + "->" + targetProcessCode;
            if (!edgeKeys.add(edgeKey)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, edgeKey);
            }
            rows.add(new RouteFlowRow(routeCode, sourceProcessCode, targetProcessCode, cell(row, 3, formatter)));
        }
        return rows;
    }

    private List<RouteBoundaryRow> parseBoundaries(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, BOUNDARY_SHEET, BOUNDARY_HEADERS, formatter);
        List<RouteBoundaryRow> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, BOUNDARY_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String boundaryType = requiredCell(sheet, row, 1, "边界类型", formatter);
            String processCode = requiredCell(sheet, row, 2, "工序编码", formatter);
            Integer sort = parseIntegerOrNull(cell(row, 3, formatter), BOUNDARY_SHEET, row);
            String key = routeCode + ":" + boundaryType + ":" + processCode + ":" + sort;
            if (!keys.add(key)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, key);
            }
            if (!"START".equals(boundaryType) && !"END".equals(boundaryType)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID, "边界类型无效：" + boundaryType);
            }
            rows.add(new RouteBoundaryRow(routeCode, boundaryType, processCode, sort));
        }
        return rows;
    }

    private List<RouteLayoutRow> parseLayouts(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, LAYOUT_SHEET, LAYOUT_HEADERS, formatter);
        List<RouteLayoutRow> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, LAYOUT_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String processCode = requiredCell(sheet, row, 1, "工序编码", formatter);
            String key = routeCode + ":" + processCode;
            if (!keys.add(key)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, key);
            }
            rows.add(new RouteLayoutRow(routeCode, processCode,
                    parseInteger(requiredCell(sheet, row, 2, "横坐标", formatter), LAYOUT_SHEET, row),
                    parseInteger(requiredCell(sheet, row, 3, "纵坐标", formatter), LAYOUT_SHEET, row),
                    parseIntegerOrNull(cell(row, 4, formatter), LAYOUT_SHEET, row),
                    parseIntegerOrNull(cell(row, 5, formatter), LAYOUT_SHEET, row)));
        }
        return rows;
    }

    private List<RouteProductRow> parseProducts(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, PRODUCT_SHEET, PRODUCT_HEADERS, formatter);
        List<RouteProductRow> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, PRODUCT_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String productCode = requiredCell(sheet, row, 1, "产品编码", formatter);
            if (!keys.add(routeCode + ":" + productCode)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, routeCode + " 产品 " + productCode);
            }
            rows.add(new RouteProductRow(routeCode, productCode, parseIntegerOrNull(cell(row, 4, formatter), PRODUCT_SHEET, row),
                    parseBigDecimalOrNull(cell(row, 5, formatter)), cell(row, 6, formatter), cell(row, 7, formatter)));
        }
        return rows;
    }

    private List<RouteBomRow> parseBoms(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, BOM_SHEET, BOM_HEADERS, formatter);
        List<RouteBomRow> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, BOM_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String processCode = requiredCell(sheet, row, 1, "工序编码", formatter);
            String productCode = requiredCell(sheet, row, 2, "产品编码", formatter);
            String bomItemCode = requiredCell(sheet, row, 3, "BOM物料编码", formatter);
            if (!keys.add(routeCode + ":" + processCode + ":" + productCode + ":" + bomItemCode)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE,
                        routeCode + " 工序 " + processCode + " 产品 " + productCode + " BOM " + bomItemCode);
            }
            rows.add(new RouteBomRow(routeCode, processCode, productCode, bomItemCode,
                    parseBigDecimalOrNull(cell(row, 6, formatter)), cell(row, 7, formatter)));
        }
        return rows;
    }

    private List<RouteScheduleConfigRow> parseScheduleConfigs(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, SCHEDULE_CONFIG_SHEET, SCHEDULE_CONFIG_HEADERS, formatter);
        List<RouteScheduleConfigRow> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, SCHEDULE_CONFIG_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String processCode = requiredCell(sheet, row, 1, "工序编码", formatter);
            if (!keys.add(routeCode + ":" + processCode)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, routeCode + " 排产配置 " + processCode);
            }
            rows.add(new RouteScheduleConfigRow(routeCode, processCode,
                    requiredCell(sheet, row, 2, "产能模式", formatter),
                    parseBigDecimalOrNull(cell(row, 3, formatter)),
                    parseBigDecimalOrNull(cell(row, 4, formatter)),
                    parseBigDecimalOrNull(cell(row, 5, formatter)),
                    parseBoolean(cell(row, 6, formatter)),
                    parseLongOrNull(cell(row, 7, formatter), SCHEDULE_CONFIG_SHEET, row),
                    cell(row, 8, formatter),
                    cell(row, 9, formatter)));
        }
        return rows;
    }

    private List<RouteFlowConfigRow> parseFlowConfigs(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, FLOW_CONFIG_SHEET, FLOW_CONFIG_HEADERS, formatter);
        List<RouteFlowConfigRow> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, FLOW_CONFIG_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String useType = requiredCell(sheet, row, 1, "用途类型", formatter);
            if (!keys.add(routeUseKey(routeCode, useType))) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, routeCode + " 用途 " + useType);
            }
            rows.add(new RouteFlowConfigRow(routeCode, useType, parseBoolean(cell(row, 2, formatter)),
                    cell(row, 3, formatter), cell(row, 4, formatter)));
        }
        return rows;
    }

    private List<RouteFlowProcessConfigRow> parseFlowProcessConfigs(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, FLOW_PROCESS_CONFIG_SHEET, FLOW_PROCESS_CONFIG_HEADERS, formatter);
        List<RouteFlowProcessConfigRow> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, FLOW_PROCESS_CONFIG_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String useType = requiredCell(sheet, row, 1, "用途类型", formatter);
            String processCode = requiredCell(sheet, row, 2, "工序编码", formatter);
            String key = routeUseProcessKey(routeCode, useType, processCode);
            if (!keys.add(key)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, key);
            }
            rows.add(new RouteFlowProcessConfigRow(routeCode, useType, processCode,
                    parseBoolean(cell(row, 3, formatter)), cell(row, 4, formatter),
                    parseBigDecimalOrNull(cell(row, 5, formatter)), cell(row, 6, formatter),
                    cell(row, 7, formatter)));
        }
        return rows;
    }

    private List<RouteBatchRecordRow> parseBatchRecords(Sheet sheet, DataFormatter formatter) {
        validateHeaders(sheet, BATCH_RECORD_SHEET, BATCH_RECORD_HEADERS, formatter);
        List<RouteBatchRecordRow> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, BATCH_RECORD_HEADERS.size(), formatter)) {
                continue;
            }
            String routeCode = requiredCell(sheet, row, 0, "路线编码", formatter);
            String useType = requiredCell(sheet, row, 1, "用途类型", formatter);
            String processCode = requiredCell(sheet, row, 2, "工序编码", formatter);
            Integer reportSort = parseIntegerOrNull(cell(row, 28, formatter), BATCH_RECORD_SHEET, row);
            String key = routeUseProcessKey(routeCode, useType, processCode) + ":" + reportSort + ":"
                    + cell(row, 3, formatter) + ":" + cell(row, 7, formatter);
            if (!keys.add(key)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE, key);
            }
            rows.add(new RouteBatchRecordRow(routeCode, useType, processCode, cell(row, 3, formatter),
                    parseLongOrNull(cell(row, 4, formatter), BATCH_RECORD_SHEET, row),
                    parseLongOrNull(cell(row, 5, formatter), BATCH_RECORD_SHEET, row),
                    cell(row, 6, formatter), cell(row, 7, formatter),
                    parseLongOrNull(cell(row, 8, formatter), BATCH_RECORD_SHEET, row),
                    cell(row, 9, formatter),
                    parseLongOrNull(cell(row, 10, formatter), BATCH_RECORD_SHEET, row),
                    cell(row, 11, formatter), cell(row, 12, formatter), cell(row, 13, formatter),
                    cell(row, 14, formatter), cell(row, 15, formatter), cell(row, 16, formatter),
                    parseBoolean(cell(row, 17, formatter)),
                    parseLongOrNull(cell(row, 18, formatter), BATCH_RECORD_SHEET, row),
                    cell(row, 19, formatter), cell(row, 20, formatter), cell(row, 21, formatter),
                    cell(row, 22, formatter), cell(row, 23, formatter), cell(row, 24, formatter),
                    cell(row, 25, formatter), cell(row, 26, formatter), cell(row, 27, formatter),
                    reportSort, cell(row, 29, formatter)));
        }
        return rows;
    }

    private ResolvedWorkbook resolveAndValidate(ParsedWorkbook workbook) {
        Map<String, RouteRow> routeByCode = new LinkedHashMap<>();
        for (RouteRow route : workbook.routes()) {
            if (routeMapper.selectByCode(route.code()) != null) {
                throw exception(PRO_ROUTE_IMPORT_ROUTE_EXISTS, route.code());
            }
            routeByCode.put(route.code(), route);
        }
        Map<String, MesProProcessDO> processByCode = new HashMap<>();
        Map<String, MesMdItemDO> itemByCode = new HashMap<>();
        validateProcesses(workbook, routeByCode, processByCode);
        Map<String, Set<String>> processCodesByRoute = buildProcessCodesByRoute(workbook.processes());
        validateFlows(workbook, routeByCode);
        validateBoundaries(workbook, routeByCode, processCodesByRoute);
        validateLayouts(workbook, routeByCode, processCodesByRoute);
        validateProducts(workbook, routeByCode, itemByCode);
        validateBoms(workbook, routeByCode, processByCode, itemByCode);
        validateScheduleConfigs(workbook, routeByCode, processCodesByRoute);
        validateFlowConfigs(workbook, routeByCode);
        validateFlowProcessConfigs(workbook, routeByCode, processCodesByRoute);
        validateBatchRecords(workbook, routeByCode, processCodesByRoute);
        return new ResolvedWorkbook(processByCode, itemByCode);
    }

    private void validateProcesses(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode,
                                   Map<String, MesProProcessDO> processByCode) {
        for (RouteProcessRow row : workbook.processes()) {
            requireRoute(routeByCode, row.routeCode());
            MesProProcessDO process = processByCode.computeIfAbsent(row.processCode(), code -> processMapper.selectByCode(code));
            if (process == null) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_MASTER_MISSING, "工序 " + row.processCode());
            }
        }
    }

    private void validateFlows(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode) {
        Map<String, Set<String>> processCodesByRoute = new LinkedHashMap<>();
        for (RouteProcessRow process : workbook.processes()) {
            processCodesByRoute.computeIfAbsent(process.routeCode(), key -> new LinkedHashSet<>())
                    .add(process.processCode());
        }
        Map<String, List<RouteFlowRow>> flowsByRoute = new LinkedHashMap<>();
        for (RouteFlowRow flow : workbook.flows()) {
            requireRoute(routeByCode, flow.routeCode());
            Set<String> routeProcessCodes = processCodesByRoute.getOrDefault(flow.routeCode(), Set.of());
            if (!routeProcessCodes.contains(flow.sourceProcessCode())
                    || !routeProcessCodes.contains(flow.targetProcessCode())) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "路线 " + flow.routeCode() + " 的流转关系引用了非当前路线工序");
            }
            if (flow.sourceProcessCode().equals(flow.targetProcessCode())) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "路线 " + flow.routeCode() + " 的工序不能连接自身");
            }
            flowsByRoute.computeIfAbsent(flow.routeCode(), key -> new ArrayList<>()).add(flow);
        }
        for (RouteRow route : workbook.routes()) {
            validateRouteFlow(route.code(), processCodesByRoute.getOrDefault(route.code(), Set.of()),
                    flowsByRoute.getOrDefault(route.code(), List.of()));
        }
    }

    private void validateRouteFlow(String routeCode, Set<String> processCodes, List<RouteFlowRow> flows) {
        if (processCodes.isEmpty()) {
            if (!flows.isEmpty()) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "路线 " + routeCode + " 不存在可连接工序");
            }
            return;
        }
        if (processCodes.size() == 1) {
            if (!flows.isEmpty()) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "路线 " + routeCode + " 的单工序路线不能配置连接线");
            }
            return;
        }
        if (flows.isEmpty()) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                    "路线 " + routeCode + " 缺少流转关系");
        }
        Map<String, Set<String>> outgoing = new LinkedHashMap<>();
        Map<String, Integer> incomingCount = new LinkedHashMap<>();
        processCodes.forEach(code -> {
            outgoing.put(code, new LinkedHashSet<>());
            incomingCount.put(code, 0);
        });
        Set<String> seenEdges = new HashSet<>();
        for (RouteFlowRow flow : flows) {
            String edgeKey = flow.sourceProcessCode() + "->" + flow.targetProcessCode();
            if (!seenEdges.add(edgeKey)) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "路线 " + routeCode + " 存在重复流转关系");
            }
            outgoing.get(flow.sourceProcessCode()).add(flow.targetProcessCode());
            incomingCount.merge(flow.targetProcessCode(), 1, Integer::sum);
        }
        List<String> isolated = processCodes.stream()
                .filter(code -> incomingCount.getOrDefault(code, 0) == 0 && outgoing.getOrDefault(code, Set.of()).isEmpty())
                .toList();
        if (!isolated.isEmpty()) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                    "路线 " + routeCode + " 存在未连接工序");
        }
        List<String> roots = processCodes.stream()
                .filter(code -> incomingCount.getOrDefault(code, 0) == 0)
                .toList();
        if (roots.isEmpty()) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                    "路线 " + routeCode + " 必须存在至少一个入口工序");
        }
        Map<String, Integer> remainingIncoming = new HashMap<>(incomingCount);
        Queue<String> queue = new ArrayDeque<>(roots);
        int reachableCount = 0;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            reachableCount++;
            for (String target : outgoing.getOrDefault(current, Set.of())) {
                int nextIncoming = remainingIncoming.merge(target, -1, Integer::sum);
                if (nextIncoming == 0) {
                    queue.add(target);
                }
            }
        }
        if (reachableCount != processCodes.size()) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                    "路线 " + routeCode + " 存在循环或无法从入口到达的工序");
        }
    }

    private Map<String, Set<String>> buildProcessCodesByRoute(List<RouteProcessRow> processes) {
        Map<String, Set<String>> processCodesByRoute = new LinkedHashMap<>();
        for (RouteProcessRow process : processes) {
            processCodesByRoute.computeIfAbsent(process.routeCode(), key -> new LinkedHashSet<>())
                    .add(process.processCode());
        }
        return processCodesByRoute;
    }

    private String routeProcessKey(String routeCode, String processCode) {
        return routeCode + "\u0000" + processCode;
    }

    private String routeUseKey(String routeCode, String useType) {
        return routeCode + "\u0000" + useType;
    }

    private String routeUseProcessKey(String routeCode, String useType, String processCode) {
        return routeUseKey(routeCode, useType) + "\u0000" + processCode;
    }

    private void validateProducts(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode,
                                  Map<String, MesMdItemDO> itemByCode) {
        for (RouteProductRow row : workbook.products()) {
            requireRoute(routeByCode, row.routeCode());
            MesMdItemDO item = itemByCode.computeIfAbsent(row.productCode(), code -> itemMapper.selectByCode(code));
            if (item == null) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_MASTER_MISSING, "产品 " + row.productCode());
            }
        }
    }

    private void validateBoms(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode,
                              Map<String, MesProProcessDO> processByCode, Map<String, MesMdItemDO> itemByCode) {
        for (RouteBomRow row : workbook.boms()) {
            requireRoute(routeByCode, row.routeCode());
            if (processByCode.computeIfAbsent(row.processCode(), code -> processMapper.selectByCode(code)) == null) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_MASTER_MISSING, "工序 " + row.processCode());
            }
            if (itemByCode.computeIfAbsent(row.productCode(), code -> itemMapper.selectByCode(code)) == null) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_MASTER_MISSING, "产品 " + row.productCode());
            }
            if (itemByCode.computeIfAbsent(row.bomItemCode(), code -> itemMapper.selectByCode(code)) == null) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_MASTER_MISSING, "BOM物料 " + row.bomItemCode());
            }
        }
    }

    private void validateBoundaries(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode,
                                    Map<String, Set<String>> processCodesByRoute) {
        for (RouteBoundaryRow row : workbook.boundaries()) {
            requireRoute(routeByCode, row.routeCode());
            requireRouteProcess(processCodesByRoute, row.routeCode(), row.processCode(), BOUNDARY_SHEET);
        }
    }

    private void validateLayouts(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode,
                                 Map<String, Set<String>> processCodesByRoute) {
        for (RouteLayoutRow row : workbook.layouts()) {
            requireRoute(routeByCode, row.routeCode());
            requireRouteProcess(processCodesByRoute, row.routeCode(), row.processCode(), LAYOUT_SHEET);
        }
    }

    private void validateScheduleConfigs(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode,
                                         Map<String, Set<String>> processCodesByRoute) {
        for (RouteScheduleConfigRow row : workbook.scheduleConfigs()) {
            requireRoute(routeByCode, row.routeCode());
            requireRouteProcess(processCodesByRoute, row.routeCode(), row.processCode(), SCHEDULE_CONFIG_SHEET);
        }
    }

    private void validateFlowConfigs(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode) {
        for (RouteFlowConfigRow row : workbook.flowConfigs()) {
            requireRoute(routeByCode, row.routeCode());
        }
    }

    private void validateFlowProcessConfigs(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode,
                                            Map<String, Set<String>> processCodesByRoute) {
        Set<String> routeUseKeys = new LinkedHashSet<>();
        for (RouteFlowConfigRow row : workbook.flowConfigs()) {
            routeUseKeys.add(routeUseKey(row.routeCode(), row.useType()));
        }
        for (RouteFlowProcessConfigRow row : workbook.flowProcessConfigs()) {
            requireRoute(routeByCode, row.routeCode());
            requireRouteProcess(processCodesByRoute, row.routeCode(), row.processCode(), FLOW_PROCESS_CONFIG_SHEET);
            if (!routeUseKeys.contains(routeUseKey(row.routeCode(), row.useType()))) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "工序用途配置缺少流程用途配置：" + row.routeCode() + " " + row.useType());
            }
        }
    }

    private void validateBatchRecords(ParsedWorkbook workbook, Map<String, RouteRow> routeByCode,
                                      Map<String, Set<String>> processCodesByRoute) {
        Set<String> useProcessKeys = new LinkedHashSet<>();
        for (RouteFlowProcessConfigRow row : workbook.flowProcessConfigs()) {
            useProcessKeys.add(routeUseProcessKey(row.routeCode(), row.useType(), row.processCode()));
        }
        for (RouteBatchRecordRow row : workbook.batchRecords()) {
            requireRoute(routeByCode, row.routeCode());
            requireRouteProcess(processCodesByRoute, row.routeCode(), row.processCode(), BATCH_RECORD_SHEET);
            if (!useProcessKeys.contains(routeUseProcessKey(row.routeCode(), row.useType(), row.processCode()))) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                        "工序表单绑定缺少工序用途配置：" + row.routeCode() + " " + row.useType()
                                + " " + row.processCode());
            }
        }
    }

    private void requireRouteProcess(Map<String, Set<String>> processCodesByRoute, String routeCode,
                                     String processCode, String sheetName) {
        if (!processCodesByRoute.getOrDefault(routeCode, Set.of()).contains(processCode)) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID,
                    sheetName + " 引用了非当前路线工序：" + routeCode + " " + processCode);
        }
    }

    private void requireRoute(Map<String, RouteRow> routeByCode, String routeCode) {
        if (!routeByCode.containsKey(routeCode)) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_MASTER_MISSING, "路线 " + routeCode);
        }
    }

    private void validateHeaders(Sheet sheet, String sheetName, List<String> headers, DataFormatter formatter) {
        Row row = sheet.getRow(0);
        if (row == null) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_HEADERS_INVALID, sheetName);
        }
        for (int i = 0; i < headers.size(); i++) {
            if (!headers.get(i).equals(cell(row, i, formatter))) {
                throw exception(PRO_ROUTE_IMPORT_WORKBOOK_HEADERS_INVALID, sheetName);
            }
        }
    }

    private boolean isBlankRow(Row row, int columnCount, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < columnCount; i++) {
            String value = cell(row, i, formatter);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String requiredCell(Sheet sheet, Row row, int index, String label, DataFormatter formatter) {
        String value = cell(row, index, formatter);
        if (value == null || value.isBlank()) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_CELL_REQUIRED, sheet.getSheetName(), row.getRowNum() + 1, label);
        }
        return value;
    }

    private String cell(Row row, int index, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        String value = formatter.formatCellValue(row.getCell(index));
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer parseStatus(String value) {
        Integer status = parseInteger(value, ROUTE_SHEET, null);
        if (!CommonStatusEnum.ENABLE.getStatus().equals(status) && !CommonStatusEnum.DISABLE.getStatus().equals(status)) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_STATUS_INVALID, value);
        }
        return status;
    }

    private Integer parseIntegerOrNull(String value, String sheetName, Row row) {
        return value == null ? null : parseInteger(value, sheetName, row);
    }

    private Integer parseInteger(String value, String sheetName, Row row) {
        try {
            return new BigDecimal(value).intValueExact();
        } catch (Exception ex) {
            String location = row == null ? sheetName : sheetName + " 第 " + (row.getRowNum() + 1) + " 行";
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID, location + " 数字无效：" + value);
        }
    }

    private Long parseLongOrNull(String value, String sheetName, Row row) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value).longValueExact();
        } catch (Exception ex) {
            String location = row == null ? sheetName : sheetName + " 第 " + (row.getRowNum() + 1) + " 行";
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID, location + " 长整数无效：" + value);
        }
    }

    private BigDecimal parseBigDecimalOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (Exception ex) {
            throw exception(PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID, "数字无效：" + value);
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null) {
            return Boolean.FALSE;
        }
        return "true".equalsIgnoreCase(value) || "是".equals(value) || "1".equals(value);
    }

    private record ParsedWorkbook(List<RouteRow> routes,
                                  List<RouteProcessRow> processes,
                                  List<RouteFlowRow> flows,
                                  List<RouteBoundaryRow> boundaries,
                                  List<RouteLayoutRow> layouts,
                                  List<RouteProductRow> products,
                                  List<RouteBomRow> boms,
                                  List<RouteScheduleConfigRow> scheduleConfigs,
                                  List<RouteFlowConfigRow> flowConfigs,
                                  List<RouteFlowProcessConfigRow> flowProcessConfigs,
                                  List<RouteBatchRecordRow> batchRecords) {
    }

    private record ResolvedWorkbook(Map<String, MesProProcessDO> processByCode,
                                    Map<String, MesMdItemDO> itemByCode) {
    }

    private record RouteRow(String code, String name, Integer status, String ownerName, String description, String remark) {
    }

    private record RouteProcessRow(String routeCode, Integer sort, String processCode,
                                   Integer prepareTime, Integer waitTime, String colorCode,
                                   Boolean keyFlag, Boolean checkFlag, String remark) {
    }

    private record RouteFlowRow(String routeCode, String sourceProcessCode, String targetProcessCode,
                                 String relationType) {
    }

    private record RouteBoundaryRow(String routeCode, String boundaryType, String processCode, Integer sort) {
    }

    private record RouteLayoutRow(String routeCode, String processCode, Integer x, Integer y,
                                  Integer width, Integer height) {
    }

    private record RouteProductRow(String routeCode, String productCode, Integer quantity, BigDecimal productionTime,
                                   String timeUnitType, String remark) {
    }

    private record RouteBomRow(String routeCode, String processCode, String productCode, String bomItemCode,
                                BigDecimal quantity, String remark) {
    }

    private record RouteScheduleConfigRow(String routeCode, String processCode, String capacityMode,
                                          BigDecimal hourlyCapacity, BigDecimal infiniteDurationQuantityFactor,
                                          BigDecimal infiniteDurationBaseMinutes, Boolean nightShiftEnabled,
                                          Long calendarRuleId, String configVersion, String remark) {
    }

    private record RouteFlowConfigRow(String routeCode, String useType, Boolean enabled, String configVersion,
                                      String remark) {
    }

    private record RouteFlowProcessConfigRow(String routeCode, String useType, String processCode, Boolean enabled,
                                             String executionMode, BigDecimal productionQuantityFactor,
                                             String batchRecordReportId, String remark) {
    }

    private record RouteBatchRecordRow(String routeCode, String useType, String processCode,
                                       String batchRecordReportId, Long batchRecordDefinitionId,
                                       Long batchRecordVersionId, String formSlotType, String formBindingKey,
                                       Long formTemplateId, String formTemplateNameSnapshot,
                                       Long lastPublishedTemplateVersionId, String lastPublishedTemplateVersionNo,
                                       String instanceScope, String sharedFormKey, String fillableScopeJson,
                                       String recordCategory, String validationProfile, Boolean recordbookEnabled,
                                       Long permissionScopeId, String recordCategorySnapshotHash,
                                       String requiredPolicy, String requiredConditionJson, String ownerRoleKey,
                                       String archiveVisibility, String slotConfigSnapshotHash,
                                       String candidateSourceType, String candidateSourceIds,
                                       String candidateSourceNames, Integer reportSort, String remark) {
    }

}
