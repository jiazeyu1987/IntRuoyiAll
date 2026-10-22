package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Set;

/** Real file drafts reserve numeric rounds; NULL means no BPM exists, never a fabricated process id. */
@Service
public class DccApplicationRoundService {
    @Resource private JdbcTemplate jdbc;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccControlledFileMapper files;
    @Resource private org.springframework.beans.factory.ObjectProvider<cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper> obsoleteForms;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper obsoleteMasters;

    /** Each real obsolete form owns its own numeric draft; no native-file open round is borrowed. */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public int reserveObsoleteFormDraft(Long actor,Long fileId,Long formId,String formCode) {
        var context=lockObsoleteForm(actor,fileId,formId,formCode,null);
        var existing=readObsoleteFormRound(context);
        if(!existing.isEmpty()) {
            var row=existing.get(0);
            if(row.bpm()!=null)throw new IllegalStateException("作废表单草稿轮次已绑定审批");
            return row.number();
        }
        int number=nextRound(context.tenant(),"OBSOLETE",fileId);
        if(jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round,form_instance_id) VALUES(?,?,'OBSOLETE',?,NULL,?,?)",
                context.tenant(),context.project(),fileId,number,formId)!=1)
            throw new IllegalStateException("作废真实表单草稿预留失败");
        return number;
    }
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public int requireObsoleteFormDraft(Long actor,Long fileId,Long formId,String formCode) {
        var context=lockObsoleteForm(actor,fileId,formId,formCode,null);
        var rows=readObsoleteFormRound(context);
        if(rows.size()!=1||rows.get(0).bpm()!=null)throw new IllegalStateException("指定作废表单开放草稿轮次缺失");
        return rows.get(0).number();
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public int bindObsoleteFormDraft(Long actor,Long fileId,Long formId,String formCode,Integer number,String actualBpm) {
        validateBpm(actualBpm);
        var context=lockObsoleteForm(actor,fileId,formId,formCode,actualBpm);
        var rows=readObsoleteFormRound(context);
        if(number==null||number<=0||rows.size()!=1||rows.get(0).number()!=number)
            throw new IllegalStateException("作废绑定必须使用该真实表单原预留数字");
        var before=rows.get(0).bpm();
        if(before!=null) {
            if(!actualBpm.equals(before))throw new IllegalStateException("作废表单审批绑定不可替换");
            return number;
        }
        var conflicts=jdbc.queryForList("SELECT form_instance_id FROM dcc_application_round_link WHERE tenant_id=? AND application_type='OBSOLETE' AND application_id=? AND bpm_round=? FOR UPDATE",
                Long.class,context.tenant(),fileId,actualBpm);
        if(!conflicts.isEmpty())throw new IllegalStateException("作废审批已经绑定另一表单轮次");
        if(jdbc.update("UPDATE dcc_application_round_link SET bpm_round=? WHERE tenant_id=? AND project_id=? AND application_type='OBSOLETE' AND application_id=? AND attribute_round=? AND form_instance_id=? AND bpm_round IS NULL",
                actualBpm,context.tenant(),context.project(),fileId,number,formId)!=1)
            throw new IllegalStateException("作废原预留轮次CAS绑定失败");
        return number;
    }
    private record ObsoleteFormContext(Long tenant,Long project,Long file,Long form) {}
    private record ObsoleteFormRound(int number,String bpm) {}
    private java.util.List<ObsoleteFormRound> readObsoleteFormRound(ObsoleteFormContext context) {
        var rows=jdbc.query("SELECT project_id,application_type,application_id,attribute_round,bpm_round FROM dcc_application_round_link WHERE tenant_id=? AND form_instance_id=? FOR UPDATE",
                (r,n)->{
                    if(!Objects.equals(context.project(),r.getObject(1,Long.class))||!"OBSOLETE".equals(r.getString(2))
                            ||!Objects.equals(context.file(),r.getObject(3,Long.class))||r.getInt(4)<=0)
                        throw new IllegalStateException("作废表单预留映射身份不一致");
                    return new ObsoleteFormRound(r.getInt(4),r.getString(5));
                },context.tenant(),context.form());
        if(rows.size()>1)throw new IllegalStateException("作废表单数值草稿不唯一");
        return rows;
    }
    /** Lock order is actual Form -> Project -> Master -> File, matching exact FormCenter submit. */
    private ObsoleteFormContext lockObsoleteForm(Long actor,Long fileId,Long formId,String formCode,String submittedBpm) {
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("作废表单轮次必须参与真实外层事务");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var principal=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(actor==null||actor<=0||fileId==null||fileId<=0||formId==null||formId<=0||formCode==null||formCode.isBlank()||!formCode.equals(formCode.strip())
                ||principal==null||!Objects.equals(principal.getId(),actor)||!Objects.equals(principal.getTenantId(),tenant)||!Integer.valueOf(2).equals(principal.getUserType()))
            throw new IllegalArgumentException("作废表单真实认证身份不完整");
        var mapper=obsoleteForms.getIfAvailable();
        if(mapper==null)throw new IllegalStateException("正式作废表单来源未就绪");
        var form=mapper.selectByIdAndTenantForUpdate(tenant,formId);
        if(form==null||!Objects.equals(form.getId(),formId)||!Objects.equals(form.getTenantId(),tenant)||Boolean.TRUE.equals(form.getDeleted())
                ||!Objects.equals(form.getApplicantUserId(),actor)||!formCode.equals(form.getInstanceCode())
                ||!"DCC".equals(form.getSystemCode())||!"CONTROLLED_FILE".equals(form.getObjectType())||!"OBSOLETE".equals(form.getActionCode())
                ||!fileId.toString().equals(form.getObjectId())
                ||!(submittedBpm==null?"DRAFT".equals(form.getStatus())&&form.getBpmProcessInstanceId()==null
                    :"IN_APPROVAL".equals(form.getStatus())&&submittedBpm.equals(form.getBpmProcessInstanceId())))
            throw new IllegalArgumentException("作废表单与真实文件、申请人或提交状态不一致");
        var identity=files.selectById(fileId);
        if(identity==null||!Objects.equals(identity.getTenantId(),tenant)||identity.getDccProjectCodeId()==null||identity.getMasterId()==null)
            throw new IllegalArgumentException("作废表单文件项目身份缺失");
        var project=projects.selectByIdForUpdate(identity.getDccProjectCodeId());
        var master=obsoleteMasters.selectByIdForUpdate(identity.getMasterId());
        var file=files.selectByIdAndTenantForUpdate(tenant,fileId);
        if(project==null||!Objects.equals(project.getTenantId(),tenant)||Boolean.TRUE.equals(project.getDeleted())||!"ENABLE".equals(project.getStatus())
                ||master==null||!Objects.equals(master.getTenantId(),tenant)||Boolean.TRUE.equals(master.getDeleted())
                ||file==null||!Objects.equals(file.getId(),fileId)||!Objects.equals(file.getTenantId(),tenant)||Boolean.TRUE.equals(file.getDeleted())
                ||!Objects.equals(file.getMasterId(),master.getId())||!Objects.equals(file.getDccProjectCodeId(),project.getId())
                ||!Objects.equals(master.getDccProjectCodeId(),project.getId())||!Objects.equals(master.getCategoryId(),file.getCategoryId())
                ||!Objects.equals(master.getFileTypeTaxonomyLeafId(),file.getFileTypeTaxonomyId())
                ||!Objects.equals(master.getLatestControlledFileId(),fileId)||file.getControlledTime()==null
                ||!Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE").contains(file.getStatus())
                ||!Objects.equals(form.getObjectVersion(),file.getVersionNo())||!Objects.equals(form.getObjectState(),file.getStatus()))
            throw new IllegalArgumentException("作废表单对应受控File、Master与启用项目身份不一致");
        return new ObsoleteFormContext(tenant,project.getId(),fileId,formId);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public int reserveDraft(Long projectId,String type,Long applicationId) {
        Long tenant=lockIdentity(projectId,type,applicationId);
        var open=jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND bpm_round IS NULL FOR UPDATE",
                Integer.class,tenant,projectId,type,applicationId);
        if(open.size()>1)throw new IllegalStateException("正式草稿轮次不唯一");
        if(open.size()==1)return open.get(0);
        int round=nextRound(tenant,type,applicationId);
        if(jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(?,?,?,?,NULL,?)",
                tenant,projectId,type,applicationId,round)!=1)throw new IllegalStateException("草稿轮次预留失败");
        return requireDraft(type,applicationId);
    }
    public int requireDraft(String type,Long applicationId) {
        validateKey(type,applicationId);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var file=files.selectById(applicationId);
        if(file==null || !Objects.equals(file.getTenantId(),tenant))throw new IllegalArgumentException("草稿文件与租户不一致");
        var saved=jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND bpm_round IS NULL",
                Integer.class,tenant,file.getDccProjectCodeId(),type,applicationId);
        if(saved.size()!=1)throw new IllegalStateException("正式开放草稿轮次缺失或不唯一，禁止补建或猜测");
        return saved.get(0);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public int bindReservedDraft(Long projectId,String type,Long applicationId,Integer attributeRound,String realBpmId) {
        Long tenant=lockIdentity(projectId,type,applicationId);validateBpm(realBpmId);
        if(attributeRound==null || attributeRound<=0)throw new IllegalArgumentException("正式预留轮次输入不完整");
        var saved=jdbc.queryForList("SELECT bpm_round FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=? FOR UPDATE",
                String.class,tenant,projectId,type,applicationId,attributeRound);
        if(saved.size()!=1)throw new IllegalStateException("指定草稿轮次未预留");
        if(saved.get(0)!=null) {
            if(!realBpmId.equals(saved.get(0)))throw new IllegalStateException("已绑定轮次不能替换BPM");
            return attributeRound;
        }
        var conflict=jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND application_type=? AND application_id=? AND bpm_round=? FOR UPDATE",
                Integer.class,tenant,type,applicationId,realBpmId);
        if(!conflict.isEmpty())throw new IllegalStateException("BPM已绑定另一正式轮次");
        if(jdbc.update("UPDATE dcc_application_round_link SET bpm_round=? WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=? AND bpm_round IS NULL",
                realBpmId,tenant,projectId,type,applicationId,attributeRound)!=1)throw new IllegalStateException("草稿正式BPM绑定失败");
        return require(type,applicationId,realBpmId);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public int bind(Long projectId,String type,Long applicationId,String bpmRound) {
        Long tenant=lockIdentity(projectId,type,applicationId);validateBpm(bpmRound);
        var saved=jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND bpm_round=? FOR UPDATE",
                Integer.class,tenant,projectId,type,applicationId,bpmRound);
        if(saved.size()==1)return saved.get(0);
        if(!jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND application_type=? AND application_id=? AND bpm_round IS NULL FOR UPDATE",
                Integer.class,tenant,type,applicationId).isEmpty())throw new IllegalStateException("已有开放预留草稿，必须显式绑定确切轮次");
        int round=nextRound(tenant,type,applicationId);
        if(jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(?,?,?,?,?,?)",
                tenant,projectId,type,applicationId,bpmRound,round)!=1)throw new IllegalStateException("申请轮次映射保存失败");
        return round;
    }
    public int require(String type,Long applicationId,String bpmRound) {
        validateKey(type,applicationId);validateBpm(bpmRound);
        var saved=jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND application_type=? AND application_id=? AND bpm_round=?",
                Integer.class,TenantContextHolder.getRequiredTenantId(),type,applicationId,bpmRound);
        if(saved.size()!=1)throw new IllegalStateException("正式审批轮次尚未绑定属性快照，禁止猜测轮次");
        return saved.get(0);
    }
    /** 精确返工来源必须为正式已绑定映射，不允许任意submitted属性数字冒用。 */
    public void assertBoundRound(Long projectId,String type,Long fileId,Integer attributeRound) {
        validateKey(type,fileId);
        if(attributeRound==null || attributeRound<=0)throw new IllegalArgumentException("正式来源轮次缺失");
        var saved=jdbc.queryForList("SELECT bpm_round FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=? AND bpm_round IS NOT NULL",
                String.class,TenantContextHolder.getRequiredTenantId(),projectId,type,fileId,attributeRound);
        if(saved.size()!=1)throw new IllegalStateException("返工来源必须为正式已绑定BPM轮次");
        validateBpm(saved.get(0));
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void assertAllocatedRound(Long projectId,String type,Long fileId,Integer attributeRound) {
        Long tenant=lockIdentity(projectId,type,fileId);
        if(attributeRound==null || attributeRound<=0)throw new IllegalArgumentException("正式目标轮次缺失");
        var saved=jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=?",
                Integer.class,tenant,projectId,type,fileId,attributeRound);
        if(saved.size()!=1)throw new IllegalStateException("返工目标轮次尚未正式分配");
    }
    // BEGIN IC14 RELATION SOURCE METHODS — existing round/attribute methods remain unchanged.
    public record RelationSource(String sourceApplicationType,Long sourceApplicationId,
                                 String sourceBpmRound,String sourceRelationDigest) {}

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public RelationSource bindRelationSource(Long projectId,String targetType,Long targetFileId,Integer targetAttributeRound,
            String sourceType,Long sourceFileId,String sourceBpmRound,String sourceRelationDigest) {
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("关系来源绑定必须参与正式外层事务");
        Long tenant=lockIdentity(projectId,targetType,targetFileId);
        validateKey(sourceType,sourceFileId);validateBpm(sourceBpmRound);
        if(targetAttributeRound==null || targetAttributeRound<=0 || sourceRelationDigest==null
                || !sourceRelationDigest.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("关系来源身份或摘要不完整");
        var source=files.selectById(sourceFileId);var target=files.selectById(targetFileId);
        if(source==null || target==null || !Objects.equals(source.getTenantId(),tenant)
                || !Objects.equals(source.getDccProjectCodeId(),projectId) || !Objects.equals(source.getMasterId(),target.getMasterId()))
            throw new IllegalArgumentException("关系来源与目标不是同租户项目和正式文件链");
        var bound=jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND bpm_round=? FOR UPDATE",
                Integer.class,tenant,projectId,sourceType,sourceFileId,sourceBpmRound);
        if(bound.size()!=1)throw new IllegalStateException("关系来源必须为唯一正式已绑定申请轮次");
        var locked=jdbc.query("SELECT source_application_type,source_application_id,source_bpm_round,source_relation_digest "
                + "FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=? FOR UPDATE",
                (r,n)->new RelationSource(r.getString(1),r.getObject(2,Long.class),r.getString(3),r.getString(4)),
                tenant,projectId,targetType,targetFileId,targetAttributeRound);
        if(locked.size()!=1)throw new IllegalStateException("正式目标关系轮次未预约或不唯一");
        var before=locked.get(0);
        if(before.sourceApplicationType()==null && before.sourceApplicationId()==null && before.sourceBpmRound()==null && before.sourceRelationDigest()==null)before=null;
        var requested=new RelationSource(sourceType,sourceFileId,sourceBpmRound,sourceRelationDigest);
        if(before!=null) {
            validateKey(before.sourceApplicationType(),before.sourceApplicationId());validateBpm(before.sourceBpmRound());
            if(before.sourceRelationDigest()==null || !before.sourceRelationDigest().matches("[a-f0-9]{64}"))
                throw new IllegalStateException("正式关系来源字段不完整");
            if(!before.equals(requested))throw new IllegalStateException("目标申请已绑定不同关系来源");
            return before;
        }
        if(jdbc.update("UPDATE dcc_application_round_link SET source_application_type=?,source_application_id=?,source_bpm_round=?,source_relation_digest=? "
                + "WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=? "
                + "AND bpm_round IS NULL AND source_application_type IS NULL AND source_application_id IS NULL AND source_bpm_round IS NULL AND source_relation_digest IS NULL",
                sourceType,sourceFileId,sourceBpmRound,sourceRelationDigest,tenant,projectId,targetType,targetFileId,targetAttributeRound)!=1)
            throw new IllegalStateException("关系来源CAS绑定失败或目标轮次已经提交");
        return requested;
    }

    /** Only an existing allocated row with four NULL fields means no source was recorded. */
    public RelationSource readRelationSource(Long projectId,String targetType,Long targetFileId,Integer targetAttributeRound) {
        validateKey(targetType,targetFileId);
        if(projectId==null || projectId<=0 || targetAttributeRound==null || targetAttributeRound<=0)
            throw new IllegalArgumentException("正式目标关系轮次缺失");
        var rows=jdbc.query("SELECT source_application_type,source_application_id,source_bpm_round,source_relation_digest "
                + "FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=?",
                (r,n)->new RelationSource(r.getString(1),r.getObject(2,Long.class),r.getString(3),r.getString(4)),
                TenantContextHolder.getRequiredTenantId(),projectId,targetType,targetFileId,targetAttributeRound);
        if(rows.size()!=1)throw new IllegalStateException("正式目标关系轮次未预约或不唯一");
        var row=rows.get(0);
        if(row.sourceApplicationType()==null && row.sourceApplicationId()==null && row.sourceBpmRound()==null && row.sourceRelationDigest()==null)return null;
        validateKey(row.sourceApplicationType(),row.sourceApplicationId());validateBpm(row.sourceBpmRound());
        if(row.sourceRelationDigest()==null || !row.sourceRelationDigest().matches("[a-f0-9]{64}"))
            throw new IllegalStateException("正式关系来源字段不完整");
        return row;
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public RelationSource readRelationSourceForUpdate(Long projectId,String targetType,Long targetFileId,Integer targetAttributeRound) {
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("关系来源锁定读取必须参与正式外层事务");
        validateKey(targetType,targetFileId);
        if(projectId==null || projectId<=0 || targetAttributeRound==null || targetAttributeRound<=0)
            throw new IllegalArgumentException("正式目标关系轮次缺失");
        var rows=jdbc.query("SELECT source_application_type,source_application_id,source_bpm_round,source_relation_digest "
                + "FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? AND application_type=? AND application_id=? AND attribute_round=? FOR UPDATE",
                (r,n)->new RelationSource(r.getString(1),r.getObject(2,Long.class),r.getString(3),r.getString(4)),
                TenantContextHolder.getRequiredTenantId(),projectId,targetType,targetFileId,targetAttributeRound);
        if(rows.size()!=1)throw new IllegalStateException("正式目标关系轮次未预约或不唯一");
        var row=rows.get(0);
        if(row.sourceApplicationType()==null && row.sourceApplicationId()==null && row.sourceBpmRound()==null && row.sourceRelationDigest()==null)return null;
        validateKey(row.sourceApplicationType(),row.sourceApplicationId());validateBpm(row.sourceBpmRound());
        if(row.sourceRelationDigest()==null || !row.sourceRelationDigest().matches("[a-f0-9]{64}"))
            throw new IllegalStateException("正式关系来源字段不完整");
        return row;
    }
    /** IC16 source provenance: exact reverse mapping, never current/latest BPM guessing. */
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public String resolveBoundBpm(String applicationType,Long fileId,Integer attributeRound) {
        validateKey(applicationType,fileId);
        if(attributeRound==null || attributeRound<=0)throw new IllegalArgumentException("明确正式来源属性轮次缺失");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var file=files.selectById(fileId);
        if(file==null || !Objects.equals(file.getId(),fileId) || !Objects.equals(file.getTenantId(),tenant)
                || Boolean.TRUE.equals(file.getDeleted()) || file.getDccProjectCodeId()==null || file.getDccProjectCodeId()<=0)
            throw new IllegalArgumentException("明确来源文件与租户项目不一致");
        var project=projects.selectById(file.getDccProjectCodeId());
        if(project==null || !Objects.equals(project.getId(),file.getDccProjectCodeId()) || !Objects.equals(project.getTenantId(),tenant)
                || Boolean.TRUE.equals(project.getDeleted()))throw new IllegalArgumentException("来源项目正式身份不存在或租户不一致");
        var rows=jdbc.queryForList("SELECT bpm_round FROM dcc_application_round_link WHERE tenant_id=? AND project_id=? "
                + "AND application_type=? AND application_id=? AND attribute_round=?",String.class,tenant,file.getDccProjectCodeId(),applicationType,fileId,attributeRound);
        if(rows.size()!=1 || rows.get(0)==null)throw new IllegalStateException("明确来源属性轮次尚未唯一绑定正式BPM");
        validateBpm(rows.get(0));return rows.get(0);
    }
    // END IC14 RELATION SOURCE METHODS

    private Long lockIdentity(Long projectId,String type,Long applicationId) {
        validateKey(type,applicationId);Long tenant=TenantContextHolder.getRequiredTenantId();
        var project=projectId==null?null:projects.selectByIdForUpdate(projectId);var file=files.selectById(applicationId);
        if(project==null || !Objects.equals(project.getTenantId(),tenant) || !"ENABLE".equals(project.getStatus())
                || file==null || !Objects.equals(file.getTenantId(),tenant) || !Objects.equals(file.getDccProjectCodeId(),projectId))
            throw new IllegalArgumentException("申请、启用项目与租户不一致");
        return tenant;
    }
    private int nextRound(Long tenant,String type,Long id) {
        var previous=jdbc.queryForList("SELECT attribute_round FROM dcc_application_round_link WHERE tenant_id=? AND application_type=? AND application_id=? ORDER BY attribute_round DESC LIMIT 1 FOR UPDATE",
                Integer.class,tenant,type,id);
        return previous.isEmpty()?1:Math.addExact(previous.get(0),1);
    }
    private void validateKey(String type,Long id) {
        if(type==null || !Set.of("UPLOAD","REVISION","OBSOLETE").contains(type) || id==null || id<=0)throw new IllegalArgumentException("正式申请身份输入不完整");
    }
    private void validateBpm(String bpm) {
        if(bpm==null || bpm.isBlank() || bpm.length()>64 || !bpm.equals(bpm.trim()))throw new IllegalArgumentException("正式BPM输入不完整");
    }
}
