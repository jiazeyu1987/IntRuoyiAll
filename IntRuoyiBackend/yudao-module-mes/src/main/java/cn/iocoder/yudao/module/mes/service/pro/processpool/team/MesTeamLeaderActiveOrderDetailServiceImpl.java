package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public MesTeamLeaderActiveOrderDetailServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                      MesProcessPoolActiveOrderDetailReadMapper detailReadMapper,
                                                      MesFrontlineProcessMaterialService processMaterialService,
                                                      MesPqcInspectionTaskMapper pqcTaskMapper,
                                                      MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.detailReadMapper = detailReadMapper;
        this.processMaterialService = processMaterialService;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pqcAggregateDetailMapper = pqcAggregateDetailMapper;
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
        attachPqcSubmissions(activeOrderId, accumulators);
        return new MesTeamLeaderActiveOrderDetail()
                .setActiveOrderId(activeOrderId)
                .setWorkOrderId(first.getWorkOrderId())
                .setWorkOrderCode(first.getWorkOrderCode())
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

    private void attachPqcSubmissions(Long activeOrderId, Map<ProcessIdentity, ProcessAccumulator> accumulators) {
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderId(activeOrderId);
        List<MesPqcProcessInspectionAggregateDetailDO> details =
                pqcAggregateDetailMapper.selectListByActiveOrderId(activeOrderId);
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
        Map<PqcSubmissionIdentity, PqcSubmissionAccumulator> pqcSubmissionAccumulators = new LinkedHashMap<>();
        for (MesPqcInspectionTaskDO task : tasks) {
            if (task == null || task.getId() == null || task.getRouteProcessId() == null
                    || task.getProcessId() == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
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
                    task.getInspectionType(), task.getRoundNo());
            pqcSubmissionAccumulators.computeIfAbsent(submissionIdentity,
                            ignored -> new PqcSubmissionAccumulator(task))
                    .add(task, taskDetails);
        }
        for (Map.Entry<PqcSubmissionIdentity, PqcSubmissionAccumulator> entry : pqcSubmissionAccumulators.entrySet()) {
            ProcessAccumulator accumulator = accumulators.get(entry.getKey().processIdentity());
            if (accumulator == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            accumulator.addPqcSubmission(entry.getValue().toDetail());
        }
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

    private static MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail toPqcSubmissionDetail(
            MesPqcInspectionTaskDO task, List<MesPqcProcessInspectionAggregateDetailDO> details) {
        return new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail()
                .setPqcTaskId(task.getId())
                .setSubmittedEventId(task.getSubmittedEventId())
                .setInspectionType(task.getInspectionType())
                .setBusinessDate(task.getBusinessDate())
                .setShiftCode(task.getShiftCode())
                .setRoundNo(task.getRoundNo())
                .setActualInspectionQuantity(task.getActualInspectionQuantity())
                .setTaskStatus(task.getTaskStatus())
                .setItems(details.stream()
                        .map(MesTeamLeaderActiveOrderDetailServiceImpl::toPqcSubmissionItemDetail)
                        .toList());
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

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record PqcSubmissionIdentity(ProcessIdentity processIdentity, String inspectionType, Integer roundNo) {
    }

    private static final class PqcSubmissionAccumulator {
        private final MesPqcInspectionTaskDO firstTask;
        private final LinkedHashSet<Long> pqcTaskIds = new LinkedHashSet<>();
        private final LinkedHashSet<Long> submittedEventIds = new LinkedHashSet<>();
        private final List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> items = new ArrayList<>();
        private Integer actualInspectionQuantity;

        private PqcSubmissionAccumulator(MesPqcInspectionTaskDO firstTask) {
            this.firstTask = firstTask;
        }

        private void add(MesPqcInspectionTaskDO task, List<MesPqcProcessInspectionAggregateDetailDO> details) {
            pqcTaskIds.add(task.getId());
            if (task.getSubmittedEventId() != null) {
                submittedEventIds.add(task.getSubmittedEventId());
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
                    .setInspectionType(firstTask.getInspectionType())
                    .setBusinessDate(firstTask.getBusinessDate())
                    .setShiftCode(firstTask.getShiftCode())
                    .setRoundNo(firstTask.getRoundNo())
                    .setActualInspectionQuantity(actualInspectionQuantity)
                    .setTaskStatus(firstTask.getTaskStatus())
                    .setItems(List.copyOf(items));
        }
    }

    private static final class ProcessAccumulator {
        private final MesTeamLeaderActiveOrderDetail.ProcessDetail process;
        private final List<MesTeamLeaderActiveOrderDetail.SubmissionDetail> submissions = new ArrayList<>();
        private final List<MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail> pqcSubmissions = new ArrayList<>();
        private List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> inputMaterials = List.of();
        private BigDecimal submittedQuantity = BigDecimal.ZERO;

        private ProcessAccumulator(MesTeamLeaderActiveOrderDetailReadDO row) {
            this.process = new MesTeamLeaderActiveOrderDetail.ProcessDetail()
                    .setRouteProcessId(row.getRouteProcessId())
                    .setProcessId(row.getProcessId())
                    .setProcessCode(row.getProcessCode())
                    .setProcessName(row.getProcessName())
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
                    .setSubmittedAt(row.getSubmittedAt()));
            submittedQuantity = submittedQuantity.add(row.getSubmittedQuantity());
        }

        private void setInputMaterials(List<MesTeamLeaderActiveOrderDetail.InputMaterialDetail> inputMaterials) {
            this.inputMaterials = List.copyOf(inputMaterials);
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
                    .setSubmissions(List.copyOf(submissions))
                    .setPqcSubmissions(List.copyOf(pqcSubmissions));
        }
    }
}
