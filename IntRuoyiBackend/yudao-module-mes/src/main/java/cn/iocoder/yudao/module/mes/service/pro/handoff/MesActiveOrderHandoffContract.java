package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import java.net.URI;
import java.util.*;

public final class MesActiveOrderHandoffContract {
    // sourceId always identifies the event; sourceType identifies the table owning roundId.
    public static final String EVENT_REJECTED_REVIEW="PROCESS_POOL_EVENT_REJECTED_REVIEW";
    public static final String EVENT_SIGNED_REVISION="PROCESS_POOL_EVENT_SIGNED_REVISION";
    public static final String TEMPLATE="MES_ACTIVE_ORDER_HANDOFF";
    public static final Set<String> TYPES=Set.of("PRODUCTION_HANDOFF","PRODUCTION_REVIEW","PQC_HANDOFF","PQC_REVIEW","PRODUCTION_RETURN","PQC_RETURN","QA_REVIEW","QA_DECISION_HANDOFF");
    private MesActiveOrderHandoffContract() { }
    public static ServiceException failure(String text) { return new ServiceException(1_076_041_001,text); }
    public static void require(boolean condition,String text) { if(!condition)throw failure(text); }
    public static Long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    public static List<Long> candidates(String snapshot) {
        require(snapshot!=null&&!snapshot.isBlank(),"交接责任候选快照缺失");
        var ids=new TreeSet<Long>();
        for(String part:snapshot.split(",",-1)) {
            require(part.matches("[1-9][0-9]*"),"交接责任候选身份无效");
            try { require(ids.add(Long.parseLong(part)),"交接责任候选身份重复"); }
            catch(NumberFormatException invalid) { throw failure("交接责任候选身份超出范围"); }
        }
        return List.copyOf(ids);
    }
    public static void access(MesActiveOrderHandoffTaskDO task,Long actor) {
        require(task!=null&&Objects.equals(tenant(),task.getTenantId())&&actor!=null
                &&candidates(task.getCandidateUserSnapshot()).contains(actor),"无权访问此周期交接");
    }
    public static void validate(MesActiveOrderHandoffTaskDO task) {
        require(task!=null&&task.getActiveOrderId()!=null&&task.getActiveOrderId()>0&&task.getSourceId()!=null
                &&task.getSourceId()>0&&task.getRoundId()!=null&&task.getRoundId()>0&&TYPES.contains(task.getTaskType()),"交接正式来源或周期身份缺失");
        candidates(task.getCandidateUserSnapshot());
        if(Set.of("PRODUCTION_REVIEW","PQC_HANDOFF","PQC_REVIEW","PRODUCTION_RETURN","PQC_RETURN").contains(task.getTaskType()))
            require(task.getRouteProcessId()!=null&&task.getRouteProcessId()>0,"逐工序交接缺少正式工序绑定");
        URI uri;
        try { uri=URI.create(Objects.requireNonNull(task.getActionUrl())); }
        catch(RuntimeException invalid) { throw failure("交接真实页面入口无效"); }
        require(!uri.isAbsolute()&&uri.getRawAuthority()==null&&uri.getFragment()==null,"交接入口必须为本系统真实页面");
        var query=actionQuery(task);
        String sourceKey=switch(task.getTaskType()) {
            case "PRODUCTION_HANDOFF" -> "cycleId";
            case "PQC_HANDOFF" -> "pqcTaskId";
            case "QA_REVIEW","QA_DECISION_HANDOFF" -> "reviewId";
            default -> "eventId";
        };
        String sourceType=switch(sourceKey) {case "cycleId"->"ACTIVE_ORDER";case "pqcTaskId"->"PQC_INSPECTION_TASK";case "reviewId"->"NONCONFORMANCE_REVIEW";default->"PROCESS_POOL_EVENT";};
        boolean review=Set.of("PRODUCTION_REVIEW","PQC_REVIEW").contains(task.getTaskType());
        require(Objects.equals(sourceType,task.getSourceType()) || (review && EVENT_SIGNED_REVISION.equals(task.getSourceType()))
                || ("PRODUCTION_REVIEW".equals(task.getTaskType()) && EVENT_REJECTED_REVIEW.equals(task.getSourceType())),"交接类型与正式来源不一致");
        Set<String> paths=switch(task.getTaskType()) {
            case "PRODUCTION_HANDOFF","PRODUCTION_RETURN" -> Set.of("/mes/pro/feedback/edhr-batch-production-fill");
            case "PQC_HANDOFF","PQC_RETURN" -> Set.of("/mes/pro/feedback/edhr-batch-pqc-fill");
            case "PQC_REVIEW" -> Set.of("/mes/pro/process-pool/pqc-leader");
            case "QA_REVIEW" -> Set.of("/mes/pro/feedback/edhr-nonconformance-review");
            case "QA_DECISION_HANDOFF" -> Set.of("/mes/pro/process-pool/production-leader","/user/profile");
            default -> Set.of("/mes/pro/process-pool/production-leader");
        };
        var allowed=new HashSet<>(Set.of("activeOrderId",sourceKey,"handoffTaskId","roundId","handoffType"));
        if("QA_DECISION_HANDOFF".equals(task.getTaskType())&&"/user/profile".equals(uri.getPath()))allowed.add("tab");
        if(task.getTaskType().endsWith("_RETURN"))allowed.addAll(Set.of("returnTaskId","rejectedReviewId"));
        require(paths.contains(uri.getPath())&&query.keySet().equals(allowed),"交接入口类型或参数不匹配");
        require(Objects.equals(task.getActiveOrderId().toString(),query.get("activeOrderId"))
                &&Objects.equals(task.getSourceId().toString(),query.get(sourceKey))
                &&Objects.equals(task.getId().toString(),query.get("handoffTaskId"))
                &&Objects.equals(task.getRoundId().toString(),query.get("roundId"))
                &&Objects.equals(task.getTaskType(),query.get("handoffType")),"交接入口与原周期、来源或轮次不一致");
        if(task.getTaskType().endsWith("_RETURN")) require(Objects.equals(task.getId().toString(),query.get("returnTaskId"))
                &&Objects.equals(task.getRoundId().toString(),query.get("rejectedReviewId")),"本人更正入口与退回轮次不一致");
        if("QA_DECISION_HANDOFF".equals(task.getTaskType())&&"/user/profile".equals(uri.getPath()))
            require("DONE".equals(task.getStatus())&&task.getReason()!=null&&task.getReason().startsWith("void：")&&"notifyMessage".equals(query.get("tab")),"作废交接只能查看正式已完成结果");
    }
    public static Map<String,String> actionQuery(MesActiveOrderHandoffTaskDO task) {
        URI uri=URI.create(task.getActionUrl());var query=new LinkedHashMap<String,String>();
        require(uri.getRawQuery()!=null,"交接入口缺少正式参数");
        for(String pair:uri.getRawQuery().split("&",-1)) {
            String[] parts=pair.split("=",-1);
            require(parts.length==2&&query.putIfAbsent(parts[0],parts[1])==null,"交接入口参数无效");
        }
        return query;
    }
    public static String label(String type) {
        return switch(type) {
            case "PRODUCTION_HANDOFF" -> "生产接手"; case "PRODUCTION_REVIEW" -> "生产组长复核";
            case "PQC_HANDOFF" -> "PQC检验接手"; case "PQC_REVIEW" -> "PQC组长复核";
            case "PRODUCTION_RETURN" -> "本人生产退回更正"; case "PQC_RETURN" -> "本人PQC退回更正";
            case "QA_REVIEW" -> "QA不合格评审"; case "QA_DECISION_HANDOFF" -> "QA处置结果交接";
            default -> throw failure("未知交接类型");
        };
    }
    static String errorSummary(RuntimeException error) {
        return error.getClass().getSimpleName()+(error instanceof ServiceException x?":code="+x.getCode():"");
    }
}
