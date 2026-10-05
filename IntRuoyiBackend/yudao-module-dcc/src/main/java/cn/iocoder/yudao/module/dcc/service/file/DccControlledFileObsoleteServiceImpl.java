package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileObsoleteAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileObsoleteAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMessageJobStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED;

@Service
@Validated
public class DccControlledFileObsoleteServiceImpl implements DccControlledFileObsoleteService {

    static final String MESSAGE_BUSINESS_TYPE_OBSOLETE = "OBSOLETE";
    private static final String MESSAGE_TEMPLATE_OBSOLETE = "dcc_obsolete";

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileObsoleteAuditMapper obsoleteAuditMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @Resource
    private DccObsoleteFileStorageService obsoleteFileStorageService;
    @Resource
    private DccControlledContentAdapter platformAdapter;
    @Resource
    private FormCenterRuntimeService formCenterRuntimeService;
    @Resource
    private DccControlledFilePendingActionGuard pendingActionGuard;
    @Resource
    private DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver;
    @Resource private DccControlledFileRouteReadinessService routeReadinessService;
    @Resource
    private DccControlledFileNameClaimService nameClaimService;
    @Resource
    private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper taskSnapshotMapper;
    @Resource
    private DccWorkflowObsoleteArchiveRequestService obsoleteArchiveRequestService;
    @Resource
    private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService obsoleteProcessService;
    @Resource
    private DccWorkflowObsoleteEvidenceGuard obsoleteEvidenceGuard;
    @Resource private DccObsoleteRetentionService obsoleteRetentionService;
    @Resource private DccWorkflowFileStateAudit fileStateAudit;
    @Resource private DccApplicationRoundService applicationRoundService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService projectAttributesService;

    @Override
    public void precheckObsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO) {
        requireObsoleteAllowed(userId, id, reqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormInstanceRespVO obsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO) {
        if(reqVO==null || StrUtil.isBlank(reqVO.getReason())) throw exception(CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED);
        if (StrUtil.isBlank(reqVO.getIdempotencyKey())) {
            throw new IllegalArgumentException("DCC obsolete idempotencyKey is required");
        }
        reqVO.setIdempotencyKey(StrUtil.trim(reqVO.getIdempotencyKey()));
        reqVO.setReason(StrUtil.trim(reqVO.getReason()));
        DccControlledFileDO file=controlledFileMapper.selectById(id);
        if(file==null) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        if(!permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.OBSOLETE))
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        var master=controlledFileMasterMapper.selectByIdForUpdate(file.getMasterId());
        file=controlledFileMapper.selectByIdAndTenantForUpdate(
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),id);
        requireLockedObsoleteIdentity(id,master,file);
        if(!permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.OBSOLETE))
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        var replay=formCenterRuntimeService.findBusinessActionByIdempotency(buildObsoleteContext(file,reqVO),reqVO.getIdempotencyKey());
        if(replay!=null) {
            requireMatchingObsoleteReplay(userId,file,reqVO,replay);
            if(!"DRAFT".equals(replay.getStatus())) return replay;
        }
        validateObsoleteAllowed(userId,file,reqVO,true);
        String actionType = DccControlledFileProcessDefinitionKeys.toActionType(
                DccControlledFileProcessDefinitionKeys.OBSOLETE);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute =
                reqVO.getSelectedSignoffDepartmentIds() == null
                        ? approvalRouteAssigneeResolver.resolveRoute(file.getCategoryId(), userId, actionType)
                        : approvalRouteAssigneeResolver.resolveRoute(file.getCategoryId(), userId, actionType,
                                reqVO.getSelectedSignoffDepartmentIds());
        var signoff = resolvedRoute.nodes().stream().filter(node -> "MATRIX_REVIEW".equals(node.stageCode()))
                .findFirst().orElseThrow(() -> new IllegalStateException("作废会签路线缺失"));
        routeReadinessService.requireReadyResolvedRoute(resolvedRoute);
        Map<String, Object> formData = buildObsoleteFormData(file, reqVO);
        formData.put("dccObsoleteSubmitActorId",userId);
        formData.put("dccApplicationPayloadHash",obsoletePayloadHash(userId,file,reqVO));
        // FormCenter builds BPM from the persisted draft JSON, so freeze obligations before createInstance.
        formData.put("dccSignoffDepartmentIds",signoff.candidateSourceIds());
        if(reqVO.getProjectAttributes()!=null) formData.put("projectAttributes",reqVO.getProjectAttributes());
        FormInstanceCreateReqVO createReqVO = new FormInstanceCreateReqVO();
        createReqVO.setContext(buildObsoleteContext(file, reqVO));
        createReqVO.setIdempotencyKey(reqVO.getIdempotencyKey());
        createReqVO.setFormData(formData);
        FormInstanceRespVO draft = formCenterRuntimeService.createInstance(createReqVO, userId);

        FormInstanceSubmitReqVO submitReqVO = new FormInstanceSubmitReqVO();
        submitReqVO.setFormData(formData);
        Map<String, List<Long>> startUserSelectAssignees = approvalRouteAssigneeResolver
                .buildStartUserSelectAssigneeMap(resolvedRoute.nodes());
        Map<String, List<Long>> approveUserSelectAssignees = approvalRouteAssigneeResolver
                .buildApproveUserSelectAssigneeMap(resolvedRoute.nodes());
        submitReqVO.setStartUserSelectAssignees(startUserSelectAssignees);
        submitReqVO.setApproveUserSelectAssignees(approveUserSelectAssignees);
        var submitted = formCenterRuntimeService.submitInstance(draft.getId(), submitReqVO, userId);
        if (submitted.getBpmProcessInstanceId() == null || submitted.getBpmProcessInstanceId().isBlank())
            throw new IllegalStateException("作废审批流程未创建");
        int attributeRound=applicationRoundService.bind(file.getDccProjectCodeId(),"OBSOLETE",file.getId(),submitted.getBpmProcessInstanceId());
        projectAttributesService.beginDraft(userId,file.getDccProjectCodeId(),"OBSOLETE",file.getId(),attributeRound);
        if(reqVO.getProjectAttributes()!=null) projectAttributesService.saveDraft(userId,file.getDccProjectCodeId(),"OBSOLETE",file.getId(),attributeRound,reqVO.getProjectAttributes());
        projectAttributesService.freeze(userId,file.getDccProjectCodeId(),"OBSOLETE",file.getId(),attributeRound);
        for(int index=0;index<signoff.candidateSourceIds().size();index++) {
            Long department = signoff.candidateSourceIds().get(index);
            Long leader = signoff.resolvedUserIds().get(index);
            if (taskSnapshotMapper.insert(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO.builder()
                    .controlledFileId(file.getId()).stageCode("MATRIX_REVIEW").stageNo(1)
                    .departmentId(department).assigneeUserId(leader).leaderUserId(leader)
                    .processInstanceId(submitted.getBpmProcessInstanceId())
                    .obligationId("form-"+draft.getId()+":MATRIX_REVIEW:"+department)
                    .tenantId(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId()).build()) != 1)
                throw new IllegalStateException("作废会签部门冻结失败");
        }
        return submitted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyApprovedObsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getReason())) throw exception(CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED);
        var identity = controlledFileMapper.selectById(id);
        if (identity == null) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        Long tenantId=cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        var master=controlledFileMasterMapper.selectByIdForUpdate(identity.getMasterId());
        DccControlledFileDO file=controlledFileMapper.selectByIdAndTenantForUpdate(tenantId,id);
        requireLockedObsoleteIdentity(id,master,file);
        if(!permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE)) throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        Long approvedBy=requireApprovedObsoleteRound(file,userId,reqVO);
        if("OBSOLETE".equals(file.getStatus())) {
            if(!reqVO.getReason().equals(file.getObsoleteReason())) throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
            return;
        }
        if(!("ACTIVE".equals(file.getStatus()) || "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus())))
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);

        LocalDateTime now = LocalDateTime.now().withNano(0);
        // Preserve the exact locked controlled state even if a downstream write mutates the local object.
        var platformPreimage = new DccControlledFileDO();
        org.springframework.beans.BeanUtils.copyProperties(file,platformPreimage);
        var beforeObsolete = fileStateAudit.capture(file, master);
        obsoleteArchiveRequestService.request(file.getId(),now);
        if (controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .status(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .obsoletedBy(approvedBy)
                .obsoletedTime(now)
                .obsoleteReason(reqVO.getReason())
                .build()) != 1) throw new IllegalStateException("作废状态保存失败");
        obsoleteRetentionService.retain(tenantId,master.getId(),now);

        if (obsoleteAuditMapper.insert(DccControlledFileObsoleteAuditDO.builder()
                .controlledFileId(file.getId())
                .operatorId(approvedBy)
                .obsoleteReason(reqVO.getReason())
                .statusBefore(file.getStatus())
                .statusAfter(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .build()) != 1) throw new IllegalStateException("作废审计保存失败");

        if (file.getId().equals(master.getCurrentActiveControlledFileId())) {
            if(controlledFileMasterMapper.clearCurrentActive(tenantId,master.getId(),file.getId())!=1)
                throw new IllegalStateException("作废执行定位清理失败");
            // Obsolete history still occupies name and number. C owns configured retention/release.
        }

        fileStateAudit.recordApprovedObsolete(beforeObsolete,reqVO.getApprovalProcessInstanceId(),reqVO.getReason());

        for (Long recipientUserId : resolveObsoleteNotificationRecipientUserIds(file, approvedBy)) {
            DccControlledFileMessageJobDO messageJob = DccControlledFileMessageJobDO.builder()
                    .businessType(MESSAGE_BUSINESS_TYPE_OBSOLETE)
                    .businessId(file.getId())
                    .templateCode(MESSAGE_TEMPLATE_OBSOLETE)
                    .recipientUserId(recipientUserId)
                    .status(DccControlledFileMessageJobStatusEnum.PENDING.getCode())
                    .build();
            int inserted = messageJobMapper.insert(messageJob);
            if (inserted <= 0) {
                throw new IllegalStateException("Failed to persist obsolete notification");
            }
            messageDeliveryService.dispatchMessageJob(messageJob, buildObsoleteNotifyParams(file, reqVO.getReason()));
        }
        platformAdapter.recordObsoleted(platformPreimage, approvedBy, reqVO.getReason(), "dcc-obsolete:" + file.getId());
    }

    private void requireLockedObsoleteIdentity(Long id,DccControlledFileMasterDO master,DccControlledFileDO file) {
        Long tenant=cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        if(master==null || file==null || !java.util.Objects.equals(id,file.getId())
                || !java.util.Objects.equals(tenant,file.getTenantId()) || !java.util.Objects.equals(tenant,master.getTenantId())
                || !java.util.Objects.equals(master.getId(),file.getMasterId()))
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
    }

    private BusinessActionContextReqVO buildObsoleteContext(DccControlledFileDO file, DccControlledFileObsoleteReqVO reqVO) {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setDataDomain("DCC");
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId(String.valueOf(file.getId()));
        context.setObjectVersion(file.getVersionNo());
        context.setActionCode("OBSOLETE");
        context.setObjectState(file.getStatus());
        context.setProductCode(file.getProductCode());
        context.setCategoryCode(file.getCategoryId() == null ? null : String.valueOf(file.getCategoryId()));
        context.setReason(reqVO.getReason());
        return context;
    }

    private Long requireApprovedObsoleteRound(DccControlledFileDO file,Long applicant,DccControlledFileObsoleteReqVO request) {
        if(StrUtil.isBlank(request.getApprovalProcessInstanceId()) || StrUtil.isBlank(request.getApprovedVersionNo())
                || !java.util.Objects.equals(file.getVersionNo(),request.getApprovedVersionNo()))
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        var process=obsoleteProcessService.getHistoricProcessInstance(request.getApprovalProcessInstanceId());
        var variables=process==null ? null:process.getProcessVariables();
        if(process==null || !request.getApprovalProcessInstanceId().equals(process.getId())
                || !DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(process.getProcessDefinitionKey())
                || !String.valueOf(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId()).equals(process.getTenantId())
                || !String.valueOf(applicant).equals(process.getStartUserId()) || variables==null
                || !Integer.valueOf(cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum.APPROVE.getStatus()).equals(
                    variables.get(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS))
                || !"DCC".equals(variables.get("systemCode")) || !"CONTROLLED_FILE".equals(variables.get("objectType"))
                || !"OBSOLETE".equals(variables.get("actionCode")) || !String.valueOf(file.getId()).equals(String.valueOf(variables.get("objectId")))
                || !file.getVersionNo().equals(variables.get("objectVersion")))
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        Long approvedBy=obsoleteEvidenceGuard.require(file,request.getApprovalProcessInstanceId(),variables);
        if(approvedBy==null || approvedBy<=0) throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        return approvedBy;
    }

    private Map<String, Object> buildObsoleteFormData(DccControlledFileDO file, DccControlledFileObsoleteReqVO reqVO) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("controlledFileId", file.getId());
        formData.put("reason", reqVO.getReason());
        return formData;
    }

    private void requireMatchingObsoleteReplay(Long userId,DccControlledFileDO file,DccControlledFileObsoleteReqVO reqVO,
            FormInstanceRespVO replay) {
        var context=replay.getContext();
        if(replay.getId()==null || context==null
                || !java.util.Objects.equals(context.getTenantId(),cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                || !"DCC".equals(context.getDataDomain())
                || !"DCC".equals(context.getSystemCode()) || !"CONTROLLED_FILE".equals(context.getObjectType())
                || !"OBSOLETE".equals(context.getActionCode()) || !String.valueOf(file.getId()).equals(context.getObjectId())
                || !java.util.Objects.equals(file.getVersionNo(),context.getObjectVersion())
                || !java.util.Objects.equals(reqVO.getReason(),context.getReason()))
            throw new IllegalArgumentException("作废重放请求与已保存申请身份或原因不一致");
        var snapshots=formCenterRuntimeService.getInstanceSnapshots(replay.getId());
        var snapshot=snapshots==null ? null:snapshots.stream().filter(row -> row!=null
                        && ("SUBMIT".equals(row.getSnapshotType()) || "REWORK_SUBMIT".equals(row.getSnapshotType())
                            || "DRAFT".equals(row.getSnapshotType())) && row.getSnapshotVersion()!=null)
                .max(java.util.Comparator.comparing(cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSnapshotRespVO::getSnapshotVersion))
                .orElse(null);
        var saved=snapshot==null ? null:snapshot.getFormData();
        if(saved==null || !String.valueOf(userId).equals(String.valueOf(saved.get("dccObsoleteSubmitActorId")))
                || !String.valueOf(file.getId()).equals(String.valueOf(saved.get("controlledFileId")))
                || !reqVO.getReason().equals(saved.get("reason"))
                || !obsoletePayloadHash(userId,file,reqVO).equals(saved.get("dccApplicationPayloadHash")))
            throw new IllegalArgumentException("作废重放缺少匹配的申请人、版本及保存载荷证据");
    }

    private String obsoletePayloadHash(Long actor,DccControlledFileDO file,DccControlledFileObsoleteReqVO request) {
        var departments=request.getSelectedSignoffDepartmentIds();
        if(departments!=null && (departments.stream().anyMatch(id->id==null || id<=0)
                || departments.stream().distinct().count()!=departments.size()))
            throw new IllegalArgumentException("作废会签部门输入不合法");
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                java.util.Arrays.asList(actor,file.getId(),file.getVersionNo(),request.getReason(),request.getProjectAttributes(),
                        departments==null?null:departments.stream().sorted().toList())));
    }

    private DccControlledFileDO requireObsoleteAllowed(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO,
            boolean enforcePendingActionGuard) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getReason())) {
            throw exception(CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED);
        }
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        validateObsoleteAllowed(userId,file,reqVO,enforcePendingActionGuard);
        return file;
    }

    private void validateObsoleteAllowed(Long userId,DccControlledFileDO file,DccControlledFileObsoleteReqVO reqVO,
            boolean enforcePendingActionGuard) {
        if(reqVO==null || StrUtil.isBlank(reqVO.getReason())) throw exception(CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED);
        if (!(DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.CONTROLLED_PENDING_EFFECTIVE.getStatus().equals(file.getStatus()))
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE)) {
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        }
        if (enforcePendingActionGuard) {
            pendingActionGuard.assertNoPendingBusinessAction(file);
        }
    }

    private Map<String, Object> buildObsoleteNotifyParams(DccControlledFileDO file, String obsoleteReason) {
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("title", StrUtil.blankToDefault(file.getTitle(), file.getFileName()));
        params.put("version", StrUtil.blankToDefault(file.getVersionNo(), "-"));
        params.put("reason", StrUtil.blankToDefault(obsoleteReason, "-"));
        return params;
    }

    private Set<Long> resolveObsoleteNotificationRecipientUserIds(DccControlledFileDO file, Long operatorUserId) {
        Set<Long> userIds = resolveAffectedRecipientUserIds(file.getId());
        if (userIds.isEmpty()) {
            addFirstPresent(userIds, file.getRequesterId(), file.getSubmitterId(), operatorUserId);
        }
        return userIds;
    }

    private Set<Long> resolveAffectedRecipientUserIds(Long controlledFileId) {
        Set<Long> userIds = new LinkedHashSet<>();
        distributionMapper.selectListByControlledFileId(controlledFileId).forEach(distribution ->
                distributionRecipientMapper.selectListByDistributionId(distribution.getId()).stream()
                        .map(DccControlledFileDistributionRecipientDO::getUserId)
                        .forEach(userIds::add));
        trainingMapper.selectListByControlledFileId(controlledFileId).forEach(training ->
                trainingAssignmentMapper.selectListByTrainingId(training.getId()).stream()
                        .map(DccControlledFileTrainingAssignmentDO::getUserId)
                        .forEach(userIds::add));
        return userIds;
    }

    private void addFirstPresent(Set<Long> userIds, Long... candidateUserIds) {
        for (Long userId : candidateUserIds) {
            if (userId != null) {
                userIds.add(userId);
                return;
            }
        }
    }
}
