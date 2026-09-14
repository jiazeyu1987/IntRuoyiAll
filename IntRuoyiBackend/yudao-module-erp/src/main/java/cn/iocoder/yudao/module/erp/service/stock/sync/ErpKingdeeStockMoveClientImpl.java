package cn.iocoder.yudao.module.erp.service.stock.sync;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_REQUEST_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Component
@RequiredArgsConstructor
public class ErpKingdeeStockMoveClientImpl implements ErpKingdeeStockMoveClient {

    private static final String FORM_ID = "STK_TransferDirect";
    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final int PAGE_LIMIT = 1000;
    private static final int INITIAL_QUERY_DAYS = 365;
    private static final int INITIAL_QUERY_LIMIT = 5000;
    private static final String MODIFY_TIME_FIELD = "FModifyDate";
    private static final String FIELD_KEYS = String.join(",",
            "FID",
            "FBillNo",
            "FDate",
            "FDocumentStatus",
            "FTransferDirect",
            "FBizType",
            "FNote",
            "FBillEntry_FEntryID",
            "FMaterialId.FNumber",
            "FMaterialId.FName",
            "FMaterialId.FSpecification",
            "FUnitID.FName",
            "FQty",
            "FSrcStockId.FNumber",
            "FSrcStockId.FName",
            "FDestStockId.FNumber",
            "FDestStockId.FName",
            "FSrcStockLocId",
            "FDestStockLocId",
            "FLot.FNumber");
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + "," + MODIFY_TIME_FIELD;
    private static final int FIELD_COUNT = 20;
    private static final int INCREMENTAL_FIELD_COUNT = 21;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeStockMove> fetchStockMoves(ErpKingdeeProperties properties) {
        properties.validateBaseConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        int remaining = INITIAL_QUERY_LIMIT;
        Map<String, ErpKingdeeStockMove> moveMap = new LinkedHashMap<>();
        while (remaining > 0) {
            int pageLimit = Math.min(PAGE_LIMIT, remaining);
            JsonNode rows = executeBillQuery(properties, cookieHeader, startRow, pageLimit);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "STK_TransferDirect response is not an array");
            }
            if (rows.isEmpty()) {
                break;
            }
            addRowsToMoves(rows, moveMap, false);
            if (rows.size() < pageLimit) {
                break;
            }
            startRow += rows.size();
            remaining -= rows.size();
        }
        return new ArrayList<>(moveMap.values());
    }

    @Override
    public List<ErpKingdeeStockMove> fetchStockMovesModifiedBetween(ErpKingdeeProperties properties,
                                                                    LocalDateTime windowStart,
                                                                    LocalDateTime windowEnd) {
        properties.validateBaseConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        Map<String, ErpKingdeeStockMove> moveMap = new LinkedHashMap<>();
        while (true) {
            JsonNode rows = executeIncrementalBillQuery(properties, cookieHeader, windowStart, windowEnd, startRow);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "STK_TransferDirect response is not an array");
            }
            if (rows.isEmpty()) {
                break;
            }
            addRowsToMoves(rows, moveMap, true);
            if (rows.size() < PAGE_LIMIT) {
                break;
            }
            startRow += rows.size();
        }
        return new ArrayList<>(moveMap.values());
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
        LocalDate startDate = LocalDate.now().minusDays(INITIAL_QUERY_DAYS);
        LocalDate nextDay = LocalDate.now().plusDays(1);
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", FORM_ID);
        query.put("FieldKeys", FIELD_KEYS);
        query.put("FilterString", "(FBillNo <> '') and (FDate >= '" + startDate + "' and FDate < '" + nextDay + "')");
        query.put("OrderString", "FModifyDate DESC");
        query.put("StartRow", startRow);
        query.put("Limit", limit);
        return postQuery(properties, cookieHeader, query, "STK_TransferDirect ExecuteBillQuery response");
    }

    private JsonNode executeIncrementalBillQuery(ErpKingdeeProperties properties,
                                                 String cookieHeader,
                                                 LocalDateTime windowStart,
                                                 LocalDateTime windowEnd,
                                                 int startRow) {
        ErpKingdeeIncrementalQuerySpec spec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId(FORM_ID)
                .fieldKeys(INCREMENTAL_FIELD_KEYS)
                .baseFilter("(FBillNo <> '')")
                .modifyTimeField(MODIFY_TIME_FIELD)
                .startRow(startRow)
                .limit(PAGE_LIMIT)
                .build();
        return postQuery(properties, cookieHeader, spec.toQuery(windowStart, windowEnd),
                "STK_TransferDirect incremental ExecuteBillQuery response");
    }

    private JsonNode postQuery(ErpKingdeeProperties properties,
                               String cookieHeader,
                               Map<String, Object> query,
                               String label) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(query));
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), QUERY_SERVICE),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        JsonNode responseJson = parseJson(response.getBody(), label);
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PURCHASE_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        return responseJson;
    }

    private void addRowsToMoves(JsonNode rows, Map<String, ErpKingdeeStockMove> moveMap, boolean incremental) {
        for (JsonNode row : rows) {
            int expectedFieldCount = incremental ? INCREMENTAL_FIELD_COUNT : FIELD_COUNT;
            if (!row.isArray() || row.size() < expectedFieldCount) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "STK_TransferDirect row field count mismatch");
            }
            String fid = requiredText(row, 0, "FID");
            ErpKingdeeStockMove move = moveMap.computeIfAbsent(fid, key -> buildMove(row));
            if (incremental && move.getSourceModifyTime() == null) {
                move.setSourceModifyTime(parseDateTime(requiredText(row, 20, MODIFY_TIME_FIELD), MODIFY_TIME_FIELD));
            }
            move.getLines().add(buildLine(row));
        }
    }

    private ErpKingdeeStockMove buildMove(JsonNode row) {
        ErpKingdeeStockMove move = new ErpKingdeeStockMove();
        move.setFid(requiredText(row, 0, "FID"));
        move.setBillNo(requiredText(row, 1, "FBillNo"));
        move.setBillDate(parseDateTime(optionalText(row, 2), "FDate"));
        move.setDocumentStatus(optionalText(row, 3));
        move.setTransferDirect(optionalText(row, 4));
        move.setTransferBizType(optionalText(row, 5));
        move.setRemark(optionalText(row, 6));
        return move;
    }

    private ErpKingdeeStockMove.Line buildLine(JsonNode row) {
        ErpKingdeeStockMove.Line line = new ErpKingdeeStockMove.Line();
        line.setEntryId(requiredText(row, 7, "FBillEntry_FEntryID"));
        line.setMaterialNumber(requiredText(row, 8, "FMaterialId.FNumber"));
        line.setMaterialName(requiredText(row, 9, "FMaterialId.FName"));
        line.setMaterialSpecification(optionalText(row, 10));
        line.setUnitName(optionalText(row, 11));
        line.setQuantity(parseDecimal(requiredText(row, 12, "FQty"), "FQty"));
        line.setFromWarehouseNumber(optionalText(row, 13));
        line.setFromWarehouseName(optionalText(row, 14));
        line.setToWarehouseNumber(optionalText(row, 15));
        line.setToWarehouseName(optionalText(row, 16));
        line.setFromStockLocation(optionalText(row, 17));
        line.setToStockLocation(optionalText(row, 18));
        line.setLotNumber(optionalText(row, 19));
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

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
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
