package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Original public baseline and explicit latest comparison under the configuration writer's category lock. */
@Service
public class DccApprovalMatrixBaselineService {
    @Resource private DccApprovalMatrixSourceService sources;
    @Resource private DccApprovalMatrixTokenService tokens;
    @Resource(name="dccControlledFileMapper") private DccControlledFileMapper files;
    @Resource private DccApplicationExecutionContractService executionContracts;
    public record Expectations(Long routeId,Integer routeVersionNo,String digest,String baselineToken,String confirmationToken,
                               Boolean changeConfirmed,String applicationContextKey){}
    public record Comparison(DccApprovalMatrixTokenService.Context context,String beforeCanonical,
            DccApprovalMatrixBaselineVO before,DccApprovalMatrixSourceService.Source after,String baselineToken,String confirmationToken,boolean changed){}
    public record Confirmed(DccApprovalMatrixSourceService.Source source,String beforeCanonical,String confirmationIdentity){}
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Comparison previewNewAction(Long actor,DccControlledFileRoutePreviewReqVO request){
        if(request==null||!Boolean.TRUE.equals(request.getPrepareNewActionBaseline())||request.getControlledFileId()==null)
            throw new IllegalArgumentException("NEW_ACTION_BASELINE_EXPLICIT_PREPARATION_REQUIRED");
        var file=requireDraft(actor,request.getControlledFileId());
        if(file.getCheckedOutBy()!=null)throw new IllegalArgumentException("NEW_ACTION_BASELINE_CHECKOUT_NOT_ALLOWED");
        executionContracts.requireNewActionBaselinePreparation(actor,file.getId());
        executionContracts.requirePrepared(file,request.getExecutionContract(),request.getExpectedProcessDefinitionId(),request.getExpectedDefinitionDigest(),DccControlledFileProcessDefinitionKeys.REVISION);
        requireRequestFamily(file,request.getCategoryId(),request.getDccProjectCodeId(),request.getActionType());
        var current=sources.readForUpdate(file.getCategoryId());
        var context=context(actor,file.getDccProjectCodeId(),current.baseline(),"REVISION",file.getId(),request.getApplicationContextKey());
        String before=request.getApprovalMatrixBaselineToken()==null?current.canonical():tokens.verifyBaseline(context,request.getApprovalMatrixBaselineToken());
        var baseline=DccApprovalMatrixSourceService.readCanonical(before);requireSourceIdentity(baseline,current.baseline());
        requireFileMatrix(file,baseline);
        boolean changed=!baseline.getMatrixDigest().equals(current.baseline().getMatrixDigest());
        return new Comparison(context,before,baseline,current,tokens.issueBaseline(context,before),changed?tokens.issueConfirmation(context,before,
                current.baseline().getRouteId(),current.baseline().getRouteVersionNo(),current.baseline().getMatrixDigest()):null,changed);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public String initializeNewActionDraft(Long actor,DccControlledFileDO file,DccWorkingApplicationSaveReqVO request){
        if(request==null||!Boolean.TRUE.equals(request.getPrepareNewActionBaseline()))throw new IllegalArgumentException("NEW_ACTION_BASELINE_EXPLICIT_PREPARATION_REQUIRED");
        executionContracts.requireNewActionBaselinePreparation(actor,file.getId());
        executionContracts.requirePrepared(file,request.getExecutionContract(),request.getExpectedProcessDefinitionId(),request.getExpectedDefinitionDigest(),DccControlledFileProcessDefinitionKeys.REVISION);
        var current=sources.readForUpdate(file.getCategoryId());
        var context=context(actor,file.getDccProjectCodeId(),current.baseline(),"REVISION",file.getId(),request.getApplicationContextKey());
        String before=tokens.verifyBaseline(context,request.getApprovalMatrixBaselineToken());var baseline=DccApprovalMatrixSourceService.readCanonical(before);
        requireSourceIdentity(baseline,current.baseline());requireFileMatrix(file,baseline);
        var expected=new Expectations(request.getExpectedApprovalRouteId(),request.getExpectedApprovalRouteVersionNo(),request.getExpectedApprovalMatrixDigest(),
                request.getApprovalMatrixBaselineToken(),request.getApprovalMatrixConfirmationToken(),request.getMatrixChangeConfirmed(),request.getApplicationContextKey());
        requireCurrentExpectation(context,before,baseline,current.baseline(),expected);
        if(files.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId,file.getId()).eq(DccControlledFileDO::getTenantId,file.getTenantId()).eq(DccControlledFileDO::getStatus,"WORKING")
                .isNull(DccControlledFileDO::getProcessInstanceId).isNull(DccControlledFileDO::getDraftApprovalMatrixJson)
                .set(DccControlledFileDO::getDraftApprovalMatrixJson,before))!=1)throw new IllegalStateException("APPROVAL_MATRIX_DRAFT_CAS_CONFLICT");
        file.setDraftApprovalMatrixJson(before);return before;
    }
    private void requireCurrentExpectation(DccApprovalMatrixTokenService.Context context,String before,DccApprovalMatrixBaselineVO baseline,DccApprovalMatrixBaselineVO current,Expectations expected){
        if(expected.routeId()==null||expected.routeVersionNo()==null||expected.digest()==null)throw new IllegalArgumentException("APPROVAL_MATRIX_EXPECTATION_REQUIRED");
        if(!Objects.equals(expected.routeId(),current.getRouteId())||!Objects.equals(expected.routeVersionNo(),current.getRouteVersionNo())||!Objects.equals(expected.digest(),current.getMatrixDigest()))
            throw change("APPROVAL_MATRIX_EXPECTATION_STALE",context,before,baseline,current);
        if(!baseline.getMatrixDigest().equals(current.getMatrixDigest())){
            if(!Boolean.TRUE.equals(expected.changeConfirmed()))throw change("APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED",context,before,baseline,current);
            tokens.requireConfirmation(context,expected.confirmationToken(),before,expected.routeId(),expected.routeVersionNo(),expected.digest());
        }
    }
    private void requireRequestFamily(DccControlledFileDO file,Long category,Long project,String action){
        if(!Objects.equals(category,file.getCategoryId())||!Objects.equals(project,file.getDccProjectCodeId())||!Objects.equals(action,file.getChangeType()))
            throw new IllegalArgumentException("APPROVAL_MATRIX_DRAFT_IDENTITY_INVALID");
    }
    private void requireFileMatrix(DccControlledFileDO file,DccApprovalMatrixBaselineVO matrix){
        if(!Objects.equals(file.getTenantId(),matrix.getTenantId())||!Objects.equals(file.getCategoryId(),matrix.getCategoryId())
                ||!Objects.equals(file.getFileTypeTaxonomyId(),matrix.getFileTypeTaxonomyId()))throw new IllegalArgumentException("APPROVAL_MATRIX_DRAFT_BASELINE_IDENTITY_INVALID");
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public String creationBaseline(Long actor,Long project,Long category,String action,String token,String applicationContextKey){
        var current=sources.readForUpdate(category);
        String before=tokens.verifyBaseline(context(actor,project,current.baseline(),action,null,applicationContextKey),token);
        requireSourceIdentity(DccApprovalMatrixSourceService.readCanonical(before),current.baseline());return before;
    }
    public Comparison preview(Long actor,DccControlledFileRoutePreviewReqVO request){
        var current=sources.read(request.getCategoryId());
        DccControlledFileDO file=request.getControlledFileId()==null?null:requireDraft(actor,request.getControlledFileId());
        Long project=file==null?request.getDccProjectCodeId():file.getDccProjectCodeId();
        var context=context(actor,project,current.baseline(),request.getActionType(),file==null?null:file.getId(),request.getApplicationContextKey());
        if(file!=null&&file.getDraftApprovalMatrixJson()==null)
            throw new IllegalStateException("APPROVAL_MATRIX_DRAFT_BASELINE_REQUIRED：既有工作申请不能用当前配置补原基线");
        String before=file!=null&&file.getDraftApprovalMatrixJson()!=null?file.getDraftApprovalMatrixJson()
                :request.getApprovalMatrixBaselineToken()==null?current.canonical():tokens.verifyBaseline(context,request.getApprovalMatrixBaselineToken());
        var baseline=DccApprovalMatrixSourceService.readCanonical(before);requireSourceIdentity(baseline,current.baseline());
        boolean changed=!baseline.getMatrixDigest().equals(current.baseline().getMatrixDigest());
        String baselineToken=tokens.issueBaseline(context,before);
        String confirmation=changed?tokens.issueConfirmation(context,before,current.baseline().getRouteId(),current.baseline().getRouteVersionNo(),current.baseline().getMatrixDigest()):null;
        return new Comparison(context,before,baseline,current,baselineToken,confirmation,changed);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Confirmed confirm(Long actor,Long project,Long category,Long fileId,String action,Expectations expected){
        if(expected==null||expected.routeId()==null||expected.routeVersionNo()==null||expected.digest()==null)
            throw new IllegalArgumentException("APPROVAL_MATRIX_EXPECTATION_REQUIRED");
        var current=sources.readForUpdate(category);var file=fileId==null?null:requireDraft(actor,fileId);
        if(file!=null&&(!Objects.equals(file.getDccProjectCodeId(),project)||!Objects.equals(file.getCategoryId(),category)))
            throw new IllegalArgumentException("APPROVAL_MATRIX_DRAFT_IDENTITY_INVALID");
        var context=context(actor,project,current.baseline(),action,fileId,expected.applicationContextKey());
        String before=file!=null&&file.getDraftApprovalMatrixJson()!=null?file.getDraftApprovalMatrixJson():tokens.verifyBaseline(context,expected.baselineToken());
        var baseline=DccApprovalMatrixSourceService.readCanonical(before);requireSourceIdentity(baseline,current.baseline());
        if(!Objects.equals(expected.routeId(),current.baseline().getRouteId())||!Objects.equals(expected.routeVersionNo(),current.baseline().getRouteVersionNo())||!Objects.equals(expected.digest(),current.baseline().getMatrixDigest()))
            throw change("APPROVAL_MATRIX_EXPECTATION_STALE",context,before,baseline,current.baseline());
        if(!baseline.getMatrixDigest().equals(current.baseline().getMatrixDigest())){
            if(!Boolean.TRUE.equals(expected.changeConfirmed()))throw change("APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED",context,before,baseline,current.baseline());
            tokens.requireConfirmation(context,expected.confirmationToken(),before,expected.routeId(),expected.routeVersionNo(),expected.digest());
        }
        return new Confirmed(current,before,expected.confirmationToken());
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public String initializeDraft(Long actor,DccControlledFileDO file,String token,String applicationContextKey){
        if(file==null||!Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())
                ||!Objects.equals(file.getRequesterId(),actor)||!"WORKING".equals(file.getStatus())||file.getProcessInstanceId()!=null)
            throw new IllegalArgumentException("APPROVAL_MATRIX_DRAFT_IDENTITY_INVALID");
        if(file.getDraftApprovalMatrixJson()!=null){
            var original=DccApprovalMatrixSourceService.readCanonical(file.getDraftApprovalMatrixJson());
            if(!Objects.equals(original.getTenantId(),file.getTenantId())||!Objects.equals(original.getCategoryId(),file.getCategoryId()))
                throw new IllegalArgumentException("APPROVAL_MATRIX_DRAFT_BASELINE_IDENTITY_INVALID");
            // A saved-file preview issues an actual-file token. Creation tokens are no longer
            // accepted for ordinary saves, and current configuration is never used to rebase it.
            String verified=tokens.verifyBaseline(context(actor,file.getDccProjectCodeId(),original,file.getChangeType(),file.getId(),applicationContextKey),token);
            if(!file.getDraftApprovalMatrixJson().equals(verified))throw new IllegalStateException("APPROVAL_MATRIX_DRAFT_CAS_CONFLICT");
            return file.getDraftApprovalMatrixJson();
        }
        var current=sources.readForUpdate(file.getCategoryId());
        // Creation token is scoped to the source application before its durable file ID exists.
        var context=context(actor,file.getDccProjectCodeId(),current.baseline(),file.getChangeType(),null,applicationContextKey);
        String baseline=tokens.verifyBaseline(context,token);requireSourceIdentity(DccApprovalMatrixSourceService.readCanonical(baseline),current.baseline());
        if(files.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId,file.getId()).eq(DccControlledFileDO::getTenantId,TenantContextHolder.getRequiredTenantId())
                .eq(DccControlledFileDO::getStatus,"WORKING").isNull(DccControlledFileDO::getProcessInstanceId)
                .isNull(DccControlledFileDO::getDraftApprovalMatrixJson).set(DccControlledFileDO::getDraftApprovalMatrixJson,baseline))!=1)
            throw new IllegalStateException("APPROVAL_MATRIX_DRAFT_CAS_CONFLICT");
        file.setDraftApprovalMatrixJson(baseline);return baseline;
    }
    private DccControlledFileDO requireDraft(Long actor,Long id){var file=files.selectById(id);if(file==null||!Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())||!Objects.equals(file.getRequesterId(),actor)||!"WORKING".equals(file.getStatus())||file.getProcessInstanceId()!=null)throw new IllegalArgumentException("APPROVAL_MATRIX_DRAFT_IDENTITY_INVALID");return file;}
    private DccApprovalMatrixChangeException change(String code,DccApprovalMatrixTokenService.Context context,String original,DccApprovalMatrixBaselineVO before,DccApprovalMatrixBaselineVO after){
        return new DccApprovalMatrixChangeException(code,before,after,tokens.issueBaseline(context,original),
                tokens.issueConfirmation(context,original,after.getRouteId(),after.getRouteVersionNo(),after.getMatrixDigest()));
    }
    private DccApprovalMatrixTokenService.Context context(Long actor,Long project,DccApprovalMatrixBaselineVO matrix,String action,Long fileId,String key){return new DccApprovalMatrixTokenService.Context(TenantContextHolder.getRequiredTenantId(),actor,project,matrix.getCategoryId(),matrix.getFileTypeTaxonomyId(),action,fileId,key);}
    private void requireSourceIdentity(DccApprovalMatrixBaselineVO before,DccApprovalMatrixBaselineVO after){if(before==null||!Objects.equals(before.getTenantId(),TenantContextHolder.getRequiredTenantId())||!Objects.equals(before.getCategoryId(),after.getCategoryId())||!Objects.equals(before.getFileTypeTaxonomyId(),after.getFileTypeTaxonomyId())||!"LEGACY".equals(before.getActionType()))throw new IllegalArgumentException("APPROVAL_MATRIX_BASELINE_IDENTITY_INVALID");}
}
