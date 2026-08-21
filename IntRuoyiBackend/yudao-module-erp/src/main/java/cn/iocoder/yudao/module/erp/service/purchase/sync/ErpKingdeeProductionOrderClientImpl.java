package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ErpKingdeeProductionOrderClientImpl implements ErpKingdeeProductionOrderClient {

    private static final String AUTH_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String VIEW_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";
    private static final String SAVE_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc";
    private static final String SUBMIT_SERVICE =
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Submit.common.kdsvc";
    private static final String FIELD_KEYS = String.join(",",
            "FID",
            "FBillNo",
            "FDocumentStatus",
            "FDate",
            "FMaterialId.FNumber",
            "FMaterialId.FName",
            "FMaterialId.FSpecification",
            "FQty",
            "FPlanStartDate",
            "FPlanFinishDate",
            "FSrcBillNo",
            "FStatus",
            "FUnitId.FNumber",
            "FUnitId.FName",
            "FLot.FNumber",
            "FWorkShopID.FName",
            "FBomId.FNumber");
    private static final String BILL_LOOKUP_FIELD_KEYS = String.join(",",
            "FID",
            "FBillNo",
            "FDocumentStatus",
            "FDate",
            "FMaterialId.FNumber",
            "FMaterialId.FName",
            "FMaterialId.FSpecification",
            "FQty",
            "FPlanStartDate",
            "FPlanFinishDate",
            "FSrcBillNo",
            "FStatus");
    private static final String INCREMENTAL_FIELD_KEYS = FIELD_KEYS + ",FModifyDate";
    private static final int INDEX_FID = 0;
    private static final int INDEX_BILL_NO = 1;
    private static final int INDEX_DOCUMENT_STATUS = 2;
    private static final int INDEX_BILL_DATE = 3;
    private static final int INDEX_MATERIAL_NUMBER = 4;
    private static final int INDEX_MATERIAL_NAME = 5;
    private static final int INDEX_MATERIAL_SPECIFICATION = 6;
    private static final int INDEX_QUANTITY = 7;
    private static final int INDEX_PLAN_START_DATE = 8;
    private static final int INDEX_PLAN_FINISH_DATE = 9;
    private static final int INDEX_SOURCE_BILL_NO = 10;
    private static final int INDEX_STATUS = 11;
    private static final int INDEX_UNIT_CODE = 12;
    private static final int INDEX_UNIT_NAME = 13;
    private static final int INDEX_BATCH_NUMBER = 14;
    private static final int INDEX_WORKSHOP_NAME = 15;
    private static final int INDEX_BOM_VERSION = 16;
    private static final int INDEX_PICK_MODE = 17;
    private static final int INDEX_AUXILIARY_CODE = 18;
    private static final int INDEX_BUSINESS_STATUS = 19;
    private static final int INDEX_DRAWING_NUMBER = 20;
    private static final int INDEX_SCHEDULE_STATUS = 21;
    private static final int INDEX_SOURCE_MODIFY_TIME = 22;
    private static final int MIN_REQUIRED_FIELD_COUNT = 12;
    private static final int PAGE_LIMIT = 1000;
    private static final int BILL_NO_QUERY_BATCH_SIZE = 50;
    private static final DateTimeFormatter KINGDEE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> HEADER_TEMPLATE_FIELDS = List.of(
            "FBillType", "FBillTypeID", "FMoBillType", "FPrdOrgId", "FStockOrgId",
            "FWorkShopID", "FWorkshopID", "FOwnerTypeId", "FOwnerId", "FBusinessType",
            "FOrgId", "FUseOrgId");
    private static final List<String> ENTRY_TEMPLATE_FIELDS = List.of(
            "FStockUnitID", "FBaseUnitId", "FBaseUnitID", "FBomId", "FRoutingId",
            "FWorkShopID", "FWorkshopID", "FOwnerTypeId", "FOwnerId", "FLot",
            "FAuxPropId");
    private static final List<String> ENTRY_KEYS = List.of("FTreeEntity", "FEntity", "FMoEntry");

    @Qualifier("erpKingdeeRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public List<ErpKingdeeProductionOrder> fetchProductionOrders(ErpKingdeeProperties properties) {
        return fetchProductionOrders(properties, null, null);
    }

    @Override
    public List<ErpKingdeeProductionOrder> fetchProductionOrdersByBillDateRange(ErpKingdeeProperties properties,
                                                                            LocalDate fromDate,
                                                                            LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_MO sync date range is invalid");
        }
        return fetchProductionOrders(properties, fromDate, toDate);
    }

    @Override
    public List<ErpKingdeeProductionOrder> fetchProductionOrdersModifiedBetween(ErpKingdeeProperties properties,
                                                                                LocalDateTime windowStart,
                                                                                LocalDateTime windowEnd) {
        properties.validateProductionOrderSyncConfig();
        String cookieHeader = login(properties);
        ErpKingdeeIncrementalQuerySpec querySpec = ErpKingdeeIncrementalQuerySpec.builder()
                .formId(ErpKingdeeProductionOrder.FORM_ID)
                .fieldKeys(INCREMENTAL_FIELD_KEYS)
                .baseFilter(buildFilterString(null, null))
                .startRow(0)
                .limit(properties.getProductionOrder().getQueryLimit())
                .build();
        JsonNode rows = executeBillQuery(properties, cookieHeader, querySpec.toQuery(windowStart, windowEnd));
        if (!rows.isArray()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_MO response is not an array");
        }
        List<ErpKingdeeProductionOrder> orders = new ArrayList<>(rows.size());
        for (JsonNode row : rows) {
            orders.add(buildOrder(row));
        }
        return orders;
    }

    @Override
    public List<ErpKingdeeProductionOrder> fetchProductionOrdersByBillNos(ErpKingdeeProperties properties,
                                                                          Collection<String> billNos) {
        properties.validateProductionOrderSyncConfig();
        List<String> normalizedBillNos = normalizeBillNos(billNos);
        if (normalizedBillNos.isEmpty()) {
            return List.of();
        }
        String cookieHeader = login(properties);
        List<ErpKingdeeProductionOrder> orders = new ArrayList<>();
        for (int index = 0; index < normalizedBillNos.size(); index += BILL_NO_QUERY_BATCH_SIZE) {
            List<String> batch = normalizedBillNos.subList(index,
                    Math.min(index + BILL_NO_QUERY_BATCH_SIZE, normalizedBillNos.size()));
            JsonNode rows = executeBillQuery(properties, cookieHeader, buildBillNosFilterString(batch), 0, PAGE_LIMIT);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_MO response is not an array");
            }
            if (rows.size() >= PAGE_LIMIT) {
                throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID,
                        "PRD_MO billNo status query returned too many rows");
            }
            for (JsonNode row : rows) {
                orders.add(buildOrder(row));
            }
        }
        return orders;
    }

    @Override
    public ErpKingdeeProductionOrder getProductionOrderByBillNo(ErpKingdeeProperties properties, String billNo) {
        properties.validateProductionOrderSyncConfig();
        requireNoSingleQuote(requireNotBlank(billNo, "billNo"), "billNo");
        String cookieHeader = login(properties);
        JsonNode rows = executeBillQuery(properties, cookieHeader,
                buildBillNoFilterString(billNo), 0, 2, BILL_LOOKUP_FIELD_KEYS);
        if (!rows.isArray()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_MO response is not an array");
        }
        if (rows.isEmpty()) {
            return null;
        }
        return buildOrder(rows.get(0));
    }

    @Override
    public ErpKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(
            ErpKingdeeProperties properties, ErpKingdeeProductionOrderCreateRequest request) {
        properties.validateProductionOrderCreateConfig();
        validateCreateRequest(request);

        String cookieHeader = login(properties);
        JsonNode duplicateRows = executeBillQuery(properties, cookieHeader,
                buildBillNoFilterString(request.getBillNo()), 0, 1, "FBillNo");
        if (!duplicateRows.isArray()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_MO duplicate response is not an array");
        }
        if (!duplicateRows.isEmpty()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL,
                    "PRD_MO billNo already exists: " + request.getBillNo());
        }

        JsonNode templateModel = viewTemplate(properties, cookieHeader, request.getTemplateBillNo());
        JsonNode saveResponse = postDynamicFormData(properties, cookieHeader, SAVE_SERVICE,
                buildSavePayload(templateModel, request), "PRD_MO Save response");
        ErpKingdeeProductionOrderCreateResult savedResult = parseSaveResult(saveResponse, request.getBillNo());
        try {
            submitProductionOrder(properties, cookieHeader, savedResult);
        } catch (RuntimeException ex) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL,
                    "PRD_MO Submit failed after Save, billNo=" + savedResult.getErpBillNo()
                            + ", fid=" + savedResult.getErpFid() + ": " + ex.getMessage());
        }
        savedResult.setSubmitted(Boolean.TRUE);
        return savedResult;
    }

    private List<ErpKingdeeProductionOrder> fetchProductionOrders(ErpKingdeeProperties properties,
                                                                  LocalDate fromDate,
                                                                  LocalDate toDate) {
        properties.validateProductionOrderSyncConfig();
        String cookieHeader = login(properties);
        int startRow = 0;
        int remaining = properties.getProductionOrder().getQueryLimit();
        List<ErpKingdeeProductionOrder> orders = new ArrayList<>();
        while (remaining > 0) {
            int pageLimit = Math.min(PAGE_LIMIT, remaining);
            JsonNode rows = executeBillQuery(properties, cookieHeader,
                    buildFilterString(fromDate, toDate), startRow, pageLimit);
            if (!rows.isArray()) {
                throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_MO response is not an array");
            }
            if (rows.isEmpty()) {
                break;
            }
            for (JsonNode row : rows) {
                orders.add(buildOrder(row));
            }
            if (rows.size() < pageLimit) {
                break;
            }
            startRow += rows.size();
            remaining -= rows.size();
        }
        return orders;
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
        return cookies.stream()
                .map(cookie -> StrUtil.subBefore(cookie, ";", false))
                .collect(Collectors.joining("; "));
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties,
                                      String cookieHeader,
                                      String filterString,
                                      int startRow,
                                      int limit) {
        return executeBillQuery(properties, cookieHeader, filterString, startRow, limit, FIELD_KEYS);
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties,
                                      String cookieHeader,
                                      String filterString,
                                      int startRow,
                                      int limit,
                                      String fieldKeys) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", ErpKingdeeProductionOrder.FORM_ID);
        query.put("FieldKeys", fieldKeys);
        query.put("FilterString", filterString);
        query.put("OrderString", "FID DESC");
        query.put("StartRow", startRow);
        query.put("Limit", limit);

        JsonNode responseJson = postJsonData(properties, cookieHeader, QUERY_SERVICE,
                query, "PRD_MO ExecuteBillQuery response");
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        throwIfQueryReturnedEmbeddedError(responseJson);
        return responseJson;
    }

    private JsonNode executeBillQuery(ErpKingdeeProperties properties,
                                      String cookieHeader,
                                      Map<String, Object> query) {
        JsonNode responseJson = postJsonData(properties, cookieHeader, QUERY_SERVICE,
                query, "PRD_MO ExecuteBillQuery response");
        if (responseJson.isObject()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL, responseJson.toString());
        }
        throwIfQueryReturnedEmbeddedError(responseJson);
        return responseJson;
    }

    private JsonNode viewTemplate(ErpKingdeeProperties properties, String cookieHeader, String templateBillNo) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("Number", templateBillNo);
        view.put("Id", "");
        view.put("CreateOrgId", 0);

        JsonNode response = postDynamicFormData(properties, cookieHeader, VIEW_SERVICE,
                view, "PRD_MO View response");
        JsonNode status = responseStatus(response, "PRD_MO View");
        if (!status.path("IsSuccess").asBoolean(false)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL,
                    "PRD_MO View template failed: " + responseErrors(status));
        }
        JsonNode model = response.path("Result").path("Result");
        if (!model.isObject()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID,
                    "PRD_MO View response missing Result.Result");
        }
        return model;
    }

    private Map<String, Object> buildSavePayload(JsonNode templateModel,
                                                 ErpKingdeeProductionOrderCreateRequest request) {
        Map<String, Object> model = new LinkedHashMap<>();
        copyFields(templateModel, model, HEADER_TEMPLATE_FIELDS);
        model.put("FBillNo", request.getBillNo());
        model.put("FDate", formatDateTime(request.getPlannedStartDate()));

        String entryKey = resolveEntryKey(templateModel);
        Map<String, Object> entry = buildEntry(templateModel.path(entryKey), request);
        model.put(entryKey, List.of(entry));

        Map<String, Object> save = new LinkedHashMap<>();
        save.put("NeedUpDateFields", new ArrayList<>());
        save.put("NeedReturnFields", List.of("FID", "FBillNo"));
        save.put("IsDeleteEntry", true);
        save.put("IsVerifyBaseDataField", true);
        save.put("Model", model);
        return save;
    }

    private Map<String, Object> buildEntry(JsonNode templateEntries,
                                           ErpKingdeeProductionOrderCreateRequest request) {
        Map<String, Object> entry = new LinkedHashMap<>();
        JsonNode templateEntry = templateEntries.isArray() && !templateEntries.isEmpty()
                ? templateEntries.get(0) : null;
        if (templateEntry != null && templateEntry.isObject()) {
            copyFields(templateEntry, entry, ENTRY_TEMPLATE_FIELDS);
        }
        entry.put("FMaterialId", numberRef(request.getMaterialNumber()));
        entry.put("FUnitId", numberRef(request.getUnitNumber()));
        entry.put("FQty", request.getQuantity());
        entry.put("FPlanStartDate", formatDateTime(request.getPlannedStartDate()));
        entry.put("FPlanFinishDate", formatDateTime(request.getPlannedFinishDate()));
        if (StrUtil.isNotBlank(request.getSourceBillNo())) {
            entry.put("FSrcBillNo", request.getSourceBillNo());
        }
        if (StrUtil.isNotBlank(request.getBatchNumber())) {
            entry.put("FLot", numberRef(request.getBatchNumber()));
        }
        return entry;
    }

    private void submitProductionOrder(ErpKingdeeProperties properties,
                                       String cookieHeader,
                                       ErpKingdeeProductionOrderCreateResult savedResult) {
        Map<String, Object> submit = new LinkedHashMap<>();
        submit.put("Numbers", List.of(savedResult.getErpBillNo()));
        submit.put("Ids", savedResult.getErpFid());

        JsonNode response = postDynamicFormData(properties, cookieHeader, SUBMIT_SERVICE,
                submit, "PRD_MO Submit response");
        JsonNode status = responseStatus(response, "PRD_MO Submit");
        if (!status.path("IsSuccess").asBoolean(false)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL, responseErrors(status));
        }
    }

    private ErpKingdeeProductionOrderCreateResult parseSaveResult(JsonNode response, String requestBillNo) {
        JsonNode status = responseStatus(response, "PRD_MO Save");
        if (!status.path("IsSuccess").asBoolean(false)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL,
                    "PRD_MO Save failed: " + responseErrors(status));
        }
        JsonNode successEntities = status.path("SuccessEntitys");
        if (!successEntities.isArray() || successEntities.isEmpty()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID,
                    "PRD_MO Save response missing SuccessEntitys");
        }
        JsonNode successEntity = successEntities.get(0);
        String fid = optionalText(successEntity, "Id");
        String number = StrUtil.blankToDefault(optionalText(successEntity, "Number"), requestBillNo);
        if (StrUtil.isBlank(fid) || StrUtil.isBlank(number)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID,
                    "PRD_MO Save response missing FID or FBillNo");
        }
        return ErpKingdeeProductionOrderCreateResult.builder()
                .erpFid(fid)
                .erpBillNo(number)
                .saved(Boolean.TRUE)
                .submitted(Boolean.FALSE)
                .build();
    }

    private JsonNode postJsonData(ErpKingdeeProperties properties,
                                  String cookieHeader,
                                  String serviceName,
                                  Map<String, Object> payload,
                                  String label) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", JsonUtils.toJsonString(payload));
        return postFormData(properties, cookieHeader, serviceName, body, label);
    }

    private JsonNode postDynamicFormData(ErpKingdeeProperties properties,
                                         String cookieHeader,
                                         String serviceName,
                                         Map<String, Object> payload,
                                         String label) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("formid", ErpKingdeeProductionOrder.FORM_ID);
        body.add("data", JsonUtils.toJsonString(payload));
        return postFormData(properties, cookieHeader, serviceName, body, label);
    }

    private JsonNode postFormData(ErpKingdeeProperties properties,
                                  String cookieHeader,
                                  String serviceName,
                                  MultiValueMap<String, String> body,
                                  String label) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                buildServiceUrl(properties.getBaseUrl(), serviceName),
                new HttpEntity<>(body, buildFormHeaders(cookieHeader)),
                String.class);
        return parseJson(response.getBody(), label);
    }

    private String buildFilterString(LocalDate fromDate, LocalDate toDate) {
        List<String> filters = new ArrayList<>();
        filters.add("(FBillNo <> '')");
        filters.add("(FMaterialId.FNumber <> '')");
        filters.add("(FDocumentStatus <> 'Z')");
        if (fromDate != null) {
            filters.add("(FDate >= '" + fromDate + "')");
        }
        if (toDate != null) {
            filters.add("(FDate <= '" + toDate + "')");
        }
        return String.join(" and ", filters);
    }

    private String buildBillNoFilterString(String billNo) {
        requireNoSingleQuote(billNo, "billNo");
        return "(FBillNo = '" + billNo + "') and (FDocumentStatus <> 'Z')";
    }

    private List<String> normalizeBillNos(Collection<String> billNos) {
        if (billNos == null || billNos.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String billNo : billNos) {
            String normalizedBillNo = StrUtil.trimToNull(billNo);
            if (normalizedBillNo == null) {
                continue;
            }
            requireNoSingleQuote(normalizedBillNo, "billNo");
            normalized.add(normalizedBillNo);
        }
        return new ArrayList<>(normalized);
    }

    private String buildBillNosFilterString(List<String> billNos) {
        if (billNos.isEmpty()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "billNos is empty");
        }
        return billNos.stream()
                .map(billNo -> "(FBillNo = '" + billNo + "')")
                .collect(Collectors.joining(" or "));
    }

    private ErpKingdeeProductionOrder buildOrder(JsonNode row) {
        if (!row.isArray() || row.size() < MIN_REQUIRED_FIELD_COUNT) {
            log.warn("PRD_MO row field count mismatch, rowType={}, rowSize={}, row={}",
                    row == null ? "null" : row.getNodeType(),
                    row == null ? -1 : row.size(),
                    row);
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "PRD_MO row field count mismatch");
        }
        ErpKingdeeProductionOrder order = new ErpKingdeeProductionOrder();
        order.setFid(requiredText(row, INDEX_FID, "FID"));
        order.setBillNo(requiredText(row, INDEX_BILL_NO, "FBillNo"));
        order.setDocumentStatus(requiredText(row, INDEX_DOCUMENT_STATUS, "FDocumentStatus"));
        order.setBillDate(parseDateTime(requiredText(row, INDEX_BILL_DATE, "FDate"), "FDate"));
        order.setMaterialNumber(requiredText(row, INDEX_MATERIAL_NUMBER, "FMaterialId.FNumber"));
        order.setMaterialName(requiredText(row, INDEX_MATERIAL_NAME, "FMaterialId.FName"));
        order.setMaterialSpecification(optionalText(row, INDEX_MATERIAL_SPECIFICATION));
        order.setQuantity(parseDecimal(requiredText(row, INDEX_QUANTITY, "FQty"), "FQty"));
        order.setPlannedStartDate(parseDateTime(optionalText(row, INDEX_PLAN_START_DATE), "FPlanStartDate"));
        order.setPlannedEndDate(parseDateTime(optionalText(row, INDEX_PLAN_FINISH_DATE), "FPlanFinishDate"));
        order.setSourceBillNo(optionalText(row, INDEX_SOURCE_BILL_NO));
        order.setStatus(requiredText(row, INDEX_STATUS, "FStatus"));
        order.setUnitCode(optionalText(row, INDEX_UNIT_CODE));
        order.setUnitName(optionalText(row, INDEX_UNIT_NAME));
        order.setBatchNumber(optionalText(row, INDEX_BATCH_NUMBER));
        order.setWorkshopName(optionalText(row, INDEX_WORKSHOP_NAME));
        order.setBomVersion(optionalText(row, INDEX_BOM_VERSION));
        order.setPickMode(optionalText(row, INDEX_PICK_MODE));
        order.setAuxiliaryCode(optionalText(row, INDEX_AUXILIARY_CODE));
        order.setBusinessStatus(optionalText(row, INDEX_BUSINESS_STATUS));
        order.setDrawingNumber(optionalText(row, INDEX_DRAWING_NUMBER));
        order.setScheduleStatus(optionalText(row, INDEX_SCHEDULE_STATUS));
        if (row.size() > INDEX_SOURCE_MODIFY_TIME) {
            order.setSourceModifyTime(parseDateTime(optionalText(row, INDEX_SOURCE_MODIFY_TIME), "FModifyDate"));
        }
        return order;
    }

    private void throwIfQueryReturnedEmbeddedError(JsonNode responseJson) {
        JsonNode errorCarrier = findEmbeddedErrorCarrier(responseJson);
        if (errorCarrier == null) {
            return;
        }
        JsonNode status = errorCarrier.path("Result").path("ResponseStatus");
        if (!status.isObject() || status.path("IsSuccess").asBoolean(true)) {
            return;
        }
        throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_FAIL,
                "PRD_MO ExecuteBillQuery failed: " + responseErrors(status));
    }

    private JsonNode findEmbeddedErrorCarrier(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            return node;
        }
        if (!node.isArray() || node.isEmpty()) {
            return null;
        }
        return findEmbeddedErrorCarrier(node.get(0));
    }

    private void validateCreateRequest(ErpKingdeeProductionOrderCreateRequest request) {
        if (request == null) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "create request is null");
        }
        requireNoSingleQuote(requireNotBlank(request.getBillNo(), "billNo"), "billNo");
        requireNoSingleQuote(requireNotBlank(request.getTemplateBillNo(), "templateBillNo"), "templateBillNo");
        requireNoSingleQuote(requireNotBlank(request.getMaterialNumber(), "materialNumber"), "materialNumber");
        requireNoSingleQuote(requireNotBlank(request.getUnitNumber(), "unitNumber"), "unitNumber");
        if (StrUtil.isNotBlank(request.getSourceBillNo())) {
            requireNoSingleQuote(request.getSourceBillNo(), "sourceBillNo");
        }
        if (StrUtil.isNotBlank(request.getBatchNumber())) {
            requireNoSingleQuote(request.getBatchNumber(), "batchNumber");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "quantity must be positive");
        }
        if (request.getPlannedStartDate() == null) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "plannedStartDate is null");
        }
        if (request.getPlannedFinishDate() == null) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, "plannedFinishDate is null");
        }
    }

    private void copyFields(JsonNode source, Map<String, Object> target, List<String> fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = source.get(fieldName);
            if (value != null && !value.isNull()) {
                target.put(fieldName, toPlainObject(value));
            }
        }
    }

    private Object toPlainObject(JsonNode value) {
        return JsonUtils.parseObject(JsonUtils.toJsonString(value), Object.class);
    }

    private Map<String, Object> numberRef(String number) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("FNumber", number);
        return value;
    }

    private String resolveEntryKey(JsonNode templateModel) {
        for (String entryKey : ENTRY_KEYS) {
            JsonNode entries = templateModel.get(entryKey);
            if (entries != null && entries.isArray()) {
                return entryKey;
            }
        }
        return "FTreeEntity";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(KINGDEE_DATE_TIME_FORMAT);
    }

    private JsonNode responseStatus(JsonNode response, String label) {
        JsonNode status = response.path("Result").path("ResponseStatus");
        if (!status.isObject()) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, label + " missing ResponseStatus");
        }
        return status;
    }

    private String responseErrors(JsonNode status) {
        JsonNode errors = status.path("Errors");
        if (errors.isArray() && !errors.isEmpty()) {
            List<String> messages = new ArrayList<>();
            for (JsonNode error : errors) {
                String message = optionalText(error, "Message");
                if (StrUtil.isNotBlank(message)) {
                    messages.add(message);
                }
            }
            if (!messages.isEmpty()) {
                return String.join("; ", messages);
            }
        }
        return status.toString();
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
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, label + " is not valid JSON");
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

    private String requireNotBlank(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, fieldName + " is blank");
        }
        return value;
    }

    private void requireNoSingleQuote(String value, String fieldName) {
        if (value.contains("'")) {
            throw exception(KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID, fieldName + " contains single quote");
        }
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

    private String optionalText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private BigDecimal parseDecimal(String text, String fieldName) {
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

}
