package cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.association.DccRegistrationCertificateProjectCodeFileAssociationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentProjectionSnapshot;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CANDIDATE_VOID_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_NOT_STAGED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.REQUEST_TYPE_UPLOAD_CERTIFICATE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;

@Service
public class DccRegistrationCertificateRenewalService {

    private static final String VERSION_TYPE_RENEWAL = "RENEWAL_CERTIFICATE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CURRENT = "CURRENT";
    private static final String STATUS_PENDING = "PENDING_EFFECTIVE";
    private static final String STATUS_VOIDED = "VOIDED";
    private static final String FILE_STATUS_STAGED = "STAGED";
    private static final String FILE_STATUS_BOUND = "BOUND";
    private static final String FILE_STATUS_VOIDED = "VOIDED";
    private static final String FILE_OWNER_VERSION = "VERSION";
    private static final String FILE_KIND_REGISTRATION_CERTIFICATE = "REGISTRATION_CERTIFICATE";
    private static final String REQUEST_STATUS_SUBMITTED = "SUBMITTED";
    private static final String REQUEST_STATUS_APPROVED = "APPROVED";
    private static final String REQUEST_FILE_STATUS_REQUESTED = "REQUESTED";
    private static final String REQUEST_FILE_STATUS_APPROVED = "APPROVED";
    private static final String REQUEST_FILE_STATUS_REJECTED = "REJECTED";
    private static final String RENEWAL_REQUEST_PURPOSE = "上传延续注册证，待注册部经理审批";
    private static final int MAX_CERTIFICATE_NO_LENGTH = 128;
    private static final int MAX_CLASSIFICATION_LENGTH = 64;
    private static final Set<String> OWNED_COMPANY = Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateSnapshotMapper snapshotMapper;
    private final DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    private final DccRegistrationCertificateFileMapper fileMapper;
    private final DccRegistrationCertificateAccessRequestMapper requestMapper;
    private final DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    private final FileService fileService;
    private final JdbcTemplate jdbcTemplate;
    private final ControlledContentRegistrationProjectionService projectionService;
    private final DccRegistrationCertificateActivationService activationService;
    private final MdmEnterpriseApi enterpriseApi;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;
    private final DccRegistrationCertificateProjectCodeFileAssociationService projectCodeFileAssociationService;

    public DccRegistrationCertificateRenewalService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateSnapshotMapper snapshotMapper,
            DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            DccRegistrationCertificateAccessRequestMapper requestMapper,
            DccRegistrationCertificateAccessRequestFileMapper requestFileMapper,
            FileService fileService,
            JdbcTemplate jdbcTemplate,
            ControlledContentRegistrationProjectionService projectionService,
            DccRegistrationCertificateActivationService activationService,
            MdmEnterpriseApi enterpriseApi,
            DccRegistrationCertificateBusinessClock businessClock,
            DccRegistrationCertificateBusinessEventNotifier businessEventNotifier,
            DccRegistrationCertificateProjectCodeFileAssociationService projectCodeFileAssociationService) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.snapshotMapper = require(snapshotMapper, "snapshotMapper");
        this.entrustedMapper = require(entrustedMapper, "entrustedMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.requestMapper = require(requestMapper, "requestMapper");
        this.requestFileMapper = require(requestFileMapper, "requestFileMapper");
        this.fileService = require(fileService, "fileService");
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.projectionService = require(projectionService, "projectionService");
        this.activationService = require(activationService, "activationService");
        this.enterpriseApi = require(enterpriseApi, "enterpriseApi");
        this.businessClock = require(businessClock, "businessClock");
        this.businessEventNotifier = require(businessEventNotifier, "businessEventNotifier");
        this.projectCodeFileAssociationService = require(
                projectCodeFileAssociationService, "projectCodeFileAssociationService");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateRenewalSubmitResult submitRenewalForApproval(
            DccRegistrationCertificateRenewalSubmitCommand command) {
        validateEventInput(command.tenantId(), command.actorId(), command.idempotencyKey(), command.requestTraceId());
        RenewalUploadFile uploadFile = requireUploadFile(command.file());
        String payloadHash = submitPayloadHash(command, uploadFile);
        DccRegistrationCertificateAccessRequestDO existing =
                requestMapper.selectByTenantAndRequestKey(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replaySubmit(existing, payloadHash);
        }

        DccRegistrationCertificateRenewalCommand candidateCommand = candidateCommand(command, null);
        DccRegistrationCertificateDO certificate = requireActiveCertificate(
                candidateCommand, certificateMapper.selectByIdForUpdate(command.certificateId()));
        DccRegistrationCertificateVersionDO currentVersion = requireCurrentVersion(certificate, command.currentVersionId());
        validateRenewalDates(certificate.getFirstObtainedDate(), command.approvalDate(),
                command.effectiveDate(), command.expiryDate());
        String renewalCertificateNo = resolveRenewalCertificateNo(
                command.categoryChanged(), command.certificateNo(), currentVersion);
        String renewalClassification = resolveRenewalClassification(
                command.categoryChanged(), command.classification(), currentVersion);
        DccRegistrationCertificateSnapshotDO currentSnapshot = requireSnapshot(
                certificate.getCurrentSnapshotId(), currentVersion.getId());
        String productName = requireSummaryText(currentSnapshot.getProductName());
        String ownerCompanyName = resolveOwnerCompanyName(command.tenantId(), certificate.getOwnerCompanyId());
        ensureNoOpenRenewalApproval(command.tenantId(), certificate.getId());

        Long infraFileId = fileService.createFileAndReturnId(
                uploadFile.content(), uploadFile.originalName(),
                "dcc/registration-certificate/renewal/" + certificate.getId(), uploadFile.mimeType());
        DccRegistrationCertificateFileDO businessFile = DccRegistrationCertificateFileDO.builder()
                .ownerType(FILE_OWNER_VERSION)
                .ownerId(currentVersion.getId())
                .fileKind(FILE_KIND_REGISTRATION_CERTIFICATE)
                .infraFileId(infraFileId)
                .originalName(uploadFile.originalName())
                .mimeType(uploadFile.mimeType())
                .fileSize(uploadFile.fileSize())
                .sha256(uploadFile.sha256())
                .status(FILE_STATUS_STAGED)
                .build();
        businessFile.setTenantId(command.tenantId());
        requireSingle(fileMapper.insert(businessFile), REGISTRATION_CERTIFICATE_FILE_CONFLICT);

        DccRegistrationCertificateAccessRequestDO request = DccRegistrationCertificateAccessRequestDO.builder()
                .ownerCompanyId(certificate.getOwnerCompanyId())
                .certificateId(certificate.getId())
                .requesterUserId(command.actorId())
                .requestType(REQUEST_TYPE_UPLOAD_CERTIFICATE)
                .requestKey(command.idempotencyKey().trim())
                .purpose(RENEWAL_REQUEST_PURPOSE)
                .status(REQUEST_STATUS_SUBMITTED)
                .requestedAt(businessClock.now())
                .detailJson(JsonUtils.toJsonString(submitDetail(
                        command, businessFile.getId(), payloadHash, uploadFile,
                        renewalCertificateNo, renewalClassification, productName, ownerCompanyName)))
                .build();
        request.setTenantId(command.tenantId());
        try {
            requireSingle(requestMapper.insert(request), REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        } catch (DuplicateKeyException exception) {
            DccRegistrationCertificateAccessRequestDO duplicate =
                    requestMapper.selectByTenantAndRequestKey(command.tenantId(), command.idempotencyKey());
            if (duplicate != null) {
                return replaySubmit(duplicate, payloadHash);
            }
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }

        DccRegistrationCertificateAccessRequestFileDO requestFile =
                DccRegistrationCertificateAccessRequestFileDO.builder()
                        .requestId(request.getId())
                        .businessFileId(businessFile.getId())
                        .fileKind(FILE_KIND_REGISTRATION_CERTIFICATE)
                        .downloadRequested(false)
                        .status(REQUEST_FILE_STATUS_REQUESTED)
                        .detailJson(JsonUtils.toJsonString(Map.of("payloadHash", payloadHash)))
                        .build();
        requestFile.setTenantId(command.tenantId());
        requireSingle(requestFileMapper.insert(requestFile), REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        return new DccRegistrationCertificateRenewalSubmitResult(
                request.getId(), certificate.getId(), businessFile.getId(), request.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateRenewalResult approveRenewalRequest(
            Long tenantId, Long approverId, Long requestId, String approvalKey) {
        DccRegistrationCertificateAccessRequestDO request = requireRenewalRequest(tenantId, requestId);
        if (!REQUEST_STATUS_APPROVED.equals(request.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        RenewalRequestDetail detail = parseRenewalRequestDetail(request);
        DccRegistrationCertificateRenewalResult result = uploadRenewalCandidate(new DccRegistrationCertificateRenewalCommand(
                tenantId, approverId, approvalKey, approvalKey, request.getCertificateId(),
                detail.expectedRowVersion(), detail.currentVersionId(), detail.businessFileId(),
                detail.approvalDate(), detail.effectiveDate(), detail.expiryDate(),
                detail.categoryChanged(), detail.certificateNo(), detail.classification()));
        markRenewalRequestFiles(tenantId, requestId, REQUEST_FILE_STATUS_APPROVED);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectRenewalRequest(Long tenantId, Long actorId, Long requestId, String reason) {
        DccRegistrationCertificateAccessRequestDO request = requireRenewalRequest(tenantId, requestId);
        RenewalRequestDetail detail = parseRenewalRequestDetail(request);
        markRenewalRequestFiles(tenantId, requestId, REQUEST_FILE_STATUS_REJECTED);
        voidStagedRenewalFile(tenantId, detail.businessFileId(), actorId, reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateRenewalResult uploadRenewalCandidate(
            DccRegistrationCertificateRenewalCommand command) {
        validateEventInput(command.tenantId(), command.actorId(), command.idempotencyKey(), command.requestTraceId());
        String payloadHash = uploadPayloadHash(command);
        LifecycleEvent existing = findEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replayUpload(command, payloadHash, existing);
        }

        DccRegistrationCertificateDO certificate = requireActiveCertificate(command);
        DccRegistrationCertificateVersionDO currentVersion = requireCurrentVersion(certificate, command.currentVersionId());
        DccRegistrationCertificateSnapshotDO currentSnapshot = requireSnapshot(certificate.getCurrentSnapshotId(),
                currentVersion.getId());
        List<DccRegistrationCertificateSnapshotEntrustedDO> entrustedRows =
                entrustedMapper.selectListBySnapshotId(currentSnapshot.getId());
        DccRegistrationCertificateFileDO file = requireStagedRenewalFile(
                command.tenantId(), currentVersion.getId(), command.businessFileId());
        validateRenewalDates(certificate.getFirstObtainedDate(), command.approvalDate(),
                command.effectiveDate(), command.expiryDate());
        boolean categoryChanged = requireCategoryChanged(command.categoryChanged());
        String renewalCertificateNo = resolveRenewalCertificateNo(
                command.categoryChanged(), command.certificateNo(), currentVersion);
        String renewalClassification = resolveRenewalClassification(
                command.categoryChanged(), command.classification(), currentVersion);

        DccRegistrationCertificateVersionDO renewalVersion = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(currentVersion.getVersionNo() + 1)
                .versionType(VERSION_TYPE_RENEWAL)
                .certificateNo(renewalCertificateNo)
                .approvalDate(command.approvalDate())
                .effectiveDate(command.effectiveDate())
                .expiryDate(command.expiryDate())
                .classification(renewalClassification)
                .categoryChanged(categoryChanged)
                .baseSnapshotId(currentSnapshot.getId())
                .status(STATUS_PENDING)
                .formalizedAt(businessClock.now())
                .formalizedBy(command.actorId())
                .build();
        renewalVersion.setTenantId(command.tenantId());
        try {
            requireSingle(versionMapper.insert(renewalVersion), REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }

        DccRegistrationCertificateSnapshotDO renewalSnapshot = copySnapshot(currentSnapshot, renewalVersion.getId());
        requireSingle(snapshotMapper.insert(renewalSnapshot), REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        for (DccRegistrationCertificateSnapshotEntrustedDO row : entrustedRows) {
            DccRegistrationCertificateSnapshotEntrustedDO copy =
                    DccRegistrationCertificateSnapshotEntrustedDO.builder()
                            .snapshotId(renewalSnapshot.getId())
                            .enterpriseId(row.getEnterpriseId())
                            .enterpriseNameSnapshot(row.getEnterpriseNameSnapshot())
                            .sortOrder(row.getSortOrder())
                            .build();
            copy.setTenantId(command.tenantId());
            requireSingle(entrustedMapper.insert(copy), REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }

        requireSingle(certificateMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, certificate.getId())
                        .eq(DccRegistrationCertificateDO::getTenantId, command.tenantId())
                        .eq(DccRegistrationCertificateDO::getStatus, STATUS_ACTIVE)
                        .eq(DccRegistrationCertificateDO::getCurrentVersionId, currentVersion.getId())
                        .isNull(DccRegistrationCertificateDO::getPendingVersionId)
                        .eq(DccRegistrationCertificateDO::getRowVersion, command.expectedRowVersion())
                        .set(DccRegistrationCertificateDO::getPendingVersionId, renewalVersion.getId())
                        .setSql("row_version = row_version + 1")),
                REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);

        requireSingle(fileMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateFileDO>()
                        .eq(DccRegistrationCertificateFileDO::getId, file.getId())
                        .eq(DccRegistrationCertificateFileDO::getTenantId, command.tenantId())
                        .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_VERSION)
                        .eq(DccRegistrationCertificateFileDO::getOwnerId, currentVersion.getId())
                        .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                        .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_STAGED)
                        .set(DccRegistrationCertificateFileDO::getOwnerId, renewalVersion.getId())
                        .set(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND)
                        .set(DccRegistrationCertificateFileDO::getBoundAt, businessClock.now())
                        .set(DccRegistrationCertificateFileDO::getBoundBy, command.actorId())),
                REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);
        projectCodeFileAssociationService.bindVersionRegistrationFile(
                command.tenantId(), renewalVersion.getId(), file.getId(), command.actorId());

        registerPlatformCandidate(command, certificate, currentVersion, renewalVersion);
        insertLifecycleEvent(command.tenantId(), certificate.getOwnerCompanyId(), certificate.getId(),
                currentVersion.getId(), renewalVersion.getId(), currentSnapshot.getId(), renewalSnapshot.getId(),
                command.idempotencyKey(), "RENEWAL_UPLOADED", command.expectedRowVersion(),
                currentSnapshot.getRevisionNo(), command.actorId(),
                new RenewalEventDetail(payloadHash, certificate.getId(), renewalVersion.getId(),
                        renewalSnapshot.getId(), file.getId(), false));
        businessEventNotifier.notifyRenewalCandidateUploaded(
                command.tenantId(), certificate.getOwnerCompanyId(), certificate.getId(), renewalVersion.getId(),
                command.actorId(), command.idempotencyKey(), renewalSnapshot.getProductName(),
                renewalVersion.getCertificateNo(), renewalVersion.getEffectiveDate(), renewalVersion.getExpiryDate());
        if (!command.effectiveDate().isAfter(businessClock.businessDate())) {
            activationService.activateDueCandidate(new DccRegistrationCertificateActivationCommand(
                    command.tenantId(), command.actorId(), command.idempotencyKey() + ":activation",
                    command.requestTraceId() + ":activation", command.certificateId(),
                    Math.addExact(command.expectedRowVersion(), 1), currentVersion.getId(), renewalVersion.getId()));
            return new DccRegistrationCertificateRenewalResult(certificate.getId(), renewalVersion.getId(),
                    renewalSnapshot.getId(), file.getId(), STATUS_ACTIVE, STATUS_CURRENT, false);
        }
        return new DccRegistrationCertificateRenewalResult(certificate.getId(), renewalVersion.getId(),
                renewalSnapshot.getId(), file.getId(), STATUS_ACTIVE, STATUS_PENDING, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateRenewalResult voidPendingCandidate(
            Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
            Long certificateId, Integer expectedRowVersion, Long pendingVersionId, String voidReason) {
        validateEventInput(tenantId, actorId, idempotencyKey, requestTraceId);
        if (isBlank(voidReason)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CANDIDATE_VOID_REASON_REQUIRED);
        }
        LifecycleEvent existing = findEvent(tenantId, idempotencyKey);
        if (existing != null) {
            RenewalEventDetail detail = parseDetail(existing);
            if (!Objects.equals("CANDIDATE_VOIDED", existing.eventType())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
            }
            return new DccRegistrationCertificateRenewalResult(
                    detail.certificateId(), detail.targetVersionId(), detail.targetSnapshotId(),
                    detail.businessFileId(), STATUS_ACTIVE, STATUS_VOIDED, true);
        }
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(certificateId);
        if (certificate == null || !Objects.equals(certificate.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        if (!STATUS_ACTIVE.equals(certificate.getStatus())
                || !Objects.equals(certificate.getPendingVersionId(), pendingVersionId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }
        DccRegistrationCertificateVersionDO pending = versionMapper.selectById(pendingVersionId);
        if (pending == null || !Objects.equals(pending.getTenantId(), tenantId)
                || !Objects.equals(pending.getCertificateId(), certificateId)
                || !VERSION_TYPE_RENEWAL.equals(pending.getVersionType())
                || !STATUS_PENDING.equals(pending.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }
        List<DccRegistrationCertificateSnapshotDO> snapshots =
                snapshotMapper.selectListByVersionId(pendingVersionId);
        DccRegistrationCertificateSnapshotDO snapshot = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1);
        DccRegistrationCertificateFileDO file = selectRenewalFile(tenantId, pendingVersionId);

        requireSingle(versionMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getId, pendingVersionId)
                        .eq(DccRegistrationCertificateVersionDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateVersionDO::getStatus, STATUS_PENDING)
                        .set(DccRegistrationCertificateVersionDO::getStatus, STATUS_VOIDED)
                        .set(DccRegistrationCertificateVersionDO::getVoidedAt, businessClock.now())
                        .set(DccRegistrationCertificateVersionDO::getVoidedBy, actorId)
                        .set(DccRegistrationCertificateVersionDO::getVoidReason, voidReason.trim())),
                REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        requireSingle(certificateMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, certificateId)
                        .eq(DccRegistrationCertificateDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateDO::getStatus, STATUS_ACTIVE)
                        .eq(DccRegistrationCertificateDO::getPendingVersionId, pendingVersionId)
                        .eq(DccRegistrationCertificateDO::getRowVersion, expectedRowVersion)
                        .set(DccRegistrationCertificateDO::getPendingVersionId, null)
                        .setSql("row_version = row_version + 1")),
                REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        if (file != null) {
            requireSingle(fileMapper.update(null,
                    new LambdaUpdateWrapper<DccRegistrationCertificateFileDO>()
                            .eq(DccRegistrationCertificateFileDO::getId, file.getId())
                            .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                            .eq(DccRegistrationCertificateFileDO::getOwnerId, pendingVersionId)
                            .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND)
                            .set(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_VOIDED)),
                    REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
        insertLifecycleEvent(tenantId, certificate.getOwnerCompanyId(), certificateId,
                certificate.getCurrentVersionId(), pendingVersionId, certificate.getCurrentSnapshotId(),
                snapshot == null ? null : snapshot.getId(), idempotencyKey, "CANDIDATE_VOIDED",
                expectedRowVersion, snapshot == null ? null : snapshot.getRevisionNo(), actorId,
                new RenewalEventDetail(voidPayloadHash(certificateId, expectedRowVersion, pendingVersionId, voidReason),
                        certificateId, pendingVersionId, snapshot == null ? null : snapshot.getId(),
                        file == null ? null : file.getId(), true));
        return new DccRegistrationCertificateRenewalResult(certificateId, pendingVersionId,
                snapshot == null ? null : snapshot.getId(), file == null ? null : file.getId(),
                STATUS_ACTIVE, STATUS_VOIDED, true);
    }

    public boolean isRenewalUploadMissing(Long tenantId, Long certificateId) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(certificateId);
        if (certificate == null || !Objects.equals(certificate.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        if (certificate.getPendingVersionId() != null) {
            return false;
        }
        Integer stagedApprovalCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_access_request r
                  JOIN dcc_registration_certificate_access_request_file rf
                    ON rf.tenant_id = r.tenant_id
                   AND rf.request_id = r.id
                   AND rf.deleted = 0
                   AND rf.file_kind = ?
                   AND rf.status = ?
                  JOIN dcc_registration_certificate_file f
                    ON f.tenant_id = r.tenant_id
                   AND f.id = rf.business_file_id
                   AND f.deleted = 0
                   AND f.owner_type = ?
                   AND f.file_kind = ?
                   AND f.status = ?
                 WHERE r.tenant_id = ?
                   AND r.certificate_id = ?
                   AND r.request_type = ?
                   AND r.status IN ('SUBMITTED', 'BPM_BOUND')
                   AND r.deleted = 0
                """, Integer.class, FILE_KIND_REGISTRATION_CERTIFICATE, REQUEST_FILE_STATUS_REQUESTED,
                FILE_OWNER_VERSION, FILE_KIND_REGISTRATION_CERTIFICATE, FILE_STATUS_STAGED,
                tenantId, certificateId, REQUEST_TYPE_UPLOAD_CERTIFICATE);
        return stagedApprovalCount == null || stagedApprovalCount == 0;
    }

    private DccRegistrationCertificateDO requireActiveCertificate(DccRegistrationCertificateRenewalCommand command) {
        return requireActiveCertificate(command, certificateMapper.selectById(command.certificateId()));
    }

    private DccRegistrationCertificateDO requireActiveCertificate(
            DccRegistrationCertificateRenewalCommand command, DccRegistrationCertificateDO certificate) {
        if (certificate == null || !Objects.equals(certificate.getTenantId(), command.tenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        if (!STATUS_ACTIVE.equals(certificate.getStatus()) || certificate.getCurrentVersionId() == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_STATUS_INVALID);
        }
        if (certificate.getPendingVersionId() != null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }
        if (!Objects.equals(certificate.getRowVersion(), command.expectedRowVersion())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        }
        return certificate;
    }

    private DccRegistrationCertificateVersionDO requireCurrentVersion(DccRegistrationCertificateDO certificate,
                                                                     Long expectedCurrentVersionId) {
        if (!Objects.equals(certificate.getCurrentVersionId(), expectedCurrentVersionId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(expectedCurrentVersionId);
        if (version == null || !Objects.equals(version.getTenantId(), certificate.getTenantId())
                || !Objects.equals(version.getCertificateId(), certificate.getId())
                || !STATUS_CURRENT.equals(version.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        return version;
    }

    private DccRegistrationCertificateSnapshotDO requireSnapshot(Long snapshotId, Long currentVersionId) {
        DccRegistrationCertificateSnapshotDO snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null || !Objects.equals(snapshot.getVersionId(), currentVersionId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        return snapshot;
    }

    private DccRegistrationCertificateFileDO requireStagedRenewalFile(Long tenantId, Long currentVersionId,
                                                                      Long businessFileId) {
        if (businessFileId == null || businessFileId <= 0) {
            List<DccRegistrationCertificateFileDO> candidates = fileMapper.selectList(
                    new LambdaQueryWrapperX<DccRegistrationCertificateFileDO>()
                            .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                            .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_VERSION)
                            .eq(DccRegistrationCertificateFileDO::getOwnerId, currentVersionId)
                            .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                            .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_STAGED));
            if (candidates.isEmpty()) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
            }
            if (candidates.size() > 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
            }
            return candidates.get(0);
        }
        DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
        if (file == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        if (!Objects.equals(file.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH);
        }
        if (!FILE_OWNER_VERSION.equals(file.getOwnerType()) || !Objects.equals(file.getOwnerId(), currentVersionId)
                || !FILE_KIND_REGISTRATION_CERTIFICATE.equals(file.getFileKind())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
        if (!FILE_STATUS_STAGED.equals(file.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);
        }
        return file;
    }

    private void validateRenewalDates(LocalDate firstObtainedDate, LocalDate approvalDate,
                                      LocalDate effectiveDate, LocalDate expiryDate) {
        if (approvalDate == null || effectiveDate == null || expiryDate == null
                || firstObtainedDate != null && firstObtainedDate.isAfter(approvalDate)
                || approvalDate.isAfter(effectiveDate)
                || !effectiveDate.isBefore(expiryDate)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID);
        }
        if (approvalDate.isAfter(businessClock.businessDate())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID);
        }
    }

    private DccRegistrationCertificateSnapshotDO copySnapshot(DccRegistrationCertificateSnapshotDO source,
                                                              Long renewalVersionId) {
        DccRegistrationCertificateSnapshotDO copy = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(renewalVersionId)
                .revisionNo(1)
                .sourceChangeId(null)
                .productName(source.getProductName())
                .registrantName(source.getRegistrantName())
                .modelSpecification(source.getModelSpecification())
                .structureComposition(source.getStructureComposition())
                .intendedUse(source.getIntendedUse())
                .technicalRequirements(source.getTechnicalRequirements())
                .residenceAddress(source.getResidenceAddress())
                .productionAddress(source.getProductionAddress())
                .entrustedProduction(source.getEntrustedProduction())
                .selfProduction(source.getSelfProduction())
                .entrustedEnterprisesJson(source.getEntrustedEnterprisesJson())
                .effectiveAt(source.getEffectiveAt())
                .build();
        copy.setTenantId(source.getTenantId());
        return copy;
    }

    private void registerPlatformCandidate(DccRegistrationCertificateRenewalCommand command,
                                           DccRegistrationCertificateDO certificate,
                                           DccRegistrationCertificateVersionDO currentVersion,
                                           DccRegistrationCertificateVersionDO renewalVersion) {
        ControlledContentKey key = ControlledContentKey.of(command.tenantId(), DCC_REGISTRATION_CERTIFICATE,
                String.valueOf(certificate.getId()));
        try {
            projectionService.registerReadyCandidate(key,
                    ControlledContentProjectionSnapshot.of(key, currentVersion.getId(), null),
                    ControlledContentProjectionSnapshot.of(key, currentVersion.getId(), renewalVersion.getId()),
                    certificate.getId(), renewalVersion.getId(), String.valueOf(renewalVersion.getVersionNo()),
                    STATUS_PENDING, command.actorId(), "注册证延续候选版本等待生效日期");
        } catch (RuntimeException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
            mapped.initCause(exception);
            throw mapped;
        }
    }

    private void insertLifecycleEvent(Long tenantId, Long ownerCompanyId, Long certificateId,
                                      Long sourceVersionId, Long targetVersionId, Long sourceSnapshotId,
                                      Long targetSnapshotId, String eventKey, String eventType,
                                      Integer baselineRowVersion, Integer baselineSnapshotRevision,
                                      Long actorId, RenewalEventDetail detail) {
        Integer nextSequence = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(event_sequence), 0) + 1
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND certificate_id = ?
                """, Integer.class, tenantId, certificateId);
        try {
            int affected = jdbcTemplate.update("""
                    INSERT INTO dcc_registration_certificate_lifecycle_event
                      (tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                       source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                       baseline_row_version, baseline_snapshot_revision, actor_id, detail_json, occurred_at, creator)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, tenantId, ownerCompanyId, certificateId, sourceVersionId, targetVersionId,
                    sourceSnapshotId, targetSnapshotId, eventKey, eventType, nextSequence,
                    baselineRowVersion, baselineSnapshotRevision, actorId, JsonUtils.toJsonString(detail),
                    businessClock.now(), String.valueOf(actorId));
            requireSingle(affected, REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        }
    }

    private LifecycleEvent findEvent(Long tenantId, String eventKey) {
        List<LifecycleEvent> events = jdbcTemplate.query("""
                SELECT event_type, target_version_id, target_snapshot_id, detail_json
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND event_key = ?
                """, (rs, rowNum) -> new LifecycleEvent(
                rs.getString("event_type"),
                rs.getLong("target_version_id"),
                rs.getLong("target_snapshot_id"),
                rs.getString("detail_json")), tenantId, eventKey);
        return events.isEmpty() ? null : events.get(0);
    }

    private DccRegistrationCertificateRenewalResult replayUpload(
            DccRegistrationCertificateRenewalCommand command, String payloadHash, LifecycleEvent event) {
        RenewalEventDetail detail = parseDetail(event);
        if (!Objects.equals("RENEWAL_UPLOADED", event.eventType())
                || !Objects.equals(payloadHash, detail.payloadHash())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(detail.certificateId());
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(detail.targetVersionId());
        return new DccRegistrationCertificateRenewalResult(detail.certificateId(), detail.targetVersionId(),
                detail.targetSnapshotId(), detail.businessFileId(),
                certificate == null ? STATUS_ACTIVE : certificate.getStatus(),
                version == null ? STATUS_PENDING : version.getStatus(),
                false);
    }

    private RenewalEventDetail parseDetail(LifecycleEvent event) {
        return JsonUtils.parseObject(event.detailJson(), RenewalEventDetail.class);
    }

    private DccRegistrationCertificateFileDO selectRenewalFile(Long tenantId, Long pendingVersionId) {
        List<DccRegistrationCertificateFileDO> files = fileMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DccRegistrationCertificateFileDO>()
                        .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_VERSION)
                        .eq(DccRegistrationCertificateFileDO::getOwnerId, pendingVersionId)
                        .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                        .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND));
        if (files.size() > 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
        return files.isEmpty() ? null : files.get(0);
    }

    private DccRegistrationCertificateRenewalCommand candidateCommand(
            DccRegistrationCertificateRenewalSubmitCommand command, Long businessFileId) {
        return new DccRegistrationCertificateRenewalCommand(
                command.tenantId(), command.actorId(), command.idempotencyKey(), command.requestTraceId(),
                command.certificateId(), command.expectedRowVersion(), command.currentVersionId(),
                businessFileId, command.approvalDate(), command.effectiveDate(), command.expiryDate(),
                command.categoryChanged(), command.certificateNo(), command.classification());
    }

    private DccRegistrationCertificateRenewalSubmitResult replaySubmit(
            DccRegistrationCertificateAccessRequestDO request, String payloadHash) {
        if (!REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        RenewalRequestDetail detail = parseRenewalRequestDetail(request);
        if (!Objects.equals(payloadHash, detail.payloadHash())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return new DccRegistrationCertificateRenewalSubmitResult(
                request.getId(), request.getCertificateId(), detail.businessFileId(), request.getStatus());
    }

    private DccRegistrationCertificateAccessRequestDO requireRenewalRequest(Long tenantId, Long requestId) {
        if (tenantId == null || tenantId <= 0 || requestId == null || requestId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(requestId);
        if (request == null || !Objects.equals(tenantId, request.getTenantId())
                || !REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return request;
    }

    private void ensureNoOpenRenewalApproval(Long tenantId, Long certificateId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_access_request
                 WHERE tenant_id = ?
                   AND certificate_id = ?
                   AND request_type = ?
                   AND status IN ('SUBMITTED', 'BPM_BOUND')
                   AND deleted = 0
                """, Integer.class, tenantId, certificateId, REQUEST_TYPE_UPLOAD_CERTIFICATE);
        if (count != null && count > 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }
    }

    private void markRenewalRequestFiles(Long tenantId, Long requestId, String status) {
        requireSingle(requestFileMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateAccessRequestFileDO>()
                        .eq(DccRegistrationCertificateAccessRequestFileDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateAccessRequestFileDO::getRequestId, requestId)
                        .eq(DccRegistrationCertificateAccessRequestFileDO::getFileKind,
                                FILE_KIND_REGISTRATION_CERTIFICATE)
                        .eq(DccRegistrationCertificateAccessRequestFileDO::getStatus,
                                REQUEST_FILE_STATUS_REQUESTED)
                        .set(DccRegistrationCertificateAccessRequestFileDO::getStatus, status)),
                REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
    }

    private void voidStagedRenewalFile(Long tenantId, Long businessFileId, Long actorId, String reason) {
        if (businessFileId == null || businessFileId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        requireSingle(fileMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateFileDO>()
                        .eq(DccRegistrationCertificateFileDO::getId, businessFileId)
                        .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_VERSION)
                        .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                        .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_STAGED)
                        .set(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_VOIDED)
                        .set(DccRegistrationCertificateFileDO::getBoundAt, businessClock.now())
                        .set(DccRegistrationCertificateFileDO::getBoundBy, actorId)),
                REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
    }

    private RenewalUploadFile requireUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        String originalName = normalize(file.getOriginalFilename());
        String mimeType = normalize(file.getContentType());
        if (isBlank(originalName) || isBlank(mimeType) || file.getSize() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
            mapped.initCause(exception);
            throw mapped;
        }
        if (content.length == 0 || content.length != file.getSize()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
        return new RenewalUploadFile(originalName, mimeType, file.getSize(), content, sha256(content));
    }

    private Map<String, Object> submitDetail(
            DccRegistrationCertificateRenewalSubmitCommand command, Long businessFileId,
            String payloadHash, RenewalUploadFile uploadFile, String certificateNo,
            String classification, String productName, String ownerCompanyName) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("payloadHash", payloadHash);
        detail.put("operation", VERSION_TYPE_RENEWAL);
        detail.put("currentVersionId", command.currentVersionId());
        detail.put("expectedRowVersion", command.expectedRowVersion());
        detail.put("businessFileId", businessFileId);
        detail.put("approvalDate", command.approvalDate().toString());
        detail.put("effectiveDate", command.effectiveDate().toString());
        detail.put("expiryDate", command.expiryDate().toString());
        detail.put("categoryChanged", Boolean.TRUE.equals(command.categoryChanged()));
        detail.put("certificateNo", certificateNo);
        detail.put("classification", classification);
        detail.put("productName", productName);
        detail.put("ownerCompanyName", ownerCompanyName);
        if (Boolean.TRUE.equals(command.categoryChanged())) {
            detail.put("certificateNo", normalize(command.certificateNo()));
            detail.put("classification", normalize(command.classification()));
        }
        detail.put("fileSha256", uploadFile.sha256());
        detail.put("originalName", uploadFile.originalName());
        detail.put("fileSize", uploadFile.fileSize());
        return detail;
    }

    private String resolveOwnerCompanyName(Long tenantId, Long ownerCompanyId) {
        if (ownerCompanyId == null || ownerCompanyId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        List<MdmEnterpriseRespDTO> companies = enterpriseApi.getEnabledEnterprises(
                List.of(ownerCompanyId), OWNED_COMPANY);
        if (companies == null || companies.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        MdmEnterpriseRespDTO company = companies.get(0);
        if (company == null || !Objects.equals(tenantId, company.getTenantId())
                || !Objects.equals(ownerCompanyId, company.getId())
                || !MdmEnterpriseTypeEnum.OWNED_COMPANY.getType().equals(company.getType())
                || isBlank(company.getName())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        return company.getName().trim();
    }

    private static String requireSummaryText(String value) {
        if (isBlank(value)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        return value.trim();
    }

    private RenewalRequestDetail parseRenewalRequestDetail(DccRegistrationCertificateAccessRequestDO request) {
        Map<?, ?> parsed = isBlank(request.getDetailJson())
                ? Map.of() : JsonUtils.parseObject(request.getDetailJson(), Map.class);
        if (parsed == null || !VERSION_TYPE_RENEWAL.equals(String.valueOf(parsed.get("operation")))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return new RenewalRequestDetail(
                requireDetailText(parsed, "payloadHash"),
                requireDetailLong(parsed, "currentVersionId"),
                requireDetailInteger(parsed, "expectedRowVersion"),
                requireDetailLong(parsed, "businessFileId"),
                LocalDate.parse(requireDetailText(parsed, "approvalDate")),
                LocalDate.parse(requireDetailText(parsed, "effectiveDate")),
                LocalDate.parse(requireDetailText(parsed, "expiryDate")),
                requireDetailBoolean(parsed, "categoryChanged"),
                optionalDetailText(parsed, "certificateNo"),
                optionalDetailText(parsed, "classification"));
    }

    private void validateEventInput(Long tenantId, Long actorId, String eventKey, String requestTraceId) {
        if (tenantId == null || tenantId <= 0 || actorId == null || actorId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        if (isBlank(eventKey)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED);
        }
        if (isBlank(requestTraceId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        }
    }

    private static String uploadPayloadHash(DccRegistrationCertificateRenewalCommand command) {
        return sha256("UPLOAD|" + command.certificateId() + "|" + command.expectedRowVersion()
                + "|" + command.currentVersionId() + "|" + command.businessFileId()
                + "|" + command.approvalDate() + "|" + command.effectiveDate() + "|" + command.expiryDate()
                + "|" + Boolean.TRUE.equals(command.categoryChanged())
                + "|" + normalize(command.certificateNo()) + "|" + normalize(command.classification()));
    }

    private static String submitPayloadHash(
            DccRegistrationCertificateRenewalSubmitCommand command, RenewalUploadFile uploadFile) {
        return sha256("SUBMIT|" + command.certificateId() + "|" + command.expectedRowVersion()
                + "|" + command.currentVersionId() + "|" + command.approvalDate()
                + "|" + command.effectiveDate() + "|" + command.expiryDate()
                + "|" + Boolean.TRUE.equals(command.categoryChanged())
                + "|" + normalize(command.certificateNo()) + "|" + normalize(command.classification())
                + "|" + uploadFile.originalName() + "|" + uploadFile.mimeType()
                + "|" + uploadFile.fileSize() + "|" + uploadFile.sha256());
    }

    private static boolean requireCategoryChanged(Boolean categoryChanged) {
        if (categoryChanged == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED);
        }
        return Boolean.TRUE.equals(categoryChanged);
    }

    private static String resolveRenewalCertificateNo(Boolean categoryChangedValue, String certificateNo,
                                                      DccRegistrationCertificateVersionDO currentVersion) {
        boolean categoryChanged = requireCategoryChanged(categoryChangedValue);
        String requestedCertificateNo = normalize(certificateNo);
        if (!categoryChanged) {
            if (!isBlank(requestedCertificateNo)) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN);
            }
            return currentVersion.getCertificateNo();
        }
        if (isBlank(requestedCertificateNo) || requestedCertificateNo.length() > MAX_CERTIFICATE_NO_LENGTH) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED);
        }
        return requestedCertificateNo;
    }

    private static String resolveRenewalClassification(Boolean categoryChangedValue, String classification,
                                                       DccRegistrationCertificateVersionDO currentVersion) {
        boolean categoryChanged = requireCategoryChanged(categoryChangedValue);
        String requestedClassification = normalize(classification);
        if (!categoryChanged) {
            if (!isBlank(requestedClassification)) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN);
            }
            return currentVersion.getClassification();
        }
        if (isBlank(requestedClassification) || requestedClassification.length() > MAX_CLASSIFICATION_LENGTH) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED);
        }
        return requestedClassification;
    }

    private static String voidPayloadHash(Long certificateId, Integer expectedRowVersion,
                                          Long pendingVersionId, String voidReason) {
        return sha256("VOID|" + certificateId + "|" + expectedRowVersion + "|" + pendingVersionId
                + "|" + normalize(voidReason));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        }
    }

    private static String requireDetailText(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value == null || isBlank(String.valueOf(value))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return String.valueOf(value).trim();
    }

    private static Long requireDetailLong(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            long result = number.longValue();
            if (result > 0) {
                return result;
            }
        } else if (value != null && String.valueOf(value).matches("[1-9]\\d*")) {
            return Long.valueOf(String.valueOf(value));
        }
        throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
    }

    private static Integer requireDetailInteger(Map<?, ?> values, String key) {
        Long value = requireDetailLong(values, key);
        if (value > Integer.MAX_VALUE) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return value.intValue();
    }

    private static Boolean requireDetailBoolean(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null && ("true".equalsIgnoreCase(String.valueOf(value))
                || "false".equalsIgnoreCase(String.valueOf(value)))) {
            return Boolean.valueOf(String.valueOf(value));
        }
        throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
    }

    private static String optionalDetailText(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? null : normalize(String.valueOf(value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void requireSingle(int affected, ErrorCode errorCode) {
        if (affected != 1) {
            throw new ServiceException(errorCode);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }

    private record LifecycleEvent(
            String eventType,
            Long targetVersionId,
            Long targetSnapshotId,
            String detailJson) {
    }

    private record RenewalEventDetail(
            String payloadHash,
            Long certificateId,
            Long targetVersionId,
            Long targetSnapshotId,
            Long businessFileId,
            Boolean renewalUploadMissing) {
    }

    private record RenewalUploadFile(
            String originalName,
            String mimeType,
            long fileSize,
            byte[] content,
            String sha256) {
    }

    private record RenewalRequestDetail(
            String payloadHash,
            Long currentVersionId,
            Integer expectedRowVersion,
            Long businessFileId,
            LocalDate approvalDate,
            LocalDate effectiveDate,
            LocalDate expiryDate,
            Boolean categoryChanged,
            String certificateNo,
            String classification) {
    }
}
