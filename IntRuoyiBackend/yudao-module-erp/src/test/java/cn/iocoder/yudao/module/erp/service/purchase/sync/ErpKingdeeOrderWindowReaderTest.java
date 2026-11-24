package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ErpKingdeeOrderWindowReaderTest {
    @Test
    void unchangedTimestampTiesAndDisappearingEarlierHeaderDoNotSkipNextOrder() {
        var replies = new ArrayDeque<>(List.of("[[1,\"v\"]]", line(1, 1), line(1, 2), "[]",
                "[[1,\"v\"]]", "[[2,\"v\"]]", line(2, 3), "[]", "[[2,\"v\"]]", "[]"));
        var result = ErpKingdeeOrderWindowReader.read(LocalDateTime.of(2026,1,1,0,0),
                LocalDateTime.of(2026,1,2,0,0), 1, "FORM", "FID > 0", "FID,FModifyDate", "ENTRY", 24, 27, query -> {
                    assertEquals(0, query.get("StartRow"));
                    if (replies.size() == 5) assertTrue(query.get("FilterString").toString().contains("FID > 1"));
                    return JsonUtils.parseTree(replies.remove());
                });
        assertEquals(3, result.size());
        assertTrue(replies.isEmpty());
    }

    @Test
    void changedVersionRejectsPartialDocument() {
        var replies = new ArrayDeque<>(List.of("[[1,\"v\"]]", line(1,1), "[]", "[[1,\"changed\"]]"));
        assertThrows(IllegalStateException.class, () -> ErpKingdeeOrderWindowReader.read(
                LocalDateTime.of(2026,1,1,0,0), LocalDateTime.of(2026,1,2,0,0), 1,
                "FORM", "FID > 0", "FID,FModifyDate", "ENTRY", 24, 27,
                query -> JsonUtils.parseTree(replies.remove())));
    }

    @Test
    void repeatedEntryKeyFailsRatherThanLoopingOrDroppingRows() {
        var replies = new ArrayDeque<>(List.of("[[1,\"v\"]]", line(1,1), line(1,1)));
        assertThrows(IllegalStateException.class, () -> ErpKingdeeOrderWindowReader.read(
                LocalDateTime.of(2026,1,1,0,0), LocalDateTime.of(2026,1,2,0,0), 1,
                "FORM", "FID > 0", "FID,FModifyDate", "ENTRY", 24, 27,
                query -> JsonUtils.parseTree(replies.remove())));
    }

    @Test
    void repeatedHeaderKeyFailsRatherThanSilentlyAdvancing() {
        var replies = new ArrayDeque<>(List.of("[[1,\"v\"]]", line(1,1), "[]", "[[1,\"v\"]]", "[[1,\"v\"]]"));
        assertThrows(IllegalStateException.class, () -> ErpKingdeeOrderWindowReader.read(
                LocalDateTime.of(2026,1,1,0,0), LocalDateTime.of(2026,1,2,0,0), 1,
                "FORM", "FID > 0", "FID,FModifyDate", "ENTRY", 24, 27,
                query -> JsonUtils.parseTree(replies.remove())));
    }

    @Test
    void fullReadRejectsNullOrNontextVersionsAtEveryReadStage() {
        for (String invalid : List.of("null", "123", "{}", "\"\"")) {
            for (int stage = 0; stage < 3; stage++) {
                var replies = new ArrayDeque<>(List.of("[[1,\"v\"]]", line(1,1), "[]", "[[1,\"v\"]]"));
                var sequence = new java.util.ArrayList<>(replies);
                if (stage == 0) sequence.set(0, "[[1," + invalid + "]]");
                if (stage == 1) {
                    var detail = (com.fasterxml.jackson.databind.node.ArrayNode) JsonUtils.parseTree(line(1,1));
                    ((com.fasterxml.jackson.databind.node.ArrayNode) detail.get(0)).set(27, JsonUtils.parseTree(invalid));
                    sequence.set(1, detail.toString());
                }
                if (stage == 2) sequence.set(3, "[[1," + invalid + "]]");
                var responses = new ArrayDeque<>(sequence);
                assertThrows(IllegalStateException.class, () -> ErpKingdeeOrderWindowReader.readAll(
                        java.util.Map.of("FilterString", "FID > 0 or FID = 1", "StartRow", 0),
                        1, "FORM", "FID > 0 or FID = 1", "FID,FModifyDate", "ENTRY", 24, 27,
                        query -> {
                            String filter = query.get("FilterString").toString();
                            assertTrue(filter.startsWith("(FID > 0 or FID = 1) and "));
                            return JsonUtils.parseTree(responses.remove());
                        }));
            }
        }
    }

    private static String line(int id, int entry) {
        var row = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < 28; i++) row.add("");
        row.set(0, JsonNodeFactory.instance.numberNode(id));
        row.set(24, JsonNodeFactory.instance.numberNode(entry));
        row.set(27, JsonNodeFactory.instance.textNode("v"));
        return "[" + row + "]";
    }
}
