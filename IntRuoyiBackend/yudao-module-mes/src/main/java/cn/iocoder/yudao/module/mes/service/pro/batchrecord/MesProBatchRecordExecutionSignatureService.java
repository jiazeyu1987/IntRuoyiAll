package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED;

@Service
public class MesProBatchRecordExecutionSignatureService {

    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REVIEW_APPROVE = "REVIEW_APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_ARCHIVE_SEAL = "ARCHIVE_SEAL";
    public static final String ACTION_FIELD_CHANGE = "FIELD_CHANGE";
    public static final String ACTION_FORM_REVIEW = "FORM_REVIEW";
    public static final String SIGNATURE_MODE_PASSWORD = "PASSWORD";
    public static final String SIGNATURE_TIME_MODE_SERVER = "SERVER_TIME";
    public static final String SIGNATURE_TIME_MODE_USER_SELECTED = "USER_SELECTED";
    public static final String SIGNATURE_TIME_POLICY_VERSION = "EDHR_SIGNATURE_TIME_V1";
    public static final String DEFAULT_SIGNATURE_TIME_ZONE = "Asia/Shanghai";
    private static final String SNAPSHOT_STATUS_CAPTURED = "CAPTURED";
    private static final String SNAPSHOT_STATUS_CAPTURED_PARTIAL_ORG = "CAPTURED_PARTIAL_ORG";
    private static final String AUTHORIZATION_BASIS_FULL = "统一电子签名授权启用；系统角色/岗位快照已记录";
    private static final String AUTHORIZATION_BASIS_PARTIAL_ORG = "统一电子签名授权启用；组织快照缺少岗位/角色配置";

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
    private DccElectronicSignatureAuthorizationService authorizationService;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long recordSubmitSignature(Long executionId, String password, String comment) {
        return recordSignature(executionId, password, comment, ACTION_SUBMIT);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long recordSubmitSignature(Long executionId, String password, String comment,
                                      MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand) {
        return recordSignature(executionId, password, comment, ACTION_SUBMIT, signatureTimeCommand);
    }

    @Transactional(rollbackFor = Exception.class)
    public void attachSubmitSignatureProcessInstance(Long signatureId, Long executionId, String processInstanceId) {
        if (signatureId == null || executionId == null || StrUtil.isBlank(processInstanceId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        MesProBatchRecordExecutionSignatureDO signature = signatureMapper.selectById(signatureId);
        if (signature == null || !executionId.equals(signature.getExecutionId())
                || !StrUtil.equals(ACTION_SUBMIT, signature.getActionType())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        int updated = signatureMapper.updateById(new MesProBatchRecordExecutionSignatureDO()
                .setId(signatureId)
                .setProcessInstanceId(processInstanceId));
        if (updated <= 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long recordApprovalSignature(MesProBatchRecordExecutionApprovalSignatureCommand command) {
        String actionType = resolveApprovalSignatureActionType(command.getApprovalResult());
        return recordSignature(command.getExecutionId(), command.getPassword(), command.getComment(), actionType,
                command.getProcessInstanceId(), command.getBpmTaskId(), command.getBpmTaskDefinitionKey(),
                command.getBpmTaskName(), command.getSignatureCellKey(), command.getSignatureRowIndex(),
                command.getSignatureColumnIndex(), command.getReviewSourceType(), command.getReviewSourceId(),
                command.getReviewSourceName(), command.getApprovalResult(), command.getReason(),
                command.getFieldAuditRevision(), command.getFieldAuditHeadHash(), command.getCellValuesHash(),
                command.getSignatureTimeCommand());
    }

    private String resolveApprovalSignatureActionType(String approvalResult) {
        if (ACTION_APPROVE.equals(approvalResult)) {
            return ACTION_APPROVE;
        }
        if (ACTION_REVIEW_APPROVE.equals(approvalResult)) {
            return ACTION_REVIEW_APPROVE;
        }
        if (ACTION_REJECT.equals(approvalResult)) {
            return ACTION_REJECT;
        }
        throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long recordArchiveSealSignature(Long executionId, String password, String comment) {
        return recordSignature(executionId, password, comment, ACTION_ARCHIVE_SEAL);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long recordArchiveSealSignature(Long executionId, String password, String comment,
                                           MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand) {
        return recordSignature(executionId, password, comment, ACTION_ARCHIVE_SEAL, signatureTimeCommand);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long recordFormReviewSignature(Long executionId, String password, String comment,
                                          Long fieldAuditRevision, String fieldAuditHeadHash, String cellValuesHash) {
        return recordFormReviewSignature(executionId, password, comment, fieldAuditRevision, fieldAuditHeadHash,
                cellValuesHash, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long recordFormReviewSignature(Long executionId, String password, String comment,
                                          Long fieldAuditRevision, String fieldAuditHeadHash, String cellValuesHash,
                                          MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand) {
        if (fieldAuditRevision == null || StrUtil.isBlank(fieldAuditHeadHash) || StrUtil.isBlank(cellValuesHash)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        return recordSignature(executionId, password, comment, ACTION_FORM_REVIEW,
                null, null, null, null, null, null, null, null, null, null, null, null,
                fieldAuditRevision, fieldAuditHeadHash, cellValuesHash, signatureTimeCommand);
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindSignatureFieldAuditEvidence(Long signatureId, Long executionId, Long fieldAuditRevision,
                                                String fieldAuditHeadHash, String cellValuesHash) {
        if (signatureId == null || executionId == null || fieldAuditRevision == null
                || StrUtil.isBlank(fieldAuditHeadHash) || StrUtil.isBlank(cellValuesHash)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        MesProBatchRecordExecutionSignatureDO signature = signatureMapper.selectById(signatureId);
        if (signature == null || !executionId.equals(signature.getExecutionId())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        int updated = signatureMapper.updateById(new MesProBatchRecordExecutionSignatureDO()
                .setId(signatureId)
                .setFieldAuditRevision(fieldAuditRevision)
                .setFieldAuditHeadHash(fieldAuditHeadHash)
                .setCellValuesHash(cellValuesHash));
        if (updated <= 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionFieldAuditSignatureResult recordFieldChangeSignature(
            MesProBatchRecordExecutionFieldAuditSignatureCommand command) {
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        if (actorId == null || !authorizationService.isElectronicSignatureEnabled(actorId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_NOT_AUTHORIZED);
        }
        AdminUserDO user = adminUserService.getUser(actorId);
        if (user == null || StrUtil.isBlank(user.getPassword())
                || !adminUserService.isPasswordMatch(command.getPassword(), user.getPassword())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID);
        }
        LocalDateTime signedAt = nowAtDatabasePrecision();
        SignatureTimeEvidence signatureTimeEvidence =
                buildSignatureTimeEvidence(command.getExecutionId(), ACTION_FIELD_CHANGE, actorId, signedAt,
                        command.getSignatureTimeCommand());
        SignatureActorSnapshot actorSnapshot = buildActorSnapshot(user, ACTION_FIELD_CHANGE, null, null);
        MesProBatchRecordExecutionSignatureDO signature = MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(command.getExecutionId())
                .actorId(actorId)
                .actionType(ACTION_FIELD_CHANGE)
                .signatureMode(SIGNATURE_MODE_PASSWORD)
                .passwordVerified(Boolean.TRUE)
                .comment(StrUtil.blankToDefault(StrUtil.trim(command.getReasonText()), null))
                .signedAt(signedAt)
                .selectedSignedAt(signatureTimeEvidence.selectedSignedAt())
                .signatureDisplayAt(signatureTimeEvidence.signatureDisplayAt())
                .signatureTimeMode(signatureTimeEvidence.signatureTimeMode())
                .selectedTimeZone(signatureTimeEvidence.selectedTimeZone())
                .selectedTimeReason(signatureTimeEvidence.selectedTimeReason())
                .selectedTimePolicyVersion(signatureTimeEvidence.selectedTimePolicyVersion())
                .selectedTimeAuditHash(signatureTimeEvidence.selectedTimeAuditHash())
                .reason(command.getReasonText())
                .actorName(user.getNickname())
                .actorUsernameSnapshot(actorSnapshot.actorUsernameSnapshot())
                .actorNicknameSnapshot(actorSnapshot.actorNicknameSnapshot())
                .actorDeptIdSnapshot(actorSnapshot.actorDeptIdSnapshot())
                .actorDeptNameSnapshot(actorSnapshot.actorDeptNameSnapshot())
                .actorPostNamesSnapshot(actorSnapshot.actorPostNamesSnapshot())
                .actorRoleNamesSnapshot(actorSnapshot.actorRoleNamesSnapshot())
                .signaturePurpose(actorSnapshot.signaturePurpose())
                .authorizationBasis(actorSnapshot.authorizationBasis())
                .authenticationMethod(actorSnapshot.authenticationMethod())
                .recordVersionSnapshot(actorSnapshot.recordVersionSnapshot())
                .recordHashSnapshot(actorSnapshot.recordHashSnapshot())
                .clientIpSnapshot(actorSnapshot.clientIpSnapshot())
                .userAgentSnapshot(actorSnapshot.userAgentSnapshot())
                .snapshotStatus(actorSnapshot.snapshotStatus())
                .reasonCategory(command.getReasonCategory())
                .signatureChallengeHash(command.getSignatureChallengeHash())
                .build();
        int inserted = signatureMapper.insert(signature);
        if (inserted <= 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        return new MesProBatchRecordExecutionFieldAuditSignatureResult()
                .setSignatureId(signature.getId())
                .setActorId(actorId)
                .setActorName(user.getNickname())
                .setSignedAt(signedAt)
                .setSelectedSignedAt(signatureTimeEvidence.selectedSignedAt())
                .setSignatureDisplayAt(signatureTimeEvidence.signatureDisplayAt())
                .setSignatureTimeMode(signatureTimeEvidence.signatureTimeMode())
                .setSelectedTimeZone(signatureTimeEvidence.selectedTimeZone())
                .setSelectedTimeReason(signatureTimeEvidence.selectedTimeReason())
                .setSelectedTimePolicyVersion(signatureTimeEvidence.selectedTimePolicyVersion())
                .setSelectedTimeAuditHash(signatureTimeEvidence.selectedTimeAuditHash());
    }

    @Transactional(rollbackFor = Exception.class)
    public void attachFieldChangeSignature(MesProBatchRecordExecutionFieldAuditSignatureAttachCommand command) {
        if (command.getSignatureId() == null || command.getExecutionId() == null
                || command.getAuditBatchId() == null || StrUtil.isBlank(command.getSignatureChallengeHash())
                || command.getFieldAuditRevision() == null || StrUtil.isBlank(command.getFieldAuditHeadHash())
                || StrUtil.isBlank(command.getCellValuesHash())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        MesProBatchRecordExecutionSignatureDO signature = signatureMapper.selectById(command.getSignatureId());
        if (signature == null || !command.getExecutionId().equals(signature.getExecutionId())
                || !StrUtil.equals(ACTION_FIELD_CHANGE, signature.getActionType())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        int updated = signatureMapper.updateById(new MesProBatchRecordExecutionSignatureDO()
                .setId(command.getSignatureId())
                .setAuditBatchId(command.getAuditBatchId())
                .setSignatureChallengeHash(command.getSignatureChallengeHash())
                .setFieldAuditRevision(command.getFieldAuditRevision())
                .setFieldAuditHeadHash(command.getFieldAuditHeadHash())
                .setCellValuesHash(command.getCellValuesHash()));
        if (updated <= 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
    }

    private Long recordSignature(Long executionId, String password, String comment, String actionType) {
        return recordSignature(executionId, password, comment, actionType, null);
    }

    private Long recordSignature(Long executionId, String password, String comment, String actionType,
                                 MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand) {
        return recordSignature(executionId, password, comment, actionType,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                signatureTimeCommand);
    }

    private Long recordSignature(Long executionId, String password, String comment, String actionType,
                                 String processInstanceId, String bpmTaskId, String bpmTaskDefinitionKey,
                                 String bpmTaskName, String signatureCellKey, Integer signatureRowIndex,
                                 Integer signatureColumnIndex, String reviewSourceType, Long reviewSourceId,
                                 String reviewSourceName, String approvalResult, String reason,
                                 Long fieldAuditRevision, String fieldAuditHeadHash, String cellValuesHash) {
        return recordSignature(executionId, password, comment, actionType, processInstanceId, bpmTaskId,
                bpmTaskDefinitionKey, bpmTaskName, signatureCellKey, signatureRowIndex, signatureColumnIndex,
                reviewSourceType, reviewSourceId, reviewSourceName, approvalResult, reason, fieldAuditRevision,
                fieldAuditHeadHash, cellValuesHash, null);
    }

    private Long recordSignature(Long executionId, String password, String comment, String actionType,
                                 String processInstanceId, String bpmTaskId, String bpmTaskDefinitionKey,
                                 String bpmTaskName, String signatureCellKey, Integer signatureRowIndex,
                                 Integer signatureColumnIndex, String reviewSourceType, Long reviewSourceId,
                                 String reviewSourceName, String approvalResult, String reason,
                                 Long fieldAuditRevision, String fieldAuditHeadHash, String cellValuesHash,
                                 MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand) {
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        if (actorId == null || !authorizationService.isElectronicSignatureEnabled(actorId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_NOT_AUTHORIZED);
        }
        AdminUserDO user = adminUserService.getUser(actorId);
        if (user == null || StrUtil.isBlank(user.getPassword())
                || !adminUserService.isPasswordMatch(password, user.getPassword())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID);
        }
        LocalDateTime signedAt = nowAtDatabasePrecision();
        SignatureTimeEvidence signatureTimeEvidence =
                buildSignatureTimeEvidence(executionId, actionType, actorId, signedAt, signatureTimeCommand);
        SignatureActorSnapshot actorSnapshot =
                buildActorSnapshot(user, actionType, fieldAuditRevision, cellValuesHash);
        MesProBatchRecordExecutionSignatureDO signature = MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(executionId)
                .actorId(actorId)
                .actionType(actionType)
                .signatureMode(SIGNATURE_MODE_PASSWORD)
                .passwordVerified(Boolean.TRUE)
                .comment(StrUtil.blankToDefault(StrUtil.trim(comment), null))
                .signedAt(signedAt)
                .selectedSignedAt(signatureTimeEvidence.selectedSignedAt())
                .signatureDisplayAt(signatureTimeEvidence.signatureDisplayAt())
                .signatureTimeMode(signatureTimeEvidence.signatureTimeMode())
                .selectedTimeZone(signatureTimeEvidence.selectedTimeZone())
                .selectedTimeReason(signatureTimeEvidence.selectedTimeReason())
                .selectedTimePolicyVersion(signatureTimeEvidence.selectedTimePolicyVersion())
                .selectedTimeAuditHash(signatureTimeEvidence.selectedTimeAuditHash())
                .processInstanceId(processInstanceId)
                .bpmTaskId(bpmTaskId)
                .bpmTaskDefinitionKey(bpmTaskDefinitionKey)
                .bpmTaskName(bpmTaskName)
                .signatureCellKey(signatureCellKey)
                .signatureRowIndex(signatureRowIndex)
                .signatureColumnIndex(signatureColumnIndex)
                .reviewSourceType(reviewSourceType)
                .reviewSourceId(reviewSourceId)
                .reviewSourceName(reviewSourceName)
                .approvalResult(approvalResult)
                .reason(StrUtil.blankToDefault(StrUtil.trim(reason), null))
                .actorName(user.getNickname())
                .actorUsernameSnapshot(actorSnapshot.actorUsernameSnapshot())
                .actorNicknameSnapshot(actorSnapshot.actorNicknameSnapshot())
                .actorDeptIdSnapshot(actorSnapshot.actorDeptIdSnapshot())
                .actorDeptNameSnapshot(actorSnapshot.actorDeptNameSnapshot())
                .actorPostNamesSnapshot(actorSnapshot.actorPostNamesSnapshot())
                .actorRoleNamesSnapshot(actorSnapshot.actorRoleNamesSnapshot())
                .signaturePurpose(actorSnapshot.signaturePurpose())
                .authorizationBasis(actorSnapshot.authorizationBasis())
                .authenticationMethod(actorSnapshot.authenticationMethod())
                .recordVersionSnapshot(actorSnapshot.recordVersionSnapshot())
                .recordHashSnapshot(actorSnapshot.recordHashSnapshot())
                .clientIpSnapshot(actorSnapshot.clientIpSnapshot())
                .userAgentSnapshot(actorSnapshot.userAgentSnapshot())
                .snapshotStatus(actorSnapshot.snapshotStatus())
                .fieldAuditRevision(fieldAuditRevision)
                .fieldAuditHeadHash(fieldAuditHeadHash)
                .cellValuesHash(cellValuesHash)
                .build();
        int inserted = signatureMapper.insert(signature);
        if (inserted <= 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        return signature.getId();
    }

    private SignatureActorSnapshot buildActorSnapshot(AdminUserDO user, String actionType,
                                                      Long fieldAuditRevision, String cellValuesHash) {
        if (user == null || user.getId() == null || StrUtil.isBlank(user.getUsername())
                || StrUtil.isBlank(user.getNickname())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        Long actorId = user.getId();
        String postNames = resolvePostNames(user);
        String roleNames = resolveRoleNames(actorId);
        String deptName = resolveDeptName(user.getDeptId());
        boolean partialOrgSnapshot = StrUtil.hasBlank(postNames, roleNames);
        String recordVersion = fieldAuditRevision == null ? null : String.valueOf(fieldAuditRevision);
        String recordHash = StrUtil.blankToDefault(StrUtil.trim(cellValuesHash), null);
        return new SignatureActorSnapshot(
                user.getUsername(),
                user.getNickname(),
                user.getDeptId(),
                deptName,
                postNames,
                roleNames,
                resolveSignaturePurpose(actionType),
                partialOrgSnapshot ? AUTHORIZATION_BASIS_PARTIAL_ORG : AUTHORIZATION_BASIS_FULL,
                SIGNATURE_MODE_PASSWORD,
                recordVersion,
                recordHash,
                resolveClientIpSnapshot(),
                resolveUserAgentSnapshot(),
                partialOrgSnapshot ? SNAPSHOT_STATUS_CAPTURED_PARTIAL_ORG : SNAPSHOT_STATUS_CAPTURED);
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

    private String resolveSignaturePurpose(String actionType) {
        return switch (StrUtil.nullToEmpty(actionType)) {
            case ACTION_SUBMIT -> "提交审批";
            case ACTION_APPROVE -> "最终批准";
            case ACTION_REVIEW_APPROVE -> "审核签名";
            case ACTION_REJECT -> "审批驳回";
            case ACTION_ARCHIVE_SEAL -> "归档封存";
            case ACTION_FIELD_CHANGE -> "字段变更";
            case ACTION_FORM_REVIEW -> "表单复核";
            default -> actionType;
        };
    }

    private String resolveClientIpSnapshot() {
        HttpServletRequest request = ServletUtils.getRequest();
        return request == null ? null : StrUtil.blankToDefault(ServletUtils.getClientIP(request), null);
    }

    private String resolveUserAgentSnapshot() {
        HttpServletRequest request = ServletUtils.getRequest();
        return request == null ? null : StrUtil.blankToDefault(ServletUtils.getUserAgent(request), null);
    }

    private SignatureTimeEvidence buildSignatureTimeEvidence(
            Long executionId,
            String actionType,
            Long actorId,
            LocalDateTime signedAt,
            MesProBatchRecordExecutionSignatureTimeCommand command) {
        LocalDateTime selectedSignedAt = command == null ? null : command.getSelectedSignedAt();
        String signatureTimeMode = selectedSignedAt == null
                ? SIGNATURE_TIME_MODE_SERVER : SIGNATURE_TIME_MODE_USER_SELECTED;
        LocalDateTime displayAt = selectedSignedAt == null
                ? signedAt : selectedSignedAt.truncatedTo(ChronoUnit.SECONDS);
        String selectedTimeZone = DEFAULT_SIGNATURE_TIME_ZONE;
        String selectedTimeReason = "";
        if (selectedSignedAt != null) {
            selectedTimeZone = StrUtil.trim(command.getSelectedTimeZone());
            selectedTimeReason = StrUtil.trim(command.getSelectedTimeReason());
            if (StrUtil.isBlank(selectedTimeZone) || StrUtil.isBlank(selectedTimeReason)) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
            }
        }
        String auditHash = DigestUtil.sha256Hex(String.join("|",
                SIGNATURE_TIME_POLICY_VERSION,
                value(executionId),
                value(actionType),
                value(actorId),
                value(signedAt),
                signatureTimeMode,
                value(displayAt),
                value(selectedSignedAt == null ? null : selectedSignedAt.truncatedTo(ChronoUnit.SECONDS)),
                value(selectedTimeZone),
                value(selectedTimeReason)));
        return new SignatureTimeEvidence(
                selectedSignedAt == null ? null : selectedSignedAt.truncatedTo(ChronoUnit.SECONDS),
                displayAt,
                signatureTimeMode,
                selectedTimeZone,
                selectedTimeReason,
                SIGNATURE_TIME_POLICY_VERSION,
                auditHash);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private LocalDateTime nowAtDatabasePrecision() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private record SignatureTimeEvidence(LocalDateTime selectedSignedAt,
                                          LocalDateTime signatureDisplayAt,
                                          String signatureTimeMode,
                                         String selectedTimeZone,
                                         String selectedTimeReason,
                                         String selectedTimePolicyVersion,
                                          String selectedTimeAuditHash) {
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
                                          String recordVersionSnapshot,
                                          String recordHashSnapshot,
                                          String clientIpSnapshot,
                                          String userAgentSnapshot,
                                          String snapshotStatus) {
    }
}
