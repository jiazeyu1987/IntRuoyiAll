package cn.iocoder.yudao.module.bpm.approval.service;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalFlowableTaskQueryContractTest {
    @Test void countAndWindowShareEligibilityAndProjectionForBothCandidateModes() throws Exception {
        var configuration=new Configuration();
        String resource="mapper/approval/ApprovalFlowableTaskQueryMapper.xml";
        try(var input=Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resource))) {
            new XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
        }
        for(boolean done:List.of(false,true)) for(boolean global:List.of(false,true))
            for(boolean useGroups:List.of(false,true)) for(String projection:List.of("BPM","DCC_TASK")) {
                var p=new HashMap<String,Object>();p.put("done",done);p.put("global",global);p.put("projection",projection);
                p.put("candidateGroups",useGroups?List.of("quality"):null);p.put("keyword","%literal!_%");
                String count=sql(configuration,"count",p),window=sql(configuration,"window",p);
                assertEquals(count.substring(count.indexOf(" FROM ")),window.substring(window.indexOf(" FROM "),window.indexOf(" ORDER BY ")));
                assertTrue(window.contains("CAST(t.ID_ AS BINARY) ASC LIMIT ? OFFSET ?"));
                assertTrue(count.contains("searched.tenant_id=?"));
                assertTrue(count.contains("CAST(searched.id AS CHAR)=o.authority_id"));
                assertTrue(count.contains("LOWER(searched.file_number) LIKE ?"));
                assertEquals(!done && projection.equals("DCC_TASK"),
                        count.contains("completed.status IN ('ACTIVE','SUPERSEDED','OBSOLETE')"));
                if(done) assertTrue(count.contains("t.TASK_DEF_KEY_ != ?"));
                if(!done && !global) {
                    assertTrue(count.contains("bpm_approval_selected_candidate"));
                    assertEquals(useGroups,count.contains("il.TYPE_='candidate'"));
                    assertEquals(!useGroups,count.contains("ACT_ID_MEMBERSHIP"));
                }
            }
    }
    @Test void keywordIsLiteralAndExactSourceRereadCannotLoseOrDuplicateRows() {
        assertEquals("%a!_!%!!%",ApprovalFlowableTaskQueryService.literalKeyword(" A_%! "));
        assertEquals(List.of("b","a"),ApprovalFlowableTaskQueryService.exactOrder(List.of("b","a"),List.of("a","b"),s->s));
        assertThrows(IllegalStateException.class,()->ApprovalFlowableTaskQueryService.exactOrder(List.of("a","b"),List.of("a"),s->s));
        assertThrows(IllegalStateException.class,()->ApprovalFlowableTaskQueryService.exactOrder(List.of("a","b"),List.of("a","a"),s->s));
    }
    private static String sql(Configuration c,String id,Map<String,Object> p) {
        return c.getMappedStatement("cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalFlowableTaskQueryMapper."+id)
                .getBoundSql(p).getSql().replaceAll("\\s+"," ").trim();
    }
}
