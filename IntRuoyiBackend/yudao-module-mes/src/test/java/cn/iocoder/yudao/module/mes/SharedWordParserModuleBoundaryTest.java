package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterWordParserConfiguration;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormTemplateRecognizer;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordDocParser;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRouteCRecognizer;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordWordParserConfiguration;
import cn.iocoder.yudao.module.wordparser.SharedWordDocumentParser;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedWordParserModuleBoundaryTest {

    private static final String SHARED_ARTIFACT = "yudao-module-word-parser";
    private static final Set<String> ALLOWED_SHARED_ARTIFACTS = Set.of(
            "poi-ooxml", "poi-scratchpad", "yudao-spring-boot-starter-test");
    private static final List<String> FORBIDDEN_SHARED_SOURCE_TOKENS = List.of(
            "cn.iocoder.yudao.module.bpm",
            "cn.iocoder.yudao.module.mes",
            "org.flowable",
            "org.jeecg.modules.jmreport",
            "@Transactional",
            "fillable",
            "componentFlag",
            "edhrCellRule",
            "routeKey",
            "batchRecordName",
            "templateId",
            "sourceSplitIndex",
            "sourceTableIndex",
            "tableTitle",
            "preserveSourceGrid",
            "routeBSource",
            "documentFrameRole",
            "visualBlank",
            "reviewedCellRule",
            "cellRuleSource",
            "placeholder",
            "inputType",
            "approval",
            "jimu",
            "versionId");

    @Test
    void sharedWordParserModule_hasOneWayBusinessNeutralDependencies() throws Exception {
        Path reactorRoot = locateReactorRoot();
        Path sharedPom = reactorRoot.resolve(SHARED_ARTIFACT).resolve("pom.xml");

        assertTrue(Files.isRegularFile(sharedPom),
                "shared Word parser module POM must exist before adapters can depend on it");

        assertTrue(moduleNames(reactorRoot.resolve("pom.xml")).contains(SHARED_ARTIFACT),
                "backend reactor must declare the shared Word parser module");
        Set<String> bpmDependencies = dependencyArtifactIds(reactorRoot.resolve("yudao-module-bpm/pom.xml"));
        Set<String> mesDependencies = dependencyArtifactIds(reactorRoot.resolve("yudao-module-mes/pom.xml"));
        assertFalse(bpmDependencies.contains("yudao-module-mes"),
                "BPM must not depend on MES because MES already depends on BPM");
        assertTrue(bpmDependencies.contains(SHARED_ARTIFACT),
                "BPM must directly depend on the shared Word parser module");
        assertTrue(mesDependencies.contains(SHARED_ARTIFACT),
                "MES must directly depend on the shared Word parser module");
        assertFalse(bpmDependencies.contains("poi-ooxml") || bpmDependencies.contains("poi-scratchpad"),
                "BPM must not retain direct POI parser dependencies after migration");

        Set<String> sharedDependencies = dependencyArtifactIds(sharedPom);
        assertTrue(ALLOWED_SHARED_ARTIFACTS.containsAll(sharedDependencies),
                "shared module dependencies must be limited to POI plus the test starter: " + sharedDependencies);
        assertSharedSourcesAreBusinessNeutral(reactorRoot.resolve(SHARED_ARTIFACT).resolve("src/main/java"));
        assertBusinessAdaptersDelegateWithoutPoiOrCrossBusinessBeanOwnership(reactorRoot);
    }

    @Test
    void businessConfigurationsCreateIndependentAdaptersWithoutSharedParserBeanCollision() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(FormCenterWordParserConfiguration.class,
                    MesProBatchRecordWordParserConfiguration.class);
            context.refresh();

            assertEquals(1, context.getBeansOfType(FormTemplateRecognizer.class).size());
            assertEquals(1, context.getBeansOfType(MesProBatchRecordDocParser.class).size());
            assertEquals(1, context.getBeansOfType(MesProBatchRecordRouteCRecognizer.class).size());
            assertTrue(context.getBeansOfType(SharedWordDocumentParser.class).isEmpty(),
                    "business configurations must not publish duplicate shared-parser interface beans");
        }
    }

    private Path locateReactorRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            Path pom = current.resolve("pom.xml");
            if (Files.isRegularFile(pom) && Files.isDirectory(current.resolve("yudao-module-mes"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate IntRuoyiBackend reactor root");
    }

    private Set<String> moduleNames(Path pom) throws Exception {
        Document document = parsePom(pom);
        Set<String> modules = new HashSet<>();
        NodeList nodes = document.getElementsByTagName("module");
        for (int index = 0; index < nodes.getLength(); index++) {
            modules.add(nodes.item(index).getTextContent().trim());
        }
        return modules;
    }

    private Set<String> dependencyArtifactIds(Path pom) throws Exception {
        Document document = parsePom(pom);
        Set<String> artifactIds = new HashSet<>();
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Node dependency = dependencies.item(index);
            if (!(dependency instanceof Element element)) {
                continue;
            }
            NodeList children = element.getChildNodes();
            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                Node child = children.item(childIndex);
                if (child instanceof Element childElement && "artifactId".equals(childElement.getLocalName())) {
                    artifactIds.add(childElement.getTextContent().trim());
                }
            }
        }
        return artifactIds;
    }

    private Document parsePom(Path pom) throws Exception {
        assertTrue(Files.isRegularFile(pom), "required Maven POM is missing: " + pom);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(pom.toFile());
    }

    private void assertSharedSourcesAreBusinessNeutral(Path sourceRoot) throws IOException {
        assertTrue(Files.isDirectory(sourceRoot), "shared main source directory must exist: " + sourceRoot);
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<Path> javaSources = paths.filter(path -> path.toString().endsWith(".java")).toList();
            assertFalse(javaSources.isEmpty(), "shared module must contain Java production sources");
            for (Path source : javaSources) {
                String content = Files.readString(source);
                for (String token : FORBIDDEN_SHARED_SOURCE_TOKENS) {
                    assertFalse(content.contains(token),
                            () -> "shared source contains forbidden business token '" + token + "': " + source);
                }
            }
        }
    }

    private void assertBusinessAdaptersDelegateWithoutPoiOrCrossBusinessBeanOwnership(Path reactorRoot)
            throws IOException {
        Path bpmRuntime = reactorRoot.resolve(
                "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime");
        Path mesRuntime = reactorRoot.resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport");
        List<Path> businessWordAdapters = List.of(
                bpmRuntime.resolve("DefaultWordFormTemplateRecognizer.java"),
                mesRuntime.resolve("MesProBatchRecordDocParser.java"),
                mesRuntime.resolve("MesProBatchRecordRouteCRecognizer.java"));
        String bpmConfiguration = Files.readString(bpmRuntime.resolve("FormCenterWordParserConfiguration.java"));
        String mesConfiguration = Files.readString(
                mesRuntime.resolve("MesProBatchRecordWordParserConfiguration.java"));

        for (Path adapter : businessWordAdapters) {
            String source = Files.readString(adapter);
            assertFalse(source.contains("org.apache.poi") || source.contains("HWPF")
                            || source.contains("XWPF") || source.contains("WordExtractor"),
                    () -> "business adapter must not retain raw POI traversal: " + adapter);
            assertTrue(source.contains("SharedWordDocumentParser"),
                    () -> "business adapter must delegate to the shared parser interface: " + adapter);
            assertTrue(source.contains("WordParseProfile.STRUCTURAL_CANONICAL"),
                    () -> "business adapter must use the single canonical profile: " + adapter);
        }
        assertNoRawWordParserSources(bpmRuntime);
        assertNoRawWordParserSources(mesRuntime);
        for (String configuration : List.of(bpmConfiguration, mesConfiguration)) {
            assertTrue(configuration.contains("new DefaultSharedWordDocumentParser()"),
                    "each business adapter configuration must construct the canonical implementation explicitly");
            assertFalse(configuration.contains("public SharedWordDocumentParser"),
                    "business configurations must not expose colliding cross-business shared-parser beans");
        }
    }

    private void assertNoRawWordParserSources(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path source : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(source);
                assertFalse(content.contains("org.apache.poi.hwpf")
                                || content.contains("org.apache.poi.xwpf")
                                || content.contains("HWPFDocument")
                                || content.contains("XWPFDocument")
                                || content.contains("WordExtractor")
                                || content.contains("document.getTables()"),
                        () -> "production Word source must not contain a second raw parser: " + source);
            }
        }
    }
}
