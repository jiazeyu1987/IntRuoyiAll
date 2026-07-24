package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

class DccMapperXmlValidityTest {

    @Test
    void mapperXmlFilesAreWellFormed() throws Exception {
        Path mapperRoot = Path.of("src/main/resources/mapper");
        List<Path> mapperFiles;
        try (var stream = Files.walk(mapperRoot)) {
            mapperFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .sorted()
                    .toList();
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

        for (Path mapperFile : mapperFiles) {
            try (var reader = Files.newBufferedReader(mapperFile)) {
                builder.parse(new InputSource(reader));
            } catch (Exception ex) {
                fail("MyBatis mapper XML must be well-formed: " + mapperFile, ex);
            }
        }
    }
}
