package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.PROJECTION_BATCH_INVALID;

/** Canonical persisted content: member sets are ordered, object fields sorted, semantic arrays retained. */
public final class ProjectionCodec {
    private final ObjectMapper mapper;
    public ProjectionCodec(ObjectMapper mapper) { this.mapper=mapper; }
    public String encode(Object value) {
        try { return mapper.writeValueAsString(canonical(mapper.valueToTree(value),"")); }
        catch(Exception failure) { throw PROJECTION_BATCH_INVALID.exception(failure); }
    }
    private JsonNode canonical(JsonNode node,String field) {
        if(node.isObject()) {
            ObjectNode out=mapper.createObjectNode(); List<String> names=new ArrayList<>(); node.fieldNames().forEachRemaining(names::add);
            names.stream().sorted().forEach(name->out.set(name,canonical(node.get(name),name))); return out;
        }
        if(node.isArray()) {
            List<JsonNode> values=new ArrayList<>(); node.forEach(item->values.add(canonical(item,"")));
            if("members".equals(field)||"sources".equals(field)) values.sort(Comparator.comparing(JsonNode::toString));
            ArrayNode out=mapper.createArrayNode(); values.forEach(out::add); return out;
        }
        return node;
    }
    public <T>T decode(String payload,Class<T> type) {
        try { return mapper.readValue(payload,type); }
        catch(Exception failure) { throw PROJECTION_BATCH_INVALID.exception(failure); }
    }
    public String hash(Object value) { return hashText(encode(value)); }
    public static String hashText(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable",impossible); }
    }
}
