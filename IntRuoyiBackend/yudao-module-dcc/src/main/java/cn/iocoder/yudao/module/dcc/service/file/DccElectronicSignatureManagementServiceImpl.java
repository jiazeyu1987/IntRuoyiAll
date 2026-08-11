package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureExportSummaryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureImageRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignaturePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureAuthorizationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureEvidenceRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;

@Service
@Validated
public class DccElectronicSignatureManagementServiceImpl implements DccElectronicSignatureManagementService {

    private static final String STATE_UNAUTHORIZED = "UNAUTHORIZED";
    private static final String STATE_ENABLED = "ENABLED";
    private static final String STATE_DISABLED = "DISABLED";
    private static final String STATE_LOCKED = "LOCKED";
    private static final String STATUS_VALID = "VALID";
    private static final String STATUS_INVALID = "INVALID";
    private static final String STATUS_HISTORICAL_UNBOUND = "HISTORICAL_UNBOUND";
    private static final String COPY_HASH_STATUS_BOUND = "BOUND";
    private static final String COPY_HASH_STATUS_NOT_APPLICABLE = "NOT_APPLICABLE";
    private static final String SIGNATURE_EVIDENCE_EXPORT_CONTENT_TYPE = "application/pdf";
    private static final String SIGNATURE_EVIDENCE_PDF_FONT_PATH = "C:/Windows/Fonts/simhei.ttf";
    private static final String SIGNATURE_EVIDENCE_SYSTEM_STATEMENT =
            "本系统电子签名证据；可通过签名 ID 与证据哈希在系统内复核。本系统认证并可校验。";
    private static final DateTimeFormatter EXPORT_FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> CANONICAL_PAYLOAD_FIELD_ORDER = List.of(
            "payloadVersion", "hashAlgorithm", "keyVersion", "tenantId", "controlledFileId", "fileNumber",
            "revisionId", "versionNo", "sourceFileHash", "controlledCopyHashStatus", "controlledCopyHash",
            "processInstanceId", "taskId", "taskActionResult", "meaningCode", "signerUserId", "signerUsername",
            "signerNickname", "signerDeptId", "signerDeptName", "signerPostNames", "signerRoleNames",
            "signaturePurpose", "authorizationBasis", "authenticationMethod", "signedAt", "reasonText");
    private static final List<String> CANONICAL_PAYLOAD_FIELD_ORDER_V3_IMAGE = List.of(
            "payloadVersion", "hashAlgorithm", "keyVersion", "tenantId", "controlledFileId", "fileNumber",
            "revisionId", "versionNo", "sourceFileHash", "controlledCopyHashStatus", "controlledCopyHash",
            "signatureImageId", "signatureImageVersionNo", "signatureImageFileId", "signatureImageSha256",
            "signatureImageContentType", "signatureImageFileSize", "signatureImageStatusSnapshot",
            "signatureImageVerifiedStatus", "processInstanceId", "taskId", "taskActionResult", "meaningCode",
            "signerUserId", "signerUsername", "signerNickname", "signerDeptId", "signerDeptName",
            "signerPostNames", "signerRoleNames", "signaturePurpose", "authorizationBasis",
            "authenticationMethod", "signedAt", "reasonText");

    @Resource
    private DccControlledFileSignatureMapper signatureMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccElectronicSignatureAuthorizationMapper authorizationMapper;
    @Resource
    private DccElectronicSignatureAuthorizationAuditMapper authorizationAuditMapper;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private DeptService deptService;
    @Resource
    private DccElectronicSignatureAuthorizationService authorizationService;
    @Resource
    private DccElectronicSignatureAuthorizationAuditService authorizationAuditService;
    @Resource
    private DccSignatureEvidenceProperties signatureEvidenceProperties;
    @Resource
    private FileService fileService;
    @Resource
    private DccElectronicSignatureImageService signatureImageService;
    @Resource
    private DccControlledFileSignatureBindingService signatureBindingService;

    @Override
    public PageResult<DccElectronicSignatureRespVO> getSignaturePage(DccElectronicSignaturePageReqVO reqVO) {
        normalizeSignaturePageFilters(reqVO);
        applyFileNumberFilter(reqVO);
        PageResult<DccControlledFileSignatureDO> pageResult = signatureMapper.selectPage(reqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> controlledFileIds = CollectionUtils.convertSet(pageResult.getList(),
                DccControlledFileSignatureDO::getControlledFileId);
        Set<Long> actorIds = CollectionUtils.convertSet(pageResult.getList(), DccControlledFileSignatureDO::getActorId);
        Map<Long, DccControlledFileDO> controlledFileMap = CollectionUtils.convertMap(
                controlledFileMapper.selectBatchIds(controlledFileIds), DccControlledFileDO::getId);
        Map<Long, AdminUserDO> actorMap = CollectionUtils.convertMap(
                adminUserService.getUserList(actorIds), AdminUserDO::getId);
        return new PageResult<>(CollectionUtils.convertList(pageResult.getList(), signature ->
                toSignatureRespVO(signature, controlledFileMap.get(signature.getControlledFileId()),
                        actorMap.get(signature.getActorId()))), pageResult.getTotal());
    }

    @Override
    public PageResult<DccElectronicSignatureAuthorizationRespVO> getAuthorizationPage(
            DccElectronicSignatureAuthorizationPageReqVO reqVO) {
        UserPageReqVO userPageReqVO = new UserPageReqVO();
        userPageReqVO.setPageNo(reqVO.getPageNo());
        userPageReqVO.setPageSize(reqVO.getPageSize());
        userPageReqVO.setUsername(reqVO.getUsername());
        userPageReqVO.setMobile(reqVO.getMobile());
        userPageReqVO.setStatus(reqVO.getStatus());
        PageResult<AdminUserDO> userPage = adminUserService.getUserPage(userPageReqVO);
        if (CollUtil.isEmpty(userPage.getList())) {
            return PageResult.empty(userPage.getTotal());
        }
        Set<Long> userIds = CollectionUtils.convertSet(userPage.getList(), AdminUserDO::getId);
        Map<Long, DccElectronicSignatureAuthorizationDO> authorizationMap = CollectionUtils.convertMap(
                authorizationMapper.selectListByUserIds(userIds),
                DccElectronicSignatureAuthorizationDO::getUserId);
        Map<Long, DeptDO> deptMap = getDeptMap(userPage.getList());
        Map<Long, DccElectronicSignatureAuthorizationAuditDO> latestAuditMap = getLatestAuditMap(userIds);
        Map<Long, String> operatorNameMap = getOperatorNameMap(latestAuditMap.values());
        return new PageResult<>(CollectionUtils.convertList(userPage.getList(), user -> {
            DccElectronicSignatureAuthorizationRespVO respVO = new DccElectronicSignatureAuthorizationRespVO();
            fillAuthorizationRespVO(respVO, user, authorizationMap.get(user.getId()), getDept(deptMap, user.getDeptId()),
                    latestAuditMap.get(user.getId()), operatorNameMap);
            return respVO;
        }), userPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccSignatureAuthorizationRespVO updateAuthorization(Long userId, Boolean enabled,
                                                               Long operatorId, String reason) {
        if (enabled == null || StrUtil.isBlank(reason)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED);
        }
        AdminUserDO user = requireUser(userId);
        authorizationService.updateAuthorization(userId, Boolean.TRUE.equals(enabled), operatorId, reason);
        DccElectronicSignatureAuthorizationDO authorization = authorizationMapper.selectByUserId(userId);
        if (authorization == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        return buildSingleAuthorizationResp(user, authorization);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccSignatureAuthorizationRespVO unlockAuthorization(Long userId, Long operatorId, String reason) {
        if (StrUtil.isBlank(reason)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED);
        }
        AdminUserDO user = requireUser(userId);
        DccElectronicSignatureAuthorizationDO existing = authorizationMapper.selectByUserId(userId);
        if (existing == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED);
        }
        boolean enabled = Boolean.TRUE.equals(existing.getElectronicSignatureEnabled());
        String afterState = enabled ? STATE_ENABLED : STATE_DISABLED;
        DccElectronicSignatureAuthorizationDO update = DccElectronicSignatureAuthorizationDO.builder()
                .id(existing.getId())
                .electronicSignatureEnabled(enabled)
                .authorizationState(afterState)
                .lockedUntil(null)
                .lockReason(null)
                .lastFailureAt(null)
                .failureCount(0)
                .build();
        if (authorizationMapper.updateById(update) <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        authorizationAuditService.recordAuthorizationChange(DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(userId)
                .operatorId(operatorId)
                .beforeState(StrUtil.blankToDefault(existing.getAuthorizationState(), STATE_UNAUTHORIZED))
                .beforeEnabled(Boolean.TRUE.equals(existing.getElectronicSignatureEnabled()))
                .afterState(afterState)
                .afterEnabled(enabled)
                .reason(StrUtil.trim(reason))
                .operatedAt(LocalDateTime.now())
                .build());
        DccElectronicSignatureAuthorizationDO authorization = authorizationMapper.selectByUserId(userId);
        if (authorization == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        return buildSingleAuthorizationResp(user, authorization);
    }

    @Override
    public PageResult<DccElectronicSignatureAuthorizationAuditRespVO> getAuthorizationAuditPage(
            Long userId, DccElectronicSignatureAuthorizationAuditPageReqVO reqVO) {
        PageResult<DccElectronicSignatureAuthorizationAuditDO> pageResult = authorizationAuditMapper.selectPage(
                reqVO, new LambdaQueryWrapperX<DccElectronicSignatureAuthorizationAuditDO>()
                        .eq(DccElectronicSignatureAuthorizationAuditDO::getTargetUserId, userId)
                        .orderByDesc(DccElectronicSignatureAuthorizationAuditDO::getOperatedAt)
                        .orderByDesc(DccElectronicSignatureAuthorizationAuditDO::getId));
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, String> operatorNameMap = getOperatorNameMap(pageResult.getList());
        return new PageResult<>(CollectionUtils.convertList(pageResult.getList(),
                audit -> toAuthorizationAuditRespVO(audit, operatorNameMap.get(audit.getOperatorId()))),
                pageResult.getTotal());
    }

    @Override
    public DccElectronicSignatureImageRespVO getMySignatureImage(Long userId) {
        return signatureImageService.getMySignatureImage(userId);
    }

    @Override
    public DccElectronicSignatureImageRespVO uploadMySignatureImage(Long userId, MultipartFile file,
                                                                    Long operatorId, String reason) {
        return signatureImageService.uploadMySignatureImage(userId, file, operatorId, reason);
    }

    @Override
    public DccElectronicSignatureImageRespVO enableMySignatureImage(Long userId, Long imageId, Long operatorId,
                                                                    String reason) {
        return signatureImageService.enableMySignatureImage(userId, imageId, operatorId, reason);
    }

    @Override
    public DccElectronicSignatureImageRespVO disableMySignatureImage(Long userId, Long operatorId, String reason) {
        return signatureImageService.disableMySignatureImage(userId, operatorId, reason);
    }

    @Override
    public DccSignatureEvidenceRespVO getSignatureEvidenceDetail(Long signatureId) {
        DccControlledFileSignatureDO signature = requireSignature(signatureId);
        DccControlledFileDO controlledFile = requireControlledFile(signature.getControlledFileId());
        DccSignatureEvidenceRespVO respVO = new DccSignatureEvidenceRespVO();
        copySignatureFields(respVO, signature, controlledFile, null);
        respVO.setSignatureId(signature.getId());
        VerificationComputation computation = verifySignature(signature, controlledFile);
        respVO.setCanonicalPayloadFieldOrder(canonicalPayloadFieldOrder(signature));
        respVO.setCanonicalPayload(computation.canonicalPayload());
        respVO.setVerificationStatus(computation.verificationStatus());
        respVO.setVerificationReason(computation.verificationReason());
        applyBindingProjection(respVO, computation.bindingVerification());
        respVO.setVerifiedAt(computation.verifiedAt());
        return respVO;
    }

    @Override
    public DccSignatureVerifyRespVO verifySignatureEvidence(Long signatureId) {
        DccControlledFileSignatureDO signature = requireSignature(signatureId);
        DccControlledFileDO controlledFile = requireControlledFile(signature.getControlledFileId());
        VerificationComputation computation = verifySignature(signature, controlledFile);
        DccSignatureVerifyRespVO respVO = new DccSignatureVerifyRespVO();
        respVO.setSignatureId(signature.getId());
        respVO.setStoredEvidenceHash(signature.getEvidenceHash());
        respVO.setRecomputedEvidenceHash(computation.recomputedEvidenceHash());
        respVO.setEvidenceHashShort(shortHashNullable(signature.getEvidenceHash()));
        respVO.setVerificationStatus(computation.verificationStatus());
        respVO.setVerificationReason(computation.verificationReason());
        applyBindingProjection(respVO, computation.bindingVerification());
        respVO.setVerifiedAt(computation.verifiedAt());
        return respVO;
    }

    @Override
    public DccControlledFileSignatureExportSummaryRespVO getSignatureExportSummary(Long controlledFileId) {
        DccControlledFileDO file = requireControlledFile(controlledFileId);
        List<DccControlledFileSignatureDO> signatures = signatureMapper.selectListByControlledFileId(controlledFileId);
        DccControlledFileSignatureExportSummaryRespVO respVO = new DccControlledFileSignatureExportSummaryRespVO();
        respVO.setControlledFileId(controlledFileId);
        respVO.setRevisionId(file.getId());
        respVO.setVersionNo(file.getVersionNo());
        List<VerificationComputation> computations = CollectionUtils.convertList(signatures,
                signature -> verifySignature(signature, file));
        List<DccControlledFileSignatureExportSummaryRespVO.SignatureItem> items = new ArrayList<>(signatures.size());
        for (int index = 0; index < signatures.size(); index++) {
            items.add(toExportSignatureItem(signatures.get(index), computations.get(index)));
        }
        respVO.setSignatures(items);
        boolean valid = CollUtil.isNotEmpty(signatures)
                && computations.stream().allMatch(computation -> STATUS_VALID.equals(computation.verificationStatus()));
        respVO.setAllRequiredEvidenceValid(valid);
        respVO.setBlockedReason(valid ? "" : (CollUtil.isEmpty(signatures)
                ? "SIGNATURE_EVIDENCE_MISSING" : "SIGNATURE_EVIDENCE_INVALID"));
        return respVO;
    }

    @Override
    public DccSignatureEvidenceExportArtifact exportSignatureEvidence(Long controlledFileId) {
        DccControlledFileDO file = requireControlledFile(controlledFileId);
        List<DccControlledFileSignatureDO> signatures = signatureMapper.selectListByControlledFileId(controlledFileId);
        if (CollUtil.isEmpty(signatures)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED);
        }

        List<DccControlledFileSignatureDO> sortedSignatures = signatures.stream()
                .sorted(Comparator
                        .comparing(DccControlledFileSignatureDO::getSignedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DccControlledFileSignatureDO::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        List<Map<String, Object>> signatureArtifacts = new ArrayList<>(sortedSignatures.size());
        for (DccControlledFileSignatureDO signature : sortedSignatures) {
            VerificationComputation computation = verifySignature(signature, file);
            if (!isExportEvidenceValid(signature, file, computation)) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED);
            }
            signatureArtifacts.add(buildExportSignatureArtifact(signature, computation));
        }

        LocalDateTime exportedAt = LocalDateTime.now();
        return new DccSignatureEvidenceExportArtifact(
                buildExportFileName(file, exportedAt),
                SIGNATURE_EVIDENCE_EXPORT_CONTENT_TYPE,
                renderSignatureEvidencePdf(file, exportedAt, signatureArtifacts));
    }

    private void applyFileNumberFilter(DccElectronicSignaturePageReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getFileNumber())) {
            return;
        }
        Set<Long> ids = controlledFileMapper.selectList(DccControlledFileDO::getFileNumber,
                        StrUtil.trim(reqVO.getFileNumber()))
                .stream()
                .map(DccControlledFileDO::getId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            reqVO.setControlledFileIds(Set.of(-1L));
            return;
        }
        if (reqVO.getControlledFileId() != null && !ids.contains(reqVO.getControlledFileId())) {
            reqVO.setControlledFileIds(Set.of(-1L));
            return;
        }
        reqVO.setControlledFileIds(ids);
    }

    private DccSignatureAuthorizationRespVO buildSingleAuthorizationResp(AdminUserDO user,
                                                                         DccElectronicSignatureAuthorizationDO authorization) {
        Map<Long, DeptDO> deptMap = getDeptMap(List.of(user));
        Map<Long, DccElectronicSignatureAuthorizationAuditDO> latestAuditMap = getLatestAuditMap(Set.of(user.getId()));
        Map<Long, String> operatorNameMap = getOperatorNameMap(latestAuditMap.values());
        DccSignatureAuthorizationRespVO respVO = new DccSignatureAuthorizationRespVO();
        fillAuthorizationRespVO(respVO, user, authorization, getDept(deptMap, user.getDeptId()),
                latestAuditMap.get(user.getId()), operatorNameMap);
        return respVO;
    }

    private AdminUserDO requireUser(Long userId) {
        AdminUserDO user = adminUserService.getUser(userId);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

    private DccControlledFileSignatureDO requireSignature(Long signatureId) {
        DccControlledFileSignatureDO signature = signatureMapper.selectById(signatureId);
        if (signature == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        return signature;
    }

    private DccControlledFileDO requireControlledFile(Long controlledFileId) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        return file;
    }

    private Map<Long, DeptDO> getDeptMap(List<AdminUserDO> users) {
        Set<Long> deptIds = users.stream()
                .map(AdminUserDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        return CollectionUtils.convertMap(deptService.getDeptList(deptIds), DeptDO::getId);
    }

    private DeptDO getDept(Map<Long, DeptDO> deptMap, Long deptId) {
        return deptId == null ? null : deptMap.get(deptId);
    }

    private Map<Long, DccElectronicSignatureAuthorizationAuditDO> getLatestAuditMap(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Map.of();
        }
        List<DccElectronicSignatureAuthorizationAuditDO> audits = authorizationAuditMapper.selectList(
                new LambdaQueryWrapperX<DccElectronicSignatureAuthorizationAuditDO>()
                        .in(DccElectronicSignatureAuthorizationAuditDO::getTargetUserId, userIds)
                        .orderByDesc(DccElectronicSignatureAuthorizationAuditDO::getOperatedAt)
                        .orderByDesc(DccElectronicSignatureAuthorizationAuditDO::getId));
        Map<Long, DccElectronicSignatureAuthorizationAuditDO> result = new LinkedHashMap<>();
        for (DccElectronicSignatureAuthorizationAuditDO audit : audits) {
            result.putIfAbsent(audit.getTargetUserId(), audit);
        }
        return result;
    }

    private Map<Long, String> getOperatorNameMap(Collection<DccElectronicSignatureAuthorizationAuditDO> audits) {
        Set<Long> operatorIds = audits.stream()
                .map(DccElectronicSignatureAuthorizationAuditDO::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (operatorIds.isEmpty()) {
            return Map.of();
        }
        return CollectionUtils.convertMap(adminUserService.getUserList(operatorIds), AdminUserDO::getId,
                this::displayUserName);
    }

    private DccElectronicSignatureRespVO toSignatureRespVO(DccControlledFileSignatureDO signature,
                                                           DccControlledFileDO controlledFile,
                                                           AdminUserDO actor) {
        DccElectronicSignatureRespVO respVO = new DccElectronicSignatureRespVO();
        copySignatureFields(respVO, signature, controlledFile, actor);
        return respVO;
    }

    private void copySignatureFields(DccElectronicSignatureRespVO respVO,
                                     DccControlledFileSignatureDO signature,
                                     DccControlledFileDO controlledFile,
                                     AdminUserDO actor) {
        respVO.setId(signature.getId());
        respVO.setControlledFileId(signature.getControlledFileId());
        respVO.setControlledFileTitle(controlledFile != null ? controlledFile.getTitle() : null);
        respVO.setFileName(controlledFile != null ? controlledFile.getFileName() : null);
        respVO.setControlledFileNumber(controlledFile != null ? controlledFile.getFileNumber() : null);
        respVO.setFileNumber(controlledFile != null ? controlledFile.getFileNumber() : null);
        respVO.setControlledFileStatus(controlledFile != null ? controlledFile.getStatus() : null);
        respVO.setRevisionId(signature.getRevisionId());
        respVO.setVersionNo(signature.getVersionNo());
        respVO.setTaskId(signature.getTaskId());
        respVO.setActorId(signature.getActorId());
        respVO.setSignerUserId(signature.getActorId());
        respVO.setActorUsername(actor != null ? actor.getUsername() : signature.getActorUsernameSnapshot());
        respVO.setActorNickname(actor != null ? actor.getNickname() : signature.getActorNicknameSnapshot());
        respVO.setSignerName(actor != null ? displayUserName(actor) : signature.getActorNicknameSnapshot());
        respVO.setActorUsernameSnapshot(signature.getActorUsernameSnapshot());
        respVO.setActorNicknameSnapshot(signature.getActorNicknameSnapshot());
        respVO.setActorDeptIdSnapshot(signature.getActorDeptIdSnapshot());
        respVO.setActorDeptNameSnapshot(signature.getActorDeptNameSnapshot());
        respVO.setActorPostNamesSnapshot(signature.getActorPostNamesSnapshot());
        respVO.setActorRoleNamesSnapshot(signature.getActorRoleNamesSnapshot());
        respVO.setSignaturePurpose(signature.getSignaturePurpose());
        respVO.setAuthorizationBasis(signature.getAuthorizationBasis());
        respVO.setAuthenticationMethod(signature.getAuthenticationMethod());
        respVO.setRecordVersionSnapshot(signature.getRecordVersionSnapshot());
        respVO.setRecordHashSnapshot(signature.getRecordHashSnapshot());
        respVO.setClientIpSnapshot(signature.getClientIpSnapshot());
        respVO.setUserAgentSnapshot(signature.getUserAgentSnapshot());
        respVO.setSnapshotStatus(signature.getSnapshotStatus());
        respVO.setActionType(signature.getActionType());
        respVO.setTaskActionResult(resolveDisplayTaskActionResult(signature));
        respVO.setMeaningCode(signature.getMeaningCode());
        respVO.setSourceFileHash(signature.getSourceFileHash());
        respVO.setSourceFileHashShort(shortHashNullable(signature.getSourceFileHash()));
        respVO.setSourceObjectKey(resolveFileObjectKey(signature.getSourceFileId()));
        respVO.setSourceVersionId(resolveFileVersionId(signature));
        respVO.setControlledCopyHashStatus(signature.getControlledCopyHashStatus());
        respVO.setControlledCopyHash(signature.getControlledCopyHash());
        respVO.setControlledCopyHashShort(shortHashNullable(signature.getControlledCopyHash()));
        respVO.setControlledCopyObjectKey(resolveFileObjectKey(signature.getControlledCopyFileId()));
        respVO.setControlledCopyVersionId(resolveFileVersionId(signature));
        respVO.setSignatureImageId(signature.getSignatureImageId());
        respVO.setSignatureImageVersionNo(signature.getSignatureImageVersionNo());
        respVO.setSignatureImageFileId(signature.getSignatureImageFileId());
        respVO.setSignatureImageFileUrl(signature.getSignatureImageFileUrl());
        respVO.setSignatureImageSha256(signature.getSignatureImageSha256());
        respVO.setSignatureImageSha256Short(shortHashNullable(signature.getSignatureImageSha256()));
        respVO.setSignatureImageContentType(signature.getSignatureImageContentType());
        respVO.setSignatureImageFileSize(signature.getSignatureImageFileSize());
        respVO.setSignatureImageStatusSnapshot(signature.getSignatureImageStatusSnapshot());
        respVO.setSignatureImageVerifiedStatus(signature.getSignatureImageVerifiedStatus());
        respVO.setPayloadVersion(signature.getEvidencePayloadVersion());
        respVO.setHashAlgorithm(signature.getEvidenceHashAlgorithm());
        respVO.setKeyVersion(signature.getEvidenceKeyVersion());
        respVO.setEvidenceHash(signature.getEvidenceHash());
        respVO.setEvidenceHashShort(shortHashNullable(signature.getEvidenceHash()));
        respVO.setEvidenceStatus(signature.getEvidenceStatus());
        respVO.setSignatureMode(signature.getSignatureMode());
        respVO.setPasswordVerified(signature.getPasswordVerified());
        respVO.setComment(signature.getComment());
        respVO.setSignedAt(signature.getSignedAt());
    }

    private void applyBindingProjection(DccElectronicSignatureRespVO respVO,
                                        DccControlledFileSignatureBindingVerification bindingVerification) {
        DccControlledFileSignatureBindingDO binding = projectedBinding(bindingVerification);
        if (binding == null) {
            if (bindingVerification != null && !bindingVerification.valid()) {
                respVO.setControlledCopyHashStatus(STATUS_INVALID);
            }
            return;
        }
        respVO.setControlledCopyHashStatus(COPY_HASH_STATUS_BOUND);
        respVO.setControlledCopyHash(binding.getControlledCopySha256());
        respVO.setControlledCopyHashShort(shortHashNullable(binding.getControlledCopySha256()));
        respVO.setControlledCopyObjectKey(resolveFileObjectKey(binding.getControlledCopyFileId()));
    }

    private void applyBindingProjection(DccSignatureVerifyRespVO respVO,
                                        DccControlledFileSignatureBindingVerification bindingVerification) {
        DccControlledFileSignatureBindingDO binding = projectedBinding(bindingVerification);
        if (binding == null) {
            respVO.setControlledCopyHashStatus(bindingVerification != null && !bindingVerification.valid()
                    ? STATUS_INVALID : COPY_HASH_STATUS_NOT_APPLICABLE);
            return;
        }
        respVO.setControlledCopyHashStatus(COPY_HASH_STATUS_BOUND);
        respVO.setControlledCopyFileId(binding.getControlledCopyFileId());
        respVO.setControlledCopyHash(binding.getControlledCopySha256());
    }

    private DccControlledFileSignatureBindingDO projectedBinding(
            DccControlledFileSignatureBindingVerification bindingVerification) {
        return bindingVerification == null ? null : bindingVerification.binding();
    }

    private String projectedCopyHashStatus(String storedStatus,
                                           DccControlledFileSignatureBindingVerification bindingVerification) {
        if (projectedBinding(bindingVerification) != null) {
            return COPY_HASH_STATUS_BOUND;
        }
        if (bindingVerification != null && !bindingVerification.valid()) {
            return STATUS_INVALID;
        }
        return storedStatus;
    }

    private String resolveFileObjectKey(Long fileId) {
        if (fileId == null) {
            return null;
        }
        FileDO file = fileService.getFile(fileId);
        return file == null ? null : StrUtil.trimToNull(file.getPath());
    }

    private String resolveFileVersionId(DccControlledFileSignatureDO signature) {
        return StrUtil.trimToNull(signature.getVersionNo());
    }

    private void fillAuthorizationRespVO(
            DccSignatureAuthorizationRespVO respVO,
            AdminUserDO user,
            DccElectronicSignatureAuthorizationDO authorization,
            DeptDO dept,
            DccElectronicSignatureAuthorizationAuditDO latestAudit,
            Map<Long, String> operatorNameMap) {
        respVO.setUserId(user.getId());
        respVO.setUsername(user.getUsername());
        respVO.setNickname(user.getNickname());
        respVO.setUserName(displayUserName(user));
        respVO.setDeptName(dept != null ? dept.getName() : null);
        respVO.setMobile(user.getMobile());
        respVO.setStatus(user.getStatus());
        respVO.setLoginDate(user.getLoginDate());
        String state = resolveAuthorizationState(authorization);
        respVO.setElectronicSignatureEnabled(authorization != null
                && Boolean.TRUE.equals(authorization.getElectronicSignatureEnabled())
                && STATE_ENABLED.equals(state));
        respVO.setAuthorizationState(state);
        respVO.setLocked(STATE_LOCKED.equals(state));
        respVO.setLockedUntil(STATE_LOCKED.equals(state) && authorization != null ? authorization.getLockedUntil() : null);
        if (latestAudit != null) {
            respVO.setLatestAuditReason(latestAudit.getReason());
            respVO.setLatestAuditAt(latestAudit.getOperatedAt());
            Long operatorId = latestAudit.getOperatorId();
            respVO.setLatestAuditOperatorId(operatorId);
            respVO.setLatestAuditOperatorName(operatorId != null ? operatorNameMap.get(operatorId) : null);
        }
    }

    private DccElectronicSignatureAuthorizationAuditRespVO toAuthorizationAuditRespVO(
            DccElectronicSignatureAuthorizationAuditDO audit,
            String operatorName) {
        DccElectronicSignatureAuthorizationAuditRespVO respVO = new DccElectronicSignatureAuthorizationAuditRespVO();
        respVO.setId(audit.getId());
        respVO.setTargetUserId(audit.getTargetUserId());
        respVO.setOperatorUserId(audit.getOperatorId());
        respVO.setOperatorName(operatorName);
        respVO.setBeforeState(audit.getBeforeState());
        respVO.setAfterState(audit.getAfterState());
        respVO.setReason(audit.getReason());
        respVO.setOperatedAt(audit.getOperatedAt());
        return respVO;
    }

    private DccControlledFileSignatureExportSummaryRespVO.SignatureItem toExportSignatureItem(
            DccControlledFileSignatureDO signature, VerificationComputation computation) {
        DccControlledFileSignatureExportSummaryRespVO.SignatureItem item =
                new DccControlledFileSignatureExportSummaryRespVO.SignatureItem();
        item.setSignatureId(signature.getId());
        item.setTaskActionResult(resolveDisplayTaskActionResult(signature));
        item.setMeaningCode(signature.getMeaningCode());
        DccControlledFileSignatureBindingDO binding = projectedBinding(computation.bindingVerification());
        item.setControlledCopyHashStatus(projectedCopyHashStatus(signature.getControlledCopyHashStatus(),
                computation.bindingVerification()));
        item.setControlledCopyFileId(binding == null
                ? signature.getControlledCopyFileId() : binding.getControlledCopyFileId());
        item.setControlledCopyHash(binding == null
                ? signature.getControlledCopyHash() : binding.getControlledCopySha256());
        item.setBindingEventKey(binding == null ? null : binding.getBindingEventKey());
        item.setBoundAt(binding == null ? null : binding.getBoundAt());
        item.setEvidenceStatus(computation.verificationStatus());
        item.setVerificationReason(computation.verificationReason());
        item.setEvidenceHashShort(shortHashNullable(signature.getEvidenceHash()));
        item.setSignedAt(signature.getSignedAt());
        return item;
    }

    private void normalizeSignaturePageFilters(DccElectronicSignaturePageReqVO reqVO) {
        reqVO.setPersistentActionType(toPersistentActionType(reqVO.getTaskActionResult()));
        reqVO.setControlledCopyHashStatus(StrUtil.trim(reqVO.getControlledCopyHashStatus()));
        reqVO.setEvidenceHashShort(StrUtil.trim(reqVO.getEvidenceHashShort()));
    }

    private String toPersistentActionType(String taskActionResult) {
        if (StrUtil.isBlank(taskActionResult)) {
            return null;
        }
        return SignatureTaskActionMapping.fromTaskActionResult(StrUtil.trim(taskActionResult))
                .persistentActionType();
    }

    private VerificationComputation verifySignature(DccControlledFileSignatureDO signature,
                                                    DccControlledFileDO controlledFile) {
        LocalDateTime verifiedAt = LocalDateTime.now();
        DccControlledFileSignatureBindingVerification bindingVerification = controlledFile != null
                && controlledFile.getPublishedFileId() != null
                ? signatureBindingService.verifyPublishedCopyBinding(signature, controlledFile)
                : DccControlledFileSignatureBindingVerification.notApplicable();
        if (!hasCompleteVerifiableEvidence(signature, controlledFile)) {
            return invalidVerification(null, null, missingEvidenceVerificationStatus(signature), verifiedAt,
                    "SIGNATURE_EVIDENCE_MISSING", null, bindingVerification);
        }
        if (!Set.of("v1", DccControlledFileSignatureEvidenceServiceImpl.PAYLOAD_VERSION_V2,
                DccControlledFileSignatureEvidenceServiceImpl.PAYLOAD_VERSION_V3_IMAGE)
                .contains(signature.getEvidencePayloadVersion())) {
            return invalidVerification(null, null, STATUS_INVALID, verifiedAt,
                    "EVIDENCE_PAYLOAD_VERSION_UNSUPPORTED", null, bindingVerification);
        }
        if (!"HMAC_SHA256".equals(signature.getEvidenceHashAlgorithm())) {
            return invalidVerification(null, null, STATUS_INVALID, verifiedAt,
                    "EVIDENCE_HASH_ALGORITHM_UNSUPPORTED", null, bindingVerification);
        }
        if (StrUtil.isNotBlank(signatureEvidenceProperties.getKeyVersion())
                && !StrUtil.equals(signatureEvidenceProperties.getKeyVersion(), signature.getEvidenceKeyVersion())) {
            return invalidVerification(null, null, STATUS_INVALID, verifiedAt,
                    "EVIDENCE_KEY_VERSION_MISMATCH", null, bindingVerification);
        }
        String sourceFileVerificationReason = verifySourceFileHash(signature);
        if (StrUtil.isNotBlank(sourceFileVerificationReason)) {
            return invalidVerification(null, null, STATUS_INVALID, verifiedAt,
                    sourceFileVerificationReason, null, bindingVerification);
        }
        try {
            signatureEvidenceProperties.validateRuntimeConfig();
        } catch (ServiceException ex) {
            return invalidVerification(null, null, STATUS_INVALID, verifiedAt,
                    "EVIDENCE_RUNTIME_CONFIG_INVALID", null, bindingVerification);
        }
        DccElectronicSignatureImageSnapshot signatureImageSnapshot = null;
        if (isImageEvidencePayload(signature)) {
            try {
                signatureImageSnapshot = signatureImageService.verifySignatureSnapshot(signature);
            } catch (RuntimeException ex) {
                return invalidVerification(null, null, STATUS_INVALID, verifiedAt,
                        "SIGNATURE_IMAGE_INVALID", null, bindingVerification);
            }
        }
        String canonicalPayload = buildCanonicalPayload(signature, controlledFile);
        String recomputedHash = hmacSha256Hex(canonicalPayload);
        if (!StrUtil.equalsIgnoreCase(signature.getEvidenceHash(), recomputedHash)) {
            return invalidVerification(canonicalPayload, recomputedHash, STATUS_INVALID, verifiedAt,
                    "EVIDENCE_HMAC_MISMATCH", signatureImageSnapshot, bindingVerification);
        }
        if (!STATUS_VALID.equals(signature.getEvidenceStatus())) {
            return invalidVerification(canonicalPayload, recomputedHash, STATUS_INVALID, verifiedAt,
                    "EVIDENCE_STATUS_INVALID", signatureImageSnapshot, bindingVerification);
        }
        if (!bindingVerification.valid()) {
            return invalidVerification(canonicalPayload, recomputedHash, STATUS_INVALID, verifiedAt,
                    bindingVerification.reasonCode(), signatureImageSnapshot, bindingVerification);
        }
        return new VerificationComputation(canonicalPayload, recomputedHash, STATUS_VALID, "", verifiedAt,
                signatureImageSnapshot == null ? null : signatureImageSnapshot.getContent(), bindingVerification);
    }

    private VerificationComputation invalidVerification(String canonicalPayload, String recomputedHash,
                                                        String verificationStatus, LocalDateTime verifiedAt,
                                                        String verificationReason,
                                                        DccElectronicSignatureImageSnapshot signatureImageSnapshot,
                                                        DccControlledFileSignatureBindingVerification bindingVerification) {
        return new VerificationComputation(canonicalPayload, recomputedHash, verificationStatus, verificationReason,
                verifiedAt, signatureImageSnapshot == null ? null : signatureImageSnapshot.getContent(),
                bindingVerification);
    }

    private boolean hasCompleteVerifiableEvidence(DccControlledFileSignatureDO signature,
                                                  DccControlledFileDO controlledFile) {
        if (signature == null || controlledFile == null) {
            return false;
        }
        if (StrUtil.hasBlank(signature.getEvidencePayloadVersion(), signature.getEvidenceHashAlgorithm(),
                signature.getEvidenceKeyVersion(), signature.getEvidenceHash(), controlledFile.getFileNumber(),
                controlledFile.getProcessInstanceId(), signature.getVersionNo(), signature.getSourceFileHash(),
                signature.getControlledCopyHashStatus(), signature.getTaskId(), signature.getActionType(),
                signature.getMeaningCode(), signature.getActorUsernameSnapshot(), signature.getActorNicknameSnapshot(),
                signature.getActorPostNamesSnapshot(), signature.getActorRoleNamesSnapshot(),
                signature.getSignaturePurpose(), signature.getAuthorizationBasis(), signature.getAuthenticationMethod())) {
            return false;
        }
        if (signature.getControlledFileId() == null || signature.getRevisionId() == null
                || signature.getActorId() == null || signature.getSignedAt() == null) {
            return false;
        }
        if (isImageEvidencePayload(signature) && !hasCompleteSignatureImageEvidence(signature)) {
            return false;
        }
        if (COPY_HASH_STATUS_BOUND.equals(signature.getControlledCopyHashStatus())) {
            return StrUtil.isNotBlank(signature.getControlledCopyHash());
        }
        return COPY_HASH_STATUS_NOT_APPLICABLE.equals(signature.getControlledCopyHashStatus());
    }

    private String verifySourceFileHash(DccControlledFileSignatureDO signature) {
        if (signature.getSourceFileId() == null) {
            return "";
        }
        FileDO sourceFile = fileService.getFile(signature.getSourceFileId());
        if (sourceFile == null || sourceFile.getConfigId() == null || StrUtil.isBlank(sourceFile.getPath())) {
            return "SOURCE_FILE_UNREADABLE";
        }
        try {
            byte[] content = fileService.getFileContent(sourceFile.getConfigId(), sourceFile.getPath());
            String actualHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            return StrUtil.equalsIgnoreCase(signature.getSourceFileHash(), actualHash)
                    ? "" : "SOURCE_FILE_HASH_MISMATCH";
        } catch (Exception ex) {
            return "SOURCE_FILE_UNREADABLE";
        }
    }

    private boolean isImageEvidencePayload(DccControlledFileSignatureDO signature) {
        return signature != null && (DccControlledFileSignatureEvidenceServiceImpl.PAYLOAD_VERSION_V3_IMAGE
                .equals(signature.getEvidencePayloadVersion()) || signature.getSignatureImageId() != null);
    }

    private boolean hasCompleteSignatureImageEvidence(DccControlledFileSignatureDO signature) {
        return signature.getSignatureImageId() != null
                && signature.getSignatureImageVersionNo() != null
                && signature.getSignatureImageFileId() != null
                && signature.getSignatureImageFileSize() != null
                && !StrUtil.hasBlank(signature.getSignatureImageSha256(), signature.getSignatureImageContentType(),
                signature.getSignatureImageStatusSnapshot(), signature.getSignatureImageVerifiedStatus());
    }

    private String buildCanonicalPayload(DccControlledFileSignatureDO signature,
                                         DccControlledFileDO controlledFile) {
        StringBuilder payload = new StringBuilder("{");
        append(payload, "payloadVersion", signature.getEvidencePayloadVersion());
        append(payload, "hashAlgorithm", signature.getEvidenceHashAlgorithm());
        append(payload, "keyVersion", signature.getEvidenceKeyVersion());
        append(payload, "tenantId", TenantContextHolder.getRequiredTenantId());
        append(payload, "controlledFileId", signature.getControlledFileId());
        append(payload, "fileNumber", controlledFile.getFileNumber());
        append(payload, "revisionId", signature.getRevisionId());
        append(payload, "versionNo", signature.getVersionNo());
        append(payload, "sourceFileHash", signature.getSourceFileHash());
        append(payload, "controlledCopyHashStatus", signature.getControlledCopyHashStatus());
        append(payload, "controlledCopyHash", COPY_HASH_STATUS_NOT_APPLICABLE.equals(signature.getControlledCopyHashStatus())
                ? "" : signature.getControlledCopyHash());
        if (isImageEvidencePayload(signature)) {
            append(payload, "signatureImageId", signature.getSignatureImageId());
            append(payload, "signatureImageVersionNo", signature.getSignatureImageVersionNo());
            append(payload, "signatureImageFileId", signature.getSignatureImageFileId());
            append(payload, "signatureImageSha256", signature.getSignatureImageSha256());
            append(payload, "signatureImageContentType", signature.getSignatureImageContentType());
            append(payload, "signatureImageFileSize", signature.getSignatureImageFileSize());
            append(payload, "signatureImageStatusSnapshot", signature.getSignatureImageStatusSnapshot());
            append(payload, "signatureImageVerifiedStatus", signature.getSignatureImageVerifiedStatus());
        }
        append(payload, "processInstanceId", controlledFile.getProcessInstanceId());
        append(payload, "taskId", signature.getTaskId());
        append(payload, "taskActionResult", normalizeTaskActionResult(signature.getActionType()));
        append(payload, "meaningCode", signature.getMeaningCode());
        append(payload, "signerUserId", signature.getActorId());
        append(payload, "signerUsername", signature.getActorUsernameSnapshot());
        append(payload, "signerNickname", signature.getActorNicknameSnapshot());
        append(payload, "signerDeptId", signature.getActorDeptIdSnapshot());
        append(payload, "signerDeptName", signature.getActorDeptNameSnapshot());
        append(payload, "signerPostNames", signature.getActorPostNamesSnapshot());
        append(payload, "signerRoleNames", signature.getActorRoleNamesSnapshot());
        append(payload, "signaturePurpose", signature.getSignaturePurpose());
        append(payload, "authorizationBasis", signature.getAuthorizationBasis());
        append(payload, "authenticationMethod", signature.getAuthenticationMethod());
        append(payload, "signedAt", OffsetDateTime.of(signature.getSignedAt(), ZoneOffset.ofHours(8)).toString());
        append(payload, "reasonText", StrUtil.trimToEmpty(signature.getComment()));
        payload.append('}');
        return payload.toString();
    }

    private String hmacSha256Hex(String canonicalPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signatureEvidenceProperties.getHmacSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
    }

    private boolean hasValidExportEvidence(DccControlledFileSignatureDO signature) {
        return STATUS_VALID.equals(signature.getEvidenceStatus())
                && StrUtil.isNotBlank(signature.getEvidenceHash())
                && StrUtil.isNotBlank(signature.getMeaningCode())
                && StrUtil.isNotBlank(signature.getControlledCopyHashStatus())
                && signature.getSignedAt() != null;
    }

    private boolean isExportEvidenceValid(DccControlledFileSignatureDO signature,
                                          DccControlledFileDO controlledFile,
                                          VerificationComputation computation) {
        return hasCompleteVerifiableEvidence(signature, controlledFile)
                && STATUS_VALID.equals(signature.getEvidenceStatus())
                && STATUS_VALID.equals(computation.verificationStatus())
                && StrUtil.isNotBlank(computation.canonicalPayload())
                && StrUtil.equalsIgnoreCase(signature.getEvidenceHash(), computation.recomputedEvidenceHash());
    }

    private Map<String, Object> buildExportSignatureArtifact(DccControlledFileSignatureDO signature,
                                                             VerificationComputation computation) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("signatureId", signature.getId());
        item.put("taskId", signature.getTaskId());
        item.put("taskActionResult", normalizeTaskActionResult(signature.getActionType()));
        item.put("meaningCode", signature.getMeaningCode());
        item.put("signerUserId", signature.getActorId());
        item.put("signerUsername", signature.getActorUsernameSnapshot());
        item.put("signerNickname", signature.getActorNicknameSnapshot());
        item.put("signerDeptId", signature.getActorDeptIdSnapshot());
        item.put("signerDeptName", signature.getActorDeptNameSnapshot());
        item.put("signerPostNames", signature.getActorPostNamesSnapshot());
        item.put("signerRoleNames", signature.getActorRoleNamesSnapshot());
        item.put("signaturePurpose", signature.getSignaturePurpose());
        item.put("authorizationBasis", signature.getAuthorizationBasis());
        item.put("authenticationMethod", signature.getAuthenticationMethod());
        item.put("clientIpSnapshot", signature.getClientIpSnapshot());
        item.put("userAgentSnapshot", signature.getUserAgentSnapshot());
        item.put("recordVersionSnapshot", signature.getRecordVersionSnapshot());
        item.put("recordHashSnapshot", signature.getRecordHashSnapshot());
        item.put("snapshotStatus", signature.getSnapshotStatus());
        item.put("signedAt", toBeijingOffsetString(signature.getSignedAt()));
        item.put("reasonText", StrUtil.trimToEmpty(signature.getComment()));
        item.put("sourceFileHash", signature.getSourceFileHash());
        DccControlledFileSignatureBindingDO binding = projectedBinding(computation.bindingVerification());
        item.put("controlledCopyHashStatus", projectedCopyHashStatus(signature.getControlledCopyHashStatus(),
                computation.bindingVerification()));
        item.put("controlledCopyFileId", binding == null
                ? signature.getControlledCopyFileId() : binding.getControlledCopyFileId());
        item.put("controlledCopyHash", binding == null
                ? signature.getControlledCopyHash() : binding.getControlledCopySha256());
        item.put("bindingEventKey", binding == null ? null : binding.getBindingEventKey());
        item.put("boundAt", binding == null ? null : binding.getBoundAt());
        item.put("signatureImageId", signature.getSignatureImageId());
        item.put("signatureImageVersionNo", signature.getSignatureImageVersionNo());
        item.put("signatureImageFileId", signature.getSignatureImageFileId());
        item.put("signatureImageFileUrl", signature.getSignatureImageFileUrl());
        item.put("signatureImageSha256", signature.getSignatureImageSha256());
        item.put("signatureImageContentType", signature.getSignatureImageContentType());
        item.put("signatureImageFileSize", signature.getSignatureImageFileSize());
        item.put("signatureImageStatusSnapshot", signature.getSignatureImageStatusSnapshot());
        item.put("signatureImageVerifiedStatus", signature.getSignatureImageVerifiedStatus());
        item.put("signatureImageContent", computation.signatureImageContent());
        item.put("payloadVersion", signature.getEvidencePayloadVersion());
        item.put("hashAlgorithm", signature.getEvidenceHashAlgorithm());
        item.put("keyVersion", signature.getEvidenceKeyVersion());
        item.put("storedEvidenceHash", signature.getEvidenceHash());
        item.put("recomputedEvidenceHash", computation.recomputedEvidenceHash());
        item.put("verificationStatus", computation.verificationStatus());
        item.put("verificationReason", computation.verificationReason());
        item.put("canonicalPayload", computation.canonicalPayload());
        return item;
    }

    private String buildExportFileName(DccControlledFileDO file, LocalDateTime exportedAt) {
        return "dcc-signature-evidence-"
                + sanitizeExportFileNameSegment(file.getFileNumber())
                + "-"
                + sanitizeExportFileNameSegment(file.getVersionNo())
                + "-"
                + exportedAt.format(EXPORT_FILE_TIMESTAMP_FORMATTER)
                + ".pdf";
    }

    private byte[] renderSignatureEvidencePdf(DccControlledFileDO file,
                                              LocalDateTime exportedAt,
                                              List<Map<String, Object>> signatureArtifacts) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFont font = PDType0Font.load(document, new File(SIGNATURE_EVIDENCE_PDF_FONT_PATH));
            SignatureEvidencePdfWriter writer = new SignatureEvidencePdfWriter(document, font);
            writer.title("电子签名证据页", "本系统认证并可校验");
            writer.section("Record Tracking / 记录追踪");
            writer.fieldRows(List.of(
                    field("记录状态", "Original"),
                    field("业务模块", "DCC"),
                    field("文件编号", value(file.getFileNumber())),
                    field("文件名称", value(file.getFileName())),
                    field("版本", value(file.getVersionNo())),
                    field("修订 ID", value(file.getId())),
                    field("流程实例", value(file.getProcessInstanceId())),
                    field("导出时间", value(toBeijingOffsetString(exportedAt))),
                    field("租户 ID", value(TenantContextHolder.getRequiredTenantId()))
            ));
            writer.section("System Verification / 系统校验");
            writer.fieldRows(List.of(
                    field("校验声明", "本系统认证并可校验"),
                    field("校验规则", "规范化载荷 + 证据哈希/HMAC 复算"),
                    field("校验时间", value(toBeijingOffsetString(exportedAt))),
                    field("证据说明", SIGNATURE_EVIDENCE_SYSTEM_STATEMENT)
            ));
            for (int index = 0; index < signatureArtifacts.size(); index++) {
                Map<String, Object> signature = signatureArtifacts.get(index);
                writer.section("Signer Events / 签名事件 " + (index + 1));
                writer.fieldRows(List.of(
                        field("签名 ID", value(signature.get("signatureId"))),
                        field("签名人", readableSignerName(signature)),
                        field("账号", value(signature.get("signerUsername"))),
                        field("部门", value(signature.get("signerDeptName"))),
                        field("岗位", value(signature.get("signerPostNames"))),
                        field("角色", value(signature.get("signerRoleNames"))),
                        field("认证方式", value(signature.get("authenticationMethod"))),
                        field("签名动作", value(signature.get("taskActionResult"))),
                        field("签名原因", value(signature.get("reasonText"))),
                        field("签名时间", value(signature.get("signedAt"))),
                        field("客户端 IP", value(signature.get("clientIpSnapshot"))),
                        field("User-Agent", value(signature.get("userAgentSnapshot"))),
                        field("签名图片版本", value(signature.get("signatureImageVersionNo"))),
                        field("签名图片文件 ID", value(signature.get("signatureImageFileId"))),
                        field("签名图片哈希", value(signature.get("signatureImageSha256"))),
                        field("签名图片状态", value(signature.get("signatureImageStatusSnapshot"))),
                        field("签名图片校验", value(signature.get("signatureImageVerifiedStatus"))),
                        field("源文件哈希", value(signature.get("sourceFileHash"))),
                        field("证据哈希", value(signature.get("storedEvidenceHash"))),
                        field("复算证据哈希", value(signature.get("recomputedEvidenceHash"))),
                        field("校验状态", value(signature.get("verificationStatus")))
                ));
                byte[] imageContent = (byte[]) signature.get("signatureImageContent");
                if (imageContent != null && imageContent.length > 0) {
                    writer.signatureImage("签名图片 / 手写签名图", imageContent,
                            value(signature.get("signatureImageFileId")));
                }
            }
            writer.footer(SIGNATURE_EVIDENCE_SYSTEM_STATEMENT);
            writer.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException | RuntimeException ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED);
        }
    }

    private static String value(Object value) {
        String text = Objects.toString(value, "");
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
        return StrUtil.isBlank(text) ? "-" : text;
    }

    private static SignatureEvidencePdfField field(String label, String value) {
        return new SignatureEvidencePdfField(label, value);
    }

    private List<String> canonicalPayloadFieldOrder(DccControlledFileSignatureDO signature) {
        return isImageEvidencePayload(signature) ? CANONICAL_PAYLOAD_FIELD_ORDER_V3_IMAGE : CANONICAL_PAYLOAD_FIELD_ORDER;
    }

    private static String readableSignerName(Map<String, Object> signature) {
        String nickname = value(signature.get("signerNickname"));
        if (isUnreadableDisplayValue(nickname)) {
            String username = value(signature.get("signerUsername"));
            if (!isUnreadableDisplayValue(username)) {
                return username;
            }
            return "用户 " + value(signature.get("signerUserId"));
        }
        return nickname;
    }

    private static boolean isUnreadableDisplayValue(String value) {
        if (StrUtil.isBlank(value) || "-".equals(value)) {
            return true;
        }
        if (value.contains("?") || value.contains("�") || value.contains("□")) {
            return true;
        }
        String compact = value.replace("?", "")
                .replace("�", "")
                .replace("□", "")
                .trim();
        return StrUtil.isBlank(compact);
    }

    private record SignatureEvidencePdfField(String label, String value) {
    }

    private static final class SignatureEvidencePdfWriter {

        private static final float MARGIN = 38F;
        private static final float CONTENT_WIDTH = PDRectangle.A4.getWidth() - MARGIN * 2;
        private static final float LEADING = 14F;
        private static final float ROW_GAP = 5F;
        private static final float FIELD_LABEL_WIDTH = 92F;
        private static final float BODY_FONT_SIZE = 10F;
        private static final float TITLE_FONT_SIZE = 18F;
        private static final float SUBTITLE_FONT_SIZE = 11F;
        private static final float SECTION_FONT_SIZE = 11F;
        private static final Color DARK_BLUE = new Color(31, 48, 79);
        private static final Color SECTION_BLUE = new Color(229, 237, 247);
        private static final Color BORDER_GRAY = new Color(190, 198, 210);
        private static final Color TEXT_GRAY = new Color(40, 45, 52);
        private static final Color MUTED_GRAY = new Color(96, 106, 122);
        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private SignatureEvidencePdfWriter(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        private void title(String text, String subtitle) throws IOException {
            ensureSpace(56F);
            contentStream.setNonStrokingColor(DARK_BLUE);
            contentStream.addRect(MARGIN, y - 42F, CONTENT_WIDTH, 42F);
            contentStream.fill();
            textAt(text, MARGIN + 16F, y - 18F, TITLE_FONT_SIZE, Color.WHITE);
            textAt(subtitle, MARGIN + 16F, y - 35F, SUBTITLE_FONT_SIZE, Color.WHITE);
            y -= 58F;
        }

        private void section(String text) throws IOException {
            ensureSpace(34F);
            contentStream.setNonStrokingColor(SECTION_BLUE);
            contentStream.addRect(MARGIN, y - 22F, CONTENT_WIDTH, 22F);
            contentStream.fill();
            contentStream.setStrokingColor(BORDER_GRAY);
            contentStream.addRect(MARGIN, y - 22F, CONTENT_WIDTH, 22F);
            contentStream.stroke();
            textAt(text, MARGIN + 10F, y - 15F, SECTION_FONT_SIZE, DARK_BLUE);
            y -= 31F;
        }

        private void fieldRows(List<SignatureEvidencePdfField> rows) throws IOException {
            for (SignatureEvidencePdfField row : rows) {
                field(row.label(), row.value());
            }
            blank();
        }

        private void field(String label, String value) throws IOException {
            String normalizedValue = value(value);
            String fullLine = label + ": " + normalizedValue;
            List<String> lines = wrap(fullLine, BODY_FONT_SIZE, CONTENT_WIDTH - 18F);
            float rowHeight = Math.max(LEADING + ROW_GAP, lines.size() * LEADING + ROW_GAP);
            ensureSpace(rowHeight + 3F);
            contentStream.setStrokingColor(BORDER_GRAY);
            contentStream.moveTo(MARGIN, y + 3F);
            contentStream.lineTo(MARGIN + CONTENT_WIDTH, y + 3F);
            contentStream.stroke();
            textAt(label + ":", MARGIN + 8F, y - 9F, BODY_FONT_SIZE, MUTED_GRAY);
            float valueY = y - 9F;
            float valueX = MARGIN + FIELD_LABEL_WIDTH;
            List<String> valueLines = wrap(normalizedValue, BODY_FONT_SIZE, CONTENT_WIDTH - FIELD_LABEL_WIDTH - 10F);
            for (String valueLine : valueLines) {
                textAt(valueLine, valueX, valueY, BODY_FONT_SIZE, TEXT_GRAY);
                valueY -= LEADING;
            }
            y -= rowHeight;
        }

        private void signatureImage(String label, byte[] imageBytes, String imageId) throws IOException {
            PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes,
                    "signature-image-" + value(imageId));
            float maxWidth = 190F;
            float maxHeight = 82F;
            float width = image.getWidth();
            float height = image.getHeight();
            float scale = Math.min(maxWidth / width, maxHeight / height);
            float drawWidth = Math.max(1F, width * scale);
            float drawHeight = Math.max(1F, height * scale);
            float blockHeight = drawHeight + 44F;
            ensureSpace(blockHeight);
            textAt(label, MARGIN + 8F, y - 10F, BODY_FONT_SIZE, MUTED_GRAY);
            float imageX = MARGIN + FIELD_LABEL_WIDTH;
            float imageY = y - 22F - drawHeight;
            contentStream.setStrokingColor(BORDER_GRAY);
            contentStream.addRect(imageX - 6F, imageY - 6F, drawWidth + 12F, drawHeight + 12F);
            contentStream.stroke();
            contentStream.drawImage(image, imageX, imageY, drawWidth, drawHeight);
            y -= blockHeight;
            blank();
        }

        private void line(String text) throws IOException {
            for (String line : wrap(text, BODY_FONT_SIZE, CONTENT_WIDTH)) {
                write(line, BODY_FONT_SIZE, false);
            }
        }

        private void blank() throws IOException {
            ensureSpace(LEADING);
            y -= LEADING;
        }

        private void write(String text, float fontSize, boolean bold) throws IOException {
            ensureSpace(LEADING);
            textAt(text, MARGIN, y, fontSize, TEXT_GRAY);
            if (bold) {
                contentStream.moveTo(MARGIN, y - 2);
                contentStream.lineTo(PDRectangle.A4.getWidth() - MARGIN, y - 2);
                contentStream.stroke();
            }
            y -= LEADING;
        }

        private List<String> wrap(String text, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                String candidate = current + String.valueOf(ch);
                if (font.getStringWidth(candidate) / 1000 * fontSize > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(String.valueOf(ch));
                } else {
                    current.append(ch);
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private void footer(String text) throws IOException {
            ensureSpace(32F);
            contentStream.setStrokingColor(BORDER_GRAY);
            contentStream.moveTo(MARGIN, y);
            contentStream.lineTo(MARGIN + CONTENT_WIDTH, y);
            contentStream.stroke();
            y -= 15F;
            for (String line : wrap(text, 8.5F, CONTENT_WIDTH)) {
                textAt(line, MARGIN, y, 8.5F, MUTED_GRAY);
                y -= 11F;
            }
        }

        private void textAt(String text, float x, float y, float fontSize, Color color) throws IOException {
            contentStream.beginText();
            contentStream.setNonStrokingColor(color);
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(value(text));
            contentStream.endText();
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight >= MARGIN) {
                return;
            }
            closePage();
            newPage();
        }

        private void newPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void closePage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private void close() throws IOException {
            closePage();
        }
    }

    private String sanitizeExportFileNameSegment(String value) {
        String trimmed = StrUtil.trim(value);
        if (StrUtil.isBlank(trimmed)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED);
        }
        return trimmed.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String toBeijingOffsetString(LocalDateTime value) {
        return value == null ? null : OffsetDateTime.of(value, ZoneOffset.ofHours(8)).toString();
    }

    private String missingEvidenceVerificationStatus(DccControlledFileSignatureDO signature) {
        if (signature != null && STATUS_HISTORICAL_UNBOUND.equals(signature.getEvidenceStatus())) {
            return STATUS_HISTORICAL_UNBOUND;
        }
        return STATUS_INVALID;
    }

    private String resolveDisplayTaskActionResult(DccControlledFileSignatureDO signature) {
        if (isHistoricalUnbound(signature) && !isNormalizableTaskAction(signature.getActionType())) {
            return signature.getActionType();
        }
        return normalizeTaskActionResult(signature.getActionType());
    }

    private boolean isHistoricalUnbound(DccControlledFileSignatureDO signature) {
        return signature != null && STATUS_HISTORICAL_UNBOUND.equals(signature.getEvidenceStatus());
    }

    private boolean isNormalizableTaskAction(String actionType) {
        if (actionType == null) {
            return false;
        }
        return switch (actionType) {
            case "APPROVE", "REJECT", "RETURN", "TRANSFER", "ADD_SIGN",
                    "DISTRIBUTION_ACK", "DISTRIBUTION_SIGN" -> true;
            default -> false;
        };
    }

    private String resolveAuthorizationState(DccElectronicSignatureAuthorizationDO authorization) {
        if (authorization == null) {
            return STATE_UNAUTHORIZED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (DccElectronicSignatureAuthorizationServiceImpl.isActiveLock(authorization, now)) {
            return STATE_LOCKED;
        }
        if (Boolean.TRUE.equals(authorization.getElectronicSignatureEnabled())
                && (STATE_ENABLED.equals(authorization.getAuthorizationState())
                || DccElectronicSignatureAuthorizationServiceImpl.isExpiredLock(authorization, now))) {
            return STATE_ENABLED;
        }
        return STATE_DISABLED;
    }

    private String normalizeTaskActionResult(String actionType) {
        return SignatureTaskActionMapping.fromPersistentActionType(actionType)
                .taskActionResult();
    }

    private String displayUserName(AdminUserDO user) {
        if (user == null) {
            return null;
        }
        String nickname = StrUtil.trimToNull(user.getNickname());
        String username = StrUtil.trimToNull(user.getUsername());
        if (!isUnreadableDisplayValue(nickname)) {
            return nickname;
        }
        return StrUtil.isNotBlank(username) ? username : nickname;
    }

    private static void append(StringBuilder payload, String field, String value) {
        appendName(payload, field);
        payload.append('"').append(escapeJson(value)).append('"');
    }

    private static void append(StringBuilder payload, String field, Long value) {
        appendName(payload, field);
        payload.append(value);
    }

    private static void append(StringBuilder payload, String field, Integer value) {
        appendName(payload, field);
        payload.append(value);
    }

    private static void appendName(StringBuilder payload, String field) {
        if (payload.length() > 1) {
            payload.append(',');
        }
        payload.append('"').append(field).append("\":");
    }

    private static String escapeJson(String value) {
        return StrUtil.nullToEmpty(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String shortHashNullable(String hash) {
        if (StrUtil.isBlank(hash)) {
            return "";
        }
        if (hash.length() < 12) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        return hash.substring(0, 12).toLowerCase();
    }

    private enum SignatureTaskActionMapping {
        APPROVE("APPROVE", "APPROVED"),
        REJECT("REJECT", "REJECTED"),
        RETURN("RETURN", "RETURNED"),
        TRANSFER("TRANSFER", "TRANSFERRED"),
        ADD_SIGN("ADD_SIGN", "SIGN_ADDED"),
        DISTRIBUTION_ACK("DISTRIBUTION_ACK", "DISTRIBUTION_ACK"),
        DISTRIBUTION_SIGN("DISTRIBUTION_SIGN", "DISTRIBUTION_SIGN");

        private final String persistentActionType;
        private final String taskActionResult;

        SignatureTaskActionMapping(String persistentActionType, String taskActionResult) {
            this.persistentActionType = persistentActionType;
            this.taskActionResult = taskActionResult;
        }

        private String persistentActionType() {
            return persistentActionType;
        }

        private String taskActionResult() {
            return taskActionResult;
        }

        private static SignatureTaskActionMapping fromPersistentActionType(String actionType) {
            if (actionType == null) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            return switch (actionType) {
                case "APPROVE" -> APPROVE;
                case "REJECT" -> REJECT;
                case "RETURN" -> RETURN;
                case "TRANSFER" -> TRANSFER;
                case "ADD_SIGN" -> ADD_SIGN;
                case "DISTRIBUTION_ACK" -> DISTRIBUTION_ACK;
                case "DISTRIBUTION_SIGN" -> DISTRIBUTION_SIGN;
                default -> throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            };
        }

        private static SignatureTaskActionMapping fromTaskActionResult(String taskActionResult) {
            if (taskActionResult == null) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            return switch (taskActionResult) {
                case "APPROVED" -> APPROVE;
                case "REJECTED" -> REJECT;
                case "RETURNED" -> RETURN;
                case "TRANSFERRED" -> TRANSFER;
                case "SIGN_ADDED" -> ADD_SIGN;
                case "DISTRIBUTION_ACK" -> DISTRIBUTION_ACK;
                case "DISTRIBUTION_SIGN" -> DISTRIBUTION_SIGN;
                default -> throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            };
        }
    }

    private record VerificationComputation(String canonicalPayload, String recomputedEvidenceHash,
                                           String verificationStatus, String verificationReason,
                                           LocalDateTime verifiedAt, byte[] signatureImageContent,
                                           DccControlledFileSignatureBindingVerification bindingVerification) {
    }
}
