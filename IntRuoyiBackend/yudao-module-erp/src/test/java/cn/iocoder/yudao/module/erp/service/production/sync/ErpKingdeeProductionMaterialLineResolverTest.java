package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class ErpKingdeeProductionMaterialLineResolverTest {
    @Test void resolvesActualMaterialListSequenceRatherThanUsingEntryInternalId() {
        var rest = new RestTemplate(); var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties();properties.setBaseUrl("https://erp.example.com");
        server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(content().string(containsString("PRD_PPBOM")))
                .andExpect(content().string(containsString("FEntity_FSeq")))
                .andRespond(withSuccess("[[2234891,7],[2240859,12]]",MediaType.APPLICATION_JSON));
        var result = new ErpKingdeeProductionMaterialLineResolver(rest).resolve(properties,"session",Set.of("2234891","2240859"));
        assertEquals(7,result.get("2234891"));assertEquals(12,result.get("2240859"));server.verify();
    }
    @Test void missingReferencedEntryFailsInsteadOfInventingSequence() {
        var rest = new RestTemplate();var server = MockRestServiceServer.bindTo(rest).build();
        var properties = new ErpKingdeeProperties();properties.setBaseUrl("https://erp.example.com");
        server.expect(requestTo("https://erp.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,()->new ErpKingdeeProductionMaterialLineResolver(rest)
                .resolve(properties,"session",Set.of("2234891")));server.verify();
    }
}
