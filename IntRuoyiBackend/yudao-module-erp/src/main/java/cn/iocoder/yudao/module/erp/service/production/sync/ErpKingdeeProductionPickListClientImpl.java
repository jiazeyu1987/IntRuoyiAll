package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeIncrementalQuerySpec;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_PICK_LIST_REQUEST_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID;

@Component
@RequiredArgsConstructor
public class ErpKingdeeProductionPickListClientImpl implements ErpKingdeeProductionPickListClient {

    private static final String FORM_ID = "PRD_PickMtrl";
    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final int FULL_QUERY_PAGE_LIMIT = 200;
    private static final int FULL_QUERY_CHUNK_DAYS = 7;
    private static final int INCREMENTAL_QUERY_PAGE_LIMIT = 1000;
    private static final String MODIFY_TIME_FIELD = "FModifyDate";
    private static final String FIELD_KEYS = String.join(",",
            "FID",
            "FBillNo",
            "FDate",
            "FDocumentStatus",
            "FStockOrgId.FNumber",
            "FStockOrgId.FName",
            "FPrdOrgId.FNumber",
            "FPrdOrgId.FName",
            "FOwnerId0.FNumber",
            "FOwnerId0.FName",
            "FDescription",
            "FEntity_FEntryID",
            "FMaterialId.FNumber",
            "FMaterialId.FName",
            "FMaterialId.FSpecification",
            "FUnitID.FName",
            "FActualQty",
            "FBaseActualQty",
            "FAppQty",
            "FStockId.FNumber",
            "FStockId.FName",
            "FStockLocId.FF100002.FNumber",
            "FStockLocId.FF100002.FName",
            "FLot.FNumber",
            "FMoBillNo",
            "FMoEntrySeq",
            "FPPBomBillNo",
            "FEntryWorkShopId.FNumber",
            "FEntryWorkShopId.FName",
            "FStockStatusId.FNumber",
            "FStockStatusId.FName");
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + "," + MODIFY_TIME_FIELD;
    private static final int FIELD_COUNT = 31;
    private static final int INCREMENTAL_FIELD_COUNT = 32;
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"));

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeProductionPickList> fetchProductionPickLists(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd) {
        properties.validateBaseConfig();
        validateWindow(windowStart, windowEnd);
        String cookieHeader = login(properties);
        LocalDate endDateExclusive = windowEnd.toLocalDate().plusDays(1);
        Map<String, ErpKingdeeProductionPickList> result = new LinkedHashMap<>();
        LocalDate chunkStart = windowStart.toLocalDate();
        while (chunkStart.isBefore(endDateExclusive)) {
            LocalDate chunkEndExclusive = chunkStart.plusDays(FULL_QUERY_CHUNK_DAYS);
            if (chunkEndExclusive.isAfter(endDateExclusive)) {
                chunkEndExclusive = endDateExclusive;
            }
            fetchFullChunk(properties, cookieHeader, chunkStart, chunkEndExclusive, result);
            chunkStart = chunkEndExclusive;
        }
        return new ArrayList<>(result.values());
    }

    private void fetchFullChunk(ErpKingdeeProperties properties, String cookieHeader,
                                LocalDate chunkStart, LocalDate chunkEndExclusive,
                                Map<String, ErpKingdeeProductionPickList> result) {
        int startRow = 0;
        while (true) {
            JsonNode rows = executeBillQuery(
                    properties, cookieHeader, chunkStart, chunkEndExclusive,
                    startRow, FULL_QUERY_PAGE_LIMIT);
            validateRows(rows);
            if (rows.isEmpty()) {
                return;
            }
            addRows(rows, result, false);
            if (rows.size() < FULL_QUERY_PAGE_LIMIT) {
                return;
            }
            startRow += rows.size();
        }
    }

    @Override
    public List<ErpKingdeeProductionPickList> fetchProductionPickListsModifiedBetween(
            ErpKingdeeProperties properties, LocalDateTime windowStart, LocalDateTime windowEnd) {
        properties.validateBaseConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        Map<String, ErpKingdeeProductionPickList> result = new LinkedHashMap<>();
        while (true) {
            JsonNode rows = executeIncrementalBillQuery(
                    properties, cookieHeader, windowStart, windowEnd, startRow);
            validateRows(rows);
            if (rows.isEmpty()) {
                break;
            }
            addRows(rows, result, true);
            if (rows.size() < INCREMENTAL_QUERY_PAGE_LIMIT) {
                break;
            }
            startRow += rows.size();
        }
        return new ArrayList<>(result.values());
    }

    private String login(ErpKingdeeProperties properties) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("acctID", properties.getAcctId());
        form.put("username", properties.getUsername());
        form.put("password", properties.getPassword());
        form.put("lcid", String.valueOf(properties.getLcid()));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), AUTH_SERVICE),
                new HttpEntity<>(toFormUrlEncodedBody(form), buildFormHeaders(null)), String.class);
        JsonNode responseJson = parseJson(response.getBody(), "ValidateUser response");
        if (!isLoginSuccess(responseJson)) {
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_REQUEST_FAIL, responseJson.toString());
        }
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID,
                    "ValidateUser response missing Set-Cookie");
        }
        return cookies.stream()
                .map(cookie -> StrUtil.subBefore(cookie, ";", false))
                .collect(Collectors.joining("; "));
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties, String cookieHeader,
                                      LocalDate startDate, LocalDate endDateExclusive,
                                      int startRow, int limit) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString",
                "(FBillNo <> '') and (FDate >= '" + startDate + "' and FDate < '" + endDateExclusive + "')");
        query.put("OrderString", "FModifyDate DESC,FID DESC");
        query.put("StartRow", startRow);
        query.put("Limit", limit);
        return postQuery(properties, cookieHeader, query, "PRD_PickMtrl ExecuteBillQuery response");
    }

    private JsonNode executeIncrementalBillQuery(ErpKingdeeProperties properties, String cookieHeader,
                                                 LocalDateTime windowStart, LocalDateTime windowEnd,
                                                 int startRow) {
        ErpKingdeeIncrementalQuerySpec spec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId(FORM_ID)
                .fieldKeys(INCREMENTAL_FIELD_KEYS)
                .baseFilter("(FBillNo <> '')")
                .modifyTimeField(MODIFY_TIME_FIELD)
                .startRow(startRow)
                .limit(INCREMENTAL_QUERY_PAGE_LIMIT)
                .build();
        return postQuery(properties, cookieHeader, spec.toQuery(windowStart, windowEnd),
                "PRD_PickMtrl incremental ExecuteBillQuery response");
    }

    private JsonNode postQuery(ErpKingdeeProperties properties, String cookieHeader,
                               Map<String, Object> query, String label) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)), String.class);
        JsonNode responseJson = parseJson(response.getBody(), label);
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_REQUEST_FAIL, responseJson.toString());
        }
        return responseJson;
    }

    private void validateWindow(LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (windowStart == null) {
            throw new IllegalArgumentException("ERP production pick list full sync windowStart is null");
        }
        if (windowEnd == null || !windowEnd.isAfter(windowStart)) {
            throw new IllegalArgumentException(
                    "ERP production pick list full sync windowEnd must be after windowStart");
        }
    }

    private void validateRows(JsonNode rows) {
        if (!rows.isArray()) {
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID,
                    "PRD_PickMtrl response is not an array");
        }
        JsonNode nestedStatus = rows.path(0).path(0).path("Result").path("ResponseStatus");
        if (nestedStatus.isObject() && !nestedStatus.path("IsSuccess").asBoolean(true)) {
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_REQUEST_FAIL, nestedStatus.toString());
        }
    }

    private void addRows(JsonNode rows, Map<String, ErpKingdeeProductionPickList> result,
                         boolean incremental) {
        for (JsonNode row : rows) {
            int expected = incremental ? INCREMENTAL_FIELD_COUNT : FIELD_COUNT;
            if (!row.isArray() || row.size() < expected) {
                throw exception(KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID,
                        "PRD_PickMtrl row field count mismatch");
            }
            String fid = requiredText(row, 0, "FID");
            ErpKingdeeProductionPickList pickList =
                    result.computeIfAbsent(fid, ignored -> buildHeader(row));
            if (incremental && pickList.getSourceModifyTime() == null) {
                pickList.setSourceModifyTime(parseDateTime(
                        requiredText(row, 31, MODIFY_TIME_FIELD), MODIFY_TIME_FIELD));
            }
            pickList.getLines().add(buildLine(row));
        }
    }

    private ErpKingdeeProductionPickList buildHeader(JsonNode row) {
        ErpKingdeeProductionPickList pickList = new ErpKingdeeProductionPickList();
        pickList.setFid(requiredText(row, 0, "FID"));
        pickList.setBillNo(requiredText(row, 1, "FBillNo"));
        pickList.setBillDate(parseDateTime(optionalText(row, 2), "FDate"));
        pickList.setDocumentStatus(optionalText(row, 3));
        pickList.setStockOrgNumber(optionalText(row, 4));
        pickList.setStockOrgName(optionalText(row, 5));
        pickList.setProductionOrgNumber(optionalText(row, 6));
        pickList.setProductionOrgName(optionalText(row, 7));
        pickList.setOwnerNumber(optionalText(row, 8));
        pickList.setOwnerName(optionalText(row, 9));
        pickList.setDescription(optionalText(row, 10));
        return pickList;
    }

    private ErpKingdeeProductionPickList.Line buildLine(JsonNode row) {
        ErpKingdeeProductionPickList.Line line = new ErpKingdeeProductionPickList.Line();
        line.setEntryId(requiredText(row, 11, "FEntity_FEntryID"));
        line.setMaterialNumber(requiredText(row, 12, "FMaterialId.FNumber"));
        line.setMaterialName(requiredText(row, 13, "FMaterialId.FName"));
        line.setMaterialSpecification(optionalText(row, 14));
        line.setUnitName(optionalText(row, 15));
        line.setActualQuantity(parseDecimal(requiredText(row, 16, "FActualQty"), "FActualQty"));
        line.setBaseActualQuantity(parseOptionalDecimal(row, 17, "FBaseActualQty"));
        line.setRequestedQuantity(parseDecimal(requiredText(row, 18, "FAppQty"), "FAppQty"));
        line.setWarehouseNumber(optionalText(row, 19));
        line.setWarehouseName(optionalText(row, 20));
        line.setStockLocationNumber(optionalText(row, 21));
        line.setStockLocationName(optionalText(row, 22));
        line.setLotNumber(optionalText(row, 23));
        line.setProductionOrderNo(optionalText(row, 24));
        line.setProductionOrderLineNo(parseOptionalInteger(row, 25, "FMoEntrySeq"));
        line.setProductionMaterialListNo(optionalText(row, 26));
        line.setWorkshopNumber(optionalText(row, 27));
        line.setWorkshopName(optionalText(row, 28));
        line.setStockStatusNumber(optionalText(row, 29));
        line.setStockStatusName(optionalText(row, 30));
        return line;
    }

    private HttpHeaders buildFormHeaders(String cookieHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/x-www-form-urlencoded;charset=UTF-8"));
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
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID,
                    label + " is not valid JSON");
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
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID,
                    fieldName + " is blank");
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
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID,
                    fieldName + " is not a decimal");
        }
    }

    private BigDecimal parseOptionalDecimal(JsonNode row, int index, String fieldName) {
        String value = optionalText(row, index);
        return StrUtil.isBlank(value) ? null : parseDecimal(value, fieldName);
    }

    private Integer parseOptionalInteger(JsonNode row, int index, String fieldName) {
        String value = optionalText(row, index);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID,
                    fieldName + " is not an integer");
        }
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Kingdee returns different formal date formats across deployments.
            }
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw exception(KINGDEE_PRODUCTION_PICK_LIST_RESPONSE_INVALID,
                    fieldName + " is invalid");
        }
    }

}
