package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** Explicit public LEGACY source. Never queries NEW first or inserts a derived route. */
@Service
public class DccApprovalMatrixSourceService {
    @Resource private DccFileCategoryMapper categories;
    @Resource private DccCategoryApprovalRouteMapper routes;
    @Resource private DccCategoryApprovalRouteNodeMapper nodes;
    @Resource private DccWorkflowDatePolicy datePolicy;
    public record Source(DccCategoryApprovalRouteDO route,List<DccCategoryApprovalRouteNodeDO> rawNodes,
                         DccApprovalMatrixBaselineVO baseline,String canonical) { public Source{rawNodes=List.copyOf(rawNodes);} }

    public Source read(Long categoryId){return readSource(categoryId,datePolicy.now(),false);}
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Source readForUpdate(Long categoryId){return readSource(categoryId,datePolicy.now(),true);}
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Source readExactForUpdate(Long categoryId,Long routeId){
        return readSource(categoryId,datePolicy.now(),true,routeId);
    }

    private Source readSource(Long categoryId,LocalDateTime selectionTime,boolean lock){
        return readSource(categoryId,selectionTime,lock,null);
    }
    private Source readSource(Long categoryId,LocalDateTime selectionTime,boolean lock,Long exactId){
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var category=lock?categories.selectMatrixCategoryForUpdate(tenant,categoryId):categories.selectById(categoryId);
        if(category==null || !Boolean.TRUE.equals(category.getActive()) || category.getFileTypeTaxonomyId()==null)
            throw new IllegalStateException("APPROVAL_MATRIX_CATEGORY_INVALID");
        var query=new QueryWrapper<DccCategoryApprovalRouteDO>().eq("tenant_id",tenant).eq("category_id",categoryId)
                .eq("action_type","LEGACY").eq("deleted",false);
        if(exactId==null)query.eq("active",true).and(q->q.isNull("effective_time").or().le("effective_time",selectionTime)).orderByDesc("version_no","id");
        else query.eq("id",exactId);
        query.last(lock?"LIMIT 1 FOR UPDATE":"LIMIT 1");
        var roots=routes.selectList(query);if(roots==null || roots.size()!=1)throw new IllegalStateException("APPROVAL_MATRIX_PUBLIC_SOURCE_MISSING");
        var route=roots.get(0);
        var nodeQuery=new QueryWrapper<DccCategoryApprovalRouteNodeDO>().eq("tenant_id",tenant).eq("route_id",route.getId()).eq("deleted",false);
        if(lock)nodeQuery.last("FOR UPDATE");
        var rawNodes=nodes.selectList(nodeQuery);if(rawNodes==null || rawNodes.isEmpty())throw new IllegalStateException("APPROVAL_MATRIX_RAW_NODES_MISSING");
        var raw=new TreeMap<String,Object>();raw.put("formatVersion",DccApprovalMatrixCanonicalizer.FORMAT);
        raw.put("tenantId",tenant);raw.put("categoryId",categoryId);raw.put("fileTypeTaxonomyId",category.getFileTypeTaxonomyId());
        raw.put("routeId",route.getId());raw.put("routeVersionNo",route.getVersionNo());raw.put("actionType",route.getActionType());
        raw.put("effectiveTime",route.getEffectiveTime()==null?null:route.getEffectiveTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                +"."+String.format(java.util.Locale.ROOT,"%09d",route.getEffectiveTime().getNano()));raw.put("routeRemark",route.getRemark());
        raw.put("nodes",rawNodes.stream().map(DccApprovalMatrixSourceService::rawNode).toList());
        String canonical=DccApprovalMatrixCanonicalizer.canonicalize(raw);
        var baseline=JsonUtils.parseObject(canonical,DccApprovalMatrixBaselineVO.class);baseline.setMatrixDigest(DccApprovalMatrixCanonicalizer.digest(raw));
        return new Source(route,rawNodes,baseline,canonical);
    }
    public static DccApprovalMatrixBaselineVO readCanonical(String json){
        if(json==null)return null;var raw=JsonUtils.parseObject(json,Map.class);
        if(raw==null)throw new IllegalStateException("APPROVAL_MATRIX_DRAFT_BASELINE_INVALID");
        String canonical=DccApprovalMatrixCanonicalizer.canonicalize(raw);
        if(!json.equals(canonical))throw new IllegalStateException("APPROVAL_MATRIX_DRAFT_BASELINE_NOT_CANONICAL");
        var baseline=JsonUtils.parseObject(canonical,DccApprovalMatrixBaselineVO.class);baseline.setMatrixDigest(DccApprovalMatrixCanonicalizer.digest(raw));return baseline;
    }
    private static Map<String,Object> rawNode(DccCategoryApprovalRouteNodeDO node){
        var raw=new TreeMap<String,Object>();raw.put("id",node.getId());raw.put("stageNo",node.getStageNo());raw.put("stageCode",node.getStageCode());
        raw.put("stageName",node.getStageName());raw.put("stageOrder",node.getStageOrder());raw.put("sort",node.getSort());
        raw.put("candidateSourceType",node.getCandidateSourceType());raw.put("candidateSourceId",node.getCandidateSourceId());raw.put("candidateSourceIds",node.getCandidateSourceIds());
        raw.put("approveMethod",node.getApproveMethod());raw.put("approveRatio",node.getApproveRatio());raw.put("requireAllApprovals",node.getRequireAllApprovals());raw.put("required",node.getRequired());
        raw.put("stageType",node.getStageType());raw.put("subjectLabel",node.getSubjectLabel());raw.put("marker",node.getMarker());raw.put("subjectType",node.getSubjectType());
        raw.put("subjectId",node.getSubjectId());raw.put("subjectName",node.getSubjectName());raw.put("subjectDepartmentPath",node.getSubjectDepartmentPath());raw.put("ruleRemark",node.getRuleRemark());return raw;
    }
}
