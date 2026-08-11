package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_ISOLATION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT;

@Service
public class DccControlledFileSourceOwnershipService {

    private static final String OWNED_SOURCE_DIRECTORY = "dcc/source-owned";

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;

    public DccControlledFilePreparedSource prepareSubmissionSource(Long sourceFileId, boolean rawFileReference) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        boolean alreadyReferenced = controlledFileMapper.countAllBySourceFileId(tenantId, sourceFileId) > 0;
        boolean alreadyClaimed = ownershipMapper.selectBySourceFileId(tenantId, sourceFileId) != null;
        return rawFileReference || alreadyReferenced || alreadyClaimed
                ? createVerifiedCopy(sourceFileId)
                : inspectSource(sourceFileId);
    }

    public DccControlledFilePreparedSource createVerifiedCopy(Long sourceFileId) {
        SourceContent source = readSource(sourceFileId);
        Long copiedFileId;
        try {
            copiedFileId = fileService.createFileAndReturnId(source.content(), source.file().getName(),
                    OWNED_SOURCE_DIRECTORY, source.file().getType());
        } catch (Exception ex) {
            throw isolationFailure("无法创建独立副本 sourceFileId=" + sourceFileId, ex);
        }
        SourceContent copied;
        try {
            copied = readSource(copiedFileId);
        } catch (RuntimeException ex) {
            deleteFailedCopy(copiedFileId, ex);
            throw ex;
        }
        if (!source.sha256().equals(copied.sha256())) {
            ServiceException mismatch = exception(CONTROLLED_FILE_SOURCE_ISOLATION_FAILED,
                    "副本 SHA-256 校验不一致 sourceFileId=" + sourceFileId);
            deleteFailedCopy(copiedFileId, mismatch);
            throw mismatch;
        }
        return new DccControlledFilePreparedSource(copiedFileId, sourceFileId, source.sha256(), true);
    }

    public DccControlledFilePreparedSource inspectSource(Long sourceFileId) {
        SourceContent source = readSource(sourceFileId);
        return new DccControlledFilePreparedSource(sourceFileId, sourceFileId, source.sha256(), false);
    }

    public void claimSubmissionSource(Long controlledFileId, DccControlledFilePreparedSource preparedSource,
                                      Long actorId, String ownershipType) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileSourceOwnershipDO existingByFile =
                ownershipMapper.selectByControlledFileId(tenantId, controlledFileId);
        if (matches(existingByFile, controlledFileId, preparedSource.sourceFileId())) {
            return;
        }
        DccControlledFileSourceOwnershipDO ownership = DccControlledFileSourceOwnershipDO.builder()
                .tenantId(tenantId)
                .controlledFileId(controlledFileId)
                .sourceFileId(preparedSource.sourceFileId())
                .originSourceFileId(preparedSource.originSourceFileId())
                .sourceSha256(preparedSource.sourceSha256())
                .ownershipType(ownershipType)
                .claimedBy(actorId)
                .claimedTime(LocalDateTime.now())
                .build();
        try {
            ownershipMapper.insert(ownership);
        } catch (DuplicateKeyException ex) {
            DccControlledFileSourceOwnershipDO owner =
                    ownershipMapper.selectBySourceFileId(tenantId, preparedSource.sourceFileId());
            if (matches(owner, controlledFileId, preparedSource.sourceFileId())) {
                return;
            }
            throw exception(CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT, preparedSource.sourceFileId());
        }
    }

    private SourceContent readSource(Long sourceFileId) {
        if (sourceFileId == null) {
            throw exception(CONTROLLED_FILE_SOURCE_ISOLATION_FAILED, "sourceFileId 为空");
        }
        FileDO file = fileMapper.selectById(sourceFileId);
        if (file == null || file.getConfigId() == null || file.getPath() == null) {
            throw exception(CONTROLLED_FILE_SOURCE_ISOLATION_FAILED,
                    "源文件记录不存在或不完整 sourceFileId=" + sourceFileId);
        }
        try {
            byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
            if (content == null) {
                throw exception(CONTROLLED_FILE_SOURCE_ISOLATION_FAILED,
                        "源文件内容不存在 sourceFileId=" + sourceFileId);
            }
            return new SourceContent(file, content, sha256Hex(content));
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw isolationFailure("源文件不可读 sourceFileId=" + sourceFileId, ex);
        }
    }

    private void deleteFailedCopy(Long copiedFileId, RuntimeException cause) {
        try {
            fileService.deleteFile(copiedFileId);
        } catch (Exception cleanupFailure) {
            IllegalStateException failure = new IllegalStateException(
                    "Failed to remove invalid DCC source copy fileId=" + copiedFileId, cause);
            failure.addSuppressed(cleanupFailure);
            throw failure;
        }
    }

    private RuntimeException isolationFailure(String reason, Exception cause) {
        ServiceException failure = exception(CONTROLLED_FILE_SOURCE_ISOLATION_FAILED, reason);
        failure.initCause(cause);
        return failure;
    }

    private boolean matches(DccControlledFileSourceOwnershipDO ownership, Long controlledFileId, Long sourceFileId) {
        return ownership != null
                && Objects.equals(controlledFileId, ownership.getControlledFileId())
                && Objects.equals(sourceFileId, ownership.getSourceFileId());
    }

    private String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private record SourceContent(FileDO file, byte[] content, String sha256) {
    }
}
