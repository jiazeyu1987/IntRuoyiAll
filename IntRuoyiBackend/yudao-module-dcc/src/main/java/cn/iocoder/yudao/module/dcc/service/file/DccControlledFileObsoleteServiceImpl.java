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
    @Resource private cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper obsoleteForms;
    @Resource private cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper obsoletePolicies;
    @Resource private DccObsoleteFormExecutionPreparationService executionPreparation;
    private record DraftContext(DccControlledFileDO file,cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO form,int number,
            cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO attributes) { }
    @Override
    @Transactional(readOnly=true,rollbackFor=Exception.class)
    public cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO getObsoleteApplicationDraft(Long actor,Long fileId){
        requireAuthenticatedApplicant(actor);var file=controlledFileMapper.selectById(fileId);
        if(file==null||!java.util.Objects.equals(file.getTenantId(),cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())||Boolean.TRUE.equals(file.getDeleted()))throw exception(CONTROLLED_FILE_NOT_EXISTS);
        if(!Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE").contains(file.getStatus())||file.getControlledTime()==null
                ||!permissionSupport.hasCategoryPermission(file.getCategoryId(),actor,DccFileCategoryPermissionActionEnum.OBSOLETE))throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        var rows=obsoleteForms.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO>()
                .eq("tenant_id",file.getTenantId()).eq("applicant_user_id",actor).eq("system_code","DCC").eq("object_type","CONTROLLED_FILE").eq("object_id",fileId.toString())
                .eq("object_version",file.getVersionNo()).eq("action_code","OBSOLETE").eq("status","DRAFT").isNull("bpm_process_instance_id").eq("deleted",false));
        if(rows.isEmpty()){
            var terminated=obsoleteForms.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO>()
                    .eq("tenant_id",file.getTenantId()).eq("applicant_user_id",actor).eq("system_code","DCC").eq("object_type","CONTROLLED_FILE").eq("object_id",fileId.toString()).eq("object_version",file.getVersionNo())
                    .eq("action_code","OBSOLETE").in("status",List.of("REJECTED","ABANDONED")).isNotNull("bpm_process_instance_id").eq("deleted",false));
            if(terminated.isEmpty())return null;
            if(terminated.size()==1){var form=terminated.get(0);int number=applicationRoundService.require("OBSOLETE",fileId,form.getBpmProcessInstanceId());return reworkSourceResponse(executionPreparation.readReworkSource(actor,fileId,form.getId(),form.getInstanceCode(),form.getBpmProcessInstanceId(),number));}
            var candidates=new java.util.ArrayList<cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReworkSourceVO>();
            for(var form:terminated){int number=applicationRoundService.require("OBSOLETE",fileId,form.getBpmProcessInstanceId());
                try{candidates.add(reworkSourceCandidate(executionPreparation.readReworkSource(actor,fileId,form.getId(),form.getInstanceCode(),form.getBpmProcessInstanceId(),number)));}
                catch(DccObsoleteFormExecutionPreparationService.ReworkSourceUnavailableException unavailable){
                    var candidate=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReworkSourceVO();candidate.setReworkSourceFormInstanceId(form.getId());candidate.setReworkSourceFormInstanceCode(form.getInstanceCode());candidate.setReworkSourceBpmProcessInstanceId(form.getBpmProcessInstanceId());candidate.setReworkSourceAttributeRound(number);candidate.setReworkSourceStatus(form.getStatus());candidate.setReworkSourceVersionNo(form.getObjectVersion());var saved=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(form.getFormDataJson(),Map.class);candidate.setReason(saved==null?null:(String)saved.get("reason"));candidate.setAvailable(false);candidate.setUnavailableReason(unavailable.getMessage());candidates.add(candidate);
                }}
            var result=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO();result.setControlledFileId(fileId);result.setDccProjectCodeId(file.getDccProjectCodeId());result.setVersionNo(file.getVersionNo());
            result.setApplicationEntryMode("SOURCE_SELECTION_REQUIRED");result.setPreparationReady(false);result.setApprovalMatrixChanged(false);result.setUnavailableReason("DCC_OBSOLETE_REWORK_SOURCE_SELECTION_REQUIRED");result.setReworkSourceCandidates(candidates);return result;
        }if(rows.size()!=1)throw new IllegalStateException("作废同版本申请人未提交表单不唯一，不能选择第一条");
        var form=rows.get(0);int number=applicationRoundService.requireObsoleteFormDraft(actor,fileId,form.getId(),form.getInstanceCode());
        var saved=projectAttributesService.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number);return draftResponse(formResponse(form),file,number,saved);
    }
    @Override
    @Transactional(readOnly=true,rollbackFor=Exception.class)
    public cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO getObsoleteReworkSource(Long actor,Long fileId,
            cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReworkSourceReqVO expected){
        if(expected==null||!expected.hasSource()||!expected.isSourceIdentityComplete())throw new IllegalArgumentException("作废重提读取必须明确完整旧Form/BPM/数字身份");
        return reworkSourceResponse(executionPreparation.readReworkSource(actor,fileId,expected.getReworkSourceFormInstanceId(),expected.getReworkSourceFormInstanceCode(),expected.getReworkSourceBpmProcessInstanceId(),expected.getReworkSourceAttributeRound()));
    }
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReworkSourceVO reworkSourceCandidate(DccObsoleteFormExecutionPreparationService.ReworkSource source){
        var result=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReworkSourceVO();result.setReworkSourceFormInstanceId(source.formInstanceId());result.setReworkSourceFormInstanceCode(source.formInstanceCode());result.setReworkSourceBpmProcessInstanceId(source.bpmProcessInstanceId());result.setReworkSourceAttributeRound(source.attributeRound());result.setReworkSourceDigest(source.sourceDigest());result.setReworkSourceStatus(source.status());result.setReworkSourceVersionNo(source.versionNo());result.setReason(source.reason());result.setAvailable(true);return result;
    }
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO reworkSourceResponse(DccObsoleteFormExecutionPreparationService.ReworkSource source){
        var row=obsoleteForms.selectById(source.formInstanceId());var result=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO();org.springframework.beans.BeanUtils.copyProperties(formResponse(row),result);
        result.setApplicationEntryMode("REWORK_SOURCE");result.setControlledFileId(source.fileId());result.setDccProjectCodeId(source.projectId());result.setVersionNo(source.versionNo());result.setAttributeRound(source.attributeRound());result.setReason(source.reason());result.setIdempotencyKey(row.getIdempotencyKey());result.setSelectedSignoffDepartmentIds(source.selectedSignoffDepartmentIds());result.setDefaultSource(source.defaultSource());result.setActualAttributes(source.actualAttributes());result.setAttributesSubmitted(true);
        result.setApplicationContextKey("DCC_OBSOLETE:"+source.formInstanceId()+":"+source.formInstanceCode());
        result.setReworkSourceFormInstanceId(source.formInstanceId());result.setReworkSourceFormInstanceCode(source.formInstanceCode());result.setReworkSourceBpmProcessInstanceId(source.bpmProcessInstanceId());result.setReworkSourceAttributeRound(source.attributeRound());result.setReworkSourceDigest(source.sourceDigest());result.setReworkSourceStatus(source.status());result.setReworkSourceVersionNo(source.versionNo());result.setReworkSourceApprovalMatrix(DccApprovalMatrixSourceService.readCanonical(source.sourceApprovalMatrixJson()));result.setReworkSourceRelations(source.relations());result.setReworkSourceRelationDigest(source.relations().relationDigest());result.setApprovalMatrixChanged(false);result.setPreparationReady(false);result.setUnavailableReason("DCC_OBSOLETE_REWORK_NEW_DRAFT_REQUIRED");return result;
    }
    private void requireAuthenticatedApplicant(Long actor){var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(login==null||!java.util.Objects.equals(login.getId(),actor)||!java.util.Objects.equals(login.getTenantId(),cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())||!Integer.valueOf(2).equals(login.getUserType()))throw new IllegalArgumentException("作废填写需要真实本租户认证申请人");}
    private FormInstanceRespVO formResponse(cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO form){var result=new FormInstanceRespVO();result.setId(form.getId());result.setInstanceCode(form.getInstanceCode());result.setStatus(form.getStatus());result.setBpmProcessInstanceId(form.getBpmProcessInstanceId());result.setContext(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(form.getBusinessContextJson(),BusinessActionContextReqVO.class));return result;}
    private DraftContext requireKnownDraft(Long actor,Long fileId,DccControlledFileObsoleteReqVO request){
        requireDraftRequest(actor,request);if(!request.isExactFormContextComplete())throw new IllegalArgumentException("作废已知草稿需要真实FormId/code及数字轮");
        int number=applicationRoundService.requireObsoleteFormDraft(actor,fileId,request.getFormInstanceId(),request.getFormInstanceCode());
        if(!Integer.valueOf(number).equals(request.getAttributeRound()))throw new IllegalArgumentException("作废请求数字轮与真实表单预留不一致");
        var form=obsoleteForms.selectByIdAndTenantForUpdate(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),request.getFormInstanceId());var file=controlledFileMapper.selectById(fileId);
        if(form==null||!java.util.Objects.equals(form.getIdempotencyKey(),request.getIdempotencyKey()))throw new IllegalArgumentException("作废草稿不能变更原申请键");
        requireSourceExpectations(request,cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(form.getFormDataJson(),Map.class));
        validateObsoleteAllowed(actor,file,request,false);var saved=projectAttributesService.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number);
        if(Boolean.TRUE.equals(saved.getSubmitted()))throw new IllegalStateException("作废草稿数字属性已经冻结");
        return new DraftContext(file,form,number,saved);
    }
    private void requireSourceExpectations(DccControlledFileObsoleteReqVO request,Map<String,Object> data){
        boolean given=request.getReworkSourceFormInstanceId()!=null||request.getReworkSourceFormInstanceCode()!=null||request.getReworkSourceBpmProcessInstanceId()!=null||request.getReworkSourceAttributeRound()!=null||request.getReworkSourceDigest()!=null;
        if(!given)return;if(!request.isReworkSourceComplete()||!(data.get("dccObsoleteReworkSource") instanceof Map<?,?> source)
                ||!java.util.Objects.equals(String.valueOf(request.getReworkSourceFormInstanceId()),String.valueOf(source.get("formInstanceId")))||!java.util.Objects.equals(request.getReworkSourceFormInstanceCode(),source.get("formInstanceCode"))||!java.util.Objects.equals(request.getReworkSourceBpmProcessInstanceId(),source.get("bpmProcessInstanceId"))
                ||!java.util.Objects.equals(String.valueOf(request.getReworkSourceAttributeRound()),String.valueOf(source.get("attributeRound")))||!java.util.Objects.equals(request.getReworkSourceDigest(),source.get("sourceDigest")))throw new IllegalArgumentException("作废新草稿不可更换原重提来源");
    }
    @Transactional(rollbackFor=Exception.class)
    public FormInstanceRespVO saveObsoleteDraft(Long actor,Long fileId,DccControlledFileObsoleteReqVO request){
        if(request!=null&&request.getSelectedSignoffDepartmentIds()==null)throw new IllegalArgumentException("作废SAVE须明确保存会签部门数组，未选择请传空数组");
        var draft=requireKnownDraft(actor,fileId,request);if(request.getPrepareExecution()==null)throw new IllegalArgumentException("作废SAVE必须明确是否准备执行");
        var prior=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(draft.form().getFormDataJson(),Map.class);var data=new java.util.TreeMap<String,Object>(prior);
        var actual=request.getProjectAttributes()==null?projectAttributesService.readValue(draft.attributes().getActualAttributesJson()):request.getProjectAttributes();
        data.put("reason",request.getReason());data.put("dccSignoffDepartmentIds",request.getSelectedSignoffDepartmentIds());data.put("projectAttributes",actual);
        data.put("dccObsoleteSubmitActorId",actor);data.putIfAbsent("dccOpenPayloadHash",prior.get("dccApplicationPayloadHash"));
        var before=new java.util.TreeMap<>(prior);before.remove("dccExecutionPreparation");var after=new java.util.TreeMap<>(data);after.remove("dccExecutionPreparation");after.remove("dccOpenPayloadHash");
        before.remove("dccOpenPayloadHash");boolean changed=!executionPreparation.sameBusinessPayload(before,after);
        if(prior.containsKey("dccExecutionPreparation"))executionPreparation.verifyReservedSavedAuthorization(persistedForm(draft.form()),draft.file(),actor,prior);
        if(changed)data.remove("dccExecutionPreparation");
        projectAttributesService.saveDraft(actor,draft.file().getDccProjectCodeId(),"OBSOLETE",fileId,draft.number(),actual);
        var save=new cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceDraftReqVO();save.setFormData(data);formCenterRuntimeService.saveDraft(draft.form().getId(),save,actor);
        if(Boolean.TRUE.equals(request.getPrepareExecution())){
            var updated=obsoleteForms.selectByIdAndTenantForUpdate(draft.file().getTenantId(),draft.form().getId());
            String canonical;
            if(!changed&&prior.containsKey("dccExecutionPreparation")){
                canonical=executionPreparation.verifyExactSubmission(persistedForm(updated),domainForm(updated),actor).formDataJson();
            }else canonical=executionPreparation.prepareReservedForDraft(persistedForm(updated),domainForm(updated),actor,matrixExpectations(request));
            save.setFormData(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(canonical,Map.class));formCenterRuntimeService.saveDraft(updated.getId(),save,actor);
        }
        var current=obsoleteForms.selectByIdAndTenantForUpdate(draft.file().getTenantId(),draft.form().getId());var saved=projectAttributesService.readSaved(actor,draft.file().getDccProjectCodeId(),"OBSOLETE",fileId,draft.number());return draftResponse(formResponse(current),draft.file(),draft.number(),saved);
    }
    @Transactional(rollbackFor=Exception.class)
    public FormInstanceRespVO previewObsoleteDraft(Long actor,Long fileId,DccControlledFileObsoleteReqVO request){
        var draft=requireKnownDraft(actor,fileId,request);requireUnchangedDraftInput(draft,request);
        var comparison=executionPreparation.previewReservedForDraft(persistedForm(draft.form()),domainForm(draft.form()),actor,request.getApplicationContextKey(),request.getApprovalMatrixBaselineToken());
        var response=draftResponse(formResponse(draft.form()),draft.file(),draft.number(),draft.attributes());response.setApprovalMatrixBefore(comparison.before());response.setApprovalMatrixAfter(comparison.after().baseline());response.setApprovalMatrixChanged(comparison.changed());response.setApprovalMatrixBaselineToken(comparison.baselineToken());response.setApprovalMatrixConfirmationToken(comparison.confirmationToken());
        String unavailable=executionPreparation.preparationUnavailableReason(draft.file(),actor);if(unavailable!=null){response.setPreparationReady(false);response.setUnavailableReason(unavailable);}return response;
    }
    @Transactional(rollbackFor=Exception.class)
    public FormInstanceRespVO submitObsoleteDraft(Long actor,Long fileId,DccControlledFileObsoleteReqVO request){
        requireDraftRequest(actor,request);if(!request.isExactFormContextComplete())throw new IllegalArgumentException("作废提交需要真实Form身份和数字轮");
        var actualForm=obsoleteForms.selectByIdAndTenantForUpdate(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),request.getFormInstanceId());
        if(actualForm!=null&&actualForm.getBpmProcessInstanceId()!=null&&!"DRAFT".equals(actualForm.getStatus())){
            var file=controlledFileMapper.selectById(fileId);
            if(file==null||!java.util.Objects.equals(file.getTenantId(),actualForm.getTenantId())||!java.util.Objects.equals(actualForm.getApplicantUserId(),actor)||!java.util.Objects.equals(actualForm.getInstanceCode(),request.getFormInstanceCode())
                    ||!java.util.Objects.equals(actualForm.getIdempotencyKey(),request.getIdempotencyKey())||!permissionSupport.hasCategoryPermission(file.getCategoryId(),actor,DccFileCategoryPermissionActionEnum.OBSOLETE))throw new IllegalArgumentException("作废提交重放身份/权限不一致");
            int number=applicationRoundService.require("OBSOLETE",fileId,actualForm.getBpmProcessInstanceId());if(!Integer.valueOf(number).equals(request.getAttributeRound()))throw new IllegalArgumentException("作废提交重放数字轮不一致");
            var saved=projectAttributesService.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number);var replay=new DraftContext(file,actualForm,number,saved);requireUnchangedDraftInput(replay,request);
            return draftResponse(formResponse(actualForm),file,number,saved);
        }
        var draft=requireKnownDraft(actor,fileId,request);requireUnchangedDraftInput(draft,request);var domain=domainForm(draft.form());
        executionPreparation.verifyReservedExactSubmission(persistedForm(draft.form()),domain,actor);
        var submit=new FormInstanceSubmitReqVO();submit.setFormData(domain.getFormData());var submitted=formCenterRuntimeService.submitInstance(draft.form().getId(),submit,actor);
        // The same shared submission callback owns bind/freeze. Never allocate/read current defaults here.
        if(submitted.getBpmProcessInstanceId()==null||submitted.getBpmProcessInstanceId().isBlank())throw new IllegalStateException("作废精确提交未返回真实BPM");
        int bound=applicationRoundService.require("OBSOLETE",fileId,submitted.getBpmProcessInstanceId());
        if(bound!=draft.number())throw new IllegalStateException("作废提交回调绑定了另一数字轮");
        var frozen=projectAttributesService.readSaved(actor,draft.file().getDccProjectCodeId(),"OBSOLETE",fileId,bound);
        if(!Boolean.TRUE.equals(frozen.getSubmitted()))throw new IllegalStateException("作废提交回调未冻结原预留属性，不能伪报告送审成功");
        return draftResponse(submitted,draft.file(),bound,frozen);
    }
    private void requireUnchangedDraftInput(DraftContext draft,DccControlledFileObsoleteReqVO request){
        var data=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(draft.form().getFormDataJson(),Map.class);
        requireSourceExpectations(request,data);
        if(!java.util.Objects.equals(request.getReason(),data.get("reason")))throw new IllegalArgumentException("作废预览/提交原因变化请先SAVE保存");
        Object selected=data.get("dccSignoffDepartmentIds");var ids=selected instanceof List<?> values?values.stream().map(v->Long.valueOf(String.valueOf(v))).toList():null;
        if(request.getSelectedSignoffDepartmentIds()!=null&&!java.util.Objects.equals(request.getSelectedSignoffDepartmentIds(),ids))throw new IllegalArgumentException("作废名单变化请先SAVE保存");
        if(request.getProjectAttributes()!=null&&!java.util.Objects.equals(projectAttributesService.encode(request.getProjectAttributes()),draft.attributes().getActualAttributesJson()))throw new IllegalArgumentException("作废实际属性变化请先SAVE保存");
        if(!java.util.Objects.equals("DCC_OBSOLETE:"+draft.form().getId()+":"+draft.form().getInstanceCode(),request.getApplicationContextKey()))throw new IllegalArgumentException("作废真实表单准备上下文不一致");
    }
    private cn.iocoder.yudao.module.bpm.formcenter.service.FormExactBpmSubmissionAdapter.PersistedDraft persistedForm(cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO form){return new cn.iocoder.yudao.module.bpm.formcenter.service.FormExactBpmSubmissionAdapter.PersistedDraft(form.getTenantId(),form.getId(),form.getInstanceCode(),form.getApplicantUserId(),form.getStatus(),form.getBpmProcessInstanceId(),form.getFormDataJson());}
    private DccApprovalMatrixBaselineService.Expectations matrixExpectations(DccControlledFileObsoleteReqVO req){return new DccApprovalMatrixBaselineService.Expectations(req.getExpectedApprovalRouteId(),req.getExpectedApprovalRouteVersionNo(),req.getExpectedApprovalMatrixDigest(),req.getApprovalMatrixBaselineToken(),req.getApprovalMatrixConfirmationToken(),req.getMatrixChangeConfirmed(),req.getApplicationContextKey());}
    private cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance domainForm(cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO form){
        var policy=obsoletePolicies.selectById(form.getPolicyId());if(policy==null||!java.util.Objects.equals(policy.getTenantId(),form.getTenantId())||!"PUBLISHED".equals(policy.getStatus())||!DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(policy.getProcessDefinitionKey()))throw new IllegalStateException("作废真实表单策略不一致或未发布");
        var context=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(form.getBusinessContextJson(),BusinessActionContextReqVO.class);
        var business=cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext.builder().tenantId(context.getTenantId()).dataDomain(context.getDataDomain()).systemCode(context.getSystemCode()).objectType(context.getObjectType()).objectId(context.getObjectId()).objectVersion(context.getObjectVersion()).actionCode(context.getActionCode()).objectState(context.getObjectState()).reason(context.getReason()).build();
        var formalPolicy=cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy.builder().policyId(policy.getId()).tenantId(policy.getTenantId()).dataDomain(policy.getDataDomain()).systemCode(policy.getSystemCode()).objectType(policy.getObjectType()).actionCode(policy.getActionCode()).objectState(policy.getObjectState()).policyType(cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType.NONE).approvalMode(cn.iocoder.yudao.module.bpm.formcenter.model.FormApprovalMode.BPM_REQUIRED).bpmProcessKey(policy.getProcessDefinitionKey()).effectExecutorCode(policy.getEffectExecutorCode()).status(policy.getStatus()).build();
        var domain=new cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance(form.getInstanceCode(),cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution.from(formalPolicy),business,form.getApplicantUserId(),form.getIdempotencyKey());domain.setStatus(cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus.valueOf(form.getStatus()));domain.setFormData(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(form.getFormDataJson(),Map.class));return domain;
    }

    /** Opens only a real form draft. No route/provider lookup or BPM creation; defaults are captured now. */
    @Override
    @Transactional(rollbackFor=Exception.class)
    public FormInstanceRespVO openObsoleteDraft(Long actor,Long fileId,DccControlledFileObsoleteReqVO request){
        if(request!=null&&request.getSelectedSignoffDepartmentIds()==null)throw new IllegalArgumentException("作废OPEN须明确保存会签部门输入，未选择请显式传空数组");
        requireDraftRequest(actor,request);
        if(request.getReworkSourceFormInstanceId()!=null||request.getReworkSourceFormInstanceCode()!=null||request.getReworkSourceBpmProcessInstanceId()!=null||request.getReworkSourceAttributeRound()!=null||request.getReworkSourceDigest()!=null)return openObsoleteReworkDraft(actor,fileId,request);
        var source=controlledFileMapper.selectById(fileId);
        if(source==null||!java.util.Objects.equals(source.getTenantId(),cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId()))throw exception(CONTROLLED_FILE_NOT_EXISTS);
        validateObsoleteAllowed(actor,source,request,false);
        // No Master/File lock before Form. The form-specific primitive performs the shared lock order.
        var existing=formCenterRuntimeService.findBusinessActionByIdempotency(buildObsoleteContext(source,request),request.getIdempotencyKey());
        FormInstanceRespVO draft;
        if(existing!=null){
            if(!"DRAFT".equals(existing.getStatus())||existing.getBpmProcessInstanceId()!=null)throw new IllegalArgumentException("作废OPEN只能回读本人的未提交草稿");
            draft=existing;
        }else{
            pendingActionGuard.assertNoPendingBusinessAction(source);
            var data=buildObsoleteFormData(source,request);data.put("dccObsoleteSubmitActorId",actor);data.put("dccApplicationPayloadHash",obsoletePayloadHash(actor,source,request));data.put("dccOpenPayloadHash",obsoletePayloadHash(actor,source,request));
            data.put("dccSignoffDepartmentIds",request.getSelectedSignoffDepartmentIds());
            if(request.getProjectAttributes()!=null)data.put("projectAttributes",request.getProjectAttributes());
            var create=new FormInstanceCreateReqVO();create.setContext(buildObsoleteContext(source,request));create.setIdempotencyKey(request.getIdempotencyKey());create.setFormData(data);
            draft=formCenterRuntimeService.createInstance(create,actor);
        }
        if(draft==null||draft.getId()==null||draft.getInstanceCode()==null||!"DRAFT".equals(draft.getStatus()))throw new IllegalStateException("作废OPEN未取得真实表单草稿身份");
        var saved=obsoleteForms.selectByIdAndTenantForUpdate(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),draft.getId());
        if(saved==null||!java.util.Objects.equals(saved.getApplicantUserId(),actor)||!java.util.Objects.equals(saved.getInstanceCode(),draft.getInstanceCode())
                ||!java.util.Objects.equals(saved.getIdempotencyKey(),request.getIdempotencyKey()))throw new IllegalArgumentException("作废OPEN重放与原表单申请人、标识不一致");
        var data=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(saved.getFormDataJson(),Map.class);
        if(data==null||!java.util.Objects.equals(obsoletePayloadHash(actor,source,request),data.get("dccOpenPayloadHash"))
                ||!java.util.Objects.equals(String.valueOf(actor),String.valueOf(data.get("dccObsoleteSubmitActorId")))||!java.util.Objects.equals(request.getReason(),data.get("reason")))
            throw new IllegalArgumentException("作废OPEN载荷不同，不能覆盖已保存草稿");
        int number=existing==null?applicationRoundService.reserveObsoleteFormDraft(actor,fileId,draft.getId(),draft.getInstanceCode()):
                applicationRoundService.requireObsoleteFormDraft(actor,fileId,draft.getId(),draft.getInstanceCode());
        source=controlledFileMapper.selectById(fileId); // Revalidated by the primitive under Project/Master/File locks.
        cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO attributes;
        if(existing==null){attributes=projectAttributesService.beginDraft(actor,source.getDccProjectCodeId(),"OBSOLETE",fileId,number);
            if(request.getProjectAttributes()!=null)attributes=projectAttributesService.saveDraft(actor,source.getDccProjectCodeId(),"OBSOLETE",fileId,number,request.getProjectAttributes());}
        else attributes=projectAttributesService.readSaved(actor,source.getDccProjectCodeId(),"OBSOLETE",fileId,number);
        return draftResponse(draft,source,number,attributes);
    }
    private FormInstanceRespVO openObsoleteReworkDraft(Long actor,Long fileId,DccControlledFileObsoleteReqVO request){
        if(!request.isReworkSourceComplete())throw new IllegalArgumentException("作废重提OPEN必须明确完整旧Form/BPM/数字和来源摘要");
        var source=executionPreparation.readReworkSourceForUpdate(actor,fileId,request.getReworkSourceFormInstanceId(),request.getReworkSourceFormInstanceCode(),request.getReworkSourceBpmProcessInstanceId(),request.getReworkSourceAttributeRound());
        if(!java.util.Objects.equals(source.sourceDigest(),request.getReworkSourceDigest()))throw new IllegalArgumentException("作废重提源事实已改变，请重新读取");
        var old=obsoleteForms.selectById(source.formInstanceId());if(java.util.Objects.equals(old.getIdempotencyKey(),request.getIdempotencyKey()))throw new IllegalArgumentException("作废重提必须使用新的申请键，不能重放旧BPM");
        var file=controlledFileMapper.selectById(fileId);var context=buildObsoleteContext(file,request);var existing=formCenterRuntimeService.findBusinessActionByIdempotency(context,request.getIdempotencyKey());
        var identity=executionPreparation.reworkSourceIdentity(source);FormInstanceRespVO draft;int number;
        if(existing!=null){
            if(!"DRAFT".equals(existing.getStatus())||existing.getBpmProcessInstanceId()!=null)throw new IllegalArgumentException("作废重提OPEN仅回读本次未提交新草稿");
            var row=obsoleteForms.selectByIdAndTenantForUpdate(file.getTenantId(),existing.getId());var data=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(row.getFormDataJson(),Map.class);
            if(!executionPreparation.sameBusinessPayload(identity,(Map<String,Object>)data.get("dccObsoleteReworkSource"))||!java.util.Objects.equals(data.get("dccOpenPayloadHash"),obsoletePayloadHash(actor,file,request))||!java.util.Objects.equals(row.getApplicantUserId(),actor))throw new IllegalArgumentException("作废重提OPEN载荷或来源不同，不能覆盖");
            draft=existing;number=applicationRoundService.requireObsoleteFormDraft(actor,fileId,row.getId(),row.getInstanceCode());
        }else{
            pendingActionGuard.assertNoPendingBusinessAction(file);var data=buildObsoleteFormData(file,request);data.put("dccObsoleteSubmitActorId",actor);data.put("dccApplicationPayloadHash",obsoletePayloadHash(actor,file,request));data.put("dccOpenPayloadHash",obsoletePayloadHash(actor,file,request));data.put("dccSignoffDepartmentIds",request.getSelectedSignoffDepartmentIds());data.put("projectAttributes",request.getProjectAttributes()==null?source.actualAttributes():request.getProjectAttributes());
            data.put("dccObsoleteReworkSource",identity);data.put("dccObsoleteReworkSourceApprovalMatrixJson",source.sourceApprovalMatrixJson());data.put("dccObsoleteReworkSourceRuntimeApprovalNodesJson",source.runtimeApprovalNodesJson());data.put("dccObsoleteReworkSourceProcessDefinitionId",source.preparedProcessDefinitionId());data.put("dccObsoleteReworkSourceDefinitionDigest",source.preparedDefinitionDigest());data.put("dccObsoleteReworkSourceExecutionContract",source.executionContract());
            var create=new FormInstanceCreateReqVO();create.setContext(context);create.setIdempotencyKey(request.getIdempotencyKey());create.setFormData(data);draft=formCenterRuntimeService.createInstance(create,actor);
            if(draft==null||draft.getId()==null||draft.getInstanceCode()==null||!"DRAFT".equals(draft.getStatus()))throw new IllegalStateException("作废重提OPEN未创建实际新Form");
            number=applicationRoundService.reserveObsoleteFormDraft(actor,fileId,draft.getId(),draft.getInstanceCode());
            projectAttributesService.forkForReworkToReservedRound(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,source.attributeRound(),number);
            if(request.getProjectAttributes()!=null)projectAttributesService.saveDraft(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number,request.getProjectAttributes());
            executionPreparation.inheritReworkRelations(actor,source,number);
        }
        var attrs=projectAttributesService.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number);var response=draftResponse(draft,file,number,attrs);fillReworkSource(response,source);return response;
    }
    private void fillReworkSource(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO response,DccObsoleteFormExecutionPreparationService.ReworkSource source){
        response.setReworkSourceFormInstanceId(source.formInstanceId());response.setReworkSourceFormInstanceCode(source.formInstanceCode());response.setReworkSourceBpmProcessInstanceId(source.bpmProcessInstanceId());response.setReworkSourceAttributeRound(source.attributeRound());response.setReworkSourceDigest(source.sourceDigest());response.setReworkSourceStatus(source.status());response.setReworkSourceVersionNo(source.versionNo());response.setReworkSourceApprovalMatrix(DccApprovalMatrixSourceService.readCanonical(source.sourceApprovalMatrixJson()));response.setReworkSourceRelations(source.relations());response.setReworkSourceRelationDigest(source.relations().relationDigest());
    }
    private void requireDraftRequest(Long actor,DccControlledFileObsoleteReqVO req){
        var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(login==null||!java.util.Objects.equals(login.getId(),actor)||!java.util.Objects.equals(login.getTenantId(),cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())||!Integer.valueOf(2).equals(login.getUserType()))throw new IllegalArgumentException("作废填写需要真实本租户认证申请人");
        if(req==null||StrUtil.isBlank(req.getReason()))throw exception(CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED);
        if(StrUtil.isBlank(req.getIdempotencyKey()))throw new IllegalArgumentException("DCC obsolete idempotencyKey is required");
        req.setReason(req.getReason().trim());req.setIdempotencyKey(req.getIdempotencyKey().trim());
        var selected=req.getSelectedSignoffDepartmentIds();if(selected!=null&&(selected.stream().anyMatch(id->id==null||id<=0)||selected.stream().distinct().count()!=selected.size()))throw new IllegalArgumentException("作废会签部门输入不合法");
    }
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO draftResponse(FormInstanceRespVO form,DccControlledFileDO file,int number,
            cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO attributes){
        if(attributes==null||!java.util.Objects.equals(attributes.getTenantId(),file.getTenantId())||!java.util.Objects.equals(attributes.getProjectCodeId(),file.getDccProjectCodeId())
                ||!java.util.Objects.equals(attributes.getApplicationId(),file.getId())||!"OBSOLETE".equals(attributes.getApplicationType())||!Integer.valueOf(number).equals(attributes.getApplicationRound()))throw new IllegalStateException("作废真实预留数字属性身份不一致");
        var response=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO();org.springframework.beans.BeanUtils.copyProperties(form,response);
        response.setApplicationEntryMode("DRAFT");
        response.setControlledFileId(file.getId());response.setDccProjectCodeId(file.getDccProjectCodeId());response.setVersionNo(file.getVersionNo());response.setAttributeRound(number);
        response.setApplicationContextKey("DCC_OBSOLETE:"+form.getId()+":"+form.getInstanceCode());response.setDefaultSource(projectAttributesService.readValue(attributes.getDefaultSourceJson()));response.setActualAttributes(projectAttributesService.readValue(attributes.getActualAttributesJson()));response.setAttributesSubmitted(Boolean.TRUE.equals(attributes.getSubmitted()));
        var actualForm=obsoleteForms.selectById(form.getId());var data=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(actualForm.getFormDataJson(),Map.class);
        response.setReason((String)data.get("reason"));response.setIdempotencyKey(actualForm.getIdempotencyKey());
        Object selected=data.get("dccSignoffDepartmentIds");if(selected instanceof List<?> list)response.setSelectedSignoffDepartmentIds(list.stream().map(value->Long.valueOf(String.valueOf(value))).toList());
        if(data.containsKey("dccObsoleteReworkSource"))fillReworkSource(response,Boolean.TRUE.equals(attributes.getSubmitted())?executionPreparation.requireRecordedReworkSource(actualForm.getApplicantUserId(),file.getId(),data):executionPreparation.requireSavedReworkSource(actualForm.getApplicantUserId(),file.getId(),data));
        response.setApprovalMatrixChanged(false);response.setPreparationReady(false);response.setUnavailableReason("DCC_OBSOLETE_EXECUTION_PREPARATION_REQUIRED");
        if(data.containsKey("dccExecutionPreparation")&&Boolean.TRUE.equals(attributes.getSubmitted())){
            var runtime=executionPreparation.requireSubmittedRuntime(file.getTenantId(),file.getId(),actualForm.getBpmProcessInstanceId());
            if(!java.util.Objects.equals(runtime.head().formInstanceId(),actualForm.getId())||!Integer.valueOf(number).equals(runtime.head().attributeRound()))throw new IllegalStateException("作废提交回执独立运行证据不一致");response.setPreparationReady(true);response.setUnavailableReason(null);
        }else if(data.containsKey("dccExecutionPreparation")){executionPreparation.verifyReservedSavedAuthorization(persistedForm(actualForm),file,actualForm.getApplicantUserId(),data);
            String unavailable=executionPreparation.preparationUnavailableReason();response.setPreparationReady(unavailable==null);response.setUnavailableReason(unavailable);}
        return response;
    }

    @Override
    public void precheckObsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO) {
        requireObsoleteAllowed(userId, id, reqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormInstanceRespVO obsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO) {
        if(reqVO==null||reqVO.getPhase()==null)throw new IllegalArgumentException("作废申请phase必须明确选择OPEN/PREVIEW/SAVE/SUBMIT");
        return switch(reqVO.getPhase()){
            case "OPEN" -> openObsoleteDraft(userId,id,reqVO);
            case "PREVIEW" -> previewObsoleteDraft(userId,id,reqVO);
            case "SAVE" -> saveObsoleteDraft(userId,id,reqVO);
            case "SUBMIT" -> submitObsoleteDraft(userId,id,reqVO);
            default -> throw new IllegalArgumentException("作废申请phase无效");
        };
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
        formData.put("fileCode", file.getFileNumber());
        formData.put("fileVersion", file.getVersionNo());
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
        // Exact submission already holds Form first; preflight validates the actual locked File.
        DccControlledFileDO file = enforcePendingActionGuard
                ? controlledFileMapper.selectByIdAndTenantForUpdate(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(),id)
                : controlledFileMapper.selectById(id);
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
