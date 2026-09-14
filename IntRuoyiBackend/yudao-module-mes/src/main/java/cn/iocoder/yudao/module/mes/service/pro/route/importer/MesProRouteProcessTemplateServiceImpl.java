package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowBoundaryEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowEdgeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowLayoutReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowValidationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
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
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.DATA_START_ROW_INDEX;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.FORBIDDEN_HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.HEADERS;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.HEADER_ROW_INDEX;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.IMPORT_MODE_REBUILD;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.IMPORT_MODE_UPGRADE;
import static cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteProcessTemplateConstants.SHEET_NAME;

/**
 * 工艺路线员工工序模板服务实现。
 */
@Service
@Validated
public class MesProRouteProcessTemplateServiceImpl implements MesProRouteProcessTemplateService {

    private static final String NORMAL_RELATION_TYPE = "NORMAL";
    private static final String START_BOUNDARY_TYPE = "START";
    private static final String END_BOUNDARY_TYPE = "END";
    private static final String BATCH_USE_TYPE = "BATCH";
    private static final String TEMPLATE_CONFIG_VERSION = "PROCESS_TEMPLATE_IMPORT";

    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesMdWorkstationMachineMapper workstationMachineMapper;
    @Resource
    private MesDvMachineryMapper machineryMapper;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Resource
    private MesProRouteVersionWorkflowService routeVersionWorkflowService;
    @Resource
    private MesProRouteCandidateConfigService routeCandidateConfigService;

    @Override
    public byte[] exportTemplate(Long routeId) {
        MesProRouteDO route = requireRoute(routeId);
        List<MesProRouteProcessDO> routeProcesses = new ArrayList<>(routeProcessMapper.selectListByRouteId(routeId));
        routeProcesses.sort(Comparator.comparing(MesProRouteProcessDO::getSort,
                Comparator.nullsLast(Integer::compareTo)));

        Map<Long, MesProProcessDO> processMap = loadProcessMap(routeProcesses);
        Map<Long, MesProRouteScheduleConfigDO> scheduleConfigMap = loadScheduleConfigMap(routeId);
        Map<Long, MesMdWorkstationDO> workstationMap = loadWorkstationMap(routeProcesses);
        Map<Long, MesDvMachineryDO> machineryMap = loadMachineryMap(routeProcesses, workstationMap);
        Map<Long, String> equipmentCodeByRouteProcessId = resolveEquipmentCodes(routeProcesses, workstationMap,
                machineryMap);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle labelStyle = createLabelStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            writeMetadata(sheet, route, labelStyle);

            Row header = sheet.createRow(HEADER_ROW_INDEX);
            for (int index = 0; index < HEADERS.size(); index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = DATA_START_ROW_INDEX;
            for (MesProRouteProcessDO routeProcess : routeProcesses) {
                MesProProcessDO process = requireProcess(processMap.get(routeProcess.getProcessId()),
                        routeProcess.getProcessId());
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(process.getName());
                MesProRouteScheduleConfigDO scheduleConfig = scheduleConfigMap.get(routeProcess.getId());
                if (scheduleConfig != null && scheduleConfig.getHourlyCapacity() != null) {
                    row.createCell(1).setCellValue(scheduleConfig.getHourlyCapacity().doubleValue());
                } else {
                    row.createCell(1).setCellValue("");
                }
                row.createCell(2).setCellValue(equipmentCodeByRouteProcessId.getOrDefault(routeProcess.getId(), ""));
                row.createCell(3).setCellValue(Boolean.TRUE.equals(routeProcess.getKeyFlag()) ? "是" : "否");
            }
            sheet.createFreezePane(0, DATA_START_ROW_INDEX);
            sheet.setColumnWidth(0, 28 * 256);
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 24 * 256);
            sheet.setColumnWidth(3, 18 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw ServiceExceptionUtil.invalidParamException("生成工序模板失败: {}", exception.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteProcessTemplateImportResult importTemplate(MultipartFile file, String importMode) {
        validateImportMode(importMode);
        ParsedTemplate parsed = parse(file);
        MesProRouteDO route = resolveTemplateRoute(parsed);
        List<ResolvedProcess> resolvedProcesses = resolveProcesses(parsed.rows());
        validateResolvedProcesses(resolvedProcesses);

        if (IMPORT_MODE_REBUILD.equals(importMode)) {
            return rebuildRoute(route, resolvedProcesses);
        }
        return upgradeRoute(route, resolvedProcesses);
    }

    private MesProRouteProcessTemplateImportResult rebuildRoute(MesProRouteDO route,
                                                                List<ResolvedProcess> resolvedProcesses) {
        routeService.validateRouteNotEnable(route.getId());
        MesProRouteVersionDO activeVersion = requireActiveVersion(route.getId());
        MesProRouteProcessFlowGraphRespVO graph = requireGraph(routeProcessFlowService.getGraph(route.getId()),
                route.getId(), null);
        List<MesProRouteProcessDO> currentRouteProcesses = new ArrayList<>(
                routeProcessMapper.selectListByRouteId(route.getId()));
        currentRouteProcesses.sort(Comparator.comparing(MesProRouteProcessDO::getSort,
                Comparator.nullsLast(Integer::compareTo)));
        GraphDraft graphDraft = buildGraphRequest(route.getId(), null, graph.getGraphVersion(),
                currentRouteProcesses, resolvedProcesses, true);
        validateRemovableRouteProcesses(graphDraft.removedRouteProcesses());
        applyExistingRouteProcessUpdates(graphDraft.existingRouteProcessUpdates());
        MesProRouteProcessFlowValidationRespVO saved = requireSavedGraph(
                routeProcessFlowService.saveGraph(graphDraft.request()));
        List<Long> routeProcessIds = resolvePersistedRouteProcessIds(
                graphDraft.routeProcessReferences(), saved.getRouteProcessIdMap());

        for (Long routeProcessId : routeProcessIds) {
            routeService.ensureDefaultScheduleArtifacts(route.getId(), routeProcessId);
        }
        routeScheduleConfigMapper.deleteByRouteVersionId(activeVersion.getId());
        for (int index = 0; index < resolvedProcesses.size(); index++) {
            routeScheduleConfigMapper.insert(buildScheduleConfig(activeVersion.getId(), routeProcessIds.get(index),
                    resolvedProcesses.get(index).row().hourlyCapacity()));
        }
        routeService.maintainRouteVersionAfterProcessChange(route.getId());
        return buildResult(route, IMPORT_MODE_REBUILD, activeVersion, resolvedProcesses);
    }

    private MesProRouteProcessTemplateImportResult upgradeRoute(MesProRouteDO route,
                                                                List<ResolvedProcess> resolvedProcesses) {
        MesProRouteVersionDO activeVersion = requireActiveVersion(route.getId());
        MesProRouteVersionCreateReqVO createReqVO = new MesProRouteVersionCreateReqVO();
        createReqVO.setRouteId(route.getId());
        createReqVO.setSourceRouteVersionId(activeVersion.getId());
        createReqVO.setChangeReason("员工工序模板导入");
        MesProRouteVersionDO candidate = routeVersionWorkflowService.createCandidate(createReqVO);
        if (candidate == null || candidate.getId() == null
                || !MesProRouteVersionMapper.STATUS_DRAFT.equals(candidate.getLifecycleStatus())) {
            throw ServiceExceptionUtil.invalidParamException("工艺路线升版未获得可编辑的草稿候选版本");
        }

        MesProRouteProcessFlowGraphRespVO graph = requireGraph(
                routeProcessFlowService.getGraph(route.getId(), candidate.getId()), route.getId(), candidate.getId());
        List<MesProRouteProcessDO> currentRouteProcesses = graph.getNodes().stream()
                .map(node -> MesProRouteProcessDO.builder()
                        .id(node.getRouteProcessId())
                        .routeId(route.getId())
                        .processId(node.getProcessId())
                        .workstationId(node.getRouteProcessWorkstationId())
                        .sort(node.getSort())
                        .keyFlag(node.getKeyFlag())
                        .checkFlag(node.getCheckFlag())
                        .build())
                .toList();
        GraphDraft graphDraft = buildGraphRequest(route.getId(), candidate.getId(), graph.getGraphVersion(),
                currentRouteProcesses, resolvedProcesses, false);
        requireSavedGraph(routeProcessFlowService.saveGraph(graphDraft.request()));

        JSONObject scheduleConfigs = new JSONObject(true);
        for (int index = 0; index < resolvedProcesses.size(); index++) {
            Long clientRouteProcessId = clientRouteProcessId(index);
            scheduleConfigs.put(String.valueOf(clientRouteProcessId),
                    buildCandidateScheduleConfig(candidate, clientRouteProcessId,
                            resolvedProcesses.get(index).row().hourlyCapacity(), index + 1));
        }
        routeCandidateConfigService.saveConfigSnapshot(candidate.getId(), "scheduleConfigs", scheduleConfigs);
        return buildResult(route, IMPORT_MODE_UPGRADE, candidate, resolvedProcesses);
    }

    private GraphDraft buildGraphRequest(Long routeId, Long routeVersionId, Long graphVersion,
                                         List<MesProRouteProcessDO> currentRouteProcesses,
                                         List<ResolvedProcess> resolvedProcesses,
                                         boolean reuseExistingRouteProcesses) {
        if (graphVersion == null) {
            throw ServiceExceptionUtil.invalidParamException("工艺路线关系图版本缺失，无法导入工序模板");
        }
        Map<Long, MesProRouteProcessDO> currentByProcessId = new LinkedHashMap<>();
        for (MesProRouteProcessDO currentRouteProcess : currentRouteProcesses) {
            if (currentRouteProcess.getId() == null || currentRouteProcess.getProcessId() == null) {
                throw ServiceExceptionUtil.invalidParamException("当前工艺路线存在身份不完整的工序，无法导入模板");
            }
            if (currentByProcessId.putIfAbsent(currentRouteProcess.getProcessId(), currentRouteProcess) != null) {
                throw ServiceExceptionUtil.invalidParamException("当前工艺路线存在重复工序 [{}]，无法导入模板",
                        currentRouteProcess.getProcessId());
            }
        }
        Set<Long> targetProcessIds = resolvedProcesses.stream().map(item -> item.process().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProRouteProcessDO> removedRouteProcesses = reuseExistingRouteProcesses
                ? currentRouteProcesses.stream().filter(item -> !targetProcessIds.contains(item.getProcessId())).toList()
                : new ArrayList<>(currentRouteProcesses);

        MesProRouteProcessFlowSaveReqVO request = new MesProRouteProcessFlowSaveReqVO();
        request.setRouteId(routeId);
        request.setRouteVersionId(routeVersionId);
        request.setGraphVersion(graphVersion);
        request.setRouteProcessDeletes(removedRouteProcesses.stream().map(MesProRouteProcessDO::getId).toList());

        List<MesProRouteProcessSaveReqVO> creates = new ArrayList<>();
        List<MesProRouteProcessDO> existingRouteProcessUpdates = new ArrayList<>();
        List<Long> routeProcessReferences = new ArrayList<>();
        List<MesProRouteProcessFlowLayoutReqVO> layouts = new ArrayList<>();
        for (int index = 0; index < resolvedProcesses.size(); index++) {
            ResolvedProcess resolved = resolvedProcesses.get(index);
            MesProRouteProcessDO currentRouteProcess = reuseExistingRouteProcesses
                    ? currentByProcessId.get(resolved.process().getId()) : null;
            Long routeProcessReference;
            if (currentRouteProcess == null) {
                routeProcessReference = clientRouteProcessId(index);
                MesProRouteProcessSaveReqVO create = new MesProRouteProcessSaveReqVO();
                create.setClientRouteProcessId(routeProcessReference);
                create.setRouteId(routeId);
                create.setProcessId(resolved.process().getId());
                create.setWorkstationId(resolved.workstation().getId());
                create.setSort(index + 1);
                create.setKeyFlag(resolved.row().keyFlag());
                create.setCheckFlag(Boolean.FALSE);
                creates.add(create);
            } else {
                routeProcessReference = currentRouteProcess.getId();
                existingRouteProcessUpdates.add(MesProRouteProcessDO.builder()
                        .id(currentRouteProcess.getId())
                        .sort(index + 1)
                        .workstationId(resolved.workstation().getId())
                        .keyFlag(resolved.row().keyFlag())
                        .checkFlag(Boolean.FALSE)
                        .build());
            }
            routeProcessReferences.add(routeProcessReference);

            MesProRouteProcessFlowLayoutReqVO layout = new MesProRouteProcessFlowLayoutReqVO();
            layout.setRouteProcessId(routeProcessReference);
            layout.setX(120 + (index % 6) * 220);
            layout.setY(120 + (index / 6) * 140);
            layout.setWidth(180);
            layout.setHeight(80);
            layouts.add(layout);
        }
        request.setRouteProcessCreates(creates);
        request.setLayouts(layouts);

        List<MesProRouteProcessFlowEdgeReqVO> edges = new ArrayList<>();
        for (int index = 1; index < resolvedProcesses.size(); index++) {
            MesProRouteProcessFlowEdgeReqVO edge = new MesProRouteProcessFlowEdgeReqVO();
            edge.setSourceRouteProcessId(routeProcessReferences.get(index - 1));
            edge.setTargetRouteProcessId(routeProcessReferences.get(index));
            edge.setRelationType(NORMAL_RELATION_TYPE);
            edges.add(edge);
        }
        request.setEdges(edges);

        List<MesProRouteProcessFlowBoundaryEdgeReqVO> boundaryEdges = new ArrayList<>();
        MesProRouteProcessFlowBoundaryEdgeReqVO start = new MesProRouteProcessFlowBoundaryEdgeReqVO();
        start.setBoundaryType(START_BOUNDARY_TYPE);
        start.setRouteProcessId(routeProcessReferences.get(0));
        start.setSort(1);
        boundaryEdges.add(start);
        MesProRouteProcessFlowBoundaryEdgeReqVO end = new MesProRouteProcessFlowBoundaryEdgeReqVO();
        end.setBoundaryType(END_BOUNDARY_TYPE);
        end.setRouteProcessId(routeProcessReferences.get(resolvedProcesses.size() - 1));
        end.setSort(1);
        boundaryEdges.add(end);
        request.setBoundaryEdges(boundaryEdges);
        return new GraphDraft(request, routeProcessReferences, existingRouteProcessUpdates, removedRouteProcesses);
    }

    private void validateRemovableRouteProcesses(List<MesProRouteProcessDO> removedRouteProcesses) {
        if (removedRouteProcesses.isEmpty()) {
            return;
        }
        for (MesProRouteProcessDO routeProcess : removedRouteProcesses) {
            if (StrUtil.isNotBlank(routeProcess.getBatchRecordReportId())) {
                throw ServiceExceptionUtil.invalidParamException(
                        "路线工序 [{}] 已配置批记录，工序模板不能直接删除；请先通过批记录独立配置链路处理",
                        routeProcess.getId());
            }
        }
        List<Long> removedIds = removedRouteProcesses.stream().map(MesProRouteProcessDO::getId).toList();
        if (!routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(removedIds, BATCH_USE_TYPE).isEmpty()
                || !routeFlowProcessBatchRecordMapper
                .selectListByRouteProcessIdsAndUseType(removedIds, BATCH_USE_TYPE).isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException(
                    "待删除工序已配置批记录表单或表单槽位，工序模板不能覆盖这些独立配置");
        }
    }

    private void applyExistingRouteProcessUpdates(List<MesProRouteProcessDO> updates) {
        for (MesProRouteProcessDO update : updates) {
            if (routeProcessMapper.updateById(update) != 1) {
                throw ServiceExceptionUtil.invalidParamException("更新路线工序 [{}] 失败", update.getId());
            }
        }
    }

    private List<Long> resolvePersistedRouteProcessIds(List<Long> routeProcessReferences,
                                                       Map<Long, Long> persistedRouteProcessIds) {
        Map<Long, Long> persistedIds = persistedRouteProcessIds == null ? Map.of() : persistedRouteProcessIds;
        List<Long> result = new ArrayList<>();
        for (int index = 0; index < routeProcessReferences.size(); index++) {
            Long reference = routeProcessReferences.get(index);
            Long routeProcessId = reference != null && reference > 0 ? reference : persistedIds.get(reference);
            if (routeProcessId == null) {
                throw ServiceExceptionUtil.invalidParamException("重建工艺路线缺少第 {} 行工序映射", index + 1);
            }
            result.add(routeProcessId);
        }
        return result;
    }

    private MesProRouteScheduleConfigDO buildScheduleConfig(Long routeVersionId, Long routeProcessId,
                                                             BigDecimal hourlyCapacity) {
        return MesProRouteScheduleConfigDO.builder()
                .routeVersionId(routeVersionId)
                .routeProcessId(routeProcessId)
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .hourlyCapacity(hourlyCapacity)
                .nightShiftEnabled(Boolean.FALSE)
                .configVersion(TEMPLATE_CONFIG_VERSION)
                .remark("员工工序模板导入")
                .build();
    }

    private JSONObject buildCandidateScheduleConfig(MesProRouteVersionDO candidate, Long routeProcessId,
                                                    BigDecimal hourlyCapacity, int sort) {
        JSONObject config = new JSONObject(true);
        config.put("id", null);
        config.put("routeVersionId", candidate.getId());
        config.put("routeId", candidate.getRouteId());
        config.put("routeProcessId", routeProcessId);
        config.put("sort", sort);
        config.put("capacityMode", MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode());
        config.put("hourlyCapacity", hourlyCapacity);
        config.put("infiniteDurationQuantityFactor", null);
        config.put("infiniteDurationBaseMinutes", null);
        config.put("nightShiftEnabled", Boolean.FALSE);
        config.put("calendarRuleId", null);
        config.put("configVersion", TEMPLATE_CONFIG_VERSION);
        config.put("remark", "员工工序模板导入");
        return config;
    }

    private List<ResolvedProcess> resolveProcesses(List<ParsedRow> rows) {
        Set<String> machineryCodes = rows.stream().map(ParsedRow::machineryCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, MesDvMachineryDO> machineryByCode = new LinkedHashMap<>();
        for (MesDvMachineryDO machinery : machineryMapper.selectListByCodes(machineryCodes)) {
            if (machinery != null && StrUtil.isNotBlank(machinery.getCode())) {
                machineryByCode.put(machinery.getCode(), machinery);
            }
        }
        for (String machineryCode : machineryCodes) {
            if (!machineryByCode.containsKey(machineryCode)) {
                throw ServiceExceptionUtil.invalidParamException("设备编号 [{}] 不存在", machineryCode);
            }
        }

        Set<Long> machineryIds = machineryByCode.values().stream().map(MesDvMachineryDO::getId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<MesMdWorkstationMachineDO>> bindingsByMachineryId =
                workstationMachineMapper.selectListByMachineryIds(machineryIds).stream()
                        .collect(Collectors.groupingBy(MesMdWorkstationMachineDO::getMachineryId,
                                LinkedHashMap::new, Collectors.toList()));
        Set<Long> workstationIds = bindingsByMachineryId.values().stream().flatMap(List::stream)
                .map(MesMdWorkstationMachineDO::getWorkstationId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdWorkstationDO> workstationMap = new LinkedHashMap<>();
        for (MesMdWorkstationDO workstation : workstationMapper.selectByIds(workstationIds)) {
            if (workstation != null && workstation.getId() != null) {
                workstationMap.put(workstation.getId(), workstation);
            }
        }

        List<ResolvedProcess> result = new ArrayList<>();
        for (ParsedRow row : rows) {
            MesProProcessDO process = processMapper.selectByName(row.processName());
            if (process == null || process.getId() == null
                    || !Objects.equals(process.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
                throw ServiceExceptionUtil.invalidParamException("第 {} 行工序 [{}] 不存在或未启用",
                        row.sourceRowNo(), row.processName());
            }
            MesDvMachineryDO machinery = machineryByCode.get(row.machineryCode());
            List<MesMdWorkstationMachineDO> bindings = bindingsByMachineryId.getOrDefault(machinery.getId(), List.of());
            List<MesMdWorkstationDO> candidates = bindings.stream()
                    .map(binding -> workstationMap.get(binding.getWorkstationId()))
                    .filter(Objects::nonNull)
                    .filter(workstation -> Objects.equals(workstation.getProcessId(), process.getId()))
                    .filter(workstation -> Objects.equals(workstation.getStatus(), CommonStatusEnum.ENABLE.getStatus()))
                    .collect(Collectors.collectingAndThen(Collectors.toMap(MesMdWorkstationDO::getId,
                                    workstation -> workstation, (left, right) -> left, LinkedHashMap::new),
                            map -> new ArrayList<>(map.values())));
            if (candidates.size() != 1) {
                throw ServiceExceptionUtil.invalidParamException(
                        "第 {} 行设备编号 [{}] 未唯一绑定到工序 [{}] 的启用工作站",
                        row.sourceRowNo(), row.machineryCode(), row.processName());
            }
            result.add(new ResolvedProcess(row, process, machinery, candidates.get(0)));
        }
        return result;
    }

    private void validateResolvedProcesses(List<ResolvedProcess> resolvedProcesses) {
        if (resolvedProcesses.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("Excel 中没有可导入的工序");
        }
        Set<Long> processIds = new HashSet<>();
        int keyProcessCount = 0;
        for (ResolvedProcess resolved : resolvedProcesses) {
            if (!processIds.add(resolved.process().getId())) {
                throw ServiceExceptionUtil.invalidParamException("Excel 中工序 [{}] 重复",
                        resolved.row().processName());
            }
            if (resolved.row().keyFlag()) {
                keyProcessCount++;
            }
        }
        if (keyProcessCount > 1) {
            throw ServiceExceptionUtil.invalidParamException("关键工序只能存在一个");
        }
    }

    private MesProRouteDO resolveTemplateRoute(ParsedTemplate parsed) {
        MesProRouteDO route = routeMapper.selectByCode(parsed.routeCode());
        if (route == null || route.getId() == null) {
            throw ServiceExceptionUtil.invalidParamException("路线编码 [{}] 不存在", parsed.routeCode());
        }
        if (!Objects.equals(StrUtil.trim(route.getName()), parsed.routeName())) {
            throw ServiceExceptionUtil.invalidParamException("Excel 路线名称 [{}] 与正式路线 [{}] 不一致",
                    parsed.routeName(), route.getName());
        }
        return route;
    }

    private ParsedTemplate parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("Excel 文件不能为空");
        }
        DataFormatter formatter = new DataFormatter();
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw ServiceExceptionUtil.invalidParamException("Excel 缺少 [{}] 工作表", SHEET_NAME);
            }
            String routeCode = requiredMetadata(sheet, 0, "路线编码", formatter);
            String routeName = requiredMetadata(sheet, 1, "路线名称", formatter);
            validateHeader(sheet.getRow(HEADER_ROW_INDEX), formatter);

            List<ParsedRow> rows = new ArrayList<>();
            Set<String> processNames = new HashSet<>();
            int keyProcessCount = 0;
            for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }
                int sourceRowNo = rowIndex + 1;
                String processName = requiredCell(row, 0, "工序名称", sourceRowNo, formatter);
                String rawCapacity = requiredCell(row, 1, "产能", sourceRowNo, formatter);
                String machineryCode = requiredCell(row, 2, "设备编号", sourceRowNo, formatter);
                BigDecimal hourlyCapacity = parsePositiveDecimal(rawCapacity, "产能", sourceRowNo);
                boolean keyFlag = parseKeyFlag(readCell(row.getCell(3), formatter), sourceRowNo);
                if (!processNames.add(processName)) {
                    throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行工序 [{}] 重复",
                            sourceRowNo, processName);
                }
                if (keyFlag) {
                    keyProcessCount++;
                }
                validateNoExtraCells(row, formatter, sourceRowNo);
                rows.add(new ParsedRow(sourceRowNo, processName, hourlyCapacity, machineryCode, keyFlag));
            }
            if (rows.isEmpty()) {
                throw ServiceExceptionUtil.invalidParamException("Excel 中没有可导入的工序");
            }
            if (keyProcessCount > 1) {
                throw ServiceExceptionUtil.invalidParamException("关键工序只能存在一个");
            }
            return new ParsedTemplate(routeCode, routeName, rows);
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ServiceExceptionUtil.invalidParamException("解析工序模板失败: {}", exception.getMessage());
        }
    }

    private void validateHeader(Row header, DataFormatter formatter) {
        if (header == null) {
            throw ServiceExceptionUtil.invalidParamException("Excel 缺少工序模板表头");
        }
        for (int index = 0; index < Math.max(HEADERS.size(), header.getLastCellNum()); index++) {
            String actual = readCell(header.getCell(index), formatter);
            if (FORBIDDEN_HEADERS.contains(actual)) {
                throw ServiceExceptionUtil.invalidParamException("工序模板不支持字段 [{}]", actual);
            }
            if (index < HEADERS.size() && !Objects.equals(HEADERS.get(index), actual)) {
                throw ServiceExceptionUtil.invalidParamException(
                        "Excel 表头不符合工序模板要求：第 {} 列应为 [{}]，实际为 [{}]",
                        index + 1, HEADERS.get(index), actual);
            }
            if (index >= HEADERS.size() && StrUtil.isNotBlank(actual)) {
                throw ServiceExceptionUtil.invalidParamException("工序模板不支持额外字段 [{}]", actual);
            }
        }
    }

    private void validateNoExtraCells(Row row, DataFormatter formatter, int sourceRowNo) {
        short lastCellNum = row.getLastCellNum();
        for (int index = HEADERS.size(); index < lastCellNum; index++) {
            if (StrUtil.isNotBlank(readCell(row.getCell(index), formatter))) {
                throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行包含模板之外的字段", sourceRowNo);
            }
        }
    }

    private String requiredMetadata(Sheet sheet, int rowIndex, String label, DataFormatter formatter) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || !Objects.equals(label, readCell(row.getCell(0), formatter))) {
            throw ServiceExceptionUtil.invalidParamException("Excel 缺少 [{}] 元数据", label);
        }
        String value = readCell(row.getCell(1), formatter);
        if (StrUtil.isBlank(value)) {
            throw ServiceExceptionUtil.invalidParamException("Excel 元数据 [{}] 不能为空", label);
        }
        return value;
    }

    private String requiredCell(Row row, int cellIndex, String fieldName, int sourceRowNo,
                                DataFormatter formatter) {
        String value = readCell(row.getCell(cellIndex), formatter);
        if (StrUtil.isBlank(value)) {
            throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行{}不能为空", sourceRowNo, fieldName);
        }
        return value;
    }

    private BigDecimal parsePositiveDecimal(String raw, String fieldName, int sourceRowNo) {
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行{}必须大于 0",
                        sourceRowNo, fieldName);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行{}不是有效数值",
                    sourceRowNo, fieldName);
        }
    }

    private boolean parseKeyFlag(String raw, int sourceRowNo) {
        if (StrUtil.isBlank(raw) || "否".equals(raw) || "0".equals(raw)
                || "FALSE".equalsIgnoreCase(raw) || "NO".equalsIgnoreCase(raw)) {
            return false;
        }
        if ("是".equals(raw) || "1".equals(raw)
                || "TRUE".equalsIgnoreCase(raw) || "YES".equalsIgnoreCase(raw)) {
            return true;
        }
        throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行是否关键工序只能填写 是/否",
                sourceRowNo);
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int index = 0; index < HEADERS.size(); index++) {
            if (StrUtil.isNotBlank(readCell(row.getCell(index), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : StrUtil.trim(formatter.formatCellValue(cell));
    }

    private void validateImportMode(String importMode) {
        if (!IMPORT_MODE_REBUILD.equals(importMode) && !IMPORT_MODE_UPGRADE.equals(importMode)) {
            throw ServiceExceptionUtil.invalidParamException("导入模式必须为 REBUILD 或 UPGRADE");
        }
    }

    private MesProRouteDO requireRoute(Long routeId) {
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route == null || route.getId() == null) {
            throw ServiceExceptionUtil.invalidParamException("工艺路线 [{}] 不存在", routeId);
        }
        return route;
    }

    private MesProRouteVersionDO requireActiveVersion(Long routeId) {
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(routeId);
        if (activeVersion == null || activeVersion.getId() == null
                || !MesProRouteVersionMapper.STATUS_ACTIVE.equals(activeVersion.getLifecycleStatus())) {
            throw ServiceExceptionUtil.invalidParamException("路线 [{}] 缺少当前生效版本", routeId);
        }
        return activeVersion;
    }

    private MesProRouteProcessFlowGraphRespVO requireGraph(MesProRouteProcessFlowGraphRespVO graph,
                                                           Long routeId, Long routeVersionId) {
        if (graph == null || !Objects.equals(routeId, graph.getRouteId()) || graph.getGraphVersion() == null) {
            throw ServiceExceptionUtil.invalidParamException("路线 [{}] 的关系图数据不完整，版本 [{}] 无法导入",
                    routeId, routeVersionId);
        }
        return graph;
    }

    private MesProRouteProcessFlowValidationRespVO requireSavedGraph(
            MesProRouteProcessFlowValidationRespVO response) {
        if (response == null || !Boolean.TRUE.equals(response.getValid())) {
            throw ServiceExceptionUtil.invalidParamException("工序模板导入后的关系图校验未通过");
        }
        return response;
    }

    private Map<Long, MesProProcessDO> loadProcessMap(List<MesProRouteProcessDO> routeProcesses) {
        Set<Long> processIds = routeProcesses.stream().map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProProcessDO> processMap = new LinkedHashMap<>();
        for (MesProProcessDO process : processMapper.selectListByIds(processIds)) {
            if (process != null && process.getId() != null) {
                processMap.put(process.getId(), process);
            }
        }
        return processMap;
    }

    private Map<Long, MesProRouteScheduleConfigDO> loadScheduleConfigMap(Long routeId) {
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(routeId);
        if (activeVersion == null || activeVersion.getId() == null) {
            return Map.of();
        }
        return routeScheduleConfigMapper.selectListByRouteVersionId(activeVersion.getId()).stream()
                .filter(config -> config.getRouteProcessId() != null)
                .collect(Collectors.toMap(MesProRouteScheduleConfigDO::getRouteProcessId,
                        config -> config, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, MesMdWorkstationDO> loadWorkstationMap(List<MesProRouteProcessDO> routeProcesses) {
        Set<Long> workstationIds = routeProcesses.stream().map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (workstationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MesMdWorkstationDO> result = new LinkedHashMap<>();
        for (MesMdWorkstationDO workstation : workstationMapper.selectByIds(workstationIds)) {
            if (workstation != null && workstation.getId() != null) {
                result.put(workstation.getId(), workstation);
            }
        }
        return result;
    }

    private Map<Long, MesDvMachineryDO> loadMachineryMap(List<MesProRouteProcessDO> routeProcesses,
                                                         Map<Long, MesMdWorkstationDO> workstationMap) {
        Set<Long> workstationIds = routeProcesses.stream().map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (workstationIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> machineryIds = workstationMachineMapper.selectListByWorkstationIds(workstationIds).stream()
                .map(MesMdWorkstationMachineDO::getMachineryId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (machineryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MesDvMachineryDO> result = new LinkedHashMap<>();
        for (MesDvMachineryDO machinery : machineryMapper.selectByIds(machineryIds)) {
            if (machinery != null && machinery.getId() != null) {
                result.put(machinery.getId(), machinery);
            }
        }
        return result;
    }

    private Map<Long, String> resolveEquipmentCodes(List<MesProRouteProcessDO> routeProcesses,
                                                    Map<Long, MesMdWorkstationDO> workstationMap,
                                                    Map<Long, MesDvMachineryDO> machineryMap) {
        Set<Long> workstationIds = routeProcesses.stream().map(MesProRouteProcessDO::getWorkstationId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (workstationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<MesMdWorkstationMachineDO>> bindingsByWorkstationId =
                workstationMachineMapper.selectListByWorkstationIds(workstationIds).stream()
                        .collect(Collectors.groupingBy(MesMdWorkstationMachineDO::getWorkstationId,
                                LinkedHashMap::new, Collectors.toList()));
        Map<Long, String> result = new HashMap<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            Long workstationId = routeProcess.getWorkstationId();
            if (workstationId == null || !workstationMap.containsKey(workstationId)) {
                continue;
            }
            List<MesMdWorkstationMachineDO> bindings = bindingsByWorkstationId.getOrDefault(workstationId, List.of());
            if (bindings.size() > 1) {
                throw ServiceExceptionUtil.invalidParamException("路线工序 [{}] 的工作站绑定多个设备，员工模板无法表达",
                        routeProcess.getId());
            }
            if (bindings.isEmpty()) {
                continue;
            }
            MesDvMachineryDO machinery = machineryMap.get(bindings.get(0).getMachineryId());
            if (machinery == null || StrUtil.isBlank(machinery.getCode())) {
                throw ServiceExceptionUtil.invalidParamException("路线工序 [{}] 的设备主数据缺失",
                        routeProcess.getId());
            }
            result.put(routeProcess.getId(), machinery.getCode());
        }
        return result;
    }

    private MesProProcessDO requireProcess(MesProProcessDO process, Long processId) {
        if (process == null || StrUtil.isBlank(process.getName())) {
            throw ServiceExceptionUtil.invalidParamException("路线工序引用的工序主数据 [{}] 不存在", processId);
        }
        return process;
    }

    private MesProRouteProcessTemplateImportResult buildResult(MesProRouteDO route, String importMode,
                                                               MesProRouteVersionDO version,
                                                               List<ResolvedProcess> resolvedProcesses) {
        MesProRouteProcessTemplateImportResult result = new MesProRouteProcessTemplateImportResult();
        result.setRouteId(route.getId());
        result.setRouteCode(route.getCode());
        result.setRouteName(route.getName());
        result.setImportMode(importMode);
        result.setRouteVersionId(version.getId());
        result.setRouteVersionNo(version.getVersionNo());
        result.setRouteProcessCount(resolvedProcesses.size());
        result.setProcessNames(resolvedProcesses.stream().map(item -> item.row().processName()).toList());
        return result;
    }

    private void writeMetadata(Sheet sheet, MesProRouteDO route, CellStyle labelStyle) {
        writeMetadataRow(sheet.createRow(0), "路线编码", route.getCode(), labelStyle);
        writeMetadataRow(sheet.createRow(1), "路线名称", route.getName(), labelStyle);
        writeMetadataRow(sheet.createRow(2), "填写说明",
                "只填写工序名称、产能、设备编号、是否关键工序；工序顺序按 Excel 行顺序生成。",
                labelStyle);
    }

    private void writeMetadataRow(Row row, String label, String value, CellStyle labelStyle) {
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        row.createCell(1).setCellValue(StrUtil.blankToDefault(value, ""));
    }

    private CellStyle createLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor((short) 22);
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private Long clientRouteProcessId(int index) {
        return -(index + 1L);
    }

    private record ParsedTemplate(String routeCode, String routeName, List<ParsedRow> rows) {
    }

    private record ParsedRow(int sourceRowNo, String processName, BigDecimal hourlyCapacity,
                             String machineryCode, boolean keyFlag) {
    }

    private record ResolvedProcess(ParsedRow row, MesProProcessDO process, MesDvMachineryDO machinery,
                                   MesMdWorkstationDO workstation) {
    }

    private record GraphDraft(MesProRouteProcessFlowSaveReqVO request,
                              List<Long> routeProcessReferences,
                              List<MesProRouteProcessDO> existingRouteProcessUpdates,
                              List<MesProRouteProcessDO> removedRouteProcesses) {
    }
}
