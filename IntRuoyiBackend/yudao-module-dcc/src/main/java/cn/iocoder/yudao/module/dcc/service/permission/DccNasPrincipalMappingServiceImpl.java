package cn.iocoder.yudao.module.dcc.service.permission;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclAceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclAceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDirectorySnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclIdentityMappingMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.PostApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PRINCIPAL_MAPPING_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PRINCIPAL_SID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PRINCIPAL_TARGET_ID_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_NAS_PRINCIPAL_TARGET_TYPE_INVALID;

@Service
@Validated
public class DccNasPrincipalMappingServiceImpl implements DccNasPrincipalMappingService {

    private static final String MAPPING_STATUS_MAPPED = "MAPPED";
    private static final String MAPPING_STATUS_INACTIVE = "INACTIVE";
    private static final String MAPPING_METHOD_MANUAL = "MANUAL";
    private static final String COLLECT_STATUS_SUCCESS = "SUCCESS";
    private static final Set<String> TARGET_SUBJECT_TYPES = Set.of("USER", "DEPT", "ROLE", "POSITION");

    @Resource
    private DccNasAclIdentityMappingMapper identityMappingMapper;
    @Resource
    private DccNasAclAceMapper aceMapper;
    @Resource
    private DccNasAclDirectorySnapshotMapper directorySnapshotMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private RoleApi roleApi;
    @Resource
    private PostApi postApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccNasAclIdentityMappingDO saveMapping(SaveMappingCommand command) {
        if (command == null) {
            throw exception(DCC_NAS_PRINCIPAL_SID_REQUIRED);
        }
        String sid = normalizeSid(command.sourceSid());
        String accountType = requireAccountType(command.accountType());
        String targetSubjectType = normalizeTargetSubjectType(command.targetSubjectType());
        Long targetSubjectId = command.targetSubjectId();
        if (targetSubjectId == null) {
            throw exception(DCC_NAS_PRINCIPAL_TARGET_ID_REQUIRED);
        }

        String sidHash = sha256(sid);
        DccNasAclIdentityMappingDO existing = identityMappingMapper.selectOne(
                new LambdaQueryWrapperX<DccNasAclIdentityMappingDO>()
                        .eq(DccNasAclIdentityMappingDO::getSidHash, sidHash));

        validateTargetSubject(targetSubjectType, targetSubjectId);

        if (existing != null) {
            if (MAPPING_STATUS_MAPPED.equals(existing.getMappingStatus())
                    && !sameSubject(existing, targetSubjectType, targetSubjectId)) {
                throw exception(DCC_NAS_PRINCIPAL_MAPPING_CONFLICT, sid);
            }
            refreshExistingMapping(existing, command, sid, accountType, targetSubjectType, targetSubjectId);
            identityMappingMapper.updateById(existing);
            return existing;
        }

        DccNasAclIdentityMappingDO mapping = DccNasAclIdentityMappingDO.builder()
                .sid(sid)
                .sidHash(sidHash)
                .domainName(StrUtil.trimToNull(command.sourceAuthority()))
                .accountName(StrUtil.trimToNull(command.accountName()))
                .accountDisplayName(StrUtil.trimToNull(command.sourceName()))
                .accountType(accountType)
                .mappingStatus(Boolean.TRUE.equals(command.active()) ? MAPPING_STATUS_MAPPED : MAPPING_STATUS_INACTIVE)
                .dccSubjectType(targetSubjectType)
                .dccSubjectId(targetSubjectId)
                .mappingMethod(MAPPING_METHOD_MANUAL)
                .verifiedAt(LocalDateTime.now())
                .mappedByUserId(command.operatorUserId())
                .build();
        identityMappingMapper.insert(mapping);
        return mapping;
    }

    private void refreshExistingMapping(DccNasAclIdentityMappingDO existing,
                                        SaveMappingCommand command,
                                        String sid,
                                        String accountType,
                                        String targetSubjectType,
                                        Long targetSubjectId) {
        existing.setSid(sid);
        existing.setSidHash(sha256(sid));
        existing.setDomainName(StrUtil.trimToNull(command.sourceAuthority()));
        existing.setAccountName(StrUtil.trimToNull(command.accountName()));
        existing.setAccountDisplayName(StrUtil.trimToNull(command.sourceName()));
        existing.setAccountType(accountType);
        existing.setMappingStatus(Boolean.TRUE.equals(command.active()) ? MAPPING_STATUS_MAPPED : MAPPING_STATUS_INACTIVE);
        existing.setDccSubjectType(targetSubjectType);
        existing.setDccSubjectId(targetSubjectId);
        existing.setMappingMethod(MAPPING_METHOD_MANUAL);
        existing.setVerifiedAt(LocalDateTime.now());
        existing.setMappedByUserId(command.operatorUserId());
        existing.setBlockReason(null);
    }

    @Override
    public List<UnmappedPrincipal> listUnmappedPrincipals(Long taskId) {
        requireNonNull(taskId, "taskId");
        List<DccNasAclDirectorySnapshotDO> directorySnapshots = directorySnapshotMapper.selectList(
                new LambdaQueryWrapperX<DccNasAclDirectorySnapshotDO>()
                        .eq(DccNasAclDirectorySnapshotDO::getTransferTaskId, taskId)
                        .eq(DccNasAclDirectorySnapshotDO::getCollectStatus, COLLECT_STATUS_SUCCESS));
        if (directorySnapshots.isEmpty()) {
            return List.of();
        }

        List<DccNasAclDirectorySnapshotDO> successfulDirectorySnapshots = new ArrayList<>();
        Set<Long> descriptorIds = new LinkedHashSet<>();
        for (DccNasAclDirectorySnapshotDO directorySnapshot : directorySnapshots) {
            if (directorySnapshot.getDescriptorId() != null
                    && COLLECT_STATUS_SUCCESS.equals(directorySnapshot.getCollectStatus())) {
                successfulDirectorySnapshots.add(directorySnapshot);
                descriptorIds.add(directorySnapshot.getDescriptorId());
            }
        }
        if (descriptorIds.isEmpty()) {
            return List.of();
        }

        List<DccNasAclAceDO> aces = aceMapper.selectList(new LambdaQueryWrapperX<DccNasAclAceDO>()
                .in(DccNasAclAceDO::getDescriptorId, descriptorIds));
        if (aces.isEmpty()) {
            return List.of();
        }

        Map<String, PrincipalAggregate> aggregates = aggregateAces(successfulDirectorySnapshots, aces);
        if (aggregates.isEmpty()) {
            return List.of();
        }

        Set<String> mappedSidHashes = selectMappedSidHashes(aggregates.keySet());
        List<UnmappedPrincipal> result = new ArrayList<>();
        for (PrincipalAggregate aggregate : aggregates.values()) {
            if (!mappedSidHashes.contains(aggregate.sidHash())) {
                result.add(new UnmappedPrincipal(sourceAuthorityOf(aggregate.sid()), aggregate.sid(),
                        aggregate.sid(), aggregate.sidHash(), aggregate.aceCount(), aggregate.firstNasPath()));
            }
        }
        return result;
    }

    private Map<String, PrincipalAggregate> aggregateAces(List<DccNasAclDirectorySnapshotDO> directorySnapshots,
                                                         List<DccNasAclAceDO> aces) {
        Map<String, PrincipalAggregate> aggregates = new LinkedHashMap<>();
        Map<Long, List<DccNasAclAceDO>> acesByDescriptorId = groupAcesByDescriptorId(aces);
        for (DccNasAclDirectorySnapshotDO directorySnapshot : directorySnapshots) {
            List<DccNasAclAceDO> descriptorAces = acesByDescriptorId.get(directorySnapshot.getDescriptorId());
            if (descriptorAces == null) {
                continue;
            }
            for (DccNasAclAceDO ace : descriptorAces) {
                if (StrUtil.isBlank(ace.getTrusteeSidHash())) {
                    continue;
                }
                String trusteeSidHash = ace.getTrusteeSidHash();
                PrincipalAggregate existing = aggregates.get(trusteeSidHash);
                if (existing == null) {
                    aggregates.put(trusteeSidHash, new PrincipalAggregate(ace.getTrusteeSid(), trusteeSidHash, 1,
                            directorySnapshot.getNasPath()));
                } else {
                    aggregates.put(trusteeSidHash, existing.increment());
                }
            }
        }
        return aggregates;
    }

    private Map<Long, List<DccNasAclAceDO>> groupAcesByDescriptorId(List<DccNasAclAceDO> aces) {
        Map<Long, List<DccNasAclAceDO>> acesByDescriptorId = new LinkedHashMap<>();
        for (DccNasAclAceDO ace : aces) {
            if (ace.getDescriptorId() == null) {
                continue;
            }
            acesByDescriptorId.computeIfAbsent(ace.getDescriptorId(), ignored -> new ArrayList<>()).add(ace);
        }
        return acesByDescriptorId;
    }

    private Set<String> selectMappedSidHashes(Set<String> sidHashes) {
        List<DccNasAclIdentityMappingDO> mappings = identityMappingMapper.selectList(
                new LambdaQueryWrapperX<DccNasAclIdentityMappingDO>()
                        .in(DccNasAclIdentityMappingDO::getSidHash, sidHashes)
                        .eq(DccNasAclIdentityMappingDO::getMappingStatus, MAPPING_STATUS_MAPPED));
        Set<String> mappedSidHashes = new HashSet<>();
        for (DccNasAclIdentityMappingDO mapping : mappings) {
            if (StrUtil.isNotBlank(mapping.getSidHash())) {
                mappedSidHashes.add(mapping.getSidHash());
            }
        }
        return mappedSidHashes;
    }

    private void validateTargetSubject(String targetSubjectType, Long targetSubjectId) {
        switch (targetSubjectType) {
            case "USER" -> adminUserApi.validateUserList(List.of(targetSubjectId));
            case "DEPT" -> deptApi.validateDeptList(List.of(targetSubjectId));
            case "ROLE" -> roleApi.validRoleList(List.of(targetSubjectId));
            case "POSITION" -> postApi.validPostList(List.of(targetSubjectId));
            default -> throw exception(DCC_NAS_PRINCIPAL_TARGET_TYPE_INVALID, targetSubjectType);
        }
    }

    private boolean sameSubject(DccNasAclIdentityMappingDO existing, String targetSubjectType, Long targetSubjectId) {
        return targetSubjectType.equals(existing.getDccSubjectType())
                && targetSubjectId.equals(existing.getDccSubjectId());
    }

    private String normalizeSid(String sid) {
        String normalizedSid = StrUtil.trimToEmpty(sid).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(normalizedSid)) {
            throw exception(DCC_NAS_PRINCIPAL_SID_REQUIRED);
        }
        return normalizedSid;
    }

    private String normalizeTargetSubjectType(String targetSubjectType) {
        String normalizedType = StrUtil.trimToEmpty(targetSubjectType).toUpperCase(Locale.ROOT);
        if (!TARGET_SUBJECT_TYPES.contains(normalizedType)) {
            throw exception(DCC_NAS_PRINCIPAL_TARGET_TYPE_INVALID, targetSubjectType);
        }
        return normalizedType;
    }

    private String requireAccountType(String accountType) {
        String normalizedType = StrUtil.trimToNull(accountType);
        if (normalizedType == null) {
            throw new IllegalArgumentException("accountType required");
        }
        return normalizedType;
    }

    private String sourceAuthorityOf(String sid) {
        if (sid == null) {
            return null;
        }
        int separatorIndex = sid.indexOf('\\');
        if (separatorIndex <= 0) {
            return null;
        }
        return sid.substring(0, separatorIndex);
    }

    private void requireNonNull(Object value, String fieldName) {
        if (value == null) {
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

    private record PrincipalAggregate(String sid, String sidHash, int aceCount, String firstNasPath) {

        private PrincipalAggregate increment() {
            return new PrincipalAggregate(sid, sidHash, aceCount + 1, firstNasPath);
        }
    }
}
