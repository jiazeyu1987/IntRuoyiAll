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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_ORDER_CONFIG_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID;

@Component
@RequiredArgsConstructor
public class ErpKingdeeProductionMaterialListClientImpl implements ErpKingdeeProductionMaterialListClient {

    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String FIELD_KEYS = String.join(",",
            "FMOEntryID",
            "FBillNo",
            "FMaterialID.FNumber",
            "FMoBillNo",
            "FMoEntrySeq",
            "FDocumentStatus",
            "FMaterialID2.FNumber",
            "FMaterialID2.FName",
            "FMaterialID2.FSpecification",
            "FMaterialType",
            "FNumerator",
            "FDenominator",
            "FUnitID.FName",
            "FMustQty",
            "FIssueType",
            "FModifyDate");
    private static final int INDEX_ENTRY_ID = 0;
    private static final int INDEX_BILL_NO = 1;
    private static final int INDEX_PRODUCT_CODE = 2;
    private static final int INDEX_PRODUCTION_ORDER_NO = 3;
    private static final int INDEX_PRODUCTION_ORDER_LINE_NO = 4;
    private static final int INDEX_PRODUCTION_ORDER_STATUS = 5;
    private static final int INDEX_CHILD_MATERIAL_CODE = 6;
    private static final int INDEX_CHILD_MATERIAL_NAME = 7;
    private static final int INDEX_CHILD_MATERIAL_SPECIFICATION = 8;
    private static final int INDEX_CHILD_MATERIAL_TYPE = 9;
    private static final int INDEX_NUMERATOR = 10;
    private static final int INDEX_DENOMINATOR = 11;
    private static final int INDEX_CHILD_UNIT_NAME = 12;
    private static final int INDEX_REQUIRED_QUANTITY = 13;
    private static final int INDEX_ISSUE_METHOD = 14;
    private static final int INDEX_SOURCE_MODIFY_TIME = 15;
    private static final int FIELD_COUNT = 16;
    private static final int PAGE_LIMIT = 1000;
    private static final int ORDER_NO_QUERY_BATCH_SIZE = 50;
    private static final DateTimeFormatter KINGDEE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeProductionMaterialList> fetchProductionMaterialListsModifiedBetween(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd) {
        properties.validateProductionOrderSyncConfig();
        if (windowStart == null || windowEnd == null || windowStart.isAfter(windowEnd)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_PPBOM sync window is invalid");
        }
        String cookieHeader = login(properties);
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeProductionMaterialList.FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", buildFilterString(windowStart, windowEnd));
        query.put("OrderString", "FModifyDate ASC");
        query.put("StartRow", 0);
        query.put("Limit", properties.getProductionOrder().getQueryLimit());
        JsonNode rows = postJsonData(properties, cookieHeader, QUERY_SERVICE, query, "PRD_PPBOM ExecuteBillQuery response");
        if (rows.isObject()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL, rows.toString());
        }
        if (!rows.isArray()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_PPBOM response is not an array");
        }
        List<ErpKingdeeProductionMaterialList> result = new ArrayList<>(rows.size());
        for (JsonNode row : rows) {
            result.add(buildRow(row));
        }
        return result;
    }

    @Override
    public List<ErpKingdeeProductionMaterialList> fetchProductionMaterialListsByProductionOrderNos(
            ErpKingdeeProperties properties, Collection<String> productionOrderNos) {
        properties.validateProductionOrderSyncConfig();
        List<String> normalizedOrderNos = normalizeProductionOrderNos(productionOrderNos);
        if (normalizedOrderNos.isEmpty()) {
            return Collections.emptyList();
        }
        String cookieHeader = login(properties);
        List<ErpKingdeeProductionMaterialList> result = new ArrayList<>();
        for (int index = 0; index < normalizedOrderNos.size(); index += ORDER_NO_QUERY_BATCH_SIZE) {
            List<String> batch = normalizedOrderNos.subList(index,
                    Math.min(index + ORDER_NO_QUERY_BATCH_SIZE, normalizedOrderNos.size()));
            int startRow = 0;
            while (true) {
                int limit = Math.min(PAGE_LIMIT, properties.getProductionOrder().getQueryLimit());
                JsonNode rows = executeBillQuery(properties, cookieHeader,
                        buildProductionOrderNoFilterString(batch), startRow, limit);
                if (!rows.isArray()) {
                    throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_PPBOM response is not an array");
                }
                if (rows.isEmpty()) {
                    break;
                }
                for (JsonNode row : rows) {
                    result.add(buildRow(row));
                }
                if (rows.size() < limit) {
                    break;
                }
                startRow += rows.size();
            }
        }
        return result;
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
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "ValidateUser response missing Set-Cookie");
        }
        return cookies.stream().map(cookie -> StrUtil.subBefore(cookie, ";", false)).collect(Collectors.joining("; "));
    }

    private ErpKingdeeProductionMaterialList buildRow(JsonNode row) {
        if (!row.isArray() || row.size() < FIELD_COUNT) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_PPBOM row field count mismatch");
        }
        return ErpKingdeeProductionMaterialList.builder()
                .formId(ErpKingdeeProductionMaterialList.FORM_ID)
                .entryId(requiredText(row, INDEX_ENTRY_ID, "FMOEntryID"))
                .billNo(requiredText(row, INDEX_BILL_NO, "FBillNo"))
                .productCode(requiredText(row, INDEX_PRODUCT_CODE, "FMaterialID.FNumber"))
                .productionOrderNo(requiredText(row, INDEX_PRODUCTION_ORDER_NO, "FMoBillNo"))
                .productionOrderLineNo(parseInteger(requiredText(row, INDEX_PRODUCTION_ORDER_LINE_NO, "FMoEntrySeq"), "FMoEntrySeq"))
                .productionOrderStatus(optionalText(row, INDEX_PRODUCTION_ORDER_STATUS))
                .childMaterialCode(requiredText(row, INDEX_CHILD_MATERIAL_CODE, "FMaterialID.FNumber"))
                .childMaterialName(requiredText(row, INDEX_CHILD_MATERIAL_NAME, "FMaterialID.FName"))
                .childMaterialSpecification(optionalText(row, INDEX_CHILD_MATERIAL_SPECIFICATION))
                .childMaterialType(optionalText(row, INDEX_CHILD_MATERIAL_TYPE))
                .numerator(parseDecimal(optionalText(row, INDEX_NUMERATOR), "FNumerator"))
                .denominator(parseDecimal(optionalText(row, INDEX_DENOMINATOR), "FDenominator"))
                .childUnitName(optionalText(row, INDEX_CHILD_UNIT_NAME))
                .requiredQuantity(parseRequiredDecimal(row, INDEX_REQUIRED_QUANTITY, "FMustQty"))
                .issueMethod(optionalText(row, INDEX_ISSUE_METHOD))
                .demandTime(null)
                .sourceModifyTime(parseDateTime(optionalText(row, INDEX_SOURCE_MODIFY_TIME), "FModifyDate"))
                .rawPayload(JsonUtils.toJsonString(row))
                .build();
    }

    private JsonNode postJsonData(ErpKingdeeProperties properties, String cookieHeader, String serviceName,
                                  Map<String, Object> payload, String label) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(payload));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), serviceName),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        return parseJson(response.getBody(), label);
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties, String cookieHeader, String filterString,
                                      int startRow, int limit) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeProductionMaterialList.FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", filterString);
        query.put("OrderString", "FMoBillNo ASC,FMoEntrySeq ASC,FMOEntryID ASC");
        query.put("StartRow", startRow);
        query.put("Limit", limit);
        JsonNode responseJson = postJsonData(properties, cookieHeader, QUERY_SERVICE,
                query, "PRD_PPBOM ExecuteBillQuery response");
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        return responseJson;
    }

    private String buildFilterString(LocalDateTime windowStart, LocalDateTime windowEnd) {
        return "(FBillNo <> '') and (FMoBillNo <> '') and (FMaterialID.FNumber <> '')"
                + " and (FMaterialID2.FNumber <> '')"
                + " and (FModifyDate >= '" + formatDateTime(windowStart) + "')"
                + " and (FModifyDate < '" + formatDateTime(windowEnd) + "')";
    }

    private String buildProductionOrderNoFilterString(Collection<String> productionOrderNos) {
        return "(FBillNo <> '') and (FMoBillNo in (" + productionOrderNos.stream()
                .map(orderNo -> "'" + orderNo + "'")
                .collect(Collectors.joining(",")) + "))";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(KINGDEE_DATE_TIME_FORMAT);
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

    private String toFormUrlEncodedBody(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> encodeFormItem(entry.getKey()) + "=" + encodeFormItem(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encodeFormItem(String value) {
        return URLEncoder.encode(StrUtil.nullToEmpty(value), StandardCharsets.UTF_8);
    }

    private JsonNode parseJson(String body, String label) {
        try {
            return JsonUtils.parseTree(body);
        } catch (RuntimeException ex) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, label + " is not valid JSON");
        }
    }

    private boolean isLoginSuccess(JsonNode responseJson) {
        return responseJson.path("LoginResultType").asInt() == 1
                || responseJson.path("IsSuccessByAPI").asBoolean(false);
    }

    private String requiredText(JsonNode row, int index, String fieldName) {
        String value = optionalText(row, index);
        if (StrUtil.isBlank(value)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, fieldName + " is blank");
        }
        return value;
    }

    private String optionalText(JsonNode row, int index) {
        JsonNode value = row.get(index);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private Integer parseInteger(String text, String fieldName) {
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, fieldName + " is not an integer");
        }
    }

    private BigDecimal parseRequiredDecimal(JsonNode row, int index, String fieldName) {
        String text = requiredText(row, index, fieldName);
        return parseDecimal(text, fieldName);
    }

    private BigDecimal parseDecimal(String text, String fieldName) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, fieldName + " is not a decimal");
        }
    }

    private LocalDateTime parseDateTime(String text, String fieldName) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            if (text.length() == 10) {
                return LocalDate.parse(text).atStartOfDay();
            }
            if (text.contains("T")) {
                return LocalDateTime.parse(text);
            }
            return LocalDateTime.parse(text, KINGDEE_DATE_TIME_FORMAT);
        } catch (RuntimeException ex) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, fieldName + " is not a datetime");
        }
    }

    private List<String> normalizeProductionOrderNos(Collection<String> productionOrderNos) {
        if (productionOrderNos == null || productionOrderNos.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String productionOrderNo : productionOrderNos) {
            String trimmed = StrUtil.trimToEmpty(productionOrderNo);
            if (StrUtil.isBlank(trimmed)) {
                continue;
            }
            if (trimmed.contains("'")) {
                throw exception(KINGDEE_PRODUCTION_ORDER_CONFIG_INVALID, "productionOrderNos contains single quote");
            }
            normalized.add(trimmed);
        }
        return new ArrayList<>(normalized);
    }

}
