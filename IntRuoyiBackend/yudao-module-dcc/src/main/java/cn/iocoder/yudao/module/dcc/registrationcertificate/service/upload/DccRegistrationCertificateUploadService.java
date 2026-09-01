package cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftData;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandMutex;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_KEY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_STATUS_INVALID;

@Service
public class DccRegistrationCertificateUploadService {

    private static final String REQUEST_TYPE_UPLOAD_CERTIFICATE = "UPLOAD_CERTIFICATE";
    private static final String REQUEST_OPERATION_UPLOAD_CERTIFICATE = "UPLOAD_CERTIFICATE";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String FILE_OWNER_VERSION = "VERSION";
    private static final String FILE_KIND_REGISTRATION_CERTIFICATE = "REGISTRATION_CERTIFICATE";
    private static final String FILE_STATUS_STAGED = "STAGED";
    private static final String FILE_STATUS_BOUND = "BOUND";
    private static final String REQUEST_FILE_STATUS_REQUESTED = "REQUESTED";
    private static final String REQUEST_FILE_STATUS_APPROVED = "APPROVED";
    private static final String REQUEST_FILE_STATUS_REJECTED = "REJECTED";
    private static final String UPLOAD_REQUEST_PURPOSE = "上传注册证，待注册部经理审批";
    private static final Set<String> OWNED_COMPANY = Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
    private static final Set<String> ENTRUSTED_PARTY = Set.of(MdmEnterpriseTypeEnum.ENTRUSTED_PARTY.getType());
    private static final int OWNER_COMPANY_CANDIDATE_LIMIT = 20;
    private static final int ENTRUSTED_ENTERPRISE_CANDIDATE_LIMIT = 20;

    private final DccRegistrationCertificateCommandMutex commandMutex;
    private final DccRegistrationCertificateCommandService commandService;
    private final DccRegistrationCertificateAccessRequestMapper requestMapper;
    private final DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    private final DccRegistrationCertificateFileMapper fileMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final FileService fileService;
    private final DccProjectCodeService projectCodeService;
    private final MdmCompanyScopeApi companyScopeApi;
    private final MdmEnterpriseApi enterpriseApi;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;

    public DccRegistrationCertificateUploadService(
            DccRegistrationCertificateCommandMutex commandMutex,
            DccRegistrationCertificateCommandService commandService,
            DccRegistrationCertificateAccessRequestMapper requestMapper,
            DccRegistrationCertificateAccessRequestFileMapper requestFileMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            FileService fileService,
            DccProjectCodeService projectCodeService,
            MdmCompanyScopeApi companyScopeApi,
            MdmEnterpriseApi enterpriseApi,
            DccRegistrationCertificateBusinessClock businessClock,
            DccRegistrationCertificateBusinessEventNotifier businessEventNotifier) {
        this.commandMutex = require(commandMutex, "commandMutex");
        this.commandService = require(commandService, "commandService");
        this.requestMapper = require(requestMapper, "requestMapper");
        this.requestFileMapper = require(requestFileMapper, "requestFileMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.fileService = require(fileService, "fileService");
        this.projectCodeService = require(projectCodeService, "projectCodeService");
        this.companyScopeApi = require(companyScopeApi, "companyScopeApi");
        this.enterpriseApi = require(enterpriseApi, "enterpriseApi");
        this.businessClock = require(businessClock, "businessClock");
        this.businessEventNotifier = require(businessEventNotifier, "businessEventNotifier");
    }

    public List<MdmEnterpriseRespDTO> listEntrustedEnterprises(Long tenantId, String keyword) {
        if (tenantId == null || tenantId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        List<MdmEnterpriseRespDTO> enterprises = enterpriseApi.listEnabledEnterprises(
                ENTRUSTED_PARTY, normalizeText(keyword), ENTRUSTED_ENTERPRISE_CANDIDATE_LIMIT);
        if (enterprises == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        for (MdmEnterpriseRespDTO enterprise : enterprises) {
            if (enterprise == null || enterprise.getId() == null || enterprise.getId() <= 0
                    || !Objects.equals(tenantId, enterprise.getTenantId())
                    || isBlank(enterprise.getName())
                    || !ENTRUSTED_PARTY.contains(enterprise.getType())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
            }
        }
        return List.copyOf(enterprises);
    }

    public List<MdmEnterpriseRespDTO> listOwnerCompanies(Long tenantId, Long actorId, String keyword) {
        if (tenantId == null || tenantId <= 0 || actorId == null || actorId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        Set<Long> companyIds = companyScopeApi.getEnabledCompanyIdsForUser(actorId);
        if (companyIds == null || companyIds.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        List<MdmEnterpriseRespDTO> enterprises = enterpriseApi.getEnabledEnterprises(companyIds, OWNED_COMPANY);
        if (enterprises == null || enterprises.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        for (MdmEnterpriseRespDTO enterprise : enterprises) {
            if (enterprise == null || enterprise.getId() == null || enterprise.getId() <= 0
                    || !Objects.equals(tenantId, enterprise.getTenantId())
                    || isBlank(enterprise.getName())
                    || !OWNED_COMPANY.contains(enterprise.getType())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
            }
        }
        String normalizedKeyword = normalizeText(keyword);
        return enterprises.stream()
                .filter(item -> matchesOwnerCompanyKeyword(item, normalizedKeyword))
                .sorted(Comparator.comparing(MdmEnterpriseRespDTO::getId))
                .limit(OWNER_COMPANY_CANDIDATE_LIMIT)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateUploadSubmitResult submitUploadForApproval(
            Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
            DccRegistrationCertificateUploadCommand command) {
        validateEventInput(tenantId, actorId, idempotencyKey, requestTraceId);
        String normalizedKey = normalizeText(idempotencyKey);
        return commandMutex.execute(tenantId + ":" + normalizedKey, () -> submitInternal(
                tenantId, actorId, normalizedKey, requestTraceId, command));
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveUploadRequest(
            Long tenantId, Long approverId, Long requestId, String approvalKey) {
        DccRegistrationCertificateAccessRequestDO request = requireUploadRequest(tenantId, requestId);
        UploadRequestDetail detail = parseUploadRequestDetail(request);
        DccRegistrationCertificateAccessRequestFileDO requestFile = requireSingleRequestFile(tenantId, requestId);
        commandService.formalizeApprovedUpload(tenantId, approverId, requireRequesterUserId(request),
                approvalKey, approvalKey, request.getCertificateId(), detail.draftRowVersion(),
                detail.draftSnapshotRevision(), requestFile.getBusinessFileId());
        requestFile.setStatus(REQUEST_FILE_STATUS_APPROVED);
        requireUpdated(requestFileMapper.updateById(requestFile));
        DccRegistrationCertificateVersionDO version = requireFormalizedVersion(
                tenantId, request.getCertificateId(), requestFile.getBusinessFileId());
        businessEventNotifier.notifyNewCertificateFormalized(
                tenantId, request.getOwnerCompanyId(), request.getCertificateId(), version.getId(),
                approverId, approvalKey, version.getCertificateNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectUploadRequest(
            Long tenantId, Long actorId, Long requestId, String approvalKey, String reason) {
        DccRegistrationCertificateAccessRequestDO request = requireUploadRequest(tenantId, requestId);
        UploadRequestDetail detail = parseUploadRequestDetail(request);
        DccRegistrationCertificateAccessRequestFileDO requestFile = requireSingleRequestFile(tenantId, requestId);
        commandService.deleteDraft(tenantId, actorId, approvalKey, approvalKey, request.getCertificateId(),
                detail.draftRowVersion(), detail.draftSnapshotRevision());
        requestFile.setStatus(REQUEST_FILE_STATUS_REJECTED);
        requireUpdated(requestFileMapper.updateById(requestFile));
    }

    private DccRegistrationCertificateUploadSubmitResult submitInternal(
            Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
            DccRegistrationCertificateUploadCommand command) {
        List<Long> entrustedEnterpriseIds = requireUploadProductionRelation(command);
        UploadFile uploadFile = requireUploadFile(command.file());
        MdmEnterpriseRespDTO ownerCompany = resolveOwnerCompany(tenantId, actorId, command.companyId());
        Long ownerCompanyId = ownerCompany.getId();
        DccProjectCodeDO projectCode = requireProjectCode(tenantId, actorId, command.projectCodeId());
        Long projectCodeId = projectCode == null ? null : projectCode.getId();
        String productName = requireProductName(command.productName());
        Long productMasterId = null;
        String payloadHash = submitPayloadHash(
                command, ownerCompany, productMasterId, projectCodeId, entrustedEnterpriseIds, uploadFile);

        DccRegistrationCertificateAccessRequestDO existing =
                requestMapper.selectByTenantAndRequestKey(tenantId, idempotencyKey);
        if (existing != null) {
            return replaySubmit(existing, payloadHash);
        }

        DccRegistrationCertificateDraftData draft = new DccRegistrationCertificateDraftData(
                ownerCompanyId, productMasterId, productName, projectCodeId,
                command.firstObtainedDate(), trim(command.certificateNo()),
                null, command.effectiveDate(), command.expiryDate(), trim(command.classification()),
                null, null, null, null, null, null, null,
                command.entrustedProduction(), command.selfProduction(), entrustedEnterpriseIds, trim(command.remark()));
        Long certificateId = commandService.createDraft(
                tenantId, actorId, idempotencyKey, requestTraceId, draft);
        DccRegistrationCertificateVersionDO draftVersion = requireDraftVersion(tenantId, certificateId);

        Long infraFileId = fileService.createFileAndReturnId(uploadFile.content(), uploadFile.originalName(),
                "dcc/registration-certificate/upload/" + certificateId, uploadFile.mimeType());
        DccRegistrationCertificateFileDO businessFile = DccRegistrationCertificateFileDO.builder()
                .ownerType(FILE_OWNER_VERSION)
                .ownerId(draftVersion.getId())
                .fileKind(FILE_KIND_REGISTRATION_CERTIFICATE)
                .infraFileId(infraFileId)
                .originalName(uploadFile.originalName())
                .mimeType(uploadFile.mimeType())
                .fileSize(uploadFile.fileSize())
                .sha256(uploadFile.sha256())
                .status(FILE_STATUS_STAGED)
                .build();
        businessFile.setTenantId(tenantId);
        requireUpdated(fileMapper.insert(businessFile));

        DccRegistrationCertificateAccessRequestDO request = DccRegistrationCertificateAccessRequestDO.builder()
                .ownerCompanyId(ownerCompanyId)
                .certificateId(certificateId)
                .requesterUserId(actorId)
                .requestType(REQUEST_TYPE_UPLOAD_CERTIFICATE)
                .requestKey(idempotencyKey)
                .purpose(UPLOAD_REQUEST_PURPOSE)
                .projectCodeId(projectCodeId)
                .status(STATUS_SUBMITTED)
                .requestedAt(businessClock.now())
                .detailJson(JsonUtils.toJsonString(submitDetail(
                        payloadHash, ownerCompanyId, ownerCompany.getName(), productName,
                        productMasterId, projectCodeId, command.certificateNo(), command.classification())))
                .build();
        request.setTenantId(tenantId);
        requireUpdated(requestMapper.insert(request));

        DccRegistrationCertificateAccessRequestFileDO requestFile = DccRegistrationCertificateAccessRequestFileDO.builder()
                .requestId(request.getId())
                .businessFileId(businessFile.getId())
                .fileKind(FILE_KIND_REGISTRATION_CERTIFICATE)
                .downloadRequested(false)
                .status(REQUEST_FILE_STATUS_REQUESTED)
                .detailJson(JsonUtils.toJsonString(Map.of("payloadHash", payloadHash)))
                .build();
        requestFile.setTenantId(tenantId);
        requireUpdated(requestFileMapper.insert(requestFile));
        return new DccRegistrationCertificateUploadSubmitResult(request.getId(), certificateId, businessFile.getId());
    }

    private DccRegistrationCertificateUploadSubmitResult replaySubmit(
            DccRegistrationCertificateAccessRequestDO request, String payloadHash) {
        if (!REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        UploadRequestDetail detail = parseUploadRequestDetail(request);
        if (!Objects.equals(payloadHash, detail.payloadHash())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        DccRegistrationCertificateAccessRequestFileDO requestFile = requireSingleRequestFile(
                request.getTenantId(), request.getId());
        return new DccRegistrationCertificateUploadSubmitResult(
                request.getId(), request.getCertificateId(), requestFile.getBusinessFileId());
    }

    private DccRegistrationCertificateAccessRequestDO requireUploadRequest(Long tenantId, Long requestId) {
        if (tenantId == null || tenantId <= 0 || requestId == null || requestId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(requestId);
        if (request == null || !Objects.equals(tenantId, request.getTenantId())
                || !REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        if (!STATUS_APPROVED.equals(request.getStatus()) && !STATUS_REJECTED.equals(request.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return request;
    }

    private Long requireRequesterUserId(DccRegistrationCertificateAccessRequestDO request) {
        if (request.getRequesterUserId() == null || request.getRequesterUserId() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return request.getRequesterUserId();
    }

    private DccRegistrationCertificateAccessRequestFileDO requireSingleRequestFile(Long tenantId, Long requestId) {
        List<DccRegistrationCertificateAccessRequestFileDO> requestFiles =
                requestFileMapper.selectByRequestId(tenantId, requestId);
        if (requestFiles == null || requestFiles.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return requestFiles.get(0);
    }

    private UploadRequestDetail parseUploadRequestDetail(DccRegistrationCertificateAccessRequestDO request) {
        Map<?, ?> parsed = isBlank(request.getDetailJson())
                ? Map.of() : JsonUtils.parseObject(request.getDetailJson(), Map.class);
        if (parsed == null || !REQUEST_OPERATION_UPLOAD_CERTIFICATE.equals(String.valueOf(parsed.get("operation")))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        Integer draftRowVersion = asInteger(parsed.get("draftRowVersion"));
        Integer draftSnapshotRevision = asInteger(parsed.get("draftSnapshotRevision"));
        if (draftRowVersion == null || draftRowVersion <= 0
                || draftSnapshotRevision == null || draftSnapshotRevision <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        String payloadHash = String.valueOf(parsed.get("payloadHash"));
        if (isBlank(payloadHash)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
        return new UploadRequestDetail(payloadHash, draftRowVersion, draftSnapshotRevision);
    }

    private MdmEnterpriseRespDTO resolveOwnerCompany(Long tenantId, Long actorId, Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        try {
            companyScopeApi.validateUserCompanyAccess(actorId, companyId);
        } catch (ServiceException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        List<MdmEnterpriseRespDTO> enterprises;
        try {
            enterprises = enterpriseApi.getEnabledEnterprises(List.of(companyId), OWNED_COMPANY);
        } catch (ServiceException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        if (enterprises == null || enterprises.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        MdmEnterpriseRespDTO enterprise = enterprises.get(0);
        if (enterprise == null || !Objects.equals(tenantId, enterprise.getTenantId())
                || !Objects.equals(companyId, enterprise.getId())
                || isBlank(enterprise.getName())
                || !OWNED_COMPANY.contains(enterprise.getType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        return enterprise;
    }

    private DccProjectCodeDO requireProjectCode(Long tenantId, Long actorId, Long projectCodeId) {
        if (projectCodeId == null) {
            return null;
        }
        if (projectCodeId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID);
        }
        DccProjectCodeDO projectCode = projectCodeService.getProjectCode(actorId, projectCodeId);
        if (projectCode == null || !Objects.equals(projectCode.getId(), projectCodeId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID);
        }
        if (!Objects.equals(tenantId, projectCode.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH);
        }
        if (!DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED);
        }
        if (projectCode.getProductMasterId() != null && projectCode.getProductMasterId() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH);
        }
        return projectCode;
    }

    private String requireProductName(String productName) {
        String normalizedProductName = trim(productName);
        if (isBlank(normalizedProductName)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCT_REQUIRED);
        }
        return normalizedProductName;
    }

    private DccRegistrationCertificateVersionDO requireDraftVersion(Long tenantId, Long certificateId) {
        List<DccRegistrationCertificateVersionDO> versions = versionMapper.selectList(
                new LambdaQueryWrapperX<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateVersionDO::getCertificateId, certificateId)
                        .eq(DccRegistrationCertificateVersionDO::getStatus, "DRAFT"));
        if (versions == null || versions.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        return versions.get(0);
    }

    private DccRegistrationCertificateVersionDO requireFormalizedVersion(
            Long tenantId, Long certificateId, Long businessFileId) {
        DccRegistrationCertificateFileDO businessFile = fileMapper.selectById(businessFileId);
        if (businessFile == null || !Objects.equals(tenantId, businessFile.getTenantId())
                || !FILE_OWNER_VERSION.equals(businessFile.getOwnerType())
                || businessFile.getOwnerId() == null || businessFile.getOwnerId() <= 0
                || !FILE_KIND_REGISTRATION_CERTIFICATE.equals(businessFile.getFileKind())
                || !FILE_STATUS_BOUND.equals(businessFile.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(businessFile.getOwnerId());
        if (version == null || !Objects.equals(tenantId, version.getTenantId())
                || !Objects.equals(certificateId, version.getCertificateId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        return version;
    }

    private static Map<String, Object> submitDetail(
            String payloadHash, Long ownerCompanyId, String ownerCompanyName, String productName,
            Long productMasterId, Long projectCodeId, String certificateNo, String classification) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operation", REQUEST_OPERATION_UPLOAD_CERTIFICATE);
        detail.put("payloadHash", payloadHash);
        detail.put("draftRowVersion", 1);
        detail.put("draftSnapshotRevision", 1);
        detail.put("ownerCompanyId", ownerCompanyId);
        detail.put("ownerCompanyName", trim(ownerCompanyName));
        detail.put("productName", trim(productName));
        detail.put("certificateNo", trim(certificateNo));
        detail.put("classification", trim(classification));
        detail.put("productMasterId", productMasterId);
        detail.put("projectCodeId", projectCodeId);
        return detail;
    }

    private String submitPayloadHash(
            DccRegistrationCertificateUploadCommand command, MdmEnterpriseRespDTO ownerCompany,
            Long productMasterId, Long projectCodeId, List<Long> entrustedEnterpriseIds, UploadFile uploadFile) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ownerCompanyId", ownerCompany.getId());
        payload.put("ownerCompanyName", trim(ownerCompany.getName()));
        payload.put("productName", trim(command.productName()));
        payload.put("projectCodeId", projectCodeId);
        payload.put("productMasterId", productMasterId);
        payload.put("certificateNo", trim(command.certificateNo()));
        payload.put("firstObtainedDate", command.firstObtainedDate());
        payload.put("effectiveDate", command.effectiveDate());
        payload.put("expiryDate", command.expiryDate());
        payload.put("classification", trim(command.classification()));
        payload.put("entrustedProduction", command.entrustedProduction());
        payload.put("selfProduction", command.selfProduction());
        payload.put("entrustedEnterpriseIds", entrustedEnterpriseIds);
        payload.put("remark", trim(command.remark()));
        payload.put("fileName", uploadFile.originalName());
        payload.put("mimeType", uploadFile.mimeType());
        payload.put("fileSize", uploadFile.fileSize());
        payload.put("fileSha256", uploadFile.sha256());
        return sha256Hex(JsonUtils.toJsonString(payload));
    }

    private static List<Long> requireUploadProductionRelation(DccRegistrationCertificateUploadCommand command) {
        if (command == null || command.entrustedProduction() == null || command.selfProduction() == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        boolean entrustedProduction = Boolean.TRUE.equals(command.entrustedProduction());
        boolean selfProduction = Boolean.TRUE.equals(command.selfProduction());
        if (!entrustedProduction && !selfProduction) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        List<Long> entrustedEnterpriseIds = command.entrustedEnterpriseIds() == null
                ? List.of() : command.entrustedEnterpriseIds();
        if (entrustedEnterpriseIds.stream().anyMatch(id -> id == null || id <= 0)
                || new LinkedHashSet<>(entrustedEnterpriseIds).size() != entrustedEnterpriseIds.size()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        if (entrustedProduction && entrustedEnterpriseIds.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        if (!entrustedProduction && !entrustedEnterpriseIds.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        return List.copyOf(entrustedEnterpriseIds);
    }

    private static UploadFile requireUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        String originalName = trim(file.getOriginalFilename());
        String mimeType = trim(file.getContentType());
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
        return new UploadFile(originalName, mimeType, file.getSize(), content, sha256(content));
    }

    private static void validateEventInput(Long tenantId, Long actorId, String idempotencyKey, String requestTraceId) {
        if (tenantId == null || tenantId <= 0 || actorId == null || actorId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        if (isBlank(idempotencyKey) || idempotencyKey.trim().length() > 256) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_KEY_REQUIRED);
        }
        if (isBlank(requestTraceId) || requestTraceId.trim().length() > 128) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("注册证上传必须启用 SHA-256 算法", exception);
        }
    }

    private static String sha256Hex(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static void requireUpdated(int updated) {
        if (updated != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_CONFLICT);
        }
    }

    private static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean matchesOwnerCompanyKeyword(MdmEnterpriseRespDTO enterprise, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        return trim(enterprise.getName()).contains(keyword)
                || (!isBlank(enterprise.getEnterpriseCode()) && trim(enterprise.getEnterpriseCode()).contains(keyword));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }

    private record UploadFile(String originalName, String mimeType, Long fileSize, byte[] content, String sha256) {
    }

    private record UploadRequestDetail(String payloadHash, Integer draftRowVersion, Integer draftSnapshotRevision) {
    }
}
