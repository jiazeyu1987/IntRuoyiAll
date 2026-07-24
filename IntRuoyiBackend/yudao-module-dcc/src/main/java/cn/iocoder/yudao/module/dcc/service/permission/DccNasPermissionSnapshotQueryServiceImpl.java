package cn.iocoder.yudao.module.dcc.service.permission;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclAceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclAceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDirectorySnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclIdentityMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclSnapshotMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY;

@Service
@Validated
public class DccNasPermissionSnapshotQueryServiceImpl implements DccNasPermissionSnapshotQueryService {

    private static final String SNAPSHOT_STATUS_CAPTURED = "CAPTURED";
    private static final String SNAPSHOT_STATUS_NOT_COLLECTED = "NOT_COLLECTED";
    private static final String COLLECT_STATUS_SUCCESS = "SUCCESS";
    private static final String ITEM_STATUS_BLOCKED = "BLOCKED";
    private static final String MAPPING_STATUS_MAPPED = "MAPPED";
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 100;

    @Resource
    private DccNasAclSnapshotMapper snapshotMapper;
    @Resource
    private DccNasAclDirectorySnapshotMapper directorySnapshotMapper;
    @Resource
    private DccNasAclAceMapper aceMapper;
    @Resource
    private DccNasAclIdentityMappingMapper identityMappingMapper;
    @Resource
    private DccControlledFileNasTransferTaskMapper transferTaskMapper;

    @Override
    public SummaryResult getSummary(Long taskId) {
        requireNonNull(taskId, "taskId");
        DccNasAclSnapshotDO snapshot = selectLatestSnapshot(taskId);
        if (snapshot == null) {
            return notCollectedSummary(taskId);
        }
        SnapshotQueryContext context = queryContext(snapshot);
        long aceCount = context.aces().size();
        long unsupportedAceCount = context.aces().stream().filter(this::isUnsupportedAce).count();
        long unmappedPrincipalCount = unmappedSidHashes(context).size();
        long blockerCount = context.directorySnapshots().stream()
                .mapToLong(directorySnapshot -> blockers(directorySnapshot, context).size())
                .sum();
        return new SummaryResult(
                context.snapshot().getTransferTaskId(),
                context.snapshot().getStatus(),
                JsonUtils.parseArray(StrUtil.blankToDefault(context.snapshot().getRootPathsJson(), "[]"),
                        String.class),
                context.directorySnapshots().size(),
                aceCount,
                unsupportedAceCount,
                unmappedPrincipalCount,
                blockerCount,
                context.snapshot().getCompletedAt(),
                context.snapshot().getFailureMessage(),
                SNAPSHOT_STATUS_CAPTURED.equals(context.snapshot().getStatus()) && blockerCount == 0);
    }

    @Override
    public PageResult<ItemResult> getItems(Long taskId, Integer pageNo, Integer pageSize, String status) {
        requireNonNull(taskId, "taskId");
        DccNasAclSnapshotDO snapshot = selectLatestSnapshot(taskId);
        if (snapshot == null) {
            throw exception(DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY, taskId);
        }
        SnapshotQueryContext context = queryContext(snapshot);
        List<ItemResult> allItems = context.directorySnapshots().stream()
                .map(directorySnapshot -> itemResult(directorySnapshot, context))
                .filter(item -> StrUtil.isBlank(status) || Objects.equals(status, item.snapshotStatus()))
                .toList();

        int safePageNo = pageNo == null || pageNo < 1 ? DEFAULT_PAGE_NO : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, allItems.size());
        int toIndex = Math.min(fromIndex + safePageSize, allItems.size());
        return new PageResult<>(allItems.subList(fromIndex, toIndex), (long) allItems.size());
    }

    private DccNasAclSnapshotDO selectLatestSnapshot(Long taskId) {
        return snapshotMapper.selectOne(new LambdaQueryWrapperX<DccNasAclSnapshotDO>()
                .eq(DccNasAclSnapshotDO::getTransferTaskId, taskId)
                .orderByDesc(DccNasAclSnapshotDO::getId)
                .last("LIMIT 1"));
    }

    private SummaryResult notCollectedSummary(Long taskId) {
        DccControlledFileNasTransferTaskDO task = transferTaskMapper.selectById(taskId);
        if (task == null) {
            throw exception(DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY, taskId);
        }
        return new SummaryResult(
                task.getId(),
                SNAPSHOT_STATUS_NOT_COLLECTED,
                JsonUtils.parseArray(StrUtil.blankToDefault(task.getSelectedNasPathsJson(), "[]"), String.class),
                0L,
                0L,
                0L,
                0L,
                0L,
                null,
                task.getLastFailureMessage(),
                false);
    }

    private SnapshotQueryContext queryContext(DccNasAclSnapshotDO snapshot) {
        requireNonNull(snapshot, "snapshot");
        Long taskId = snapshot.getTransferTaskId();
        requireNonNull(taskId, "snapshot.transferTaskId");

        List<DccNasAclDirectorySnapshotDO> directorySnapshots = directorySnapshotMapper.selectList(
                new LambdaQueryWrapperX<DccNasAclDirectorySnapshotDO>()
                        .eq(DccNasAclDirectorySnapshotDO::getTransferTaskId, taskId)
                        .eq(DccNasAclDirectorySnapshotDO::getSnapshotId, snapshot.getId())
                        .orderByDesc(DccNasAclDirectorySnapshotDO::getId));
        requireNonNull(directorySnapshots, "directorySnapshots");
        List<DccNasAclDirectorySnapshotDO> sortedDirectorySnapshots = directorySnapshots.stream()
                .sorted(Comparator.comparing(DccNasAclDirectorySnapshotDO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .toList();

        Map<Long, List<DccNasAclAceDO>> acesByDescriptorId = selectAcesByDescriptorId(sortedDirectorySnapshots);
        List<DccNasAclAceDO> aces = acesByDescriptorId.values().stream().flatMap(List::stream).toList();
        Set<String> mappedSidHashes = selectMappedSidHashes(aces);
        return new SnapshotQueryContext(snapshot, sortedDirectorySnapshots, acesByDescriptorId, aces, mappedSidHashes);
    }

    private ItemResult itemResult(DccNasAclDirectorySnapshotDO directorySnapshot, SnapshotQueryContext context) {
        List<DccNasAclAceDO> aces = acesFor(directorySnapshot, context);
        List<BlockerResult> blockers = blockers(directorySnapshot, context);
        String snapshotStatus = collectStatus(directorySnapshot, blockers);
        return new ItemResult(directorySnapshot.getTransferTaskItemId(), directorySnapshot.getNasPath(),
                directorySnapshot.getDccDirectoryId(), snapshotStatus, aces.size(), blockers);
    }

    private String collectStatus(DccNasAclDirectorySnapshotDO directorySnapshot, List<BlockerResult> blockers) {
        if (!COLLECT_STATUS_SUCCESS.equals(directorySnapshot.getCollectStatus())) {
            return directorySnapshot.getCollectStatus();
        }
        return blockers.isEmpty() ? SNAPSHOT_STATUS_CAPTURED : ITEM_STATUS_BLOCKED;
    }

    private List<BlockerResult> blockers(DccNasAclDirectorySnapshotDO directorySnapshot,
                                         SnapshotQueryContext context) {
        List<BlockerResult> blockers = new ArrayList<>();
        if (!COLLECT_STATUS_SUCCESS.equals(directorySnapshot.getCollectStatus())) {
            blockers.add(new BlockerResult(
                    StrUtil.blankToDefault(directorySnapshot.getFailureCode(), "DCC_NAS_ACL_COLLECT_FAILED"),
                    StrUtil.blankToDefault(directorySnapshot.getFailureMessage(), "NAS ACL snapshot collection failed"),
                    null,
                    null));
            return blockers;
        }

        List<DccNasAclAceDO> aces = acesFor(directorySnapshot, context);
        for (DccNasAclAceDO ace : sortedAces(aces)) {
            if (DccNasAclAceTypeMapper.isDeny(ace.getAceType())) {
                blockers.add(new BlockerResult("DCC_NAS_ACL_DENY_UNSUPPORTED",
                        "NAS ACL contains explicit DENY ACE",
                        ace.getTrusteeSid(),
                        ace.getAceIndex()));
            } else if (!DccNasAclAceTypeMapper.isAllow(ace.getAceType())
                    || DccNasAclAccessMaskMapper.toDccPermissions(ace.getAccessMask()) == null) {
                blockers.add(new BlockerResult("DCC_NAS_ACL_SPECIAL_MASK_UNSUPPORTED",
                        "NAS ACL accessMask cannot be restored to DCC query/preview/download: "
                                + ace.getAccessMask(),
                        ace.getTrusteeSid(),
                        ace.getAceIndex()));
            } else if (StrUtil.isNotBlank(ace.getTrusteeSidHash())
                    && !context.mappedSidHashes().contains(ace.getTrusteeSidHash())) {
                blockers.add(new BlockerResult("DCC_NAS_PRINCIPAL_UNMAPPED",
                        "NAS principal is not mapped to a DCC subject: " + ace.getTrusteeSid(),
                        ace.getTrusteeSid(),
                        ace.getAceIndex()));
            }
        }
        return blockers;
    }

    private List<DccNasAclAceDO> acesFor(DccNasAclDirectorySnapshotDO directorySnapshot,
                                         SnapshotQueryContext context) {
        if (directorySnapshot.getDescriptorId() == null) {
            return List.of();
        }
        return context.acesByDescriptorId().getOrDefault(directorySnapshot.getDescriptorId(), List.of());
    }

    private Map<Long, List<DccNasAclAceDO>> selectAcesByDescriptorId(
            List<DccNasAclDirectorySnapshotDO> directorySnapshots) {
        Set<Long> descriptorIds = new LinkedHashSet<>();
        for (DccNasAclDirectorySnapshotDO directorySnapshot : directorySnapshots) {
            if (COLLECT_STATUS_SUCCESS.equals(directorySnapshot.getCollectStatus())
                    && directorySnapshot.getDescriptorId() != null) {
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

    private Set<String> selectMappedSidHashes(List<DccNasAclAceDO> aces) {
        Set<String> sidHashes = new LinkedHashSet<>();
        for (DccNasAclAceDO ace : aces) {
            if (StrUtil.isNotBlank(ace.getTrusteeSidHash())) {
                sidHashes.add(ace.getTrusteeSidHash());
            }
        }
        if (sidHashes.isEmpty()) {
            return Set.of();
        }
        List<DccNasAclIdentityMappingDO> mappings = identityMappingMapper.selectList(
                new LambdaQueryWrapperX<DccNasAclIdentityMappingDO>()
                        .in(DccNasAclIdentityMappingDO::getSidHash, sidHashes)
                        .eq(DccNasAclIdentityMappingDO::getMappingStatus, MAPPING_STATUS_MAPPED));
        requireNonNull(mappings, "identityMappings");
        Set<String> mappedSidHashes = new LinkedHashSet<>();
        for (DccNasAclIdentityMappingDO mapping : mappings) {
            if (StrUtil.isNotBlank(mapping.getSidHash())) {
                mappedSidHashes.add(mapping.getSidHash());
            }
        }
        return mappedSidHashes;
    }

    private Set<String> unmappedSidHashes(SnapshotQueryContext context) {
        Set<String> unmappedSidHashes = new LinkedHashSet<>();
        for (DccNasAclAceDO ace : context.aces()) {
            if (StrUtil.isNotBlank(ace.getTrusteeSidHash())
                    && !context.mappedSidHashes().contains(ace.getTrusteeSidHash())) {
                unmappedSidHashes.add(ace.getTrusteeSidHash());
            }
        }
        return unmappedSidHashes;
    }

    private List<DccNasAclAceDO> sortedAces(List<DccNasAclAceDO> aces) {
        return aces.stream()
                .sorted(Comparator.comparing(DccNasAclAceDO::getAceIndex,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccNasAclAceDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private boolean isUnsupportedAce(DccNasAclAceDO ace) {
        return DccNasAclAceTypeMapper.isDeny(ace.getAceType())
                || !DccNasAclAceTypeMapper.isAllow(ace.getAceType())
                || DccNasAclAccessMaskMapper.toDccPermissions(ace.getAccessMask()) == null;
    }

    private void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(fieldName + " required");
        }
    }

    private record SnapshotQueryContext(DccNasAclSnapshotDO snapshot,
                                        List<DccNasAclDirectorySnapshotDO> directorySnapshots,
                                        Map<Long, List<DccNasAclAceDO>> acesByDescriptorId,
                                        List<DccNasAclAceDO> aces,
                                        Set<String> mappedSidHashes) {
    }
}
