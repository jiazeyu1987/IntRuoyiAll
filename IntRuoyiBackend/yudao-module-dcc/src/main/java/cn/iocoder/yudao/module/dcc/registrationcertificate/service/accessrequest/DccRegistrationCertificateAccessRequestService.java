package cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_KEY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_NOT_STAGED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_STATUS_INVALID;

@Service
public class DccRegistrationCertificateAccessRequestService {

    private static final String TYPE_VIEW_OLD_CERTIFICATE = "VIEW_OLD_CERTIFICATE";
    private static final String TYPE_DOWNLOAD_FILE = "DOWNLOAD_FILE";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String FILE_STATUS_REQUESTED = "REQUESTED";
    private static final String FILE_KIND_REGISTRATION_CERTIFICATE = "REGISTRATION_CERTIFICATE";
    private static final String FILE_KIND_CHANGE_APPROVAL = "CHANGE_APPROVAL";
    private static final String FILE_OWNER_TYPE_VERSION = "VERSION";
    private static final String FILE_OWNER_TYPE_CHANGE = "CHANGE";
    private static final String FILE_STATUS_BOUND = "BOUND";
    private static final String CHANGE_STATUS_APPLIED = "APPLIED";

    private final DccRegistrationCertificateAccessRequestMapper requestMapper;
    private final DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateFileMapper fileMapper;
    private final DccProjectCodeService projectCodeService;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateAccessRequestService(
            DccRegistrationCertificateAccessRequestMapper requestMapper,
            DccRegistrationCertificateAccessRequestFileMapper requestFileMapper,
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            DccProjectCodeService projectCodeService,
            DccRegistrationCertificateBusinessClock businessClock,
            JdbcTemplate jdbcTemplate) {
        this.requestMapper = require(requestMapper, "requestMapper");
        this.requestFileMapper = require(requestFileMapper, "requestFileMapper");
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.projectCodeService = require(projectCodeService, "projectCodeService");
        this.businessClock = require(businessClock, "businessClock");
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateAccessRequestResult submit(
            Long tenantId, Long actorId, String requestKey,
            DccRegistrationCertificateAccessRequestCommand command) {
        String normalizedKey = requireText(requestKey, "requestKey", REGISTRATION_CERTIFICATE_ACCESS_REQUEST_KEY_REQUIRED);
        NormalizedCommand requested = normalize(command);
        DccRegistrationCertificateDO certificate = requireCertificate(tenantId, requested.certificateId());
        requireOwnerCompany(certificate.getOwnerCompanyId());
        NormalizedCommand normalized = resolveFormalReferences(tenantId, certificate, requested);
        String payloadHash = payloadHash(normalized);
        DccRegistrationCertificateAccessRequestDO existing =
                requestMapper.selectByTenantAndRequestKey(tenantId, normalizedKey);
        if (existing != null) {
            return replay(existing, payloadHash);
        }
        if (TYPE_DOWNLOAD_FILE.equals(normalized.requestType()) && normalized.projectCodeId() != null) {
            validateProjectCode(tenantId, normalized.projectCodeId(), certificate.getProductMasterId());
        }
        List<DccRegistrationCertificateFileDO> files = validateFiles(
                tenantId, certificate, normalized.requestType(), normalized.businessFileIds());
        LocalDateTime now = businessClock.now();
        DccRegistrationCertificateAccessRequestDO request = DccRegistrationCertificateAccessRequestDO.builder()
                .ownerCompanyId(certificate.getOwnerCompanyId())
                .certificateId(certificate.getId())
                .requesterUserId(actorId)
                .requestType(normalized.requestType())
                .requestKey(normalizedKey)
                .purpose(normalized.purpose())
                .projectCodeId(normalized.projectCodeId())
                .status(STATUS_SUBMITTED)
                .requestedAt(now)
                .detailJson(detailJson(payloadHash, normalized))
                .build();
        request.setTenantId(tenantId);
        try {
            requestMapper.insert(request);
        } catch (DuplicateKeyException ex) {
            DccRegistrationCertificateAccessRequestDO duplicate =
                    requestMapper.selectByTenantAndRequestKey(tenantId, normalizedKey);
            if (duplicate != null) {
                return replay(duplicate, payloadHash);
            }
            throw ex;
        }
        for (DccRegistrationCertificateFileDO file : files) {
            DccRegistrationCertificateAccessRequestFileDO requestFile =
                    DccRegistrationCertificateAccessRequestFileDO.builder()
                            .requestId(request.getId())
                            .businessFileId(file.getId())
                            .fileKind(file.getFileKind())
                            .downloadRequested(TYPE_DOWNLOAD_FILE.equals(normalized.requestType()))
                            .status(FILE_STATUS_REQUESTED)
                            .detailJson(JsonUtils.toJsonString(Map.of("payloadHash", payloadHash)))
                            .build();
            requestFile.setTenantId(tenantId);
            requestFileMapper.insert(requestFile);
        }
        return new DccRegistrationCertificateAccessRequestResult(request.getId(), request.getCertificateId(),
                request.getOwnerCompanyId(), request.getStatus(), files.stream()
                .map(DccRegistrationCertificateFileDO::getId).toList());
    }

    private DccRegistrationCertificateAccessRequestResult replay(
            DccRegistrationCertificateAccessRequestDO existing, String payloadHash) {
        Map<?, ?> detail = JsonUtils.parseObject(existing.getDetailJson(), Map.class);
        if (detail == null || !Objects.equals(payloadHash, detail.get("payloadHash"))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        List<Long> fileIds = requestFileMapper.selectByRequestId(existing.getTenantId(), existing.getId()).stream()
                .map(DccRegistrationCertificateAccessRequestFileDO::getBusinessFileId)
                .toList();
        return new DccRegistrationCertificateAccessRequestResult(existing.getId(), existing.getCertificateId(),
                existing.getOwnerCompanyId(), existing.getStatus(), fileIds);
    }

    private NormalizedCommand normalize(DccRegistrationCertificateAccessRequestCommand command) {
        if (command == null || command.certificateId() == null || command.certificateId() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        String type = requireText(command.requestType(), "requestType",
                REGISTRATION_CERTIFICATE_ACCESS_REQUEST_TYPE_INVALID);
        if (!TYPE_VIEW_OLD_CERTIFICATE.equals(type) && !TYPE_DOWNLOAD_FILE.equals(type)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_TYPE_INVALID);
        }
        String purpose = requireText(command.purpose(), "purpose",
                REGISTRATION_CERTIFICATE_ACCESS_REQUEST_TYPE_INVALID);
        List<Long> businessFileIds = normalizeFileIds(command.businessFileIds());
        if (TYPE_DOWNLOAD_FILE.equals(type)) {
            // Project and file identities are resolved from the formal certificate facts in submit().
        } else if (!businessFileIds.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_TYPE_INVALID);
        }
        return new NormalizedCommand(command.certificateId(), type, purpose, command.projectCodeId(), businessFileIds);
    }

    private NormalizedCommand resolveFormalReferences(
            Long tenantId, DccRegistrationCertificateDO certificate,
            NormalizedCommand requested) {
        if (!TYPE_DOWNLOAD_FILE.equals(requested.requestType())) {
            return requested;
        }
        Long projectCodeId = requested.projectCodeId();
        if (projectCodeId == null) {
            projectCodeId = certificate.getProjectCodeId();
        } else if (!Objects.equals(projectCodeId, certificate.getProjectCodeId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID);
        }
        if (projectCodeId != null && projectCodeId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID);
        }
        List<Long> fileIds = requested.businessFileIds();
        if (fileIds.isEmpty()) {
            if (certificate.getCurrentVersionId() == null) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);
            }
            List<DccRegistrationCertificateFileDO> candidates = fileMapper.selectList(
                    new LambdaQueryWrapperX<DccRegistrationCertificateFileDO>()
                            .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                            .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_TYPE_VERSION)
                            .eq(DccRegistrationCertificateFileDO::getOwnerId, certificate.getCurrentVersionId())
                            .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                            .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND));
            if (candidates == null || candidates.isEmpty()) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);
            }
            if (candidates.size() != 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
            }
            fileIds = List.of(candidates.get(0).getId());
        }
        return new NormalizedCommand(requested.certificateId(), requested.requestType(), requested.purpose(),
                projectCodeId, fileIds);
    }

    private List<Long> normalizeFileIds(List<Long> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Long rawId : rawIds) {
            if (rawId == null || rawId <= 0 || !ids.add(rawId)) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
            }
        }
        return new ArrayList<>(ids);
    }

    private DccRegistrationCertificateDO requireCertificate(Long tenantId, Long certificateId) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(certificateId);
        if (certificate == null || !Objects.equals(certificate.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        if ("DRAFT".equals(certificate.getStatus()) || "VOIDED".equals(certificate.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_STATUS_INVALID);
        }
        return certificate;
    }

    private void requireOwnerCompany(Long ownerCompanyId) {
        if (ownerCompanyId == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED);
        }
    }

    private void validateProjectCode(Long tenantId, Long projectCodeId, Long productMasterId) {
        DccProjectCodeDO projectCode = projectCodeService.getProjectCode(projectCodeId);
        if (projectCode == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID);
        }
        if (!Objects.equals(projectCode.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH);
        }
        if (!DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED);
        }
        if (hasConflictingProductBinding(productMasterId, projectCode.getProductMasterId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH);
        }
    }

    private boolean hasConflictingProductBinding(Long certificateProductMasterId, Long projectProductMasterId) {
        return certificateProductMasterId != null && projectProductMasterId != null
                && !Objects.equals(certificateProductMasterId, projectProductMasterId);
    }

    private List<DccRegistrationCertificateFileDO> validateFiles(
            Long tenantId, DccRegistrationCertificateDO certificate, String requestType, List<Long> businessFileIds) {
        if (!TYPE_DOWNLOAD_FILE.equals(requestType)) {
            return List.of();
        }
        List<DccRegistrationCertificateFileDO> files = new ArrayList<>();
        for (Long businessFileId : businessFileIds) {
            DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
            if (file == null || !Objects.equals(file.getTenantId(), tenantId)
                    || !FILE_STATUS_BOUND.equals(file.getStatus())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);
            }
            if (isVersionRegistrationFile(file)) {
                validateVersionFile(tenantId, certificate, file);
            } else if (isChangeApprovalFile(file)) {
                validateChangeApprovalFile(tenantId, certificate, file);
            } else {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
            }
            files.add(file);
        }
        return files;
    }

    private boolean isVersionRegistrationFile(DccRegistrationCertificateFileDO file) {
        return FILE_OWNER_TYPE_VERSION.equals(file.getOwnerType())
                && FILE_KIND_REGISTRATION_CERTIFICATE.equals(file.getFileKind());
    }

    private boolean isChangeApprovalFile(DccRegistrationCertificateFileDO file) {
        return FILE_OWNER_TYPE_CHANGE.equals(file.getOwnerType())
                && FILE_KIND_CHANGE_APPROVAL.equals(file.getFileKind());
    }

    private void validateVersionFile(
            Long tenantId, DccRegistrationCertificateDO certificate, DccRegistrationCertificateFileDO file) {
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(file.getOwnerId());
        if (version == null || !Objects.equals(version.getTenantId(), tenantId)
                || !Objects.equals(version.getCertificateId(), certificate.getId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
    }

    private void validateChangeApprovalFile(
            Long tenantId, DccRegistrationCertificateDO certificate, DccRegistrationCertificateFileDO file) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_change
                 WHERE tenant_id = ? AND id = ? AND certificate_id = ?
                   AND status = ? AND deleted = 0
                """, Long.class, tenantId, file.getOwnerId(), certificate.getId(), CHANGE_STATUS_APPLIED);
        if (!Objects.equals(count, 1L)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
    }

    private String payloadHash(NormalizedCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("certificateId", command.certificateId());
        payload.put("requestType", command.requestType());
        payload.put("purpose", command.purpose());
        payload.put("projectCodeId", command.projectCodeId());
        payload.put("businessFileIds", command.businessFileIds());
        return sha256Hex(JsonUtils.toJsonString(payload));
    }

    private static String detailJson(String payloadHash, NormalizedCommand command) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("payloadHash", payloadHash);
        detail.put("requestType", command.requestType());
        detail.put("businessFileIds", command.businessFileIds());
        return JsonUtils.toJsonString(detail);
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("注册证访问申请必须启用 SHA-256 算法", ex);
        }
    }

    private static String requireText(String value, String name, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        if (value == null || value.trim().isEmpty()) {
            throw new ServiceException(errorCode);
        }
        return value.trim();
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }

    private record NormalizedCommand(
            Long certificateId,
            String requestType,
            String purpose,
            Long projectCodeId,
            List<Long> businessFileIds) {
    }
}
