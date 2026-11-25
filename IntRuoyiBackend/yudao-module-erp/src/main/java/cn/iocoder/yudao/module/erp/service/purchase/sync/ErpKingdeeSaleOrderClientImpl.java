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
            "FAllAmount",
            "FUnitId.FName",
            "FEntryTaxAmount",
            "FAmount",
            "FEntryNote",
            "FAllDisCount",
            "FPayAdvanceAmount",
            "FBillAmount",
            "FBillTaxAmount",
            "FBillAllAmount",
            "FSaleOrderEntry_FEntryID",
            "FNote",
            "FPriceUnitId.FName",
            "FPriceUnitQty");
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + ",FModifyDate";
    private static final int FIELD_COUNT = 28;
    private static final int INDEX_ENTRY_ID = 24;
    private static final int INDEX_SOURCE_MODIFY_TIME = 28;
    private static final int INCREMENTAL_FIELD_COUNT = 29;

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeSaleOrder> fetchSaleOrders(ErpKingdeeProperties properties) {
        return fetchSaleOrders(properties, false);
    }

    @Override
    public List<ErpKingdeeSaleOrder> fetchAllSaleOrders(ErpKingdeeProperties properties) {
        return fetchSaleOrders(properties, true);
    }

    private List<ErpKingdeeSaleOrder> fetchSaleOrders(ErpKingdeeProperties properties, boolean fullHistory) {
        properties.validateSaleOrderSyncConfig();
        String cookieHeader = login(properties);
        int limit = Math.min(PAGE_LIMIT, properties.getSaleOrder().getQueryLimit());
        String filter = "(FBillNo <> '') and (FMaterialId.FNumber <> '')";
        if (!fullHistory) {
            LocalDate today = LocalDate.now();
            filter = "(FDate >= '" + today.minusDays(properties.getSaleOrder().getQueryDays())
                    + "' and FDate < '" + today.plusDays(1) + "') and " + filter;
        }
        Map<String, Object> discovery = new LinkedHashMap<>();
        discovery.put("FormId", ErpKingdeeSaleOrder.FORM_ID);
        discovery.put("FieldKeys", "FID,FModifyDate");
        discovery.put("FilterString", filter);
        discovery.put("StartRow", 0);
        var rows = ErpKingdeeOrderWindowReader.readAll(discovery, limit, ErpKingdeeSaleOrder.FORM_ID,
                filter, INCREMENTAL_FIELD_KEYS, "FSaleOrderEntry_FEntryID", INDEX_ENTRY_ID, INDEX_SOURCE_MODIFY_TIME,
                query -> executeIncrementalBillQuery(properties, cookieHeader, null, null, query));
        Map<String, ErpKingdeeSaleOrder> orders = new LinkedHashMap<>();
        addRowsToOrders(rows, orders, true);
        return new ArrayList<>(orders.values());
    }

    @Override
    public List<ErpKingdeeSaleOrder> fetchSaleOrdersByBillNo(ErpKingdeeProperties properties, String billNo) {
        properties.validateSaleOrderSyncConfig();
        if (StrUtil.isBlank(billNo)) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "sale order bill number is blank");
        }
        String cookieHeader = login(properties);
        int limit = Math.min(PAGE_LIMIT, properties.getSaleOrder().getQueryLimit());
        String filter = "(FBillNo <> '') and (FMaterialId.FNumber <> '') and (FBillNo = '"
                + billNo.replace("'", "''") + "')";
        Map<String, Object> discovery = new LinkedHashMap<>();
        discovery.put("FormId", ErpKingdeeSaleOrder.FORM_ID);
        discovery.put("FieldKeys", "FID,FModifyDate");
        discovery.put("FilterString", filter);
        discovery.put("StartRow", 0);
        var rows = ErpKingdeeOrderWindowReader.readAll(discovery, limit, ErpKingdeeSaleOrder.FORM_ID,
                filter, INCREMENTAL_FIELD_KEYS, "FSaleOrderEntry_FEntryID", INDEX_ENTRY_ID, INDEX_SOURCE_MODIFY_TIME,
                query -> executeIncrementalBillQuery(properties, cookieHeader, null, null, query));
        Map<String, ErpKingdeeSaleOrder> orders = new LinkedHashMap<>();
        addRowsToOrders(rows, orders, true);
        return new ArrayList<>(orders.values());
    }

    @Override
    public List<ErpKingdeeSaleOrder> fetchSaleOrdersModifiedBetween(ErpKingdeeProperties properties,
                                                                    LocalDateTime windowStart,
                                                                    LocalDateTime windowEnd) {
        properties.validateSaleOrderSyncConfig();
        String cookieHeader = login(properties);
        int pageLimit = Math.min(PAGE_LIMIT, properties.getSaleOrder().getQueryLimit());
        var allRows = ErpKingdeeOrderWindowReader.read(windowStart, windowEnd, pageLimit,
                ErpKingdeeSaleOrder.FORM_ID, "FBillNo <> ''", INCREMENTAL_FIELD_KEYS, "FSaleOrderEntry_FEntryID", INDEX_ENTRY_ID, 28,
                query -> executeIncrementalBillQuery(properties, cookieHeader, windowStart, windowEnd, query));
        Map<String, ErpKingdeeSaleOrder> orderMap = new LinkedHashMap<>();
        addRowsToOrders(allRows, orderMap, true);
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
                                      int limit,
                                      boolean fullHistory) {
        return executeBillQuery(properties, cookieHeader, startRow, limit, fullHistory, null);
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties, String cookieHeader,
                                      int startRow, int limit, boolean fullHistory, String billNo) {
        return executeBillQuery(properties, cookieHeader, startRow, limit, fullHistory, billNo, null, null);
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties, String cookieHeader,
                                      int startRow, int limit, boolean fullHistory, String billNo,
                                      Long lastFid, Long lastEntryId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(properties.getSaleOrder().getQueryDays());
        LocalDate nextDay = today.plusDays(1);
        String filterString = "(FBillNo <> '') and (FMaterialId.FNumber <> '')";
        if (!fullHistory) {
            filterString = "(FDate >= '" + startDate + "' and FDate < '" + nextDay + "') and " + filterString;
        }
        if (billNo != null) {
            filterString += " and (FBillNo = '" + billNo.replace("'", "''") + "')";
        }
        if (lastFid != null) {
            // Stable identity pagination avoids live inserts shifting every later offset.
            filterString += " and (FID < " + lastFid + " or (FID = " + lastFid
                    + " and FSaleOrderEntry_FEntryID > " + lastEntryId + "))";
        }

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeSaleOrder.FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", filterString);
        query.put("OrderString", "FID DESC,FSaleOrderEntry_FEntryID ASC");
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

    private long numericSourceKey(JsonNode row, int index, String field) {
        String value = requiredText(row, index, field);
        try {
            long key = Long.parseLong(value);
            if (key > 0) return key;
        } catch (NumberFormatException invalid) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "invalid source identity " + field + ": " + value);
        }
        throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "invalid source identity " + field + ": " + value);
    }

    private JsonNode executeIncrementalBillQuery(ErpKingdeeProperties properties,
                                                 String cookieHeader,
                                                 LocalDateTime windowStart,
                                                 LocalDateTime windowEnd,
                                                 Map<String, Object> query) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
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
            addUniqueLine(order, row);
        }
    }

    private void addUniqueLine(ErpKingdeeSaleOrder order, JsonNode row) {
        ErpKingdeeSaleOrder.Line line = buildLine(row);
        for (ErpKingdeeSaleOrder.Line existing : order.getLines()) {
            if (!existing.getEntryId().equals(line.getEntryId())) continue;
            if (!existing.equals(line)) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "sale order entry changed during pagination: fid=" + order.getFid()
                                + ", entryId=" + line.getEntryId());
            }
            org.slf4j.LoggerFactory.getLogger(ErpKingdeeSaleOrderClientImpl.class).warn(
                    "Repeated source sale entry returned by pagination: fid={}, entryId={}",
                    order.getFid(), line.getEntryId());
            return;
        }
        order.getLines().add(line);
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
        order.setDiscountAmount(parseDecimal(requiredText(row, 19, "FAllDisCount"), "FAllDisCount"));
        order.setDepositAmount(parseDecimal(requiredText(row, 20, "FPayAdvanceAmount"), "FPayAdvanceAmount"));
        order.setTotalProductAmount(parseDecimal(requiredText(row, 21, "FBillAmount"), "FBillAmount"));
        order.setTotalTaxAmount(parseDecimal(requiredText(row, 22, "FBillTaxAmount"), "FBillTaxAmount"));
        order.setTotalAmount(parseDecimal(requiredText(row, 23, "FBillAllAmount"), "FBillAllAmount"));
        order.setRemark(optionalText(row, 25));
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
        line.setUnitName(requiredText(row, 15, "FUnitId.FName"));
        JsonNode priceUnit = row.get(26);
        line.setPriceUnitName(priceUnit == null || priceUnit.isNull() ? null : priceUnit.asText());
        line.setPriceUnitQuantity(parseDecimal(requiredText(row, 27, "FPriceUnitQty"), "FPriceUnitQty"));
        line.setTaxAmount(parseDecimal(requiredText(row, 16, "FEntryTaxAmount"), "FEntryTaxAmount"));
        line.setNetAmount(parseDecimal(requiredText(row, 17, "FAmount"), "FAmount"));
        line.setEntryId(requiredText(row, 24, "ERP entry ID"));
        line.setRemark(optionalText(row, 18));
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
