package cn.iocoder.yudao.module.mes.service.pro.feedback;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportAttributeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportBatchSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportConfirmBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackSurplusAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackSurplusPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackImportRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackSurplusAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackSurplusPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportPayload;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportAttributeReqVO.TARGET_TYPE_CURRENT_ORDER;
import static cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportAttributeReqVO.TARGET_TYPE_EXTERNAL_OTHER_ORDER;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_PENDING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MesProFeedbackImportRecordServiceImpl implements MesProFeedbackImportRecordService {

    private static final String MODIFY_BLOCK_LINK_INCOMPLETE = "LINK_INCOMPLETE";
    private static final String MODIFY_BLOCK_FEEDBACK_NOT_PREPARE = "FEEDBACK_NOT_PREPARE";
    private static final String MODIFY_BLOCK_POOL_CONSUMED = "POOL_CONSUMED";

    @Resource
    private MesProFeedbackImportRecordMapper importRecordMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesMdWorkstationMapper workstationMapper;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Resource
    private MesProFeedbackService feedbackService;
    @Resource
    private MesProFeedbackMapper feedbackMapper;
    @Resource
    private MesProScheduleOrderService scheduleOrderService;
    @Resource
    private MesProFeedbackSurplusPoolMapper surplusPoolMapper;
    @Resource
    private MesProFeedbackSurplusAllocationMapper surplusAllocationMapper;

    @Override
    public PageResult<MesProFeedbackImportRecordRespVO> getImportRecordPage(MesProFeedbackImportRecordPageReqVO reqVO) {
        PageResult<MesProFeedbackImportRecordDO> pageResult = importRecordMapper.selectPage(reqVO);
        Map<Long, LinkedFeedbackSnapshot> snapshotMap = buildLinkedFeedbackSnapshotMap(pageResult.getList());
        List<MesProFeedbackImportRecordRespVO> list = pageResult.getList().stream()
                .map(record -> toImportRecordResp(record, snapshotMap.get(record.getId())))
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public MesProFeedbackImportBatchSummaryRespVO getImportRecordBatchSummary(Collection<Long> importRecordIds) {
        return buildBatchSummary(importRecordMapper.selectListByIds(importRecordIds),
                buildLinkedFeedbackSnapshotMap(importRecordMapper.selectListByIds(importRecordIds)));
    }

    @Override
    public Map<Long, MesProFeedbackImportRecordDO> getImportRecordMapByFeedbackIds(Collection<Long> feedbackIds) {
        if (CollUtil.isEmpty(feedbackIds)) {
            return Map.of();
        }
        return importRecordMapper.selectListByFeedbackIds(feedbackIds).stream()
                .filter(record -> record.getFeedbackId() != null)
                .collect(LinkedHashMap::new,
                        (map, record) -> map.putIfAbsent(record.getFeedbackId(), record),
                        Map::putAll);
    }

    @Override
    public Map<Long, MesProFeedbackImportRecordDO> getImportRecordMapByFeedbacks(Collection<MesProFeedbackDO> feedbacks) {
        if (CollUtil.isEmpty(feedbacks)) {
            return Map.of();
        }
        Map<Long, MesProFeedbackImportRecordDO> result = new LinkedHashMap<>();
        List<MesProFeedbackDO> linkedBySourceImportRecord = feedbacks.stream()
                .filter(feedback -> feedback.getId() != null && feedback.getSourceImportRecordId() != null)
                .toList();
        if (CollUtil.isNotEmpty(linkedBySourceImportRecord)) {
            Map<Long, MesProFeedbackImportRecordDO> importRecordById = importRecordMapper.selectListByIds(
                            linkedBySourceImportRecord.stream()
                                    .map(MesProFeedbackDO::getSourceImportRecordId)
                                    .distinct()
                                    .toList())
                    .stream()
                    .collect(LinkedHashMap::new, (map, record) -> map.put(record.getId(), record), Map::putAll);
            linkedBySourceImportRecord.forEach(feedback -> {
                MesProFeedbackImportRecordDO importRecord = importRecordById.get(feedback.getSourceImportRecordId());
                if (importRecord != null) {
                    result.putIfAbsent(feedback.getId(), importRecord);
                }
            });
        }
        List<Long> unresolvedFeedbackIds = feedbacks.stream()
                .map(MesProFeedbackDO::getId)
                .filter(Objects::nonNull)
                .filter(feedbackId -> !result.containsKey(feedbackId))
                .toList();
        if (CollUtil.isNotEmpty(unresolvedFeedbackIds)) {
            getImportRecordMapByFeedbackIds(unresolvedFeedbackIds).forEach(result::putIfAbsent);
        }
        return result;
    }

    @Override
    public List<MesProFeedbackImportCandidateRespVO> getAttributionCandidates(Long importRecordId) {
        MesProFeedbackImportRecordDO importRecord = validateImportRecordExists(importRecordId);
        ThirdPartyFeedbackImportPayload payload = parsePayload(importRecord);
        List<MesProProcessDO> importProcessList = loadImportProcesses(payload);
        if (CollUtil.isEmpty(importProcessList)) {
            return List.of();
        }
        ModifyEligibility modifyEligibility = null;
        Map<Long, BigDecimal> selectedQuantityByProcessId = new LinkedHashMap<>();
        BigDecimal externalOtherSelectedQuantity = BigDecimal.ZERO;
        List<Long> importProcessIds = importProcessList.stream().map(MesProProcessDO::getId).distinct().toList();
        BigDecimal processSurplusPoolQuantity = sumAvailableQuantityByProcessIds(importProcessIds);
        if (StrUtil.equals(importRecord.getAttributionStatus(), ATTRIBUTION_STATUS_ATTRIBUTED)) {
            modifyEligibility = evaluateModifyEligibility(importRecord);
            if (!modifyEligibility.canModify()) {
                throwModifyBlockedException(modifyEligibility);
            }
            selectedQuantityByProcessId = toSelectedQuantityByProcessId(modifyEligibility.linkedFeedbacks());
            externalOtherSelectedQuantity = sumAllocatedQuantity(filterAllocationsByTargetType(
                    modifyEligibility.importRecordAllocations(), MesProFeedbackSurplusAllocationDO.TARGET_TYPE_EXTERNAL_OTHER_ORDER));
            processSurplusPoolQuantity = processSurplusPoolQuantity
                    .subtract(modifyEligibility.ownedAvailablePoolQuantity())
                    .add(modifyEligibility.restorableConsumedQuantity())
                    .max(BigDecimal.ZERO);
        }
        List<MesProScheduleOrderProcessDO> processList =
                scheduleOrderProcessMapper.selectListByProcessIdsOrZeroSnapshots(importProcessIds);
        if (processList.isEmpty()) {
            List<MesProFeedbackImportCandidateRespVO> candidates = List.of(
                    buildExternalOtherOrderCandidate(importRecord, payload, importProcessList, processSurplusPoolQuantity));
            if (modifyEligibility != null) {
                candidates.get(0).setSelectedQuantity(externalOtherSelectedQuantity);
            }
            return candidates;
        }
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = toScheduleOrderMap(processList);
        Map<Long, Long> scheduleProcessIdentityProcessIdMap =
                resolveScheduleProcessIdentityProcessIdMap(processList, scheduleOrderMap);
        Map<Long, Long> processIdentityMap = resolveProcessIdentityMap(
                collectPositiveProcessIds(importProcessIds, scheduleProcessIdentityProcessIdMap.values()));
        List<Long> importProcessIdentityIds = importProcessIds;
        processList = processList.stream()
                .filter(processDO -> importProcessIdentityIds.stream().anyMatch(importProcessId ->
                        isSameProcessIdentity(importProcessId,
                                scheduleProcessIdentityProcessIdMap.getOrDefault(processDO.getId(), processDO.getProcessId()),
                                processIdentityMap))
                        || StrUtil.equals(payload.getProcessCode(), processDO.getProcessCode()))
                .toList();
        Map<Long, MesProWorkOrderDO> workOrderMap = toWorkOrderMap(scheduleOrderMap.values());
        Map<Long, MesMdItemDO> itemMap = toItemMap(workOrderMap.values());
        Map<Long, MesProTaskDO> taskByScheduleOrderProcessId = resolveActiveTaskByScheduleOrderProcessId(
                processList, scheduleOrderMap, payload.getTaskCode(), scheduleProcessIdentityProcessIdMap, processIdentityMap);

        List<MesProFeedbackImportCandidateRespVO> candidates = new ArrayList<>();
        for (MesProScheduleOrderProcessDO processDO : processList) {
            if (!hasRemainingQuantity(processDO)) {
                continue;
            }
            MesProScheduleOrderDO scheduleOrder = scheduleOrderMap.get(processDO.getScheduleOrderId());
            if (!isAttributable(scheduleOrder)) {
                continue;
            }
            MesProWorkOrderDO workOrder = workOrderMap.get(scheduleOrder.getWorkOrderId());
            MesMdItemDO item = workOrder == null ? null : itemMap.get(workOrder.getProductId());
            MesProTaskDO task = taskByScheduleOrderProcessId.get(processDO.getId());
            if (task == null) {
                continue;
            }
            MesProFeedbackImportCandidateRespVO candidate = new MesProFeedbackImportCandidateRespVO();
            candidate.setScheduleOrderId(scheduleOrder.getId());
            candidate.setScheduleOrderCode(scheduleOrder.getCode());
            candidate.setScheduleOrderProcessId(processDO.getId());
            candidate.setWorkOrderId(scheduleOrder.getWorkOrderId());
            candidate.setWorkOrderCode(scheduleOrder.getErpWorkOrderCode());
            candidate.setProductId(scheduleOrder.getProductId());
            candidate.setItemCode(item == null ? null : item.getCode());
            candidate.setItemName(item == null ? null : item.getName());
            candidate.setSpecification(item == null ? null : item.getSpecification());
            candidate.setProcessId(scheduleProcessIdentityProcessIdMap.getOrDefault(processDO.getId(), processDO.getProcessId()));
            candidate.setProcessCode(StrUtil.blankToDefault(processDO.getProcessCode(), payload.getProcessCode()));
            candidate.setProcessName(StrUtil.blankToDefault(processDO.getProcessName(), payload.getProcessName()));
            candidate.setPlannedQuantity(processDO.getPlannedQuantity());
            candidate.setReportedQuantity(processDO.getReportedQuantity());
            candidate.setRemainingQuantity(processDO.getRemainingQuantity());
            candidate.setTaskId(task.getId());
            candidate.setTaskCode(task.getCode());
            candidate.setExactWorkOrderMatch(StrUtil.isNotBlank(payload.getWorkOrderCode())
                    && StrUtil.equals(payload.getWorkOrderCode(), scheduleOrder.getErpWorkOrderCode()));
            candidate.setTargetType(TARGET_TYPE_CURRENT_ORDER);
            candidate.setExternalOtherOrder(false);
            candidate.setTargetOrderLabel(scheduleOrder.getCode());
            candidate.setTargetProductLabel(item == null ? null : item.getName());
            candidate.setOverproduceQuantity(calculateOverproduceQuantity(payload.getFeedbackQuantity(), processDO.getRemainingQuantity()));
            candidate.setSurplusPoolQuantity(processSurplusPoolQuantity);
            candidate.setAvailableFeedbackQuantity(payload.getFeedbackQuantity().add(processSurplusPoolQuantity));
            candidate.setSelectedQuantity(selectedQuantityByProcessId.get(processDO.getId()));
            candidates.add(candidate);
        }
        MesProFeedbackImportCandidateRespVO externalOtherOrderCandidate =
                buildExternalOtherOrderCandidate(importRecord, payload, importProcessList, processSurplusPoolQuantity);
        externalOtherOrderCandidate.setSelectedQuantity(externalOtherSelectedQuantity);
        candidates.add(externalOtherOrderCandidate);
        candidates.sort(Comparator
                .comparing((MesProFeedbackImportCandidateRespVO candidate) ->
                        !StrUtil.equals(candidate.getTargetType(), TARGET_TYPE_CURRENT_ORDER))
                .thenComparing(candidate -> !Boolean.TRUE.equals(candidate.getExactWorkOrderMatch()))
                .thenComparing(candidate -> StrUtil.nullToDefault(candidate.getScheduleOrderCode(), ""))
                .thenComparing(candidate -> StrUtil.nullToDefault(candidate.getWorkOrderCode(), ""))
                .thenComparing(candidate -> candidate.getScheduleOrderProcessId() == null
                        ? Long.MAX_VALUE : candidate.getScheduleOrderProcessId()));
        return candidates;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long attributeImportRecord(MesProFeedbackImportAttributeReqVO reqVO) {
        MesProFeedbackImportRecordDO importRecord = validateImportRecordPending(reqVO.getImportRecordId());
        return applyAttribution(importRecord, parsePayload(importRecord), normalizeAttributionAllocations(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reattributeImportRecord(MesProFeedbackImportAttributeReqVO reqVO) {
        MesProFeedbackImportRecordDO importRecord = validateImportRecordExists(reqVO.getImportRecordId());
        if (StrUtil.equals(importRecord.getAttributionStatus(), ATTRIBUTION_STATUS_PENDING)) {
            throw exception(PRO_FEEDBACK_IMPORT_REATTRIBUTION_NOT_ALLOWED);
        }
        ModifyEligibility modifyEligibility = evaluateModifyEligibility(importRecord);
        if (!modifyEligibility.canModify()) {
            throwModifyBlockedException(modifyEligibility);
        }
        rollbackExistingAttribution(importRecord, modifyEligibility);
        return applyAttribution(importRecord, parsePayload(importRecord), normalizeAttributionAllocations(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmImportRecordBatch(MesProFeedbackImportConfirmBatchReqVO reqVO) {
        List<MesProFeedbackImportRecordDO> importRecords = importRecordMapper.selectListByIds(reqVO.getImportRecordIds());
        if (CollUtil.isEmpty(importRecords)) {
            throw exception(PRO_FEEDBACK_IMPORT_RECORD_NOT_EXISTS);
        }
        Map<Long, MesProFeedbackImportRecordDO> importRecordMap = importRecords.stream()
                .collect(Collectors.toMap(MesProFeedbackImportRecordDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, LinkedFeedbackSnapshot> snapshotMap = buildLinkedFeedbackSnapshotMap(importRecords);
        Map<Long, MesProFeedbackImportConfirmBatchReqVO.Row> rowMap = reqVO.getRows() == null ? Map.of() : reqVO.getRows().stream()
                .collect(Collectors.toMap(MesProFeedbackImportConfirmBatchReqVO.Row::getImportRecordId,
                        item -> item, (left, right) -> right, LinkedHashMap::new));

        List<String> pendingRows = new ArrayList<>();
        List<String> requiredFieldMissingRows = new ArrayList<>();
        List<String> linkIncompleteRows = new ArrayList<>();
        List<String> feedbackNotPrepareRows = new ArrayList<>();
        List<BatchConfirmSubmission> submissions = new ArrayList<>();

        for (MesProFeedbackImportRecordDO importRecord : importRecords) {
            LinkedFeedbackSnapshot snapshot = snapshotMap.get(importRecord.getId());
            if (StrUtil.equals(importRecord.getAttributionStatus(), ATTRIBUTION_STATUS_PENDING)) {
                pendingRows.add(describeImportRecord(importRecord));
                continue;
            }
            if (isExternalOtherOrderRecord(importRecord, snapshot)) {
                continue;
            }
            if (snapshot == null || CollUtil.isEmpty(snapshot.linkedFeedbacks())) {
                linkIncompleteRows.add(describeImportRecord(importRecord));
                continue;
            }
            if (!snapshot.allPrepare()) {
                feedbackNotPrepareRows.add(describeImportRecord(importRecord));
                continue;
            }
            MesProFeedbackImportConfirmBatchReqVO.Row row = rowMap.get(importRecord.getId());
            if (row == null
                    || row.getFeedbackUserId() == null
                    || row.getFeedbackTime() == null
                    || row.getApproveUserId() == null) {
                requiredFieldMissingRows.add(describeImportRecord(importRecord));
                continue;
            }
            submissions.add(new BatchConfirmSubmission(importRecord, snapshot.linkedFeedbacks(), row));
        }

        if (CollUtil.isNotEmpty(pendingRows)) {
            throw exception(PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_PENDING_EXISTS, String.join("、", pendingRows));
        }
        if (CollUtil.isNotEmpty(linkIncompleteRows)) {
            throw exception(PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_LINK_INCOMPLETE, String.join("、", linkIncompleteRows));
        }
        if (CollUtil.isNotEmpty(feedbackNotPrepareRows)) {
            throw exception(PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_FEEDBACK_NOT_PREPARE, String.join("、", feedbackNotPrepareRows));
        }
        if (CollUtil.isNotEmpty(requiredFieldMissingRows)) {
            throw exception(PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_REQUIRED_FIELD_MISSING, String.join("、", requiredFieldMissingRows));
        }

        for (BatchConfirmSubmission submission : submissions) {
            for (MesProFeedbackDO linkedFeedback : submission.linkedFeedbacks()) {
                feedbackMapper.updateById(MesProFeedbackDO.builder()
                        .id(linkedFeedback.getId())
                        .feedbackUserId(submission.row().getFeedbackUserId())
                        .feedbackTime(submission.row().getFeedbackTime())
                        .approveUserId(submission.row().getApproveUserId())
                        .remark(submission.row().getRemark())
                        .build());
            }
        }
        for (BatchConfirmSubmission submission : submissions) {
            for (MesProFeedbackDO linkedFeedback : submission.linkedFeedbacks()) {
                feedbackService.submitFeedback(linkedFeedback.getId(), true);
            }
        }
    }

    private List<MesProFeedbackImportAttributeReqVO.Allocation> normalizeAttributionAllocations(
            MesProFeedbackImportAttributeReqVO reqVO) {
        if (CollUtil.isNotEmpty(reqVO.getAllocations())) {
            return reqVO.getAllocations();
        }
        return List.of(new MesProFeedbackImportAttributeReqVO.Allocation()
                .setTargetType(reqVO.getTargetType())
                .setScheduleOrderId(reqVO.getScheduleOrderId())
                .setScheduleOrderProcessId(reqVO.getScheduleOrderProcessId())
                .setFeedbackQuantity(reqVO.getFeedbackQuantity()));
    }

    private BigDecimal resolveAvailableFeedbackQuantity(ThirdPartyFeedbackImportPayload payload) {
        BigDecimal importFeedbackQuantity = payload.getFeedbackQuantity() == null ? BigDecimal.ZERO : payload.getFeedbackQuantity();
        return importFeedbackQuantity.add(sumAvailableQuantityByProcessIds(loadImportProcessIds(payload)));
    }

    private void validateAllocationTotal(BigDecimal availableFeedbackQuantity,
                                         List<MesProFeedbackImportAttributeReqVO.Allocation> allocations) {
        if (CollUtil.isEmpty(allocations)) {
            throw exception(PRO_FEEDBACK_IMPORT_ATTRIBUTION_TARGET_INVALID);
        }
        BigDecimal totalQuantity = allocations.stream()
                .map(MesProFeedbackImportAttributeReqVO.Allocation::getFeedbackQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalQuantity.compareTo(availableFeedbackQuantity) > 0) {
            throw exception(PRO_FEEDBACK_IMPORT_ATTRIBUTION_TARGET_INVALID);
        }
    }

    private Long applyAttribution(MesProFeedbackImportRecordDO importRecord,
                                  ThirdPartyFeedbackImportPayload payload,
                                  List<MesProFeedbackImportAttributeReqVO.Allocation> allocations) {
        BigDecimal availableFeedbackQuantity = resolveAvailableFeedbackQuantity(payload);
        validateAllocationTotal(availableFeedbackQuantity, allocations);
        Long firstFeedbackId = 0L;
        AttributionResult firstCurrentResult = null;
        String finalTargetType = TARGET_TYPE_EXTERNAL_OTHER_ORDER;
        for (MesProFeedbackImportAttributeReqVO.Allocation allocation : allocations) {
            AttributionResult result = attributeImportRecordAllocation(importRecord, payload, allocation);
            if (result.feedbackId() != null && result.feedbackId() > 0 && firstFeedbackId == 0L) {
                firstFeedbackId = result.feedbackId();
            }
            if (StrUtil.equals(result.targetType(), TARGET_TYPE_CURRENT_ORDER) && firstCurrentResult == null) {
                firstCurrentResult = result;
            }
            if (StrUtil.equals(result.targetType(), TARGET_TYPE_CURRENT_ORDER)) {
                finalTargetType = TARGET_TYPE_CURRENT_ORDER;
            }
        }
        consumeSurplusPoolIfNeeded(importRecord, payload, allocations);
        createResidualSurplusPool(importRecord, payload, allocations);
        MesProFeedbackImportRecordDO.MesProFeedbackImportRecordDOBuilder updateBuilder = MesProFeedbackImportRecordDO.builder()
                .id(importRecord.getId())
                .attributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED)
                .feedbackId(firstFeedbackId > 0 ? firstFeedbackId : null)
                .attributionTargetType(finalTargetType);
        if (allocations.size() == 1 && firstCurrentResult != null) {
            updateBuilder.scheduleOrderId(firstCurrentResult.scheduleOrderId())
                    .scheduleOrderProcessId(firstCurrentResult.scheduleOrderProcessId());
        } else {
            updateBuilder.scheduleOrderId(null).scheduleOrderProcessId(null);
        }
        importRecordMapper.updateById(updateBuilder.build());
        return firstFeedbackId;
    }

    private void createResidualSurplusPool(MesProFeedbackImportRecordDO importRecord,
                                           ThirdPartyFeedbackImportPayload payload,
                                           List<MesProFeedbackImportAttributeReqVO.Allocation> allocations) {
        BigDecimal allocatedQuantity = allocations.stream()
                .map(MesProFeedbackImportAttributeReqVO.Allocation::getFeedbackQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal importFeedbackQuantity = payload.getFeedbackQuantity() == null ? BigDecimal.ZERO : payload.getFeedbackQuantity();
        BigDecimal residualQuantity = importFeedbackQuantity.subtract(allocatedQuantity).max(BigDecimal.ZERO);
        if (residualQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        MesProProcessDO process = resolveResidualSurplusPoolProcess(payload, allocations);
        MesProFeedbackSurplusPoolDO pool = buildBaseSurplusPool(importRecord, payload)
                .setSourceType(MesProFeedbackSurplusPoolDO.SOURCE_TYPE_EXTERNAL_OTHER_ORDER)
                .setProcessId(process.getId())
                .setTotalQuantity(residualQuantity)
                .setAllocatedQuantity(BigDecimal.ZERO)
                .setAvailableQuantity(residualQuantity)
                .setStatus(MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE);
        surplusPoolMapper.insert(pool);
    }

    private void consumeSurplusPoolIfNeeded(MesProFeedbackImportRecordDO importRecord,
                                            ThirdPartyFeedbackImportPayload payload,
                                            List<MesProFeedbackImportAttributeReqVO.Allocation> allocations) {
        BigDecimal importFeedbackQuantity = payload.getFeedbackQuantity() == null ? BigDecimal.ZERO : payload.getFeedbackQuantity();
        BigDecimal allocatedQuantity = allocations.stream()
                .map(MesProFeedbackImportAttributeReqVO.Allocation::getFeedbackQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal quantityToConsume = allocatedQuantity.subtract(importFeedbackQuantity).max(BigDecimal.ZERO);
        if (quantityToConsume.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<Long> importProcessIds = loadImportProcessIds(payload);
        if (CollUtil.isEmpty(importProcessIds)) {
            throw exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_NOT_EXISTS);
        }
        List<MesProFeedbackSurplusPoolDO> availablePools = importProcessIds.stream()
                .flatMap(processId -> safeList(surplusPoolMapper.selectAvailableListByProcessId(processId)).stream())
                .sorted(Comparator.comparing(pool -> pool.getId() == null ? Long.MAX_VALUE : pool.getId()))
                .toList();
        BigDecimal remainingToConsume = quantityToConsume;
        for (MesProFeedbackSurplusPoolDO pool : availablePools) {
            if (remainingToConsume.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal poolAvailable = pool.getAvailableQuantity() == null ? BigDecimal.ZERO : pool.getAvailableQuantity();
            if (poolAvailable.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal consumeQuantity = poolAvailable.min(remainingToConsume);
            BigDecimal nextAvailable = poolAvailable.subtract(consumeQuantity);
            BigDecimal nextAllocated = (pool.getAllocatedQuantity() == null ? BigDecimal.ZERO : pool.getAllocatedQuantity())
                    .add(consumeQuantity);
            pool.setAvailableQuantity(nextAvailable);
            pool.setAllocatedQuantity(nextAllocated);
            pool.setStatus(nextAvailable.compareTo(BigDecimal.ZERO) > 0
                    ? MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE
                    : MesProFeedbackSurplusPoolDO.STATUS_ALLOCATED);
            surplusPoolMapper.updateById(pool);
            surplusAllocationMapper.insert(MesProFeedbackSurplusAllocationDO.builder()
                    .poolId(pool.getId())
                    .importRecordId(importRecord.getId())
                    .targetType(MesProFeedbackSurplusAllocationDO.TARGET_TYPE_POOL_CONSUME)
                    .allocatedQuantity(consumeQuantity)
                    .remark(importRecord.getRemark())
                    .build());
            remainingToConsume = remainingToConsume.subtract(consumeQuantity);
        }
        if (remainingToConsume.compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PRO_FEEDBACK_IMPORT_ATTRIBUTION_TARGET_INVALID);
        }
    }

    private AttributionResult attributeImportRecordAllocation(MesProFeedbackImportRecordDO importRecord,
                                                             ThirdPartyFeedbackImportPayload payload,
                                                             MesProFeedbackImportAttributeReqVO.Allocation allocation) {
        BigDecimal feedbackQuantity = allocation.getFeedbackQuantity();
        if (feedbackQuantity == null || feedbackQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_FEEDBACK_QUANTITY_MUST_POSITIVE);
        }
        String targetType = StrUtil.blankToDefault(allocation.getTargetType(), TARGET_TYPE_CURRENT_ORDER);
        if (StrUtil.equals(targetType, TARGET_TYPE_EXTERNAL_OTHER_ORDER)) {
            return attributeExternalOtherOrder(importRecord, payload, feedbackQuantity);
        }
        if (!StrUtil.equals(targetType, TARGET_TYPE_CURRENT_ORDER)) {
            throw exception(PRO_FEEDBACK_IMPORT_ATTRIBUTION_TARGET_INVALID);
        }
        if (allocation.getScheduleOrderId() == null || allocation.getScheduleOrderProcessId() == null) {
            throw exception(PRO_FEEDBACK_IMPORT_ATTRIBUTION_TARGET_INVALID);
        }
        MesProScheduleOrderDO scheduleOrder = scheduleOrderMapper.selectById(allocation.getScheduleOrderId());
        if (scheduleOrder == null) {
            throw exception(PRO_SCHEDULE_ORDER_NOT_EXISTS);
        }
        if (!isAttributable(scheduleOrder)) {
            throw exception(PRO_SCHEDULE_ORDER_NOT_EXISTS);
        }
        MesProScheduleOrderProcessDO scheduleOrderProcess = scheduleOrderProcessMapper.selectById(allocation.getScheduleOrderProcessId());
        if (scheduleOrderProcess == null || ObjUtil.notEqual(scheduleOrderProcess.getScheduleOrderId(), scheduleOrder.getId())) {
            throw exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_NOT_EXISTS);
        }
        BigDecimal remainingQuantity = scheduleOrderProcess.getRemainingQuantity();
        if (remainingQuantity == null || remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH);
        }
        Long scheduleProcessIdentityProcessId =
                resolveScheduleProcessIdentityProcessId(scheduleOrderProcess, scheduleOrder);
        Map<Long, Long> processIdentityMap = resolveProcessIdentityMap(
                collectPositiveProcessIds(loadImportProcessIds(payload), List.of(scheduleProcessIdentityProcessId)));
        MesMdItemDO selectedItem = validateSelectedProcess(payload, scheduleOrder, scheduleOrderProcess,
                scheduleProcessIdentityProcessId, processIdentityMap);
        MesProTaskDO task = resolveTargetTask(scheduleOrderProcess, scheduleOrder, payload.getTaskCode(),
                scheduleProcessIdentityProcessId, processIdentityMap);
        if (task == null) {
            throw exception(PRO_FEEDBACK_IMPORT_TARGET_TASK_NOT_EXISTS);
        }
        Long feedbackUserId = resolveFeedbackUserId(importRecord, payload);
        Long approveUserId = resolveApproveUserId(importRecord, payload);
        MesProRouteProcessDO routeProcess = routeProcessService.resolveFrozenRouteProcess(
                scheduleOrderProcess.getRouteProcessId(), scheduleOrder.getRouteId(), scheduleOrderProcess.getProcessId());
        boolean checkFlag = Boolean.TRUE.equals(routeProcess.getCheckFlag());

        String feedbackCode = autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode());
        MesProFeedbackSaveReqVO req = new MesProFeedbackSaveReqVO();
        req.setCode(feedbackCode);
        req.setType(MesProFeedbackTypeEnum.SELF.getType());
        req.setWorkstationId(task.getWorkstationId());
        req.setRouteId(task.getRouteId());
        req.setProcessId(routeProcess.getProcessId());
        req.setWorkOrderId(task.getWorkOrderId());
        req.setTaskId(task.getId());
        req.setItemId(task.getItemId());
        req.setScheduledQuantity(task.getQuantity());
        req.setFeedbackQuantity(feedbackQuantity);
        req.setFeedbackUserId(feedbackUserId);
        req.setFeedbackTime(payload.getFeedbackTime());
        req.setApproveUserId(approveUserId);
        req.setRemark(importRecord.getRemark());
        if (checkFlag) {
            req.setQualifiedQuantity(BigDecimal.ZERO);
            req.setUnqualifiedQuantity(BigDecimal.ZERO);
            req.setUncheckQuantity(feedbackQuantity);
        } else {
            req.setQualifiedQuantity(feedbackQuantity);
            req.setUnqualifiedQuantity(BigDecimal.ZERO);
            req.setUncheckQuantity(BigDecimal.ZERO);
        }
        req.setLaborScrapQuantity(BigDecimal.ZERO);
        req.setMaterialScrapQuantity(BigDecimal.ZERO);
        req.setOtherScrapQuantity(BigDecimal.ZERO);

        req.setScheduleOrderId(scheduleOrder.getId());
        req.setScheduleOrderProcessId(scheduleOrderProcess.getId());
        Long feedbackId = feedbackService.createFeedbackWithScheduleSnapshot(req);
        feedbackMapper.updateById(MesProFeedbackDO.builder()
                .id(feedbackId)
                .sourceImportRecordId(importRecord.getId())
                .build());
        createCurrentOrderOverproduceSurplusPool(importRecord, payload, scheduleOrder, scheduleOrderProcess,
                selectedItem, feedbackId, feedbackQuantity, remainingQuantity, routeProcess.getProcessId());
        return new AttributionResult(feedbackId, scheduleOrder.getId(), scheduleOrderProcess.getId(), TARGET_TYPE_CURRENT_ORDER);
    }

    private record AttributionResult(Long feedbackId, Long scheduleOrderId, Long scheduleOrderProcessId, String targetType) {
    }

    private void createCurrentOrderOverproduceSurplusPool(MesProFeedbackImportRecordDO importRecord,
                                                          ThirdPartyFeedbackImportPayload payload,
                                                          MesProScheduleOrderDO scheduleOrder,
                                                          MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                          MesMdItemDO selectedItem,
                                                          Long feedbackId,
                                                          BigDecimal feedbackQuantity,
                                                          BigDecimal remainingQuantity,
                                                          Long processId) {
        BigDecimal overproduceQuantity = calculateOverproduceQuantity(feedbackQuantity, remainingQuantity);
        if (overproduceQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        MesProFeedbackSurplusPoolDO pool = buildBaseSurplusPool(importRecord, payload)
                .setSourceType(MesProFeedbackSurplusPoolDO.SOURCE_TYPE_CURRENT_ORDER_OVERPRODUCE)
                .setSourceFeedbackId(feedbackId)
                .setSourceScheduleOrderId(scheduleOrder.getId())
                .setSourceScheduleOrderProcessId(scheduleOrderProcess.getId())
                .setProcessId(processId)
                .setProcessCode(StrUtil.blankToDefault(scheduleOrderProcess.getProcessCode(), payload.getProcessCode()))
                .setProcessName(StrUtil.blankToDefault(scheduleOrderProcess.getProcessName(), payload.getProcessName()))
                .setProductId(scheduleOrder.getProductId())
                .setItemCode(selectedItem == null ? payload.getItemCode() : selectedItem.getCode())
                .setItemName(selectedItem == null ? payload.getItemName() : selectedItem.getName())
                .setSpecification(selectedItem == null ? payload.getSpecification() : selectedItem.getSpecification())
                .setTotalQuantity(overproduceQuantity)
                .setAllocatedQuantity(BigDecimal.ZERO)
                .setAvailableQuantity(overproduceQuantity)
                .setStatus(MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE);
        surplusPoolMapper.insert(pool);
    }

    private AttributionResult attributeExternalOtherOrder(MesProFeedbackImportRecordDO importRecord,
                                                          ThirdPartyFeedbackImportPayload payload,
                                                          BigDecimal feedbackQuantity) {
        MesProProcessDO process = resolveSingleImportProcessForPool(payload);
        MesProFeedbackSurplusPoolDO pool = buildBaseSurplusPool(importRecord, payload)
                .setSourceType(MesProFeedbackSurplusPoolDO.SOURCE_TYPE_EXTERNAL_OTHER_ORDER)
                .setProcessId(process.getId())
                .setTotalQuantity(feedbackQuantity)
                .setAllocatedQuantity(feedbackQuantity)
                .setAvailableQuantity(BigDecimal.ZERO)
                .setStatus(MesProFeedbackSurplusPoolDO.STATUS_ALLOCATED);
        surplusPoolMapper.insert(pool);
        surplusAllocationMapper.insert(MesProFeedbackSurplusAllocationDO.builder()
                .poolId(pool.getId())
                .importRecordId(importRecord.getId())
                .targetType(MesProFeedbackSurplusAllocationDO.TARGET_TYPE_EXTERNAL_OTHER_ORDER)
                .targetOrderLabel("其他订单")
                .targetProductLabel("其他产品")
                .allocatedQuantity(feedbackQuantity)
                .remark(importRecord.getRemark())
                .build());
        return new AttributionResult(0L, null, null, TARGET_TYPE_EXTERNAL_OTHER_ORDER);
    }

    private MesProFeedbackSurplusPoolDO buildBaseSurplusPool(
            MesProFeedbackImportRecordDO importRecord,
            ThirdPartyFeedbackImportPayload payload) {
        return MesProFeedbackSurplusPoolDO.builder()
                .sourceImportRecordId(importRecord.getId())
                .sourceWorkOrderCode(StrUtil.blankToDefault(payload.getWorkOrderCode(), importRecord.getWorkOrderCode()))
                .sourceTaskCode(StrUtil.blankToDefault(payload.getTaskCode(), importRecord.getTaskCode()))
                .processCode(StrUtil.blankToDefault(payload.getProcessCode(), importRecord.getProcessCode()))
                .processName(payload.getProcessName())
                .itemCode(StrUtil.blankToDefault(payload.getItemCode(), importRecord.getItemCode()))
                .itemName(payload.getItemName())
                .specification(payload.getSpecification())
                .remark(importRecord.getRemark())
                .build();
    }

    private MesProFeedbackImportRecordDO validateImportRecordExists(Long id) {
        MesProFeedbackImportRecordDO importRecord = importRecordMapper.selectById(id);
        if (importRecord == null) {
            throw exception(PRO_FEEDBACK_IMPORT_RECORD_NOT_EXISTS);
        }
        return importRecord;
    }

    private MesProFeedbackImportRecordDO validateImportRecordPending(Long id) {
        MesProFeedbackImportRecordDO importRecord = validateImportRecordExists(id);
        if (!StrUtil.equals(importRecord.getAttributionStatus(), ATTRIBUTION_STATUS_PENDING)) {
            throw exception(PRO_FEEDBACK_IMPORT_RECORD_NOT_PENDING);
        }
        return importRecord;
    }

    private ModifyEligibility evaluateModifyEligibility(MesProFeedbackImportRecordDO importRecord) {
        List<MesProFeedbackDO> linkedFeedbacks = feedbackMapper.selectListBySourceImportRecordId(importRecord.getId());
        List<MesProFeedbackSurplusPoolDO> ownedPools = surplusPoolMapper.selectListBySourceImportRecordId(importRecord.getId());
        List<MesProFeedbackSurplusAllocationDO> importRecordAllocations =
                surplusAllocationMapper.selectListByImportRecordId(importRecord.getId());
        if (CollUtil.isEmpty(linkedFeedbacks) && CollUtil.isEmpty(ownedPools) && CollUtil.isEmpty(importRecordAllocations)) {
            return ModifyEligibility.blocked(MODIFY_BLOCK_LINK_INCOMPLETE,
                    PRO_FEEDBACK_IMPORT_REATTRIBUTION_LINK_INCOMPLETE.getMsg(),
                    linkedFeedbacks, ownedPools, importRecordAllocations, List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (linkedFeedbacks.stream().anyMatch(feedback ->
                !ObjUtil.equal(feedback.getStatus(), cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum.PREPARE.getStatus()))) {
            return ModifyEligibility.blocked(MODIFY_BLOCK_FEEDBACK_NOT_PREPARE,
                    PRO_FEEDBACK_IMPORT_REATTRIBUTION_FEEDBACK_NOT_PREPARE.getMsg(),
                    linkedFeedbacks, ownedPools, importRecordAllocations, List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<MesProFeedbackSurplusAllocationDO> ownPoolAllocations = CollUtil.isEmpty(ownedPools)
                ? List.of()
                : surplusAllocationMapper.selectListByPoolIds(ownedPools.stream().map(MesProFeedbackSurplusPoolDO::getId).toList());
        if (ownPoolAllocations.stream().anyMatch(allocation -> !ObjUtil.equal(allocation.getImportRecordId(), importRecord.getId()))) {
            return ModifyEligibility.blocked(MODIFY_BLOCK_POOL_CONSUMED,
                    PRO_FEEDBACK_IMPORT_REATTRIBUTION_POOL_CONSUMED.getMsg(),
                    linkedFeedbacks, ownedPools, importRecordAllocations, ownPoolAllocations, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<MesProFeedbackSurplusAllocationDO> poolConsumeAllocations =
                filterAllocationsByTargetType(importRecordAllocations, MesProFeedbackSurplusAllocationDO.TARGET_TYPE_POOL_CONSUME);
        List<MesProFeedbackSurplusPoolDO> restorePools = poolConsumeAllocations.isEmpty()
                ? List.of()
                : surplusPoolMapper.selectListByIds(poolConsumeAllocations.stream()
                .map(MesProFeedbackSurplusAllocationDO::getPoolId).distinct().toList());
        if (restorePools.size() != poolConsumeAllocations.stream()
                .map(MesProFeedbackSurplusAllocationDO::getPoolId).filter(Objects::nonNull).distinct().count()) {
            return ModifyEligibility.blocked(MODIFY_BLOCK_LINK_INCOMPLETE,
                    PRO_FEEDBACK_IMPORT_REATTRIBUTION_LINK_INCOMPLETE.getMsg(),
                    linkedFeedbacks, ownedPools, importRecordAllocations, ownPoolAllocations, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal restorableConsumedQuantity = sumAllocatedQuantity(poolConsumeAllocations);
        BigDecimal ownedAvailablePoolQuantity = ownedPools.stream()
                .map(MesProFeedbackSurplusPoolDO::getAvailableQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ModifyEligibility.allowed(linkedFeedbacks, ownedPools, importRecordAllocations, ownPoolAllocations,
                restorableConsumedQuantity, ownedAvailablePoolQuantity);
    }

    private void rollbackExistingAttribution(MesProFeedbackImportRecordDO importRecord, ModifyEligibility modifyEligibility) {
        restoreConsumedPools(importRecord, modifyEligibility.importRecordAllocations());
        for (MesProFeedbackDO linkedFeedback : modifyEligibility.linkedFeedbacks()) {
            feedbackMapper.deleteById(linkedFeedback.getId());
        }
        Set<Long> allocationIdsToDelete = new LinkedHashSet<>();
        modifyEligibility.importRecordAllocations().stream()
                .map(MesProFeedbackSurplusAllocationDO::getId)
                .filter(Objects::nonNull)
                .forEach(allocationIdsToDelete::add);
        modifyEligibility.ownedPoolAllocations().stream()
                .map(MesProFeedbackSurplusAllocationDO::getId)
                .filter(Objects::nonNull)
                .forEach(allocationIdsToDelete::add);
        for (Long allocationId : allocationIdsToDelete) {
            surplusAllocationMapper.deleteById(allocationId);
        }
        for (MesProFeedbackSurplusPoolDO ownedPool : modifyEligibility.ownedPools()) {
            surplusPoolMapper.deleteById(ownedPool.getId());
        }
    }

    private void restoreConsumedPools(MesProFeedbackImportRecordDO importRecord,
                                      List<MesProFeedbackSurplusAllocationDO> importRecordAllocations) {
        List<MesProFeedbackSurplusAllocationDO> poolConsumeAllocations =
                filterAllocationsByTargetType(importRecordAllocations, MesProFeedbackSurplusAllocationDO.TARGET_TYPE_POOL_CONSUME);
        if (poolConsumeAllocations.isEmpty()) {
            return;
        }
        Map<Long, MesProFeedbackSurplusPoolDO> poolMap = surplusPoolMapper.selectListByIds(poolConsumeAllocations.stream()
                        .map(MesProFeedbackSurplusAllocationDO::getPoolId).distinct().toList())
                .stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
        for (MesProFeedbackSurplusAllocationDO allocation : poolConsumeAllocations) {
            MesProFeedbackSurplusPoolDO pool = poolMap.get(allocation.getPoolId());
            if (pool == null) {
                throw exception(PRO_FEEDBACK_IMPORT_REATTRIBUTION_LINK_INCOMPLETE);
            }
            BigDecimal restoreQuantity = allocation.getAllocatedQuantity() == null ? BigDecimal.ZERO : allocation.getAllocatedQuantity();
            pool.setAvailableQuantity((pool.getAvailableQuantity() == null ? BigDecimal.ZERO : pool.getAvailableQuantity()).add(restoreQuantity));
            pool.setAllocatedQuantity((pool.getAllocatedQuantity() == null ? BigDecimal.ZERO : pool.getAllocatedQuantity())
                    .subtract(restoreQuantity).max(BigDecimal.ZERO));
            pool.setStatus(pool.getAvailableQuantity().compareTo(BigDecimal.ZERO) > 0
                    ? MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE
                    : MesProFeedbackSurplusPoolDO.STATUS_ALLOCATED);
            surplusPoolMapper.updateById(pool);
        }
    }

    private ThirdPartyFeedbackImportPayload parsePayload(MesProFeedbackImportRecordDO importRecord) {
        ThirdPartyFeedbackImportPayload payload = JsonUtils.parseObject(importRecord.getSourcePayloadJson(), ThirdPartyFeedbackImportPayload.class);
        if (payload == null) {
            throw exception(PRO_FEEDBACK_IMPORT_RECORD_NOT_EXISTS);
        }
        return payload;
    }

    private Map<Long, MesProScheduleOrderDO> toScheduleOrderMap(List<MesProScheduleOrderProcessDO> processList) {
        List<Long> scheduleOrderIds = processList.stream().map(MesProScheduleOrderProcessDO::getScheduleOrderId).distinct().toList();
        return scheduleOrderMapper.selectListByIds(scheduleOrderIds).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
    }

    private Map<Long, MesProWorkOrderDO> toWorkOrderMap(Collection<MesProScheduleOrderDO> scheduleOrders) {
        List<Long> workOrderIds = scheduleOrders.stream().map(MesProScheduleOrderDO::getWorkOrderId).distinct().toList();
        return workOrderMapper.selectListByIds(workOrderIds).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
    }

    private Map<Long, MesMdItemDO> toItemMap(Collection<MesProWorkOrderDO> workOrders) {
        List<Long> itemIds = workOrders.stream().map(MesProWorkOrderDO::getProductId).distinct().toList();
        return itemMapper.selectListByIds(itemIds).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
    }

    private Map<Long, MesProTaskDO> resolveActiveTaskByScheduleOrderProcessId(
            List<MesProScheduleOrderProcessDO> processList,
            Map<Long, MesProScheduleOrderDO> scheduleOrderMap,
            String rowTaskCode,
            Map<Long, Long> scheduleProcessIdentityProcessIdMap,
            Map<Long, Long> processIdentityMap) {
        if (CollUtil.isEmpty(processList)) {
            return Map.of();
        }
        Map<Long, MesProScheduleOrderProcessDO> processMap = processList.stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
        List<Long> processIds = new ArrayList<>(processMap.keySet());
        List<MesProTaskScheduleExtDO> extList =
                taskScheduleExtMapper.selectListByScheduleOrderProcessIds(processIds);
        if (CollUtil.isEmpty(extList)) {
            return Map.of();
        }
        List<Long> taskIds = extList.stream().map(MesProTaskScheduleExtDO::getTaskId).distinct().toList();
        Map<Long, MesProTaskDO> taskMap = taskIds.isEmpty() ? Map.of() : safeList(taskMapper.selectListByIds(taskIds)).stream()
                .filter(task -> !MesProTaskStatusEnum.isEndStatus(task.getStatus()))
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
        Map<Long, MesMdWorkstationDO> workstationMap = loadExistingWorkstations(taskMap.values());
        Map<Long, List<MesProTaskDO>> tasksByProcessId = new LinkedHashMap<>();
        for (MesProTaskScheduleExtDO ext : extList) {
            MesProTaskDO task = taskMap.get(ext.getTaskId());
            MesProScheduleOrderProcessDO scheduleOrderProcess = processMap.get(ext.getScheduleOrderProcessId());
            if (task == null || scheduleOrderProcess == null) {
                continue;
            }
            MesProScheduleOrderDO scheduleOrder = scheduleOrderMap.get(scheduleOrderProcess.getScheduleOrderId());
            Long scheduleProcessIdentityProcessId = scheduleProcessIdentityProcessIdMap.getOrDefault(
                    scheduleOrderProcess.getId(), scheduleOrderProcess.getProcessId());
            if (!isValidAttributionTask(task, scheduleOrder, scheduleOrderProcess,
                    scheduleProcessIdentityProcessId, processIdentityMap, workstationMap)) {
                continue;
            }
            tasksByProcessId.computeIfAbsent(ext.getScheduleOrderProcessId(), key -> new ArrayList<>()).add(task);
        }
        Map<Long, MesProTaskDO> result = new LinkedHashMap<>();
        tasksByProcessId.forEach((scheduleOrderProcessId, tasks) -> {
            MesProTaskDO task = selectUniqueAttributionTask(rowTaskCode, tasks);
            if (task != null) {
                result.put(scheduleOrderProcessId, task);
            }
        });
        return result;
    }

    private Map<Long, MesMdWorkstationDO> loadExistingWorkstations(Collection<MesProTaskDO> tasks) {
        Set<Long> workstationIds = tasks.stream()
                .map(MesProTaskDO::getWorkstationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (workstationIds.isEmpty()) {
            return Map.of();
        }
        return safeList(workstationMapper.selectBatchIds(new ArrayList<>(workstationIds))).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
    }

    private boolean isValidAttributionTask(MesProTaskDO task,
                                           MesProScheduleOrderDO scheduleOrder,
                                           MesProScheduleOrderProcessDO scheduleOrderProcess,
                                           Long scheduleProcessIdentityProcessId,
                                           Map<Long, Long> processIdentityMap,
                                           Map<Long, MesMdWorkstationDO> workstationMap) {
        if (scheduleOrder == null || ObjUtil.notEqual(task.getWorkOrderId(), scheduleOrder.getWorkOrderId())) {
            return false;
        }
        if (!matchesTaskProcessIdentity(task, scheduleOrderProcess, scheduleProcessIdentityProcessId, processIdentityMap)) {
            return false;
        }
        return task.getWorkstationId() != null && workstationMap.containsKey(task.getWorkstationId());
    }

    private boolean matchesTaskProcessIdentity(MesProTaskDO task,
                                               MesProScheduleOrderProcessDO scheduleOrderProcess,
                                               Long scheduleProcessIdentityProcessId,
                                               Map<Long, Long> processIdentityMap) {
        Long taskProcessId = task.getProcessId();
        if (taskProcessId == null) {
            return false;
        }
        if (taskProcessId <= 0
                && scheduleOrderProcess.getProcessId() != null
                && scheduleOrderProcess.getProcessId() <= 0) {
            return true;
        }
        return isSameProcessIdentity(taskProcessId, scheduleProcessIdentityProcessId, processIdentityMap);
    }

    private MesProTaskDO selectUniqueAttributionTask(String rowTaskCode, List<MesProTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return null;
        }
        List<MesProTaskDO> taskCodeMatches = tasks.stream()
                .filter(task -> StrUtil.equals(task.getCode(), rowTaskCode))
                .toList();
        if (taskCodeMatches.size() == 1) {
            return taskCodeMatches.get(0);
        }
        if (taskCodeMatches.size() > 1) {
            return null;
        }
        return tasks.size() == 1 ? tasks.get(0) : null;
    }

    private Map<Long, Long> resolveScheduleProcessIdentityProcessIdMap(
            Collection<MesProScheduleOrderProcessDO> processList,
            Map<Long, MesProScheduleOrderDO> scheduleOrderMap) {
        if (CollUtil.isEmpty(processList)) {
            return Map.of();
        }
        Map<Long, Long> result = new LinkedHashMap<>();
        for (MesProScheduleOrderProcessDO scheduleOrderProcess : processList) {
            MesProScheduleOrderDO scheduleOrder = scheduleOrderMap.get(scheduleOrderProcess.getScheduleOrderId());
            result.put(scheduleOrderProcess.getId(), resolveScheduleProcessIdentityProcessId(scheduleOrderProcess, scheduleOrder));
        }
        return result;
    }

    private Long resolveScheduleProcessIdentityProcessId(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                         MesProScheduleOrderDO scheduleOrder) {
        Long processId = scheduleOrderProcess.getProcessId();
        if (processId != null && processId > 0) {
            return processId;
        }
        if (scheduleOrder == null || scheduleOrder.getRouteId() == null || scheduleOrderProcess.getRouteProcessId() == null) {
            return processId;
        }
        MesProRouteProcessDO routeProcess = routeProcessService.resolveFrozenRouteProcess(
                scheduleOrderProcess.getRouteProcessId(), scheduleOrder.getRouteId(), processId);
        return routeProcess.getProcessId();
    }

    private List<Long> loadImportProcessIds(ThirdPartyFeedbackImportPayload payload) {
        return loadImportProcesses(payload).stream().map(MesProProcessDO::getId).distinct().toList();
    }

    private Map<Long, Long> resolveProcessIdentityMap(Collection<Long> processIds) {
        List<Long> positiveProcessIds = safeList(processIds).stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (positiveProcessIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> identityMap = routeProcessService.getProcessIdentityMap(positiveProcessIds);
        return identityMap == null ? Map.of() : identityMap;
    }

    private List<Long> collectPositiveProcessIds(Collection<Long> left, Collection<Long> right) {
        List<Long> result = new ArrayList<>();
        safeList(left).stream()
                .filter(id -> id != null && id > 0)
                .forEach(result::add);
        safeList(right).stream()
                .filter(id -> id != null && id > 0)
                .forEach(result::add);
        return result.stream().distinct().toList();
    }

    private boolean isSameProcessIdentity(Long left, Long right, Map<Long, Long> processIdentityMap) {
        if (left == null || right == null) {
            return false;
        }
        Long leftIdentity = processIdentityMap.getOrDefault(left, left);
        Long rightIdentity = processIdentityMap.getOrDefault(right, right);
        return ObjUtil.equal(leftIdentity, rightIdentity);
    }

    private <T> List<T> safeList(Collection<T> list) {
        return list == null ? List.of() : new ArrayList<>(list);
    }

    private List<MesProProcessDO> loadImportProcesses(ThirdPartyFeedbackImportPayload payload) {
        if (payload == null || StrUtil.isBlank(payload.getProcessCode())) {
            return List.of();
        }
        return safeList(processMapper.selectListByCodes(List.of(payload.getProcessCode()))).stream()
                .filter(Objects::nonNull)
                .filter(process -> process.getId() != null)
                .collect(Collectors.toMap(MesProProcessDO::getId, process -> process,
                        (left, right) -> left, LinkedHashMap::new))
                .values().stream().toList();
    }

    private BigDecimal sumAvailableQuantityByProcessIds(Collection<Long> processIds) {
        return safeList(processIds).stream()
                .filter(processId -> processId != null && processId > 0)
                .distinct()
                .map(surplusPoolMapper::sumAvailableQuantityByProcessId)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private MesProProcessDO resolveSingleImportProcessForPool(ThirdPartyFeedbackImportPayload payload) {
        List<MesProProcessDO> importProcesses = loadImportProcesses(payload);
        if (CollUtil.isEmpty(importProcesses)) {
            throw exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_NOT_EXISTS);
        }
        if (importProcesses.size() > 1) {
            throw exception(PRO_FEEDBACK_IMPORT_ATTRIBUTION_TARGET_INVALID);
        }
        return importProcesses.get(0);
    }

    private MesProProcessDO resolveResidualSurplusPoolProcess(ThirdPartyFeedbackImportPayload payload,
                                                              List<MesProFeedbackImportAttributeReqVO.Allocation> allocations) {
        for (MesProFeedbackImportAttributeReqVO.Allocation allocation : safeList(allocations)) {
            if (!StrUtil.equals(TARGET_TYPE_CURRENT_ORDER, allocation.getTargetType())
                    || allocation.getScheduleOrderProcessId() == null
                    || allocation.getScheduleOrderProcessId() <= 0) {
                continue;
            }
            MesProScheduleOrderProcessDO scheduleOrderProcess =
                    scheduleOrderProcessMapper.selectById(allocation.getScheduleOrderProcessId());
            if (scheduleOrderProcess != null
                    && scheduleOrderProcess.getProcessId() != null
                    && scheduleOrderProcess.getProcessId() > 0) {
                return MesProProcessDO.builder()
                        .id(scheduleOrderProcess.getProcessId())
                        .code(StrUtil.blankToDefault(scheduleOrderProcess.getProcessCode(), payload.getProcessCode()))
                        .name(StrUtil.blankToDefault(scheduleOrderProcess.getProcessName(), payload.getProcessName()))
                        .build();
            }
        }
        return resolveSingleImportProcessForPool(payload);
    }

    private boolean matchesPayloadItem(ThirdPartyFeedbackImportPayload payload, MesMdItemDO item) {
        if (item == null) {
            return false;
        }
        if (StrUtil.isNotBlank(payload.getItemCode()) && !StrUtil.equals(payload.getItemCode(), item.getCode())) {
            return false;
        }
        return StrUtil.isBlank(payload.getSpecification()) || StrUtil.equals(payload.getSpecification(), item.getSpecification());
    }

    private boolean hasRemainingQuantity(MesProScheduleOrderProcessDO processDO) {
        return processDO != null
                && processDO.getRemainingQuantity() != null
                && processDO.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    private MesProFeedbackImportCandidateRespVO buildExternalOtherOrderCandidate(MesProFeedbackImportRecordDO importRecord,
                                                                                 ThirdPartyFeedbackImportPayload payload,
                                                                                 List<MesProProcessDO> importProcesses,
                                                                                 BigDecimal processSurplusPoolQuantity) {
        MesProProcessDO process = importProcesses.size() == 1 ? importProcesses.get(0) : null;
        MesProFeedbackImportCandidateRespVO candidate = new MesProFeedbackImportCandidateRespVO();
        candidate.setTargetType(TARGET_TYPE_EXTERNAL_OTHER_ORDER);
        candidate.setExternalOtherOrder(true);
        candidate.setScheduleOrderCode("其他订单");
        candidate.setScheduleOrderProcessId(-importRecord.getId());
        candidate.setWorkOrderCode("其他订单");
        candidate.setItemCode(payload.getItemCode());
        candidate.setItemName("其他产品");
        candidate.setSpecification(payload.getSpecification());
        candidate.setProcessId(process == null ? null : process.getId());
        candidate.setProcessCode(process == null ? payload.getProcessCode() : process.getCode());
        candidate.setProcessName(process == null ? payload.getProcessName()
                : StrUtil.blankToDefault(process.getName(), payload.getProcessName()));
        candidate.setReportedQuantity(BigDecimal.ZERO);
        candidate.setRemainingQuantity(BigDecimal.ZERO);
        candidate.setExactWorkOrderMatch(false);
        candidate.setTargetOrderLabel("其他订单");
        candidate.setTargetProductLabel("其他产品");
        candidate.setOverproduceQuantity(payload.getFeedbackQuantity());
        candidate.setSurplusPoolQuantity(processSurplusPoolQuantity);
        candidate.setAvailableFeedbackQuantity((payload.getFeedbackQuantity() == null ? BigDecimal.ZERO : payload.getFeedbackQuantity())
                .add(processSurplusPoolQuantity == null ? BigDecimal.ZERO : processSurplusPoolQuantity));
        return candidate;
    }

    private BigDecimal calculateOverproduceQuantity(BigDecimal feedbackQuantity, BigDecimal remainingQuantity) {
        if (feedbackQuantity == null || feedbackQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal remaining = remainingQuantity == null ? BigDecimal.ZERO : remainingQuantity;
        return feedbackQuantity.subtract(remaining).max(BigDecimal.ZERO);
    }

    private MesMdItemDO validateSelectedProcess(ThirdPartyFeedbackImportPayload payload,
                                                MesProScheduleOrderDO scheduleOrder,
                                                MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                Long scheduleProcessIdentityProcessId,
                                                Map<Long, Long> processIdentityMap) {
        List<MesProProcessDO> importProcessList = loadImportProcesses(payload);
        boolean processMatched = importProcessList.stream()
                .anyMatch(process -> isSameProcessIdentity(process.getId(), scheduleProcessIdentityProcessId, processIdentityMap));
        if (!processMatched && !StrUtil.equals(payload.getProcessCode(), scheduleOrderProcess.getProcessCode())) {
            throw exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_MISMATCH);
        }
        if (scheduleOrder.getProductId() == null) {
            throw exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_ITEM_MISMATCH);
        }
        return itemMapper.selectById(scheduleOrder.getProductId());
    }

    private boolean isAttributable(MesProScheduleOrderDO scheduleOrder) {
        if (scheduleOrder == null) {
            return false;
        }
        Integer status = scheduleOrder.getStatus();
        return !ObjUtil.equal(status, MesProScheduleOrderStatusEnum.CANCELED.getStatus())
                && !ObjUtil.equal(status, MesProScheduleOrderStatusEnum.FINISHED.getStatus());
    }

    private MesProTaskDO resolveTargetTask(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                           MesProScheduleOrderDO scheduleOrder,
                                           String rowTaskCode,
                                           Long scheduleProcessIdentityProcessId,
                                           Map<Long, Long> processIdentityMap) {
        return resolveActiveTaskByScheduleOrderProcessId(List.of(scheduleOrderProcess),
                Map.of(scheduleOrder.getId(), scheduleOrder), rowTaskCode,
                Map.of(scheduleOrderProcess.getId(), scheduleProcessIdentityProcessId), processIdentityMap)
                .get(scheduleOrderProcess.getId());
    }

    private Long resolveFeedbackUserId(MesProFeedbackImportRecordDO importRecord, ThirdPartyFeedbackImportPayload payload) {
        AdminUserDO user = adminUserMapper.selectByUsername(payload.getFeedbackUserCode());
        if (user == null) {
            throw exception(PRO_FEEDBACK_IMPORT_FEEDBACK_USER_NOT_EXISTS, payload.getSheetName(), payload.getRowNo(), payload.getFeedbackUserCode());
        }
        return user.getId();
    }

    private Long resolveApproveUserId(MesProFeedbackImportRecordDO importRecord, ThirdPartyFeedbackImportPayload payload) {
        AdminUserDO approverByUsername = adminUserMapper.selectByUsername(payload.getApproverName());
        if (approverByUsername != null) {
            return approverByUsername.getId();
        }
        List<AdminUserDO> approvers = adminUserMapper.selectListByNicknamesExact(List.of(payload.getApproverName()));
        if (approvers == null || approvers.isEmpty()) {
            throw exception(PRO_FEEDBACK_IMPORT_APPROVER_NOT_EXISTS, payload.getSheetName(), payload.getRowNo(), payload.getApproverName());
        }
        if (approvers.size() > 1) {
            throw exception(PRO_FEEDBACK_IMPORT_APPROVER_NOT_UNIQUE, payload.getSheetName(), payload.getRowNo(), payload.getApproverName());
        }
        return approvers.get(0).getId();
    }

    private boolean resolveCheckFlag(Long routeProcessId, Long routeId, Long processId) {
        MesProRouteProcessDO routeProcess =
                routeProcessService.resolveFrozenRouteProcess(routeProcessId, routeId, processId);
        return Boolean.TRUE.equals(routeProcess.getCheckFlag());
    }

    private MesProFeedbackImportRecordRespVO toImportRecordResp(MesProFeedbackImportRecordDO importRecord,
                                                                LinkedFeedbackSnapshot snapshot) {
        MesProFeedbackImportRecordRespVO respVO = BeanUtils.toBean(importRecord, MesProFeedbackImportRecordRespVO.class);
        if (respVO.getFeedbackId() != null && respVO.getFeedbackId() <= 0) {
            respVO.setFeedbackId(null);
        }
        if (StrUtil.equals(respVO.getAttributionStatus(), ATTRIBUTION_STATUS_ATTRIBUTED)) {
            respVO.setAttributionTime(importRecord.getUpdateTime());
        }
        ThirdPartyFeedbackImportPayload payload = parsePayload(importRecord);
        respVO.setWorkOrderCode(payload.getWorkOrderCode());
        respVO.setTaskCode(payload.getTaskCode());
        respVO.setItemCode(payload.getItemCode());
        respVO.setItemName(payload.getItemName());
        respVO.setSpecification(payload.getSpecification());
        respVO.setProcessCode(payload.getProcessCode());
        respVO.setProcessName(payload.getProcessName());
        respVO.setFeedbackQuantity(payload.getFeedbackQuantity());
        respVO.setFeedbackTime(payload.getFeedbackTime());
        respVO.setFeedbackUserCode(payload.getFeedbackUserCode());
        respVO.setFeedbackUserName(payload.getFeedbackUserName());
        respVO.setApproverName(payload.getApproverName());
        respVO.setSurplusPoolQuantity(sumAvailableQuantityByProcessIds(loadImportProcessIds(payload)));
        if (snapshot != null) {
            respVO.setGeneratedFeedbackDraft(snapshot.generatedFeedbackDraft());
            respVO.setLinkedFeedbackStatus(snapshot.primaryStatus());
            respVO.setFeedbackUserId(snapshot.feedbackUserId());
            respVO.setFeedbackUserNickname(snapshot.feedbackUserNickname());
            respVO.setApproveUserId(snapshot.approveUserId());
            respVO.setApproveUserNickname(snapshot.approveUserNickname());
            respVO.setFeedbackTime(snapshot.feedbackTime());
            respVO.setRemark(snapshot.remark());
        } else {
            respVO.setGeneratedFeedbackDraft(false);
        }
        if (StrUtil.equals(importRecord.getAttributionStatus(), ATTRIBUTION_STATUS_ATTRIBUTED)) {
            ModifyEligibility modifyEligibility = evaluateModifyEligibility(importRecord);
            respVO.setCanModifyAttribution(modifyEligibility.canModify());
            respVO.setModifyBlockedReason(modifyEligibility.canModify() ? null : modifyEligibility.blockedReason());
            respVO.setLinkedFeedbackCount(modifyEligibility.linkedFeedbacks().size());
        } else {
            respVO.setCanModifyAttribution(false);
            respVO.setLinkedFeedbackCount(0);
        }
        return respVO;
    }

    private Map<Long, LinkedFeedbackSnapshot> buildLinkedFeedbackSnapshotMap(Collection<MesProFeedbackImportRecordDO> importRecords) {
        if (CollUtil.isEmpty(importRecords)) {
            return Map.of();
        }
        List<Long> importRecordIds = importRecords.stream().map(MesProFeedbackImportRecordDO::getId).distinct().toList();
        Map<Long, List<MesProFeedbackDO>> feedbackMap = feedbackMapper.selectListBySourceImportRecordIds(importRecordIds).stream()
                .collect(Collectors.groupingBy(MesProFeedbackDO::getSourceImportRecordId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> userIds = new LinkedHashSet<>();
        feedbackMap.values().forEach(feedbacks -> feedbacks.forEach(feedback -> {
            if (feedback.getFeedbackUserId() != null) {
                userIds.add(feedback.getFeedbackUserId());
            }
            if (feedback.getApproveUserId() != null) {
                userIds.add(feedback.getApproveUserId());
            }
        }));
        Map<Long, AdminUserDO> userMap = userIds.stream()
                .map(adminUserMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AdminUserDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, LinkedFeedbackSnapshot> result = new LinkedHashMap<>();
        for (MesProFeedbackImportRecordDO importRecord : importRecords) {
            List<MesProFeedbackDO> linkedFeedbacks = feedbackMap.getOrDefault(importRecord.getId(), List.of());
            if (CollUtil.isEmpty(linkedFeedbacks)) {
                continue;
            }
            MesProFeedbackDO firstFeedback = linkedFeedbacks.get(0);
            AdminUserDO feedbackUser = firstFeedback.getFeedbackUserId() == null ? null : userMap.get(firstFeedback.getFeedbackUserId());
            AdminUserDO approveUser = firstFeedback.getApproveUserId() == null ? null : userMap.get(firstFeedback.getApproveUserId());
            result.put(importRecord.getId(), new LinkedFeedbackSnapshot(
                    linkedFeedbacks,
                    true,
                    firstFeedback.getStatus(),
                    linkedFeedbacks.stream().allMatch(feedback ->
                            ObjUtil.equal(feedback.getStatus(), MesProFeedbackStatusEnum.PREPARE.getStatus())),
                    firstFeedback.getFeedbackUserId(),
                    feedbackUser == null ? null : feedbackUser.getNickname(),
                    firstFeedback.getApproveUserId(),
                    approveUser == null ? null : approveUser.getNickname(),
                    firstFeedback.getFeedbackTime(),
                    firstFeedback.getRemark()
            ));
        }
        return result;
    }

    private MesProFeedbackImportBatchSummaryRespVO buildBatchSummary(Collection<MesProFeedbackImportRecordDO> importRecords,
                                                                    Map<Long, LinkedFeedbackSnapshot> snapshotMap) {
        MesProFeedbackImportBatchSummaryRespVO summary = new MesProFeedbackImportBatchSummaryRespVO();
        if (CollUtil.isEmpty(importRecords)) {
            summary.setTotalCount(0);
            summary.setPendingCount(0);
            summary.setAttributedCount(0);
            summary.setConfirmableCount(0);
            summary.setSkippedOtherOrderCount(0);
            return summary;
        }
        summary.setSourceFileName(importRecords.stream()
                .map(MesProFeedbackImportRecordDO::getSourceFileName)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null));
        int totalCount = importRecords.size();
        int pendingCount = 0;
        int attributedCount = 0;
        int confirmableCount = 0;
        int skippedOtherOrderCount = 0;
        for (MesProFeedbackImportRecordDO importRecord : importRecords) {
            if (StrUtil.equals(importRecord.getAttributionStatus(), ATTRIBUTION_STATUS_PENDING)) {
                pendingCount++;
                continue;
            }
            attributedCount++;
            LinkedFeedbackSnapshot snapshot = snapshotMap.get(importRecord.getId());
            if (isExternalOtherOrderRecord(importRecord, snapshot)) {
                skippedOtherOrderCount++;
                continue;
            }
            if (snapshot != null && snapshot.allPrepare()) {
                confirmableCount++;
            }
        }
        summary.setTotalCount(totalCount);
        summary.setPendingCount(pendingCount);
        summary.setAttributedCount(attributedCount);
        summary.setConfirmableCount(confirmableCount);
        summary.setSkippedOtherOrderCount(skippedOtherOrderCount);
        return summary;
    }

    private boolean isExternalOtherOrderRecord(MesProFeedbackImportRecordDO importRecord, LinkedFeedbackSnapshot snapshot) {
        return StrUtil.equals(importRecord.getAttributionTargetType(),
                MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_EXTERNAL_OTHER_ORDER)
                || (snapshot == null && importRecord.getFeedbackId() == null);
    }

    private String describeImportRecord(MesProFeedbackImportRecordDO importRecord) {
        ThirdPartyFeedbackImportPayload payload = parsePayload(importRecord);
        return "#" + importRecord.getId() + "("
                + StrUtil.blankToDefault(payload.getWorkOrderCode(), "-") + "/"
                + StrUtil.blankToDefault(payload.getProcessName(), StrUtil.blankToDefault(payload.getProcessCode(), "-"))
                + ")";
    }

    private record BatchConfirmSubmission(MesProFeedbackImportRecordDO importRecord,
                                          List<MesProFeedbackDO> linkedFeedbacks,
                                          MesProFeedbackImportConfirmBatchReqVO.Row row) {
    }

    private record LinkedFeedbackSnapshot(List<MesProFeedbackDO> linkedFeedbacks,
                                          boolean generatedFeedbackDraft,
                                          Integer primaryStatus,
                                          boolean allPrepare,
                                          Long feedbackUserId,
                                          String feedbackUserNickname,
                                          Long approveUserId,
                                          String approveUserNickname,
                                          java.time.LocalDateTime feedbackTime,
                                          String remark) {
    }

    private Map<Long, BigDecimal> toSelectedQuantityByProcessId(List<MesProFeedbackDO> linkedFeedbacks) {
        Map<Long, BigDecimal> selectedQuantityByProcessId = new LinkedHashMap<>();
        for (MesProFeedbackDO linkedFeedback : linkedFeedbacks) {
            if (linkedFeedback.getScheduleOrderProcessId() == null || linkedFeedback.getFeedbackQuantity() == null) {
                continue;
            }
            selectedQuantityByProcessId.merge(linkedFeedback.getScheduleOrderProcessId(),
                    linkedFeedback.getFeedbackQuantity(), BigDecimal::add);
        }
        return selectedQuantityByProcessId;
    }

    private List<MesProFeedbackSurplusAllocationDO> filterAllocationsByTargetType(
            List<MesProFeedbackSurplusAllocationDO> allocations, String targetType) {
        if (CollUtil.isEmpty(allocations)) {
            return List.of();
        }
        return allocations.stream()
                .filter(allocation -> StrUtil.equals(targetType, allocation.getTargetType()))
                .toList();
    }

    private BigDecimal sumAllocatedQuantity(List<MesProFeedbackSurplusAllocationDO> allocations) {
        if (CollUtil.isEmpty(allocations)) {
            return BigDecimal.ZERO;
        }
        return allocations.stream()
                .map(MesProFeedbackSurplusAllocationDO::getAllocatedQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void throwModifyBlockedException(ModifyEligibility modifyEligibility) {
        String blockType = modifyEligibility.blockType();
        if (StrUtil.equals(blockType, MODIFY_BLOCK_FEEDBACK_NOT_PREPARE)) {
            throw exception(PRO_FEEDBACK_IMPORT_REATTRIBUTION_FEEDBACK_NOT_PREPARE);
        }
        if (StrUtil.equals(blockType, MODIFY_BLOCK_POOL_CONSUMED)) {
            throw exception(PRO_FEEDBACK_IMPORT_REATTRIBUTION_POOL_CONSUMED);
        }
        if (StrUtil.equals(blockType, MODIFY_BLOCK_LINK_INCOMPLETE)) {
            throw exception(PRO_FEEDBACK_IMPORT_REATTRIBUTION_LINK_INCOMPLETE);
        }
        throw exception(PRO_FEEDBACK_IMPORT_REATTRIBUTION_NOT_ALLOWED);
    }

    private record ModifyEligibility(
            boolean canModify,
            String blockType,
            String blockedReason,
            List<MesProFeedbackDO> linkedFeedbacks,
            List<MesProFeedbackSurplusPoolDO> ownedPools,
            List<MesProFeedbackSurplusAllocationDO> importRecordAllocations,
            List<MesProFeedbackSurplusAllocationDO> ownedPoolAllocations,
            BigDecimal restorableConsumedQuantity,
            BigDecimal ownedAvailablePoolQuantity) {

        private static ModifyEligibility allowed(List<MesProFeedbackDO> linkedFeedbacks,
                                                 List<MesProFeedbackSurplusPoolDO> ownedPools,
                                                 List<MesProFeedbackSurplusAllocationDO> importRecordAllocations,
                                                 List<MesProFeedbackSurplusAllocationDO> ownedPoolAllocations,
                                                 BigDecimal restorableConsumedQuantity,
                                                 BigDecimal ownedAvailablePoolQuantity) {
            return new ModifyEligibility(true, null, null, linkedFeedbacks, ownedPools,
                    importRecordAllocations, ownedPoolAllocations, restorableConsumedQuantity, ownedAvailablePoolQuantity);
        }

        private static ModifyEligibility blocked(String blockType, String blockedReason,
                                                 List<MesProFeedbackDO> linkedFeedbacks,
                                                 List<MesProFeedbackSurplusPoolDO> ownedPools,
                                                 List<MesProFeedbackSurplusAllocationDO> importRecordAllocations,
                                                 List<MesProFeedbackSurplusAllocationDO> ownedPoolAllocations,
                                                 BigDecimal restorableConsumedQuantity,
                                                 BigDecimal ownedAvailablePoolQuantity) {
            return new ModifyEligibility(false, blockType, blockedReason, linkedFeedbacks, ownedPools,
                    importRecordAllocations, ownedPoolAllocations, restorableConsumedQuantity, ownedAvailablePoolQuantity);
        }
    }
}
