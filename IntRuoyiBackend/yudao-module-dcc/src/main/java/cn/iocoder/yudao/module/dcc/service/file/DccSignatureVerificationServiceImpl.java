package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileSignatureModeEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_APPROVER_POST_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_REASON_REQUIRED;

@Service
@Validated
public class DccSignatureVerificationServiceImpl implements DccSignatureVerificationService {

    @Resource
    private AdminUserService adminUserService;
    @Resource
    private DeptService deptService;
    @Resource
    private PostService postService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private DccElectronicSignatureAuthorizationService electronicSignatureAuthorizationService;
    @Resource
    private DccControlledFileSignatureEvidenceService signatureEvidenceService;
    @Resource
    private DccElectronicSignatureImageService signatureImageService;
    @Resource
    private DccControlledFileSignatureMapper signatureMapper;
    @Resource
    private ElectronicSignatureService electronicSignatureService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccUnifiedSignatureResult verifyPasswordAndCreateSignature(Long actorId, Long controlledFileId, String taskId,
                                                                      String stageCode, String actionType,
                                                                      String password, String comment) {
        String reasonText = requireReasonText(comment);
        electronicSignatureAuthorizationService.validateElectronicSignatureEnabled(actorId);
        String meaningCode = resolveMeaningCode(stageCode, actionType);
        AdminUserDO user = adminUserService.getUser(actorId);
        LocalDateTime signedAt = LocalDateTime.now().withNano(0);
        SignatureActorSnapshot actorSnapshot = buildActorSnapshot(user, meaningCode);
        DccElectronicSignatureImageSnapshot imageSnapshot = signatureImageService.requireActiveSnapshot(actorId);
        DccControlledFileSignatureEvidence evidence = signatureEvidenceService.createEvidence(
                DccControlledFileSignatureEvidenceCreateReq.builder()
                        .tenantId(TenantContextHolder.getRequiredTenantId())
                        .controlledFileId(controlledFileId)
                        .taskId(taskId)
                        .taskActionResult(normalizeTaskActionResult(actionType))
                        .meaningCode(meaningCode)
                        .signerUserId(actorId)
                        .signerDeptId(user.getDeptId())
                        .signerUsername(actorSnapshot.actorUsernameSnapshot())
                        .signerNickname(actorSnapshot.actorNicknameSnapshot())
                        .signerDeptName(actorSnapshot.actorDeptNameSnapshot())
                        .signerPostNames(actorSnapshot.actorPostNamesSnapshot())
                        .signerRoleNames(actorSnapshot.actorRoleNamesSnapshot())
                        .signaturePurpose(actorSnapshot.signaturePurpose())
                        .authorizationBasis(actorSnapshot.authorizationBasis())
                        .authenticationMethod(actorSnapshot.authenticationMethod())
                        .signedAt(signedAt)
                        .reasonText(reasonText)
                        .controlledCopyHashStatus(DccControlledFileSignatureEvidenceServiceImpl.COPY_HASH_STATUS_NOT_APPLICABLE)
                        .signatureImageId(imageSnapshot.getImageId())
                        .signatureImageVersionNo(imageSnapshot.getVersionNo())
                        .signatureImageFileId(imageSnapshot.getFileId())
                        .signatureImageFileUrl(imageSnapshot.getFileUrl())
                        .signatureImageSha256(imageSnapshot.getSha256())
                        .signatureImageContentType(imageSnapshot.getContentType())
                        .signatureImageFileSize(imageSnapshot.getFileSize())
                        .signatureImageStatusSnapshot(imageSnapshot.getImageStatus())
                        .signatureImageVerifiedStatus(imageSnapshot.getVerifiedStatus())
                        .build());
        String subjectId = DccControlledFileSignatureSubjectAdapter.encodeSubjectId(controlledFileId, taskId,
                stageCode, actionType, meaningCode, evidence);
        ElectronicSignatureResult signature = electronicSignatureService.sign(new ElectronicSignatureCommand(
                DccControlledFileSignatureSubjectAdapter.MODULE_CODE,
                actionType,
                DccControlledFileSignatureSubjectAdapter.SUBJECT_TYPE,
                subjectId,
                DccControlledFileSignatureSubjectAdapter.subjectVersion(subjectId),
                password,
                reasonText,
                idempotencyKey(actorId, controlledFileId, taskId, stageCode, actionType, meaningCode, evidence),
                null,
                null));
        DccControlledFileSignatureDO projection = persistSignatureEvidence(actorId, controlledFileId, taskId,
                actionType, reasonText, signedAt, meaningCode, actorSnapshot, evidence, signature);
        signatureImageService.markReferenced(imageSnapshot.getImageId());
        return DccUnifiedSignatureResult.builder()
                .signatureId(projection.getId())
                .controlledFileId(controlledFileId)
                .revisionId(evidence.getRevisionId())
                .versionNo(evidence.getVersionNo())
                .meaningCode(meaningCode)
                .controlledCopyHashStatus(evidence.getControlledCopyHashStatus())
                .evidenceStatus(projection.getEvidenceStatus())
                .evidenceHash(projection.getEvidenceHash())
                .signedAt(projection.getSignedAt())
                .build();
    }

    private DccControlledFileSignatureDO persistSignatureEvidence(Long actorId, Long controlledFileId, String taskId,
                                                                  String actionType, String comment,
                                                                  LocalDateTime signedAt, String meaningCode,
                                                                  SignatureActorSnapshot actorSnapshot,
                                                                  DccControlledFileSignatureEvidence evidence,
                                                                  ElectronicSignatureResult signature) {
        DccControlledFileSignatureDO record = DccControlledFileSignatureDO.builder()
                .controlledFileId(controlledFileId)
                .revisionId(evidence.getRevisionId())
                .versionNo(evidence.getVersionNo())
                .taskId(taskId)
                .actorId(actorId)
                .actorUsernameSnapshot(actorSnapshot.actorUsernameSnapshot())
                .actorNicknameSnapshot(actorSnapshot.actorNicknameSnapshot())
                .actorDeptIdSnapshot(actorSnapshot.actorDeptIdSnapshot())
                .actorDeptNameSnapshot(actorSnapshot.actorDeptNameSnapshot())
                .actorPostNamesSnapshot(actorSnapshot.actorPostNamesSnapshot())
                .actorRoleNamesSnapshot(actorSnapshot.actorRoleNamesSnapshot())
                .signaturePurpose(actorSnapshot.signaturePurpose())
                .authorizationBasis(actorSnapshot.authorizationBasis())
                .authenticationMethod(actorSnapshot.authenticationMethod())
                .recordVersionSnapshot(evidence.getRecordVersionSnapshot())
                .recordHashSnapshot(evidence.getRecordHashSnapshot())
                .clientIpSnapshot(actorSnapshot.clientIpSnapshot())
                .userAgentSnapshot(actorSnapshot.userAgentSnapshot())
                .snapshotStatus(actorSnapshot.snapshotStatus())
                .actionType(actionType)
                .meaningCode(meaningCode)
                .meaningLabel(resolveMeaningLabel(meaningCode))
                .signatureMode(actorSnapshot.authenticationMethod())
                .passwordVerified(Boolean.TRUE)
                .comment(comment)
                .signedAt(signedAt)
                .sourceFileId(evidence.getSourceFileId())
                .sourceFileHash(evidence.getSourceFileHash())
                .sourceFileHashAlgorithm(evidence.getSourceFileHashAlgorithm())
                .sourceFileHashStatus(evidence.getSourceFileHashStatus())
                .controlledCopyFileId(evidence.getControlledCopyFileId())
                .controlledCopyHash(evidence.getControlledCopyHash())
                .controlledCopyHashAlgorithm(evidence.getControlledCopyHashAlgorithm())
                .controlledCopyHashStatus(evidence.getControlledCopyHashStatus())
                .signatureImageId(evidence.getSignatureImageId())
                .signatureImageVersionNo(evidence.getSignatureImageVersionNo())
                .signatureImageFileId(evidence.getSignatureImageFileId())
                .signatureImageFileUrl(evidence.getSignatureImageFileUrl())
                .signatureImageSha256(evidence.getSignatureImageSha256())
                .signatureImageContentType(evidence.getSignatureImageContentType())
                .signatureImageFileSize(evidence.getSignatureImageFileSize())
                .signatureImageStatusSnapshot(evidence.getSignatureImageStatusSnapshot())
                .signatureImageVerifiedStatus(evidence.getSignatureImageVerifiedStatus())
                .evidencePayloadVersion(evidence.getEvidencePayloadVersion())
                .evidenceKeyVersion(evidence.getEvidenceKeyVersion())
                .evidenceHash(evidence.getEvidenceHash())
                .evidenceHashAlgorithm(evidence.getEvidenceHashAlgorithm())
                .evidenceStatus(evidence.getEvidenceStatus())
                .build();
        if (signatureMapper.insert(record) <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        return record;
    }

    private static String requireReasonText(String comment) {
        if (StrUtil.isBlank(comment)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_REASON_REQUIRED);
        }
        return StrUtil.trim(comment);
    }

    private static String resolveMeaningLabel(String meaningCode) {
        return switch (StrUtil.trimToEmpty(meaningCode)) {
            case "APPROVE", "APPLICANT_REWORK_APPROVE", "DOC_CONTROL_REVIEW_APPROVE", "MATRIX_REVIEW_APPROVE",
                    "MATRIX_APPROVAL_APPROVE", "DOC_CONTROL_APPROVAL_APPROVE" -> "审批通过";
            case "REJECT", "APPLICANT_REWORK_REJECT", "DOC_CONTROL_REVIEW_REJECT", "MATRIX_REVIEW_REJECT",
                    "MATRIX_APPROVAL_REJECT", "DOC_CONTROL_APPROVAL_REJECT" -> "审批驳回";
            case "RETURN", "APPLICANT_REWORK_RETURN", "DOC_CONTROL_REVIEW_RETURN", "MATRIX_REVIEW_RETURN",
                    "MATRIX_APPROVAL_RETURN", "DOC_CONTROL_APPROVAL_RETURN" -> "流程回退";
            case "TRANSFER", "APPLICANT_REWORK_TRANSFER", "DOC_CONTROL_REVIEW_TRANSFER", "MATRIX_REVIEW_TRANSFER",
                    "MATRIX_APPROVAL_TRANSFER", "DOC_CONTROL_APPROVAL_TRANSFER" -> "任务转办";
            case "ADD_SIGN", "APPLICANT_REWORK_ADD_SIGN", "DOC_CONTROL_REVIEW_ADD_SIGN", "MATRIX_REVIEW_ADD_SIGN",
                    "MATRIX_APPROVAL_ADD_SIGN", "DOC_CONTROL_APPROVAL_ADD_SIGN" -> "加签";
            case "DISTRIBUTION_ACK" -> "分发确认";
            case "DISTRIBUTION_SIGN" -> "分发签收";
            default -> meaningCode;
        };
    }

    private static String idempotencyKey(Long actorId, Long controlledFileId, String taskId, String stageCode,
                                         String actionType, String meaningCode,
                                         DccControlledFileSignatureEvidence evidence) {
        return "DCC|" + actorId
                + "|" + controlledFileId
                + "|" + taskId
                + "|" + stageCode
                + "|" + actionType
                + "|" + meaningCode
                + "|" + evidence.getRevisionId()
                + "|" + evidence.getVersionNo()
                + "|" + evidence.getEvidenceHash();
    }

    private SignatureActorSnapshot buildActorSnapshot(AdminUserDO user, String meaningCode) {
        if (user == null || user.getId() == null || StrUtil.isBlank(user.getUsername())
                || StrUtil.isBlank(user.getNickname())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        String postNames = resolvePostNames(user);
        if (StrUtil.isBlank(postNames)) {
            throw exception(CONTROLLED_FILE_APPROVER_POST_REQUIRED);
        }
        String roleNames = resolveRoleNames(user.getId());
        if (StrUtil.isBlank(roleNames)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        return new SignatureActorSnapshot(
                user.getUsername(),
                user.getNickname(),
                user.getDeptId(),
                resolveDeptName(user.getDeptId()),
                postNames,
                roleNames,
                meaningCode,
                "DCC电子签名授权启用；系统角色/岗位快照已记录",
                DccControlledFileSignatureModeEnum.PASSWORD.getCode(),
                resolveClientIpSnapshot(),
                resolveUserAgentSnapshot(),
                "CAPTURED");
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        DeptDO dept = deptService.getDept(deptId);
        return dept == null ? null : StrUtil.blankToDefault(StrUtil.trim(dept.getName()), null);
    }

    private String resolvePostNames(AdminUserDO user) {
        Set<Long> postIds = user.getPostIds();
        if (postIds == null || postIds.isEmpty()) {
            return null;
        }
        List<PostDO> posts = postService.getPostList(postIds);
        if (posts == null || posts.isEmpty()) {
            return null;
        }
        return posts.stream()
                .filter(post -> post != null && StrUtil.isNotBlank(post.getName()))
                .sorted(Comparator.comparing(PostDO::getName))
                .map(PostDO::getName)
                .collect(Collectors.joining("、"));
    }

    private String resolveRoleNames(Long actorId) {
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(actorId);
        if (roleIds == null || roleIds.isEmpty()) {
            return null;
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream()
                .filter(role -> role != null && StrUtil.isNotBlank(role.getName()))
                .sorted(Comparator.comparing(RoleDO::getName))
                .map(RoleDO::getName)
                .collect(Collectors.joining("、"));
    }

    private String resolveClientIpSnapshot() {
        HttpServletRequest request = ServletUtils.getRequest();
        return request == null ? null : StrUtil.blankToDefault(ServletUtils.getClientIP(request), null);
    }

    private String resolveUserAgentSnapshot() {
        HttpServletRequest request = ServletUtils.getRequest();
        return request == null ? null : StrUtil.blankToDefault(ServletUtils.getUserAgent(request), null);
    }

    private static String normalizeTaskActionResult(String actionType) {
        if (actionType == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        return switch (actionType) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            case "RETURN" -> "RETURNED";
            case "TRANSFER" -> "TRANSFERRED";
            case "ADD_SIGN" -> "SIGN_ADDED";
            case "DISTRIBUTION_ACK" -> "DISTRIBUTION_ACK";
            case "DISTRIBUTION_SIGN" -> "DISTRIBUTION_SIGN";
            default -> throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        };
    }

    private static String resolveMeaningCode(String stageCode, String actionType) {
        if (stageCode == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        String stagePrefix = switch (stageCode) {
            case "APPLICANT_REWORK" -> "APPLICANT_REWORK";
            case "DOC_CONTROL_REVIEW" -> "DOC_CONTROL_REVIEW";
            case "MATRIX_REVIEW" -> "MATRIX_REVIEW";
            case "MATRIX_APPROVAL" -> "MATRIX_APPROVAL";
            case "DOC_CONTROL_APPROVAL" -> "DOC_CONTROL_APPROVAL";
            case "DISTRIBUTION" -> "DISTRIBUTION";
            default -> throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        };
        if ("DISTRIBUTION".equals(stagePrefix)) {
            return switch (actionType) {
                case "DISTRIBUTION_ACK" -> "DISTRIBUTION_ACK";
                case "DISTRIBUTION_SIGN" -> "DISTRIBUTION_SIGN";
                default -> throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            };
        }
        return stagePrefix + "_" + resolveMeaningSuffix(actionType);
    }

    private static String resolveMeaningSuffix(String actionType) {
        if (actionType == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        return switch (actionType) {
            case "APPROVE" -> "APPROVE";
            case "REJECT" -> "REJECT";
            case "RETURN" -> "RETURN";
            case "TRANSFER" -> "TRANSFER";
            case "ADD_SIGN" -> "ADD_SIGN";
            default -> throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        };
    }

    private record SignatureActorSnapshot(String actorUsernameSnapshot,
                                          String actorNicknameSnapshot,
                                          Long actorDeptIdSnapshot,
                                          String actorDeptNameSnapshot,
                                          String actorPostNamesSnapshot,
                                          String actorRoleNamesSnapshot,
                                          String signaturePurpose,
                                          String authorizationBasis,
                                          String authenticationMethod,
                                          String clientIpSnapshot,
                                          String userAgentSnapshot,
                                          String snapshotStatus) {
    }
}
