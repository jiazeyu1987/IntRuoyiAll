package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderEventPartyReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;

@Service
@Validated
public class MesTeamLeaderActiveOrderDetailServiceImpl implements MesTeamLeaderActiveOrderDetailService {

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderDetailReadMapper detailReadMapper;
    private final MesFrontlineProcessMaterialService processMaterialService;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper;
    private final MesQaInspectionRegulationProcessMapper qaProcessMapper;
    private final ErpKingdeeProductionReplenishmentListItemMapper replenishmentListItemMapper;
    private final ErpKingdeeProductionReplenishmentListMapper replenishmentListMapper;
    private final MesMdItemMapper itemMapper;

    public MesTeamLeaderActiveOrderDetailServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                       MesProcessPoolActiveOrderDetailReadMapper detailReadMapper,
                                                       MesFrontlineProcessMaterialService processMaterialService,
                                                       MesPqcInspectionTaskMapper pqcTaskMapper,
                                                       MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper,
                                                       MesQaInspectionRegulationProcessMapper qaProcessMapper,
                                                       ErpKingdeeProductionReplenishmentListItemMapper replenishmentListItemMapper,
                                                       ErpKingdeeProductionReplenishmentListMapper replenishmentListMapper,
                                                       MesMdItemMapper itemMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.detailReadMapper = detailReadMapper;
        this.processMaterialService = processMaterialService;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pqcAggregateDetailMapper = pqcAggregateDetailMapper;
        this.qaProcessMapper = qaProcessMapper;
        this.replenishmentListItemMapper = replenishmentListItemMapper;
        this.replenishmentListMapper = replenishmentListMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public MesTeamLeaderActiveOrderDetail getDetail(Long leaderUserId, Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null
                || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)
                || !MesTeamLeaderActiveOrderServiceImpl.STATUS_ACTIVE.equals(activeOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetailReadDO> rows = detailReadMapper.selectByActiveOrderId(activeOrderId);
        if (rows == null || rows.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        MesTeamLeaderActiveOrderDetailReadDO first = rows.get(0);
        requireText(first.getWorkOrderCode(), activeOrderId);
        requireText(first.getRouteName(), activeOrderId);

        Map<ProcessIdentity, ProcessAccumulator> accumulators = new LinkedHashMap<>();
        for (MesTeamLeaderActiveOrderDetailReadDO row : rows) {
            validateProcessRow(row, activeOrderId);
            ProcessIdentity identity = new ProcessIdentity(row.getRouteProcessId(), row.getProcessId());
            ProcessAccumulator accumulator = accumulators.computeIfAbsent(identity,
                    ignored -> new ProcessAccumulator(row));
            accumulator.addSubmission(row, activeOrderId);
        }
        attachInputMaterials(activeOrder, activeOrderId, accumulators);
        attachSupplementMaterials(first.getWorkOrderCode(), activeOrderId, accumulators);
        attachPqcSubmissions(activeOrderId, accumulators);
        return new MesTeamLeaderActiveOrderDetail()
                .setActiveOrderId(activeOrderId)
                .setVersion(activeOrder.getVersion())
                .setWorkOrderId(first.getWorkOrderId())
                .setWorkOrderCode(first.getWorkOrderCode())
                .setBatchCode(first.getBatchCode())
                .setWorkOrderQuantity(first.getWorkOrderQuantity())
                .setDrawingNumber(first.getDrawingNumber())
                .setProductCode(first.getProductCode())
                .setProductName(first.getProductName())
                .setProductSpecification(first.getProductSpecification())
                .setWorkOrderCreateTime(first.getWorkOrderCreateTime())
                .setRouteName(first.getRouteName())
                .setProcesses(accumulators.values().stream().map(ProcessAccumulator::toDetail).toList());
    }

    private void attachInputMaterials(MesProcessPoolActiveOrderDO activeOrder, Long activeOrderId,
                                       Map<ProcessIdentity, ProcessAccumulator> accumulators) {
        for (ProcessAccumulator accumulator : accumulators.values()) {
            MesTeamLeaderActiveOrderDetail.ProcessDetail process = accumulator.process;
            List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> inputMaterials =
                    processMaterialService.listFrozenMaterials(activeOrderId, activeOrder.getRouteId(),
                                    process.getRouteProcessId(), process.getProcessId()).stream()
                            .filter(material -> MesFrontlineProcessMaterial.ROLE_INPUT.equals(material.materialRole()))
                            .map(MesTeamLeaderActiveOrderDetailServiceImpl::toInputMaterialDetail)
                            .toList();
            accumulator.setInputMaterials(inputMaterials);
        }
    }

    private void attachSupplementMaterials(String workOrderCode, Long activeOrderId,
                                           Map<ProcessIdentity, ProcessAccumulator> accumulators) {
        List<ErpKingdeeProductionReplenishmentListItemDO> items = replenishmentListItemMapper
                .selectListByProductionOrderNo(workOrderCode);
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, ErpKingdeeProductionReplenishmentListDO> headersById = loadReplenishmentHeaders(activeOrderId, items);
        Map<String, MesMdItemDO> materialByCode = loadMaterialByCode(activeOrderId, items);
        Map<String, List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail>> supplementsByMaterialCode =
                groupSupplementMaterials(activeOrderId, items, headersById, materialByCode);
        for (ProcessAccumulator accumulator : accumulators.values()) {
            Set<String> inputMaterialCodes = accumulator.inputMaterials.stream()
                    .map(MesTeamLeaderActiveOrderDetail.InputMaterialDetail::getMaterialCode)
                    .map(MesTeamLeaderActiveOrderDetailServiceImpl::trimToNull)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (inputMaterialCodes.isEmpty()) {
                accumulator.setSupplementMaterials(List.of());
                continue;
            }
            List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail> matched = inputMaterialCodes.stream()
                    .flatMap(code -> supplementsByMaterialCode.getOrDefault(code, List.of()).stream())
                    .toList();
            accumulator.setSupplementMaterials(matched);
        }
    }

    private Map<Long, ErpKingdeeProductionReplenishmentListDO> loadReplenishmentHeaders(
            Long activeOrderId, List<ErpKingdeeProductionReplenishmentListItemDO> items) {
        List<Long> headerIds = items.stream()
                .map(ErpKingdeeProductionReplenishmentListItemDO::getProductionReplenishmentListId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (headerIds.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<ErpKingdeeProductionReplenishmentListDO> headers = replenishmentListMapper.selectBatchIds(headerIds);
        Map<Long, ErpKingdeeProductionReplenishmentListDO> headersById = mapById(headers,
                ErpKingdeeProductionReplenishmentListDO::getId, activeOrderId, "replenishmentList");
        if (!headersById.keySet().containsAll(headerIds)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        for (ErpKingdeeProductionReplenishmentListDO header : headersById.values()) {
            requireText(header.getSourceBillNo(), activeOrderId);
            if (!"C".equals(header.getDocumentStatus())) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
        }
        return headersById;
    }

    private Map<String, MesMdItemDO> loadMaterialByCode(Long activeOrderId,
                                                        List<ErpKingdeeProductionReplenishmentListItemDO> items) {
        Set<String> materialCodes = items.stream()
                .map(ErpKingdeeProductionReplenishmentListItemDO::getMaterialNumber)
                .map(MesTeamLeaderActiveOrderDetailServiceImpl::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (materialCodes.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        Map<String, MesMdItemDO> result = new LinkedHashMap<>();
        for (String materialCode : materialCodes) {
            MesMdItemDO item = itemMapper.selectByCode(materialCode);
            if (item == null || item.getId() == null || trimToNull(item.getCode()) == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            result.put(materialCode, item);
        }
        return result;
    }

    private Map<String, List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail>> groupSupplementMaterials(
            Long activeOrderId,
            List<ErpKingdeeProductionReplenishmentListItemDO> items,
            Map<Long, ErpKingdeeProductionReplenishmentListDO> headersById,
            Map<String, MesMdItemDO> materialByCode) {
        Map<String, SupplementAccumulator> byMaterialAndSource = new LinkedHashMap<>();
        for (ErpKingdeeProductionReplenishmentListItemDO item : items) {
            String materialCode = trimToNull(item.getMaterialNumber());
            String lotNumber = trimToNull(item.getLotNumber());
            if (item.getId() == null || item.getProductionReplenishmentListId() == null
                    || materialCode == null || trimToNull(item.getMaterialName()) == null || lotNumber == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            ErpKingdeeProductionReplenishmentListDO header = headersById.get(item.getProductionReplenishmentListId());
            MesMdItemDO material = materialByCode.get(materialCode);
            if (header == null || material == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            String key = item.getProductionReplenishmentListId() + "|" + materialCode;
            byMaterialAndSource.computeIfAbsent(key, ignored -> new SupplementAccumulator(material, item, header))
                    .add(item, header);
        }
        Map<String, List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail>> byMaterialCode =
                new LinkedHashMap<>();
        for (SupplementAccumulator accumulator : byMaterialAndSource.values()) {
            MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail detail = accumulator.toDetail();
            byMaterialCode.computeIfAbsent(detail.getMaterialCode(), ignored -> new ArrayList<>()).add(detail);
        }
        return byMaterialCode;
    }

    private void attachPqcSubmissions(Long activeOrderId, Map<ProcessIdentity, ProcessAccumulator> accumulators) {
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderId(activeOrderId);
        if (tasks == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesPqcProcessInspectionAggregateDetailDO> details =
                pqcAggregateDetailMapper.selectListByActiveOrderId(activeOrderId);
        if (details == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        for (MesPqcProcessInspectionAggregateDetailDO detail : details) {
            if (detail == null || detail.getPqcTaskId() == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
        }
        Map<Long, List<MesPqcProcessInspectionAggregateDetailDO>> detailsByTask = details.stream()
                .collect(Collectors.groupingBy(MesPqcProcessInspectionAggregateDetailDO::getPqcTaskId,
                        LinkedHashMap::new, Collectors.toList()));
        Set<Long> taskIds = tasks.stream().map(MesPqcInspectionTaskDO::getId).collect(Collectors.toSet());
        for (Long pqcTaskId : detailsByTask.keySet()) {
            if (pqcTaskId == null || !taskIds.contains(pqcTaskId)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
        }
        Map<Long, MesQaInspectionRegulationProcessDO> qaProcessesById = mapById(
                qaProcessMapper.selectBatchIds(distinctIds(tasks, MesPqcInspectionTaskDO::getQaProcessId)),
                MesQaInspectionRegulationProcessDO::getId, activeOrderId, "qaProcess");
        Map<Long, MesTeamLeaderActiveOrderEventPartyReadDO> eventPartiesById =
                loadEventParties(activeOrderId, tasks);
        Map<PqcSubmissionIdentity, PqcSubmissionAccumulator> pqcSubmissionAccumulators = new LinkedHashMap<>();
        for (MesPqcInspectionTaskDO task : tasks) {
            if (task == null || task.getId() == null || task.getRouteProcessId() == null
                    || task.getProcessId() == null || task.getQaProcessId() == null
                    || trimToNull(task.getQaItemCode()) == null
                    || trimToNull(task.getInspectionRuleKey()) == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            MesQaInspectionRegulationProcessDO qaProcess = qaProcessesById.get(task.getQaProcessId());
            if (qaProcess == null || !Objects.equals(qaProcess.getRegulationVersionId(), task.getRegulationVersionId())) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            requireText(qaProcess.getProcessName(), activeOrderId);
            List<MesPqcProcessInspectionAggregateDetailDO> taskDetails =
                    detailsByTask.getOrDefault(task.getId(), List.of());
            if (task.getSubmittedEventId() == null && taskDetails.isEmpty()) {
                continue;
            }
            ProcessAccumulator accumulator = accumulators.get(
                    new ProcessIdentity(task.getRouteProcessId(), task.getProcessId()));
            if (accumulator == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            PqcSubmissionIdentity submissionIdentity = new PqcSubmissionIdentity(
                    new ProcessIdentity(task.getRouteProcessId(), task.getProcessId()),
                    task.getQaProcessId(), trimToNull(task.getQaItemCode()), task.getInspectionRuleKey());
            pqcSubmissionAccumulators.computeIfAbsent(submissionIdentity,
                            ignored -> new PqcSubmissionAccumulator(task, qaProcess))
                    .add(task, taskDetails, eventPartiesById.get(task.getSubmittedEventId()), activeOrderId);
        }
        for (Map.Entry<PqcSubmissionIdentity, PqcSubmissionAccumulator> entry : pqcSubmissionAccumulators.entrySet()) {
            ProcessAccumulator accumulator = accumulators.get(entry.getKey().processIdentity());
            if (accumulator == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            accumulator.addPqcSubmission(entry.getValue().toDetail());
        }
    }

    private Map<Long, MesTeamLeaderActiveOrderEventPartyReadDO> loadEventParties(
            Long activeOrderId, List<MesPqcInspectionTaskDO> tasks) {
        List<Long> eventIds = tasks.stream()
                .map(MesPqcInspectionTaskDO::getSubmittedEventId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        List<MesTeamLeaderActiveOrderEventPartyReadDO> parties =
                detailReadMapper.selectEventPartiesByEventIds(eventIds);
        Map<Long, MesTeamLeaderActiveOrderEventPartyReadDO> partiesById = mapById(parties,
                MesTeamLeaderActiveOrderEventPartyReadDO::getEventId, activeOrderId, "pqcEventParty");
        if (!partiesById.keySet().containsAll(eventIds)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        for (MesTeamLeaderActiveOrderEventPartyReadDO party : partiesById.values()) {
            requireText(party.getSubmitterName(), activeOrderId);
        }
        return partiesById;
    }

    private static MesTeamLeaderActiveOrderDetail.InputMaterialDetail toInputMaterialDetail(
            MesFrontlineProcessMaterial material) {
        return new MesTeamLeaderActiveOrderDetail.InputMaterialDetail()
                .setMaterialId(material.materialId())
                .setMaterialCode(material.materialCode())
                .setMaterialName(material.materialName())
                .setMaterialSpecification(material.materialSpecification())
                .setBatchCodes(List.copyOf(material.batchCodes()))
                .setRequestedQuantity(material.requestedQuantity())
                .setActualQuantity(material.actualQuantity())
                .setBaseActualQuantity(material.baseActualQuantity())
                .setSourcePickListIds(List.copyOf(material.sourcePickListIds()))
                .setSourcePickListNos(List.copyOf(material.sourcePickListNos()))
                .setSourcePickListItemIds(List.copyOf(material.sourcePickListItemIds()))
                .setSourceSnapshotHash(material.sourceSnapshotHash());
    }

    private static MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail toPqcSubmissionItemDetail(
            MesPqcProcessInspectionAggregateDetailDO detail) {
        return new MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail()
                .setAggregateDetailId(detail.getId())
                .setSampleNo(detail.getSampleNo())
                .setItemCode(detail.getItemCode())
                .setItemName(detail.getItemName())
                .setInspectionMethod(detail.getInspectionMethod())
                .setStandardText(detail.getStandardText())
                .setMeasuredValue(detail.getMeasuredValue())
                .setItemResult(detail.getItemResult())
                .setJudgement(detail.getJudgement())
                .setSelectedEquipmentName(detail.getSelectedEquipmentName())
                .setSelectedEquipmentNumber(detail.getSelectedEquipmentNumber());
    }

    private static void validateProcessRow(MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        if (row == null || row.getSnapshotId() == null || row.getRouteProcessId() == null || row.getProcessId() == null
                || row.getRequiredQuantity() == null || row.getRequiredQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        requireText(row.getProcessName(), activeOrderId);
    }

    private static void requireText(String value, Long activeOrderId) {
        if (value == null || value.isBlank()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private static <T> List<Long> distinctIds(List<T> rows, Function<T, Long> idGetter) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .map(idGetter)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static <T> Map<Long, T> mapById(List<T> rows, Function<T, Long> idGetter,
                                             Long activeOrderId, String sourceName) {
        if (rows == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        try {
            return rows.stream().collect(Collectors.toMap(idGetter, Function.identity(),
                    (left, right) -> {
                        throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
                    }, LinkedHashMap::new));
        } catch (NullPointerException exception) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record PqcSubmissionIdentity(ProcessIdentity processIdentity, Long qaProcessId, String qaItemCode,
                                         String inspectionRuleKey) {
    }

    private static final class SupplementAccumulator {
        private final MesMdItemDO material;
        private final String materialName;
        private final String materialSpecification;
        private final LinkedHashSet<String> batchCodes = new LinkedHashSet<>();
        private final LinkedHashSet<Long> replenishmentListIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> replenishmentListNos = new LinkedHashSet<>();
        private final LinkedHashSet<Long> replenishmentListItemIds = new LinkedHashSet<>();
        private BigDecimal requestedQuantity = BigDecimal.ZERO;
        private BigDecimal actualQuantity = BigDecimal.ZERO;
        private BigDecimal baseActualQuantity = BigDecimal.ZERO;

        private SupplementAccumulator(MesMdItemDO material, ErpKingdeeProductionReplenishmentListItemDO firstItem,
                                      ErpKingdeeProductionReplenishmentListDO firstHeader) {
            this.material = material;
            this.materialName = trimToNull(firstItem.getMaterialName());
            this.materialSpecification = trimToNull(firstItem.getMaterialSpecification());
            add(firstItem, firstHeader);
        }

        private void add(ErpKingdeeProductionReplenishmentListItemDO item,
                         ErpKingdeeProductionReplenishmentListDO header) {
            batchCodes.add(trimToNull(item.getLotNumber()));
            replenishmentListIds.add(item.getProductionReplenishmentListId());
            replenishmentListNos.add(trimToNull(header.getSourceBillNo()));
            replenishmentListItemIds.add(item.getId());
            requestedQuantity = requestedQuantity.add(zeroIfNull(item.getRequestedQuantity()));
            actualQuantity = actualQuantity.add(zeroIfNull(item.getActualQuantity()));
            baseActualQuantity = baseActualQuantity.add(zeroIfNull(item.getBaseActualQuantity()));
        }

        private MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail toDetail() {
            return new MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail()
                    .setMaterialId(material.getId())
                    .setMaterialCode(trimToNull(material.getCode()))
                    .setMaterialName(materialName)
                    .setMaterialSpecification(materialSpecification)
                    .setBatchCodes(batchCodes.stream().filter(Objects::nonNull).sorted().toList())
                    .setRequestedQuantity(requestedQuantity)
                    .setActualQuantity(actualQuantity)
                    .setBaseActualQuantity(baseActualQuantity)
                    .setSourceReplenishmentListIds(replenishmentListIds.stream().sorted().toList())
                    .setSourceReplenishmentListNos(replenishmentListNos.stream().filter(Objects::nonNull).sorted().toList())
                    .setSourceReplenishmentListItemIds(replenishmentListItemIds.stream().sorted().toList());
        }

        private static BigDecimal zeroIfNull(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private static final class PqcSubmissionAccumulator {
        private final MesPqcInspectionTaskDO firstTask;
        private final MesQaInspectionRegulationProcessDO qaProcess;
        private final LinkedHashSet<Long> pqcTaskIds = new LinkedHashSet<>();
        private final LinkedHashSet<Long> submittedEventIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> submitterNames = new LinkedHashSet<>();
        private final LinkedHashSet<String> reviewerNames = new LinkedHashSet<>();
        private final List<MesTeamLeaderActiveOrderDetail.SignatureDetail> submitterSignatures = new ArrayList<>();
        private final List<MesTeamLeaderActiveOrderDetail.SignatureDetail> reviewerSignatures = new ArrayList<>();
        private final List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> items = new ArrayList<>();
        private Integer actualInspectionQuantity;
        private Integer scrapQuantity;

        private PqcSubmissionAccumulator(MesPqcInspectionTaskDO firstTask,
                                         MesQaInspectionRegulationProcessDO qaProcess) {
            this.firstTask = firstTask;
            this.qaProcess = qaProcess;
        }

        private void add(MesPqcInspectionTaskDO task, List<MesPqcProcessInspectionAggregateDetailDO> details,
                         MesTeamLeaderActiveOrderEventPartyReadDO eventParty, Long activeOrderId) {
            pqcTaskIds.add(task.getId());
            if (task.getSubmittedEventId() != null) {
                submittedEventIds.add(task.getSubmittedEventId());
                if (eventParty == null) {
                    throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
                }
                submitterNames.add(trimToNull(eventParty.getSubmitterName()));
                reviewerNames.add(trimToNull(eventParty.getReviewerName()));
                addSignature(submitterSignatures, eventParty.getSubmitterSignatureId(),
                        eventParty.getSubmitterName(), eventParty.getSubmitterSignedAt(), "PQC_SUBMIT");
                addSignature(reviewerSignatures, eventParty.getReviewerSignatureId(),
                        eventParty.getReviewerName(), eventParty.getReviewerSignedAt(), "FORM_REVIEW");
                scrapQuantity = addScrapQuantity(scrapQuantity, eventParty.getScrapQuantity());
            }
            if (actualInspectionQuantity == null
                    || (task.getActualInspectionQuantity() != null
                    && task.getActualInspectionQuantity() > actualInspectionQuantity)) {
                actualInspectionQuantity = task.getActualInspectionQuantity();
            }
            details.stream()
                    .sorted(Comparator
                            .comparing(MesPqcProcessInspectionAggregateDetailDO::getSampleNo,
                                    Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getItemCode,
                                    Comparator.nullsLast(String::compareTo))
                            .thenComparing(MesPqcProcessInspectionAggregateDetailDO::getId,
                                    Comparator.nullsLast(Long::compareTo)))
                    .map(MesTeamLeaderActiveOrderDetailServiceImpl::toPqcSubmissionItemDetail)
                    .forEach(items::add);
        }

        private MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail toDetail() {
            List<Long> taskIds = List.copyOf(pqcTaskIds);
            List<Long> eventIds = List.copyOf(submittedEventIds);
            return new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail()
                    .setPqcTaskId(taskIds.isEmpty() ? null : taskIds.get(0))
                    .setPqcTaskIds(taskIds)
                    .setSubmittedEventId(eventIds.isEmpty() ? null : eventIds.get(0))
                    .setSubmittedEventIds(eventIds)
                    .setQaProcessId(qaProcess.getId())
                    .setQaProcessCode(qaProcess.getProcessCode())
                    .setQaProcessName(qaProcess.getProcessName())
                    .setQaItemCode(firstTask.getQaItemCode())
                    .setInspectionRuleKey(firstTask.getInspectionRuleKey())
                    .setInspectionType(firstTask.getInspectionType())
                    .setBusinessDate(firstTask.getBusinessDate())
                    .setShiftCode(firstTask.getShiftCode())
                    .setRoundNo(firstTask.getRoundNo())
                    .setActualInspectionQuantity(actualInspectionQuantity)
                    .setScrapQuantity(scrapQuantity)
                    .setTaskStatus(firstTask.getTaskStatus())
                    .setSubmitterName(joinDistinctTexts(submitterNames))
                    .setReviewerName(joinDistinctTexts(reviewerNames))
                    .setSubmitterSignatures(List.copyOf(submitterSignatures))
                    .setReviewerSignatures(List.copyOf(reviewerSignatures))
                    .setItems(List.copyOf(items));
        }
    }

    private static Integer addScrapQuantity(Integer current, Integer next) {
        if (next == null) {
            return current;
        }
        return (current == null ? 0 : current) + next;
    }

    private static void addSignature(List<MesTeamLeaderActiveOrderDetail.SignatureDetail> signatures,
                                     Long signatureId, String signerName, LocalDateTime signedAt, String role) {
        if (signatureId == null) {
            return;
        }
        boolean existed = signatures.stream()
                .anyMatch(signature -> Objects.equals(signature.getSignatureId(), signatureId));
        if (existed) {
            return;
        }
        signatures.add(toSignatureDetail(signatureId, signerName, signedAt, role));
    }

    private static MesTeamLeaderActiveOrderDetail.SignatureDetail toSignatureDetail(
            Long signatureId, String signerName, LocalDateTime signedAt, String role) {
        if (signatureId == null) {
            return null;
        }
        return new MesTeamLeaderActiveOrderDetail.SignatureDetail()
                .setSignatureId(signatureId)
                .setSignerName(trimToNull(signerName))
                .setSignedAt(signedAt)
                .setRole(role);
    }

    private static String joinDistinctTexts(Set<String> values) {
        String joined = values.stream()
                .map(MesTeamLeaderActiveOrderDetailServiceImpl::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("、"));
        return joined.isBlank() ? null : joined;
    }

    private static final class ProcessAccumulator {
        private final MesTeamLeaderActiveOrderDetail.ProcessDetail process;
        private final List<MesTeamLeaderActiveOrderDetail.SubmissionDetail> submissions = new ArrayList<>();
        private final List<MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail> pqcSubmissions = new ArrayList<>();
        private List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> inputMaterials = List.of();
        private List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail> supplementMaterials = List.of();
        private BigDecimal submittedQuantity = BigDecimal.ZERO;

        private ProcessAccumulator(MesTeamLeaderActiveOrderDetailReadDO row) {
            this.process = new MesTeamLeaderActiveOrderDetail.ProcessDetail()
                    .setRouteProcessId(row.getRouteProcessId())
                    .setProcessId(row.getProcessId())
                    .setProcessCode(row.getProcessCode())
                    .setProcessName(row.getProcessName())
                    .setKeyFlag(Boolean.TRUE.equals(row.getKeyFlag()))
                    .setRequiredQuantity(row.getRequiredQuantity());
        }

        private void addSubmission(MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
            if (row.getEventId() == null) {
                return;
            }
            if (row.getSubmittedQuantity() == null || row.getSubmittedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            requireText(row.getSubmitterName(), activeOrderId);
            if (row.getSubmittedAt() == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            submissions.add(new MesTeamLeaderActiveOrderDetail.SubmissionDetail()
                    .setEventId(row.getEventId())
                    .setSubmittedQuantity(row.getSubmittedQuantity())
                    .setSubmitterName(row.getSubmitterName())
                    .setReviewerName(row.getReviewerName())
                    .setSubmittedAt(row.getSubmittedAt())
                    .setSubmitterSignature(toSignatureDetail(row.getSubmitterSignatureId(),
                            row.getSubmitterName(), row.getSubmitterSignedAt(), "PRODUCTION_SUBMIT"))
                    .setReviewerSignature(toSignatureDetail(row.getReviewerSignatureId(),
                            row.getReviewerName(), row.getReviewerSignedAt(), "FORM_REVIEW"))
                    .setDevices(resolveSubmissionDevices(row, activeOrderId))
                    .setDeviceParameters(resolveSubmissionDeviceParameters(row, activeOrderId))
                    .setClearanceConfirmations(resolveClearanceConfirmations(row, activeOrderId))
                    .setMaterials(resolveSubmissionMaterials(row, activeOrderId)));
            submittedQuantity = submittedQuantity.add(row.getSubmittedQuantity());
        }

        private void setInputMaterials(List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> inputMaterials) {
            this.inputMaterials = List.copyOf(inputMaterials);
        }

        private void setSupplementMaterials(
                List<MesTeamLeaderActiveOrderDetail.SupplementMaterialDetail> supplementMaterials) {
            this.supplementMaterials = List.copyOf(supplementMaterials);
        }

        private void addPqcSubmission(MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail submission) {
            this.pqcSubmissions.add(submission);
        }

        private MesTeamLeaderActiveOrderDetail.ProcessDetail toDetail() {
            BigDecimal overageQuantity = submittedQuantity.subtract(process.getRequiredQuantity());
            if (overageQuantity.compareTo(BigDecimal.ZERO) < 0) {
                overageQuantity = BigDecimal.ZERO;
            }
            boolean quantityConflict = overageQuantity.compareTo(BigDecimal.ZERO) > 0;
            submissions.forEach(submission -> submission.setQuantityConflict(quantityConflict));
            return process
                    .setSubmittedQuantity(submittedQuantity)
                    .setSubmissionCount(submissions.size())
                    .setQuantityConflict(quantityConflict)
                    .setOverageQuantity(overageQuantity)
                    .setInputMaterials(inputMaterials)
                    .setSupplementMaterials(supplementMaterials)
                    .setSubmissions(List.copyOf(submissions))
                    .setPqcSubmissions(List.copyOf(pqcSubmissions));
        }
    }

    private static List<MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> resolveSubmissionDevices(
            MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices = new LinkedHashMap<>();
        Map<?, ?> payload = parseOriginalPayload(row.getOriginalPayloadJson(), activeOrderId);
        if (payload != null) {
            addDevicesFromValue(devices, payload.get("selectedDevice"), activeOrderId);
            addDevicesFromValue(devices, payload.get("selectedDevices"), activeOrderId);
            addDevicesFromMaterialDetails(devices, payload.get("materialDetails"), activeOrderId);
            addDevicesFromValue(devices, payload.get("deviceParameterReadings"), activeOrderId);
            addDeviceMeteringValidityFromValue(devices, payload.get("deviceMeteringValidity"), activeOrderId);
        }
        addDevice(devices, row.getEventDeviceId(), row.getEventDeviceCode(), row.getEventDeviceName());
        return List.copyOf(devices.values());
    }

    private static List<MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail> resolveSubmissionMaterials(
            MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        Map<?, ?> payload = parseOriginalPayload(row.getOriginalPayloadJson(), activeOrderId);
        if (payload == null) {
            return List.of();
        }
        Object value = payload.get("materialDetails");
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> materialDetails)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail> rows = new ArrayList<>();
        for (Object materialDetail : materialDetails) {
            if (!(materialDetail instanceof Map<?, ?> detail)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices = new LinkedHashMap<>();
            addDevicesFromValue(devices, detail.get("selectedDevice"), activeOrderId);
            addDevicesFromValue(devices, detail.get("selectedDevices"), activeOrderId);
            addDevicesFromValue(devices, detail.get("deviceParameterReadings"), activeOrderId);
            addDeviceMeteringValidityFromValue(devices, detail.get("deviceMeteringValidity"), activeOrderId);
            rows.add(new MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail()
                    .setMaterialId(requirePositiveLongValue(detail.get("materialId"), activeOrderId))
                    .setMaterialCode(requireStringValue(detail.get("materialCode"), activeOrderId))
                    .setMaterialName(requireStringValue(detail.get("materialName"), activeOrderId))
                    .setMaterialSpecification(stringValue(detail.get("materialSpecification")))
                    .setOutputQuantity(bigDecimalValue(detail.get("outputQuantity"), activeOrderId))
                    .setLossQuantity(bigDecimalValue(detail.get("lossQuantity"), activeOrderId))
                    .setDevices(List.copyOf(devices.values()))
                    .setDeviceParameters(resolveSubmissionDeviceParameters(detail.get("deviceParameterReadings"),
                            activeOrderId))
                    .setClearanceConfirmations(resolveClearanceConfirmations(detail.get("clearanceConfirmations"),
                            activeOrderId)));
        }
        return List.copyOf(rows);
    }

    private static List<MesTeamLeaderActiveOrderDetail.SubmissionDeviceParameterDetail>
    resolveSubmissionDeviceParameters(MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        Map<?, ?> payload = parseOriginalPayload(row.getOriginalPayloadJson(), activeOrderId);
        if (payload == null) {
            return List.of();
        }
        return resolveSubmissionDeviceParameters(payload.get("deviceParameterReadings"), activeOrderId);
    }

    private static List<MesTeamLeaderActiveOrderDetail.SubmissionDeviceParameterDetail>
    resolveSubmissionDeviceParameters(Object value, Long activeOrderId) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> readings)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetail.SubmissionDeviceParameterDetail> rows = new ArrayList<>();
        for (Object reading : readings) {
            if (!(reading instanceof Map<?, ?> detail)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            rows.add(new MesTeamLeaderActiveOrderDetail.SubmissionDeviceParameterDetail()
                    .setDeviceId(longValue(detail.get("deviceId"), activeOrderId))
                    .setDeviceCode(stringValue(detail.get("deviceCode")))
                    .setDeviceName(stringValue(detail.get("deviceName")))
                    .setParameterCode(requireStringValue(detail.get("parameterCode"), activeOrderId))
                    .setParameterName(stringValue(detail.get("parameterName")))
                    .setUnit(stringValue(detail.get("unit")))
                    .setValue(bigDecimalValueOrNull(detail.get("value"), activeOrderId))
                    .setTextValue(firstNonBlankString(detail.get("textValue"), detail.get("value")))
                    .setLowerLimit(bigDecimalValueOrNull(detail.get("lowerLimit"), activeOrderId))
                    .setUpperLimit(bigDecimalValueOrNull(detail.get("upperLimit"), activeOrderId))
                    .setParameterStatus(stringValue(detail.get("parameterStatus"))));
        }
        return List.copyOf(rows);
    }

    private static List<MesTeamLeaderActiveOrderDetail.ClearanceConfirmationDetail> resolveClearanceConfirmations(
            MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        Map<?, ?> payload = parseOriginalPayload(row.getOriginalPayloadJson(), activeOrderId);
        if (payload == null) {
            return List.of();
        }
        return resolveClearanceConfirmations(payload.get("clearanceConfirmations"), activeOrderId);
    }

    private static List<MesTeamLeaderActiveOrderDetail.ClearanceConfirmationDetail> resolveClearanceConfirmations(
            Object value, Long activeOrderId) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> confirmations)) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetail.ClearanceConfirmationDetail> rows = new ArrayList<>();
        for (Object confirmation : confirmations) {
            if (!(confirmation instanceof Map<?, ?> detail)) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            rows.add(new MesTeamLeaderActiveOrderDetail.ClearanceConfirmationDetail()
                    .setKey(requireStringValue(detail.get("key"), activeOrderId))
                    .setLabel(requireStringValue(detail.get("label"), activeOrderId))
                    .setConfirmed(booleanValue(detail.get("confirmed"), activeOrderId))
                    .setDescription(stringValue(detail.get("description"))));
        }
        return List.copyOf(rows);
    }

    private static Map<?, ?> parseOriginalPayload(String originalPayloadJson, Long activeOrderId) {
        if (originalPayloadJson == null || originalPayloadJson.isBlank()) {
            return null;
        }
        try {
            Map<?, ?> payload = JsonUtils.parseObject(originalPayloadJson, Map.class);
            if (payload == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            return payload;
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private static void addDevicesFromValue(
            Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices, Object value,
            Long activeOrderId) {
        if (value instanceof List<?> rows) {
            for (Object item : rows) {
                addDevicesFromValue(devices, item, activeOrderId);
            }
            return;
        }
        if (value instanceof Map<?, ?> row) {
            addDevice(devices, longValue(row.get("deviceId"), activeOrderId), stringValue(row.get("deviceCode")),
                    stringValue(row.get("deviceName")), optionalBooleanValue(row.get("inMeteringValidityPeriod"), activeOrderId));
        }
    }

    private static void addDeviceMeteringValidityFromValue(
            Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices, Object value,
            Long activeOrderId) {
        if (value instanceof List<?> rows) {
            for (Object item : rows) {
                addDeviceMeteringValidityFromValue(devices, item, activeOrderId);
            }
            return;
        }
        if (value instanceof Map<?, ?> row && row.get("inMeteringValidityPeriod") instanceof Boolean inPeriod) {
            addDevice(devices, longValue(row.get("deviceId"), activeOrderId), stringValue(row.get("deviceCode")),
                    stringValue(row.get("deviceName")), inPeriod);
        }
    }

    private static void addDevicesFromMaterialDetails(
            Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices, Object value,
            Long activeOrderId) {
        if (!(value instanceof List<?> materialDetails)) {
            return;
        }
        for (Object materialDetail : materialDetails) {
            if (!(materialDetail instanceof Map<?, ?> detail)) {
                continue;
            }
            addDevicesFromValue(devices, detail.get("selectedDevice"), activeOrderId);
            addDevicesFromValue(devices, detail.get("selectedDevices"), activeOrderId);
            addDevicesFromValue(devices, detail.get("deviceParameterReadings"), activeOrderId);
            addDeviceMeteringValidityFromValue(devices, detail.get("deviceMeteringValidity"), activeOrderId);
        }
    }

    private static void addDevice(Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices,
                                  Long deviceId, String deviceCode, String deviceName) {
        addDevice(devices, deviceId, deviceCode, deviceName, null);
    }

    private static void addDevice(Map<String, MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail> devices,
                                  Long deviceId, String deviceCode, String deviceName,
                                  Boolean inMeteringValidityPeriod) {
        String normalizedCode = trimToNull(deviceCode);
        String normalizedName = trimToNull(deviceName);
        String key = deviceKey(deviceId, normalizedCode, normalizedName);
        if (key == null) {
            return;
        }
        MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail existing = devices.get(key);
        if (existing == null) {
            devices.put(key, new MesTeamLeaderActiveOrderDetail.SubmissionDeviceDetail()
                    .setDeviceId(deviceId)
                    .setDeviceCode(normalizedCode)
                    .setDeviceName(normalizedName)
                    .setInMeteringValidityPeriod(inMeteringValidityPeriod));
            return;
        }
        if (existing.getDeviceCode() == null && normalizedCode != null) {
            existing.setDeviceCode(normalizedCode);
        }
        if (existing.getDeviceName() == null && normalizedName != null) {
            existing.setDeviceName(normalizedName);
        }
        if (existing.getInMeteringValidityPeriod() == null && inMeteringValidityPeriod != null) {
            existing.setInMeteringValidityPeriod(inMeteringValidityPeriod);
        }
    }

    private static String deviceKey(Long deviceId, String deviceCode, String deviceName) {
        if (deviceId != null && deviceId > 0) {
            return "id:" + deviceId;
        }
        if (deviceCode != null) {
            return "code:" + deviceCode;
        }
        if (deviceName != null) {
            return "name:" + deviceName;
        }
        return null;
    }

    private static Long longValue(Object value, Long activeOrderId) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private static Long requirePositiveLongValue(Object value, Long activeOrderId) {
        Long parsed = longValue(value, activeOrderId);
        if (parsed == null || parsed <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return parsed;
    }

    private static String stringValue(Object value) {
        return trimToNull(value);
    }

    private static String requireStringValue(Object value, Long activeOrderId) {
        String text = trimToNull(value);
        if (text == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        return text;
    }

    private static BigDecimal bigDecimalValue(Object value, Long activeOrderId) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = trimToNull(value);
        if (text == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private static BigDecimal bigDecimalValueOrNull(Object value, Long activeOrderId) {
        if (value == null) {
            return null;
        }
        String text = trimToNull(value);
        if (text == null || "true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return null;
        }
        return bigDecimalValue(value, activeOrderId);
    }

    private static Boolean booleanValue(Object value, Long activeOrderId) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = trimToNull(value);
        if (text == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
    }

    private static Boolean optionalBooleanValue(Object value, Long activeOrderId) {
        if (value == null) {
            return null;
        }
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        return booleanValue(value, activeOrderId);
    }

    private static String firstNonBlankString(Object... values) {
        for (Object value : values) {
            String text = trimToNull(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
