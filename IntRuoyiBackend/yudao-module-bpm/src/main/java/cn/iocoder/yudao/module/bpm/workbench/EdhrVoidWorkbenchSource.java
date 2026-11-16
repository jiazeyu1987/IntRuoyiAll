package cn.iocoder.yudao.module.bpm.workbench;

import cn.iocoder.yudao.module.bpm.approval.service.ApprovalFlowableTaskQueryService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.workbench.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** Read-only projection of the existing Flowable task; approval remains owned by BPM. */
@Component
public class EdhrVoidWorkbenchSource implements SourceAdapter {
    public static final List<String> REQUIRED_PERMISSIONS=List.of("bpm:task:query","bpm:process-instance:query","bpm:task:update");
    private final EdhrVoidWorkbenchMapper mapper;
    private final ApprovalFlowableTaskQueryService queries;
    private final PermissionApi permissions;
    public EdhrVoidWorkbenchSource(EdhrVoidWorkbenchMapper mapper,ApprovalFlowableTaskQueryService queries,PermissionApi permissions) {
        this.mapper=mapper;this.queries=queries;this.permissions=permissions;
    }
    @Override public SourceDescriptor descriptor() {return new SourceDescriptor("BPM_EDHR_VOID","bpm",1,5,"批次作废审批","批记录","bpm.edhrVoid.policy","bpm.edhrVoid.open",1);}
    @Override public RowOrderContract rowOrderContract(){return RowOrderContract.UTF8_BINARY_DESC;}
    @Override public SourceIdentityContract identityContract(){return SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY;}
    @Override public boolean isAuthorized(long user,long tenant){return REQUIRED_PERMISSIONS.stream().allMatch(permission->permissions.hasAnyPermissions(user,permission));}
    Map<String,Object> parameters(QueryContext q,boolean export) {
        q.validate();
        var p=queries.edhrVoidWorkbenchParameters(q.getTenantId(),q.getUserId(),export);
        p.put("q",q);
        requireDomain(p);
        return p;
    }
    void requireDomain(Map<String,Object> p) {
        if(mapper.countBrokenDomain(p)!=0)throw new IllegalStateException("EDHR_VOID_WORKBENCH_FORMAL_RELATION_REQUIRED");
    }
    private void requireAuthorized(QueryContext q) {
        q.validate();
        if(!isAuthorized(q.getUserId(),q.getTenantId()))throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
    }
    @Override public long count(QueryContext q) {
        requireAuthorized(q);
        Long count=mapper.count(parameters(q,false));
        if(count==null||count<0)throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
        return count;
    }
    @Override public TaskChunk readChunk(QueryContext q,ChunkCursor after,int limit) {
        if(limit!=100)throw WorkbenchFailure.INVALID_QUERY.exception();
        q.validatedChunk(after,limit+1);
        requireAuthorized(q);
        var p=parameters(q,false);p.put("after",after);p.put("fetchLimit",limit+1);
        return TaskChunk.from(mapper.chunk(p).stream().map(this::project).toList(),limit);
    }
    @Override public TaskSummary resolveVisibilityIdentity(QueryContext q,String id) {
        requireAuthorized(q);
        if(id==null||id.isBlank()||id.length()>128)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
        var p=parameters(q,false);p.put("taskId",id);p.put("lockIdentity",true);
        var rows=mapper.rows(p);
        if(rows.size()!=1)throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
        return project(rows.get(0));
    }
    TaskSummary project(EdhrVoidWorkbenchRow row) {return row.summary(action(row));}
    ActionEnvelope action(EdhrVoidWorkbenchRow row) {
        if(row.getBusinessId()==null||row.getBusinessId().isBlank()||row.getProcessInstanceId()==null||row.getProcessInstanceId().isBlank())
            throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
        return new ActionEnvelope(descriptor().actionKind(),1,Map.of("taskId",row.getBusinessId(),"processInstanceId",row.getProcessInstanceId()));
    }
}
