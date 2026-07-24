package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ControlledContentLifecycleHealthCheckService {

    private final ControlledContentVersionRefMapper versionRefMapper;
    private final ControlledContentTransitionAuditMapper transitionAuditMapper;

    public ControlledContentLifecycleHealthCheckService(ControlledContentVersionRefMapper versionRefMapper,
                                                       ControlledContentTransitionAuditMapper transitionAuditMapper) {
        if (versionRefMapper == null) {
            throw new IllegalArgumentException("versionRefMapper must not be null");
        }
        if (transitionAuditMapper == null) {
            throw new IllegalArgumentException("transitionAuditMapper must not be null");
        }
        this.versionRefMapper = versionRefMapper;
        this.transitionAuditMapper = transitionAuditMapper;
    }

    public HealthCheckResult checkContentKey(ControlledContentKey key, Long nativeActiveCount,
                                             Long nativeOpenCandidateCount) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        long normalizedNativeActiveCount = nullToZero(nativeActiveCount);
        long normalizedNativeOpenCandidateCount = nullToZero(nativeOpenCandidateCount);
        long platformActiveCount = nullToZero(versionRefMapper.countActiveRefs(key.getTenantId(),
                key.getContentType().name(), key.getContentKey()));
        long platformOpenCandidateCount = nullToZero(versionRefMapper.countOpenCandidateRefs(key.getTenantId(),
                key.getContentType().name(), key.getContentKey()));
        long transitionAuditCount = nullToZero(transitionAuditMapper.countTransitions(key.getTenantId(),
                key.getContentType().name(), key.getContentKey()));

        List<String> issues = new ArrayList<>();
        if (normalizedNativeActiveCount != platformActiveCount) {
            issues.add("native active count " + normalizedNativeActiveCount
                    + " != platform active count " + platformActiveCount);
        }
        if (normalizedNativeOpenCandidateCount != platformOpenCandidateCount) {
            issues.add("native open candidate count " + normalizedNativeOpenCandidateCount
                    + " != platform open candidate count " + platformOpenCandidateCount);
        }
        if (transitionAuditCount == 0) {
            issues.add("platform transition audit is missing");
        }
        return new HealthCheckResult(key, normalizedNativeActiveCount, normalizedNativeOpenCandidateCount,
                platformActiveCount, platformOpenCandidateCount, transitionAuditCount, List.copyOf(issues));
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    public record HealthCheckResult(ControlledContentKey key,
                                    long nativeActiveCount,
                                    long nativeOpenCandidateCount,
                                    long platformActiveCount,
                                    long platformOpenCandidateCount,
                                    long transitionAuditCount,
                                    List<String> issues) {

        public boolean consistent() {
            return issues.isEmpty();
        }

    }

}
