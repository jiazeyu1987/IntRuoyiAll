package cn.iocoder.yudao.module.mes.approval;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import java.util.HashMap;
import static cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterSql.*;
/** Database predicates for static display facts; outcome facts use the authoritative realtime reader. */
public final class MesEdhrWorkTaskApprovalListFilters {
    private MesEdhrWorkTaskApprovalListFilters() { }
    public static void apply(String filters,boolean done,AbstractWrapper<?,?,?> query) {
        var facts=new HashMap<String,String>();
        facts.put("moduleLabel","'电子批记录审批'");facts.put("sourceLabel","'电子批记录工作任务'");
        facts.put("applicant",done?"'姓名未记录'":userLabel("mes_pro_edhr_work_task.source_user_id","mes_pro_edhr_work_task.tenant_id"));
        facts.put("capabilities","'AUDIT EVIDENCE_LEDGER SIGNATURE_AUTHORIZATION TIMELINE'");
        var all=fields(facts,done);
        var whitelist=new HashMap<String,ListFilterQueryUtils.Field>();
        for(String key:cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterValues.STATIC_KEYS)whitelist.put(key,all.get(key));
        var params=new PageParam();params.setListFilters(filters);ListFilterQueryUtils.apply(params,query,whitelist);
    }
}
