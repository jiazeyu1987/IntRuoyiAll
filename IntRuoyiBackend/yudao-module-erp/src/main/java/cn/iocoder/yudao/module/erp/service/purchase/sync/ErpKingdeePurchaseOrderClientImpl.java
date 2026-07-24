package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.hutool.core.collection.CollUtil;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.net.URLEncoder;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_REQUEST_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Component
@RequiredArgsConstructor
public class ErpKingdeePurchaseOrderClientImpl implements ErpKingdeePurchaseOrderClient {

    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String VIEW_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";
    private static final int PAGE_LIMIT = 1000;
    private static final String FIELD_KEYS = String.join(",",
            "FID",
            "FBillNo",
            "FDate",
            "FDocumentStatus",
            "FCloseStatus",
            "FCancelStatus",
            "FPurchaseOrgId.FNumber",
            "FSupplierId.FNumber",
            "FSupplierId.FName",
            "FMaterialId.FNumber",
            "FMaterialId.FName",
            "FQty",
            "FPrice",
            "FEntryTaxRate",
            "FEntryNote");
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + ",FModifyDate";

    private static final int INDEX_FID = 0;
    private static final int INDEX_BILL_NO = 1;
    private static final int INDEX_BILL_DATE = 2;
    private static final int INDEX_DOCUMENT_STATUS = 3;
    private static final int INDEX_CLOSE_STATUS = 4;
    private static final int INDEX_CANCEL_STATUS = 5;
    private static final int INDEX_SUPPLIER_NUMBER = 7;
    private static final int INDEX_SUPPLIER_NAME = 8;
    private static final int INDEX_MATERIAL_NUMBER = 9;
    private static final int INDEX_MATERIAL_NAME = 10;
    private static final int INDEX_QTY = 11;
    private static final int INDEX_PRICE = 12;
    private static final int INDEX_TAX_RATE = 13;
    private static final int INDEX_LINE_REMARK = 14;
    private static final int FIELD_COUNT = 15;
    private static final int INDEX_SOURCE_MODIFY_TIME = 15;
    private static final int INCREMENTAL_FIELD_COUNT = 16;

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeePurchaseOrder> fetchPurchaseOrders(ErpKingdeeProperties properties) {
        properties.validatePurchaseOrderSyncConfig();
        String cookieHeader = login(properties);
        JsonNode queryResponse = executeBillQuery(properties, cookieHeader);
        if (!queryResponse.isArray()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ExecuteBillQuery response is not an array");
        }
        return rowsToOrders(queryResponse);
    }

    @Override
    public List<ErpKingdeePurchaseOrder> fetchPurchaseOrdersModifiedBetween(ErpKingdeeProperties properties,
                                                                            LocalDateTime windowStart,
                                                                            LocalDateTime windowEnd) {
        properties.validatePurchaseOrderSyncConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        int remaining = properties.getPurchaseOrder().getQueryLimit();
        List<JsonNode> allRows = new ArrayList<>();
        while (remaining > 0) {
            int pageLimit = Math.min(PAGE_LIMIT, remaining);
            JsonNode rows = executeIncrementalBillQuery(properties, cookieHeader, windowStart, windowEnd, startRow,
                    pageLimit);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ExecuteBillQuery response is not an array");
            }
            if (rows.isEmpty()) {
                break;
            }
            rows.forEach(allRows::add);
            if (rows.size() < pageLimit) {
                break;
            }
            startRow += rows.size();
            remaining -= rows.size();
        }
        return rowsToOrders(JsonUtils.parseTree(JsonUtils.toJsonString(allRows)), true);
    }

    @Override
    public Map<String, ErpKingdeeMaterialDetail> fetchMaterialDetails(ErpKingdeeProperties properties,
                                                                      Collection<String> materialNumbers) {
        properties.validatePurchaseOrderSyncConfig();
        if (CollUtil.isEmpty(materialNumbers)) {
            return Map.of();
        }
        String cookieHeader = login(properties);
        Map<String, ErpKingdeeMaterialDetail> details = new LinkedHashMap<>();
        for (String materialNumber : materialNumbers) {
            if (StrUtil.isBlank(materialNumber)) {
                continue;
            }
            details.put(materialNumber, fetchMaterialDetail(properties, cookieHeader, materialNumber));
        }
        return details;
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
            String message = firstNonBlank(responseJson.path("Message").asText(null),
                    responseJson.path("message").asText(null),
                    responseJson.toString());
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, message);
        }
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (CollUtil.isEmpty(cookies)) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ValidateUser response missing Set-Cookie");
        }
        return CollUtil.join(CollUtil.map(cookies, cookie -> StrUtil.subBefore(cookie, ";", false), true), "; ");
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties, String cookieHeader) {
        Map<String, Object> query = buildPurchaseOrderQuery(properties);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "ExecuteBillQuery response");
        if (responseJson.isObject()) {
            JsonNode status = responseJson.path("Result").path("ResponseStatus");
            if (status.path("IsSuccess").asBoolean(true)) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, responseJson.toString());
            }
            JsonNode errors = status.path("Errors");
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL,
                    errors.isMissingNode() || errors.isNull() ? status.toString() : errors.toString());
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
                .formId(ErpKingdeePurchaseOrder.FORM_ID)
                .fieldKeys(INCREMENTAL_FIELD_KEYS)
                .baseFilter("FPurchaseOrgId.FNumber = '" + properties.getPurchaseOrder().getPurchaseOrgNumber() + "'")
                .startRow(startRow)
                .limit(limit)
                .build();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(spec.toQuery(windowStart, windowEnd)));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "PUR_PurchaseOrder incremental ExecuteBillQuery response");
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        return responseJson;
    }

    private ErpKingdeeMaterialDetail fetchMaterialDetail(ErpKingdeeProperties properties,
                                                         String cookieHeader,
                                                         String materialNumber) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("formid", "BD_MATERIAL");
        body.add("data", JsonUtils.toJsonString(Map.of("Number", materialNumber)));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), VIEW_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "BD_MATERIAL view response");
        JsonNode result = responseJson.path("Result");
        JsonNode status = result.path("ResponseStatus");
        if (!status.path("IsSuccess").asBoolean(false)) {
            JsonNode errors = status.path("Errors");
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL,
                    errors.isMissingNode() || errors.isNull() ? status.toString() : errors.toString());
        }
        JsonNode material = result.path("Result");
        if (!material.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "BD_MATERIAL view result is not an object");
        }
        return parseMaterialDetail(material, materialNumber);
    }

    private Map<String, Object> buildPurchaseOrderQuery(ErpKingdeeProperties properties) {
        ErpKingdeeProperties.PurchaseOrderProperties purchaseOrder = properties.getPurchaseOrder();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(purchaseOrder.getQueryDays());
        LocalDate nextDay = today.plusDays(1);

        String filterString = "FPurchaseOrgId.FNumber = '" + purchaseOrder.getPurchaseOrgNumber() + "'"
                + " and ((FCreateDate >= '" + startDate + " 00:00:00' and FCreateDate < '" + nextDay + " 00:00:00')"
                + " or (FDate >= '" + startDate + "' and FDate < '" + nextDay + "'))";

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeePurchaseOrder.FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", filterString);
        query.put("OrderString", "FID DESC");
        query.put("StartRow", 0);
        query.put("Limit", purchaseOrder.getQueryLimit());
        return query;
    }

    private List<ErpKingdeePurchaseOrder> rowsToOrders(JsonNode rows) {
        return rowsToOrders(rows, false);
    }

    private List<ErpKingdeePurchaseOrder> rowsToOrders(JsonNode rows, boolean incremental) {
        Map<String, ErpKingdeePurchaseOrder> orderMap = new LinkedHashMap<>();
        for (JsonNode row : rows) {
            int expectedFieldCount = incremental ? INCREMENTAL_FIELD_COUNT : FIELD_COUNT;
            if (!row.isArray() || row.size() < expectedFieldCount) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ExecuteBillQuery row field count mismatch");
            }
            String fid = requiredText(row, INDEX_FID, "FID");
            ErpKingdeePurchaseOrder order = orderMap.computeIfAbsent(fid, key -> buildOrder(row));
            if (incremental && order.getSourceModifyTime() == null) {
                order.setSourceModifyTime(parseDateTime(requiredText(row, INDEX_SOURCE_MODIFY_TIME, "FModifyDate"),
                        "FModifyDate"));
            }
            order.getLines().add(buildLine(row));
        }
        return new ArrayList<>(orderMap.values());
    }

    private ErpKingdeePurchaseOrder buildOrder(JsonNode row) {
        ErpKingdeePurchaseOrder order = new ErpKingdeePurchaseOrder();
        order.setFid(requiredText(row, INDEX_FID, "FID"));
        order.setBillNo(requiredText(row, INDEX_BILL_NO, "FBillNo"));
        order.setBillDate(parseDateTime(requiredText(row, INDEX_BILL_DATE, "FDate"), "FDate"));
        order.setDocumentStatus(requiredText(row, INDEX_DOCUMENT_STATUS, "FDocumentStatus"));
        order.setCloseStatus(optionalText(row, INDEX_CLOSE_STATUS));
        order.setCancelStatus(optionalText(row, INDEX_CANCEL_STATUS));
        order.setSupplierNumber(requiredText(row, INDEX_SUPPLIER_NUMBER, "FSupplierId.FNumber"));
        order.setSupplierName(optionalText(row, INDEX_SUPPLIER_NAME));
        return order;
    }

    private ErpKingdeePurchaseOrder.Line buildLine(JsonNode row) {
        ErpKingdeePurchaseOrder.Line line = new ErpKingdeePurchaseOrder.Line();
        line.setMaterialNumber(requiredText(row, INDEX_MATERIAL_NUMBER, "FMaterialId.FNumber"));
        line.setMaterialName(optionalText(row, INDEX_MATERIAL_NAME));
        line.setQuantity(parseDecimal(requiredText(row, INDEX_QTY, "FQty"), "FQty"));
        line.setPrice(parseDecimal(requiredText(row, INDEX_PRICE, "FPrice"), "FPrice"));
        line.setTaxPercent(parseDecimal(requiredText(row, INDEX_TAX_RATE, "FEntryTaxRate"), "FEntryTaxRate"));
        line.setRemark(optionalText(row, INDEX_LINE_REMARK));
        return line;
    }

    private ErpKingdeeMaterialDetail parseMaterialDetail(JsonNode material, String materialNumber) {
        JsonNode materialBase = firstArrayObject(material.path("MaterialBase"), "MaterialBase", materialNumber);
        JsonNode purchaseInfo = firstArrayObject(material.path("MaterialPurchase"), "MaterialPurchase", materialNumber);
        JsonNode category = materialBase.path("CategoryID");
        JsonNode purchaseUnit = purchaseInfo.path("PurchaseUnitID");

        ErpKingdeeMaterialDetail detail = new ErpKingdeeMaterialDetail();
        detail.setMaterialNumber(requiredObjectText(material, "Number", "BD_MATERIAL.Number", materialNumber));
        detail.setMaterialName(requiredLocalizedText(material.path("Name"), "BD_MATERIAL.Name", materialNumber));
        detail.setSpecification(optionalLocalizedText(material.path("Specification")));
        detail.setCategoryCode(requiredObjectText(category, "Number", "BD_MATERIAL.CategoryID.Number", materialNumber));
        detail.setCategoryName(requiredLocalizedText(category.path("Name"), "BD_MATERIAL.CategoryID.Name", materialNumber));
        detail.setUnitName(requiredLocalizedText(purchaseUnit.path("Name"), "BD_MATERIAL.PurchaseUnitID.Name", materialNumber));
        return detail;
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

    private JsonNode firstArrayObject(JsonNode node, String fieldName, String materialNumber) {
        if (!node.isArray() || node.isEmpty() || !node.get(0).isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    fieldName + " is missing for material " + materialNumber);
        }
        return node.get(0);
    }

    private String requiredObjectText(JsonNode node, String propertyName, String fieldName, String materialNumber) {
        if (!node.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    fieldName + " is missing for material " + materialNumber);
        }
        String text = StrUtil.trim(node.path(propertyName).asText(null));
        if (StrUtil.isBlank(text)) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    fieldName + " is blank for material " + materialNumber);
        }
        return text;
    }

    private String requiredLocalizedText(JsonNode node, String fieldName, String materialNumber) {
        String text = optionalLocalizedText(node);
        if (StrUtil.isBlank(text)) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    fieldName + " is blank for material " + materialNumber);
        }
        return text;
    }

    private String optionalLocalizedText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return StrUtil.trim(node.asText());
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (!item.isObject()) {
                    continue;
                }
                String value = StrUtil.emptyToNull(firstNonBlank(
                        StrUtil.trim(item.path("Value").asText(null)),
                        StrUtil.trim(item.path("Name").asText(null))));
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
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
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText();
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String toFormUrlEncodedBody(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> encodeFormItem(entry.getKey()) + "=" + encodeFormItem(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encodeFormItem(String value) {
        return URLEncoder.encode(StrUtil.nullToEmpty(value), StandardCharsets.UTF_8);
    }

}
