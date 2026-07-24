package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesModuleEnablementTest {

    private final Path projectDir = findProjectDir();

    @Test
    void rootPomShouldIncludeMesModule() throws Exception {
        Document document = readPom(projectDir.resolve("pom.xml"));

        assertTrue(hasModule(document, "yudao-module-mes"),
                "root pom.xml must include yudao-module-mes in <modules>");
    }

    @Test
    void serverPomShouldDependOnMesModule() throws Exception {
        Document document = readPom(projectDir.resolve("yudao-server/pom.xml"));

        assertTrue(hasDependency(document, "cn.iocoder.boot", "yudao-module-mes"),
                "yudao-server/pom.xml must depend on cn.iocoder.boot:yudao-module-mes");
    }

    private static Path findProjectDir() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("yudao-server/pom.xml"))
                    && Files.exists(current.resolve("yudao-module-mes/pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find ruoyi-vue-pro project directory");
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

}
