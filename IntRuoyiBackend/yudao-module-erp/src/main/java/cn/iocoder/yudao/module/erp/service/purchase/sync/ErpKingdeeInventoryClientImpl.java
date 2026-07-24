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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
public class ErpKingdeeInventoryClientImpl implements ErpKingdeeInventoryClient {

    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final int PAGE_LIMIT = 1000;
    private static final String FIELD_KEYS = String.join(",",
            "FMATERIALID.FNumber",
            "FMATERIALID.FName",
            "FMATERIALID.FSpecification",
            "FBaseQty",
            "FStockId.FNumber",
            "FStockId.FName",
            "FLOT.FNumber",
            "FBaseUnitId.FName",
            "FStockOrgId.FNumber",
            "FStockOrgId.FName");
    private static final String MODIFY_TIME_FIELD = "FUpdateTime";
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + "," + MODIFY_TIME_FIELD;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeInventoryRow> fetchInventoryRows(ErpKingdeeProperties properties) {
        properties.validateBaseConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        List<ErpKingdeeInventoryRow> rows = new ArrayList<>();
        while (true) {
            JsonNode responseRows = executeBillQuery(properties, cookieHeader, startRow);
            if (!responseRows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "STK_Inventory response is not an array");
            }
            if (responseRows.isEmpty()) {
                break;
            }
            for (JsonNode row : responseRows) {
                rows.add(buildRow(row));
            }
            if (responseRows.size() < PAGE_LIMIT) {
                break;
            }
            startRow += PAGE_LIMIT;
        }
        return rows;
    }

    @Override
    public List<ErpKingdeeInventoryRow> fetchInventoryRowsModifiedBetween(ErpKingdeeProperties properties,
                                                                          LocalDateTime windowStart,
                                                                          LocalDateTime windowEnd) {
        properties.validateBaseConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        List<ErpKingdeeInventoryRow> rows = new ArrayList<>();
        while (true) {
            JsonNode responseRows = executeIncrementalBillQuery(properties, cookieHeader, windowStart, windowEnd,
                    startRow);
            if (!responseRows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "STK_Inventory response is not an array");
            }
            if (responseRows.isEmpty()) {
                break;
            }
            for (JsonNode row : responseRows) {
                rows.add(buildIncrementalRow(row));
            }
            startRow += responseRows.size();
        }
        return rows;
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

    private JsonNode executeBillQuery(ErpKingdeeProperties properties, String cookieHeader, int startRow) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeInventoryRow.FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", "FBaseQty > 0");
        query.put("OrderString", "FID DESC");
        query.put("StartRow", startRow);
        query.put("Limit", PAGE_LIMIT);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "STK_Inventory ExecuteBillQuery response");
        throwIfKingdeeErrorResponse(responseJson);
        return responseJson;
    }

    private JsonNode executeIncrementalBillQuery(ErpKingdeeProperties properties,
                                                 String cookieHeader,
                                                 LocalDateTime windowStart,
                                                 LocalDateTime windowEnd,
                                                 int startRow) {
        ErpKingdeeIncrementalQuerySpec spec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId(ErpKingdeeInventoryRow.FORM_ID)
                .fieldKeys(INCREMENTAL_FIELD_KEYS)
                .modifyTimeField(MODIFY_TIME_FIELD)
                .startRow(startRow)
                .limit(PAGE_LIMIT)
                .build();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(spec.toQuery(windowStart, windowEnd)));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "STK_Inventory incremental ExecuteBillQuery response");
        throwIfKingdeeErrorResponse(responseJson);
        return responseJson;
    }

    private void throwIfKingdeeErrorResponse(JsonNode responseJson) {
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        JsonNode responseStatus = responseJson.path(0).path(0).path("Result").path("ResponseStatus");
        if (!responseStatus.isMissingNode() && !responseStatus.path("IsSuccess").asBoolean(true)) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseStatus.toString());
        }
    }

    private ErpKingdeeInventoryRow buildRow(JsonNode row) {
        if (!row.isArray() || row.size() < 10) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "STK_Inventory row field count mismatch");
        }
        ErpKingdeeInventoryRow inventoryRow = new ErpKingdeeInventoryRow();
        inventoryRow.setMaterialNumber(requiredText(row, 0, "FMATERIALID.FNumber"));
        inventoryRow.setMaterialName(requiredText(row, 1, "FMATERIALID.FName"));
        inventoryRow.setMaterialSpecification(optionalText(row, 2));
        inventoryRow.setQuantity(parseDecimal(requiredText(row, 3, "FBaseQty"), "FBaseQty"));
        inventoryRow.setWarehouseNumber(requiredText(row, 4, "FStockId.FNumber"));
        inventoryRow.setWarehouseName(requiredText(row, 5, "FStockId.FName"));
        inventoryRow.setLotNumber(optionalText(row, 6));
        inventoryRow.setUnitName(optionalText(row, 7));
        inventoryRow.setStockOrgNumber(requiredText(row, 8, "FStockOrgId.FNumber"));
        inventoryRow.setStockOrgName(requiredText(row, 9, "FStockOrgId.FName"));
        return inventoryRow;
    }

    private ErpKingdeeInventoryRow buildIncrementalRow(JsonNode row) {
        if (!row.isArray() || row.size() < 11) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "STK_Inventory incremental row field count mismatch");
        }
        ErpKingdeeInventoryRow inventoryRow = buildRow(row);
        inventoryRow.setSourceModifyTime(parseDateTime(requiredText(row, 10, MODIFY_TIME_FIELD), MODIFY_TIME_FIELD));
        return inventoryRow;
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

    private LocalDateTime parseDateTime(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ignored) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, fieldName + " is invalid");
            }
        }
    }

}
