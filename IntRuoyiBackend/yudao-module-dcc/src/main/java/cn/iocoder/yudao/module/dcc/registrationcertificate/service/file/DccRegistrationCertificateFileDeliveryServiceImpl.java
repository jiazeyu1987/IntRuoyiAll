package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDownloadConsumptionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateDownloadConsumptionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DOWNLOAD_ALREADY_CONSUMED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DOWNLOAD_CONSUMPTION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DOWNLOAD_PROJECT_CODE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_DELIVERY_AUDIT_CONFLICT;

@Service
public class DccRegistrationCertificateFileDeliveryServiceImpl implements DccRegistrationCertificateFileDeliveryService {

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAILURE = "FAILURE";
    private static final String RESULT_FAILED_BEFORE_START = "FAILED_BEFORE_START";
    private static final String EVENT_TYPE_DOWNLOAD = "DOWNLOAD";
    private static final String GRANT_SOURCE_REGISTRATION_MANAGER_ROLE = "REGISTRATION_MANAGER_ROLE";
    private static final DateTimeFormatter APPROVAL_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final DccRegistrationCertificateAccessPolicyService accessPolicyService;
    private final DccRegistrationCertificateAccessRequestMapper requestMapper;
    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateSnapshotMapper snapshotMapper;
    private final DccRegistrationCertificateFileMapper registrationFileMapper;
    private final DccRegistrationCertificateDownloadConsumptionMapper consumptionMapper;
    private final DccRegistrationCertificateAccessAuditMapper accessAuditMapper;
    private final DccProjectCodeService projectCodeService;
    private final FileService fileService;
    private final JdbcTemplate jdbcTemplate;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentMap<String, Object> downloadLocks = new ConcurrentHashMap<>();

    public DccRegistrationCertificateFileDeliveryServiceImpl(
            DccRegistrationCertificateAccessPolicyService accessPolicyService,
            DccRegistrationCertificateAccessRequestMapper requestMapper,
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateSnapshotMapper snapshotMapper,
            DccRegistrationCertificateFileMapper registrationFileMapper,
            DccRegistrationCertificateDownloadConsumptionMapper consumptionMapper,
            DccRegistrationCertificateAccessAuditMapper accessAuditMapper,
            DccProjectCodeService projectCodeService,
            FileService fileService,
            DccRegistrationCertificateBusinessClock businessClock,
            PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate) {
        this.accessPolicyService = require(accessPolicyService, "accessPolicyService");
        this.requestMapper = require(requestMapper, "requestMapper");
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.snapshotMapper = require(snapshotMapper, "snapshotMapper");
        this.registrationFileMapper = require(registrationFileMapper, "registrationFileMapper");
        this.consumptionMapper = require(consumptionMapper, "consumptionMapper");
        this.accessAuditMapper = require(accessAuditMapper, "accessAuditMapper");
        this.projectCodeService = require(projectCodeService, "projectCodeService");
        this.fileService = require(fileService, "fileService");
        this.businessClock = require(businessClock, "businessClock");
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.transactionTemplate = new TransactionTemplate(require(transactionManager, "transactionManager"));
    }

    @Override
    public DccRegistrationCertificateFileDownloadResult download(Long tenantId, Long userId, Long businessFileId,
                                                                 String attemptKey,
                                                                 DccRequestAuditContext auditContext) {
        String normalizedAttemptKey = requireText(attemptKey, "下载尝试标识");
        DccRequestAuditContext checkedAuditContext = require(auditContext, "auditContext");
        checkedAuditContext.requireRequestId("注册证文件下载");
        LocalDateTime now = businessClock.now();
        DccRegistrationCertificateGrantDO grant = null;
        DccRegistrationCertificateFileDO businessFile = null;
        try {
            businessFile = requireBusinessFile(tenantId, businessFileId);
            DccRegistrationCertificateVersionDO version = requireVersion(tenantId, businessFile);
            DccRegistrationCertificateDO certificate = requireCertificate(tenantId, version.getCertificateId());
            DccRegistrationCertificateSnapshotDO snapshot = requireSnapshot(version.getId());
            if (accessPolicyService.authorizeRegistrationManagerDownloadIfRole(tenantId, userId,
                    certificate.getId())) {
                DccProjectCodeDO projectCode = resolveLiveProjectCode(certificate.getProjectCodeId(),
                        certificate.getProductMasterId());
                String fileName = buildFileName(projectCode, certificate, version, snapshot, businessFile);
                FileDO infraFile = requireInfraFile(businessFile);
                byte[] content;
                try {
                    content = fileService.getFileContent(infraFile.getConfigId(), infraFile.getPath());
                } catch (Exception ex) {
                    recordAudit(tenantId, userId, null, businessFileId, normalizedAttemptKey, RESULT_FAILURE,
                            checkedAuditContext, failureMessage(ex));
                    throw propagate(ex);
                }
                recordSuccessfulRegistrationManagerDelivery(tenantId, userId, businessFileId,
                        normalizedAttemptKey, checkedAuditContext);
                return new DccRegistrationCertificateFileDownloadResult(
                        fileName,
                        firstNotBlank(businessFile.getMimeType(), infraFile.getType()),
                        content,
                        null,
                        businessFileId);
            }
            grant = accessPolicyService.requireDownloadGrant(tenantId, userId, businessFileId, now);
            DccRegistrationCertificateAccessRequestDO request = requireRequest(tenantId, grant.getRequestId());
            DccProjectCodeDO projectCode = resolveLiveProjectCode(request.getProjectCodeId(),
                    certificate.getProductMasterId());
            String fileName = buildFileName(projectCode, certificate, version, snapshot, businessFile);
            String lockKey = tenantId + ":" + grant.getId() + ":" + businessFileId;
            Object lock = downloadLocks.computeIfAbsent(lockKey, ignored -> new Object());
            synchronized (lock) {
                try {
                    if (consumptionMapper.countSuccess(tenantId, grant.getId(), businessFileId) > 0) {
                        throw new ServiceException(REGISTRATION_CERTIFICATE_DOWNLOAD_ALREADY_CONSUMED);
                    }
                    FileDO infraFile = requireInfraFile(businessFile);
                    byte[] content;
                    try {
                        content = fileService.getFileContent(infraFile.getConfigId(), infraFile.getPath());
                    } catch (Exception ex) {
                        recordConsumption(tenantId, grant, businessFileId, normalizedAttemptKey,
                                RESULT_FAILED_BEFORE_START, now, failureMessage(ex));
                        recordAudit(tenantId, userId, grant, businessFileId, normalizedAttemptKey, RESULT_FAILURE,
                                checkedAuditContext, failureMessage(ex));
                        throw propagate(ex);
                    }
                    recordSuccessfulDelivery(tenantId, userId, grant, businessFileId, normalizedAttemptKey, now,
                            checkedAuditContext);
                    return new DccRegistrationCertificateFileDownloadResult(
                            fileName,
                            firstNotBlank(businessFile.getMimeType(), infraFile.getType()),
                            content,
                            grant.getId(),
                            businessFileId);
                } finally {
                    downloadLocks.remove(lockKey, lock);
                }
            }
        } catch (ServiceException ex) {
            recordFailureAuditIfPossible(tenantId, userId, grant, businessFileId, normalizedAttemptKey,
                    checkedAuditContext, ex.getMessage());
            throw ex;
        } catch (DuplicateKeyException ex) {
            recordFailureAuditIfPossible(tenantId, userId, grant, businessFileId, normalizedAttemptKey,
                    checkedAuditContext, "重复使用注册证下载授权");
            throw new ServiceException(REGISTRATION_CERTIFICATE_DOWNLOAD_ALREADY_CONSUMED);
        } catch (RuntimeException ex) {
            recordFailureAuditIfPossible(tenantId, userId, grant, businessFileId, normalizedAttemptKey,
                    checkedAuditContext, failureMessage(ex));
            throw ex;
        }
    }

    private DccRegistrationCertificateFileDO requireBusinessFile(Long tenantId, Long businessFileId) {
        DccRegistrationCertificateFileDO file = registrationFileMapper.selectById(businessFileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenantId)
                || !"BOUND".equals(file.getStatus())
                || !("VERSION".equals(file.getOwnerType()) && "REGISTRATION_CERTIFICATE".equals(file.getFileKind())
                || "CHANGE".equals(file.getOwnerType()) && "CHANGE_APPROVAL".equals(file.getFileKind()))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        return file;
    }

    private DccRegistrationCertificateVersionDO requireVersion(Long tenantId, DccRegistrationCertificateFileDO file) {
        Long versionId = "VERSION".equals(file.getOwnerType()) ? file.getOwnerId() : jdbcTemplate.query("""
                SELECT source_version_id FROM dcc_registration_certificate_change
                 WHERE tenant_id = ? AND id = ? AND status = 'APPLIED' AND deleted = 0
                """, rs -> rs.next() ? rs.getLong(1) : null, tenantId, file.getOwnerId());
        if (versionId == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(versionId);
        if (version == null || !Objects.equals(version.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        return version;
    }

    private DccRegistrationCertificateDO requireCertificate(Long tenantId, Long certificateId) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(certificateId);
        if (certificate == null || !Objects.equals(certificate.getTenantId(), tenantId)
                || "VOIDED".equals(certificate.getStatus()) || "DRAFT".equals(certificate.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        return certificate;
    }

    private DccRegistrationCertificateSnapshotDO requireSnapshot(Long versionId) {
        List<DccRegistrationCertificateSnapshotDO> snapshots = snapshotMapper.selectListByVersionId(versionId);
        if (snapshots == null || snapshots.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        return snapshots.get(snapshots.size() - 1);
    }

    private DccRegistrationCertificateAccessRequestDO requireRequest(Long tenantId, Long requestId) {
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(requestId);
        if (request == null || !Objects.equals(request.getTenantId(), tenantId)
                || !"DOWNLOAD_FILE".equals(request.getRequestType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DOWNLOAD_PROJECT_CODE_INVALID);
        }
        return request;
    }

    private DccProjectCodeDO resolveLiveProjectCode(Long projectCodeId, Long productMasterId) {
        if (projectCodeId == null) {
            return null;
        }
        DccProjectCodeDO projectCode = projectCodeService.getProjectCode(projectCodeId);
        if (projectCode == null || !DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())
                || hasConflictingProductBinding(productMasterId, projectCode.getProductMasterId())
                || StrUtil.isBlank(projectCode.getProjectCode())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DOWNLOAD_PROJECT_CODE_INVALID);
        }
        return projectCode;
    }

    private boolean hasConflictingProductBinding(Long certificateProductMasterId, Long projectProductMasterId) {
        return certificateProductMasterId != null && projectProductMasterId != null
                && !Objects.equals(certificateProductMasterId, projectProductMasterId);
    }

    private FileDO requireInfraFile(DccRegistrationCertificateFileDO businessFile) {
        FileDO infraFile = fileService.getFile(businessFile.getInfraFileId());
        if (infraFile == null || infraFile.getConfigId() == null || StrUtil.isBlank(infraFile.getPath())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        return infraFile;
    }

    private void recordConsumption(Long tenantId, DccRegistrationCertificateGrantDO grant, Long businessFileId,
                                   String attemptKey, String result, LocalDateTime startedAt, String failureReason) {
        DccRegistrationCertificateDownloadConsumptionDO consumption =
                DccRegistrationCertificateDownloadConsumptionDO.builder()
                        .grantId(grant.getId())
                        .businessFileId(businessFileId)
                        .attemptKey(attemptKey)
                        .result(result)
                        .startedAt(startedAt)
                        .completedAt(businessClock.now())
                        .failureReason(failureReason)
                        .detailJson("{}")
                        .build();
        consumption.setTenantId(tenantId);
        try {
            consumptionMapper.insert(consumption);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DOWNLOAD_CONSUMPTION_CONFLICT);
        }
    }

    private void recordSuccessfulDelivery(Long tenantId, Long userId, DccRegistrationCertificateGrantDO grant,
                                          Long businessFileId, String attemptKey, LocalDateTime startedAt,
                                          DccRequestAuditContext auditContext) {
        transactionTemplate.executeWithoutResult(status -> {
            recordConsumption(tenantId, grant, businessFileId, attemptKey, RESULT_SUCCESS, startedAt, null);
            recordAudit(tenantId, userId, grant, businessFileId, attemptKey, RESULT_SUCCESS, auditContext, null);
        });
    }

    private void recordSuccessfulRegistrationManagerDelivery(Long tenantId, Long userId, Long businessFileId,
                                                             String attemptKey,
                                                             DccRequestAuditContext auditContext) {
        transactionTemplate.executeWithoutResult(status -> recordAudit(tenantId, userId, null, businessFileId,
                attemptKey, RESULT_SUCCESS, auditContext, null, GRANT_SOURCE_REGISTRATION_MANAGER_ROLE));
    }

    private void recordAudit(Long tenantId, Long userId, DccRegistrationCertificateGrantDO grant, Long businessFileId,
                             String attemptKey, String result, DccRequestAuditContext auditContext, String reason) {
        recordAudit(tenantId, userId, grant, businessFileId, attemptKey, result, auditContext, reason, null);
    }

    private void recordAudit(Long tenantId, Long userId, DccRegistrationCertificateGrantDO grant, Long businessFileId,
                             String attemptKey, String result, DccRequestAuditContext auditContext, String reason,
                             String grantSource) {
        DccRegistrationCertificateAccessAuditDO audit = DccRegistrationCertificateAccessAuditDO.builder()
                .requestId(grant == null ? null : grant.getRequestId())
                .grantId(grant == null ? null : grant.getId())
                .businessFileId(businessFileId)
                .actorUserId(userId)
                .eventType(EVENT_TYPE_DOWNLOAD)
                .eventKey(attemptKey + ":" + EVENT_TYPE_DOWNLOAD + ":" + result)
                .result(result)
                .occurredAt(businessClock.now())
                .detailJson(buildAuditDetail(auditContext, reason, grantSource))
                .build();
        audit.setTenantId(tenantId);
        try {
            accessAuditMapper.insert(audit);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_DELIVERY_AUDIT_CONFLICT);
        }
    }

    private void recordFailureAuditIfPossible(Long tenantId, Long userId, DccRegistrationCertificateGrantDO grant,
                                              Long businessFileId, String attemptKey,
                                              DccRequestAuditContext auditContext, String reason) {
        if (accessAuditMapper.selectByEventKey(tenantId, attemptKey + ":" + EVENT_TYPE_DOWNLOAD + ":" + RESULT_FAILURE)
                == null) {
            recordAudit(tenantId, userId, grant, businessFileId, attemptKey, RESULT_FAILURE, auditContext, reason);
        }
    }

    private String buildFileName(DccProjectCodeDO projectCode, DccRegistrationCertificateDO certificate,
                                 DccRegistrationCertificateVersionDO version,
                                 DccRegistrationCertificateSnapshotDO snapshot,
                                 DccRegistrationCertificateFileDO businessFile) {
        String extension = extensionOf(businessFile.getOriginalName());
        String changeSuffix = "CHANGE".equals(businessFile.getOwnerType()) ? "_变更文件" : "";
        String expiredSuffix = "OLD".equals(version.getStatus()) ? "_已失效" : "";
        String projectSegment = projectCode == null ? "" : safeSegment(projectCode.getProjectCode());
        return projectSegment + "_"
                + resolveFileNameDate(certificate, version).format(APPROVAL_DATE_FORMAT) + "_"
                + safeSegment(snapshot.getProductName()) + changeSuffix + "_"
                + safeSegment(version.getCertificateNo()) + expiredSuffix + extension;
    }

    private LocalDate resolveFileNameDate(DccRegistrationCertificateDO certificate,
                                          DccRegistrationCertificateVersionDO version) {
        if (version.getApprovalDate() != null) {
            return version.getApprovalDate();
        }
        if (certificate.getFirstObtainedDate() != null) {
            return certificate.getFirstObtainedDate();
        }
        throw invalidParamException("注册证下载文件名日期不能为空");
    }

    private static String extensionOf(String originalName) {
        String name = requireText(originalName, "注册证原始文件名");
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw invalidParamException("注册证原始文件名必须包含扩展名");
        }
        String extension = name.substring(dot).toLowerCase();
        if (!extension.matches("\\.[a-z0-9]{1,12}")) {
            throw invalidParamException("注册证原始文件扩展名不合法");
        }
        return extension;
    }

    private static String safeSegment(String value) {
        String text = requireText(value, "注册证文件名组成部分");
        String normalized = text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException("注册证文件名组成部分不合法");
        }
        return normalized;
    }

    private static String buildAuditDetail(DccRequestAuditContext auditContext, String reason, String grantSource) {
        Map<String, String> detail = new LinkedHashMap<>();
        detail.put("requestId", auditContext.requestId());
        detail.put("sourceIp", auditContext.sourceIp());
        detail.put("userAgent", auditContext.userAgent());
        if (reason != null) {
            detail.put("reason", reason);
        }
        if (grantSource != null) {
            detail.put("grantSource", grantSource);
        }
        return JsonUtils.toJsonString(detail);
    }

    private static RuntimeException propagate(Exception ex) {
        if (ex instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("注册证文件内容不可用", ex);
    }

    private static String failureMessage(Throwable ex) {
        String message = ex.getMessage();
        return StrUtil.isBlank(message) ? ex.getClass().getSimpleName() : message;
    }

    private static String firstNotBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : requireText(second, "注册证文件内容类型");
    }

    private static String requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw invalidParamException("{}不能为空", fieldName);
        }
        return StrUtil.trim(value);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }
}
