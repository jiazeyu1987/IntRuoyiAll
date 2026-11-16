package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class NativeApprovalListFilterSqlTest {
    private static ApprovalTaskQueryContext context() {
        return ApprovalTaskQueryContext.of(100L,ApprovalTaskViewType.TODO,ApprovalModuleCode.BPM,null,1,10);
    }
    @Test
    void allFourViewsMapEveryVisibleFilterColumn() {
        for(var fields:List.of(NativeApprovalListFilterSql.taskFields(context(),false,List.of(),true).fields(),
                NativeApprovalListFilterSql.taskFields(context(),true,List.of(),true).fields(),
                NativeApprovalListFilterSql.initiatedFields(context(),List.of()).fields(),
                NativeApprovalListFilterSql.copiedFields(context(),List.of()).fields())) {
            assertEquals(Set.of("source","businessSummary","applicant","node","reviewer","approvalResult","approvalRemark","time","capabilities"),fields.keySet());
            assertFalse(fields.get("businessSummary").expression().contains("GROUP_CONCAT"));
        }
    }
    @Test
    void currentAndHistoricVariablesKeepTheirRealStorageAndTenantBoundary() {
        String todo=NativeApprovalListFilterSql.variablesJoin("t.PROC_INST_ID_",true)+NativeApprovalListFilterSql.taskFields(context(),false,List.of(),false).fields().get("businessSummary").expression();
        String done=NativeApprovalListFilterSql.variablesJoin("t.PROC_INST_ID_",false)+NativeApprovalListFilterSql.taskFields(context(),true,List.of(),true).fields().get("businessSummary").expression();
        assertTrue(todo.contains("ACT_RU_VARIABLE"));assertFalse(todo.contains("FROM ACT_HI_VARINST"));
        assertTrue(done.contains("ACT_HI_VARINST"));
        assertFalse(todo.contains("v.TENANT_ID_"));assertFalse(done.contains("v.TENANT_ID_"));
        assertTrue(todo.contains("nvp.TENANT_ID_=t.TENANT_ID_"));
        assertTrue(todo.contains("mes_pro_edhr_work_task"));
        assertTrue(todo.contains("当前用户缺少领域审核权限"));
    }
    @Test
    void dynamicTitlesAndUserInputUseCompleteSqlAndBoundValues() {
        var fields=NativeApprovalListFilterSql.initiatedFields(context(),List.of()).fields();
        String summary=NativeApprovalListFilterSql.variablesJoin("p.PROC_INST_ID_",false)+fields.get("businessSummary").expression();
        assertTrue(summary.contains("WITH RECURSIVE"));assertFalse(summary.contains("JSON_TABLE"));assertTrue(summary.contains("JSON_ARRAYAGG"));
        assertTrue(summary.contains("UNBOUNDED FOLLOWING"));
        PageParam request=new PageParam();
        request.setListFilters("[{\"key\":\"businessSummary\",\"operator\":\"contains\",\"value\":\"审核%\"}]");
        var query=ListFilterQueryUtils.wrapper(request,fields);
        assertFalse(query.getSqlSegment().contains("审核%"));
        assertTrue(query.getParamNameValuePairs().containsValue("%审核!%%"));
    }
    @Test
    void completeNativePredicatesStayWithinTheProductionParserContract() throws Exception {
        for(boolean done:List.of(false,true)) {
            var projection=NativeApprovalListFilterSql.taskFields(context(),done,List.of(),false);
            String sql="SELECT t.ID_ FROM "+(done?"ACT_HI_TASKINST":"ACT_RU_TASK")+" t"
                    +" LEFT JOIN ACT_HI_PROCINST p ON p.PROC_INST_ID_=t.PROC_INST_ID_"
                    +" LEFT JOIN ACT_RE_PROCDEF d ON d.ID_=t.PROC_DEF_ID_"
                    +" LEFT JOIN bpm_approval_process_ownership o ON o.process_instance_id=t.PROC_INST_ID_"
                    +NativeApprovalListFilterSql.variablesJoin("t.PROC_INST_ID_",!done)
                    +projection.joins()+" WHERE ("+projection.fields().get("businessSummary").expression()+") LIKE '%review%'";
            assertNotNull(net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql));
        }
    }

}
