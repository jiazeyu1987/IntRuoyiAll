package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGlobalClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGlobalClaimMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT;
import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceGovernanceWriteGuard.requireExactlyOne;

@Service
public class DccControlledFileSourceGlobalClaimService {

    @Resource
    private DccControlledFileSourceGlobalClaimMapper claimMapper;

    public void claim(Long tenantId, Long sourceFileId, Long controlledFileId, String sourceSha256,
                      Long actorId, Long governanceBatchId, Long governanceItemId) {
        if (sourceFileId == null || controlledFileId == null || sourceSha256 == null) {
            throw exception(CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT, sourceFileId);
        }
        DccControlledFileSourceGlobalClaimDO existing = claimMapper.selectBySourceFileId(sourceFileId);
        if (matches(existing, tenantId, sourceFileId, controlledFileId, sourceSha256)) {
            return;
        }
        if (existing != null) {
            throw exception(CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT, sourceFileId);
        }
        DccControlledFileSourceGlobalClaimDO claim = DccControlledFileSourceGlobalClaimDO.builder()
                .sourceFileId(sourceFileId)
                .tenantId(tenantId)
                .controlledFileId(controlledFileId)
                .governanceBatchId(governanceBatchId)
                .governanceItemId(governanceItemId)
                .claimStatus("ACTIVE")
                .sourceSha256(sourceSha256)
                .claimedBy(actorId)
                .claimedTime(LocalDateTime.now())
                .build();
        try {
            requireExactlyOne(claimMapper.insert(claim), "insert global source claim");
        } catch (DuplicateKeyException ex) {
            DccControlledFileSourceGlobalClaimDO concurrent = claimMapper.selectBySourceFileId(sourceFileId);
            if (matches(concurrent, tenantId, sourceFileId, controlledFileId, sourceSha256)) {
                return;
            }
            throw exception(CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT, sourceFileId);
        }
    }

    private boolean matches(DccControlledFileSourceGlobalClaimDO claim, Long tenantId, Long sourceFileId,
                            Long controlledFileId, String sourceSha256) {
        return claim != null
                && Objects.equals(claim.getTenantId(), tenantId)
                && Objects.equals(claim.getSourceFileId(), sourceFileId)
                && Objects.equals(claim.getControlledFileId(), controlledFileId)
                && Objects.equals(claim.getSourceSha256(), sourceSha256);
    }
}
