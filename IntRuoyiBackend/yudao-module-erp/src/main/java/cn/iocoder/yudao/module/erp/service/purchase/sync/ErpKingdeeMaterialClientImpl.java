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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_REQUEST_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Component
@RequiredArgsConstructor
public class ErpKingdeeMaterialClientImpl implements ErpKingdeeMaterialClient {

    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String FIELD_KEYS = String.join(",",
            "FNumber",
            "FName",
            "FSpecification",
            "FCategoryID.FNumber",
            "FCategoryID.FName",
            "FBaseUnitId.FName",
            "FForbidStatus",
            "FDocumentStatus");
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + ",FModifyDate";
    private static final int PAGE_LIMIT = 1000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeMaterial> fetchMaterials(ErpKingdeeProperties properties) {
        properties.validateProductSyncConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        int pageLimit = Math.min(PAGE_LIMIT, properties.getProduct().getQueryLimit());
        List<ErpKingdeeMaterial> materials = new ArrayList<>();
        while (true) {
            JsonNode rows = executeBillQuery(properties, cookieHeader, startRow, pageLimit);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "BD_MATERIAL response is not an array");
            }
            if (rows.isEmpty()) {
                break;
            }
            for (JsonNode row : rows) {
                materials.add(buildMaterial(row));
            }
            startRow += rows.size();
        }
        return materials;
    }

    @Override
    public List<ErpKingdeeMaterial> fetchMaterialsModifiedBetween(ErpKingdeeProperties properties,
                                                                  LocalDateTime windowStart,
                                                                  LocalDateTime windowEnd) {
        properties.validateProductSyncConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        int pageLimit = Math.min(PAGE_LIMIT, properties.getProduct().getQueryLimit());
        List<ErpKingdeeMaterial> materials = new ArrayList<>();
        while (true) {
            JsonNode rows = executeIncrementalBillQuery(properties, cookieHeader, windowStart, windowEnd, startRow,
                    pageLimit);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "BD_MATERIAL response is not an array");
            }
            if (rows.isEmpty()) {
                break;
            }
            for (JsonNode row : rows) {
                materials.add(buildIncrementalMaterial(row));
            }
            startRow += rows.size();
        }
        return materials;
    }

    @Override
    public List<ErpKingdeeMaterial> fetchMaterialsByNumbers(ErpKingdeeProperties properties,
                                                            Collection<String> materialNumbers) {
        properties.validateProductSyncConfig();
        LinkedHashSet<String> normalizedNumbers = materialNumbers.stream()
                .map(StrUtil::trimToEmpty)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedNumbers.isEmpty()) {
            return List.of();
        }
        String cookieHeader = login(properties);
        JsonNode rows = executeMaterialNumbersBillQuery(properties, cookieHeader, normalizedNumbers);
        if (!rows.isArray()) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "BD_MATERIAL by numbers response is not an array");
        }
        List<ErpKingdeeMaterial> materials = new ArrayList<>();
        for (JsonNode row : rows) {
            materials.add(buildIncrementalMaterial(row));
        }
        return materials;
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
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeMaterial.FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", "(FNumber <> '') and (FDocumentStatus = 'C')");
        query.put("OrderString", "FNumber ASC");
        query.put("StartRow", startRow);
        query.put("Limit", limit);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "BD_MATERIAL ExecuteBillQuery response");
        throwIfKingdeeErrorResponse(responseJson);
        return responseJson;
    }

    private JsonNode executeIncrementalBillQuery(ErpKingdeeProperties properties,
                                                 String cookieHeader,
                                                 LocalDateTime windowStart,
                                                 LocalDateTime windowEnd,
                                                 int startRow,
                                                 int limit) {
        ErpKingdeeIncrementalQuerySpec spec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId(ErpKingdeeMaterial.FORM_ID)
                .fieldKeys(INCREMENTAL_FIELD_KEYS)
                .baseFilter("(FNumber <> '')")
                .startRow(startRow)
                .limit(limit)
                .build();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(spec.toQuery(windowStart, windowEnd)));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "BD_MATERIAL incremental ExecuteBillQuery response");
        throwIfKingdeeErrorResponse(responseJson);
        return responseJson;
    }

    private JsonNode executeMaterialNumbersBillQuery(ErpKingdeeProperties properties,
                                                     String cookieHeader,
                                                     Collection<String> materialNumbers) {
        String numberFilter = materialNumbers.stream()
                .map(number -> "'" + number.replace("'", "''") + "'")
                .collect(Collectors.joining(","));
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeMaterial.FORM_ID);
        query.put("FieldKeys", INCREMENTAL_FIELD_KEYS);
        query.put("FilterString", "FNumber in (" + numberFilter + ")");
        query.put("OrderString", "FNumber ASC");
        query.put("StartRow", 0);
        query.put("Limit", materialNumbers.size());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), "BD_MATERIAL by numbers ExecuteBillQuery response");
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

    private ErpKingdeeMaterial buildMaterial(JsonNode row) {
        if (!row.isArray() || row.size() < 8) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "BD_MATERIAL row field count mismatch");
        }
        ErpKingdeeMaterial material = new ErpKingdeeMaterial();
        material.setMaterialNumber(requiredText(row, 0, "FNumber"));
        material.setMaterialName(requiredText(row, 1, "FName"));
        material.setSpecification(optionalText(row, 2));
        material.setCategoryCode(requiredText(row, 3, "FCategoryID.FNumber"));
        material.setCategoryName(requiredText(row, 4, "FCategoryID.FName"));
        material.setUnitName(requiredText(row, 5, "FBaseUnitId.FName"));
        material.setForbidStatus(requiredText(row, 6, "FForbidStatus"));
        material.setDocumentStatus(requiredText(row, 7, "FDocumentStatus"));
        return material;
    }

    private ErpKingdeeMaterial buildIncrementalMaterial(JsonNode row) {
        if (!row.isArray() || row.size() < 9) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "BD_MATERIAL incremental row field count mismatch");
        }
        ErpKingdeeMaterial material = buildMaterial(row);
        material.setSourceModifyTime(parseDateTime(requiredText(row, 8, "FModifyDate"), "FModifyDate"));
        return material;
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
        if (value == null || value.isNull()) {
            return "";
        }
        return ErpKingdeeTextNormalizer.normalize(value.asText());
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(value, ISO_DATE_TIME_FORMATTER);
            } catch (DateTimeParseException isoEx) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, fieldName + " is invalid");
            }
        }
    }

}
