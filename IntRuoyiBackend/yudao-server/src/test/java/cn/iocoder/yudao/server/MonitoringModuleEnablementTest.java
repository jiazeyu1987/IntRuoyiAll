package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringModuleEnablementTest {

    private final Path projectDir = findProjectDir();

    @Test
    void serverPomShouldDependOnSpringBootAdminServerStarter() throws Exception {
        Document document = readPom(projectDir.resolve("yudao-server/pom.xml"));

        assertTrue(hasDependency(document, "de.codecentric", "spring-boot-admin-starter-server"),
                "yudao-server/pom.xml must depend on de.codecentric:spring-boot-admin-starter-server because the Infrastructure Java monitor route expects a local Spring Boot Admin server");
    }

    @Test
    void serverClasspathShouldContainAdminServerProperties() {
        assertDoesNotThrow(() -> Class.forName(
                "de.codecentric.boot.admin.server.config.AdminServerProperties"));
    }

    private static Document readPom(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(path.toFile());
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
