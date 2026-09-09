package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFileHighPriorityDataValidationContractTest {

    private static final Path FINALIZATION_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/dcc/service/file/"
                    + "DccControlledFileFinalizationServiceImpl.java");
    private static final Path WORKFLOW_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/dcc/service/file/"
                    + "DccControlledFileWorkflowServiceImpl.java");

    @Test
    void controlledFilePublish_mustFailFastWhenStampingFails() throws Exception {
        String source = read(FINALIZATION_SERVICE);

        assertTrue(!source.contains("publish original PDF instead"),
                "受控文件发布盖章失败不得发布原 PDF 作为降级成功。");
        assertTrue(!source.contains("new PublishedArtifact(sourceFile.getId(), null, null)"),
                "受控文件发布产物必须是盖章 PDF，不能以未盖章源文件冒充发布件。");
        assertTrue(source.contains("CONTROLLED_FILE_STAMP_GENERATION_FAILED"),
                "盖章失败必须返回明确业务错误。");
    }

    @Test
    void withdrawnControlledFileDelete_mustNotDeleteSourceArtifactsPhysically() throws Exception {
        String source = read(WORKFLOW_SERVICE);
        String deleteMethod = methodBody(source, "deleteWithdrawnControlledFile");

        assertTrue(!deleteMethod.contains("deleteUnreferencedArtifacts("),
                "撤回受控文件删除不得顺手物理删除源文件、原始文件或图纸 PDF。");
        assertTrue(deleteMethod.contains("validateWithdrawnApplicantAction("),
                "撤回文件删除仍必须保留申请人、状态和流程实例校验。");
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String methodBody(String source, String methodName) {
        int methodIndex = source.indexOf(methodName + "(");
        assertTrue(methodIndex >= 0, "Missing method " + methodName);
        int bodyStart = source.indexOf('{', methodIndex);
        assertTrue(bodyStart >= 0, "Missing method body " + methodName);
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body " + methodName);
    }
}
