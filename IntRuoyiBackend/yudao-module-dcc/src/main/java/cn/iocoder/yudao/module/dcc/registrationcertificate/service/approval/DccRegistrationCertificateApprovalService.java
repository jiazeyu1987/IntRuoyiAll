package cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateBpmBindingMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateGrantMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateBpmBindingDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.grant.DccRegistrationCertificateGrantService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_BPM_CANDIDATE_EMPTY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_APPROVAL_REJECT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVAL_PERMISSION;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVAL_TASK_DEFINITION_KEY;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.APPROVER_ROLE_CODE;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.BUSINESS_KEY_PREFIX;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.PROCESS_DEFINITION_KEY;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.REQUEST_TYPE_UPLOAD_CERTIFICATE;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.UPLOAD_APPROVAL_PERMISSION;

@Service
public class DccRegistrationCertificateApprovalService {

    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_BPM_BOUND = "BPM_BOUND";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String BINDING_RUNNING = "RUNNING";
    private static final String BINDING_APPROVED = "APPROVED";
    private static final String BINDING_REJECTED = "REJECTED";
    private static final String BINDING_WITHDRAWN = "WITHDRAWN";
    private static final String OPERATION_RENEWAL_CERTIFICATE = "RENEWAL_CERTIFICATE";
    private static final String OPERATION_UPLOAD_CERTIFICATE = "UPLOAD_CERTIFICATE";
    private static final String OPERATION_CHANGE_CERTIFICATE = "CHANGE_CERTIFICATE";
    private final DccRegistrationCertificateAccessRequestMapper requestMapper;
    private final DccRegistrationCertificateBpmBindingMapper bindingMapper;
    private final DccRegistrationCertificateGrantMapper grantMapper;
    private final DccRegistrationCertificateGrantService grantService;
    private final BpmProcessInstanceApi bpmProcessInstanceApi;
    private final MdmCompanyScopeApi companyScopeApi;
    private final RoleApi roleApi;
    private final PermissionApi permissionApi;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final DccRegistrationCertificateRenewalService renewalService;
    private final DccRegistrationCertificateUploadService uploadService;
    private final DccRegistrationCertificateChangeService changeService;

    @Autowired
    public DccRegistrationCertificateApprovalService(
            DccRegistrationCertificateAccessRequestMapper requestMapper,
            DccRegistrationCertificateBpmBindingMapper bindingMapper,
            DccRegistrationCertificateGrantMapper grantMapper,
            DccRegistrationCertificateGrantService grantService,
            BpmProcessInstanceApi bpmProcessInstanceApi,
            MdmCompanyScopeApi companyScopeApi,
            RoleApi roleApi,
            PermissionApi permissionApi,
            DccRegistrationCertificateBusinessClock businessClock,
            DccRegistrationCertificateRenewalService renewalService,
            DccRegistrationCertificateUploadService uploadService,
            DccRegistrationCertificateChangeService changeService) {
        this.requestMapper = require(requestMapper, "requestMapper");
        this.bindingMapper = require(bindingMapper, "bindingMapper");
        this.grantMapper = require(grantMapper, "grantMapper");
        this.grantService = require(grantService, "grantService");
        this.bpmProcessInstanceApi = require(bpmProcessInstanceApi, "bpmProcessInstanceApi");
        this.companyScopeApi = require(companyScopeApi, "companyScopeApi");
        this.roleApi = require(roleApi, "roleApi");
        this.permissionApi = require(permissionApi, "permissionApi");
        this.businessClock = require(businessClock, "businessClock");
        this.renewalService = require(renewalService, "renewalService");
        this.uploadService = require(uploadService, "uploadService");
        this.changeService = require(changeService, "changeService");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateApprovalResult startNativeApproval(
            Long tenantId, Long actorId, DccRegistrationCertificateApprovalStartCommand command) {
        if (tenantId == null || actorId == null || command == null || command.requestId() == null
                || command.requestId() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(command.requestId());
        requireRequestTenant(request, tenantId);
        DccRegistrationCertificateBpmBindingDO existing = bindingMapper.selectByRequestId(tenantId, request.getId());
        if (existing != null) {
            requireReplayableBinding(request, existing);
            return result(request, existing, grantIds(tenantId, request.getId()));
        }
        if (!STATUS_SUBMITTED.equals(request.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        RoleRespDTO approverRole = roleApi.getRoleByCode(APPROVER_ROLE_CODE);
        if (approverRole == null || approverRole.getId() == null || approverRole.getId() <= 0
                || !APPROVER_ROLE_CODE.equals(approverRole.getCode())
                || !CommonStatusEnum.isEnable(approverRole.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_CANDIDATE_EMPTY);
        }
        List<Long> candidates = REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())
                ? resolveUploadApprovalCandidates(approverRole.getId(), actorId)
                : resolveScopedApprovalCandidates(request, approverRole.getId(), actorId);
        if (candidates.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_CANDIDATE_EMPTY);
        }
        String businessKey = BUSINESS_KEY_PREFIX + request.getId();
        BpmProcessInstanceCreateReqDTO bpmRequest = new BpmProcessInstanceCreateReqDTO();
        bpmRequest.setProcessDefinitionKey(PROCESS_DEFINITION_KEY);
        bpmRequest.setBusinessKey(businessKey);
        bpmRequest.setStartUserSelectAssignees(Map.of(APPROVAL_TASK_DEFINITION_KEY, candidates));
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("registrationCertificateAccessRequestId", request.getId());
        variables.put("requestId", request.getId());
        variables.put("certificateId", request.getCertificateId());
        variables.put("ownerCompanyId", request.getOwnerCompanyId());
        variables.put("requestType", request.getRequestType());
        variables.put("requestKey", request.getRequestKey());
        if (REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            addRegistrationCertificateSummaryVariables(variables, request);
        }
        bpmRequest.setVariables(variables);
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(actorId, bpmRequest);
        if (isBlank(processInstanceId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        DccRegistrationCertificateBpmBindingDO binding = DccRegistrationCertificateBpmBindingDO.builder()
                .requestId(request.getId()).businessKey(businessKey).bpmProcessInstanceId(processInstanceId.trim())
                .status(BINDING_RUNNING).createdAt(businessClock.now())
                .detailJson(JsonUtils.toJsonString(Map.of("candidateUserIds", candidates))).build();
        binding.setTenantId(tenantId);
        try {
            if (bindingMapper.insert(binding) != 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
            }
            request.setBpmProcessInstanceId(processInstanceId.trim());
            request.setStatus(STATUS_BPM_BOUND);
            if (requestMapper.updateById(request) != 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
            }
        } catch (DuplicateKeyException ex) {
            cancelCreatedProcess(actorId, processInstanceId.trim(),
                    "注册证访问审批绑定重复", ex);
            DccRegistrationCertificateBpmBindingDO winner = bindingMapper.selectByRequestId(tenantId, request.getId());
            if (winner != null && Objects.equals(winner.getBusinessKey(), businessKey)) {
                DccRegistrationCertificateAccessRequestDO winnerRequest = requestMapper.selectById(request.getId());
                requireRequestTenant(winnerRequest, tenantId);
                requireReplayableBinding(winnerRequest, winner);
                return result(winnerRequest, winner, grantIds(tenantId, request.getId()));
            }
            throw ex;
        } catch (RuntimeException ex) {
            cancelCreatedProcess(actorId, processInstanceId.trim(),
                    "注册证访问审批数据保存失败", ex);
            throw ex;
        }
        return result(request, binding, List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateApprovalResult approve(
            Long tenantId, Long approverId, DccRegistrationCertificateApprovalCallbackCommand command) {
        DccRegistrationCertificateBpmBindingDO binding = requireBinding(tenantId, command);
        DccRegistrationCertificateAccessRequestDO request = requireBoundRequest(tenantId, binding);
        if (STATUS_APPROVED.equals(request.getStatus())) {
            requireTerminalApprovalKey(binding, command.approvalKey());
            return result(request, binding, grantIds(tenantId, request.getId()));
        }
        if (!STATUS_BPM_BOUND.equals(request.getStatus()) || !BINDING_RUNNING.equals(binding.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID);
        }
        LocalDateTime decidedAt = command.decidedAt() == null ? businessClock.now() : command.decidedAt();
        request.setStatus(STATUS_APPROVED);
        request.setCompletedAt(decidedAt);
        binding.setStatus(BINDING_APPROVED);
        binding.setCompletedAt(decidedAt);
        recordTerminalApprovalKey(binding, command.approvalKey());
        requireUpdated(requestMapper.updateById(request));
        requireUpdated(bindingMapper.updateById(binding));
        if (isRenewalUploadRequest(request)) {
            renewalService.approveRenewalRequest(tenantId, approverId, request.getId(), command.approvalKey().trim());
            return result(request, binding, List.of());
        }
        if (isInitialUploadRequest(request)) {
            uploadService.approveUploadRequest(tenantId, approverId, request.getId(), command.approvalKey().trim());
            return result(request, binding, List.of());
        }
        if (isChangeUploadRequest(request)) {
            changeService.approveChangeRequest(tenantId, approverId, request.getId(), command.approvalKey().trim(), decidedAt);
            return result(request, binding, List.of());
        }
        if (REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID);
        }
        List<DccRegistrationCertificateGrantDO> grants = grantService.createGrantsForApprovedRequest(
                tenantId, approverId, request.getId(), command.approvalKey().trim(), decidedAt);
        return result(request, binding, grants.stream().map(DccRegistrationCertificateGrantDO::getId).toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateApprovalResult reject(
            Long tenantId, Long approverId, DccRegistrationCertificateApprovalCallbackCommand command) {
        if (command == null || isBlank(command.rejectReason())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_APPROVAL_REJECT_REASON_REQUIRED);
        }
        DccRegistrationCertificateBpmBindingDO binding = requireBinding(tenantId, command);
        DccRegistrationCertificateAccessRequestDO request = requireBoundRequest(tenantId, binding);
        if (STATUS_REJECTED.equals(request.getStatus())) {
            requireTerminalApprovalKey(binding, command.approvalKey());
            return result(request, binding, List.of());
        }
        if (!STATUS_BPM_BOUND.equals(request.getStatus()) || !BINDING_RUNNING.equals(binding.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID);
        }
        LocalDateTime decidedAt = command.decidedAt() == null ? businessClock.now() : command.decidedAt();
        request.setStatus(STATUS_REJECTED);
        request.setRejectReason(command.rejectReason().trim());
        request.setCompletedAt(decidedAt);
        binding.setStatus(BINDING_REJECTED);
        binding.setCompletedAt(decidedAt);
        recordTerminalApprovalKey(binding, command.approvalKey());
        requireUpdated(requestMapper.updateById(request));
        requireUpdated(bindingMapper.updateById(binding));
        if (isRenewalUploadRequest(request)) {
            renewalService.rejectRenewalRequest(tenantId, approverId, request.getId(), command.rejectReason().trim());
        } else if (isInitialUploadRequest(request)) {
            uploadService.rejectUploadRequest(tenantId, approverId, request.getId(), command.approvalKey().trim(),
                    command.rejectReason().trim());
        } else if (isChangeUploadRequest(request)) {
            changeService.rejectChangeRequest(tenantId, approverId, request.getId(), decidedAt);
        } else if (REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_REQUEST_STATUS_INVALID);
        }
        return result(request, binding, List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateApprovalResult withdraw(
            Long tenantId, Long actorId, Long requestId, String reason) {
        if (tenantId == null || actorId == null || requestId == null || requestId <= 0 || isBlank(reason)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT);
        }
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(requestId);
        requireRequestTenant(request, tenantId);
        if (!Objects.equals(actorId, request.getRequesterUserId())
                || (!STATUS_SUBMITTED.equals(request.getStatus()) && !STATUS_BPM_BOUND.equals(request.getStatus()))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT);
        }
        DccRegistrationCertificateBpmBindingDO binding = bindingMapper.selectByRequestId(tenantId, requestId);
        if (STATUS_BPM_BOUND.equals(request.getStatus())
                && (binding == null || !BINDING_RUNNING.equals(binding.getStatus()))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT);
        }
        if (binding != null && BINDING_RUNNING.equals(binding.getStatus())) {
            String nativeCancelKey = "BPM:" + binding.getBpmProcessInstanceId() + ":4";
            binding.setStatus(BINDING_WITHDRAWN);
            binding.setCompletedAt(businessClock.now());
            recordTerminalApprovalKey(binding, nativeCancelKey);
            requireUpdated(bindingMapper.updateById(binding));
        }
        request.setStatus(STATUS_WITHDRAWN);
        request.setWithdrawnAt(businessClock.now());
        request.setWithdrawReason(reason.trim());
        request.setCompletedAt(request.getWithdrawnAt());
        requireUpdated(requestMapper.updateById(request));
        if (binding != null) {
            bpmProcessInstanceApi.cancelProcessInstance(actorId, binding.getBpmProcessInstanceId(), reason.trim());
        }
        return result(request, binding, List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateApprovalResult cancelFromNative(
            Long tenantId, Long actorId, DccRegistrationCertificateApprovalCallbackCommand command) {
        if (command == null || isBlank(command.rejectReason())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT);
        }
        DccRegistrationCertificateBpmBindingDO binding = requireBinding(tenantId, command);
        DccRegistrationCertificateAccessRequestDO request = requireBoundRequest(tenantId, binding);
        if (!Objects.equals(actorId, request.getRequesterUserId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT);
        }
        if (STATUS_WITHDRAWN.equals(request.getStatus())) {
            if (!BINDING_WITHDRAWN.equals(binding.getStatus())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT);
            }
            requireTerminalApprovalKey(binding, command.approvalKey());
            return result(request, binding, List.of());
        }
        if (!STATUS_BPM_BOUND.equals(request.getStatus()) || !BINDING_RUNNING.equals(binding.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_WITHDRAW_CONFLICT);
        }
        LocalDateTime decidedAt = command.decidedAt() == null ? businessClock.now() : command.decidedAt();
        request.setStatus(STATUS_WITHDRAWN);
        request.setWithdrawnAt(decidedAt);
        request.setCompletedAt(decidedAt);
        request.setWithdrawReason(command.rejectReason().trim());
        binding.setStatus(BINDING_WITHDRAWN);
        binding.setCompletedAt(decidedAt);
        recordTerminalApprovalKey(binding, command.approvalKey());
        requireUpdated(requestMapper.updateById(request));
        requireUpdated(bindingMapper.updateById(binding));
        return result(request, binding, List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeGrant(Long tenantId, Long actorId, Long grantId, String reason) {
        if (tenantId == null || actorId == null || grantId == null || grantId <= 0 || isBlank(reason)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_STATUS_INVALID);
        }
        DccRegistrationCertificateGrantDO grant = grantMapper.selectById(grantId);
        if (grant == null || !Objects.equals(tenantId, grant.getTenantId())
                || !"ACTIVE".equals(grant.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_STATUS_INVALID);
        }
        if (!isReviewer(grant.getOwnerCompanyId(), actorId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_STATUS_INVALID);
        }
        grant.setStatus("REVOKED");
        grant.setRevokedAt(businessClock.now());
        grant.setRevokedBy(actorId);
        grant.setRevokeReason(reason.trim());
        if (grantMapper.updateById(grant) != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_STATUS_INVALID);
        }
    }

    @Transactional(readOnly = true)
    public DccRegistrationCertificateAccessRequestStatus getStatus(
            Long tenantId, Long actorId, Long requestId) {
        if (tenantId == null || actorId == null || requestId == null || requestId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(requestId);
        requireRequestTenant(request, tenantId);
        if (!Objects.equals(actorId, request.getRequesterUserId())
                && !isReviewer(request.getOwnerCompanyId(), actorId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        DccRegistrationCertificateBpmBindingDO binding = bindingMapper.selectByRequestId(tenantId, requestId);
        List<DccRegistrationCertificateGrantStatus> grants = grantMapper.selectByRequest(tenantId, requestId).stream()
                .map(grant -> new DccRegistrationCertificateGrantStatus(
                        grant.getId(), grant.getRequestFileId(), grant.getBusinessFileId(), grant.getGrantType(),
                        grant.getStatus(), grant.getGrantedAt(), grant.getExpiresAt(), grant.getRevokedAt(),
                        grant.getRevokeReason()))
                .toList();
        return new DccRegistrationCertificateAccessRequestStatus(
                request.getId(), request.getCertificateId(), request.getOwnerCompanyId(),
                request.getRequesterUserId(), request.getRequestType(), request.getPurpose(),
                request.getProjectCodeId(), request.getStatus(), request.getBpmProcessInstanceId(),
                binding == null ? null : binding.getStatus(), request.getRequestedAt(), request.getCompletedAt(),
                request.getWithdrawnAt(), request.getWithdrawReason(), request.getRejectReason(), grants);
    }

    private DccRegistrationCertificateBpmBindingDO requireBinding(
            Long tenantId, DccRegistrationCertificateApprovalCallbackCommand command) {
        if (tenantId == null || command == null || isBlank(command.bpmProcessInstanceId())
                || isBlank(command.approvalKey())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        DccRegistrationCertificateBpmBindingDO binding = bindingMapper.selectByProcessInstanceId(
                tenantId, command.bpmProcessInstanceId().trim());
        if (binding == null || !Objects.equals(tenantId, binding.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        return binding;
    }

    private DccRegistrationCertificateAccessRequestDO requireBoundRequest(
            Long tenantId, DccRegistrationCertificateBpmBindingDO binding) {
        DccRegistrationCertificateAccessRequestDO request = requestMapper.selectById(binding.getRequestId());
        requireRequestTenant(request, tenantId);
        return request;
    }

    private static void requireRequestTenant(DccRegistrationCertificateAccessRequestDO request, Long tenantId) {
        if (request == null || !Objects.equals(tenantId, request.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
    }

    private static void requireReplayableBinding(DccRegistrationCertificateAccessRequestDO request,
                                                 DccRegistrationCertificateBpmBindingDO binding) {
        if (!STATUS_BPM_BOUND.equals(request.getStatus())
                || !Objects.equals(BUSINESS_KEY_PREFIX + request.getId(), binding.getBusinessKey())
                || isBlank(binding.getBpmProcessInstanceId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
    }

    private List<Long> normalizeCandidates(Collection<Long> rawCandidates, Long actorId) {
        if (rawCandidates == null) {
            return List.of();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        rawCandidates.stream().filter(Objects::nonNull).filter(candidate -> !Objects.equals(candidate, actorId))
                .forEach(unique::add);
        List<Long> candidates = new ArrayList<>(unique);
        candidates.sort(Comparator.naturalOrder());
        return candidates;
    }

    private List<Long> resolveScopedApprovalCandidates(
            DccRegistrationCertificateAccessRequestDO request, Long roleId, Long actorId) {
        Set<Long> rawCandidates = companyScopeApi.resolveRecipientUserIds(
                request.getOwnerCompanyId(), List.of(roleId), APPROVAL_PERMISSION);
        return normalizeCandidates(rawCandidates, actorId);
    }

    private List<Long> resolveUploadApprovalCandidates(Long roleId, Long actorId) {
        if (!permissionApi.hasAnyPermissionsInRoles(List.of(roleId), UPLOAD_APPROVAL_PERMISSION)) {
            return List.of();
        }
        Set<Long> rawCandidates = permissionApi.getUserRoleIdListByRoleIds(List.of(roleId));
        return normalizeCandidates(rawCandidates, actorId);
    }

    private List<Long> grantIds(Long tenantId, Long requestId) {
        return grantMapper.selectByRequest(tenantId, requestId).stream()
                .map(DccRegistrationCertificateGrantDO::getId).toList();
    }

    private boolean isReviewer(Long ownerCompanyId, Long actorId) {
        if (ownerCompanyId == null || actorId == null) {
            return false;
        }
        RoleRespDTO approverRole = roleApi.getRoleByCode(APPROVER_ROLE_CODE);
        if (approverRole == null || approverRole.getId() == null || approverRole.getId() <= 0
                || !APPROVER_ROLE_CODE.equals(approverRole.getCode())
                || !CommonStatusEnum.isEnable(approverRole.getStatus())) {
            return false;
        }
        Set<Long> candidates = companyScopeApi.resolveRecipientUserIds(
                ownerCompanyId, List.of(approverRole.getId()), APPROVAL_PERMISSION);
        return candidates != null && candidates.contains(actorId);
    }

    private void cancelCreatedProcess(Long actorId, String processInstanceId, String reason, RuntimeException original) {
        try {
            bpmProcessInstanceApi.cancelProcessInstance(actorId, processInstanceId, reason);
        } catch (RuntimeException cancelFailure) {
            original.addSuppressed(cancelFailure);
            throw original;
        }
    }

    private void recordTerminalApprovalKey(DccRegistrationCertificateBpmBindingDO binding, String approvalKey) {
        Map<String, Object> detail = readBindingDetail(binding);
        detail.put("terminalApprovalKey", approvalKey.trim());
        binding.setDetailJson(JsonUtils.toJsonString(detail));
    }

    private void requireTerminalApprovalKey(DccRegistrationCertificateBpmBindingDO binding, String approvalKey) {
        Object recorded = readBindingDetail(binding).get("terminalApprovalKey");
        if (!Objects.equals(recorded, approvalKey.trim())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
    }

    private Map<String, Object> readBindingDetail(DccRegistrationCertificateBpmBindingDO binding) {
        Map<?, ?> parsed = isBlank(binding.getDetailJson())
                ? Map.of() : JsonUtils.parseObject(binding.getDetailJson(), Map.class);
        Map<String, Object> detail = new LinkedHashMap<>();
        if (parsed != null) {
            parsed.forEach((key, value) -> detail.put(String.valueOf(key), value));
        }
        return detail;
    }

    private static DccRegistrationCertificateApprovalResult result(
            DccRegistrationCertificateAccessRequestDO request,
            DccRegistrationCertificateBpmBindingDO binding,
            List<Long> grantIds) {
        return new DccRegistrationCertificateApprovalResult(request.getId(),
                binding == null ? BUSINESS_KEY_PREFIX + request.getId() : binding.getBusinessKey(),
                binding == null ? request.getBpmProcessInstanceId() : binding.getBpmProcessInstanceId(),
                request.getStatus(), grantIds == null ? List.of() : grantIds);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isRenewalUploadRequest(DccRegistrationCertificateAccessRequestDO request) {
        if (!REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            return false;
        }
        return OPERATION_RENEWAL_CERTIFICATE.equals(resolveRequestOperation(request));
    }

    private static boolean isInitialUploadRequest(DccRegistrationCertificateAccessRequestDO request) {
        if (!REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            return false;
        }
        return OPERATION_UPLOAD_CERTIFICATE.equals(resolveRequestOperation(request));
    }

    private static boolean isChangeUploadRequest(DccRegistrationCertificateAccessRequestDO request) {
        if (!REQUEST_TYPE_UPLOAD_CERTIFICATE.equals(request.getRequestType())) {
            return false;
        }
        return OPERATION_CHANGE_CERTIFICATE.equals(resolveRequestOperation(request));
    }

    private static String resolveRequestOperation(DccRegistrationCertificateAccessRequestDO request) {
        Map<?, ?> parsed = isBlank(request.getDetailJson())
                ? Map.of() : JsonUtils.parseObject(request.getDetailJson(), Map.class);
        if (parsed == null || parsed.get("operation") == null) {
            return null;
        }
        String operation = String.valueOf(parsed.get("operation")).trim();
        return operation.isEmpty() ? null : operation;
    }

    private static void addRegistrationCertificateSummaryVariables(
            Map<String, Object> variables, DccRegistrationCertificateAccessRequestDO request) {
        Map<?, ?> detail = isBlank(request.getDetailJson())
                ? Map.of() : JsonUtils.parseObject(request.getDetailJson(), Map.class);
        if (detail == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        String operation = requireSummaryText(detail, "operation");
        if (!OPERATION_UPLOAD_CERTIFICATE.equals(operation)
                && !OPERATION_RENEWAL_CERTIFICATE.equals(operation)
                && !OPERATION_CHANGE_CERTIFICATE.equals(operation)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        variables.put("requestOperation", operation);
        if (OPERATION_CHANGE_CERTIFICATE.equals(operation)) {
            return;
        }
        variables.put("certificateNo", requireSummaryText(detail, "certificateNo"));
        variables.put("classification", requireSummaryText(detail, "classification"));
        variables.put("productName", requireSummaryText(detail, "productName"));
        variables.put("ownerCompanyName", requireSummaryText(detail, "ownerCompanyName"));
    }

    private static String requireSummaryText(Map<?, ?> detail, String key) {
        Object value = detail.get(key);
        if (value == null || isBlank(String.valueOf(value))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        return String.valueOf(value).trim();
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }

    private static void requireUpdated(int updated) {
        if (updated != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
    }
}
