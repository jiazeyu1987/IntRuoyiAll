package cn.iocoder.yudao.module.dcc.service.permission;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclAceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDescriptorDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclAceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDescriptorMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDirectorySnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclSnapshotMapper;
import cn.iocoder.yudao.module.infra.service.file.NasAclAce;
import cn.iocoder.yudao.module.infra.service.file.NasAclReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Validated
public class DccNasPermissionSnapshotCaptureServiceImpl implements DccNasPermissionSnapshotCaptureService {

    private static final String SNAPSHOT_STATUS_RUNNING = "RUNNING";
    private static final String SNAPSHOT_STATUS_CAPTURED = "CAPTURED";
    private static final String SNAPSHOT_STATUS_FAILED = "FAILED";
    private static final String COLLECT_STATUS_SUCCESS = "SUCCESS";
    private static final String COLLECT_STATUS_FAILED = "FAILED";
    private static final String ITEM_TYPE_DIRECTORY = "DIRECTORY";
    private static final String SNAPSHOT_FAILURE_INCOMPLETE = "SNAPSHOT_INCOMPLETE";
    private static final String NORMALIZATION_VERSION = "NAS_ACL_V1";
    private static final String PATH_KEY_SCOPE = "DCC_NAS_PATH_KEY_V1";
    private static final String CAPTURE_CAPABILITY = "SMBJ_SECURITY_DESCRIPTOR_DACL";

    @Resource
    private DccNasAclSnapshotMapper snapshotMapper;
    @Resource
    private DccNasAclDescriptorMapper descriptorMapper;
    @Resource
    private DccNasAclAceMapper aceMapper;
    @Resource
    private DccNasAclDirectorySnapshotMapper directorySnapshotMapper;
    @Resource
    private DccControlledFileNasTransferTaskMapper taskMapper;
    @Resource
    private DccControlledFileNasTransferTaskItemMapper taskItemMapper;
    @Resource
    private NasSettingsService nasSettingsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void captureDirectorySnapshot(Long transferTaskId,
                                         Long transferTaskItemId,
                                         String nasPath,
                                         Long dccDirectoryId,
                                         NasAclReadResult acl) {
        requireNonNull(transferTaskId, "transferTaskId");
        requireNonNull(transferTaskItemId, "transferTaskItemId");
        requireNotBlank(nasPath, "nasPath");
        requireNonNull(dccDirectoryId, "dccDirectoryId");
        requireNonNull(acl, "acl");
        validateAcl(acl);
        NasPathKey requestedPath = canonicalNasPath(nasPath, "nasPath");
        NasPathKey aclPath = canonicalNasPath(acl.path(), "acl.path");
        if (!requestedPath.canonicalKey().equals(aclPath.canonicalKey())) {
            throw new IllegalArgumentException("nasPath must match acl.path after normalization");
        }

        DccControlledFileNasTransferTaskDO task = taskMapper.selectById(transferTaskId);
        if (task == null) {
            throw new IllegalStateException("nas acl snapshot task missing: " + transferTaskId);
        }
        DccControlledFileNasTransferTaskItemDO taskItem = taskItemMapper.selectById(transferTaskItemId);
        if (taskItem == null) {
            throw new IllegalStateException("nas acl snapshot task item missing: " + transferTaskItemId);
        }
        if (!transferTaskId.equals(taskItem.getTaskId())) {
            throw new IllegalStateException("nas acl snapshot task item does not belong to task: " + transferTaskItemId);
        }
        NasPathKey taskItemPath = canonicalNasPath(taskItem.getNasPath(), "taskItem.nasPath");
        if (!taskItemPath.canonicalKey().equals(requestedPath.canonicalKey())) {
            throw new IllegalArgumentException("taskItem.nasPath must match nasPath after normalization");
        }

        NasConnectionConfig config = nasSettingsService.getRequiredNasConfig();
        requireNonNull(config, "nas config");
        requireNotBlank(config.server(), "nas config server");
        requireNotBlank(config.share(), "nas config share");

        DccNasAclSnapshotDO snapshot = getOrCreateSnapshot(task, config);
        DccNasAclDescriptorDO descriptor = getOrCreateDescriptor(acl);
        DirectorySnapshotChange change = upsertDirectorySnapshot(snapshot, descriptor, taskItem, requestedPath,
                dccDirectoryId);
        updateSnapshotHeaderAfterDirectorySuccess(snapshot, change);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSnapshotForTask(Long transferTaskId) {
        requireNonNull(transferTaskId, "transferTaskId");
        DccControlledFileNasTransferTaskDO task = taskMapper.selectById(transferTaskId);
        if (task == null) {
            throw new IllegalStateException("nas acl snapshot task missing: " + transferTaskId);
        }
        DccNasAclSnapshotDO snapshot = snapshotMapper.selectBySnapshotKey(snapshotKeyForTask(transferTaskId));
        if (snapshot == null) {
            throw new IllegalStateException("nas acl snapshot missing for completed task: " + transferTaskId);
        }

        List<DccControlledFileNasTransferTaskItemDO> taskItems = taskItemMapper.selectListByTaskId(transferTaskId);
        requireNonNull(taskItems, "taskItems");
        long expectedDirectoryCount = taskItems.stream()
                .filter(item -> ITEM_TYPE_DIRECTORY.equals(item.getItemType()))
                .count();
        if (expectedDirectoryCount <= 0) {
            throw new IllegalStateException("nas acl snapshot task directory items missing: " + transferTaskId);
        }

        List<DccNasAclDirectorySnapshotDO> directorySnapshots =
                directorySnapshotMapper.selectListBySnapshotId(snapshot.getId());
        requireNonNull(directorySnapshots, "directorySnapshots");
        long successDirectoryCount = directorySnapshots.stream()
                .filter(directorySnapshot -> COLLECT_STATUS_SUCCESS.equals(directorySnapshot.getCollectStatus()))
                .count();
        long nonSuccessDirectorySnapshotCount = directorySnapshots.size() - successDirectoryCount;
        long failedDirectoryCount = Math.max(expectedDirectoryCount - successDirectoryCount,
                nonSuccessDirectorySnapshotCount);

        snapshot.setTotalDirectoryCount(expectedDirectoryCount);
        snapshot.setSnapshottedDirectoryCount(successDirectoryCount);
        snapshot.setFailedDirectoryCount(failedDirectoryCount);
        snapshot.setCompletedAt(LocalDateTime.now());
        if (directorySnapshots.size() == expectedDirectoryCount
                && successDirectoryCount == expectedDirectoryCount
                && failedDirectoryCount == 0L) {
            snapshot.setStatus(SNAPSHOT_STATUS_CAPTURED);
            snapshot.setFailureCode(null);
            snapshot.setFailureMessage(null);
        } else {
            snapshot.setStatus(SNAPSHOT_STATUS_FAILED);
            snapshot.setFailureCode(SNAPSHOT_FAILURE_INCOMPLETE);
            snapshot.setFailureMessage("NAS ACL snapshot incomplete: expectedDirectoryCount="
                    + expectedDirectoryCount
                    + ", directorySnapshotCount=" + directorySnapshots.size()
                    + ", successDirectoryCount=" + successDirectoryCount
                    + ", failedDirectoryCount=" + failedDirectoryCount);
        }
        snapshotMapper.updateById(snapshot);
    }

    private DccNasAclSnapshotDO getOrCreateSnapshot(DccControlledFileNasTransferTaskDO task,
                                                   NasConnectionConfig config) {
        String snapshotKey = snapshotKeyForTask(task.getId());
        DccNasAclSnapshotDO existing = snapshotMapper.selectBySnapshotKey(snapshotKey);
        if (existing != null) {
            return existing;
        }
        String rootPathsJson = task.getSelectedNasPathsJson();
        requireNotBlank(rootPathsJson, "task selectedNasPathsJson");
        DccNasAclSnapshotDO snapshot = DccNasAclSnapshotDO.builder()
                .transferTaskId(task.getId())
                .snapshotKey(snapshotKey)
                .server(config.server())
                .share(config.share())
                .rootPathsJson(rootPathsJson)
                .status(SNAPSHOT_STATUS_RUNNING)
                .normalizationVersion(NORMALIZATION_VERSION)
                .totalDirectoryCount(0L)
                .snapshottedDirectoryCount(0L)
                .failedDirectoryCount(0L)
                .startedAt(LocalDateTime.now())
                .build();
        snapshotMapper.insert(snapshot);
        requireNonNull(snapshot.getId(), "snapshot id");
        return snapshot;
    }

    private DccNasAclDescriptorDO getOrCreateDescriptor(NasAclReadResult acl) {
        String normalizedDescriptorJson = normalizedDescriptorJson(acl);
        String descriptorHash = sha256(normalizedDescriptorJson);
        DccNasAclDescriptorDO existing = descriptorMapper.selectByDescriptorHash(descriptorHash);
        if (existing != null) {
            return existing;
        }
        DccNasAclDescriptorDO descriptor = DccNasAclDescriptorDO.builder()
                .descriptorHash(descriptorHash)
                .ownerSid(acl.ownerSid())
                .groupSid(acl.groupSid())
                .controlFlags(joinSorted(acl.controlFlags()))
                .daclPresent(acl.daclPresent())
                .daclProtected(acl.daclProtected())
                .saclPresent(Boolean.FALSE)
                .rawDescriptorSha256(descriptorHash)
                .rawDescriptorBlob(null)
                .normalizedDescriptorJson(normalizedDescriptorJson)
                .captureCapability(CAPTURE_CAPABILITY)
                .build();
        descriptorMapper.insert(descriptor);
        requireNonNull(descriptor.getId(), "descriptor id");
        insertAces(descriptor.getId(), acl.aces());
        return descriptor;
    }

    private void insertAces(Long descriptorId, List<NasAclAce> aces) {
        for (NasAclAce ace : sortedAces(aces)) {
            String rawAceJson = rawAceJson(ace);
            DccNasAclAceDO aceDO = DccNasAclAceDO.builder()
                    .descriptorId(descriptorId)
                    .aceIndex(ace.index())
                    .aceHash(sha256(rawAceJson))
                    .aceType(ace.aceType())
                    .aceFlags(null)
                    .accessMask(ace.accessMask())
                    .trusteeSid(ace.trusteeSid())
                    .trusteeSidHash(sha256(ace.trusteeSid()))
                    .inherited(ace.inherited())
                    .inheritanceFlags(joinSorted(ace.aceFlags()))
                    .propagationFlags(null)
                    .objectTypeGuid(null)
                    .inheritedObjectTypeGuid(null)
                    .rawAceJson(rawAceJson)
                    .build();
            aceMapper.insert(aceDO);
        }
    }

    private DirectorySnapshotChange upsertDirectorySnapshot(DccNasAclSnapshotDO snapshot,
                                                            DccNasAclDescriptorDO descriptor,
                                                            DccControlledFileNasTransferTaskItemDO taskItem,
                                                            NasPathKey nasPath,
                                                            Long dccDirectoryId) {
        String pathHash = pathHash(nasPath);
        String itemName = StrUtil.blankToDefault(StrUtil.trimToEmpty(taskItem.getItemName()),
                lastPathSegment(nasPath.canonicalPath()));
        requireNotBlank(itemName, "directory snapshot itemName");

        DccNasAclDirectorySnapshotDO directorySnapshot = directorySnapshotMapper
                .selectBySnapshotIdAndPathHash(snapshot.getId(), pathHash);
        if (directorySnapshot == null) {
            directorySnapshot = DccNasAclDirectorySnapshotDO.builder()
                    .snapshotId(snapshot.getId())
                    .transferTaskId(taskItem.getTaskId())
                    .transferTaskItemId(taskItem.getId())
                    .dccDirectoryId(dccDirectoryId)
                    .depth(depthOf(nasPath.canonicalPath()))
                    .nasPath(nasPath.canonicalPath())
                    .pathHash(pathHash)
                    .itemName(itemName)
                    .descriptorId(descriptor.getId())
                    .collectStatus(COLLECT_STATUS_SUCCESS)
                    .build();
            directorySnapshotMapper.insert(directorySnapshot);
            return new DirectorySnapshotChange(true, null);
        }
        String previousCollectStatus = directorySnapshot.getCollectStatus();
        directorySnapshot.setTransferTaskId(taskItem.getTaskId());
        directorySnapshot.setTransferTaskItemId(taskItem.getId());
        directorySnapshot.setDccDirectoryId(dccDirectoryId);
        directorySnapshot.setDepth(depthOf(nasPath.canonicalPath()));
        directorySnapshot.setNasPath(nasPath.canonicalPath());
        directorySnapshot.setPathHash(pathHash);
        directorySnapshot.setItemName(itemName);
        directorySnapshot.setDescriptorId(descriptor.getId());
        directorySnapshot.setCollectStatus(COLLECT_STATUS_SUCCESS);
        directorySnapshot.setFailureCode(null);
        directorySnapshot.setFailureMessage(null);
        directorySnapshotMapper.updateById(directorySnapshot);
        return new DirectorySnapshotChange(false, previousCollectStatus);
    }

    private void updateSnapshotHeaderAfterDirectorySuccess(DccNasAclSnapshotDO snapshot,
                                                          DirectorySnapshotChange change) {
        long totalDirectoryCount = countOf(snapshot.getTotalDirectoryCount());
        long snapshottedDirectoryCount = countOf(snapshot.getSnapshottedDirectoryCount());
        long failedDirectoryCount = countOf(snapshot.getFailedDirectoryCount());
        if (change.created()) {
            totalDirectoryCount++;
            snapshottedDirectoryCount++;
        } else if (!COLLECT_STATUS_SUCCESS.equals(change.previousCollectStatus())) {
            snapshottedDirectoryCount++;
            if (COLLECT_STATUS_FAILED.equals(change.previousCollectStatus()) && failedDirectoryCount > 0) {
                failedDirectoryCount--;
            }
        }
        snapshot.setTotalDirectoryCount(totalDirectoryCount);
        snapshot.setSnapshottedDirectoryCount(snapshottedDirectoryCount);
        snapshot.setFailedDirectoryCount(failedDirectoryCount);
        snapshotMapper.updateById(snapshot);
    }

    private String snapshotKeyForTask(Long transferTaskId) {
        return sha256("DCC_NAS_ACL_SNAPSHOT:" + transferTaskId);
    }

    private void validateAcl(NasAclReadResult acl) {
        requireNotBlank(acl.path(), "acl.path");
        requireNotBlank(acl.ownerSid(), "acl.ownerSid");
        requireNotBlank(acl.groupSid(), "acl.groupSid");
        requireNonNull(acl.controlFlags(), "acl.controlFlags");
        requireNonNull(acl.aces(), "acl.aces");
        for (NasAclAce ace : acl.aces()) {
            requireNonNull(ace, "acl.ace");
            requireNotBlank(ace.aceType(), "acl.aceType");
            requireNonNull(ace.aceFlags(), "acl.aceFlags");
            requireNotBlank(ace.trusteeSid(), "acl.trusteeSid");
        }
    }

    private String normalizedDescriptorJson(NasAclReadResult acl) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("normalizationVersion", NORMALIZATION_VERSION);
        descriptor.put("ownerSid", acl.ownerSid());
        descriptor.put("groupSid", acl.groupSid());
        descriptor.put("controlFlags", sortedStrings(acl.controlFlags()));
        descriptor.put("daclPresent", acl.daclPresent());
        descriptor.put("daclProtected", acl.daclProtected());
        descriptor.put("captureCapability", CAPTURE_CAPABILITY);
        List<Map<String, Object>> normalizedAces = new ArrayList<>();
        for (NasAclAce ace : sortedAces(acl.aces())) {
            normalizedAces.add(aceMap(ace));
        }
        descriptor.put("aces", normalizedAces);
        return JsonUtils.toJsonString(descriptor);
    }

    private String rawAceJson(NasAclAce ace) {
        return JsonUtils.toJsonString(aceMap(ace));
    }

    private Map<String, Object> aceMap(NasAclAce ace) {
        Map<String, Object> aceMap = new LinkedHashMap<>();
        aceMap.put("index", ace.index());
        aceMap.put("aceType", ace.aceType());
        aceMap.put("aceFlags", sortedStrings(ace.aceFlags()));
        aceMap.put("accessMask", ace.accessMask());
        aceMap.put("trusteeSid", ace.trusteeSid());
        aceMap.put("inherited", ace.inherited());
        return aceMap;
    }

    private List<NasAclAce> sortedAces(List<NasAclAce> aces) {
        return aces.stream()
                .sorted(Comparator.comparingInt(NasAclAce::index)
                        .thenComparing(NasAclAce::aceType)
                        .thenComparing(NasAclAce::trusteeSid))
                .toList();
    }

    private String joinSorted(List<String> values) {
        return String.join(",", sortedStrings(values));
    }

    private List<String> sortedStrings(List<String> values) {
        return values.stream()
                .map(StrUtil::trimToEmpty)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .sorted()
                .toList();
    }

    private NasPathKey canonicalNasPath(String rawPath, String fieldName) {
        String normalized = StrUtil.trimToEmpty(rawPath).replace("\\", "/");
        List<String> parts = new ArrayList<>();
        for (String token : normalized.split("/")) {
            String clean = StrUtil.trimToEmpty(token);
            if (StrUtil.isBlank(clean) || ".".equals(clean)) {
                continue;
            }
            if ("..".equals(clean)) {
                throw new IllegalArgumentException(fieldName + " contains traversal");
            }
            parts.add(clean);
        }
        String canonicalPath = String.join("/", parts);
        requireNotBlank(canonicalPath, fieldName);
        return new NasPathKey(canonicalPath, canonicalPath.toLowerCase(Locale.ROOT));
    }

    private String pathHash(NasPathKey nasPath) {
        return sha256(PATH_KEY_SCOPE + ":" + NORMALIZATION_VERSION + ":" + nasPath.canonicalKey());
    }

    private long countOf(Long value) {
        return value == null ? 0L : value;
    }

    private int depthOf(String normalizedPath) {
        if (StrUtil.isBlank(normalizedPath)) {
            return 0;
        }
        return normalizedPath.split("/").length - 1;
    }

    private String lastPathSegment(String path) {
        int index = StrUtil.nullToEmpty(path).lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
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

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private record NasPathKey(String canonicalPath, String canonicalKey) {
    }

    private record DirectorySnapshotChange(boolean created, String previousCollectStatus) {
    }
}
