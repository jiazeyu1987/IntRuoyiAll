package cn.iocoder.yudao.module.mes.approval;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import java.util.HashMap;
import static cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterSql.*;
/** Database predicates for static display facts; outcome facts use the authoritative realtime reader. */
public final class MesHandoffApprovalListFilters {
    private MesHandoffApprovalListFilters() { }
    public static void apply(String filters,boolean done,AbstractWrapper<?,?,?> query) {
        var facts=new HashMap<String,String>();
        facts.put("moduleLabel","'电子批记录审批'");facts.put("sourceLabel","'未配置中文任务来源'");
        facts.put("applicant",done?"'姓名未记录'":userLabel("mes_active_order_handoff_task.initiated_by","mes_active_order_handoff_task.tenant_id"));
        facts.put("capabilities","'AUDIT EVIDENCE_LEDGER TIMELINE'");
        var all=fields(facts,done);
        var whitelist=new HashMap<String,ListFilterQueryUtils.Field>();
        for(String key:cn.iocoder.yudao.module.bpm.approval.service.ApprovalListFilterValues.STATIC_KEYS)whitelist.put(key,all.get(key));
        var params=new PageParam();params.setListFilters(filters);ListFilterQueryUtils.apply(params,query,whitelist);
    }
}
