package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeMaterialClientImplTest {

    @Test
    void fetchMaterials_filtersApprovedRowsAndParsesFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeMaterialClientImpl client = new ErpKingdeeMaterialClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 500);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("username=kingdee-user")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("BD_MATERIAL")))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andExpect(content().string(containsString("FNumber+%3C%3E+%27%27")))
                .andExpect(content().string(containsString("FDocumentStatus+%3D+%27C%27")))
                .andExpect(content().string(containsString("%22Limit%22%3A500")))
                .andRespond(withSuccess("""
                        [["MAT-001","Product A","12ml","CHLB05_SYS","Finished Goods","PCS","A","C"]]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andExpect(content().string(containsString("%22Limit%22%3A500")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeMaterial> materials = client.fetchMaterials(properties);

        assertEquals(1, materials.size());
        assertEquals("MAT-001", materials.get(0).getMaterialNumber());
        assertEquals("Product A", materials.get(0).getMaterialName());
        assertEquals("CHLB05_SYS", materials.get(0).getCategoryCode());
        assertEquals("PCS", materials.get(0).getUnitName());
        server.verify();
    }

    @Test
    void fetchMaterials_continuesPagingUntilEmptyPage() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeMaterialClientImpl client = new ErpKingdeeMaterialClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 1);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("%22StartRow%22%3A0")))
                .andExpect(content().string(containsString("%22Limit%22%3A1")))
                .andRespond(withSuccess("""
                        [["MAT-001","Product A","12ml","CHLB05_SYS","Finished Goods","PCS","A","C"]]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andExpect(content().string(containsString("%22Limit%22%3A1")))
                .andRespond(withSuccess("""
                        [["MAT-002","Product B","24ml","CHLB05_SYS","Finished Goods","PCS","A","C"]]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("%22StartRow%22%3A2")))
                .andExpect(content().string(containsString("%22Limit%22%3A1")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeMaterial> materials = client.fetchMaterials(properties);

        assertEquals(2, materials.size());
        assertEquals("MAT-001", materials.get(0).getMaterialNumber());
        assertEquals("MAT-002", materials.get(1).getMaterialNumber());
        server.verify();
    }

    @Test
    void fetchMaterials_normalizesUtf8MojibakeChineseText() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeMaterialClientImpl client = new ErpKingdeeMaterialClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 500);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        String responseBody = JsonUtils.toJsonString(List.of(List.of(
                "A002.11.001.000012",
                toUtf8Mojibake("合格证（内贸INT）"),
                toUtf8Mojibake("三类产品（包含 介入手术器械不带配件）（90*65mm）规格型号、数量根据订单变量 黑色 70克书写纸"),
                "CHLB05_SYS",
                "Finished Goods",
                "PCS",
                "A",
                "C"
        )));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeMaterial> materials = client.fetchMaterials(properties);

        assertEquals(1, materials.size());
        assertEquals("合格证（内贸INT）", materials.get(0).getMaterialName());
        assertEquals("三类产品（包含 介入手术器械不带配件）（90*65mm）规格型号、数量根据订单变量 黑色 70克书写纸",
                materials.get(0).getSpecification());
        server.verify();
    }

    @Test
    void fetchMaterialsModifiedBetween_usesModifyTimeWindowAndParsesSourceModifyTime() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeMaterialClientImpl client = new ErpKingdeeMaterialClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 500);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FModifyDate")))
                .andExpect(content().string(containsString("FModifyDate+%3E%3D+%272026-01-01+08%3A30%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+%3C+%272026-01-02+08%3A30%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+ASC")))
                .andRespond(withSuccess("""
                        [["MAT-001","Product A","12ml","CHLB05_SYS","Finished Goods","PCS","A","C","2026-01-01T09:00:00.123"]]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("%22StartRow%22%3A1")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ErpKingdeeMaterial> materials = client.fetchMaterialsModifiedBetween(properties,
                LocalDateTime.of(2026, 1, 1, 8, 30, 0),
                LocalDateTime.of(2026, 1, 2, 8, 30, 0));

        assertEquals(1, materials.size());
        assertEquals("MAT-001", materials.get(0).getMaterialNumber());
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0, 0, 123_000_000),
                materials.get(0).getSourceModifyTime());
        server.verify();
    }

    @Test
    void fetchMaterialsModifiedBetween_exposesKingdeeArrayWrappedErrorResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeMaterialClientImpl client = new ErpKingdeeMaterialClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 500);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("""
                        [[{"Result":{"ResponseStatus":{"IsSuccess":false,"Errors":[{"Message":"会话信息已丢失，请重新登录"}]}}}]]
                        """, MediaType.APPLICATION_JSON));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> client.fetchMaterialsModifiedBetween(
                properties, LocalDateTime.of(2026, 1, 1, 8, 30),
                LocalDateTime.of(2026, 1, 1, 8, 31)));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("会话信息已丢失"));
        server.verify();
    }

    @Test
    void fetchMaterialsByNumbers_queriesOnlyTargetMaterial() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeMaterialClientImpl client = new ErpKingdeeMaterialClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com", 500);

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("BD_MATERIAL")))
                .andExpect(content().string(containsString("FNumber+in+%28%27A002.09.002.230396%27%29")))
                .andExpect(content().string(containsString("%22Limit%22%3A1")))
                .andRespond(withSuccess("""
                        [["A002.09.002.230396","外标签 (INT)","造影导管4F 通用","CHLB10_SYS","包装物","张","A","C","2025-11-14T16:05:20.16"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeMaterial> materials = client.fetchMaterialsByNumbers(properties,
                List.of("A002.09.002.230396"));

        assertEquals(1, materials.size());
        assertEquals("A002.09.002.230396", materials.get(0).getMaterialNumber());
        assertEquals("外标签 (INT)", materials.get(0).getMaterialName());
        assertEquals("包装物", materials.get(0).getCategoryName());
        server.verify();
    }

    private static ErpKingdeeProperties buildProperties(String baseUrl, int queryLimit) {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);
        properties.getProduct().setQueryLimit(queryLimit);
        return properties;
    }

    private static String toUtf8Mojibake(String text) {
        return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

}
