package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import java.util.Objects;

/** Formal bridge; no dependency on relation-enriched detail reads, avoiding circular authorization. */
@Service
public class DccRelationAccessPolicyImpl implements DccRelationAccessPolicy {
    @Lazy @Resource private DccControlledFileQueryService query;
    @Resource private DccControlledFileTaskAssigneeSnapshotMapper obligations;
    @Resource private TaskService tasks;
    @Resource private AdminUserApi users;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccProjectAccessService projects;
    public void assertNameVisible(Long actor,Long id) { query.assertRelationNameVisible(actor,id); }
    public void assertContentReadable(Long actor,Long id) { query.assertRelationContentReadable(actor,id); }
    public void assertCanEditRelations(Long actor,Long id) { query.assertRelationEditable(actor,id); }
    private void requireParticipant(Long actor,Long fileId,String round) {
        assertNameVisible(actor,fileId);
        if(actor==null || round==null || round.isBlank() || obligations.selectListByControlledFileId(fileId).stream()
                .noneMatch(row->Objects.equals(row.getTenantId(),TenantContextHolder.getRequiredTenantId())
                        && Objects.equals(row.getProcessInstanceId(),round)
                        && "MATRIX_REVIEW".equals(row.getStageCode())
                        && (Objects.equals(row.getLeaderUserId(),actor) || Objects.equals(row.getAssigneeUserId(),actor))))
            throw new DccRelationFailure("DCC_RELATION_ARRANGEMENT_FORBIDDEN");
    }
    public void assertCanArrange(Long actor,Long fileId,String round) {
        requireParticipant(actor,fileId,round);
        var active=tasks.createTaskQuery().processInstanceId(round).taskDefinitionKey("MATRIX_REVIEW")
                .taskAssignee(String.valueOf(actor)).taskTenantId(String.valueOf(TenantContextHolder.getRequiredTenantId())).list();
        if(active.isEmpty()) throw new DccRelationFailure("DCC_RELATION_SIGNOFF_TASK_MISSING");
    }
    public void assertCanReadArrangements(Long actor,Long fileId,String round) { requireParticipant(actor,fileId,round); }
    public void assertAssigneeAvailable(Long actor,Long masterId) {
        var account=actor==null?null:users.getUser(actor);
        var master=masterId==null?null:masters.selectById(masterId);
        if(account==null || !Objects.equals(account.getId(),actor) || !Integer.valueOf(0).equals(account.getStatus())
                || master==null || !Objects.equals(master.getTenantId(),TenantContextHolder.getRequiredTenantId()))
            throw new DccRelationFailure("DCC_RELATION_ASSIGNEE_UNAVAILABLE");
        projects.assertProjectEditorOrOwner(actor,master.getDccProjectCodeId());
    }
}
