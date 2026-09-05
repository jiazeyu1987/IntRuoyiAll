package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_BATCH_SIZE_SPLITS_GROUP;

@Service
public class DccControlledFileSourceGovernanceBatchService {

    public static final int MAX_BATCH_SIZE = 200;

    @Resource
    private DccControlledFileSourceGovernanceBatchMapper batchMapper;
    @Resource
    private DccControlledFileSourceGovernanceItemMapper itemMapper;
    @Resource
    private DccControlledFileSourceGovernanceManifestService manifestService;
    @Resource
    private DccControlledFileSourceGovernanceExecutionService executionService;

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileSourceGovernanceBatchDO confirmBatch(String taskKey, Long actorId,
                                                                  String manifestSha256,
                                                                  String requestSha256) {
        DccControlledFileSourceGovernanceBatchDO batch = requireBatch(taskKey);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        manifestService.requireVersioned(batch);
        manifestService.requireTenantInScope(batch, tenantId);
        if (Objects.equals(batch.getBatchStatus(), "CONFIRMED")) {
            manifestService.requireConfirmed(batch, manifestSha256, requestSha256);
            return batch;
        }
        if (!Objects.equals(batch.getBatchStatus(), "PREPARED")
                && !Objects.equals(batch.getBatchStatus(), "READY")) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
        if (StrUtil.isBlank(manifestSha256) || StrUtil.isBlank(requestSha256)) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
        batch.setManifestSha256(manifestSha256);
        batch.setRequestSha256(requestSha256);
        batch.setBatchStatus("CONFIRMED");
        batch.setConfirmedBy(actorId);
        batch.setConfirmedTime(LocalDateTime.now());
        batchMapper.updateById(batch);
        return batch;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileSourceGovernanceBatchExecutionResult executeConfirmedBatch(
            String taskKey, int batchSize, String manifestSha256, String requestSha256, Long actorId) {
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("batchSize must be between 1 and " + MAX_BATCH_SIZE);
        }
        DccControlledFileSourceGovernanceBatchDO batch = requireBatch(taskKey);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        manifestService.requireVersioned(batch);
        manifestService.requireTenantInScope(batch, tenantId);
        Set<Long> tenantScope = Set.of(tenantId);
        manifestService.requireConfirmed(batch, manifestSha256, requestSha256);
        List<DccControlledFileSourceGovernanceItemDO> items =
                itemMapper.selectByBatchAndTenant(batch.getId(), tenantId);
        List<DccControlledFileSourceGovernanceItemDO> readyItems = items.stream()
                .filter(item -> Objects.equals(item.getItemStatus(), "READY"))
                .sorted(Comparator.comparing(DccControlledFileSourceGovernanceItemDO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .toList();
        int processed = 0;
        Map<String, List<DccControlledFileSourceGovernanceItemDO>> groups = new LinkedHashMap<>();
        for (DccControlledFileSourceGovernanceItemDO item : readyItems) {
            String groupKey = Objects.equals(item.getGovernanceAction(), "COPY_SHARED_SOURCE")
                    && item.getSharedGroupKey() != null
                    ? item.getSharedGroupKey() : "item:" + item.getId();
            groups.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(item);
        }
        for (List<DccControlledFileSourceGovernanceItemDO> group : groups.values()) {
            if (processed + group.size() > batchSize) {
                if (group.size() > 1) {
                    throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_BATCH_SIZE_SPLITS_GROUP,
                            group.get(0).getSharedGroupKey());
                }
                break;
            }
            if (Objects.equals(group.get(0).getGovernanceAction(), "COPY_SHARED_SOURCE")) {
                executionService.executeSharedGroup(batch, group, tenantScope,
                        manifestSha256, requestSha256, actorId);
            } else {
                executionService.executeItem(batch, group.get(0), tenantScope,
                        manifestSha256, requestSha256, actorId);
            }
            processed += group.size();
        }
        Map<String, Integer> counts = statusCounts(batch.getId(), tenantId);
        int completed = counts.getOrDefault("COMPLETED", 0);
        int blocked = counts.getOrDefault("BLOCKED", 0);
        int failed = counts.getOrDefault("FAILED", 0);
        int remaining = counts.getOrDefault("READY", 0);
        String status = remaining > 0 ? "CONFIRMED"
                : failed > 0 ? "FAILED"
                : blocked > 0 ? "BLOCKED" : "COMPLETED";
        batch.setBatchStatus(status);
        batch.setCompletedCount((long) completed);
        batch.setBlockedCount((long) blocked);
        batch.setFailedCount((long) failed);
        batchMapper.updateById(batch);
        return new DccControlledFileSourceGovernanceBatchExecutionResult(
                taskKey, status, processed, completed, blocked, failed, remaining);
    }

    public List<DccControlledFileSourceGovernanceItemDO> getBlockers(String taskKey) {
        DccControlledFileSourceGovernanceBatchDO batch = requireBatch(taskKey);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        manifestService.requireTenantInScope(batch, tenantId);
        return itemMapper.selectByBatchAndTenant(batch.getId(), tenantId).stream()
                .filter(item -> Objects.equals(item.getItemStatus(), "BLOCKED")
                        || Objects.equals(item.getItemStatus(), "FAILED"))
                .toList();
    }

    private DccControlledFileSourceGovernanceBatchDO requireBatch(String taskKey) {
        if (StrUtil.isBlank(taskKey)) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
        DccControlledFileSourceGovernanceBatchDO batch = batchMapper.selectByTaskKey(taskKey);
        if (batch == null) {
            throw exception(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID);
        }
        return batch;
    }

    private Map<String, Integer> statusCounts(Long batchId, Long tenantId) {
        Map<String, Integer> counts = new HashMap<>();
        for (Map<String, Object> row : itemMapper.selectStatusCountsByBatchAndTenant(batchId, tenantId)) {
            Object status = row.get("itemStatus");
            Object count = row.get("itemCount");
            if (status != null && count instanceof Number number) {
                counts.put(status.toString(), number.intValue());
            }
        }
        return counts;
    }
}
