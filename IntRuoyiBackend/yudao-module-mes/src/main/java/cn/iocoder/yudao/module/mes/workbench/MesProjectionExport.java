package cn.iocoder.yudao.module.mes.workbench;

import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.workbench.index.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrWorkbenchSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionExportSupport.*;

@Component @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class MesProjectionExport implements WorkbenchProjectionExport {
    private final MesProjectionExportMapper mapper;
    private final WorkbenchAuthorityExport authority;
    private final SourceDescriptor edhr;
    public MesProjectionExport(MesProjectionExportMapper mapper,WorkbenchAuthorityExport authority,
                               MesEdhrWorkbenchSource edhr) {
        this.mapper=mapper;this.authority=authority;this.edhr=edhr.descriptor();
    }
    @Override public String ownerModule(){return "mes";}
    @Override public Set<String> sourceIds(){return Set.of(edhr.sourceId());}
    @Override public Set<String> writerIds(){return Set.of("mes.formal.task-facts");}
    @Override public List<Task> exportTasks(long tenant,Watermark watermark) {
        List<Task> tasks=new ArrayList<>();
        List<MesEdhrWorkbenchRow> edhrRows=new ArrayList<>(mapper.exportEdhr(query(tenant)));
        edhrRows.addAll(mapper.exportHandoff(query(tenant)));
        for(var row:edhrRows) {
            Map<String,String> payload=new LinkedHashMap<>();payload.put("id",row.getBusinessId());payload.put("taskType",row.getDomainTaskType());
            if("批次冻结，暂不可处理".equals(row.getStatusLabel())) payload.put("blockedReason","批次已冻结，待不合格/偏差处理解除后才能继续此任务");
            put(payload,"actionUrl",row.getActionUrl());put(payload,"batchExecutionId",row.getBatchExecutionId());put(payload,"batchTaskId",row.getBatchTaskId());put(payload,"executionId",row.getExecutionId());put(payload,"businessScopeType",row.getBusinessScopeType());put(payload,"businessScopeId",row.getBusinessScopeId());
            tasks.add(task(tenant,watermark,edhr,row,new ActionEnvelope(edhr.actionKind(),1,payload),AudienceKind.CANDIDATE_SET,candidates(row.getOwnerUserId(),row.getCandidateUserSnapshot())));
        }
        return List.copyOf(tasks);
    }
    private static void put(Map<String,String> payload,String key,String value){if(value!=null)payload.put(key,value);}
    @Override public List<Authorization> exportAuthorizations(long tenant,Watermark watermark) {
        List<Task> tasks=exportTasks(tenant,watermark);List<Authorization> values=new ArrayList<>();
        for(SourceDescriptor d:List.of(edhr)) {
            Set<String> permissions=Set.of("mes:pro-edhr-work-task:query","mes:pro-edhr-batch-execution:query","mes:pro-process-pool-team-leader:query","mes:pro-edhr-nonconformance-review:query");
            values.addAll(authorizations(tenant,watermark,d,tasks.stream().filter(t->t.sourceId().equals(d.sourceId())).toList(),authority.exportGrants(tenant,new WorkbenchAuthorityExport.Requirement(WorkbenchAuthorityExport.Mode.ANY_PERMISSION,permissions))));
        }
        return List.copyOf(values);
    }
}
