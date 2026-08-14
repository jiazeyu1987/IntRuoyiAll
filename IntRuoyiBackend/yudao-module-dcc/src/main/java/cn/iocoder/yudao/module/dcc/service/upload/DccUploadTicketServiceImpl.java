package cn.iocoder.yudao.module.dcc.service.upload;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_SESSION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;

@Service
public class DccUploadTicketServiceImpl implements DccUploadTicketService {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_BOUND = "BOUND";
    public static final String CLEANUP_ACTIVE = "ACTIVE";
    public static final String CLEANUP_CLEANING = "CLEANING";
    public static final String CLEANUP_BOUND = "BOUND";
    public static final String CLEANUP_CLEANED = "CLEANED";
    public static final String CLEANUP_REASON_EXPIRED_UNBOUND = "EXPIRED_UNBOUND";
    public static final String CLEANUP_REASON_USER_DISCARDED = "USER_DISCARDED";

    private static final long TICKET_TTL_MINUTES = 30L;

    @Resource
    private DccControlledFileTemporaryFileMapper temporaryFileMapper;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;

    @Override
    public DccUploadTicketCreated createTicket(DccUploadTicketCreateCommand command) {
        requireTenantContext();
        validateCreateCommand(command);
        String purpose = normalizePurpose(command.purpose());
        String sessionId = normalizeSession(command.sessionId());
        String contentSha256 = sha256Hex(command.content());
        DccUploadTicketCreated existing = resolveReusableActiveTicket(command.userId(), sessionId, purpose,
                contentSha256);
        if (existing != null) {
            return existing;
        }
        String uploadTicket = newTicket();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(TICKET_TTL_MINUTES);
        try {
            temporaryFileMapper.insert(DccControlledFileTemporaryFileDO.builder()
                    .uploadTicket(uploadTicket)
                    .sessionId(sessionId)
                    .purpose(purpose)
                    .uploaderId(command.userId())
                    .originalFileName(StrUtil.trim(command.originalFileName()))
                    .contentType(StrUtil.trimToNull(command.contentType()))
                    .fileSize(command.fileSize())
                    .fileSha256(contentSha256)
                    .storageFileId(command.storageFileId())
                    .status(STATUS_AVAILABLE)
                    .expireTime(expireTime)
                    .cleanupStatus(CLEANUP_ACTIVE)
                    .requestId(StrUtil.trimToNull(command.requestId()))
                    .build());
        } catch (DuplicateKeyException ex) {
            DccUploadTicketCreated winner = resolveReusableActiveTicket(command.userId(), sessionId, purpose,
                    contentSha256);
            if (winner != null) {
                return winner;
            }
            throw exception(CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT);
        }
        return new DccUploadTicketCreated(uploadTicket, sessionId, purpose, STATUS_AVAILABLE, expireTime,
                command.storageFileId(), StrUtil.trim(command.originalFileName()), StrUtil.trimToNull(command.contentType()),
                command.fileSize());
    }

    @Override
    public DccUploadTicketCreated reuseActiveTicketOrReject(DccUploadTicketPreflightCommand command) {
        requireTenantContext();
        if (command == null || command.userId() == null || command.content() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        String purpose = normalizePurpose(command.purpose());
        String sessionId = normalizeSession(command.sessionId());
        return resolveReusableActiveTicket(command.userId(), sessionId, purpose, sha256Hex(command.content()));
    }

    @Override
    public DccUploadTicketBoundFile resolveForBinding(DccUploadTicketResolveCommand command) {
        requireTenantContext();
        DccControlledFileTemporaryFileDO temporaryFile = requireBindableTemporaryFile(command);
        FileDO storageFile = fileMapper.selectById(temporaryFile.getStorageFileId());
        if (storageFile == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        return new DccUploadTicketBoundFile(temporaryFile.getUploadTicket(), temporaryFile.getStorageFileId(),
                storageFile.getName(), storageFile.getType(), temporaryFile.getFileSize());
    }

    @Override
    public void markBound(DccUploadTicketMarkBoundCommand command) {
        requireTenantContext();
        DccControlledFileTemporaryFileDO temporaryFile = requireBindableTemporaryFile(new DccUploadTicketResolveCommand(
                command.uploadTicket(), command.userId(), command.sessionId(), command.purpose()));
        if (command.controlledFileId() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        int updated = temporaryFileMapper.update(null, new UpdateWrapper<DccControlledFileTemporaryFileDO>()
                .eq("id", temporaryFile.getId())
                .eq("status", STATUS_AVAILABLE)
                .isNull("bound_controlled_file_id")
                .set("status", STATUS_BOUND)
                .set("bound_controlled_file_id", command.controlledFileId())
                .set("bound_time", LocalDateTime.now())
                .set("cleanup_status", CLEANUP_BOUND));
        if (updated != 1) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
    }

    @Override
    public int cleanupExpiredTemporaryFiles(LocalDateTime cleanupTime, int limit) throws Exception {
        requireTenantContext();
        if (cleanupTime == null) {
            throw new IllegalArgumentException("DCC upload temporary cleanup time must not be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("DCC upload temporary cleanup limit must be positive");
        }
        List<DccControlledFileTemporaryFileDO> temporaryFiles = temporaryFileMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileTemporaryFileDO>()
                        .eq(DccControlledFileTemporaryFileDO::getStatus, STATUS_AVAILABLE)
                        .in(DccControlledFileTemporaryFileDO::getCleanupStatus, List.of(CLEANUP_ACTIVE, CLEANUP_CLEANING))
                        .isNull(DccControlledFileTemporaryFileDO::getBoundControlledFileId)
                        .isNotNull(DccControlledFileTemporaryFileDO::getStorageFileId)
                        .le(DccControlledFileTemporaryFileDO::getExpireTime, cleanupTime)
                        .orderByAsc(DccControlledFileTemporaryFileDO::getExpireTime)
                        .last("LIMIT " + limit));
        int cleaned = 0;
        for (DccControlledFileTemporaryFileDO temporaryFile : temporaryFiles) {
            if (!isExpiredUnboundCleanupCandidate(temporaryFile, cleanupTime)) {
                continue;
            }
            if (cleanTemporaryFile(temporaryFile, cleanupTime, CLEANUP_REASON_EXPIRED_UNBOUND, true)) {
                cleaned++;
            }
        }
        return cleaned;
    }

    @Override
    public int cleanupSessionTemporaryFiles(Long userId, String sessionId, LocalDateTime cleanupTime,
                                            String cleanupReason) throws Exception {
        requireTenantContext();
        requirePositiveUser(userId);
        String normalizedSessionId = normalizeSession(sessionId);
        String normalizedReason = normalizeCleanupReason(cleanupReason);
        if (cleanupTime == null) {
            throw new IllegalArgumentException("DCC upload temporary cleanup time must not be null");
        }
        List<DccControlledFileTemporaryFileDO> temporaryFiles = temporaryFileMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileTemporaryFileDO>()
                        .eq(DccControlledFileTemporaryFileDO::getUploaderId, userId)
                        .eq(DccControlledFileTemporaryFileDO::getSessionId, normalizedSessionId)
                        .eq(DccControlledFileTemporaryFileDO::getStatus, STATUS_AVAILABLE)
                        .in(DccControlledFileTemporaryFileDO::getCleanupStatus, List.of(CLEANUP_ACTIVE, CLEANUP_CLEANING))
                        .isNull(DccControlledFileTemporaryFileDO::getBoundControlledFileId)
                        .isNotNull(DccControlledFileTemporaryFileDO::getStorageFileId)
                        .orderByAsc(DccControlledFileTemporaryFileDO::getCreateTime));
        int cleaned = 0;
        for (DccControlledFileTemporaryFileDO temporaryFile : temporaryFiles) {
            if (!isSessionCleanupCandidate(temporaryFile, userId, normalizedSessionId)) {
                continue;
            }
            if (cleanTemporaryFile(temporaryFile, cleanupTime, normalizedReason, false)) {
                cleaned++;
            }
        }
        return cleaned;
    }

    @Override
    public DccUploadTemporaryFileStatus getTemporaryFileStatusByRequestId(Long userId, String requestId) {
        requireTenantContext();
        requirePositiveUser(userId);
        String normalizedRequestId = StrUtil.trim(requestId);
        if (StrUtil.isBlank(normalizedRequestId)) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        List<DccControlledFileTemporaryFileDO> temporaryFiles = temporaryFileMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileTemporaryFileDO>()
                        .eq(DccControlledFileTemporaryFileDO::getUploaderId, userId)
                        .eq(DccControlledFileTemporaryFileDO::getRequestId, normalizedRequestId)
                        .orderByDesc(DccControlledFileTemporaryFileDO::getCreateTime)
                        .last("LIMIT 1"));
        if (temporaryFiles.isEmpty()) {
            return new DccUploadTemporaryFileStatus(normalizedRequestId, 0, false, null, null, null,
                    null, null, null, null);
        }
        DccControlledFileTemporaryFileDO temporaryFile = temporaryFiles.get(0);
        return new DccUploadTemporaryFileStatus(normalizedRequestId, temporaryFiles.size(),
                isBindableNow(temporaryFile), temporaryFile.getSessionId(), temporaryFile.getPurpose(),
                temporaryFile.getStatus(), temporaryFile.getExpireTime(), temporaryFile.getCleanupStatus(),
                temporaryFile.getCleanupReason(), temporaryFile.getCleanupTime());
    }

    private boolean cleanTemporaryFile(DccControlledFileTemporaryFileDO temporaryFile, LocalDateTime cleanupTime,
                                       String cleanupReason, boolean requireExpired) throws Exception {
        if (CLEANUP_ACTIVE.equals(temporaryFile.getCleanupStatus())
                && !claimTemporaryFileCleanup(temporaryFile, cleanupTime, cleanupReason, requireExpired)) {
            return false;
        }
        if (isReferencedByActiveDccArtifact(temporaryFile.getStorageFileId())
                || fileMapper.selectById(temporaryFile.getStorageFileId()) == null) {
            markTemporaryFileCleaned(temporaryFile, cleanupTime, cleanupReason);
            return true;
        }
        fileService.deleteFile(temporaryFile.getStorageFileId());
        markTemporaryFileCleaned(temporaryFile, cleanupTime, cleanupReason);
        return true;
    }

    private boolean claimTemporaryFileCleanup(DccControlledFileTemporaryFileDO temporaryFile, LocalDateTime cleanupTime,
                                              String cleanupReason, boolean requireExpired) {
        UpdateWrapper<DccControlledFileTemporaryFileDO> wrapper = baseCleanupWrapper(temporaryFile)
                .eq("cleanup_status", CLEANUP_ACTIVE)
                .set("cleanup_status", CLEANUP_CLEANING)
                .set("cleanup_reason", cleanupReason)
                .set("cleanup_time", cleanupTime);
        if (requireExpired) {
            wrapper.le("expire_time", cleanupTime);
        }
        return temporaryFileMapper.update(null, wrapper) == 1;
    }

    private void markTemporaryFileCleaned(DccControlledFileTemporaryFileDO temporaryFile, LocalDateTime cleanupTime,
                                          String cleanupReason) {
        int updated = temporaryFileMapper.update(null, baseCleanupWrapper(temporaryFile)
                .eq("cleanup_status", CLEANUP_CLEANING)
                .set("cleanup_status", CLEANUP_CLEANED)
                .set("cleanup_reason", cleanupReason)
                .set("cleanup_time", cleanupTime));
        if (updated != 1) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
    }

    private UpdateWrapper<DccControlledFileTemporaryFileDO> baseCleanupWrapper(
            DccControlledFileTemporaryFileDO temporaryFile) {
        return new UpdateWrapper<DccControlledFileTemporaryFileDO>()
                .eq("id", temporaryFile.getId())
                .eq("status", STATUS_AVAILABLE)
                .eq("storage_file_id", temporaryFile.getStorageFileId())
                .isNull("bound_controlled_file_id");
    }

    private boolean isReferencedByActiveDccArtifact(Long storageFileId) {
        return storageFileId != null
                && temporaryFileMapper.countActiveDccStorageReferencesByStorageFileId(
                TenantContextHolder.getRequiredTenantId(), storageFileId) > 0;
    }

    private DccControlledFileTemporaryFileDO requireBindableTemporaryFile(DccUploadTicketResolveCommand command) {
        if (command == null || StrUtil.isBlank(command.uploadTicket())) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        String sessionId = normalizeSession(command.sessionId());
        String purpose = normalizePurpose(command.purpose());
        DccControlledFileTemporaryFileDO temporaryFile = temporaryFileMapper.selectOne(
                DccControlledFileTemporaryFileDO::getUploadTicket, StrUtil.trim(command.uploadTicket()));
        if (temporaryFile == null
                || !Objects.equals(command.userId(), temporaryFile.getUploaderId())
                || !StrUtil.equals(sessionId, temporaryFile.getSessionId())
                || !StrUtil.equals(purpose, temporaryFile.getPurpose())
                || !STATUS_AVAILABLE.equals(temporaryFile.getStatus())
                || temporaryFile.getBoundControlledFileId() != null
                || temporaryFile.getExpireTime() == null
                || !temporaryFile.getExpireTime().isAfter(LocalDateTime.now())
                || temporaryFile.getStorageFileId() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        return temporaryFile;
    }

    private boolean isExpiredUnboundCleanupCandidate(DccControlledFileTemporaryFileDO temporaryFile,
                                                     LocalDateTime cleanupTime) {
        return temporaryFile != null
                && temporaryFile.getId() != null
                && STATUS_AVAILABLE.equals(temporaryFile.getStatus())
                && isCleanupRetryable(temporaryFile.getCleanupStatus())
                && temporaryFile.getBoundControlledFileId() == null
                && temporaryFile.getStorageFileId() != null
                && temporaryFile.getExpireTime() != null
                && !temporaryFile.getExpireTime().isAfter(cleanupTime);
    }

    private boolean isSessionCleanupCandidate(DccControlledFileTemporaryFileDO temporaryFile, Long userId,
                                              String sessionId) {
        return temporaryFile != null
                && temporaryFile.getId() != null
                && Objects.equals(userId, temporaryFile.getUploaderId())
                && StrUtil.equals(sessionId, temporaryFile.getSessionId())
                && STATUS_AVAILABLE.equals(temporaryFile.getStatus())
                && isCleanupRetryable(temporaryFile.getCleanupStatus())
                && temporaryFile.getBoundControlledFileId() == null
                && temporaryFile.getStorageFileId() != null;
    }

    private boolean isCleanupRetryable(String cleanupStatus) {
        return CLEANUP_ACTIVE.equals(cleanupStatus) || CLEANUP_CLEANING.equals(cleanupStatus);
    }

    private boolean isBindableNow(DccControlledFileTemporaryFileDO temporaryFile) {
        return temporaryFile != null
                && STATUS_AVAILABLE.equals(temporaryFile.getStatus())
                && CLEANUP_ACTIVE.equals(temporaryFile.getCleanupStatus())
                && temporaryFile.getBoundControlledFileId() == null
                && temporaryFile.getStorageFileId() != null
                && temporaryFile.getExpireTime() != null
                && temporaryFile.getExpireTime().isAfter(LocalDateTime.now());
    }

    private DccUploadTicketCreated resolveReusableActiveTicket(Long userId, String sessionId, String purpose,
                                                               String contentSha256) {
        List<DccControlledFileTemporaryFileDO> activeSlots = findActiveUploadSlotRows(userId, sessionId, purpose);
        if (activeSlots == null || activeSlots.isEmpty()) {
            return null;
        }
        if (activeSlots.size() != 1) {
            throw exception(CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT);
        }
        DccControlledFileTemporaryFileDO activeSlot = activeSlots.get(0);
        if (!CLEANUP_ACTIVE.equals(activeSlot.getCleanupStatus())
                || activeSlot.getExpireTime() == null
                || !activeSlot.getExpireTime().isAfter(LocalDateTime.now())) {
            throw exception(CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT);
        }
        if (!StrUtil.equals(contentSha256, activeSlot.getFileSha256())) {
            throw exception(CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT);
        }
        FileDO storageFile = fileMapper.selectById(activeSlot.getStorageFileId());
        if (storageFile == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        return new DccUploadTicketCreated(activeSlot.getUploadTicket(), activeSlot.getSessionId(),
                activeSlot.getPurpose(), activeSlot.getStatus(), activeSlot.getExpireTime(),
                activeSlot.getStorageFileId(), storageFile.getName(), storageFile.getType(), activeSlot.getFileSize());
    }

    private List<DccControlledFileTemporaryFileDO> findActiveUploadSlotRows(Long userId, String sessionId,
                                                                           String purpose) {
        return temporaryFileMapper.selectList(new LambdaQueryWrapperX<DccControlledFileTemporaryFileDO>()
                .eq(DccControlledFileTemporaryFileDO::getUploaderId, userId)
                .eq(DccControlledFileTemporaryFileDO::getSessionId, sessionId)
                .eq(DccControlledFileTemporaryFileDO::getPurpose, purpose)
                .eq(DccControlledFileTemporaryFileDO::getStatus, STATUS_AVAILABLE)
                .in(DccControlledFileTemporaryFileDO::getCleanupStatus, List.of(CLEANUP_ACTIVE, CLEANUP_CLEANING))
                .isNull(DccControlledFileTemporaryFileDO::getBoundControlledFileId)
                .isNotNull(DccControlledFileTemporaryFileDO::getStorageFileId));
    }

    private void requireTenantContext() {
        try {
            TenantContextHolder.getRequiredTenantId();
        } catch (NullPointerException ex) {
            throw new IllegalStateException("DCC upload ticket requires tenant context", ex);
        }
    }

    private void validateCreateCommand(DccUploadTicketCreateCommand command) {
        if (command == null
                || command.userId() == null
                || command.storageFileId() == null
                || command.fileSize() == null
                || command.fileSize() < 0
                || StrUtil.isBlank(command.originalFileName())
                || command.content() == null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        normalizeSession(command.sessionId());
        normalizePurpose(command.purpose());
    }

    private void requirePositiveUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
    }

    private String normalizeCleanupReason(String cleanupReason) {
        String normalized = StrUtil.trimToEmpty(cleanupReason).toUpperCase(Locale.ROOT);
        if (!CLEANUP_REASON_USER_DISCARDED.equals(normalized)
                && !CLEANUP_REASON_EXPIRED_UNBOUND.equals(normalized)) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        return normalized;
    }

    private String normalizeSession(String sessionId) {
        String normalized = StrUtil.trim(sessionId);
        if (StrUtil.isBlank(normalized)) {
            throw exception(CONTROLLED_FILE_UPLOAD_SESSION_INVALID);
        }
        return normalized;
    }

    private String normalizePurpose(String purpose) {
        String normalized = StrUtil.trimToEmpty(purpose).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(normalized)) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        return normalized;
    }

    private String newTicket() {
        return "UT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }
}
