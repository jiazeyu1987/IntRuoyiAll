package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernancePolicySourceConfigTest {

    private static final List<String> MODULES = List.of("DCC", "EDHR", "SHOWROOM", "INTAUTH");

    private final Path projectDir = findProjectDir();

    @Test
    void localAndDevProfilesMustDeclareConfirmedSignaturePolicySources() throws IOException {
        assertProfileDeclaresConfirmedPolicySources("yudao-server/src/main/resources/application-local.yaml");
        assertProfileDeclaresConfirmedPolicySources("yudao-server/src/main/resources/application-dev.yaml");
    }

    private void assertProfileDeclaresConfirmedPolicySources(String relativePath) throws IOException {
        String content = Files.readString(projectDir.resolve(relativePath), StandardCharsets.UTF_8);

        assertTrue(content.contains("signature:"), relativePath + " must declare signature governance config");
        assertTrue(content.contains("governance:"), relativePath + " must declare signature governance config");
        assertTrue(content.contains("policy:"), relativePath + " must declare signature governance policy config");
        assertTrue(content.contains("modules:"), relativePath + " must declare signature governance module sources");
        for (String module : MODULES) {
            assertModulePolicySource(content, module, relativePath);
        }
    }

    private static void assertModulePolicySource(String content, String module, String relativePath) {
        List<String> block = extractModuleBlock(content, module, relativePath);
        assertBlockContains(block, "source-code:", relativePath, module);
        assertBlockContains(block, "policy-version:", relativePath, module);
        assertBlockContains(block, "authority-confirmed: true", relativePath, module);
        assertBlockContains(block, "owner:", relativePath, module);
        assertBlockContains(block, "approval-ref:", relativePath, module);
    }

    private static List<String> extractModuleBlock(String content, String module, String relativePath) {
        String moduleLine = "        " + module + ":";
        String[] lines = content.split("\\R");
        List<String> block = new ArrayList<>();
        boolean inModule = false;
        for (String line : lines) {
            if (!inModule) {
                inModule = moduleLine.equals(line);
                continue;
            }
            if (!line.isBlank() && line.startsWith("        ") && !line.startsWith("          ")) {
                break;
            }
            if (!line.isBlank()) {
                block.add(line.trim());
            }
        }
        assertFalse(block.isEmpty(), relativePath + " must declare " + module + " policy source");
        return block;
    }

    private static void assertBlockContains(List<String> block, String prefix, String relativePath, String module) {
        assertTrue(block.stream().anyMatch(line -> line.startsWith(prefix)),
                relativePath + " must declare " + module + " " + prefix);
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
