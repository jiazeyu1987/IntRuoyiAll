package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/** 领料/补料只有用料清单分录内码；行号必须从正式用料清单读取。 */
@RequiredArgsConstructor
public class ErpKingdeeProductionMaterialLineResolver {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ErpKingdeeProductionMaterialLineResolver.class);
    private final RestTemplate restTemplate;

    public Map<String, Integer> resolve(ErpKingdeeProperties properties, String cookie, Set<String> entryIds) {
        Map<String, Integer> result = new HashMap<>();
        List<String> ids = new ArrayList<>(entryIds);
        for (String id : ids) {
            if (!id.matches("[1-9][0-9]*")) { throw new IllegalStateException("invalid ERP material list entry ID"); }
        }
        for (int begin = 0; begin < ids.size(); begin += 200) {
            List<String> batch = ids.subList(begin, Math.min(begin + 200, ids.size()));
            int offset = 0;
            while (true) {
                Map<String, Object> query = new LinkedHashMap<>();
                query.put("FormId", "PRD_PPBOM");
                query.put("FieldKeys", "FEntity_FEntryID,FEntity_FSeq");
                query.put("FilterString", "FEntity_FEntryID in (" + String.join(",", batch) + ")");
                query.put("OrderString", "FID ASC,FEntity_FEntryID ASC");
                query.put("StartRow", offset);
                query.put("Limit", 1000);
                var body = new LinkedMultiValueMap<String, String>();
                body.add("data", JsonUtils.toJsonString(query));
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/x-www-form-urlencoded;charset=UTF-8"));
                headers.set(HttpHeaders.COOKIE, cookie);
                String base = properties.getBaseUrl().trim().replaceAll("/+$", "");
                if (!base.toLowerCase(Locale.ROOT).endsWith("/k3cloud")) { base += "/K3Cloud"; }
                JsonNode rows;
                for (int attempt = 1; ; attempt++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new java.util.concurrent.CancellationException("同步任务已停止");
                    }
                    var response = restTemplate.postForEntity(base
                            + "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc",
                            new HttpEntity<>(body, headers), String.class);
                    rows = JsonUtils.parseTree(response.getBody());
                    if (rows == null) { throw new IllegalStateException("ERP material list query returned no JSON"); }
                    JsonNode failure = queryFailure(rows);
                    if (failure == null) break;
                    if (!isDeadlockVictim(failure) || attempt >= 3) {
                        throw new IllegalStateException("ERP material list query failed: " + failure);
                    }
                    log.warn("PRD_PPBOM line query deadlock victim: startRow={}, attempt={}/3; retrying same query", offset, attempt);
                    waitBeforeRetry(attempt);
                }
                if (!rows.isArray()) { throw new IllegalStateException("ERP material list query response is not an array"); }
                for (var row : rows) {
                    if (!row.isArray() || row.size() != 2 || !batch.contains(row.get(0).asText())
                            || !row.get(1).asText().matches("[1-9][0-9]*")) {
                        throw new IllegalStateException("invalid ERP material list line identity or sequence: entryId="
                                + row.path(0).asText("missing") + ", sequence=" + row.path(1).asText("missing")
                                + ", requested=" + batch.contains(row.path(0).asText()) + ", fieldCount=" + row.size());
                    }
                    if (result.putIfAbsent(row.get(0).asText(), Integer.valueOf(row.get(1).asText())) != null) {
                        throw new IllegalStateException("duplicate ERP material list entry ID");
                    }
                }
                if (rows.size() < 1000) { break; }
                offset += rows.size();
            }
        }
        if (!result.keySet().equals(entryIds)) { throw new IllegalStateException("ERP material list referenced entries are missing"); }
        return result;
    }

    static JsonNode queryFailure(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode failure = queryFailure(child);
                if (failure != null) return failure;
            }
            return null;
        }
        if (!node.isObject()) return null;
        JsonNode status = node.path("Result").path("ResponseStatus");
        if (status.isMissingNode()) status = node.path("ResponseStatus");
        if (status.isMissingNode()) status = node;
        return status.path("IsSuccess").isBoolean() && !status.path("IsSuccess").booleanValue() ? status : null;
    }

    static boolean isDeadlockVictim(JsonNode failure) {
        String message = failure.toString().toLowerCase(Locale.ROOT);
        return (message.contains("死锁") && message.contains("牺牲品"))
                || message.contains("deadlock victim") || (message.contains("deadlocked") && message.contains("victim"));
    }

    void waitBeforeRetry(int failedAttempt) {
        try {
            Thread.sleep(failedAttempt == 1 ? 5_000L : 15_000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            var cancelled = new java.util.concurrent.CancellationException("用料清单行号查询重试被中断");
            cancelled.initCause(interrupted);
            throw cancelled;
        }
    }
}
