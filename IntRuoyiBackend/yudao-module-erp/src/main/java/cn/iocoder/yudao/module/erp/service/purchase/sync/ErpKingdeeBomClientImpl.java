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
public class ErpKingdeeBomClientImpl implements ErpKingdeeBomClient {

    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String FIELD_KEYS = String.join(",",
            "FID",
            "FNumber",
            "FDocumentStatus",
            "FMATERIALID.FNumber",
            "FMATERIALID.FName",
            "FMATERIALID.FSpecification",
            "FMATERIALIDCHILD.FNumber",
            "FMATERIALIDCHILD.FName",
            "FMATERIALIDCHILD.FSpecification",
            "FNUMERATOR",
            "FDENOMINATOR");
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + ",FModifyDate";
    private static final int FIELD_COUNT = 11;
    private static final int INDEX_SOURCE_MODIFY_TIME = 11;
    private static final DateTimeFormatter KINGDEE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeBomLine> fetchApprovedBomByParentMaterialNumber(ErpKingdeeProperties properties,
                                                                          String parentMaterialNumber) {
        properties.validateBomSyncConfig();
        if (StrUtil.isBlank(parentMaterialNumber)) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ENG_BOM parent material number is blank");
        }
        String cookieHeader = login(properties);
        JsonNode responseJson = executeBillQueryByParentMaterialNumber(properties, cookieHeader, parentMaterialNumber);
        if (!responseJson.isArray()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ENG_BOM response is not an array");
        }
        List<ErpKingdeeBomLine> result = new ArrayList<>(responseJson.size());
        for (JsonNode row : responseJson) {
            result.add(buildLine(row));
        }
        return result;
    }

    @Override
    public List<ErpKingdeeBomLine> fetchBomLines(ErpKingdeeProperties properties) {
        properties.validateBomSyncConfig();
        String cookieHeader = login(properties);
        JsonNode responseJson = executeBillQueryByFilter(properties, cookieHeader, "FDocumentStatus = 'C'");
        if (!responseJson.isArray()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ENG_BOM response is not an array");
        }
        List<ErpKingdeeBomLine> result = new ArrayList<>(responseJson.size());
        for (JsonNode row : responseJson) {
            result.add(buildLine(row));
        }
        return result;
    }

    @Override
    public List<ErpKingdeeBomLine> fetchBomLinesModifiedBetween(ErpKingdeeProperties properties,
                                                                LocalDateTime windowStart,
                                                                LocalDateTime windowEnd) {
        properties.validateBomSyncConfig();
        String cookieHeader = login(properties);
        ErpKingdeeIncrementalQuerySpec querySpec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId(ErpKingdeeBomLine.FORM_ID)
                .fieldKeys(INCREMENTAL_FIELD_KEYS)
                .baseFilter("FDocumentStatus = 'C'")
                .startRow(0)
                .limit(properties.getBom().getQueryLimit())
                .build();
        JsonNode responseJson = executeBillQuery(properties, cookieHeader, querySpec.toQuery(windowStart, windowEnd));
        if (!responseJson.isArray()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ENG_BOM response is not an array");
        }
        List<ErpKingdeeBomLine> result = new ArrayList<>(responseJson.size());
        for (JsonNode row : responseJson) {
            result.add(buildLine(row));
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

    private JsonNode executeBillQueryByParentMaterialNumber(ErpKingdeeProperties properties,
                                                            String cookieHeader,
                                                            String parentMaterialNumber) {
        return executeBillQueryByFilter(properties, cookieHeader,
                "FMATERIALID.FNumber = '" + sanitizeFilterValue(parentMaterialNumber) + "' and FDocumentStatus = 'C'");
    }

    private JsonNode executeBillQueryByFilter(ErpKingdeeProperties properties,
                                              String cookieHeader,
                                              String filterString) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeBomLine.FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", filterString);
        query.put("OrderString", "FID DESC");
        query.put("StartRow", 0);
        query.put("Limit", properties.getBom().getQueryLimit());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "ENG_BOM ExecuteBillQuery response");
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        return responseJson;
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties,
                                      String cookieHeader,
                                      Map<String, Object> query) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "ENG_BOM ExecuteBillQuery response");
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        return responseJson;
    }

    private ErpKingdeeBomLine buildLine(JsonNode row) {
        if (!row.isArray() || row.size() < FIELD_COUNT) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ENG_BOM row field count mismatch");
        }
        ErpKingdeeBomLine line = new ErpKingdeeBomLine();
        line.setFid(requiredText(row, 0, "FID"));
        line.setBomVersion(requiredText(row, 1, "FNumber"));
        line.setParentMaterialNumber(requiredText(row, 3, "FMATERIALID.FNumber"));
        line.setParentMaterialName(optionalText(row, 4));
        line.setParentMaterialSpecification(optionalText(row, 5));
        line.setChildMaterialNumber(requiredText(row, 6, "FMATERIALIDCHILD.FNumber"));
        line.setChildMaterialName(requiredText(row, 7, "FMATERIALIDCHILD.FName"));
        line.setChildMaterialSpecification(optionalText(row, 8));
        line.setChildUnitName(null);
        line.setNumerator(parsePositiveDecimal(requiredText(row, 9, "FNUMERATOR"), "FNUMERATOR"));
        line.setDenominator(parsePositiveDecimal(requiredText(row, 10, "FDENOMINATOR"), "FDENOMINATOR"));
        if (row.size() > INDEX_SOURCE_MODIFY_TIME) {
            line.setSourceModifyTime(parseDateTime(optionalText(row, INDEX_SOURCE_MODIFY_TIME), "FModifyDate"));
        }
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
        return value == null || value.isNull() ? "" : ErpKingdeeTextNormalizer.normalize(value.asText());
    }

    private BigDecimal parsePositiveDecimal(String text, String fieldName) {
        try {
            BigDecimal value = new BigDecimal(text);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, fieldName + " must be positive");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, fieldName + " is not a decimal");
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
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, fieldName + " is not a datetime");
        }
    }

    private String sanitizeFilterValue(String value) {
        if (value.contains("'")) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ENG_BOM parent material number contains quote");
        }
        return value;
    }

}
