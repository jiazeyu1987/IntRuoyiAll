package cn.iocoder.yudao.module.mes.workbench;

import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.workbench.index.*;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesWorkOrderWorkbenchSource;
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
    private final SourceDescriptor edhr,workOrder;
    public MesProjectionExport(MesProjectionExportMapper mapper,WorkbenchAuthorityExport authority,
                               MesEdhrWorkbenchSource edhr,MesWorkOrderWorkbenchSource workOrder) {
        this.mapper=mapper;this.authority=authority;this.edhr=edhr.descriptor();this.workOrder=workOrder.descriptor();
    }
    @Override public String ownerModule(){return "mes";}
    @Override public Set<String> sourceIds(){return Set.of(edhr.sourceId(),workOrder.sourceId());}
    @Override public Set<String> writerIds(){return Set.of("mes.formal.task-facts");}
    @Override public List<Task> exportTasks(long tenant,Watermark watermark) {
        List<Task> tasks=new ArrayList<>();
        for(var row:mapper.exportEdhr(query(tenant))) {
            Map<String,String> payload=new LinkedHashMap<>();payload.put("id",row.getBusinessId());payload.put("taskType",row.getDomainTaskType());
            put(payload,"actionUrl",row.getActionUrl());put(payload,"batchExecutionId",row.getBatchExecutionId());put(payload,"batchTaskId",row.getBatchTaskId());put(payload,"executionId",row.getExecutionId());put(payload,"businessScopeType",row.getBusinessScopeType());put(payload,"businessScopeId",row.getBusinessScopeId());
            tasks.add(task(tenant,watermark,edhr,row,new ActionEnvelope(edhr.actionKind(),1,payload),AudienceKind.CANDIDATE_SET,candidates(row.getOwnerUserId(),row.getCandidateUserSnapshot())));
        }
        for(var row:mapper.exportWorkOrder(query(tenant))) tasks.add(task(tenant,watermark,workOrder,row,
            new ActionEnvelope(workOrder.actionKind(),1,row.getCode()==null?Map.of():Map.of("code",row.getCode())),AudienceKind.TENANT_PERMISSION_QUEUE,Set.of()));
        return List.copyOf(tasks);
    }
    private static void put(Map<String,String> payload,String key,String value){if(value!=null)payload.put(key,value);}
    @Override public List<Authorization> exportAuthorizations(long tenant,Watermark watermark) {
        List<Task> tasks=exportTasks(tenant,watermark);List<Authorization> values=new ArrayList<>();
        for(SourceDescriptor d:List.of(edhr,workOrder)) {
            Set<String> permissions=d==edhr?Set.of("mes:pro-edhr-work-task:query","mes:pro-edhr-batch-execution:query"):Set.of("mes:pro-work-order:query");
            values.addAll(authorizations(tenant,watermark,d,tasks.stream().filter(t->t.sourceId().equals(d.sourceId())).toList(),authority.exportGrants(tenant,new WorkbenchAuthorityExport.Requirement(WorkbenchAuthorityExport.Mode.ANY_PERMISSION,permissions))));
        }
        return List.copyOf(values);
    }
}
