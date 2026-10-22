package cn.iocoder.yudao.module.signature.service;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

final class ElectronicSignatureJsonCanonicalizer {

    private ElectronicSignatureJsonCanonicalizer() {
    }

    static String canonicalize(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return JsonUtils.toJsonString(sort(JsonUtils.parseTree(json)));
    }

    private static JsonNode sort(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            var fields = new ArrayList<Map.Entry<String, JsonNode>>();
            node.fields().forEachRemaining(fields::add);
            fields.sort(Comparator.comparing(Map.Entry::getKey));
            for (Map.Entry<String, JsonNode> field : fields) {
                sorted.set(field.getKey(), sort(field.getValue()));
            }
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode sorted = JsonNodeFactory.instance.arrayNode();
            for (JsonNode child : node) {
                sorted.add(sort(child));
            }
            return sorted;
        }
        return node;
    }
}
