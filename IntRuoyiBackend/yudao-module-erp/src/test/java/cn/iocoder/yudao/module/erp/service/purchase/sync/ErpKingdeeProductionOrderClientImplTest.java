package cn.iocoder.yudao.module.erp.service.purchase.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ErpKingdeeProductionOrderClientImplTest {

    @Test
    void fetchProductionOrders_usesConfiguredEndpointAndParsesRows() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("username=kingdee-user")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));

        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("PRD_MO")))
                .andExpect(content().string(containsString("%22Limit%22%3A500")))
                .andExpect(content().string(containsString("FWorkShopID.FName")))
                .andExpect(content().string(containsString("FBomId.FNumber")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("F_PAEZ_Remark1"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FBizStatus"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("F_PAEZ_TuHao"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("F_PAEZ_PaiChanStatus"))))
                .andExpect(content().string(containsString("FBillNo+%3C%3E+%27%27")))
                .andExpect(content().string(containsString("FMaterialId.FNumber+%3C%3E+%27%27")))
                .andExpect(content().string(containsString("FDocumentStatus+%3C%3E+%27Z%27")))
                .andExpect(content().string(containsString("FDate+%3E%3D+%272025-06-10%27")))
                .andExpect(content().string(containsString("FDate+%3C%3D+%272026-06-10%27")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FStatus+%3C%3E+%275%27"))))
                .andRespond(withSuccess("""
                        [[310119,"881MO091049","A","2026-03-25T00:00:00","A001.01.053.001","ABS","TR558A",1.0,"2026-03-25T00:00:00","2026-03-25T00:00:00"," ","1","kg","千克","BATCH-001","组装车间","BOM-2026-01"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionOrder> orders = client.fetchProductionOrdersByBillDateRange(
                properties, LocalDate.of(2025, 6, 10), LocalDate.of(2026, 6, 10));

        assertEquals(1, orders.size());
        assertEquals("310119", orders.get(0).getFid());
        assertEquals("881MO091049", orders.get(0).getBillNo());
        assertEquals("A001.01.053.001", orders.get(0).getMaterialNumber());
        assertEquals("kg", orders.get(0).getUnitCode());
        assertEquals("千克", orders.get(0).getUnitName());
        assertEquals("BATCH-001", orders.get(0).getBatchNumber());
        assertEquals("组装车间", orders.get(0).getWorkshopName());
        assertEquals("BOM-2026-01", orders.get(0).getBomVersion());
        assertEquals("", orders.get(0).getPickMode());
        assertEquals("", orders.get(0).getAuxiliaryCode());
        assertEquals("", orders.get(0).getBusinessStatus());
        assertEquals("", orders.get(0).getDrawingNumber());
        assertEquals("", orders.get(0).getScheduleStatus());
        server.verify();
    }

    @Test
    void fetchProductionOrdersByBillDateRange_usesBillDateWindowAndParsesBillDate() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        expectLogin(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FDate+%3E%3D+%272025-06-10%27")))
                .andExpect(content().string(containsString("FDate+%3C%3D+%272026-06-10%27")))
                .andRespond(withSuccess("""
                        [[310121,"123123123","C","2026-03-19T00:00:00","A001.01.053.001","ABS","TR558A-MNP-1",12.0,"","","","2","Pcs","Pcs","","","","","","","",""]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionOrder> orders = client.fetchProductionOrdersByBillDateRange(
                properties, LocalDate.of(2025, 6, 10), LocalDate.of(2026, 6, 10));

        assertEquals(1, orders.size());
        assertEquals("123123123", orders.get(0).getBillNo());
        assertEquals(LocalDateTime.of(2026, 3, 19, 0, 0), orders.get(0).getBillDate());
        assertEquals("2", orders.get(0).getStatus());
        server.verify();
    }

    @Test
    void fetchProductionOrdersByBillDateRange_doesNotFilterFinishedStatusVisibleInErpList() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        expectLogin(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FDate+%3E%3D+%272025-06-10%27")))
                .andExpect(content().string(containsString("FDate+%3C%3D+%272026-06-10%27")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FStatus+%3C%3E+%275%27"))))
                .andRespond(withSuccess("""
                        [[310121,"881MO101245","C","2026-08-20T00:00:00","A011.002.1085","1.5-2.7F管胚","0.80*1.18*1000mm",100.0,"","","","5","支","支","","中试车间1","BOM-2026-08"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionOrder> orders = client.fetchProductionOrdersByBillDateRange(
                properties, LocalDate.of(2025, 6, 10), LocalDate.of(2026, 6, 10));

        assertEquals(1, orders.size());
        assertEquals("881MO101245", orders.get(0).getBillNo());
        assertEquals("5", orders.get(0).getStatus());
        server.verify();
    }

    @Test
    void fetchProductionOrdersModifiedBetween_usesModifyTimeWindowAndStableOrder() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        expectLogin(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andExpect(content().string(containsString("FModifyDate")))
                .andExpect(content().string(containsString("FModifyDate+%3E%3D+%272026-06-12+08%3A00%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+%3C+%272026-06-12+09%3A00%3A00%27")))
                .andExpect(content().string(containsString("FModifyDate+ASC")))
                .andExpect(content().string(containsString("FBillNo+%3C%3E+%27%27")))
                .andExpect(content().string(containsString("FMaterialId.FNumber+%3C%3E+%27%27")))
                .andRespond(withSuccess("""
                        [[310121,"123123123","C","2026-03-19T00:00:00","A001.01.053.001","ABS","TR558A-MNP-1",12.0,"","","","2","Pcs","Pcs","","","","","","","","","2026-06-12T08:30:00"]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionOrder> orders = client.fetchProductionOrdersModifiedBetween(properties,
                LocalDateTime.of(2026, 6, 12, 8, 0),
                LocalDateTime.of(2026, 6, 12, 9, 0));

        assertEquals(1, orders.size());
        assertEquals("123123123", orders.get(0).getBillNo());
        assertEquals(LocalDateTime.of(2026, 6, 12, 8, 30), orders.get(0).getSourceModifyTime());
        server.verify();
    }

    @Test
    void getProductionOrderByBillNo_queriesActiveProductionOrderByBillNo() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        expectLogin(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("PRD_MO")))
                .andExpect(content().string(containsString("FBillNo+%3D+%27WO-001%27")))
                .andExpect(content().string(containsString("FieldKeys%22%3A%22FID%2CFBillNo%2CFDocumentStatus%2CFDate%2CFMaterialId.FNumber%2CFMaterialId.FName%2CFMaterialId.FSpecification%2CFQty%2CFPlanStartDate%2CFPlanFinishDate%2CFSrcBillNo%2CFStatus%22")))
                .andExpect(content().string(containsString("FDocumentStatus+%3C%3E+%27Z%27")))
                .andRespond(withSuccess("""
                        [[310119,"WO-001","A","2026-06-12T08:00:00","MAT-001","ABS","TR558A",12,"2026-06-12T08:00:00","2026-06-12T08:00:00","SO-001","1","kg","千克","BATCH-WO-001","","","","","","",""]]
                        """, MediaType.APPLICATION_JSON));

        ErpKingdeeProductionOrder order = client.getProductionOrderByBillNo(properties, "WO-001");

        assertEquals("310119", order.getFid());
        assertEquals("WO-001", order.getBillNo());
        assertEquals("MAT-001", order.getMaterialNumber());
        assertEquals("BATCH-WO-001", order.getBatchNumber());
        server.verify();
    }

    @Test
    void fetchProductionOrdersByBillNos_queriesBillsWithoutActiveStatusFilter() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        expectLogin(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("PRD_MO")))
                .andExpect(content().string(containsString("FBillNo+%3D+%27WO-VOID%27")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FDocumentStatus+%3C%3E+%27Z%27"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FStatus+%3C%3E+%275%27"))))
                .andRespond(withSuccess("""
                        [[310122,"WO-VOID","Z","2026-06-12T08:00:00","MAT-001","ABS","TR558A",12,"2026-06-12T08:00:00","2026-06-12T08:00:00","SO-001","1","kg","千克","BATCH-VOID","","","","","","",""]]
                        """, MediaType.APPLICATION_JSON));

        List<ErpKingdeeProductionOrder> orders =
                client.fetchProductionOrdersByBillNos(properties, List.of("WO-VOID"));

        assertEquals(1, orders.size());
        assertEquals("WO-VOID", orders.get(0).getBillNo());
        assertEquals("Z", orders.get(0).getDocumentStatus());
        server.verify();
    }

    @Test
    void getProductionOrderByBillNo_acceptsMinimalDuplicateCheckRow() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        expectLogin(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("kdservice-sessionid=abc")))
                .andExpect(content().string(containsString("FBillNo+%3D+%27WO-MIN%27")))
                .andRespond(withSuccess("""
                        [[310123,"WO-MIN","A","2026-06-12T08:00:00","MAT-001","ABS","TR558A",12,"","","","1"]]
                        """, MediaType.APPLICATION_JSON));

        ErpKingdeeProductionOrder order = client.getProductionOrderByBillNo(properties, "WO-MIN");

        assertEquals("310123", order.getFid());
        assertEquals("WO-MIN", order.getBillNo());
        assertEquals("MAT-001", order.getMaterialNumber());
        assertEquals("1", order.getStatus());
        server.verify();
    }

    @Test
    void getProductionOrderByBillNo_throwsRealKingdeeQueryErrorWhenFieldMetadataIsInvalid() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");

        expectLogin(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("FIssueType"))))
                .andRespond(withSuccess("""
                        [[{"Result":{"ResponseStatus":{"ErrorCode":500,"IsSuccess":false,"Errors":[{"FieldName":null,"Message":"元数据中标识为FIssueType的字段不存在","DIndex":0}],"SuccessEntitys":[],"SuccessMessages":[],"MsgCode":9}}}]]
                        """, MediaType.APPLICATION_JSON));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.getProductionOrderByBillNo(properties, "WO-ERR"));

        assertThat(exception.getMessage())
                .contains("PRD_MO ExecuteBillQuery failed")
                .contains("元数据中标识为FIssueType的字段不存在");
        server.verify();
    }

    @Test
    void createAndSubmitProductionOrder_viewsTemplateSavesAndSubmitsPrdMo() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");
        properties.getProductionOrder().setTemplateBillNo("TEMPLATE-MO-001");

        expectLogin(server);
        expectNoDuplicate(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andExpect(content().string(containsString("TEMPLATE-MO-001")))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":true},"Result":{
                          "FID":999,
                          "FBillNo":"TEMPLATE-MO-001",
                          "FBillType":{"FNumber":"SCDD01_SYS"},
                          "FPrdOrgId":{"FNumber":"100"},
                          "FWorkShopID":{"FNumber":"WS-01"},
                          "FOwnerTypeId":"BD_OwnerOrg",
                          "FOwnerId":{"FNumber":"100"},
                          "FTreeEntity":[{
                            "FMaterialId":{"FNumber":"OLD-MAT"},
                            "FUnitId":{"FNumber":"kg"},
                            "FQty":1,
                            "FPlanStartDate":"2026-06-01T00:00:00",
                            "FPlanFinishDate":"2026-06-01T00:00:00"
                          }]
                        }}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andExpect(content().string(containsString("WO-001")))
                .andExpect(content().string(containsString("MAT-001")))
                .andExpect(content().string(containsString("BATCH-WO-001")))
                .andExpect(content().string(containsString("2026-06-12")))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":true,"SuccessEntitys":[{"Id":310119,"Number":"WO-001"}]}}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Submit.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andExpect(content().string(containsString("WO-001")))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":true,"SuccessEntitys":[{"Id":310119,"Number":"WO-001"}]}}}
                        """, MediaType.APPLICATION_JSON));

        ErpKingdeeProductionOrderCreateResult result =
                client.createAndSubmitProductionOrder(properties, buildCreateRequest());

        assertEquals("310119", result.getErpFid());
        assertEquals("WO-001", result.getErpBillNo());
        assertEquals(Boolean.TRUE, result.getSaved());
        assertEquals(Boolean.TRUE, result.getSubmitted());
        server.verify();
    }

    @Test
    void createAndSubmitProductionOrder_allowsDuplicateCheckRowsWithMinimalFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");
        properties.getProductionOrder().setTemplateBillNo("TEMPLATE-MO-001");

        expectLogin(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FBillNo+%3D+%27WO-001%27")))
                .andExpect(content().string(containsString("FieldKeys%22%3A%22FBillNo%22")))
                .andRespond(withSuccess("""
                        []
                        """, MediaType.APPLICATION_JSON));
        expectTemplate(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andExpect(content().string(containsString("WO-001")))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":true,"SuccessEntitys":[{"Id":310119,"Number":"WO-001"}]}}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Submit.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":true,"SuccessEntitys":[{"Id":310119,"Number":"WO-001"}]}}}
                        """, MediaType.APPLICATION_JSON));

        ErpKingdeeProductionOrderCreateResult result =
                client.createAndSubmitProductionOrder(properties, buildCreateRequest());

        assertEquals("310119", result.getErpFid());
        assertEquals("WO-001", result.getErpBillNo());
        server.verify();
    }

    @Test
    void createAndSubmitProductionOrder_throwsWithSavedFidWhenSubmitFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ErpKingdeeProductionOrderClientImpl client = new ErpKingdeeProductionOrderClientImpl(restTemplate);
        ErpKingdeeProperties properties = buildProperties("https://k3.example.com");
        properties.getProductionOrder().setTemplateBillNo("TEMPLATE-MO-001");

        expectLogin(server);
        expectNoDuplicate(server);
        expectTemplate(server);
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":true,"SuccessEntitys":[{"Id":310119,"Number":"WO-001"}]}}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Submit.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":false,"Errors":[{"Message":"submit denied"}]}}}
                        """, MediaType.APPLICATION_JSON));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.createAndSubmitProductionOrder(properties, buildCreateRequest()));

        org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                .contains("WO-001")
                .contains("310119");
        server.verify();
    }

    private static ErpKingdeeProperties buildProperties(String baseUrl) {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);
        properties.getProductionOrder().setQueryLimit(500);
        return properties;
    }

    private static ErpKingdeeProductionOrderCreateRequest buildCreateRequest() {
        return ErpKingdeeProductionOrderCreateRequest.builder()
                .billNo("WO-001")
                .templateBillNo("TEMPLATE-MO-001")
                .materialNumber("MAT-001")
                .unitNumber("kg")
                .quantity(new BigDecimal("12"))
                .plannedStartDate(LocalDateTime.of(2026, 6, 12, 8, 0))
                .plannedFinishDate(LocalDateTime.of(2026, 6, 12, 8, 0))
                .sourceBillNo("SO-001")
                .batchNumber("BATCH-WO-001")
                .build();
    }

    private static void expectLogin(MockRestServiceServer server) {
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("username=kingdee-user")))
                .andRespond(withSuccess("{\"LoginResultType\":1}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "kdservice-sessionid=abc; Path=/"));
    }

    private static void expectNoDuplicate(MockRestServiceServer server) {
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("FBillNo+%3D+%27WO-001%27")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    }

    private static void expectTemplate(MockRestServiceServer server) {
        server.expect(requestTo("https://k3.example.com/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(containsString("PRD_MO")))
                .andRespond(withSuccess("""
                        {"Result":{"ResponseStatus":{"IsSuccess":true},"Result":{
                          "FID":999,
                          "FBillNo":"TEMPLATE-MO-001",
                          "FBillType":{"FNumber":"SCDD01_SYS"},
                          "FPrdOrgId":{"FNumber":"100"},
                          "FTreeEntity":[{"FMaterialId":{"FNumber":"OLD-MAT"},"FUnitId":{"FNumber":"kg"},"FQty":1}]
                        }}}
                        """, MediaType.APPLICATION_JSON));
    }

}
