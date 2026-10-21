package cn.iocoder.yudao.module.dcc.workbench;

import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.workbench.index.*;
import cn.iocoder.yudao.module.dcc.service.file.DccDistributionWorkbenchSource;
import cn.iocoder.yudao.module.dcc.service.file.DccTrainingWorkbenchSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionExportSupport.*;

@Component @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class DccProjectionExport implements WorkbenchProjectionExport {
    private final DccProjectionExportMapper mapper;
    private final WorkbenchAuthorityExport authority;
    private final SourceDescriptor distribution,training;
    public DccProjectionExport(DccProjectionExportMapper mapper,WorkbenchAuthorityExport authority,
                               DccDistributionWorkbenchSource distribution,DccTrainingWorkbenchSource training) {
        this.mapper=mapper;this.authority=authority;this.distribution=distribution.descriptor();this.training=training.descriptor();
    }
    @Override public String ownerModule(){return "dcc";}
    @Override public Set<String> sourceIds(){return Set.of(distribution.sourceId(),training.sourceId());}
    @Override public Set<String> writerIds(){return Set.of("dcc.formal.task-facts");}
    @Override public List<Task> exportTasks(long tenant,Watermark watermark) {
        List<Task> tasks=new ArrayList<>();
        for(var row:mapper.exportDistribution(query(tenant))) tasks.add(task(tenant,watermark,distribution,row,
            new ActionEnvelope(distribution.actionKind(),1,Map.of("controlledFileId",row.getControlledFileId(),"distributionId",row.getDistributionId(),"recipientId",row.getBusinessId())),AudienceKind.PERSON,candidates(row.getOwnerUserId(),null)));
        for(var row:mapper.exportTraining(query(tenant))) tasks.add(task(tenant,watermark,training,row,
            new ActionEnvelope(training.actionKind(),1,Map.of("controlledFileId",row.getControlledFileId(),"progressId",row.getBusinessId())),AudienceKind.PERSON,candidates(row.getOwnerUserId(),null)));
        return List.copyOf(tasks);
    }
    @Override public List<Authorization> exportAuthorizations(long tenant,Watermark watermark) {
        List<Task> tasks=exportTasks(tenant,watermark);List<Authorization> values=new ArrayList<>();
        for(SourceDescriptor d:List.of(distribution,training)) {
            String permission=d==distribution?"dcc:controlled-file:query":"dcc:controlled-file:training:mine";
            values.addAll(authorizations(tenant,watermark,d,tasks.stream().filter(t->t.sourceId().equals(d.sourceId())).toList(),authority.exportGrants(tenant,new WorkbenchAuthorityExport.Requirement(WorkbenchAuthorityExport.Mode.ANY_PERMISSION,Set.of(permission)))));
        }
        return List.copyOf(values);
    }
}
