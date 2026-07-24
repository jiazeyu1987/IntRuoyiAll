package cn.iocoder.yudao.module.dcc.service.upload;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileUploadPolicyDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileUploadPolicyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_EXCEEDED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_POLICY_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_POLICY_MISSING;

@Service
@Validated
public class DccUploadSizePolicyServiceImpl implements DccUploadSizePolicyService {

    @Resource
    private DccControlledFileUploadPolicyMapper uploadPolicyMapper;

    @Override
    public List<DccControlledFileUploadPolicyDO> getPolicyList() {
        return uploadPolicyMapper.selectList().stream()
                .sorted(Comparator.comparing(DccControlledFileUploadPolicyDO::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPolicy(DccUploadSizePolicySaveCommand command) {
        DccControlledFileUploadPolicyDO policy = buildPolicy(null, command);
        uploadPolicyMapper.insert(policy);
        return policy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePolicy(Long id, DccUploadSizePolicySaveCommand command) {
        if (id == null || uploadPolicyMapper.selectById(id) == null) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_MISSING);
        }
        uploadPolicyMapper.updateById(buildPolicy(id, command));
    }

    @Override
    public DccUploadSizePolicyMatch resolveEffectivePolicy(Long categoryId, String purpose, LocalDateTime now) {
        LocalDateTime effectiveAt = now == null ? LocalDateTime.now() : now;
        String normalizedPurpose = normalizePurpose(purpose);
        List<PolicyCandidate> candidates = uploadPolicyMapper.selectList().stream()
                .map(policy -> toCandidate(policy, categoryId, normalizedPurpose))
                .filter(Objects::nonNull)
                .toList();
        if (candidates.isEmpty()) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_MISSING);
        }

        int highestScopePriority = candidates.stream()
                .mapToInt(candidate -> candidate.scopeType().getPriority())
                .max()
                .orElseThrow(() -> exception(DCC_UPLOAD_SIZE_POLICY_MISSING));
        List<PolicyCandidate> highestScopeCandidates = candidates.stream()
                .filter(candidate -> candidate.scopeType().getPriority() == highestScopePriority)
                .toList();

        PolicyCandidate selected = highestScopeCandidates.stream()
                .filter(candidate -> isValidPolicy(candidate.policy(), effectiveAt))
                .max(this::compareSameScopePolicy)
                .orElseThrow(() -> exception(DCC_UPLOAD_SIZE_POLICY_MISSING));
        return toMatch(selected);
    }

    @Override
    public DccUploadSizePolicyMatch validateUploadSize(Long categoryId, String purpose, long fileSize, LocalDateTime now) {
        if (fileSize < 0) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "fileSize must be greater than or equal to 0");
        }
        DccUploadSizePolicyMatch match = resolveEffectivePolicy(categoryId, purpose, now);
        if (fileSize > match.maxBytes()) {
            throw exception(DCC_UPLOAD_SIZE_EXCEEDED, fileSize, match.maxBytes());
        }
        return match;
    }

    private DccControlledFileUploadPolicyDO buildPolicy(Long id, DccUploadSizePolicySaveCommand command) {
        DccUploadSizePolicyScopeType scopeType = requireScopeType(command.getScopeType());
        validatePolicyCommand(command, scopeType);
        return DccControlledFileUploadPolicyDO.builder()
                .id(id)
                .policyCode(command.getPolicyCode().trim())
                .scopeType(scopeType.name())
                .categoryId(command.getCategoryId())
                .purpose(normalizePurpose(command.getPurpose()))
                .maxBytes(command.getMaxBytes())
                .enabled(command.getEnabled())
                .priority(scopeType.getPriority())
                .policyVersion(command.getPolicyVersion().trim())
                .effectiveFrom(command.getEffectiveFrom())
                .effectiveTo(command.getEffectiveTo())
                .changeReason(command.getChangeReason().trim())
                .build();
    }

    private void validatePolicyCommand(DccUploadSizePolicySaveCommand command, DccUploadSizePolicyScopeType scopeType) {
        if (StrUtil.isBlank(command.getPolicyCode())) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "policyCode is required");
        }
        if (command.getMaxBytes() == null || command.getMaxBytes() <= 0) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "maxBytes must be greater than 0");
        }
        if (command.getEnabled() == null) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "enabled is required");
        }
        if (StrUtil.isBlank(command.getPolicyVersion())) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "policyVersion is required");
        }
        if (StrUtil.isBlank(command.getChangeReason())) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "changeReason is required");
        }
        if (command.getEffectiveFrom() != null && command.getEffectiveTo() != null
                && !command.getEffectiveTo().isAfter(command.getEffectiveFrom())) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "effectiveTo must be after effectiveFrom");
        }
        validateScopeFields(command, scopeType);
    }

    private void validateScopeFields(DccUploadSizePolicySaveCommand command, DccUploadSizePolicyScopeType scopeType) {
        String purpose = normalizePurpose(command.getPurpose());
        boolean categoryPresent = command.getCategoryId() != null;
        boolean purposePresent = purpose != null;
        switch (scopeType) {
            case GLOBAL -> {
                if (categoryPresent || purposePresent) {
                    throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "GLOBAL scope cannot set categoryId or purpose");
                }
            }
            case CATEGORY -> {
                if (!categoryPresent || purposePresent) {
                    throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "CATEGORY scope requires categoryId only");
                }
            }
            case PURPOSE -> {
                if (categoryPresent || !purposePresent) {
                    throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "PURPOSE scope requires purpose only");
                }
            }
            case CATEGORY_PURPOSE -> {
                if (!categoryPresent || !purposePresent) {
                    throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID,
                            "CATEGORY_PURPOSE scope requires categoryId and purpose");
                }
            }
        }
    }

    private PolicyCandidate toCandidate(DccControlledFileUploadPolicyDO policy, Long categoryId, String purpose) {
        DccUploadSizePolicyScopeType scopeType = DccUploadSizePolicyScopeType.of(policy.getScopeType());
        if (scopeType == null) {
            return null;
        }
        return switch (scopeType) {
            case GLOBAL -> new PolicyCandidate(policy, scopeType);
            case CATEGORY -> categoryId != null && Objects.equals(policy.getCategoryId(), categoryId)
                    ? new PolicyCandidate(policy, scopeType) : null;
            case PURPOSE -> purpose != null && Objects.equals(normalizePurpose(policy.getPurpose()), purpose)
                    ? new PolicyCandidate(policy, scopeType) : null;
            case CATEGORY_PURPOSE -> categoryId != null && purpose != null
                    && Objects.equals(policy.getCategoryId(), categoryId)
                    && Objects.equals(normalizePurpose(policy.getPurpose()), purpose)
                    ? new PolicyCandidate(policy, scopeType) : null;
        };
    }

    private boolean isValidPolicy(DccControlledFileUploadPolicyDO policy, LocalDateTime now) {
        return Boolean.TRUE.equals(policy.getEnabled())
                && policy.getMaxBytes() != null
                && policy.getMaxBytes() > 0
                && (policy.getEffectiveFrom() == null || !policy.getEffectiveFrom().isAfter(now))
                && (policy.getEffectiveTo() == null || policy.getEffectiveTo().isAfter(now));
    }

    private int compareSameScopePolicy(PolicyCandidate left, PolicyCandidate right) {
        DccControlledFileUploadPolicyDO leftPolicy = left.policy();
        DccControlledFileUploadPolicyDO rightPolicy = right.policy();
        int priorityCompare = Integer.compare(nullToZero(leftPolicy.getPriority()), nullToZero(rightPolicy.getPriority()));
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        int effectiveCompare = nullToMin(leftPolicy.getEffectiveFrom()).compareTo(nullToMin(rightPolicy.getEffectiveFrom()));
        if (effectiveCompare != 0) {
            return effectiveCompare;
        }
        return Long.compare(nullToZero(leftPolicy.getId()), nullToZero(rightPolicy.getId()));
    }

    private DccUploadSizePolicyMatch toMatch(PolicyCandidate candidate) {
        DccControlledFileUploadPolicyDO policy = candidate.policy();
        return new DccUploadSizePolicyMatch(policy.getId(), policy.getPolicyCode(), candidate.scopeType(),
                policy.getCategoryId(), policy.getPurpose(), policy.getMaxBytes(), policy.getPolicyVersion(),
                policy.getPriority(), candidate.scopeType().getPriority());
    }

    private DccUploadSizePolicyScopeType requireScopeType(String scopeType) {
        DccUploadSizePolicyScopeType parsed = DccUploadSizePolicyScopeType.of(scopeType);
        if (parsed == null) {
            throw exception(DCC_UPLOAD_SIZE_POLICY_INVALID, "scopeType is invalid");
        }
        return parsed;
    }

    private String normalizePurpose(String purpose) {
        String trimmed = StrUtil.trim(purpose);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private LocalDateTime nullToMin(LocalDateTime value) {
        return value == null ? LocalDateTime.MIN : value;
    }

    private record PolicyCandidate(DccControlledFileUploadPolicyDO policy,
                                   DccUploadSizePolicyScopeType scopeType) {
    }

}
