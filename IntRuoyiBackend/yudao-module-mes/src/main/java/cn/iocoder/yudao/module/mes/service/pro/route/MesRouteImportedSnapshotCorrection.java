package cn.iocoder.yudao.module.mes.service.pro.route;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;

/** Explicit, pinned correction planner. It never connects to or writes a database. */
public final class MesRouteImportedSnapshotCorrection {
    public static final String CONTRACT = "mes-route-import-index-correction/v1";
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final MesProRouteSnapshotCanonicalizer CANONICAL = new MesProRouteSnapshotCanonicalizer();
    private record Pin(long routeId, String hash, int count) {
        Pin {require(routeId>0 && hash.matches("[a-f0-9]{64}") && count>0,"invalid approved correction pin");}
    }
    private static final Map<Long, Pin> APPROVED = Map.of(
        2123251871104147457L, new Pin(2123251871099953154L,"bc82f081e7b3a4562b27e551ed74b50c97606f79d215880c9f94c79ac72a21c9",15),
        2123251871229976578L, new Pin(2123251871225782338L,"e19fc94d31f8da3a72773162a2d767285c9da604837c53438d2967ebeb68baaa",15),
        2123251871292891138L, new Pin(2123251871288696898L,"d585f4e8b4c36bd5c96b2fb1154070988f5117da3de1cecfd354b273b8518345",15),
        2123251871347417090L, new Pin(2123251871343222810L,"84444bbf0905fdafc776e5dfc264f11169a588c6aa5ecafcdd66c7aa8800f2a3",14),
        2123251871393554435L, new Pin(2123251871393554433L,"efb2b0018530e74ea98d7e55e11eb1b80ac936e7221fe89c6ac595dbf98f8608",15));

    static String correctScheduleIndexes(Long routeId, String originalJson, int expectedCount) {
        try {
            // Keep decimal precision and reject duplicate/trailing JSON through the production canonicalizer.
            CANONICAL.canonicalize(originalJson);
            ObjectMapper precise = JSON.copy().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
            ObjectNode root = (ObjectNode) precise.readTree(originalJson);
            Set<Long> nodes = new HashSet<>();
            for (JsonNode node : root.path("configSnapshots").path("flowGraph").path("nodes"))
                nodes.add(node.path("routeProcessId").longValue());
            JsonNode configs = root.path("configSnapshots").path("scheduleConfigs");
            require(configs.isObject() && configs.size() == expectedCount, "unexpected schedule configuration count");
            ObjectNode corrected = precise.createObjectNode();
            Iterator<Map.Entry<String,JsonNode>> fields = configs.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                JsonNode id = entry.getValue().path("routeProcessId");
                require(id.isIntegralNumber() && id.canConvertToLong() && id.longValue()>0 && nodes.contains(id.longValue()),
                    "schedule process is outside the frozen graph");
                String key = id.asText();
                require(entry.getKey().matches("[1-9][0-9]*") && !entry.getKey().equals(key)
                    && !nodes.contains(Long.parseLong(entry.getKey())) && !corrected.has(key), "unexpected or ambiguous schedule index");
                corrected.set(key,entry.getValue());
            }
            ((ObjectNode) root.path("configSnapshots")).set("scheduleConfigs",corrected);
            String result = precise.writeValueAsString(root);
            require(CANONICAL.validate(routeId,result).ready(), "corrected snapshot fails semantic validation");
            return result;
        } catch (IllegalArgumentException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalArgumentException("unsupported correction input");
        }
    }

    public static Map<String,Object> plan(JsonNode rows) {
        require(rows.isArray() && rows.size()==APPROVED.size(), "exact approved five-row scope required");
        List<Map<String,Object>> corrections = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (JsonNode row : rows) {
            long id=row.path("id").asLong(); Pin pin=APPROVED.get(id);
            require(pin!=null && seen.add(id) && row.path("tenant_id").asLong()==163
                && row.path("route_id").asLong()==pin.routeId(), "unapproved correction identity");
            String original=row.path("route_snapshot_json").asText();
            require(pin.hash().equals(row.path("route_snapshot_sha256").asText()) && pin.hash().equals(CANONICAL.sha256(original))
                && MesProRouteSnapshotCanonicalizer.FORMAT_VERSION.equals(row.path("route_snapshot_format_version").asText()),
                "original frozen identity differs");
            require("ACTIVE".equals(row.path("lifecycle_status").asText()) && row.path("active").asInt()==1
                && row.path("deleted").asInt()==0 && row.path("approval_process_instance_id").isNull()
                && row.path("source_route_version_id").isNull(), "original lifecycle or approval differs");
            String corrected=correctScheduleIndexes(pin.routeId(),original,pin.count());
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("id",id); result.put("tenantId",163); result.put("routeId",pin.routeId());
            result.put("originalJson",original); result.put("originalSha256",pin.hash());
            result.put("originalRawSha256",rawSha(original)); result.put("correctedJson",corrected);
            result.put("correctedSha256",CANONICAL.sha256(corrected)); result.put("correctedRawSha256",rawSha(corrected));
            result.put("indexCount",pin.count()); corrections.add(result);
        }
        corrections.sort(Comparator.comparingLong(row -> ((Number)row.get("id")).longValue()));
        return Map.of("contract",CONTRACT,"corrections",corrections,"indexCount",74);
    }
    private static String rawSha(String json) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)));}
        catch (Exception failure) {throw new IllegalStateException("SHA-256 unavailable");}
    }
    private static void require(boolean allowed,String reason) {if(!allowed) throw new IllegalArgumentException(reason);}

    public static void main(String[] args) throws Exception {
        require(args.length==2 && args[0].startsWith("--input=") && args[1].startsWith("--output="), "explicit input/output required");
        Path input=Path.of(args[0].substring(8)), output=Path.of(args[1].substring(9));
        require(input.isAbsolute() && output.isAbsolute() && !Files.exists(output), "fresh absolute output required");
        JsonNode rows=JSON.readTree(Files.readString(input)).path("rows").path("mes_pro_route_version");
        Files.writeString(output,JSON.writeValueAsString(plan(rows)),StandardOpenOption.CREATE_NEW);
        System.out.println("CORRECTION_PLAN_READY: rows=5 indexes=74; no database changes");
    }
}
