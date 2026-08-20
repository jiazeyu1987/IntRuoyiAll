package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

@Component
public class MesProRouteSnapshotCanonicalizer {

    public static final String FORMAT_VERSION = "MES_ROUTE_SNAPSHOT_CANONICAL_V1";

    private static final Comparator<String> UNICODE_CODE_POINT_ORDER = (left, right) -> {
        int[] leftPoints = left.codePoints().toArray();
        int[] rightPoints = right.codePoints().toArray();
        int length = Math.min(leftPoints.length, rightPoints.length);
        for (int index = 0; index < length; index++) {
            int compared = Integer.compare(leftPoints[index], rightPoints[index]);
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(leftPoints.length, rightPoints.length);
    };

    private final ObjectMapper mapper = JsonUtils.getObjectMapper().copy()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .disable(JsonParser.Feature.ALLOW_COMMENTS)
            .disable(JsonParser.Feature.ALLOW_YAML_COMMENTS)
            .disable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .disable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .disable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .disable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
            .disable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS)
            .disable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS)
            .disable(JsonParser.Feature.ALLOW_MISSING_VALUES)
            .disable(JsonParser.Feature.ALLOW_TRAILING_COMMA)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public String canonicalize(String routeSnapshotJson) {
        if (routeSnapshotJson == null || routeSnapshotJson.isBlank()) {
            throw new IllegalArgumentException("route snapshot json is required");
        }
        try {
            JsonNode root = mapper.readTree(routeSnapshotJson);
            if (root == null) {
                throw new IllegalArgumentException("route snapshot json is required");
            }
            return canonicalize(root);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("route snapshot json is invalid", ex);
        }
    }

    public String sha256(String routeSnapshotJson) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalize(routeSnapshotJson).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }

    public Validation validate(Long expectedRouteId, String routeSnapshotJson) {
        JsonNode root;
        try {
            root = mapper.readTree(routeSnapshotJson);
        } catch (Exception ex) {
            return new Validation(List.of(new Blocker("JSON_INVALID", safeMessage(ex))));
        }
        if (root == null || !root.isObject()) {
            return new Validation(List.of(new Blocker("JSON_INVALID", "root must be an object")));
        }
        List<Blocker> blockers = new ArrayList<>();
        JsonNode routeIdNode = root.get("routeId");
        if (routeIdNode == null || !routeIdNode.isIntegralNumber() || !routeIdNode.canConvertToLong()) {
            blockers.add(new Blocker("ROUTE_ID_MISSING", "routeId is required"));
        } else if (expectedRouteId != null && routeIdNode.longValue() != expectedRouteId) {
            blockers.add(new Blocker("ROUTE_ID_MISMATCH",
                    "expected=" + expectedRouteId + ", actual=" + routeIdNode.longValue()));
        }
        JsonNode nodes = root.path("configSnapshots").path("flowGraph").path("nodes");
        if (!nodes.isArray() || nodes.isEmpty()) {
            blockers.add(new Blocker("FLOW_NODES_MISSING", "configSnapshots.flowGraph.nodes is required"));
            return new Validation(List.copyOf(blockers));
        }
        Set<Long> identities = new HashSet<>();
        Set<Long> processIdentities = new HashSet<>();
        Map<Long, Long> processIdByRouteProcessId = new LinkedHashMap<>();
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode routeProcessIdNode = nodes.get(index).get("routeProcessId");
            if (routeProcessIdNode == null || !routeProcessIdNode.isIntegralNumber()
                    || !routeProcessIdNode.canConvertToLong()) {
                blockers.add(new Blocker("ROUTE_PROCESS_ID_MISSING", "nodeIndex=" + index));
                continue;
            }
            long routeProcessId = routeProcessIdNode.longValue();
            if (!identities.add(routeProcessId)) {
                blockers.add(new Blocker("ROUTE_PROCESS_ID_DUPLICATE", "routeProcessId=" + routeProcessId));
            }
            JsonNode processIdNode = nodes.get(index).get("processId");
            if (!isPositiveLong(processIdNode)) {
                blockers.add(new Blocker("PROCESS_ID_MISSING", "nodeIndex=" + index));
            } else {
                processIdentities.add(processIdNode.longValue());
                processIdByRouteProcessId.put(routeProcessId, processIdNode.longValue());
            }
        }
        validateRequiredRouteProcessReferences(root.path("configSnapshots").path("flowGraph"),
                identities, blockers);
        validateConfigRouteProcessReferences(root.path("configSnapshots"), identities, blockers);
        validateFormalProductIdentities(root.path("configSnapshots"), blockers);
        validateNestedFormalIdentities(root, identities, processIdentities,
                processIdByRouteProcessId, "$", blockers);
        return new Validation(List.copyOf(blockers));
    }

    public Validation validateCandidate(Long expectedRouteId, String routeSnapshotJson) {
        JsonNode root;
        try {
            root = mapper.readTree(routeSnapshotJson);
        } catch (Exception ex) {
            return new Validation(List.of(new Blocker("JSON_INVALID", safeMessage(ex))));
        }
        if (root == null || !root.isObject()) {
            return new Validation(List.of(new Blocker("JSON_INVALID", "root must be an object")));
        }
        List<Blocker> blockers = new ArrayList<>();
        JsonNode routeIdNode = root.get("routeId");
        if (!isPositiveLong(routeIdNode)) {
            blockers.add(new Blocker("ROUTE_ID_MISSING", "routeId is required"));
        } else if (expectedRouteId != null && routeIdNode.longValue() != expectedRouteId) {
            blockers.add(new Blocker("ROUTE_ID_MISMATCH",
                    "expected=" + expectedRouteId + ", actual=" + routeIdNode.longValue()));
        }
        JsonNode nodes = root.path("configSnapshots").path("flowGraph").path("nodes");
        if (!nodes.isArray() || nodes.isEmpty()) {
            blockers.add(new Blocker("FLOW_NODES_MISSING", "configSnapshots.flowGraph.nodes is required"));
            return new Validation(List.copyOf(blockers));
        }
        boolean allOfficial = true;
        Set<Long> officialIds = new HashSet<>();
        Set<Long> clientIds = new HashSet<>();
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            JsonNode routeProcessId = node.get("routeProcessId");
            if (isPositiveLong(routeProcessId)) {
                if (!officialIds.add(routeProcessId.longValue())) {
                    blockers.add(new Blocker("ROUTE_PROCESS_ID_DUPLICATE",
                            "routeProcessId=" + routeProcessId.longValue()));
                }
            } else {
                allOfficial = false;
                JsonNode clientRouteProcessId = node.get("clientRouteProcessId");
                if (!isNegativeLong(clientRouteProcessId)) {
                    blockers.add(new Blocker("CLIENT_ROUTE_PROCESS_ID_INVALID", "nodeIndex=" + index));
                } else if (!clientIds.add(clientRouteProcessId.longValue())) {
                    blockers.add(new Blocker("CLIENT_ROUTE_PROCESS_ID_DUPLICATE",
                            "clientRouteProcessId=" + clientRouteProcessId.longValue()));
                }
            }
            if (!isPositiveLong(node.get("processId"))) {
                blockers.add(new Blocker("PROCESS_ID_MISSING", "nodeIndex=" + index));
            }
        }
        if (allOfficial && blockers.isEmpty()) {
            return validate(expectedRouteId, routeSnapshotJson);
        }
        return new Validation(List.copyOf(blockers));
    }

    private void validateFormalProductIdentities(JsonNode configSnapshots, List<Blocker> blockers) {
        validateFormalCollection(configSnapshots.get("products"), "products",
                Set.of("itemId"), "PRODUCT_IDENTITY_INVALID", blockers);
        validateFormalCollection(configSnapshots.get("productBoms"), "productBoms",
                Set.of("processId", "productId", "itemId"), "PRODUCT_BOM_IDENTITY_INVALID", blockers);
    }

    private void validateFormalCollection(JsonNode collection, String name, Set<String> requiredIds,
                                          String reasonCode, List<Blocker> blockers) {
        if (collection == null || collection.isNull()) {
            return;
        }
        Iterable<JsonNode> values;
        if (collection.isArray()) {
            values = collection;
        } else if (collection.isObject()) {
            List<JsonNode> objectValues = new ArrayList<>();
            collection.elements().forEachRemaining(objectValues::add);
            values = objectValues;
        } else {
            blockers.add(new Blocker(reasonCode, "configSnapshots." + name + " must be an array or object"));
            return;
        }
        int index = 0;
        for (JsonNode value : values) {
            if (!value.isObject()) {
                blockers.add(new Blocker(reasonCode, "configSnapshots." + name + "[" + index + "]"));
            } else {
                for (String requiredId : requiredIds) {
                    if (!isPositiveLong(value.get(requiredId))) {
                        blockers.add(new Blocker(reasonCode,
                                "configSnapshots." + name + "[" + index + "]." + requiredId));
                    }
                }
            }
            index++;
        }
    }

    private void validateRequiredRouteProcessReferences(JsonNode flowGraph, Set<Long> routeProcessIds,
                                                        List<Blocker> blockers) {
        validateArrayReferencePair(flowGraph.get("edges"), "sourceRouteProcessId", "targetRouteProcessId",
                "configSnapshots.flowGraph.edges", routeProcessIds, blockers);
        validateArrayReference(flowGraph.get("boundaryEdges"), "routeProcessId",
                "configSnapshots.flowGraph.boundaryEdges", routeProcessIds, blockers);
        validateArrayReference(flowGraph.get("layouts"), "routeProcessId",
                "configSnapshots.flowGraph.layouts", routeProcessIds, blockers);
    }

    private void validateConfigRouteProcessReferences(JsonNode configSnapshots, Set<Long> routeProcessIds,
                                                       List<Blocker> blockers) {
        validateConfigCollection(configSnapshots.get("scheduleConfigs"), "scheduleConfigs",
                routeProcessIds, blockers);
        validateConfigCollection(configSnapshots.get("batchUseConfigs"), "batchUseConfigs",
                routeProcessIds, blockers);
        validateConfigCollection(configSnapshots.get("scheduleUseConfigs"), "scheduleUseConfigs",
                routeProcessIds, blockers);
    }

    private void validateConfigCollection(JsonNode configs, String name, Set<Long> routeProcessIds,
                                          List<Blocker> blockers) {
        if (configs == null || configs.isNull()) {
            return;
        }
        if (configs.isArray()) {
            validateArrayReference(configs, "routeProcessId", "configSnapshots." + name,
                    routeProcessIds, blockers);
            return;
        }
        if (!configs.isObject()) {
            blockers.add(new Blocker("ROUTE_PROCESS_REFERENCE_INVALID", "configSnapshots." + name));
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = configs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            Long keyedId;
            try {
                keyedId = Long.valueOf(entry.getKey());
            } catch (NumberFormatException ex) {
                blockers.add(new Blocker("ROUTE_PROCESS_KEY_INVALID", name + "." + entry.getKey()));
                continue;
            }
            if (!routeProcessIds.contains(keyedId)) {
                blockers.add(new Blocker("ROUTE_PROCESS_UNRESOLVED", name + "." + entry.getKey()));
            }
            JsonNode value = entry.getValue();
            if (!value.isObject()) {
                blockers.add(new Blocker("ROUTE_PROCESS_REFERENCE_INVALID", name + "." + entry.getKey()));
                continue;
            }
            JsonNode explicitId = value.get("routeProcessId");
            if (explicitId != null && (!isPositiveLong(explicitId) || explicitId.longValue() != keyedId)) {
                blockers.add(new Blocker("ROUTE_PROCESS_KEY_MISMATCH", name + "." + entry.getKey()));
            }
        }
    }

    private void validateArrayReferencePair(JsonNode array, String firstKey, String secondKey, String path,
                                            Set<Long> routeProcessIds, List<Blocker> blockers) {
        validateArrayReference(array, firstKey, path, routeProcessIds, blockers);
        validateArrayReference(array, secondKey, path, routeProcessIds, blockers);
    }

    private void validateArrayReference(JsonNode array, String key, String path,
                                        Set<Long> routeProcessIds, List<Blocker> blockers) {
        if (array == null || array.isNull()) {
            return;
        }
        if (!array.isArray()) {
            blockers.add(new Blocker("ROUTE_PROCESS_REFERENCE_INVALID", path));
            return;
        }
        for (int index = 0; index < array.size(); index++) {
            JsonNode value = array.get(index).get(key);
            if (!isPositiveLong(value)) {
                blockers.add(new Blocker("ROUTE_PROCESS_REFERENCE_MISSING",
                        path + "[" + index + "]." + key));
            } else if (!routeProcessIds.contains(value.longValue())) {
                blockers.add(new Blocker("ROUTE_PROCESS_UNRESOLVED",
                        path + "[" + index + "]." + key + "=" + value.longValue()));
            }
        }
    }

    private void validateNestedFormalIdentities(JsonNode node, Set<Long> routeProcessIds,
                                                Set<Long> processIds,
                                                Map<Long, Long> processIdByRouteProcessId, String path,
                                                List<Blocker> blockers) {
        if (node.isObject()) {
            JsonNode routeProcessIdNode = node.get("routeProcessId");
            JsonNode processIdNode = node.get("processId");
            if (isPositiveLong(routeProcessIdNode) && isPositiveLong(processIdNode)) {
                Long expectedProcessId = processIdByRouteProcessId.get(routeProcessIdNode.longValue());
                if (expectedProcessId != null && expectedProcessId.longValue() != processIdNode.longValue()) {
                    blockers.add(new Blocker("ROUTE_PROCESS_PROCESS_ID_MISMATCH",
                            path + ": routeProcessId=" + routeProcessIdNode.longValue()
                                    + ", expectedProcessId=" + expectedProcessId
                                    + ", actualProcessId=" + processIdNode.longValue()));
                }
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String childPath = path + "." + entry.getKey();
                if (isRouteProcessReferenceKey(entry.getKey())) {
                    if (!isPositiveLong(entry.getValue())) {
                        blockers.add(new Blocker("ROUTE_PROCESS_REFERENCE_INVALID", childPath));
                    } else if (!routeProcessIds.contains(entry.getValue().longValue())) {
                        blockers.add(new Blocker("ROUTE_PROCESS_UNRESOLVED",
                                childPath + "=" + entry.getValue().longValue()));
                    }
                } else if ("processId".equals(entry.getKey())) {
                    if (!isPositiveLong(entry.getValue())) {
                        blockers.add(new Blocker("PROCESS_ID_INVALID", childPath));
                    } else if (!processIds.contains(entry.getValue().longValue())) {
                        blockers.add(new Blocker("PROCESS_ID_UNRESOLVED",
                                childPath + "=" + entry.getValue().longValue()));
                    }
                }
                validateNestedFormalIdentities(entry.getValue(), routeProcessIds, processIds,
                        processIdByRouteProcessId, childPath, blockers);
            }
        } else if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                validateNestedFormalIdentities(node.get(index), routeProcessIds, processIds,
                        processIdByRouteProcessId,
                        path + "[" + index + "]", blockers);
            }
        }
    }

    private boolean isRouteProcessReferenceKey(String key) {
        return "routeProcessId".equals(key)
                || (key != null && key.endsWith("RouteProcessId")
                && !"clientRouteProcessId".equals(key));
    }

    private boolean isPositiveLong(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToLong() && node.longValue() > 0;
    }

    private boolean isNegativeLong(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToLong() && node.longValue() < 0;
    }

    private boolean isNonZeroLong(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToLong() && node.longValue() != 0;
    }

    private String canonicalize(JsonNode node) throws Exception {
        if (node.isObject()) {
            Map<String, JsonNode> fields = new TreeMap<>(UNICODE_CODE_POINT_ORDER);
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                fields.put(entry.getKey(), entry.getValue());
            }
            StringBuilder result = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, JsonNode> entry : fields.entrySet()) {
                if (!first) {
                    result.append(',');
                }
                first = false;
                result.append(mapper.writeValueAsString(entry.getKey()))
                        .append(':')
                        .append(canonicalize(entry.getValue()));
            }
            return result.append('}').toString();
        }
        if (node.isArray()) {
            StringBuilder result = new StringBuilder("[");
            for (int index = 0; index < node.size(); index++) {
                if (index > 0) {
                    result.append(',');
                }
                result.append(canonicalize(node.get(index)));
            }
            return result.append(']').toString();
        }
        if (node.isNumber()) {
            if (node.isIntegralNumber()) {
                return node.bigIntegerValue().toString();
            }
            BigDecimal normalized = node.decimalValue().stripTrailingZeros();
            return normalized.signum() == 0 ? "0" : normalized.toPlainString();
        }
        if (node.isTextual()) {
            return mapper.writeValueAsString(node.textValue());
        }
        if (node.isBoolean()) {
            return node.booleanValue() ? "true" : "false";
        }
        if (node.isNull()) {
            return "null";
        }
        throw new IllegalArgumentException("unsupported route snapshot JSON node: " + node.getNodeType());
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    public record Validation(List<Blocker> blockers) {

        public boolean ready() {
            return blockers != null && blockers.isEmpty();
        }
    }

    public record Blocker(String reasonCode, String detail) {
    }
}
