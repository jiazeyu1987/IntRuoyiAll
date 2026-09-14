package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordReportParseOnlyContractTest {

    @Test
    void parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyBoundary() throws Exception {
        Path servicePath = Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module", "mes",
                "service", "pro", "batchrecordreport", "MesProBatchRecordReportServiceImpl.java");
        String serviceSource = Files.readString(servicePath);
        String methodBody = extractMethodBody(serviceSource,
                "public String parseProductionBatchRecordTotalRecognitionJson");
        String serializerBody = extractMethodBody(serviceSource, "private String buildTotalRecognitionJson");

        assertTrue(methodBody.contains("validateUploadedRouteDoc(file)"));
        assertTrue(methodBody.contains("parseWordByFileName(bytes, sourceFileName)"));
        assertTrue(methodBody.contains("buildTotalRecognitionJson(sourceFileName, parsedTables)"));
        assertTrue(serializerBody.contains("OBJECT_MAPPER.writeValueAsString"));
        assertTrue(serializerBody.contains("new MesProBatchRecordTotalRecognitionExtractor().extract(sourceFileName, parsedTables)"));
        assertNoWrites(methodBody);
        assertNoWrites(serializerBody);
    }

    private static void assertNoWrites(String methodBody) {
        assertFalse(methodBody.contains("saveGeneratedReports("));
        assertFalse(methodBody.contains("saveProjectCodeBatchRecordTotalRecognitionJson("));
        assertFalse(methodBody.contains("recognitionDeviceSyncService"));
        assertFalse(methodBody.contains("jimuReportGateway"));
        assertFalse(methodBody.contains("definitionMapper"));
        assertFalse(methodBody.contains("versionMapper"));
    }

    private static String extractMethodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        assertTrue(signatureIndex >= 0, signature + " must exist");
        int bodyStart = source.indexOf('{', signatureIndex);
        assertTrue(bodyStart >= 0, signature + " body must start");
        int depth = 0;
        for (int index = bodyStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, index + 1);
                }
            }
        }
        throw new AssertionError(signature + " body must end");
    }
}
