package cn.iocoder.yudao.module.report.framework.jmreport.config;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JmReportPdfExportAutoConfigurationTest {

    @Test
    void autoConfigurationImports_shouldRegisterPdfExportConfiguration() throws Exception {
        try (var inputStream = getClass().getClassLoader().getResourceAsStream(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")) {
            String imports = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            assertTrue(imports.contains(JmReportPdfExportAutoConfiguration.class.getName()));
        }
    }

}
