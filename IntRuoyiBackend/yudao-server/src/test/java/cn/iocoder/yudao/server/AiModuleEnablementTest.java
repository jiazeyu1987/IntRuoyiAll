package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModuleEnablementTest {

    private final Path projectDir = findProjectDir();

    @Test
    void rootPomShouldIncludeAiModule() throws Exception {
        Document document = readPom(projectDir.resolve("pom.xml"));

        assertTrue(hasModule(document, "yudao-module-ai"),
                "root pom.xml must include yudao-module-ai because AI big-model menus are enabled");
    }

    @Test
    void serverPomShouldDependOnAiModule() throws Exception {
        Document document = readPom(projectDir.resolve("yudao-server/pom.xml"));

        assertTrue(hasDependency(document, "cn.iocoder.boot", "yudao-module-ai"),
                "yudao-server/pom.xml must depend on cn.iocoder.boot:yudao-module-ai because /admin-api/ai/** routes are enabled");
    }

    @Test
    void serverClasspathShouldContainAiChatMessageController() {
        assertDoesNotThrow(() -> Class.forName(
                "cn.iocoder.yudao.module.ai.controller.admin.chat.AiChatMessageController"));
    }

    private static Document readPom(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private static boolean hasModule(Document document, String moduleName) {
        NodeList modules = document.getElementsByTagName("module");
        for (int i = 0; i < modules.getLength(); i++) {
            if (moduleName.equals(modules.item(i).getTextContent().trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDependency(Document document, String groupId, String artifactId) {
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            if (groupId.equals(textOfFirst(dependency, "groupId"))
                    && artifactId.equals(textOfFirst(dependency, "artifactId"))) {
                return true;
            }
        }
        return false;
    }

    private static String textOfFirst(Element element, String tagName) {
        NodeList values = element.getElementsByTagName(tagName);
        return values.getLength() == 0 ? "" : values.item(0).getTextContent().trim();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
