package cn.iocoder.yudao.module.erp.service.production.sync;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/** 领料/补料只有用料清单分录内码；行号必须从正式用料清单读取。 */
@RequiredArgsConstructor
public class ErpKingdeeProductionMaterialLineResolver {
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
                var response = restTemplate.postForEntity(base
                        + "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc",
                        new HttpEntity<>(body, headers), String.class);
                var rows = JsonUtils.parseTree(response.getBody());
                if (!rows.isArray()) { throw new IllegalStateException("ERP material list query failed"); }
                for (var row : rows) {
                    if (!row.isArray() || row.size() != 2 || !batch.contains(row.get(0).asText())
                            || !row.get(1).asText().matches("[1-9][0-9]*")) {
                        throw new IllegalStateException("invalid ERP material list line identity or sequence");
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
}
