package cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.*;
import java.util.*;
import static cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterSql.*;

/** Static SQL prefilter; every candidate window retains original formal proof visibility semantics. */
@Mapper
public interface MesFeedbackApprovalQueryMapper {
    String FROM = " FROM mes_pro_feedback f LEFT JOIN mes_pro_process_pool_event e ON e.tenant_id=f.tenant_id AND e.deleted=0 AND e.feedback_source_type='MES_PRO_FEEDBACK' AND e.feedback_source_id=f.id AND e.event_type='PRODUCTION_SUBMIT' LEFT JOIN mes_pro_process_pool_submission_review r ON r.id=(SELECT rr.id FROM mes_pro_process_pool_submission_review rr WHERE rr.event_id=e.id AND rr.tenant_id=e.tenant_id AND rr.deleted=0 ORDER BY rr.review_round DESC,rr.id DESC LIMIT 1) ";
    String CREATED = "CASE WHEN e.id IS NOT NULL THEN e.server_submit_time ELSE f.create_time END";
    // Review visibility and effective corrections are decided by the original proof reader, never inferred here.
    String COMPLETED = "CASE WHEN e.id IS NOT NULL THEN r.reviewed_at ELSE f.update_time END";
    @Select("SELECT COUNT(*)"+FROM+"${ew.customSqlSegment}")
    long count(@Param("ew") QueryWrapper<Long> query);
    @Select("SELECT f.id"+FROM+"${ew.customSqlSegment}")
    List<Long> window(@Param("ew") QueryWrapper<Long> query);

    default QueryWrapperX<Long> query(ApprovalTaskQueryContext context) {
        var q=new QueryWrapperX<Long>();
        q.eq("f.tenant_id",TenantContextHolder.getRequiredTenantId()).eq("f.deleted",false);
        Long actor=Objects.requireNonNull(context.getLoginUserId(),"APPROVAL_LOGIN_USER_REQUIRED");
        switch(context.getViewType()) {
            case TODO -> { q.eq("f.status",2); if(!context.isGlobalView()) q.eq("f.approve_user_id",actor); }
            case MY_INITIATED -> { q.in("f.status",List.of(2,3,4)); if(!context.isGlobalView()) q.eq("f.feedback_user_id",actor); }
            case DONE -> {
                if(context.isGlobalView()) q.in("f.status",List.of(0,2,3,4));
                else q.and(w->w.nested(s->s.in("f.status",List.of(0,2,3,4)).eq("f.approve_user_id",actor))
                        .or().apply("EXISTS (SELECT 1 FROM mes_pro_process_pool_submission_review ar WHERE ar.tenant_id=f.tenant_id AND ar.event_id=e.id AND ar.deleted=0 AND ar.leader_type='PRODUCTION' AND ar.leader_user_id={0})",actor));
            }
            default -> throw new IllegalArgumentException("APPROVAL_VIEW_TYPE_UNSUPPORTED: MES_FEEDBACK");
        }
        if(context.getKeyword()!=null && !context.getKeyword().isEmpty()) q.like("f.code",context.getKeyword());
        var facts=new HashMap<String,String>();
        facts.put("moduleLabel","'生产报工审批'");facts.put("sourceLabel","'生产报工复核任务'");
        facts.put("applicant",userLabel("f.feedback_user_id","f.tenant_id"));
        facts.put("capabilities", "'AUDIT TIMELINE'");
        var params=new PageParam();params.setListFilters(context.getListFilters());
        var all=fields(facts,false);
        var whitelist=new HashMap<String,ListFilterQueryUtils.Field>();
        for(String key:ApprovalListFilterValues.STATIC_KEYS)whitelist.put(key,all.get(key));
        ListFilterQueryUtils.apply(params,q,whitelist);return q;
    }
}
