package cn.iocoder.yudao.module.ai.service.knowledge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiKnowledgeSegmentReadOnlyContractTest {

    @Test
    void searchShouldBeReadOnlyAndChatShouldRecordRetrievalExplicitly() throws IOException {
        String segmentSource = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/ai/service/knowledge/AiKnowledgeSegmentServiceImpl.java"));
        String searchMethod = method(segmentSource,
                "public List<AiKnowledgeSegmentSearchRespBO> searchKnowledgeSegment");
        assertFalse(searchMethod.contains("updateRetrievalCountIncrByIds"),
                "知识库搜索查询不得隐式更新召回次数");

        String chatSource = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/ai/service/chat/AiChatMessageServiceImpl.java"));
        String recallMethod = method(chatSource,
                "private List<AiKnowledgeSegmentSearchRespBO> recallKnowledgeSegment");
        assertTrue(recallMethod.contains("recordKnowledgeSegmentRetrieval"),
                "聊天命令链路应显式记录知识段召回次数");
    }

    private static String method(String source, String startMarker) {
        int start = source.indexOf(startMarker);
        assertTrue(start >= 0, "无法定位目标方法");
        int bodyStart = source.indexOf('{', start);
        assertTrue(bodyStart > start, "无法定位目标方法体");
        int depth = 0;
        for (int index = bodyStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("目标方法体未闭合");
    }
}
