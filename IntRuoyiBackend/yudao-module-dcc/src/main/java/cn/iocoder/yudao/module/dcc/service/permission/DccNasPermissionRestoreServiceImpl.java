package cn.iocoder.yudao.module.dcc.service.permission;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclAceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclAceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDirectorySnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclIdentityMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestorePlanItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestorePlanMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclSnapshotMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PERMISSION_RESTORE_BLOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PERMISSION_RESTORE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PERMISSION_RESTORE_MODE_UNSUPPORTED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PERMISSION_RESTORE_PLAN_STALE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY;

@Service
@Validated
public class DccNasPermissionRestoreServiceImpl implements DccNasPermissionRestoreService {

    private static final String SNAPSHOT_STATUS_CAPTURED = "CAPTURED";
    private static final String COLLECT_STATUS_SUCCESS = "SUCCESS";
    private static final String MAPPING_STATUS_MAPPED = "MAPPED";
    private static final String RESTORE_MODE_REPLACE_DIRECTORY_RULES = "REPLACE_DIRECTORY_RULES";
    private static final String TARGET_MODEL_DCC_PERMISSION_RULES = "DCC_PERMISSION_RULES";
    private static final String PLAN_STATUS_READY = "READY";
    private static final String PLAN_STATUS_COMPLETED = "COMPLETED";
    private static final String PLAN_STATUS_FAILED = "FAILED";
    private static final String PLAN_ITEM_STATUS_WAITING = "WAITING";
    private static final String PLAN_ITEM_STATUS_VERIFIED = "VERIFIED";
    private static final String PLAN_ITEM_STATUS_FAILED = "FAILED";
    private static final String PLAN_ITEM_STATUS_BLOCKED = "BLOCKED";
    private static final String APPLY_STATUS_WAITING = "WAITING";
    private static final String SEMANTIC_POLICY_VERSION = "DCC_NAS_ACL_TO_DIRECTORY_RULES_V1";
    private static final String IDENTITY_MAPPING_VERSION = "DCC_NAS_IDENTITY_MAPPING_V1";
    private static final boolean RUNTIME_ENFORCEMENT_READY = true;
    private static final String RUNTIME_ENFORCEMENT_BLOCKER = null;
    private static final int SAMPLE_RULE_LIMIT = 20;
    private static final int RESTORE_QUERY_CHUNK_SIZE = 500;
    private static final TypeReference<Map<String, Object>> VALIDATION_SUMMARY_TYPE =
            new TypeReference<>() {
            };

    @Resource
    private DccNasAclSnapshotMapper snapshotMapper;
    @Resource
    private DccNasAclDirectorySnapshotMapper directorySnapshotMapper;
    @Resource
    private DccNasAclAceMapper aceMapper;
    @Resource
    private DccNasAclIdentityMappingMapper identityMappingMapper;
    @Resource
    private DccNasAclRestorePlanMapper restorePlanMapper;
    @Resource
    private DccNasAclRestorePlanItemMapper restorePlanItemMapper;
    @Resource
    private DccDirectoryAccessRuleMapper directoryAccessRuleMapper;

    @Override
    public PreviewResult preview(Long taskId) {
        RestorePlan restorePlan = buildRestorePlan(taskId);
        return new PreviewResult(
                restorePlan.snapshot().getTransferTaskId(),
                restorePlan.blockers().isEmpty(),
                restorePlan.planHash(),
                RESTORE_MODE_REPLACE_DIRECTORY_RULES,
                restorePlan.directoryCount(),
                restorePlan.rules().size(),
                RUNTIME_ENFORCEMENT_READY,
                RUNTIME_ENFORCEMENT_BLOCKER,
                restorePlan.blockers(),
                sampleRules(restorePlan.rules()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplyResult apply(ApplyRestoreCommand command) {
        requireNonNull(command, "command");
        requireNotBlank(command.restoreMode(), "restoreMode");
        if (!RESTORE_MODE_REPLACE_DIRECTORY_RULES.equals(command.restoreMode())) {
            throw exception(DCC_NAS_PERMISSION_RESTORE_MODE_UNSUPPORTED, command.restoreMode());
        }
        requireNotBlank(command.planHash(), "planHash");
        requireNotBlank(command.idempotencyKey(), "idempotencyKey");
        requireNonNull(command.operatorUserId(), "operatorUserId");

        String idempotencyRequestHash = idempotencyRequestHash(command);
        DccNasAclRestorePlanDO existingPlan = restorePlanMapper.selectOne(
                new LambdaQueryWrapperX<DccNasAclRestorePlanDO>()
                        .eq(DccNasAclRestorePlanDO::getPlanKey, planKey(command)));
        if (existingPlan != null) {
            return existingApplyResult(existingPlan, command, idempotencyRequestHash);
        }

        RestorePlan restorePlan = buildRestorePlan(command.taskId());
        if (!restorePlan.blockers().isEmpty()) {
            throw exception(DCC_NAS_PERMISSION_RESTORE_BLOCKED, restorePlan.blockers().size());
        }
        if (!restorePlan.planHash().equals(command.planHash())) {
            throw exception(DCC_NAS_PERMISSION_RESTORE_PLAN_STALE);
        }

        DccNasAclRestorePlanDO plan = DccNasAclRestorePlanDO.builder()
                .snapshotId(restorePlan.snapshot().getId())
                .transferTaskId(restorePlan.snapshot().getTransferTaskId())
                .planKey(planKey(command))
                .targetModel(TARGET_MODEL_DCC_PERMISSION_RULES)
                .status(PLAN_STATUS_READY)
                .semanticPolicyVersion(SEMANTIC_POLICY_VERSION)
                .identityMappingVersion(IDENTITY_MAPPING_VERSION)
                .validationSummaryJson(validationSummaryJson(restorePlan, idempotencyRequestHash))
                .createdByUserId(command.operatorUserId())
                .build();
        restorePlanMapper.insert(plan);
        requireNonNull(plan.getId(), "restore plan id");

        for (DirectoryPlanItem directoryPlanItem : toDirectoryPlanItems(restorePlan.rules())) {
            Long directoryId = directoryPlanItem.directorySnapshot().getDccDirectoryId();
            requireNonNull(directoryId, "dccDirectoryId");
            List<DccDirectoryAccessRuleDO> targetRules = targetDirectoryRules(directoryPlanItem.rules(),
                    command.changeReason());
            String expectedCurrentRuleHash = DccDirectoryAccessRuleCanonicalHash.directoryRulesHash(
                    restorePlan.runtimeRulesByDirectoryId().getOrDefault(directoryId, List.of()));
            String expectedAfterHash = DccDirectoryAccessRuleCanonicalHash.directoryRulesHash(targetRules);
            String plannedOperationsJson = plannedOperationsJson(directoryId, expectedCurrentRuleHash,
                    expectedAfterHash, targetRules, command.changeReason());
            DccNasAclRestorePlanItemDO item = DccNasAclRestorePlanItemDO.builder()
                    .planId(plan.getId())
                    .directorySnapshotId(directoryPlanItem.directorySnapshot().getId())
                    .transferTaskItemId(directoryPlanItem.directorySnapshot().getTransferTaskItemId())
                    .dccDirectoryId(directoryId)
                    .sourceDescriptorId(directoryPlanItem.directorySnapshot().getDescriptorId())
                    .plannedOperationsHash(sha256Hex(plannedOperationsJson))
                    .plannedOperationsJson(plannedOperationsJson)
                    .status(PLAN_ITEM_STATUS_WAITING)
                    .expectedAfterHash(expectedAfterHash)
                    .build();
            restorePlanItemMapper.insert(item);
        }

        return new ApplyResult(plan.getId(), restorePlan.snapshot().getTransferTaskId(), APPLY_STATUS_WAITING,
                restorePlan.directoryCount(), restorePlan.rules().size(), 0L, 0L);
    }

    @Override
    public RestoreStatusResult getStatus(Long taskId, Long restoreId) {
        requireNonNull(taskId, "taskId");
        requireNonNull(restoreId, "restoreId");
        DccNasAclRestorePlanDO plan = restorePlanMapper.selectById(restoreId);
        if (plan == null || !Objects.equals(taskId, plan.getTransferTaskId())) {
            throw new IllegalArgumentException("restore plan not found: " + restoreId);
        }
        Map<String, Object> summary = validationSummary(plan);
        long directoryCount = requiredSummaryLong(summary, "directoryCount", restoreId);
        long ruleCount = requiredSummaryLong(summary, "ruleCount", restoreId);
        RestoreProgressCounts progressCounts = restoreProgressCounts(plan, summary, restoreId);
        return new RestoreStatusResult(
                plan.getId(),
                plan.getTransferTaskId(),
                plan.getStatus(),
                directoryCount,
                ruleCount,
                progressCounts.completedDirectoryCount(),
                progressCounts.failedDirectoryCount(),
                plan.getFailureMessage(),
                plan.getStartedAt(),
                plan.getCompletedAt());
    }

    private RestoreProgressCounts restoreProgressCounts(DccNasAclRestorePlanDO plan,
                                                        Map<String, Object> summary,
                                                        Long restoreId) {
        if (isFinalPlanStatus(plan.getStatus())) {
            return new RestoreProgressCounts(
                    requiredSummaryLong(summary, "completedDirectoryCount", restoreId),
                    requiredSummaryLong(summary, "failedDirectoryCount", restoreId));
        }
        List<DccNasAclRestorePlanItemDO> items = restorePlanItemMapper.selectList(
                new LambdaQueryWrapperX<DccNasAclRestorePlanItemDO>()
                        .eq(DccNasAclRestorePlanItemDO::getPlanId, plan.getId()));
        requireNonNull(items, "restorePlanItems");
        long completedDirectoryCount = 0L;
        long failedDirectoryCount = 0L;
        for (DccNasAclRestorePlanItemDO item : items) {
            if (PLAN_ITEM_STATUS_VERIFIED.equals(item.getStatus())) {
                completedDirectoryCount++;
            } else if (PLAN_ITEM_STATUS_FAILED.equals(item.getStatus())
                    || PLAN_ITEM_STATUS_BLOCKED.equals(item.getStatus())) {
                failedDirectoryCount++;
            }
        }
        return new RestoreProgressCounts(completedDirectoryCount, failedDirectoryCount);
    }

    private boolean isFinalPlanStatus(String status) {
        return PLAN_STATUS_COMPLETED.equals(status) || PLAN_STATUS_FAILED.equals(status);
    }

    private RestorePlan buildRestorePlan(Long taskId) {
        requireNonNull(taskId, "taskId");
        DccNasAclSnapshotDO snapshotRef = snapshotMapper.selectOne(new LambdaQueryWrapperX<DccNasAclSnapshotDO>()
                .eq(DccNasAclSnapshotDO::getTransferTaskId, taskId)
                .eq(DccNasAclSnapshotDO::getStatus, SNAPSHOT_STATUS_CAPTURED));
        if (snapshotRef == null) {
            throw exception(DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY, taskId);
        }
        DccNasAclSnapshotDO snapshot = snapshotMapper.selectById(snapshotRef.getId());
        if (snapshot == null || !SNAPSHOT_STATUS_CAPTURED.equals(snapshot.getStatus())) {
            throw exception(DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY, taskId);
        }

        List<DccNasAclDirectorySnapshotDO> directorySnapshots = directorySnapshotMapper.selectList(
                new LambdaQueryWrapperX<DccNasAclDirectorySnapshotDO>()
                        .eq(DccNasAclDirectorySnapshotDO::getTransferTaskId, taskId)
                        .eq(DccNasAclDirectorySnapshotDO::getSnapshotId, snapshot.getId())
                        .eq(DccNasAclDirectorySnapshotDO::getCollectStatus, COLLECT_STATUS_SUCCESS));
        requireNonNull(directorySnapshots, "directorySnapshots");

        List<DccNasAclDirectorySnapshotDO> sortedDirectorySnapshots = sortedDirectorySnapshots(directorySnapshots);
        Map<Long, List<DccNasAclAceDO>> acesByDescriptorId = selectAcesByDescriptorId(sortedDirectorySnapshots);
        Map<String, DccNasAclIdentityMappingDO> mappingsBySidHash = selectMappingsBySidHash(acesByDescriptorId);
        Map<Long, List<DccDirectoryAccessRuleDO>> runtimeRulesByDirectoryId =
                selectRuntimeRulesByDirectoryId(sortedDirectorySnapshots);

        List<RulePlan> rules = new ArrayList<>();
        List<RestoreBlocker> blockers = new ArrayList<>();
        for (DccNasAclDirectorySnapshotDO directorySnapshot : sortedDirectorySnapshots) {
            if (directorySnapshot.getDescriptorId() == null) {
                throw new IllegalStateException("directory snapshot descriptorId required: " + directorySnapshot.getId());
            }
            List<DccNasAclAceDO> aces = acesByDescriptorId.getOrDefault(directorySnapshot.getDescriptorId(), List.of());
            for (DccNasAclAceDO ace : sortedAces(aces)) {
                appendPreviewItem(directorySnapshot, ace, mappingsBySidHash, rules, blockers);
            }
        }

        String planHash = planHash(snapshot, sortedDirectorySnapshots.size(), rules, blockers,
                runtimeRulesByDirectoryId);
        return new RestorePlan(snapshot, sortedDirectorySnapshots.size(), rules, blockers, planHash,
                runtimeRulesByDirectoryId);
    }

    private void appendPreviewItem(DccNasAclDirectorySnapshotDO directorySnapshot,
                                   DccNasAclAceDO ace,
                                   Map<String, DccNasAclIdentityMappingDO> mappingsBySidHash,
                                   List<RulePlan> rules,
                                   List<RestoreBlocker> blockers) {
        if (DccNasAclAceTypeMapper.isDeny(ace.getAceType())) {
            blockers.add(new RestoreBlocker("DCC_NAS_ACL_DENY_UNSUPPORTED",
                    "NAS ACL contains explicit DENY ACE and cannot be restored to allow-only DCC rules",
                    directorySnapshot.getId(), directorySnapshot.getNasPath(), ace.getTrusteeSid()));
            return;
        }
        DccNasAclAccessMaskMapper.DccPermissions permissions =
                DccNasAclAccessMaskMapper.toDccPermissions(ace.getAccessMask());
        if (!DccNasAclAceTypeMapper.isAllow(ace.getAceType()) || permissions == null) {
            blockers.add(new RestoreBlocker("DCC_NAS_ACL_SPECIAL_MASK_UNSUPPORTED",
                    "NAS ACL accessMask cannot be restored to DCC query/preview/download: " + ace.getAccessMask(),
                    directorySnapshot.getId(), directorySnapshot.getNasPath(), ace.getTrusteeSid()));
            return;
        }

        String trusteeSidHash = StrUtil.trimToNull(ace.getTrusteeSidHash());
        DccNasAclIdentityMappingDO mapping = trusteeSidHash == null ? null : mappingsBySidHash.get(trusteeSidHash);
        if (mapping == null) {
            blockers.add(new RestoreBlocker("DCC_NAS_PRINCIPAL_UNMAPPED",
                    "NAS principal is not mapped to a DCC subject: " + ace.getTrusteeSid(),
                    directorySnapshot.getId(), directorySnapshot.getNasPath(), ace.getTrusteeSid()));
            return;
        }
        if (StrUtil.isBlank(mapping.getDccSubjectType()) || mapping.getDccSubjectId() == null) {
            throw new IllegalStateException("mapped DCC subject required for SID hash: " + trusteeSidHash);
        }

        rules.add(new RulePlan(directorySnapshot, ace, mapping.getDccSubjectType(), mapping.getDccSubjectId(),
                permissions.canQuery(), permissions.canPreview(), permissions.canDownload()));
    }

    private Map<Long, List<DccNasAclAceDO>> selectAcesByDescriptorId(List<DccNasAclDirectorySnapshotDO> directorySnapshots) {
        Set<Long> descriptorIds = new LinkedHashSet<>();
        for (DccNasAclDirectorySnapshotDO directorySnapshot : directorySnapshots) {
            if (directorySnapshot.getDescriptorId() != null) {
                descriptorIds.add(directorySnapshot.getDescriptorId());
            }
        }
        if (descriptorIds.isEmpty()) {
            return Map.of();
        }

        List<DccNasAclAceDO> aces = aceMapper.selectList(new LambdaQueryWrapperX<DccNasAclAceDO>()
                .in(DccNasAclAceDO::getDescriptorId, descriptorIds));
        requireNonNull(aces, "aces");
        Map<Long, List<DccNasAclAceDO>> acesByDescriptorId = new LinkedHashMap<>();
        for (DccNasAclAceDO ace : aces) {
            if (ace.getDescriptorId() == null) {
                continue;
            }
            acesByDescriptorId.computeIfAbsent(ace.getDescriptorId(), ignored -> new ArrayList<>()).add(ace);
        }
        return acesByDescriptorId;
    }

    private Map<String, DccNasAclIdentityMappingDO> selectMappingsBySidHash(Map<Long, List<DccNasAclAceDO>> acesByDescriptorId) {
        Set<String> sidHashes = new LinkedHashSet<>();
        for (List<DccNasAclAceDO> aces : acesByDescriptorId.values()) {
            for (DccNasAclAceDO ace : aces) {
                if (StrUtil.isNotBlank(ace.getTrusteeSidHash())) {
                    sidHashes.add(ace.getTrusteeSidHash());
                }
            }
        }
        if (sidHashes.isEmpty()) {
            return Map.of();
        }

        Map<String, DccNasAclIdentityMappingDO> mappingsBySidHash = new LinkedHashMap<>();
        for (List<String> sidHashChunk : chunks(sidHashes)) {
            List<DccNasAclIdentityMappingDO> mappings = identityMappingMapper.selectList(
                    new LambdaQueryWrapperX<DccNasAclIdentityMappingDO>()
                            .in(DccNasAclIdentityMappingDO::getSidHash, sidHashChunk)
                            .eq(DccNasAclIdentityMappingDO::getMappingStatus, MAPPING_STATUS_MAPPED));
            requireNonNull(mappings, "identity mappings");
            for (DccNasAclIdentityMappingDO mapping : mappings) {
                putMapping(mappingsBySidHash, mapping);
            }
        }
        return mappingsBySidHash;
    }

    private void putMapping(Map<String, DccNasAclIdentityMappingDO> mappingsBySidHash,
                            DccNasAclIdentityMappingDO mapping) {
        if (mapping != null && StrUtil.isNotBlank(mapping.getSidHash())) {
            mappingsBySidHash.put(mapping.getSidHash(), mapping);
        }
    }

    private Map<Long, List<DccDirectoryAccessRuleDO>> selectRuntimeRulesByDirectoryId(
            List<DccNasAclDirectorySnapshotDO> directorySnapshots) {
        Set<Long> directoryIds = new LinkedHashSet<>();
        for (DccNasAclDirectorySnapshotDO directorySnapshot : directorySnapshots) {
            if (directorySnapshot.getDccDirectoryId() != null) {
                directoryIds.add(directorySnapshot.getDccDirectoryId());
            }
        }
        if (directoryIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<DccDirectoryAccessRuleDO>> runtimeRulesByDirectoryId = new LinkedHashMap<>();
        for (Long directoryId : directoryIds) {
            runtimeRulesByDirectoryId.put(directoryId, new ArrayList<>());
        }
        for (List<Long> directoryIdChunk : chunks(directoryIds)) {
            List<DccDirectoryAccessRuleDO> rules = directoryAccessRuleMapper.selectList(
                    new LambdaQueryWrapperX<DccDirectoryAccessRuleDO>()
                            .in(DccDirectoryAccessRuleDO::getDirectoryId, directoryIdChunk));
            requireNonNull(rules, "runtime directory access rules");
            for (DccDirectoryAccessRuleDO rule : rules) {
                if (rule.getDirectoryId() != null && runtimeRulesByDirectoryId.containsKey(rule.getDirectoryId())) {
                    runtimeRulesByDirectoryId.get(rule.getDirectoryId()).add(rule);
                }
            }
        }
        return runtimeRulesByDirectoryId;
    }

    private <T> List<List<T>> chunks(Set<T> values) {
        List<T> orderedValues = List.copyOf(values);
        List<List<T>> chunks = new ArrayList<>();
        for (int index = 0; index < orderedValues.size(); index += RESTORE_QUERY_CHUNK_SIZE) {
            chunks.add(orderedValues.subList(index,
                    Math.min(index + RESTORE_QUERY_CHUNK_SIZE, orderedValues.size())));
        }
        return chunks;
    }

    private List<DccNasAclDirectorySnapshotDO> sortedDirectorySnapshots(List<DccNasAclDirectorySnapshotDO> directorySnapshots) {
        return directorySnapshots.stream()
                .sorted(Comparator.comparing(DccNasAclDirectorySnapshotDO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private List<DccNasAclAceDO> sortedAces(List<DccNasAclAceDO> aces) {
        return aces.stream()
                .sorted(Comparator.comparing(DccNasAclAceDO::getAceIndex,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccNasAclAceDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private List<RestoreRulePreview> sampleRules(List<RulePlan> rules) {
        return rules.stream()
                .limit(SAMPLE_RULE_LIMIT)
                .map(rule -> new RestoreRulePreview(rule.directorySnapshot().getDccDirectoryId(),
                        rule.directorySnapshot().getNasPath(), rule.subjectType(), rule.subjectId(),
                        rule.canQuery(), rule.canPreview(), rule.canDownload()))
                .toList();
    }

    private List<DirectoryPlanItem> toDirectoryPlanItems(List<RulePlan> rules) {
        Map<Long, DirectoryPlanItemBuilder> buildersByDirectorySnapshotId = new LinkedHashMap<>();
        for (RulePlan rule : rules) {
            Long directorySnapshotId = rule.directorySnapshot().getId();
            DirectoryPlanItemBuilder builder = buildersByDirectorySnapshotId.computeIfAbsent(directorySnapshotId,
                    ignored -> new DirectoryPlanItemBuilder(rule.directorySnapshot()));
            builder.rules().add(rule);
        }
        return buildersByDirectorySnapshotId.values().stream()
                .map(builder -> new DirectoryPlanItem(builder.directorySnapshot(), builder.rules()))
                .toList();
    }

    private String validationSummaryJson(RestorePlan restorePlan, String idempotencyRequestHash) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("planHash", restorePlan.planHash());
        summary.put("restoreMode", RESTORE_MODE_REPLACE_DIRECTORY_RULES);
        summary.put("directoryCount", restorePlan.directoryCount());
        summary.put("ruleCount", restorePlan.rules().size());
        summary.put("completedDirectoryCount", 0L);
        summary.put("failedDirectoryCount", 0L);
        summary.put("blockerCount", restorePlan.blockers().size());
        summary.put("idempotencyRequestHash", idempotencyRequestHash);
        return JsonUtils.toJsonString(summary);
    }

    private List<DccDirectoryAccessRuleDO> targetDirectoryRules(List<RulePlan> rules, String changeReason) {
        String normalizedChangeReason = StrUtil.trimToNull(changeReason);
        List<DccDirectoryAccessRuleDO> targetRules = new ArrayList<>();
        for (RulePlan rule : rules) {
            targetRules.add(DccDirectoryAccessRuleDO.builder()
                    .directoryId(rule.directorySnapshot().getDccDirectoryId())
                    .subjectType(rule.subjectType())
                    .subjectId(rule.subjectId())
                    .canQuery(rule.canQuery())
                    .canPreview(rule.canPreview())
                    .canDownload(rule.canDownload())
                    .active(Boolean.TRUE)
                    .changeReason(normalizedChangeReason)
                    .build());
        }
        return targetRules;
    }

    private String plannedOperationsJson(Long directoryId,
                                         String expectedCurrentRuleHash,
                                         String expectedAfterHash,
                                         List<DccDirectoryAccessRuleDO> targetRules,
                                         String changeReason) {
        Map<String, Object> operations = new LinkedHashMap<>();
        operations.put("restoreMode", RESTORE_MODE_REPLACE_DIRECTORY_RULES);
        operations.put("directoryId", directoryId);
        operations.put("expectedCurrentRuleHash", expectedCurrentRuleHash);
        operations.put("expectedAfterHash", expectedAfterHash);
        operations.put("changeReason", StrUtil.trimToNull(changeReason));
        List<Map<String, Object>> replaceRules = new ArrayList<>();
        for (DccDirectoryAccessRuleDO targetRule : targetRules.stream()
                .sorted(DccDirectoryAccessRuleCanonicalHash.ruleComparator())
                .toList()) {
            replaceRules.add(DccDirectoryAccessRuleCanonicalHash.rulePayload(targetRule));
        }
        operations.put("replaceDirectoryRules", replaceRules);
        return JsonUtils.toJsonString(operations);
    }

    private String planHash(DccNasAclSnapshotDO snapshot,
                            long directoryCount,
                            List<RulePlan> rules,
                            List<RestoreBlocker> blockers,
                            Map<Long, List<DccDirectoryAccessRuleDO>> runtimeRulesByDirectoryId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("snapshotId", snapshot.getId());
        payload.put("transferTaskId", snapshot.getTransferTaskId());
        payload.put("snapshotKey", snapshot.getSnapshotKey());
        payload.put("restoreMode", RESTORE_MODE_REPLACE_DIRECTORY_RULES);
        payload.put("semanticPolicyVersion", SEMANTIC_POLICY_VERSION);
        payload.put("identityMappingVersion", IDENTITY_MAPPING_VERSION);
        payload.put("directoryCount", directoryCount);
        payload.put("rules", planHashRules(rules));
        payload.put("blockers", planHashBlockers(blockers));
        payload.put("runtimeDirectoryRules", planHashRuntimeRules(runtimeRulesByDirectoryId));
        return "sha256:" + sha256Hex(JsonUtils.toJsonString(payload));
    }

    private List<Map<String, Object>> planHashRules(List<RulePlan> rules) {
        List<Map<String, Object>> planRules = new ArrayList<>();
        for (RulePlan rule : rules) {
            Map<String, Object> planRule = new LinkedHashMap<>();
            planRule.put("directorySnapshotId", rule.directorySnapshot().getId());
            planRule.put("dccDirectoryId", rule.directorySnapshot().getDccDirectoryId());
            planRule.put("sourceDescriptorId", rule.directorySnapshot().getDescriptorId());
            planRule.put("aceId", rule.ace().getId());
            planRule.put("aceIndex", rule.ace().getAceIndex());
            planRule.put("trusteeSidHash", rule.ace().getTrusteeSidHash());
            planRule.put("subjectType", rule.subjectType());
            planRule.put("subjectId", rule.subjectId());
            planRule.put("canQuery", rule.canQuery());
            planRule.put("canPreview", rule.canPreview());
            planRule.put("canDownload", rule.canDownload());
            planRules.add(planRule);
        }
        return planRules;
    }

    private List<Map<String, Object>> planHashBlockers(List<RestoreBlocker> blockers) {
        List<Map<String, Object>> planBlockers = new ArrayList<>();
        for (RestoreBlocker blocker : blockers) {
            Map<String, Object> planBlocker = new LinkedHashMap<>();
            planBlocker.put("code", blocker.code());
            planBlocker.put("directorySnapshotId", blocker.directorySnapshotId());
            planBlocker.put("nasPath", blocker.nasPath());
            planBlocker.put("trusteeSid", blocker.trusteeSid());
            planBlockers.add(planBlocker);
        }
        return planBlockers;
    }

    private List<Map<String, Object>> planHashRuntimeRules(
            Map<Long, List<DccDirectoryAccessRuleDO>> runtimeRulesByDirectoryId) {
        List<Map<String, Object>> runtimeRules = new ArrayList<>();
        for (Map.Entry<Long, List<DccDirectoryAccessRuleDO>> entry : runtimeRulesByDirectoryId.entrySet()) {
            for (DccDirectoryAccessRuleDO rule : entry.getValue().stream()
                    .sorted(Comparator.comparing(DccDirectoryAccessRuleDO::getId,
                            Comparator.nullsLast(Long::compareTo)))
                    .toList()) {
                Map<String, Object> runtimeRule = new LinkedHashMap<>();
                runtimeRule.put("directoryId", entry.getKey());
                runtimeRule.put("subjectType", rule.getSubjectType());
                runtimeRule.put("subjectId", rule.getSubjectId());
                runtimeRule.put("canQuery", rule.getCanQuery());
                runtimeRule.put("canPreview", rule.getCanPreview());
                runtimeRule.put("canDownload", rule.getCanDownload());
                runtimeRule.put("active", rule.getActive());
                runtimeRule.put("changeReason", rule.getChangeReason());
                runtimeRules.add(runtimeRule);
            }
        }
        return runtimeRules;
    }

    private ApplyResult existingApplyResult(DccNasAclRestorePlanDO existingPlan,
                                            ApplyRestoreCommand command,
                                            String idempotencyRequestHash) {
        Map<String, Object> summary = validationSummary(existingPlan, command);
        if (!Objects.equals(existingPlan.getTransferTaskId(), command.taskId())
                || !TARGET_MODEL_DCC_PERMISSION_RULES.equals(existingPlan.getTargetModel())
                || !SEMANTIC_POLICY_VERSION.equals(existingPlan.getSemanticPolicyVersion())
                || !IDENTITY_MAPPING_VERSION.equals(existingPlan.getIdentityMappingVersion())
                || !Objects.equals(summary.get("planHash"), command.planHash())
                || !RESTORE_MODE_REPLACE_DIRECTORY_RULES.equals(summary.get("restoreMode"))
                || !Objects.equals(summary.get("idempotencyRequestHash"), idempotencyRequestHash)) {
            throw exception(DCC_NAS_PERMISSION_RESTORE_IDEMPOTENCY_CONFLICT, command.idempotencyKey());
        }
        return new ApplyResult(existingPlan.getId(), existingPlan.getTransferTaskId(), APPLY_STATUS_WAITING,
                requiredSummaryLong(summary, "directoryCount", command),
                requiredSummaryLong(summary, "ruleCount", command),
                0L,
                0L);
    }

    private Map<String, Object> validationSummary(DccNasAclRestorePlanDO existingPlan,
                                                  ApplyRestoreCommand command) {
        Map<String, Object> summary =
                JsonUtils.parseObject(existingPlan.getValidationSummaryJson(), VALIDATION_SUMMARY_TYPE);
        if (summary == null) {
            throw exception(DCC_NAS_PERMISSION_RESTORE_IDEMPOTENCY_CONFLICT, command.idempotencyKey());
        }
        return summary;
    }

    private Map<String, Object> validationSummary(DccNasAclRestorePlanDO plan) {
        Map<String, Object> summary =
                JsonUtils.parseObject(plan.getValidationSummaryJson(), VALIDATION_SUMMARY_TYPE);
        if (summary == null) {
            throw new IllegalStateException("restore plan validationSummaryJson required: " + plan.getId());
        }
        return summary;
    }

    private long requiredSummaryLong(Map<String, Object> summary,
                                     String fieldName,
                                     ApplyRestoreCommand command) {
        Object value = summary.get(fieldName);
        if (!(value instanceof Number number)) {
            throw exception(DCC_NAS_PERMISSION_RESTORE_IDEMPOTENCY_CONFLICT, command.idempotencyKey());
        }
        return number.longValue();
    }

    private long requiredSummaryLong(Map<String, Object> summary,
                                     String fieldName,
                                     Long restoreId) {
        Object value = summary.get(fieldName);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("restore plan " + restoreId + " validationSummaryJson."
                    + fieldName + " required");
        }
        return number.longValue();
    }

    private String idempotencyRequestHash(ApplyRestoreCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", command.taskId());
        payload.put("idempotencyKey", command.idempotencyKey());
        payload.put("planHash", command.planHash());
        payload.put("restoreMode", command.restoreMode());
        payload.put("changeReason", StrUtil.trimToNull(command.changeReason()));
        payload.put("operatorUserId", command.operatorUserId());
        return "sha256:" + sha256Hex(JsonUtils.toJsonString(payload));
    }

    private String planKey(ApplyRestoreCommand command) {
        return sha256Hex("DCC_NAS_ACL_RESTORE_PLAN:" + command.taskId() + ":" + command.idempotencyKey());
    }

    private void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " required");
        }
    }

    private void requireNotBlank(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " required");
        }
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private record RestorePlan(DccNasAclSnapshotDO snapshot,
                               long directoryCount,
                               List<RulePlan> rules,
                               List<RestoreBlocker> blockers,
                               String planHash,
                               Map<Long, List<DccDirectoryAccessRuleDO>> runtimeRulesByDirectoryId) {
    }

    private record RulePlan(DccNasAclDirectorySnapshotDO directorySnapshot,
                            DccNasAclAceDO ace,
                            String subjectType,
                            Long subjectId,
                            boolean canQuery,
                            boolean canPreview,
                            boolean canDownload) {
    }

    private record DirectoryPlanItem(DccNasAclDirectorySnapshotDO directorySnapshot,
                                     List<RulePlan> rules) {
    }

    private record DirectoryPlanItemBuilder(DccNasAclDirectorySnapshotDO directorySnapshot,
                                            List<RulePlan> rules) {

        private DirectoryPlanItemBuilder(DccNasAclDirectorySnapshotDO directorySnapshot) {
            this(directorySnapshot, new ArrayList<>());
        }
    }

    private record RestoreProgressCounts(long completedDirectoryCount,
                                         long failedDirectoryCount) {
    }

}
