package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.ApprovalProcessOwnershipService;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.flowable.engine.*;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants.START_USER_NODE_ID;

@Service
public class ApprovalFlowableTaskQueryService {
    private final ApprovalFlowableTaskQueryMapper mapper;
    private final TaskService tasks;
    private final HistoryService history;
    private final ProcessEngineConfiguration configuration;
    private final PermissionApi permissions;
    private final ManagementService management;
    private final ApprovalProcessOwnershipService ownership;
    public ApprovalFlowableTaskQueryService(ApprovalFlowableTaskQueryMapper mapper, TaskService tasks,
            HistoryService history, ProcessEngineConfiguration configuration, PermissionApi permissions,
            ManagementService management,ApprovalProcessOwnershipService ownership) {
        this.mapper=mapper; this.tasks=tasks; this.history=history; this.configuration=configuration;
        this.permissions=permissions;
        this.management=management;
        this.ownership=ownership;
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResult<Task> todo(ApprovalTaskQueryContext context,String projection) {
        Map<String,Object> p=parameters(context,projection,false); verify(p);
        long count=mapper.count(p); List<String> ids=mapper.window(p);
        if(ids.isEmpty()) return new PageResult<>(List.of(),count);
        var rows=tasks.createTaskQuery().taskIds(new HashSet<>(ids)).taskTenantId((String)p.get("tenantText"))
                .active().includeProcessVariables().list();
        return new PageResult<>(exactOrder(ids,rows,Task::getId),count);
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResult<HistoricTaskInstance> done(ApprovalTaskQueryContext context,String projection) {
        Map<String,Object> p=parameters(context,projection,true); verify(p);
        long count=mapper.count(p); List<String> ids=mapper.window(p);
        if(ids.isEmpty()) return new PageResult<>(List.of(),count);
        var rows=history.createHistoricTaskInstanceQuery().taskIds(new HashSet<>(ids))
                .taskTenantId((String)p.get("tenantText")).finished().includeTaskLocalVariables().list();
        return new PageResult<>(exactOrder(ids,rows,HistoricTaskInstance::getId),count);
    }

    private void verify(Map<String,Object> p) {
        if(mapper.countBrokenProvenance(p)!=0) throw new IllegalStateException("APPROVAL_OWNERSHIP_PROVENANCE_INCOMPLETE");
        Objects.requireNonNull(mapper.ownerships(p),"APPROVAL_OWNERSHIP_ROWS_REQUIRED")
                .forEach(row->{ApprovalProcessOwnershipService.verify(row);ownership.verifyRegistrationSnapshot(row);});
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResult<org.flowable.engine.history.HistoricProcessInstance> initiated(ApprovalTaskQueryContext context) {
        var p=parameters(context,"BPM",true);
        long count=mapper.countInitiated(p); var ids=mapper.windowInitiated(p);
        var rows=ids.stream().map(id->Objects.requireNonNull(history.createHistoricProcessInstanceQuery()
                .processInstanceId(id).processInstanceTenantId((String)p.get("tenantText")).includeProcessVariables().singleResult(),
                "APPROVAL_PROCESS_WINDOW_CHANGED")).toList();
        return new PageResult<>(rows,count);
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResult<cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmProcessInstanceCopyDO> copied(ApprovalTaskQueryContext context) {
        var p=parameters(context,"BPM",true);
        return new PageResult<>(mapper.windowCopied(p),mapper.countCopied(p));
    }

    private Map<String,Object> parameters(ApprovalTaskQueryContext context,String projection,boolean done) {
        if(!Set.of("BPM","DCC_TASK").contains(projection)) throw new IllegalArgumentException("APPROVAL_PROJECTION_UNSUPPORTED");
        if(!"mysql".equals(configuration.getDatabaseType()) || configuration.getDatabaseTablePrefix()!=null
                && !configuration.getDatabaseTablePrefix().isEmpty())
            throw new IllegalStateException("APPROVAL_QUERY_REQUIRES_MYSQL_UNPREFIXED_FLOWABLE");
        Long tenant=TenantContextHolder.getRequiredTenantId(); Long user=context.getLoginUserId();
        if(user==null || user<=0) throw new IllegalArgumentException("APPROVAL_QUERY_ACTOR_REQUIRED");
        ApprovalQueryWindow w=ApprovalQueryWindow.from(context);
        var p=new HashMap<String,Object>(); p.put("tenant",tenant); p.put("tenantText",tenant.toString());
        p.put("userId",user); p.put("userText",user.toString()); p.put("global",context.isGlobalView());
        p.put("done",done); p.put("projection",projection); p.put("startNode",START_USER_NODE_ID);
        p.put("offset",w.offset());p.put("limit",w.limit());p.put("keyword",literalKeyword(context.getKeyword()));
        Collection<String> groups=null;
        if(!done && !context.isGlobalView()) {
            var query=tasks.createTaskQuery().taskCandidateOrAssigned(user.toString());
            if(!(query instanceof TaskQueryImpl actual)) throw new IllegalStateException("APPROVAL_QUERY_FLOWABLE_VERSION_UNSUPPORTED");
            groups=management.executeCommand(commandContext->actual.getCandidateGroups());
        }
        p.put("candidateGroups",groups);
        boolean role=!done && !context.isGlobalView()
                && permissions.hasAnyRoles(user,"dcc_registration_certificate_approver");
        p.put("registrationUpload",role && permissions.hasAnyPermissions(user,"dcc:registration-certificate:upload:approve"));
        p.put("registrationDownload",role && permissions.hasAnyPermissions(user,"dcc:registration-certificate:access-request:approve"));
        return p;
    }

    static String literalKeyword(String keyword) {
        if(keyword==null || keyword.isBlank()) return null;
        return "%"+keyword.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public void requireNativeTaskAuthority(String taskId,Long actor) {
        var context=ApprovalTaskQueryContext.of(actor,ApprovalTaskViewType.TODO,
                cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode.BPM,null,1,1,false);
        var p=parameters(context,"BPM",false);p.put("taskId",taskId);verify(p);
        if(mapper.count(p)!=1) throw new IllegalStateException("APPROVAL_TASK_AUTHORITY_REQUIRED");
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public boolean isFormalRegistrationCandidate(String taskId,Long actor) {
        var context=ApprovalTaskQueryContext.of(actor,ApprovalTaskViewType.TODO,
                cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode.BPM,null,1,1,false);
        var p=parameters(context,"BPM",false);p.put("taskId",taskId);p.put("registrationOnly",true);verify(p);
        return mapper.count(p)==1;
    }
    static <T> List<T> exactOrder(List<String> ids,List<T> rows,java.util.function.Function<T,String> identity) {
        var byId=new HashMap<String,T>();
        for(T row:Objects.requireNonNull(rows,"APPROVAL_QUERY_SOURCE_ROWS_REQUIRED"))
            if(byId.put(identity.apply(row),row)!=null) throw new IllegalStateException("APPROVAL_QUERY_SOURCE_DUPLICATE");
        if(byId.size()!=ids.size()) throw new IllegalStateException("APPROVAL_QUERY_SOURCE_WINDOW_CHANGED");
        return ids.stream().map(id->Objects.requireNonNull(byId.get(id),"APPROVAL_QUERY_SOURCE_WINDOW_CHANGED")).toList();
    }
}
