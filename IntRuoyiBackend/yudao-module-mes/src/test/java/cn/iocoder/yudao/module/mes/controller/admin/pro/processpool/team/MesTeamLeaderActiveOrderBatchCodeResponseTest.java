package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MesTeamLeaderActiveOrderBatchCodeResponseTest {
    @Test
    void listResponseSerializesFormalBatchCodeIndependentlyOfOrderCode() throws Exception {
        var row = new MesTeamLeaderActiveOrderRow().setId(627L)
                .setWorkOrderCode("WO-627").setBatchCode("PRODUCTION-BATCH-627");
        var response = map(row);
        var json = new ObjectMapper().valueToTree(response);
        assertEquals("PRODUCTION-BATCH-627", json.get("batchCode").asText());
        assertEquals("WO-627", json.get("workOrderCode").asText());
    }

    @Test
    void missingFormalBatchCodeIsNotReplacedWithOrderCode() throws Exception {
        var response = map(new MesTeamLeaderActiveOrderRow().setId(628L).setWorkOrderCode("WO-628"));
        assertNull(response.getBatchCode());
        assertEquals("WO-628", response.getWorkOrderCode());
    }

    private MesTeamLeaderActiveOrderRespVO map(MesTeamLeaderActiveOrderRow row) throws Exception {
        var method = MesProcessPoolTeamLeaderController.class.getDeclaredMethod(
                "toActiveOrderRespVO", MesTeamLeaderActiveOrderRow.class);
        method.setAccessible(true);
        return (MesTeamLeaderActiveOrderRespVO) method.invoke(null, row);
    }
}
