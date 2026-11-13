package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesLegacyApprovalPageMapper;
import org.flowable.engine.*;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

@Service
public class MesLegacyApprovalPageQuery {
    @Resource private MesLegacyApprovalPageMapper mapper;
    @Resource private TaskService tasks;
    @Resource private HistoryService history;
    @Resource private ManagementService management;
    @Resource private ProcessEngineConfiguration configuration;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResult<Task> todo(Long actor,MesProBatchRecordExecutionApprovalPageReqVO req){
        var query=authorized(actor,false);verifySource(query,false);filters(query,req);long count=mapper.countTodo(query);
        if(count==0)return new PageResult<>(List.of(),0L);
        window(query,req,false);var ids=mapper.todo(query);
        var rows=ids.isEmpty()?List.<Task>of():tasks.createTaskQuery().taskIds(new HashSet<>(ids))
                .taskTenantId(TenantContextHolder.getRequiredTenantId().toString()).active().includeProcessVariables().list();
        return new PageResult<>(ordered(ids,rows,Task::getId),count);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResult<HistoricTaskInstance> done(Long actor,MesProBatchRecordExecutionApprovalPageReqVO req){
        var query=authorized(actor,true);verifySource(query,true);filters(query,req);long count=mapper.countDone(query);
        if(count==0)return new PageResult<>(List.of(),0L);
        window(query,req,true);var ids=mapper.done(query);
        var rows=ids.isEmpty()?List.<HistoricTaskInstance>of():history.createHistoricTaskInstanceQuery().taskIds(new HashSet<>(ids))
                .taskTenantId(TenantContextHolder.getRequiredTenantId().toString()).finished().includeTaskLocalVariables().list();
        return new PageResult<>(ordered(ids,rows,HistoricTaskInstance::getId),count);
    }
    private QueryWrapperX<Object> authorized(Long actor,boolean done){
        Objects.requireNonNull(actor,"EDHR_APPROVAL_LOGIN_REQUIRED");
        if(!"mysql".equals(configuration.getDatabaseType()) || configuration.getDatabaseTablePrefix()!=null&&!configuration.getDatabaseTablePrefix().isEmpty())
            throw new IllegalStateException("EDHR_APPROVAL_QUERY_REQUIRES_MYSQL_UNPREFIXED_FLOWABLE");
        var q=new QueryWrapperX<Object>();String tenant=TenantContextHolder.getRequiredTenantId().toString();
        q.eq("t.TENANT_ID_",tenant).eq("d.TENANT_ID_",tenant).eq("d.KEY_","mes-edhr-approval-v1");
        if(done){q.isNotNull("t.END_TIME_").eq("t.ASSIGNEE_",actor.toString())
                .and(w->w.isNull("t.TASK_DEF_KEY_").or().ne("t.TASK_DEF_KEY_",cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants.START_USER_NODE_ID));}
        else {
            var engineQuery=tasks.createTaskQuery().taskCandidateOrAssigned(actor.toString());
            if(!(engineQuery instanceof TaskQueryImpl actual))throw new IllegalStateException("EDHR_APPROVAL_FLOWABLE_VERSION_UNSUPPORTED");
            Collection<String> groups=management.executeCommand(new cn.iocoder.yudao.module.bpm.approval.service.ApprovalFlowableTaskQueryService.CandidateGroupsRead(actual));
            String groupSql=groups==null?" OR il.GROUP_ID_ IN (SELECT m.GROUP_ID_ FROM ACT_ID_MEMBERSHIP m WHERE m.USER_ID_={0})":"";
            Object[] args={actor.toString()};
            if(groups!=null&&!groups.isEmpty()){
                var values=new ArrayList<Object>();values.add(actor.toString());values.addAll(groups);args=values.toArray();
                groupSql=" OR il.GROUP_ID_ IN ("+java.util.stream.IntStream.range(1,args.length).mapToObj(i->"{"+i+"}").collect(java.util.stream.Collectors.joining(","))+")";
            }
            q.eq("t.SUSPENSION_STATE_",1).apply("(t.ASSIGNEE_={0} OR (t.ASSIGNEE_ IS NULL AND EXISTS (SELECT 1 FROM ACT_RU_IDENTITYLINK il WHERE il.TASK_ID_=t.ID_ AND il.TYPE_='candidate' AND (il.USER_ID_={0}"+groupSql+"))))",args);
        }
        return q;
    }
    private void verifySource(QueryWrapperX<Object> authorized,boolean done){
        var broken=authorized.clone();broken.and(q->q.isNull("s.id").or().isNull("x.id").or().apply("(SELECT COUNT(*) FROM mes_pro_batch_record_approval_snapshot ds WHERE ds.process_instance_id=t.PROC_INST_ID_ AND ds.deleted=0 AND ds.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED))<>1"));
        if((done?mapper.countDone(broken):mapper.countTodo(broken))!=0)throw new IllegalStateException("EDHR_APPROVAL_PERSISTED_SOURCE_MISSING_OR_DUPLICATE");
    }
    private static void filters(QueryWrapperX<Object> q,MesProBatchRecordExecutionApprovalPageReqVO req){
        contains(q,"x.execution_code",req.getExecutionCode());contains(q,"x.work_order_code",req.getWorkOrderCode());contains(q,"x.batch_code",req.getBatchCode());
        if(req.getSubmittedBy()!=null)q.eq("x.submitted_by",req.getSubmittedBy());
        if(req.getSubmittedAtStart()!=null)q.ge("x.submitted_at",req.getSubmittedAtStart());
        if(req.getSubmittedAtEnd()!=null)q.le("x.submitted_at",req.getSubmittedAtEnd());
        ListFilterQueryUtils.apply(req,q,Map.of("executionCode",text("x.execution_code"),"workOrderCode",text("x.work_order_code"),"batchCode",text("x.batch_code"),"submittedBy",number("x.submitted_by"),"submittedAt",dateTime("x.submitted_at")));
    }
    private static void contains(QueryWrapperX<Object> q,String column,String value){if(value!=null&&!value.isBlank())q.apply("LOCATE(LOWER({0}),LOWER(COALESCE("+column+",'')))>0",value.trim());}
    private static void window(QueryWrapperX<Object> q,MesProBatchRecordExecutionApprovalPageReqVO req,boolean done){
        long page=req.getPageNo()==null?1:req.getPageNo(),size=req.getPageSize()==null?10:req.getPageSize();
        if(page<1||size<1)throw new IllegalArgumentException("QUERY_WINDOW_RANGE_UNSUPPORTED");
        q.last("ORDER BY t."+(done?"END_TIME_":"CREATE_TIME_")+" DESC,CAST(t.ID_ AS BINARY) ASC LIMIT "+Math.multiplyExact(page-1,size)+","+size);
    }
    private static <T> List<T> ordered(List<String> ids,List<T> rows,Function<T,String> key){
        var values=new HashMap<String,T>();for(var row:rows)if(values.put(key.apply(row),row)!=null)throw new IllegalStateException("EDHR_APPROVAL_SOURCE_DUPLICATE");
        if(values.size()!=ids.size())throw new IllegalStateException("EDHR_APPROVAL_SOURCE_WINDOW_CHANGED");
        return ids.stream().map(id->Objects.requireNonNull(values.get(id),"EDHR_APPROVAL_SOURCE_WINDOW_CHANGED")).toList();
    }
}
