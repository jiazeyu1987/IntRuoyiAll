package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeProductionMaterialListClientImplTest {

    @Test
    void fetchProductionMaterialListsModifiedBetween_queriesPrdPpbomAndParsesRows() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionMaterialListClientImpl client =
                new ErpKingdeeProductionMaterialListClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties();

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("PRD_PPBOM")))
                .andExpect(content().string(containsString("FMOEntryID")))
                .andExpect(content().string(containsString("FBillNo")))
                .andExpect(content().string(containsString("FMoBillNo")))
                .andExpect(content().string(containsString("FDocumentStatus")))
                .andExpect(content().string(containsString("FMaterialID.FNumber")))
                .andExpect(content().string(containsString("FMaterialID2.FNumber")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FProductId"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FMoStatus"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FPlanBeginDate"))))
                .andExpect(content().string(containsString("FModifyDate+%3E%3D+%272026-06-12+08%3A00%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+%3C+%272026-06-12+09%3A00%3A00%27")))
                .andRespond(withSuccess("""
                        [[1001,"PPBOM0030888","AW.106.03.08.10","CODXMO20260",1,"C","A001.02.014.300","造影导管软端","4F","标准件",3,1000,"支",1,"直接领料","2026-06-12T08:30:00"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionMaterialList> rows = client.fetchProductionMaterialListsModifiedBetween(properties,
                LocalDateTime.of(2026, 6, 12, 8, 0),
                LocalDateTime.of(2026, 6, 12, 9, 0));

        assertEquals(1, rows.size());
        ErpKingdeeProductionMaterialList row = rows.get(0);
        assertEquals("PRD_PPBOM", row.getFormId());
        assertEquals("1001", row.getEntryId());
        assertEquals("PPBOM0030888", row.getBillNo());
        assertEquals("AW.106.03.08.10", row.getProductCode());
        assertEquals("CODXMO20260", row.getProductionOrderNo());
        assertEquals("C", row.getProductionOrderStatus());
        assertEquals("A001.02.014.300", row.getChildMaterialCode());
        assertEquals(new BigDecimal("1"), row.getRequiredQuantity());
        assertEquals(null, row.getDemandTime());
        assertEquals(LocalDateTime.of(2026, 6, 12, 8, 30), row.getSourceModifyTime());
        server.verify();
    }

    @Test
    void fetchProductionMaterialListsByProductionOrderNos_queriesByOrderNoWithPaging() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionMaterialListClientImpl client =
                new ErpKingdeeProductionMaterialListClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties();
        properties.getProductionOrder().setQueryLimit(1);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FMoBillNo+in+%28%27881MO090863%27%29")))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andExpect(content().string(containsString("%22Limit%22%3A1")))
                .andRespond(withSuccess("""
                        [[1001,"PPBOM0030818","YXN.037.011.1002","881MO090863",1,"C","A001.02.014.300","造影导管软端","4F","标准件",3,1000,"支",1,"直接领料","2024-02-12 08:30:00"]]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andExpect(content().string(containsString("%22Limit%22%3A1")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionMaterialList> rows = client.fetchProductionMaterialListsByProductionOrderNos(
                properties, Set.of("881MO090863"));

        assertEquals(1, rows.size());
        assertEquals("881MO090863", rows.get(0).getProductionOrderNo());
        assertEquals("PPBOM0030818", rows.get(0).getBillNo());
        server.verify();
    }

    private static ErpKingdeeProperties buildProperties() {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-pass");
        properties.setLcid(2052);
        properties.getProductionOrder().setQueryLimit(500);
        return properties;
    }

}
