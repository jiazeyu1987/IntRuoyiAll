package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateCodes;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlinePqcResults;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateTypes;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ACTIVE_ORDER_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ACTIVE_ORDER_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_PERSONNEL_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_REGULATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_QUANTITY_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_PQC_TASK_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SIGNATURE_EMPLOYEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PQC_RESULT_INVALID;

@Service
@Validated
public class MesFrontlinePqcContextServiceImpl implements MesFrontlinePqcContextService {

    private static final String PQC_INSPECTION_TASK_SOURCE_TYPE = "MES_PQC_INSPECTION_TASK";
    private static final String PQC_TASK_STATUS_PENDING = "PENDING";
    private static final String PQC_TASK_STATUS_SUBMITTED = "SUBMITTED";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProProcessPoolMapper processPoolMapper;
    private final MesProProcessPoolEventMapper processPoolEventMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProRouteMapper routeMapper;
    private final MesProRouteProductMapper routeProductMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationItemMapper regulationItemMapper;
    private final MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    private final MesProProcessService processService;
    private final MesMdItemService itemService;
    private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private final AdminUserApi adminUserApi;
    private final MesFrontlineTemplateResolver templateResolver;
    private final MesProcessPoolEventService processPoolEventService;

    public MesFrontlinePqcContextServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                             MesProProcessPoolMapper processPoolMapper,
                                             MesProProcessPoolEventMapper processPoolEventMapper,
                                             MesProWorkOrderMapper workOrderMapper,
                                             MesProRouteMapper routeMapper,
                                             MesProRouteProductMapper routeProductMapper,
                                             MesProRouteProcessMapper routeProcessMapper,
                                             MesQaInspectionRegulationMapper regulationMapper,
                                             MesQaInspectionRegulationItemMapper regulationItemMapper,
                                             MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper,
                                             MesPqcInspectionTaskMapper pqcTaskMapper,
                                             MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper,
                                             MesProProcessService processService,
                                             MesMdItemService itemService,
                                             MesProcessPoolTeamLeaderScopeMapper scopeMapper,
                                             AdminUserApi adminUserApi,
                                             MesFrontlineTemplateResolver templateResolver,
                                             MesProcessPoolEventService processPoolEventService) {
        this.activeOrderMapper = activeOrderMapper;
        this.processPoolMapper = processPoolMapper;
        this.processPoolEventMapper = processPoolEventMapper;
        this.workOrderMapper = workOrderMapper;
        this.routeMapper = routeMapper;
        this.routeProductMapper = routeProductMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.regulationMapper = regulationMapper;
        this.regulationItemMapper = regulationItemMapper;
        this.regulationItemEquipmentMapper = regulationItemEquipmentMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pqcPieceDetailMapper = pqcPieceDetailMapper;
        this.processService = processService;
        this.itemService = itemService;
        this.scopeMapper = scopeMapper;
        this.adminUserApi = adminUserApi;
        this.templateResolver = templateResolver;
        this.processPoolEventService = processPoolEventService;
    }

    @Override
    public List<MesFrontlineActiveOrderCandidate> listActiveOrders() {
        List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper.selectActiveList();
        if (CollUtil.isEmpty(activeOrders)) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_EMPTY);
        }

        Map<ActiveOrderKey, LocalDateTime> latestActiveTimeByOrder = new LinkedHashMap<>();
        for (MesProcessPoolActiveOrderDO activeOrder : activeOrders) {
            requireActiveOrderIdentity(activeOrder);
            ActiveOrderKey key = new ActiveOrderKey(activeOrder.getWorkOrderId(), activeOrder.getRouteId());
            LocalDateTime current = latestActiveTimeByOrder.get(key);
            if (current == null || isAfter(activeOrder.getJoinedAt(), current)) {
                latestActiveTimeByOrder.put(key, activeOrder.getJoinedAt());
            }
        }

        Set<Long> workOrderIds = latestActiveTimeByOrder.keySet().stream()
                .map(ActiveOrderKey::workOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> routeIds = latestActiveTimeByOrder.keySet().stream()
                .map(ActiveOrderKey::routeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProWorkOrderDO> workOrderMap = mapById(workOrderMapper.selectListByIds(workOrderIds),
                MesProWorkOrderDO::getId);
        Map<Long, MesProRouteDO> routeMap = mapById(routeMapper.selectListByIdsIgnoreDeleted(routeIds),
                MesProRouteDO::getId);
        Set<Long> productIds = workOrderMap.values().stream()
                .map(MesProWorkOrderDO::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesMdItemDO> itemMap = productIds.isEmpty() ? Map.of() : itemService.getItemMap(productIds);

        List<MesFrontlineActiveOrderCandidate> candidates = new ArrayList<>();
        for (Map.Entry<ActiveOrderKey, LocalDateTime> entry : latestActiveTimeByOrder.entrySet()) {
            ActiveOrderKey key = entry.getKey();
            MesProWorkOrderDO workOrder = requireWorkOrder(workOrderMap, key.workOrderId());
            MesProRouteDO route = requireRoute(routeMap, key.routeId());
            requireProductRoute(workOrder, key.routeId());
            MesMdItemDO item = requireProduct(itemMap, workOrder.getProductId());
            candidates.add(new MesFrontlineActiveOrderCandidate(workOrder.getId(), workOrder.getCode(),
                    workOrder.getName(), workOrder.getProductId(), item.getCode(), item.getName(),
                    route.getId(), route.getCode(), route.getName(), entry.getValue()));
        }
        candidates.sort(Comparator
                .comparing(MesFrontlineActiveOrderCandidate::latestSubmitTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MesFrontlineActiveOrderCandidate::workOrderId));
        return candidates;
    }

    @Override
    public List<MesFrontlineRouteProcessCandidate> listProcessesByActiveOrder(Long workOrderId, Long routeId) {
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrder(workOrderId, routeId);
        MesProWorkOrderDO workOrder = requireWorkOrder(workOrderId);
        requireProductRoute(workOrder, routeId);
        MesProRouteDO route = requireRoute(routeId);
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        if (CollUtil.isEmpty(routeProcesses)) {
            throw exception(PRO_FRONTLINE_PQC_ROUTE_PROCESS_EMPTY, workOrderId, routeId);
        }
        Set<Long> processIds = routeProcesses.stream()
                .filter(Objects::nonNull)
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(processIds);
        Map<PqcTaskProcessKey, List<MesPqcInspectionTaskDO>> tasksByProcess = groupTasksByProcess(
                pqcTaskMapper.selectListByActiveOrderId(activeOrder.getId()));

        List<MesFrontlineRouteProcessCandidate> candidates = new ArrayList<>();
        for (MesProRouteProcessDO routeProcess : routeProcesses) {
            requireRouteProcessIdentity(routeProcess, routeId);
            MesProProcessDO process = processMap.get(routeProcess.getProcessId());
            if (process == null || !CommonStatusEnum.isEnable(process.getStatus())) {
                throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                        "processId=" + routeProcess.getProcessId());
            }
            MesFrontlinePqcTaskContext taskContext = resolvePqcTaskContext(activeOrder, workOrder, routeProcess,
                    tasksByProcess.get(new PqcTaskProcessKey(routeProcess.getId(), routeProcess.getProcessId())));
            if (taskContext == null) {
                continue;
            }
            candidates.add(new MesFrontlineRouteProcessCandidate(route.getId(), route.getCode(), route.getName(),
                    routeProcess.getId(), routeProcess.getProcessId(), process.getCode(), process.getName(),
                    routeProcess.getSort(), null, null, null, routeProcess.getWorkstationId(), null, null,
                    activeOrder.getId(), taskContext.task().getId(), taskContext.task().getRegulationVersionId(),
                    taskContext.task().getInspectionType(), taskContext.task().getBusinessDate(),
                    taskContext.task().getShiftCode(), taskContext.task().getRoundNo(),
                    taskContext.task().getPlannedInspectionQuantity(), taskContext.inspectionItems()));
        }
        if (candidates.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_TASK_REQUIRED, activeOrder.getId(), null, null);
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing((MesFrontlineRouteProcessCandidate candidate) ->
                                candidate.sort() == null ? Integer.MAX_VALUE : candidate.sort())
                        .thenComparing(MesFrontlineRouteProcessCandidate::routeProcessId))
                .toList();
    }

    @Override
    public List<MesFrontlineEmployeeCandidate> listPqcEmployeeCandidates() {
        List<MesProcessPoolTeamLeaderScopeDO> scopes = scopeMapper.selectActiveScopesByLeaderType(LEADER_TYPE_PQC);
        Set<Long> userIds = new LinkedHashSet<>();
        for (MesProcessPoolTeamLeaderScopeDO scope : scopes) {
            if (scope == null || !Boolean.TRUE.equals(scope.getEnabled())) {
                continue;
            }
            if (scope.getLeaderUserId() != null) {
                userIds.add(scope.getLeaderUserId());
            }
            if (SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType()) && scope.getEmployeeUserId() != null) {
                userIds.add(scope.getEmployeeUserId());
            }
        }
        if (userIds.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_PERSONNEL_EMPTY);
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds);
        if (CollUtil.isEmpty(users)) {
            throw exception(PRO_FRONTLINE_PQC_PERSONNEL_EMPTY);
        }
        Map<Long, MesFrontlineEmployeeCandidate> candidateByUserId = new LinkedHashMap<>();
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null || !CommonStatusEnum.isEnable(user.getStatus())) {
                continue;
            }
            candidateByUserId.putIfAbsent(user.getId(),
                    new MesFrontlineEmployeeCandidate(user.getId(), user.getUsername(), user.getNickname()));
        }
        if (candidateByUserId.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_PERSONNEL_EMPTY);
        }
        return candidateByUserId.values().stream()
                .sorted(Comparator
                        .comparing((MesFrontlineEmployeeCandidate candidate) -> displayName(candidate),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesFrontlineEmployeeCandidate::userId))
                .toList();
    }

    @Override
    public MesFrontlineRouteProcessCandidate requireActiveOrderProcess(Long workOrderId, Long routeId,
                                                                       Long routeProcessId, Long processId) {
        requireValue(routeProcessId, "routeProcessId");
        requireValue(processId, "processId");
        return listProcessesByActiveOrder(workOrderId, routeId).stream()
                .filter(candidate -> Objects.equals(candidate.routeProcessId(), routeProcessId)
                        && Objects.equals(candidate.processId(), processId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED, workOrderId, routeId));
    }

    @Override
    public MesFrontlineEmployeeCandidate requirePqcEmployee(Long actualEmployeeId) {
        requireValue(actualEmployeeId, "actualEmployeeId");
        return listPqcEmployeeCandidates().stream()
                .filter(candidate -> Objects.equals(candidate.userId(), actualEmployeeId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND, actualEmployeeId));
    }

    @Override
    public MesFrontlineEmployeeSwitchResult switchPqcActualEmployee(Long loginUserId, Long workOrderId, Long routeId,
                                                                    Long routeProcessId, Long processId,
                                                                    Long actualEmployeeId) {
        requireValue(loginUserId, "loginUserId");
        MesFrontlineRouteProcessCandidate process = requireActiveOrderProcess(workOrderId, routeId,
                routeProcessId, processId);
        requirePqcEmployee(actualEmployeeId);
        MesFrontlineTemplateDescriptor template = new MesFrontlineTemplateDescriptor(
                FrontlineTemplateCodes.PQC_SIMPLIFIED, FrontlineTemplateTypes.PQC,
                process.routeProcessId(), process.processId(), actualEmployeeId);
        return new MesFrontlineEmployeeSwitchResult(loginUserId, actualEmployeeId, process.routeId(),
                process.routeProcessId(), process.processId(), false, template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitPqcInspection(Long loginUserId, MesFrontlinePqcSubmitCommand command) {
        requireValue(loginUserId, "loginUserId");
        requirePqcSubmitCommand(command);
        String requestedInspectionResult = resolvePqcInspectionResult(command.getInspectionResult());
        String nonconformanceDescription = normalizeNonconformanceDescription(
                command.getNonconformanceDescription());
        requireNonconformanceDescriptionWhenFailed(requestedInspectionResult, nonconformanceDescription);
        if (!Objects.equals(command.getActualEmployeeId(), command.getSignatureEmployeeId())) {
            throw exception(PRO_FRONTLINE_SIGNATURE_EMPLOYEE_MISMATCH,
                    command.getActualEmployeeId(), command.getSignatureEmployeeId());
        }
        MesProProcessPoolEventDO sourceEvent = requireProductionSubmitEvent(command);
        Optional<Long> existingPqcTaskId = processPoolEventService.findExistingPqcInspectionTaskId(
                buildPqcInspectionLookup(command, sourceEvent));
        if (existingPqcTaskId.isPresent()) {
            return existingPqcTaskId.get();
        }
        MesPqcInspectionTaskDO task = pqcTaskMapper.selectById(command.getPqcTaskId());
        if (task != null && !PQC_TASK_STATUS_PENDING.equals(task.getTaskStatus())) {
            throw exception(PRO_FRONTLINE_PQC_TASK_STATUS_INVALID, task.getId(), task.getTaskStatus());
        }
        MesFrontlineRouteProcessCandidate process = requireActiveOrderProcess(command.getWorkOrderId(),
                command.getRouteId(), command.getRouteProcessId(), command.getProcessId());
        requirePqcEmployee(command.getActualEmployeeId());
        requirePqcTaskIdentity(task, command, process);
        List<MesPqcInspectionPieceDetailDO> pieceDetails = buildPieceDetails(task.getId(), command,
                process.inspectionItems());
        String inspectionResult = resolvePqcInspectionResult(requestedInspectionResult, pieceDetails);
        requireNonconformanceDescriptionWhenFailed(inspectionResult, nonconformanceDescription);
        String rawPayload = buildPqcInspectionEventRawPayload(command, pieceDetails,
                nonconformanceDescription);

        int updated = pqcTaskMapper.updateSubmittedIfPending(task.getId(), command.getActualInspectionQuantity(),
                PQC_TASK_STATUS_PENDING, PQC_TASK_STATUS_SUBMITTED);
        if (updated != 1) {
            throw exception(PRO_FRONTLINE_PQC_TASK_STATUS_INVALID, task.getId(), task.getTaskStatus());
        }
        pqcPieceDetailMapper.insertBatch(pieceDetails);
        processPoolEventService.createPqcInspectionEvent(MesProcessPoolCreatePqcInspectionReqDTO.builder()
                .workOrderId(command.getWorkOrderId())
                .productionSubmitEventId(command.getProductionSubmitEventId())
                .pqcSubmissionIdempotencyKey(command.getPqcSubmissionIdempotencyKey())
                .routeId(process.routeId())
                .routeProcessId(process.routeProcessId())
                .processId(process.processId())
                .actualEmployeeId(command.getActualEmployeeId())
                .deviceAccountId(sourceEvent.getDeviceAccountId())
                .deviceId(sourceEvent.getDeviceId())
                .workstationId(sourceEvent.getWorkstationId())
                .templateType(command.getTemplateType())
                .feedbackSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .feedbackSourceId(task.getId())
                .recordbookSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .recordbookSourceId(task.getId())
                .inspectionResult(inspectionResult)
                .rawPayload(rawPayload)
                .clientSubmitTime(command.getClientSubmitTime())
                .signatureId(command.getSignatureId())
                .signatureUserId(command.getSignatureEmployeeId())
                .signatureSnapshot(command.getSignatureSnapshot())
                .build());
        return task.getId();
    }

    private String buildPqcInspectionEventRawPayload(MesFrontlinePqcSubmitCommand command,
                                                     List<MesPqcInspectionPieceDetailDO> pieceDetails,
                                                     String nonconformanceDescription) {
        Map<String, Object> payload = new LinkedHashMap<>(command.getRawPayload());
        payload.put("activeOrderId", command.getActiveOrderId());
        payload.put("pqcTaskId", command.getPqcTaskId());
        payload.put("regulationVersionId", command.getRegulationVersionId());
        payload.put("workOrderId", command.getWorkOrderId());
        payload.put("routeId", command.getRouteId());
        payload.put("routeProcessId", command.getRouteProcessId());
        payload.put("processId", command.getProcessId());
        payload.put("inspectionType", command.getInspectionType());
        payload.put("businessDate", command.getBusinessDate());
        payload.put("shiftCode", command.getShiftCode());
        payload.put("roundNo", command.getRoundNo());
        payload.put("actualInspectionQuantity", command.getActualInspectionQuantity());
        if (nonconformanceDescription != null) {
            payload.put("nonconformanceDescription", nonconformanceDescription);
        }
        payload.put("pqcItemDetails", buildPqcItemDetailsSnapshot(pieceDetails));
        payload.put("pieceDetailCount", pieceDetails.size());
        return JsonUtils.toJsonString(payload);
    }

    private List<Map<String, Object>> buildPqcItemDetailsSnapshot(List<MesPqcInspectionPieceDetailDO> pieceDetails) {
        Map<String, Map<String, Object>> snapshotByItem = new LinkedHashMap<>();
        for (MesPqcInspectionPieceDetailDO detail : pieceDetails) {
            String itemCode = detail.getItemCode();
            Map<String, Object> item = snapshotByItem.computeIfAbsent(itemCode, key -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("itemCode", detail.getItemCode());
                value.put("itemName", detail.getItemName());
                value.put("selectedEquipmentId", detail.getSelectedEquipmentId());
                value.put("selectedEquipmentCode", detail.getSelectedEquipmentCode());
                value.put("selectedEquipmentName", detail.getSelectedEquipmentName());
                value.put("selectedEquipmentNumber", detail.getSelectedEquipmentNumber());
                value.put("standardText", detail.getStandardText());
                value.put("standardLowerLimit", detail.getStandardLowerLimit());
                value.put("standardUpperLimit", detail.getStandardUpperLimit());
                value.put("standardUnit", detail.getStandardUnit());
                value.put("standardPrecision", detail.getStandardPrecision());
                value.put("inspectionMethod", detail.getInspectionMethod());
                value.put("resultType", detail.getResultType());
                value.put("sampleValues", new ArrayList<String>());
                value.put("judgement", detail.getJudgement());
                return value;
            });
            @SuppressWarnings("unchecked")
            List<String> sampleValues = (List<String>) item.get("sampleValues");
            sampleValues.add(detail.getMeasuredValue());
        }
        return new ArrayList<>(snapshotByItem.values());
    }

    private MesProcessPoolCreatePqcInspectionReqDTO buildPqcInspectionLookup(
            MesFrontlinePqcSubmitCommand command, MesProProcessPoolEventDO sourceEvent) {
        return MesProcessPoolCreatePqcInspectionReqDTO.builder()
                .workOrderId(command.getWorkOrderId())
                .productionSubmitEventId(command.getProductionSubmitEventId())
                .pqcSubmissionIdempotencyKey(command.getPqcSubmissionIdempotencyKey())
                .routeId(command.getRouteId())
                .routeProcessId(command.getRouteProcessId())
                .processId(command.getProcessId())
                .actualEmployeeId(command.getActualEmployeeId())
                .deviceAccountId(sourceEvent.getDeviceAccountId())
                .deviceId(sourceEvent.getDeviceId())
                .workstationId(sourceEvent.getWorkstationId())
                .templateType(command.getTemplateType())
                .feedbackSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .feedbackSourceId(command.getPqcTaskId())
                .recordbookSourceType(PQC_INSPECTION_TASK_SOURCE_TYPE)
                .recordbookSourceId(command.getPqcTaskId())
                .inspectionResult(resolvePqcInspectionResult(command.getInspectionResult()))
                .clientSubmitTime(command.getClientSubmitTime())
                .signatureId(command.getSignatureId())
                .signatureUserId(command.getSignatureEmployeeId())
                .signatureSnapshot(command.getSignatureSnapshot())
                .build();
    }

    private void requirePqcSubmitCommand(MesFrontlinePqcSubmitCommand command) {
        if (command == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "request");
        }
        requirePositive(command.getActiveOrderId(), "activeOrderId");
        requirePositive(command.getPqcTaskId(), "pqcTaskId");
        requirePositive(command.getRegulationVersionId(), "regulationVersionId");
        requirePositive(command.getWorkOrderId(), "workOrderId");
        requirePositive(command.getProductionSubmitEventId(), "productionSubmitEventId");
        requirePositive(command.getRouteId(), "routeId");
        requirePositive(command.getRouteProcessId(), "routeProcessId");
        requirePositive(command.getProcessId(), "processId");
        requireText(command.getInspectionType(), "inspectionType");
        requireValue(command.getBusinessDate(), "businessDate");
        requireText(command.getShiftCode(), "shiftCode");
        requireValue(command.getRoundNo(), "roundNo");
        requireValue(command.getActualInspectionQuantity(), "actualInspectionQuantity");
        requirePositive(command.getActualEmployeeId(), "actualEmployeeId");
        requireText(command.getPqcSubmissionIdempotencyKey(), "pqcSubmissionIdempotencyKey");
        requirePositive(command.getSignatureId(), "signatureId");
        requirePositive(command.getSignatureEmployeeId(), "signatureEmployeeId");
        requireText(command.getTemplateType(), "templateType");
        requireText(command.getInspectionResult(), "inspectionResult");
        if (CollUtil.isEmpty(command.getRawPayload())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "rawPayload");
        }
    }

    private String resolvePqcInspectionResult(String inspectionResult) {
        if (FrontlinePqcResults.DETECTION_SUCCESS.equals(inspectionResult)
                || MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS.equals(inspectionResult)) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
        }
        if (FrontlinePqcResults.DETECTION_FAILED.equals(inspectionResult)
                || MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(inspectionResult)) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        }
        throw exception(PRO_PROCESS_POOL_PQC_RESULT_INVALID, inspectionResult);
    }

    private String resolvePqcInspectionResult(String requestedInspectionResult,
                                              List<MesPqcInspectionPieceDetailDO> pieceDetails) {
        if (pieceDetails.stream().anyMatch(detail ->
                MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(detail.getJudgement()))) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        }
        return requestedInspectionResult;
    }

    private String normalizeNonconformanceDescription(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private void requireNonconformanceDescriptionWhenFailed(String inspectionResult,
                                                            String nonconformanceDescription) {
        if (MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(inspectionResult)
                && StrUtil.isBlank(nonconformanceDescription)) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "nonconformanceDescription");
        }
    }

    private MesProProcessPoolEventDO requireProductionSubmitEvent(MesFrontlinePqcSubmitCommand command) {
        MesProProcessPoolEventDO sourceEvent =
                processPoolEventMapper.selectByIdForUpdate(command.getProductionSubmitEventId());
        if (sourceEvent == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "productionSubmitEventId");
        }
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(sourceEvent.getEventType())
                || !Objects.equals(sourceEvent.getWorkOrderId(), command.getWorkOrderId())
                || !Objects.equals(sourceEvent.getRouteId(), command.getRouteId())
                || !Objects.equals(sourceEvent.getRouteProcessId(), command.getRouteProcessId())
                || !Objects.equals(sourceEvent.getProcessId(), command.getProcessId())) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                    "productionSubmitEventId=" + command.getProductionSubmitEventId());
        }
        requirePositive(sourceEvent.getDeviceAccountId(), "productionSubmitEvent.deviceAccountId");
        requirePositive(sourceEvent.getDeviceId(), "productionSubmitEvent.deviceId");
        requirePositive(sourceEvent.getWorkstationId(), "productionSubmitEvent.workstationId");
        return sourceEvent;
    }

    private MesProcessPoolActiveOrderDO requireActiveOrder(Long workOrderId, Long routeId) {
        requireValue(workOrderId, "workOrderId");
        requireValue(routeId, "routeId");
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectActiveByWorkOrderAndRoute(workOrderId, routeId);
        if (activeOrder == null) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_REQUIRED, workOrderId, routeId);
        }
        requirePositive(activeOrder.getId(), "activeOrderId");
        requirePositive(activeOrder.getRouteVersionId(), "activeOrder.routeVersionId");
        return activeOrder;
    }

    private MesFrontlinePqcTaskContext resolvePqcTaskContext(MesProcessPoolActiveOrderDO activeOrder,
                                                             MesProWorkOrderDO workOrder,
                                                             MesProRouteProcessDO routeProcess,
                                                             List<MesPqcInspectionTaskDO> tasksForProcess) {
        MesQaInspectionRegulationDO regulation = regulationMapper.selectPublishedByRouteProcess(
                workOrder.getProductId(), activeOrder.getRouteId(), activeOrder.getRouteVersionId(),
                routeProcess.getId(), routeProcess.getProcessId());
        if (regulation == null || regulation.getCurrentVersionId() == null) {
            throw exception(PRO_FRONTLINE_PQC_REGULATION_REQUIRED,
                    activeOrder.getId(), routeProcess.getId(), routeProcess.getProcessId());
        }
        MesPqcInspectionTaskDO task = selectPendingTask(tasksForProcess);
        if (task == null && hasSubmittedTask(tasksForProcess)) {
            return null;
        }
        if (task == null || !Objects.equals(task.getRegulationVersionId(), regulation.getCurrentVersionId())) {
            throw exception(PRO_FRONTLINE_PQC_TASK_REQUIRED,
                    activeOrder.getId(), routeProcess.getId(), routeProcess.getProcessId());
        }
        Map<InspectionItemKey, List<MesFrontlinePqcInspectionItem.EquipmentOption>> equipmentOptionsByItem =
                regulationItemEquipmentMapper.selectListByVersionId(task.getRegulationVersionId()).stream()
                        .collect(Collectors.groupingBy(
                                row -> new InspectionItemKey(row.getInspectionType(), row.getItemCode()),
                                LinkedHashMap::new,
                                Collectors.mapping(MesFrontlinePqcContextServiceImpl::toEquipmentOption,
                                        Collectors.toList())));
        List<MesFrontlinePqcInspectionItem> items = regulationItemMapper
                .selectListByVersionId(task.getRegulationVersionId())
                .stream()
                .filter(item -> item != null && Objects.equals(item.getInspectionType(), task.getInspectionType()))
                .map(item -> toInspectionItem(item,
                        equipmentOptionsByItem.getOrDefault(
                                new InspectionItemKey(item.getInspectionType(), item.getItemCode()), List.of())))
                .toList();
        if (items.isEmpty()) {
            throw exception(PRO_FRONTLINE_PQC_REGULATION_REQUIRED,
                    activeOrder.getId(), routeProcess.getId(), routeProcess.getProcessId());
        }
        return new MesFrontlinePqcTaskContext(task, items);
    }

    private static Map<PqcTaskProcessKey, List<MesPqcInspectionTaskDO>> groupTasksByProcess(
            List<MesPqcInspectionTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return Map.of();
        }
        return tasks.stream()
                .filter(task -> task != null && task.getRouteProcessId() != null && task.getProcessId() != null)
                .collect(Collectors.groupingBy(task -> new PqcTaskProcessKey(task.getRouteProcessId(),
                                task.getProcessId()),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private static boolean hasSubmittedTask(List<MesPqcInspectionTaskDO> tasks) {
        return CollUtil.isNotEmpty(tasks) && tasks.stream()
                .anyMatch(task -> task != null && PQC_TASK_STATUS_SUBMITTED.equals(task.getTaskStatus()));
    }

    private static MesPqcInspectionTaskDO selectPendingTask(List<MesPqcInspectionTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return null;
        }
        return tasks.stream()
                .filter(task -> task != null && PQC_TASK_STATUS_PENDING.equals(task.getTaskStatus()))
                .min(Comparator
                        .comparing(MesPqcInspectionTaskDO::getBusinessDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MesPqcInspectionTaskDO::getInspectionType,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MesPqcInspectionTaskDO::getRoundNo,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MesPqcInspectionTaskDO::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private static MesFrontlinePqcInspectionItem toInspectionItem(MesQaInspectionRegulationItemDO item,
                                                                  List<MesFrontlinePqcInspectionItem.EquipmentOption>
                                                                          equipmentOptions) {
        if (item == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "inspectionItem");
        }
        boolean equipmentRequired = item.getEquipmentRequired() == null || Boolean.TRUE.equals(item.getEquipmentRequired());
        if (equipmentRequired && CollUtil.isEmpty(equipmentOptions)) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED,
                    "inspectionItem.equipmentOptions." + item.getItemCode());
        }
        return new MesFrontlinePqcInspectionItem(item.getItemCode(), item.getItemName(),
                item.getInspectionMethod(), item.getStandardText(), item.getStandardLowerLimit(),
                item.getStandardUpperLimit(), item.getStandardUnit(), item.getStandardPrecision(),
                equipmentRequired, item.getResultType(), List.copyOf(equipmentOptions));
    }

    private static MesFrontlinePqcInspectionItem.EquipmentOption toEquipmentOption(
            MesQaInspectionRegulationItemEquipmentDO row) {
        if (row == null || row.getEquipmentId() == null || StrUtil.isBlank(row.getEquipmentCode())
                || StrUtil.isBlank(row.getEquipmentName()) || StrUtil.isBlank(row.getEquipmentNumber())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "inspectionItem.equipmentOption");
        }
        return new MesFrontlinePqcInspectionItem.EquipmentOption(row.getEquipmentId(), row.getEquipmentCode(),
                row.getEquipmentName(), row.getEquipmentNumber(), row.getDefaultFlag(), row.getSort());
    }

    private void requirePqcTaskIdentity(MesPqcInspectionTaskDO task, MesFrontlinePqcSubmitCommand command,
                                        MesFrontlineRouteProcessCandidate process) {
        if (task == null) {
            throw exception(PRO_FRONTLINE_PQC_TASK_REQUIRED,
                    command.getActiveOrderId(), command.getRouteProcessId(), command.getProcessId());
        }
        if (!PQC_TASK_STATUS_PENDING.equals(task.getTaskStatus())) {
            throw exception(PRO_FRONTLINE_PQC_TASK_STATUS_INVALID, task.getId(), task.getTaskStatus());
        }
        if (!Objects.equals(task.getId(), command.getPqcTaskId())
                || !Objects.equals(task.getActiveOrderId(), command.getActiveOrderId())
                || !Objects.equals(task.getWorkOrderId(), command.getWorkOrderId())
                || !Objects.equals(task.getRouteId(), process.routeId())
                || !Objects.equals(task.getRouteProcessId(), process.routeProcessId())
                || !Objects.equals(task.getProcessId(), process.processId())
                || !Objects.equals(task.getRegulationVersionId(), command.getRegulationVersionId())
                || !Objects.equals(task.getInspectionType(), command.getInspectionType())
                || !Objects.equals(task.getBusinessDate(), command.getBusinessDate())
                || !Objects.equals(task.getShiftCode(), command.getShiftCode())
                || !Objects.equals(task.getRoundNo(), command.getRoundNo())) {
            throw exception(PRO_FRONTLINE_PQC_TASK_IDENTITY_MISMATCH, command.getPqcTaskId());
        }
        requirePqcTaskQuantity(task, command);
    }

    private void requirePqcTaskQuantity(MesPqcInspectionTaskDO task, MesFrontlinePqcSubmitCommand command) {
        Integer plannedQuantity = task.getPlannedInspectionQuantity();
        Integer actualQuantity = command.getActualInspectionQuantity();
        if (plannedQuantity == null || plannedQuantity <= 0 || actualQuantity == null || actualQuantity <= 0
                || !Objects.equals(plannedQuantity, actualQuantity)) {
            throw exception(PRO_FRONTLINE_PQC_TASK_QUANTITY_MISMATCH,
                    task.getId(), plannedQuantity, actualQuantity);
        }
    }

    private List<MesPqcInspectionPieceDetailDO> buildPieceDetails(Long taskId, MesFrontlinePqcSubmitCommand command,
                                                                  List<MesFrontlinePqcInspectionItem> inspectionItems) {
        if (CollUtil.isEmpty(command.getItemResults())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "itemResults");
        }
        Map<String, MesFrontlinePqcSubmitCommand.ItemResult> resultByItem = command.getItemResults().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(MesFrontlinePqcSubmitCommand.ItemResult::getItemCode,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<MesPqcInspectionPieceDetailDO> details = new ArrayList<>();
        for (MesFrontlinePqcInspectionItem item : inspectionItems) {
            requireText(item.itemCode(), "inspectionItem.itemCode");
            MesFrontlinePqcSubmitCommand.ItemResult itemResult = resultByItem.get(item.itemCode());
            if (itemResult == null) {
                throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "itemResults." + item.itemCode());
            }
            MesFrontlinePqcInspectionItem.EquipmentOption selectedEquipment =
                    resolveSelectedEquipment(item, itemResult);
            List<String> values = itemResult.getSampleValues();
            if (CollUtil.isEmpty(values) || values.size() != command.getActualInspectionQuantity()) {
                throw exception(PRO_FRONTLINE_PQC_TASK_QUANTITY_MISMATCH,
                        taskId, command.getActualInspectionQuantity(), values == null ? null : values.size());
            }
            for (int sampleIndex = 0; sampleIndex < command.getActualInspectionQuantity(); sampleIndex += 1) {
                String value = Objects.toString(values.get(sampleIndex), "").trim();
                if (StrUtil.isBlank(value)) {
                    throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED,
                            "itemResults." + item.itemCode() + ".sampleValues[" + sampleIndex + "]");
                }
                details.add(MesPqcInspectionPieceDetailDO.builder()
                        .taskId(taskId)
                        .sampleNo(sampleIndex + 1)
                        .itemCode(item.itemCode())
                        .itemName(item.itemName())
                        .inspectionMethod(item.inspectionMethod())
                        .standardText(item.standardText())
                        .selectedEquipmentId(selectedEquipment.equipmentId())
                        .selectedEquipmentCode(selectedEquipment.equipmentCode())
                        .selectedEquipmentName(selectedEquipment.equipmentName())
                        .selectedEquipmentNumber(selectedEquipment.equipmentNumber())
                        .standardLowerLimit(item.standardLowerLimit())
                        .standardUpperLimit(item.standardUpperLimit())
                        .standardUnit(item.standardUnit())
                        .standardPrecision(item.standardPrecision())
                        .resultType(item.resultType())
                        .itemResult(value)
                        .measuredValue(value)
                        .judgement(resolvePieceJudgement(value, command.getInspectionResult()))
                        .build());
            }
        }
        if (details.isEmpty()) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "pqcPieceDetails");
        }
        return details;
    }

    private MesFrontlinePqcInspectionItem.EquipmentOption resolveSelectedEquipment(
            MesFrontlinePqcInspectionItem item, MesFrontlinePqcSubmitCommand.ItemResult itemResult) {
        requirePositive(itemResult.getSelectedEquipmentId(), "itemResults." + item.itemCode() + ".selectedEquipmentId");
        requireText(itemResult.getSelectedEquipmentNumber(),
                "itemResults." + item.itemCode() + ".selectedEquipmentNumber");
        return item.equipmentOptions().stream()
                .filter(option -> Objects.equals(option.equipmentId(), itemResult.getSelectedEquipmentId())
                        && Objects.equals(option.equipmentNumber(), itemResult.getSelectedEquipmentNumber()))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED,
                        "itemResults." + item.itemCode() + ".selectedEquipmentNumber"));
    }

    private String resolvePieceJudgement(String value, String overallInspectionResult) {
        if ("不合格".equals(value)
                || FrontlinePqcResults.DETECTION_FAILED.equals(overallInspectionResult)
                || MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(overallInspectionResult)) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        }
        if ("合格".equals(value)
                || FrontlinePqcResults.DETECTION_SUCCESS.equals(overallInspectionResult)
                || MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS.equals(overallInspectionResult)) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
        }
        throw exception(PRO_PROCESS_POOL_PQC_RESULT_INVALID, overallInspectionResult);
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null || workOrder.getProductId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "workOrderId=" + workOrderId);
        }
        return workOrder;
    }

    private static MesProWorkOrderDO requireWorkOrder(Map<Long, MesProWorkOrderDO> workOrderMap, Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderMap.get(workOrderId);
        if (workOrder == null || workOrder.getProductId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "workOrderId=" + workOrderId);
        }
        return workOrder;
    }

    private MesProRouteDO requireRoute(Long routeId) {
        MesProRouteDO route = routeMapper.selectByIdIgnoreDeleted(routeId);
        if (route == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "routeId=" + routeId);
        }
        return route;
    }

    private static MesProRouteDO requireRoute(Map<Long, MesProRouteDO> routeMap, Long routeId) {
        MesProRouteDO route = routeMap.get(routeId);
        if (route == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "routeId=" + routeId);
        }
        return route;
    }

    private void requireProductRoute(MesProWorkOrderDO workOrder, Long routeId) {
        if (workOrder.getProductId() == null
                || routeProductMapper.selectByRouteIdAndItemId(routeId, workOrder.getProductId()) == null) {
            throw exception(PRO_FRONTLINE_PQC_ACTIVE_ORDER_ROUTE_REQUIRED,
                    workOrder.getId(), workOrder.getProductId(), routeId);
        }
    }

    private static MesMdItemDO requireProduct(Map<Long, MesMdItemDO> itemMap, Long productId) {
        MesMdItemDO item = itemMap.get(productId);
        if (item == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "productId=" + productId);
        }
        return item;
    }

    private static void requireActiveOrderIdentity(MesProcessPoolActiveOrderDO activeOrder) {
        if (activeOrder == null || activeOrder.getWorkOrderId() == null || activeOrder.getRouteId() == null) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "active order");
        }
    }

    private static void requireRouteProcessIdentity(MesProRouteProcessDO routeProcess, Long routeId) {
        if (routeProcess == null
                || routeProcess.getId() == null
                || routeProcess.getRouteId() == null
                || routeProcess.getProcessId() == null
                || !Objects.equals(routeProcess.getRouteId(), routeId)) {
            throw exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID, "route process");
        }
    }

    private static void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private static void requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private static boolean isAfter(LocalDateTime candidate, LocalDateTime current) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        return candidate.isAfter(current);
    }

    private static String displayName(MesFrontlineEmployeeCandidate candidate) {
        if (candidate.nickname() != null) {
            return candidate.nickname();
        }
        return candidate.username();
    }

    private static <T> Map<Long, T> mapById(Collection<T> rows, Function<T, Long> idGetter) {
        if (CollUtil.isEmpty(rows)) {
            return Map.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> idGetter.apply(row) != null)
                .collect(Collectors.toMap(idGetter, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private record ActiveOrderKey(Long workOrderId, Long routeId) {
    }

    private record PqcTaskProcessKey(Long routeProcessId, Long processId) {
    }

    private record InspectionItemKey(String inspectionType, String itemCode) {
    }

    private record MesFrontlinePqcTaskContext(MesPqcInspectionTaskDO task,
                                              List<MesFrontlinePqcInspectionItem> inspectionItems) {
    }
}
