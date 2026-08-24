package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
        String normalizedAttemptKey = requireText(attemptKey, "download attempt key");
        DccRequestAuditContext checkedAuditContext = require(auditContext, "auditContext");
        checkedAuditContext.requireRequestId("registration certificate file download");
        LocalDateTime now = businessClock.now();
        DccRegistrationCertificateGrantDO grant = null;
        DccRegistrationCertificateFileDO businessFile = null;
        try {
            grant = accessPolicyService.requireDownloadGrant(tenantId, userId, businessFileId, now);
            businessFile = requireBusinessFile(tenantId, businessFileId);
            DccRegistrationCertificateVersionDO version = requireVersion(tenantId, businessFile);
            DccRegistrationCertificateDO certificate = requireCertificate(tenantId, grant.getCertificateId());
            DccRegistrationCertificateSnapshotDO snapshot = requireSnapshot(version.getId());
            DccRegistrationCertificateAccessRequestDO request = requireRequest(tenantId, grant.getRequestId());
            DccProjectCodeDO projectCode = requireLiveProjectCode(userId, request.getProjectCodeId(),
                    certificate.getProductMasterId());
            String fileName = buildFileName(projectCode, version, snapshot, businessFile);
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
                    checkedAuditContext, "download duplicate consumption");
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
                || !"DOWNLOAD_FILE".equals(request.getRequestType())
                || request.getProjectCodeId() == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DOWNLOAD_PROJECT_CODE_INVALID);
        }
        return request;
    }

    private DccProjectCodeDO requireLiveProjectCode(Long userId, Long projectCodeId, Long productMasterId) {
        DccProjectCodeDO projectCode = projectCodeService.getProjectCode(userId, projectCodeId);
        if (projectCode == null || !DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())
                || !Objects.equals(projectCode.getProductMasterId(), productMasterId)
                || StrUtil.isBlank(projectCode.getProjectCode())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DOWNLOAD_PROJECT_CODE_INVALID);
        }
        return projectCode;
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

    private void recordAudit(Long tenantId, Long userId, DccRegistrationCertificateGrantDO grant, Long businessFileId,
                             String attemptKey, String result, DccRequestAuditContext auditContext, String reason) {
        DccRegistrationCertificateAccessAuditDO audit = DccRegistrationCertificateAccessAuditDO.builder()
                .requestId(grant == null ? null : grant.getRequestId())
                .grantId(grant == null ? null : grant.getId())
                .businessFileId(businessFileId)
                .actorUserId(userId)
                .eventType(EVENT_TYPE_DOWNLOAD)
                .eventKey(attemptKey + ":" + EVENT_TYPE_DOWNLOAD + ":" + result)
                .result(result)
                .occurredAt(businessClock.now())
                .detailJson(buildAuditDetail(auditContext, reason))
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

    private String buildFileName(DccProjectCodeDO projectCode, DccRegistrationCertificateVersionDO version,
                                 DccRegistrationCertificateSnapshotDO snapshot,
                                 DccRegistrationCertificateFileDO businessFile) {
        String extension = extensionOf(businessFile.getOriginalName());
        String changeSuffix = "CHANGE".equals(businessFile.getOwnerType()) ? "_变更文件" : "";
        String expiredSuffix = "OLD".equals(version.getStatus()) ? "_已失效" : "";
        return safeSegment(projectCode.getProjectCode()) + "_"
                + version.getApprovalDate().format(APPROVAL_DATE_FORMAT) + "_"
                + safeSegment(snapshot.getProductName()) + changeSuffix + "_"
                + safeSegment(version.getCertificateNo()) + expiredSuffix + extension;
    }

    private static String extensionOf(String originalName) {
        String name = requireText(originalName, "registration certificate original filename");
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw invalidParamException("registration certificate original filename extension is required");
        }
        String extension = name.substring(dot).toLowerCase();
        if (!extension.matches("\\.[a-z0-9]{1,12}")) {
            throw invalidParamException("registration certificate original filename extension is invalid");
        }
        return extension;
    }

    private static String safeSegment(String value) {
        String text = requireText(value, "registration certificate filename segment");
        String normalized = text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException("registration certificate filename segment is invalid");
        }
        return normalized;
    }

    private static String buildAuditDetail(DccRequestAuditContext auditContext, String reason) {
        String reasonJson = reason == null ? "" : ",\"reason\":\"" + jsonEscape(reason) + "\"";
        return "{\"requestId\":\"" + jsonEscape(auditContext.requestId()) + "\",\"sourceIp\":\""
                + jsonEscape(auditContext.sourceIp()) + "\",\"userAgent\":\""
                + jsonEscape(auditContext.userAgent()) + "\"" + reasonJson + "}";
    }

    private static RuntimeException propagate(Exception ex) {
        if (ex instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("registration certificate file content unavailable", ex);
    }

    private static String failureMessage(Throwable ex) {
        String message = ex.getMessage();
        return StrUtil.isBlank(message) ? ex.getClass().getSimpleName() : message;
    }

    private static String firstNotBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : requireText(second, "registration certificate content type");
    }

    private static String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw invalidParamException("{} is required", fieldName);
        }
        return StrUtil.trim(value);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
