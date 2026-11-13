package cn.iocoder.yudao.module.showroom.dal.mysql.workflow;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.*;
import java.util.*;
import static cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterSql.*;

/** Filters/counts the authorized approval source in SQL before loading its bounded window. */
@Mapper
public interface ShowroomApprovalQueryMapper {
    @Select("SELECT COUNT(*) FROM showroom_change_request c ${ew.customSqlSegment}")
    long count(@Param("ew") QueryWrapper<Long> query);
    @Select("SELECT c.id FROM showroom_change_request c ${ew.customSqlSegment}")
    List<Long> window(@Param("ew") QueryWrapper<Long> query);

    default QueryWrapperX<Long> query(ApprovalTaskQueryContext context) {
        var q = new QueryWrapperX<Long>();
        q.eq("c.tenant_id",TenantContextHolder.getRequiredTenantId()).eq("c.deleted",false);
        Long actor=Objects.requireNonNull(context.getLoginUserId(),"APPROVAL_LOGIN_USER_REQUIRED");
        switch(context.getViewType()) {
            case TODO -> {
                if(context.isGlobalView()) q.in("c.status",List.of("PENDING_SUPERVISOR_REVIEW","PENDING_GAOXIN_APPROVAL"));
                else q.and(w->w.nested(s->s.eq("c.status","PENDING_SUPERVISOR_REVIEW").eq("c.supervisor_user_id",actor))
                        .or().nested(s->s.eq("c.status","PENDING_GAOXIN_APPROVAL").eq("c.gaoxin_user_id",actor)));
            }
            case MY_INITIATED -> { if(!context.isGlobalView()) q.eq("c.submitted_by",actor); }
            case DONE -> {
                if(context.isGlobalView()) q.and(w->w.isNotNull("c.supervisor_action_at").or().isNotNull("c.gaoxin_action_at"));
                else q.and(w->w.nested(s->s.eq("c.supervisor_user_id",actor).isNotNull("c.supervisor_action_at"))
                        .or().nested(s->s.eq("c.gaoxin_user_id",actor).isNotNull("c.gaoxin_action_at")));
            }
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: SHOWROOM");
        }
        if(context.getKeyword()!=null && !context.getKeyword().isBlank())
            q.and(w->w.apply("LOCATE(CAST({0} AS BINARY),CAST(c.id AS BINARY))>0",context.getKeyword().trim())
                    .or().apply("LOCATE(CAST({0} AS BINARY),CAST(c.status AS BINARY))>0",context.getKeyword().trim())
                    .or().apply("LOCATE(CAST({0} AS BINARY),CAST(c.target_type AS BINARY))>0",context.getKeyword().trim()));
        var fields=new HashMap<String,String>();
        String assignee="CASE c.status WHEN 'PENDING_SUPERVISOR_REVIEW' THEN c.supervisor_user_id WHEN 'PENDING_GAOXIN_APPROVAL' THEN c.gaoxin_user_id ELSE NULL END";
        String node="CASE c.status WHEN 'PENDING_SUPERVISOR_REVIEW' THEN '主管审核' WHEN 'PENDING_GAOXIN_APPROVAL' THEN '企宣审批' WHEN 'PUBLISHED' THEN '已发布' WHEN 'REJECTED' THEN '已驳回' ELSE CASE WHEN c.status REGEXP '[A-Za-z]' THEN '未配置中文节点' ELSE COALESCE(c.status,'--') END END";
        String remark="CASE WHEN c.status='REJECTED' THEN (SELECT NULLIF(TRIM(s.comment),'') FROM showroom_change_request_signature s WHERE s.tenant_id=c.tenant_id AND s.deleted=0 AND s.change_request_id=c.id AND UPPER(s.action_type)='REJECT' AND NULLIF(TRIM(s.comment),'') IS NOT NULL ORDER BY s.id ASC LIMIT 1) ELSE NULL END";
        fields.put("moduleLabel","'展厅审批'"); fields.put("sourceLabel","'未配置中文任务来源'");
        fields.put("title","CONCAT('展厅内容变更 #',c.id)");
        fields.put("identifier","CONCAT('业务编号：',c.target_type,'-',c.target_id)");
        fields.put("businessStatus","c.status");fields.put("nodeName",node);
        fields.put("nodeStatus","CASE c.status WHEN 'PENDING_SUPERVISOR_REVIEW' THEN '待直属主管审核' WHEN 'REJECTED' THEN '已驳回' WHEN 'APPROVED' THEN '已通过' ELSE '未配置中文状态' END");
        fields.put("applicant",userLabel("c.submitted_by","c.tenant_id"));fields.put("reviewer",userLabel(assignee,"c.tenant_id"));
        fields.put("result","CASE c.status WHEN 'REJECTED' THEN 'REJECT' WHEN 'PUBLISHED' THEN 'APPROVE' WHEN 'APPROVED' THEN 'APPROVE' ELSE NULL END");
        fields.put("remark",remark);fields.put("created","c.submitted_at");fields.put("initiated","c.submitted_at");
        fields.put("capabilities", "'AUDIT EVIDENCE_LEDGER NOTIFICATION SIGNATURE_AUTHORIZATION TIMELINE'");
        var params=new PageParam();params.setListFilters(context.getListFilters());
        ListFilterQueryUtils.apply(params,q,fields(fields,false));return q;
    }
}
