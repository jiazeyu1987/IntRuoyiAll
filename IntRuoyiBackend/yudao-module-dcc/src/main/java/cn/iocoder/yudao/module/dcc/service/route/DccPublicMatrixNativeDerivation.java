package cn.iocoder.yudao.module.dcc.service.route;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import java.util.*;

/** Explicit algorithm on a raw public LEGACY matrix; no typed route lookup or persistence. */
public final class DccPublicMatrixNativeDerivation {
    public static final String VERSION="DCC_PUBLIC_MATRIX_NATIVE_V1";
    private DccPublicMatrixNativeDerivation(){}
    public record RuleGroup(@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long sourceNodeId,String stageType,String marker,Boolean required,String subjectType,
                            String candidateSourceType,@com.fasterxml.jackson.databind.annotation.JsonSerialize(contentUsing=com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) List<Long> candidateSourceIds,String approveMethod,Integer approveRatio,
                            Boolean requireAllApprovals,boolean applied){public RuleGroup{candidateSourceIds=List.copyOf(candidateSourceIds);}}
    public record Projection(List<DccCategoryApprovalRouteNodeDO> nodes,List<RuleGroup> groups){public Projection{nodes=List.copyOf(nodes);groups=List.copyOf(groups);}}
    public static List<DccCategoryApprovalRouteNodeDO> derive(List<DccCategoryApprovalRouteNodeDO> raw,String action,List<Long> selected){
        return project(raw,action,selected).nodes();
    }
    public static Projection project(List<DccCategoryApprovalRouteNodeDO> raw,String action,List<Long> selected){
        if(!Set.of("NEW","REVISION","OBSOLETE").contains(action==null?"":action)||raw==null||raw.isEmpty())throw invalid("PUBLIC_SOURCE_INVALID");
        if(raw.stream().anyMatch(node->node==null||!Set.of("SIGNOFF","APPROVAL","DOC_CONTROL").contains(node.getStageType()==null?"":node.getStageType())))
            throw invalid("RAW_STAGE_UNSUPPORTED");
        var signoff=raw.stream().filter(n->"SIGNOFF".equals(n.getStageType())).toList();
        var approvals=raw.stream().filter(n->"APPROVAL".equals(n.getStageType())).toList();
        if(signoff.isEmpty()||approvals.isEmpty())throw invalid("APPROVAL_GROUPS_MISSING");
        var groups=new ArrayList<RuleGroup>();
        var required=new TreeSet<Long>();var optional=new TreeSet<Long>();
        for(var rule:signoff){if(!"DEPT".equals(rule.getSubjectType())||!"DEPT".equals(rule.getCandidateSourceType()))throw invalid("SIGNOFF_SUBJECT_UNSUPPORTED");
            validatePolicy(rule);if(!"ALL".equals(rule.getApproveMethod()))throw invalid("SIGNOFF_METHOD_CONFLICT",rule);
            boolean must=requiredParticipation(rule);
            for(Long id:ids(rule)){if(required.contains(id)||optional.contains(id))throw invalid("DUPLICATE_DEPARTMENT_RULE");(must?required:optional).add(id);}}
        List<Long> actual;
        if(selected==null){var defaults=new TreeSet<>(required);defaults.addAll(optional);actual=List.copyOf(defaults);}
        // Public participation markers describe the source; this application's explicit roster is authoritative.
        else {if(selected.stream().anyMatch(id->id==null||id<=0)||selected.stream().distinct().count()!=selected.size())throw invalid("SIGNOFF_SELECTION_INVALID");actual=selected.stream().sorted().toList();}
        if(actual.isEmpty())throw invalid("SIGNOFF_EMPTY");
        for(var rule:signoff)groups.add(group(rule,actual.stream().anyMatch(ids(rule)::contains)));
        var approval=approvalNode(approvals,groups);
        var result=new ArrayList<DccCategoryApprovalRouteNodeDO>();
        result.add(DccCategoryApprovalRouteNodeDO.builder().stageNo(1).stageCode("MATRIX_REVIEW").stageName("会签指派与实际签名").stageOrder(1)
                .candidateSourceType("DEPT").candidateSourceId(actual.get(0)).candidateSourceIds(join(actual)).approveMethod("ALL").approveRatio(100)
                .requireAllApprovals(true).required(true).sort(1).build());
        result.add(approval);
        if(!"OBSOLETE".equals(action)){
            var controls=raw.stream().filter(n->"DOC_CONTROL".equals(n.getStageType())&&Integer.valueOf(4).equals(n.getStageNo())).toList();
            if(controls.size()!=1)throw invalid("DOC_CONTROL_SOURCE_UNSUPPORTED");var control=controls.get(0);validatePolicy(control);
            groups.add(group(control,true));result.add(runtime(control,3,"DOC_CONTROL_REVIEW","文控标准化审核"));
        }
        return new Projection(result,groups);
    }
    private static DccCategoryApprovalRouteNodeDO runtime(DccCategoryApprovalRouteNodeDO raw,int stage,String code,String name){var ids=ids(raw);return DccCategoryApprovalRouteNodeDO.builder().stageNo(stage).stageCode(code).stageName(name).stageOrder(stage)
            .candidateSourceType(raw.getCandidateSourceType()).candidateSourceId(ids.get(0)).candidateSourceIds(join(ids))
            .approveMethod(raw.getApproveMethod()).approveRatio(raw.getApproveRatio()).requireAllApprovals(raw.getRequireAllApprovals())
            .required(raw.getRequired()).sort(stage).build();}
    private static boolean requiredParticipation(DccCategoryApprovalRouteNodeDO rule){
        if("●".equals(rule.getMarker()))return true;if("◎".equals(rule.getMarker()))return false;
        if("▲".equals(rule.getMarker())&&rule.getRequired()!=null)return rule.getRequired();
        throw invalid("PARTICIPATION_FACT_MISSING");
    }
    private static void validatePolicy(DccCategoryApprovalRouteNodeDO rule){
        if(rule.getRequireAllApprovals()==null||rule.getRequired()==null)throw invalid("POLICY_FACT_MISSING");
        if("ANY".equals(rule.getApproveMethod())&& !rule.getRequireAllApprovals()&&rule.getApproveRatio()==null)return;
        if("ALL".equals(rule.getApproveMethod())&& rule.getRequireAllApprovals()&&Integer.valueOf(100).equals(rule.getApproveRatio()))return;
        throw invalid("APPROVAL_POLICY_NOT_EXPRESSIBLE");
    }
    public static void validateRuntime(List<DccCategoryApprovalRouteNodeDO> nodes,String action){
        List<String> stages="OBSOLETE".equals(action)?List.of("MATRIX_REVIEW","MATRIX_APPROVAL"):List.of("MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW");
        if(nodes==null||nodes.size()!=stages.size())throw invalid("RUNTIME_STAGES_INVALID");
        for(int i=0;i<nodes.size();i++){var node=nodes.get(i);if(!Integer.valueOf(i+1).equals(node.getStageNo())
                ||!Integer.valueOf(i+1).equals(node.getStageOrder())||!stages.get(i).equals(node.getStageCode()))throw invalid("RUNTIME_STAGES_INVALID");validatePolicy(node);}
        if(!"DEPT".equals(nodes.get(0).getCandidateSourceType())||!"ALL".equals(nodes.get(0).getApproveMethod()))throw invalid("RUNTIME_SIGNOFF_INVALID");
    }
    private static DccCategoryApprovalRouteNodeDO approvalNode(List<DccCategoryApprovalRouteNodeDO> approvals,List<RuleGroup> groups){
        for(var approval:approvals){
            if(!requiredParticipation(approval))throw invalid("OPTIONAL_APPROVAL_SELECTION_REQUIRED",approval);validatePolicy(approval);
            if(!Set.of("USER","DEPT","DCC_POSITION","POSITION").contains(approval.getSubjectType()==null?"":approval.getSubjectType()))throw invalid("APPROVAL_SUBJECT_UNSUPPORTED");
            if(!Set.of("USER","DEPT","POSITION").contains(approval.getCandidateSourceType()==null?"":approval.getCandidateSourceType()))throw invalid("APPROVAL_SOURCE_UNSUPPORTED");
            groups.add(group(approval,true));
        }
        if(approvals.size()==1)return runtime(approvals.get(0),2,"MATRIX_APPROVAL","批准并确认文件负责人");
        var all=new ArrayList<Long>();var first=approvals.get(0);
        for(var approval:approvals){var candidates=ids(approval);
            // Every group is mandatory. Union is logically exact only when every member must sign,
            // or an ANY group has one identity. Multiple-candidate ANY needs separate group execution.
            if(!"ALL".equals(approval.getApproveMethod())&&candidates.size()!=1)throw invalid("APPROVAL_GROUP_EXECUTION_REQUIRED",approval);
            if(!Objects.equals(first.getCandidateSourceType(),approval.getCandidateSourceType())
                    ||!Objects.equals(first.getSubjectType(),approval.getSubjectType()))throw invalid("APPROVAL_GROUP_EXECUTION_REQUIRED",approval);
            for(Long candidate:candidates){if(all.contains(candidate))throw invalid("APPROVAL_GROUP_IDENTITY_OVERLAP");all.add(candidate);}
        }
        return DccCategoryApprovalRouteNodeDO.builder().stageNo(2).stageCode("MATRIX_APPROVAL").stageName("各必需批准规则全部履行").stageOrder(2)
                .candidateSourceType(first.getCandidateSourceType()).candidateSourceId(all.get(0)).candidateSourceIds(join(all))
                .approveMethod("ALL").approveRatio(100).requireAllApprovals(true).required(true).sort(2).build();
    }
    private static RuleGroup group(DccCategoryApprovalRouteNodeDO raw,boolean applied){return new RuleGroup(raw.getId(),raw.getStageType(),raw.getMarker(),raw.getRequired(),raw.getSubjectType(),raw.getCandidateSourceType(),ids(raw),raw.getApproveMethod(),raw.getApproveRatio(),raw.getRequireAllApprovals(),applied);}
    private static List<Long> ids(DccCategoryApprovalRouteNodeDO raw){String csv=raw.getCandidateSourceIds();if(csv==null||csv.isBlank())throw invalid("RAW_CANDIDATES_MISSING");try{var ids=Arrays.stream(csv.split(",",-1)).map(String::trim).map(Long::valueOf).toList();if(ids.isEmpty()||ids.stream().anyMatch(id->id<=0)||ids.stream().distinct().count()!=ids.size())throw invalid("RAW_CANDIDATES_INVALID");return ids;}catch(NumberFormatException invalid){throw invalid("RAW_CANDIDATES_INVALID");}}
    private static String join(List<Long> ids){return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));}
    private static DccApprovalMatrixCapabilityException invalid(String code){return new DccApprovalMatrixCapabilityException("APPROVAL_MATRIX_"+code,
            switch(code){case "APPROVAL_GROUP_EXECUTION_REQUIRED"->"多个批准规则需要分别完成本组签名，当前模型尚未支持独立批准组";
                case "OPTIONAL_APPROVAL_SELECTION_REQUIRED"->"可选批准规则缺少本次明确选择或正式条件，不会默认参加或跳过";
                case "APPROVAL_POLICY_NOT_EXPRESSIBLE"->"原批准比例或全部参与事实尚无可保真的运行模型";
                case "SIGNOFF_SUBJECT_UNSUPPORTED"->"会签规则必须有正式部门义务，不能把人员或岗位冒充部门";
                case "SIGNOFF_EMPTY"->"请至少选择一个会签部门";
                case "SIGNOFF_SELECTION_INVALID"->"会签部门身份必须有效且不能重复";
                default->"原公共矩阵规则当前不可准确执行，需修正正式配置或补齐模型能力";});}
    private static DccApprovalMatrixCapabilityException invalid(String code,DccCategoryApprovalRouteNodeDO node){var error=invalid(code);return new DccApprovalMatrixCapabilityException(error.getReasonCode(),
            error.getMessage().substring(error.getMessage().indexOf('：')+1)+"；原节点="+node.getId()+"，阶段="+node.getStageType()+"，主体="+node.getSubjectType()
                    +"，标记="+node.getMarker()+"，审批方式="+node.getApproveMethod()+"，比例="+node.getApproveRatio());}
}
