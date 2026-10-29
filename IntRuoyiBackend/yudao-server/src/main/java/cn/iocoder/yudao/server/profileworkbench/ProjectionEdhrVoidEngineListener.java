package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.bpm.workbench.EdhrVoidWorkbenchMapper;
import org.flowable.common.engine.api.delegate.event.*;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.*;

/** Only real task/identity mutations of the existing batch-void definition dirty its projection. */
@Component @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionEdhrVoidEngineListener extends AbstractFlowableEngineEventListener {
    private final EdhrVoidWorkbenchMapper mapper;
    public ProjectionEdhrVoidEngineListener(EdhrVoidWorkbenchMapper mapper) {
        super(Set.of(FlowableEngineEventType.ENTITY_CREATED,FlowableEngineEventType.ENTITY_UPDATED,
                FlowableEngineEventType.ENTITY_DELETED,FlowableEngineEventType.ENTITY_SUSPENDED,
                FlowableEngineEventType.ENTITY_ACTIVATED,FlowableEngineEventType.TASK_ASSIGNED));
        this.mapper=mapper;
    }
    @Override protected void entityCreated(FlowableEngineEntityEvent event){changed(event);}
    @Override protected void entityUpdated(FlowableEngineEntityEvent event){changed(event);}
    @Override protected void entityDeleted(FlowableEngineEntityEvent event){changed(event);}
    @Override protected void entitySuspended(FlowableEngineEntityEvent event){changed(event);}
    @Override protected void entityActivated(FlowableEngineEntityEvent event){changed(event);}
    @Override protected void taskAssigned(FlowableEngineEntityEvent event){changed(event);}
    private void changed(FlowableEngineEntityEvent event) {
        Object entity=event.getEntity();
        Task task;
        if(entity instanceof Task value)task=value;
        else if(entity instanceof IdentityLink link && "candidate".equals(link.getType()) && link.getTaskId()!=null) {
            task=CommandContextUtil.getTaskService().getTask(link.getTaskId());
            // Task deletion emits its own entity event even when its links are already removed.
            if(task==null && event.getType()==FlowableEngineEventType.ENTITY_DELETED)return;
            if(task==null)throw new IllegalStateException("EDHR candidate identity has no current engine task");
        } else return;
        if(mapper.countVoidDefinition(task.getTenantId(),task.getProcessDefinitionId())!=1)return;
        ProjectionWriteContext held=ProjectionWriteContext.CURRENT.get();
        if(held==null)throw new IllegalStateException("EDHR void engine mutation without projection fence");
        if(!Objects.equals(Long.toString(held.tenant),task.getTenantId())) {
            held.failed=true;throw new IllegalStateException("EDHR void engine mutation changed tenant");
        }
        held.changed=true;
    }
    @Override public boolean isFailOnException(){return true;}
}
