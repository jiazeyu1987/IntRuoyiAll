package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_REQUEST_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Component
@RequiredArgsConstructor
public class ErpKingdeeSaleOrderClientImpl implements ErpKingdeeSaleOrderClient {

    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final int PAGE_LIMIT = 1000;
    private static final String FIELD_KEYS = String.join(",",
            "FID",
            "FBillNo",
            "FDate",
            "FDocumentStatus",
            "FCloseStatus",
            "FCancelStatus",
            "FCustId.FNumber",
            "FCustId.FName",
            "FMaterialId.FNumber",
            "FMaterialId.FName",
            "FQty",
            "FPrice",
            "FTaxPrice",
            "FEntryTaxRate",
            "FAllAmount");
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + ",FModifyDate";
    private static final int FIELD_COUNT = 15;
    private static final int INDEX_SOURCE_MODIFY_TIME = 15;
    private static final int INCREMENTAL_FIELD_COUNT = 16;

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeSaleOrder> fetchSaleOrders(ErpKingdeeProperties properties) {
        properties.validateSaleOrderSyncConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        int remaining = properties.getSaleOrder().getQueryLimit();
        Map<String, ErpKingdeeSaleOrder> orderMap = new LinkedHashMap<>();
        while (remaining > 0) {
            int pageLimit = Math.min(PAGE_LIMIT, remaining);
            JsonNode rows = executeBillQuery(properties, cookieHeader, startRow, pageLimit);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "SAL_SaleOrder response is not an array");
            }
            if (rows.isEmpty()) {
                break;
            }
            for (JsonNode row : rows) {
                if (!row.isArray() || row.size() < FIELD_COUNT) {
                    throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "SAL_SaleOrder row field count mismatch");
                }
                String fid = requiredText(row, 0, "FID");
                ErpKingdeeSaleOrder order = orderMap.computeIfAbsent(fid, key -> buildOrder(row));
                order.getLines().add(buildLine(row));
            }
            if (rows.size() < PAGE_LIMIT) {
                break;
            }
            startRow += PAGE_LIMIT;
            remaining -= rows.size();
        }
        return new ArrayList<>(orderMap.values());
    }

    @Override
    public List<ErpKingdeeSaleOrder> fetchSaleOrdersModifiedBetween(ErpKingdeeProperties properties,
                                                                    LocalDateTime windowStart,
                                                                    LocalDateTime windowEnd) {
        properties.validateSaleOrderSyncConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        int remaining = properties.getSaleOrder().getQueryLimit();
        Map<String, ErpKingdeeSaleOrder> orderMap = new LinkedHashMap<>();
        while (remaining > 0) {
            int pageLimit = Math.min(PAGE_LIMIT, remaining);
            JsonNode rows = executeIncrementalBillQuery(properties, cookieHeader, windowStart, windowEnd, startRow,
                    pageLimit);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "SAL_SaleOrder response is not an array");
            }
            if (rows.isEmpty()) {
                break;
            }
            addRowsToOrders(rows, orderMap, true);
            if (rows.size() < PAGE_LIMIT) {
                break;
            }
            startRow += rows.size();
            remaining -= rows.size();
        }
        return new ArrayList<>(orderMap.values());
    }

    private String login(ErpKingdeeProperties properties) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("acctID", properties.getAcctId());
        form.put("username", properties.getUsername());
        form.put("password", properties.getPassword());
        form.put("lcid", String.valueOf(properties.getLcid()));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), AUTH_SERVICE),
                new HttpEntity<>(toFormUrlEncodedBody(form), buildFormHeaders(null)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "ValidateUser response");
        if (!isLoginSuccess(responseJson)) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ValidateUser response missing Set-Cookie");
        }
        return cookies.stream()
                .map(cookie -> StrUtil.subBefore(cookie, ";", false))
                .collect(Collectors.joining("; "));
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties,
                                      String cookieHeader,
                                      int startRow,
                                      int limit) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(properties.getSaleOrder().getQueryDays());
        LocalDate nextDay = today.plusDays(1);
        String filterString = "(FDate >= '" + startDate + "' and FDate < '" + nextDay + "')"
                + " and (FBillNo <> '')"
                + " and (FMaterialId.FNumber <> '')";

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeSaleOrder.FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", filterString);
        query.put("OrderString", "FID DESC");
        query.put("StartRow", startRow);
        query.put("Limit", limit);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "SAL_SaleOrder ExecuteBillQuery response");
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        return responseJson;
    }

    private JsonNode executeIncrementalBillQuery(ErpKingdeeProperties properties,
                                                 String cookieHeader,
                                                 LocalDateTime windowStart,
                                                 LocalDateTime windowEnd,
                                                 int startRow,
                                                 int limit) {
        ErpKingdeeIncrementalQuerySpec spec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId(ErpKingdeeSaleOrder.FORM_ID)
                .fieldKeys(INCREMENTAL_FIELD_KEYS)
                .baseFilter("(FBillNo <> '') and (FMaterialId.FNumber <> '')")
                .startRow(startRow)
                .limit(limit)
                .build();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(spec.toQuery(windowStart, windowEnd)));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "SAL_SaleOrder incremental ExecuteBillQuery response");
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        return responseJson;
    }

    private void addRowsToOrders(JsonNode rows, Map<String, ErpKingdeeSaleOrder> orderMap, boolean incremental) {
        for (JsonNode row : rows) {
            int expectedFieldCount = incremental ? INCREMENTAL_FIELD_COUNT : FIELD_COUNT;
            if (!row.isArray() || row.size() < expectedFieldCount) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "SAL_SaleOrder row field count mismatch");
            }
            String fid = requiredText(row, 0, "FID");
            ErpKingdeeSaleOrder order = orderMap.computeIfAbsent(fid, key -> buildOrder(row));
            if (incremental && order.getSourceModifyTime() == null) {
                order.setSourceModifyTime(parseDateTime(requiredText(row, INDEX_SOURCE_MODIFY_TIME, "FModifyDate"),
                        "FModifyDate"));
            }
            order.getLines().add(buildLine(row));
        }
    }

    private ErpKingdeeSaleOrder buildOrder(JsonNode row) {
        ErpKingdeeSaleOrder order = new ErpKingdeeSaleOrder();
        order.setFid(requiredText(row, 0, "FID"));
        order.setBillNo(requiredText(row, 1, "FBillNo"));
        order.setBillDate(parseDateTime(requiredText(row, 2, "FDate"), "FDate"));
        order.setDocumentStatus(requiredText(row, 3, "FDocumentStatus"));
        order.setCloseStatus(optionalText(row, 4));
        order.setCancelStatus(optionalText(row, 5));
        order.setCustomerNumber(requiredText(row, 6, "FCustId.FNumber"));
        order.setCustomerName(requiredText(row, 7, "FCustId.FName"));
        return order;
    }

    private ErpKingdeeSaleOrder.Line buildLine(JsonNode row) {
        ErpKingdeeSaleOrder.Line line = new ErpKingdeeSaleOrder.Line();
        line.setMaterialNumber(requiredText(row, 8, "FMaterialId.FNumber"));
        line.setMaterialName(requiredText(row, 9, "FMaterialId.FName"));
        line.setQuantity(parseDecimal(requiredText(row, 10, "FQty"), "FQty"));
        line.setPrice(parseDecimal(requiredText(row, 11, "FPrice"), "FPrice"));
        line.setTaxPrice(parseDecimal(requiredText(row, 12, "FTaxPrice"), "FTaxPrice"));
        line.setTaxPercent(parseDecimal(requiredText(row, 13, "FEntryTaxRate"), "FEntryTaxRate"));
        line.setTotalAmount(parseDecimal(requiredText(row, 14, "FAllAmount"), "FAllAmount"));
        return line;
    }

    private HttpHeaders buildFormHeaders(String cookieHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-www-form-urlencoded;charset=UTF-8"));
        if (StrUtil.isNotBlank(cookieHeader)) {
            headers.set(HttpHeaders.COOKIE, cookieHeader);
        }
        return headers;
    }

    private String buildServiceUrl(String baseUrl, String serviceName) {
        String normalizedBaseUrl = StrUtil.removeSuffix(baseUrl.trim(), "/");
        if (!StrUtil.endWithIgnoreCase(normalizedBaseUrl, "/K3Cloud")) {
            normalizedBaseUrl += "/K3Cloud";
        }
        return normalizedBaseUrl + "/" + serviceName;
    }

    private JsonNode parseJson(String body, String label) {
        try {
            return JsonUtils.parseTree(body);
        } catch (RuntimeException ex) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, label + " is not valid JSON");
        }
    }

    private boolean isLoginSuccess(JsonNode responseJson) {
        return responseJson.path("LoginResultType").asInt() == 1
                || responseJson.path("IsSuccessByAPI").asBoolean(false);
    }

    private String toFormUrlEncodedBody(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> encodeFormItem(entry.getKey()) + "=" + encodeFormItem(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encodeFormItem(String value) {
        return URLEncoder.encode(StrUtil.nullToEmpty(value), StandardCharsets.UTF_8);
    }

    private String requiredText(JsonNode row, int index, String fieldName) {
        String value = optionalText(row, index);
        if (StrUtil.isBlank(value)) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, fieldName + " is blank");
        }
        return value;
    }

    private String optionalText(JsonNode row, int index) {
        JsonNode value = row.get(index);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private BigDecimal parseDecimal(String text, String fieldName) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, fieldName + " is not a decimal");
        }
    }

    private LocalDateTime parseDateTime(String text, String fieldName) {
        try {
            if (text.length() == 10) {
                return LocalDate.parse(text).atStartOfDay();
            }
            if (text.contains("T")) {
                return LocalDateTime.parse(text);
            }
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (RuntimeException ex) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, fieldName + " is not a datetime");
        }
    }

}
