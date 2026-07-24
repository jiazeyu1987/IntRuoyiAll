package cn.iocoder.yudao.module.dcc.service.permission;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestoreLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestoreLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestorePlanItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestorePlanMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Validated
public class DccNasPermissionRestoreExecutionServiceImpl implements DccNasPermissionRestoreExecutionService {

    private static final String PLAN_STATUS_READY = "READY";
    private static final String PLAN_STATUS_EXECUTING = "EXECUTING";
    private static final String PLAN_STATUS_COMPLETED = "COMPLETED";
    private static final String PLAN_STATUS_FAILED = "FAILED";
    private static final String ITEM_STATUS_WAITING = "WAITING";
    private static final String ITEM_STATUS_APPLIED = "APPLIED";
    private static final String ITEM_STATUS_VERIFIED = "VERIFIED";
    private static final String ITEM_STATUS_FAILED = "FAILED";
    private static final String ITEM_STATUS_BLOCKED = "BLOCKED";
    private static final String RESTORE_MODE_REPLACE_DIRECTORY_RULES = "REPLACE_DIRECTORY_RULES";
    private static final String LOG_ACTION_VALIDATE = "VALIDATE";
    private static final String LOG_ACTION_APPLY = "APPLY";
    private static final String LOG_ACTION_VERIFY = "VERIFY";
    private static final String LOG_STATUS_SUCCESS = "SUCCEEDED";
    private static final String LOG_STATUS_FAILED = "FAILED";
    private static final String CURRENT_HASH_MISMATCH_CODE = "DCC_NAS_ACL_RESTORE_CURRENT_HASH_MISMATCH";
    private static final String AFTER_HASH_MISMATCH_CODE = "DCC_NAS_ACL_RESTORE_AFTER_HASH_MISMATCH";
    private static final String PLAN_ITEM_PREREQUISITE_INVALID_CODE =
            "DCC_NAS_ACL_RESTORE_PLAN_ITEM_PREREQUISITE_INVALID";
    private static final String ITEM_PROCESSING_FAILED_CODE = "DCC_NAS_ACL_RESTORE_ITEM_PROCESSING_FAILED";
    private static final long EXECUTING_PLAN_STALE_MINUTES = 30L;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Resource
    private DccNasAclRestorePlanMapper restorePlanMapper;
    @Resource
    private DccNasAclRestorePlanItemMapper restorePlanItemMapper;
    @Resource
    private DccNasAclRestoreLogMapper restoreLogMapper;
    @Resource
    private DccDirectoryAccessRuleMapper directoryAccessRuleMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void processWaitingRestorePlans() {
        LocalDateTime staleStartedBefore = leaseNow().minusMinutes(EXECUTING_PLAN_STALE_MINUTES);
        List<DccNasAclRestorePlanDO> readyPlans = restorePlanMapper.selectList(
                new LambdaQueryWrapperX<DccNasAclRestorePlanDO>()
                        .and(wrapper -> wrapper.eq(DccNasAclRestorePlanDO::getStatus, PLAN_STATUS_READY)
                                .or(executingWrapper -> executingWrapper
                                        .eq(DccNasAclRestorePlanDO::getStatus, PLAN_STATUS_EXECUTING)
                                        .le(DccNasAclRestorePlanDO::getStartedAt, staleStartedBefore))));
        requireNonNull(readyPlans, "ready restore plans");
        for (DccNasAclRestorePlanDO plan : readyPlans) {
            processPlan(plan);
        }
    }

    private void processPlan(DccNasAclRestorePlanDO plan) {
        requireNonNull(plan.getId(), "restore plan id");
        PlanClaim claim = claimPlan(plan);
        if (!claim.claimed()) {
            return;
        }
        LocalDateTime currentLeaseStartedAt = claim.startedAt();
        List<DccNasAclRestorePlanItemDO> planItems;
        try {
            planItems = selectPlanItems(plan.getId());
        } catch (RuntimeException ex) {
            failPlanPrerequisite(plan, currentLeaseStartedAt, "restore plan items unavailable: " + exceptionMessage(ex),
                    List.of(), null);
            return;
        }
        if (!validatePlanItemsBeforeProcessing(plan, currentLeaseStartedAt, planItems)) {
            return;
        }
        List<DccNasAclRestorePlanItemDO> waitingItems = planItems.stream()
                .filter(this::isExecutableItemStatus)
                .toList();

        Map<Long, String> processedStatuses = new LinkedHashMap<>();
        for (DccNasAclRestorePlanItemDO item : waitingItems) {
            LocalDateTime refreshedLeaseStartedAt = refreshPlanLease(plan.getId(), currentLeaseStartedAt);
            if (refreshedLeaseStartedAt == null) {
                return;
            }
            currentLeaseStartedAt = refreshedLeaseStartedAt;
            RestoreItemResult result;
            try {
                result = transactionTemplate.execute(status -> processItem(plan, item));
                requireNonNull(result, "restore item result");
            } catch (RestoreItemProcessingException ex) {
                result = failItemProcessing(plan, item, ex.actionType(), ex.beforeHash(),
                        ex.expectedAfterHash(), ex.actualAfterHash(), ex.getCause());
            } catch (RuntimeException ex) {
                result = failItemProcessing(plan, item, LOG_ACTION_VALIDATE, null,
                        item.getExpectedAfterHash(), null, ex);
            }
            if (result.succeeded()) {
                processedStatuses.put(item.getId(), ITEM_STATUS_VERIFIED);
                continue;
            }
            processedStatuses.put(item.getId(), ITEM_STATUS_FAILED);
            RestoreItemCounts itemCounts = countItems(selectPlanItems(plan.getId()), processedStatuses);
            updatePlanFailed(plan, currentLeaseStartedAt, result.errorCode(), result.errorMessage(),
                    itemCounts.completedDirectoryCount(), itemCounts.failedDirectoryCount());
            return;
        }
        List<DccNasAclRestorePlanItemDO> latestItems = selectPlanItems(plan.getId());
        RestoreItemCounts itemCounts = countItems(latestItems, processedStatuses);
        if (!canCompletePlan(plan, currentLeaseStartedAt, latestItems, processedStatuses, itemCounts)) {
            return;
        }
        updatePlanCompleted(plan, currentLeaseStartedAt, itemCounts.completedDirectoryCount(),
                itemCounts.failedDirectoryCount());
    }

    private PlanClaim claimPlan(DccNasAclRestorePlanDO plan) {
        LocalDateTime claimedAt = leaseNow();
        if (PLAN_STATUS_READY.equals(plan.getStatus())) {
            return new PlanClaim(restorePlanMapper.claimReadyPlan(plan.getId(), claimedAt) > 0, claimedAt);
        }
        if (PLAN_STATUS_EXECUTING.equals(plan.getStatus()) && isStaleExecutingPlan(plan, claimedAt)) {
            LocalDateTime currentStartedAt = normalizeLeaseTimestamp(plan.getStartedAt());
            return new PlanClaim(restorePlanMapper.reclaimExecutingPlan(plan.getId(), currentStartedAt, claimedAt) > 0,
                    claimedAt);
        }
        return new PlanClaim(false, claimedAt);
    }

    private boolean isStaleExecutingPlan(DccNasAclRestorePlanDO plan, LocalDateTime now) {
        return plan.getStartedAt() != null
                && !plan.getStartedAt().isAfter(now.minusMinutes(EXECUTING_PLAN_STALE_MINUTES));
    }

    private LocalDateTime refreshPlanLease(Long planId, LocalDateTime currentLeaseStartedAt) {
        LocalDateTime normalizedCurrentLeaseStartedAt = normalizeLeaseTimestamp(currentLeaseStartedAt);
        LocalDateTime refreshedAt = leaseNow();
        if (restorePlanMapper.refreshExecutingPlanLease(planId, normalizedCurrentLeaseStartedAt, refreshedAt) <= 0) {
            return null;
        }
        return refreshedAt;
    }

    private LocalDateTime leaseNow() {
        return normalizeLeaseTimestamp(LocalDateTime.now());
    }

    private LocalDateTime normalizeLeaseTimestamp(LocalDateTime value) {
        requireNonNull(value, "restore plan lease timestamp");
        return value.truncatedTo(ChronoUnit.SECONDS);
    }

    private RestoreItemResult processItem(DccNasAclRestorePlanDO plan, DccNasAclRestorePlanItemDO item) {
        if (ITEM_STATUS_APPLIED.equals(item.getStatus())) {
            return verifyAppliedItem(plan, item);
        }
        PlannedOperations operations = parsePlannedOperations(item);
        List<DccDirectoryAccessRuleDO> currentRules;
        String currentHash;
        try {
            currentRules = selectDirectoryRules(operations.directoryId());
            currentHash = DccDirectoryAccessRuleCanonicalHash.directoryRulesHash(currentRules);
        } catch (RuntimeException ex) {
            return failItemProcessing(plan, item, LOG_ACTION_VALIDATE, null, operations.expectedAfterHash(), null, ex);
        }
        if (!Objects.equals(currentHash, operations.expectedCurrentRuleHash())) {
            String errorMessage = "Current directory rule hash mismatch for directory "
                    + operations.directoryId() + ": expected " + operations.expectedCurrentRuleHash()
                    + " but actual " + currentHash;
            insertRestoreLog(plan, item, LOG_ACTION_VALIDATE, LOG_STATUS_FAILED,
                    currentHash, operations.expectedAfterHash(), null,
                    CURRENT_HASH_MISMATCH_CODE, errorMessage);
            updateItemFailed(item, operations.expectedAfterHash(), currentHash,
                    CURRENT_HASH_MISMATCH_CODE + ": " + errorMessage);
            return RestoreItemResult.failed(CURRENT_HASH_MISMATCH_CODE, errorMessage);
        }

        insertRestoreLog(plan, item, LOG_ACTION_VALIDATE, LOG_STATUS_SUCCESS,
                operations.expectedCurrentRuleHash(), operations.expectedAfterHash(), currentHash, null, null);
        try {
            directoryAccessRuleMapper.delete(new LambdaQueryWrapperX<DccDirectoryAccessRuleDO>()
                    .eq(DccDirectoryAccessRuleDO::getDirectoryId, operations.directoryId()));
            for (DccDirectoryAccessRuleDO targetRule : operations.replaceDirectoryRules().stream()
                    .sorted(DccDirectoryAccessRuleCanonicalHash.ruleComparator())
                    .toList()) {
                directoryAccessRuleMapper.insert(targetRule);
            }
        } catch (RuntimeException ex) {
            throw new RestoreItemProcessingException(LOG_ACTION_APPLY, operations.expectedCurrentRuleHash(),
                    operations.expectedAfterHash(), null, ex);
        }
        insertRestoreLog(plan, item, LOG_ACTION_APPLY, LOG_STATUS_SUCCESS,
                operations.expectedCurrentRuleHash(), operations.expectedAfterHash(), null, null, null);

        String actualAfterHash;
        try {
            actualAfterHash = DccDirectoryAccessRuleCanonicalHash.directoryRulesHash(
                    selectDirectoryRules(operations.directoryId()));
        } catch (RuntimeException ex) {
            throw new RestoreItemProcessingException(LOG_ACTION_VERIFY, operations.expectedAfterHash(),
                    operations.expectedAfterHash(), null, ex);
        }
        if (!Objects.equals(actualAfterHash, operations.expectedAfterHash())) {
            String errorMessage = "Restored directory rule hash mismatch for directory "
                    + operations.directoryId() + ": expected " + operations.expectedAfterHash()
                    + " but actual " + actualAfterHash;
            insertRestoreLog(plan, item, LOG_ACTION_VERIFY, LOG_STATUS_FAILED,
                    operations.expectedAfterHash(), operations.expectedAfterHash(), actualAfterHash,
                    AFTER_HASH_MISMATCH_CODE, errorMessage);
            updateItemFailed(item, operations.expectedAfterHash(), actualAfterHash,
                    AFTER_HASH_MISMATCH_CODE + ": " + errorMessage);
            return RestoreItemResult.failed(AFTER_HASH_MISMATCH_CODE, errorMessage);
        }

        insertRestoreLog(plan, item, LOG_ACTION_VERIFY, LOG_STATUS_SUCCESS,
                operations.expectedAfterHash(), operations.expectedAfterHash(), actualAfterHash, null, null);
        updateItemVerified(item, operations.expectedAfterHash(), actualAfterHash);
        return RestoreItemResult.success();
    }

    private RestoreItemResult verifyAppliedItem(DccNasAclRestorePlanDO plan, DccNasAclRestorePlanItemDO item) {
        PlannedOperations operations;
        try {
            operations = parsePlannedOperations(item);
            String actualAfterHash = DccDirectoryAccessRuleCanonicalHash.directoryRulesHash(
                    selectDirectoryRules(operations.directoryId()));
            if (!Objects.equals(actualAfterHash, operations.expectedAfterHash())) {
                String errorMessage = "Restored directory rule hash mismatch for directory "
                        + operations.directoryId() + ": expected " + operations.expectedAfterHash()
                        + " but actual " + actualAfterHash;
                insertRestoreLog(plan, item, LOG_ACTION_VERIFY, LOG_STATUS_FAILED,
                        operations.expectedAfterHash(), operations.expectedAfterHash(), actualAfterHash,
                        AFTER_HASH_MISMATCH_CODE, errorMessage);
                updateItemFailed(item, operations.expectedAfterHash(), actualAfterHash,
                        AFTER_HASH_MISMATCH_CODE + ": " + errorMessage);
                return RestoreItemResult.failed(AFTER_HASH_MISMATCH_CODE, errorMessage);
            }
            insertRestoreLog(plan, item, LOG_ACTION_VERIFY, LOG_STATUS_SUCCESS,
                    operations.expectedAfterHash(), operations.expectedAfterHash(), actualAfterHash, null, null);
            updateItemVerified(item, operations.expectedAfterHash(), actualAfterHash);
            return RestoreItemResult.success();
        } catch (RuntimeException ex) {
            return failItemProcessing(plan, item, LOG_ACTION_VERIFY, item.getExpectedAfterHash(),
                    item.getExpectedAfterHash(), null, ex);
        }
    }

    private List<DccDirectoryAccessRuleDO> selectDirectoryRules(Long directoryId) {
        List<DccDirectoryAccessRuleDO> rules = directoryAccessRuleMapper.selectList(
                new LambdaQueryWrapperX<DccDirectoryAccessRuleDO>()
                        .eq(DccDirectoryAccessRuleDO::getDirectoryId, directoryId));
        requireNonNull(rules, "directory access rules");
        return rules;
    }

    private List<DccNasAclRestorePlanItemDO> selectPlanItems(Long planId) {
        List<DccNasAclRestorePlanItemDO> items = restorePlanItemMapper.selectList(
                new LambdaQueryWrapperX<DccNasAclRestorePlanItemDO>()
                        .eq(DccNasAclRestorePlanItemDO::getPlanId, planId));
        requireNonNull(items, "restore plan items");
        return items;
    }

    private boolean validatePlanItemsBeforeProcessing(DccNasAclRestorePlanDO plan,
                                                      LocalDateTime startedAt,
                                                      List<DccNasAclRestorePlanItemDO> items) {
        if (items.isEmpty()) {
            failPlanPrerequisite(plan, startedAt, "restore plan items required", items, null);
            return false;
        }
        Long expectedDirectoryCount = expectedDirectoryCount(plan);
        if (expectedDirectoryCount == null) {
            failPlanPrerequisite(plan, startedAt, "validationSummaryJson.directoryCount required", items, null);
            return false;
        }
        if (expectedDirectoryCount != items.size()) {
            failPlanPrerequisite(plan, startedAt, "validationSummaryJson.directoryCount " + expectedDirectoryCount
                    + " does not match restore plan item count " + items.size(), items, null);
            return false;
        }
        for (DccNasAclRestorePlanItemDO item : items) {
            if (isKnownItemStatus(item.getStatus())) {
                continue;
            }
            failPlanPrerequisite(plan, startedAt, "unsupported restore item status " + item.getStatus()
                    + " for restore plan item " + item.getId(), items, item);
            return false;
        }
        for (DccNasAclRestorePlanItemDO item : items) {
            if (!isFailedItemStatus(item.getStatus())) {
                continue;
            }
            failPlanPrerequisite(plan, startedAt, "restore plan item " + item.getId()
                    + " already failed with status " + item.getStatus(), items, null);
            return false;
        }
        return true;
    }

    private boolean canCompletePlan(DccNasAclRestorePlanDO plan,
                                    LocalDateTime startedAt,
                                    List<DccNasAclRestorePlanItemDO> latestItems,
                                    Map<Long, String> processedStatuses,
                                    RestoreItemCounts itemCounts) {
        Long expectedDirectoryCount = expectedDirectoryCount(plan);
        if (expectedDirectoryCount == null) {
            failPlanPrerequisite(plan, startedAt, "validationSummaryJson.directoryCount required",
                    latestItems, null);
            return false;
        }
        if (expectedDirectoryCount != latestItems.size()) {
            failPlanPrerequisite(plan, startedAt, "validationSummaryJson.directoryCount " + expectedDirectoryCount
                    + " does not match restore plan item count " + latestItems.size(), latestItems, null);
            return false;
        }
        DccNasAclRestorePlanItemDO incompleteItem = firstIncompleteItem(latestItems, processedStatuses);
        if (incompleteItem != null) {
            failPlanPrerequisite(plan, startedAt, "restore plan item " + incompleteItem.getId()
                    + " status cannot prove directory was verified: "
                    + processedStatuses.getOrDefault(incompleteItem.getId(), incompleteItem.getStatus()),
                    latestItems, incompleteItem);
            return false;
        }
        if (itemCounts.completedDirectoryCount() != expectedDirectoryCount || itemCounts.failedDirectoryCount() != 0) {
            failPlanPrerequisite(plan, startedAt, "verified item count " + itemCounts.completedDirectoryCount()
                    + " and failed item count " + itemCounts.failedDirectoryCount()
                    + " do not match expected directoryCount " + expectedDirectoryCount, latestItems, null);
            return false;
        }
        return true;
    }

    private DccNasAclRestorePlanItemDO firstIncompleteItem(List<DccNasAclRestorePlanItemDO> items,
                                                           Map<Long, String> processedStatuses) {
        for (DccNasAclRestorePlanItemDO item : items) {
            String status = processedStatuses.getOrDefault(item.getId(), item.getStatus());
            if (!ITEM_STATUS_VERIFIED.equals(status)) {
                return item;
            }
        }
        return null;
    }

    private Long expectedDirectoryCount(DccNasAclRestorePlanDO plan) {
        Map<String, Object> summary = JsonUtils.parseObject(plan.getValidationSummaryJson(), MAP_TYPE);
        if (summary == null || !(summary.get("directoryCount") instanceof Number directoryCount)) {
            return null;
        }
        return directoryCount.longValue();
    }

    private boolean isExecutableItemStatus(DccNasAclRestorePlanItemDO item) {
        return ITEM_STATUS_WAITING.equals(item.getStatus()) || ITEM_STATUS_APPLIED.equals(item.getStatus());
    }

    private boolean isKnownItemStatus(String status) {
        return ITEM_STATUS_WAITING.equals(status)
                || ITEM_STATUS_APPLIED.equals(status)
                || ITEM_STATUS_VERIFIED.equals(status)
                || isFailedItemStatus(status);
    }

    private RestoreItemCounts countItems(List<DccNasAclRestorePlanItemDO> items,
                                         Map<Long, String> processedStatuses) {
        long completedDirectoryCount = 0L;
        long failedDirectoryCount = 0L;
        Set<Long> countedProcessedIds = new HashSet<>();
        for (DccNasAclRestorePlanItemDO item : items) {
            String status = processedStatuses.getOrDefault(item.getId(), item.getStatus());
            if (ITEM_STATUS_VERIFIED.equals(status)) {
                completedDirectoryCount++;
            } else if (isFailedItemStatus(status)) {
                failedDirectoryCount++;
            }
            countedProcessedIds.add(item.getId());
        }
        for (Map.Entry<Long, String> entry : processedStatuses.entrySet()) {
            if (countedProcessedIds.contains(entry.getKey())) {
                continue;
            }
            if (ITEM_STATUS_VERIFIED.equals(entry.getValue())) {
                completedDirectoryCount++;
            } else if (isFailedItemStatus(entry.getValue())) {
                failedDirectoryCount++;
            }
        }
        return new RestoreItemCounts(completedDirectoryCount, failedDirectoryCount);
    }

    private boolean isFailedItemStatus(String status) {
        return ITEM_STATUS_FAILED.equals(status) || ITEM_STATUS_BLOCKED.equals(status);
    }

    private PlannedOperations parsePlannedOperations(DccNasAclRestorePlanItemDO item) {
        requireNonNull(item.getId(), "restore plan item id");
        Map<String, Object> operations = JsonUtils.parseObject(item.getPlannedOperationsJson(), MAP_TYPE);
        if (operations == null) {
            throw new IllegalStateException("plannedOperationsJson required for restore plan item " + item.getId());
        }
        String restoreMode = requiredString(operations, "restoreMode", item.getId());
        if (!RESTORE_MODE_REPLACE_DIRECTORY_RULES.equals(restoreMode)) {
            throw new IllegalStateException("unsupported restoreMode for restore plan item " + item.getId()
                    + ": " + restoreMode);
        }
        Long directoryId = requiredLong(operations, "directoryId", item.getId());
        String expectedCurrentRuleHash = requiredString(operations, "expectedCurrentRuleHash", item.getId());
        String expectedAfterHash = requiredString(operations, "expectedAfterHash", item.getId());
        List<DccDirectoryAccessRuleDO> replaceDirectoryRules = requiredReplaceDirectoryRules(
                operations, directoryId, item.getId());
        return new PlannedOperations(directoryId, expectedCurrentRuleHash, expectedAfterHash, replaceDirectoryRules);
    }

    private List<DccDirectoryAccessRuleDO> requiredReplaceDirectoryRules(Map<String, Object> operations,
                                                                         Long directoryId,
                                                                         Long itemId) {
        if (!operations.containsKey("replaceDirectoryRules")
                || !(operations.get("replaceDirectoryRules") instanceof List<?> ruleValues)) {
            throw new IllegalStateException("replaceDirectoryRules required for restore plan item " + itemId);
        }
        List<DccDirectoryAccessRuleDO> rules = new ArrayList<>();
        for (Object ruleValue : ruleValues) {
            Map<String, Object> rule = asMap(ruleValue, "replaceDirectoryRules item", itemId);
            Long ruleDirectoryId = requiredLong(rule, "directoryId", itemId);
            if (!Objects.equals(directoryId, ruleDirectoryId)) {
                throw new IllegalStateException("replaceDirectoryRules directoryId mismatch for restore plan item "
                        + itemId);
            }
            rules.add(DccDirectoryAccessRuleDO.builder()
                    .directoryId(ruleDirectoryId)
                    .subjectType(requiredString(rule, "subjectType", itemId))
                    .subjectId(requiredLong(rule, "subjectId", itemId))
                    .canQuery(requiredBoolean(rule, "canQuery", itemId))
                    .canPreview(requiredBoolean(rule, "canPreview", itemId))
                    .canDownload(requiredBoolean(rule, "canDownload", itemId))
                    .active(requiredBoolean(rule, "active", itemId))
                    .changeReason(optionalString(rule, "changeReason", itemId))
                    .build());
        }
        return rules;
    }

    private Map<String, Object> asMap(Object value, String fieldName, Long itemId) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalStateException(fieldName + " must be object for restore plan item " + itemId);
        }
        Map<String, Object> converted = JsonUtils.convertObject(value, MAP_TYPE);
        if (converted == null) {
            throw new IllegalStateException(fieldName + " required for restore plan item " + itemId);
        }
        return converted;
    }

    private String requiredString(Map<String, Object> values, String fieldName, Long itemId) {
        if (!values.containsKey(fieldName)
                || !(values.get(fieldName) instanceof String value)
                || StrUtil.isBlank(value)) {
            throw new IllegalStateException(fieldName + " required for restore plan item " + itemId);
        }
        return value;
    }

    private String optionalString(Map<String, Object> values, String fieldName, Long itemId) {
        if (!values.containsKey(fieldName) || values.get(fieldName) == null) {
            return null;
        }
        if (!(values.get(fieldName) instanceof String value)) {
            throw new IllegalStateException(fieldName + " must be string for restore plan item " + itemId);
        }
        return value;
    }

    private Long requiredLong(Map<String, Object> values, String fieldName, Long itemId) {
        if (!values.containsKey(fieldName) || !(values.get(fieldName) instanceof Number number)) {
            throw new IllegalStateException(fieldName + " required for restore plan item " + itemId);
        }
        return number.longValue();
    }

    private Boolean requiredBoolean(Map<String, Object> values, String fieldName, Long itemId) {
        if (!values.containsKey(fieldName) || !(values.get(fieldName) instanceof Boolean value)) {
            throw new IllegalStateException(fieldName + " required for restore plan item " + itemId);
        }
        return value;
    }

    private void updateItemVerified(DccNasAclRestorePlanItemDO item, String expectedAfterHash, String actualAfterHash) {
        restorePlanItemMapper.updateById(DccNasAclRestorePlanItemDO.builder()
                .id(item.getId())
                .status(ITEM_STATUS_VERIFIED)
                .expectedAfterHash(expectedAfterHash)
                .actualAfterHash(actualAfterHash)
                .verifiedAt(LocalDateTime.now())
                .build());
    }

    private void updateItemFailed(DccNasAclRestorePlanItemDO item,
                                  String expectedAfterHash,
                                  String actualAfterHash,
                                  String blockReason) {
        restorePlanItemMapper.updateById(DccNasAclRestorePlanItemDO.builder()
                .id(item.getId())
                .status(ITEM_STATUS_FAILED)
                .blockReason(blockReason)
                .expectedAfterHash(expectedAfterHash)
                .actualAfterHash(actualAfterHash)
                .build());
    }

    private RestoreItemResult failItemProcessing(DccNasAclRestorePlanDO plan,
                                                 DccNasAclRestorePlanItemDO item,
                                                 String actionType,
                                                 String beforeHash,
                                                 String expectedAfterHash,
                                                 String actualAfterHash,
                                                 Throwable ex) {
        String errorMessage = "Restore plan item " + item.getId() + " failed during " + actionType
                + ": " + exceptionMessage(ex);
        insertRestoreLog(plan, item, actionType, LOG_STATUS_FAILED, beforeHash, expectedAfterHash, actualAfterHash,
                ITEM_PROCESSING_FAILED_CODE, errorMessage);
        updateItemFailed(item, expectedAfterHash, actualAfterHash,
                ITEM_PROCESSING_FAILED_CODE + ": " + errorMessage);
        return RestoreItemResult.failed(ITEM_PROCESSING_FAILED_CODE, errorMessage);
    }

    private void failPlanPrerequisite(DccNasAclRestorePlanDO plan,
                                      LocalDateTime startedAt,
                                      String failureMessage,
                                      List<DccNasAclRestorePlanItemDO> items,
                                      DccNasAclRestorePlanItemDO failedItem) {
        if (failedItem != null) {
            updateItemFailed(failedItem, failedItem.getExpectedAfterHash(), failedItem.getActualAfterHash(),
                    PLAN_ITEM_PREREQUISITE_INVALID_CODE + ": " + failureMessage);
        }
        Map<Long, String> processedStatuses = failedItem == null
                ? Map.of()
                : Map.of(failedItem.getId(), ITEM_STATUS_FAILED);
        RestoreItemCounts itemCounts = countItems(items, processedStatuses);
        updatePlanFailed(plan, startedAt, PLAN_ITEM_PREREQUISITE_INVALID_CODE, failureMessage,
                itemCounts.completedDirectoryCount(), itemCounts.failedDirectoryCount());
    }

    private void updatePlanCompleted(DccNasAclRestorePlanDO plan,
                                     LocalDateTime startedAt,
                                     long completedDirectoryCount,
                                     long failedDirectoryCount) {
        restorePlanMapper.updateById(DccNasAclRestorePlanDO.builder()
                .id(plan.getId())
                .status(PLAN_STATUS_COMPLETED)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .validationSummaryJson(validationSummaryJson(plan, completedDirectoryCount, failedDirectoryCount))
                .build());
    }

    private void updatePlanFailed(DccNasAclRestorePlanDO plan,
                                  LocalDateTime startedAt,
                                  String failureCode,
                                  String failureMessage,
                                  long completedDirectoryCount,
                                  long failedDirectoryCount) {
        restorePlanMapper.updateById(DccNasAclRestorePlanDO.builder()
                .id(plan.getId())
                .status(PLAN_STATUS_FAILED)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .failureCode(failureCode)
                .failureMessage(failureMessage)
                .validationSummaryJson(validationSummaryJson(plan, completedDirectoryCount, failedDirectoryCount))
                .build());
    }

    private String validationSummaryJson(DccNasAclRestorePlanDO plan,
                                         long completedDirectoryCount,
                                         long failedDirectoryCount) {
        Map<String, Object> summary = JsonUtils.parseObject(plan.getValidationSummaryJson(), MAP_TYPE);
        if (summary == null) {
            summary = new LinkedHashMap<>();
        }
        summary.put("completedDirectoryCount", completedDirectoryCount);
        summary.put("failedDirectoryCount", failedDirectoryCount);
        return JsonUtils.toJsonString(summary);
    }

    private void insertRestoreLog(DccNasAclRestorePlanDO plan,
                                  DccNasAclRestorePlanItemDO item,
                                  String actionType,
                                  String status,
                                  String beforeHash,
                                  String expectedAfterHash,
                                  String actualAfterHash,
                                  String errorCode,
                                  String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        restoreLogMapper.insert(DccNasAclRestoreLogDO.builder()
                .planId(plan.getId())
                .planItemId(item.getId())
                .attemptNo(1)
                .actionType(actionType)
                .status(status)
                .beforeHash(beforeHash)
                .expectedAfterHash(expectedAfterHash)
                .actualAfterHash(actualAfterHash)
                .requestPayloadHash(item.getPlannedOperationsHash())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .startedAt(now)
                .completedAt(now)
                .operatorUserId(plan.getCreatedByUserId())
                .build());
    }

    private void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(fieldName + " required");
        }
    }

    private String exceptionMessage(Throwable ex) {
        if (ex == null) {
            return "unknown error";
        }
        if (StrUtil.isNotBlank(ex.getMessage())) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    private record PlannedOperations(Long directoryId,
                                     String expectedCurrentRuleHash,
                                     String expectedAfterHash,
                                     List<DccDirectoryAccessRuleDO> replaceDirectoryRules) {
    }

    private record RestoreItemResult(boolean succeeded, String errorCode, String errorMessage) {

        private static RestoreItemResult success() {
            return new RestoreItemResult(true, null, null);
        }

        private static RestoreItemResult failed(String errorCode, String errorMessage) {
            return new RestoreItemResult(false, errorCode, errorMessage);
        }
    }

    private record RestoreItemCounts(long completedDirectoryCount, long failedDirectoryCount) {
    }

    private record PlanClaim(boolean claimed, LocalDateTime startedAt) {
    }

    private static final class RestoreItemProcessingException extends RuntimeException {

        private final String actionType;
        private final String beforeHash;
        private final String expectedAfterHash;
        private final String actualAfterHash;

        private RestoreItemProcessingException(String actionType,
                                               String beforeHash,
                                               String expectedAfterHash,
                                               String actualAfterHash,
                                               Throwable cause) {
            super(cause);
            this.actionType = actionType;
            this.beforeHash = beforeHash;
            this.expectedAfterHash = expectedAfterHash;
            this.actualAfterHash = actualAfterHash;
        }

        private String actionType() {
            return actionType;
        }

        private String beforeHash() {
            return beforeHash;
        }

        private String expectedAfterHash() {
            return expectedAfterHash;
        }

        private String actualAfterHash() {
            return actualAfterHash;
        }
    }
}
