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
