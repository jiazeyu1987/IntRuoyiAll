package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrFormDataIntegrityContractTest {

    @Test
    void draftAndSubmitMustValidateValuesBeforeReplacingRows() throws Exception {
        String source = readText("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormServiceImpl.java");

        assertInOrder(source,
                "public MesProEdhrFormInstanceRespVO saveDraft",
                "validateDraftValues(fieldSpecs, values);",
                "replaceInstanceValues(instance.getId(), fieldSpecs, values);");
        assertInOrder(source,
                "public MesProEdhrFormInstanceRespVO submit",
                "validateSubmissionValues(fieldSpecs, values);",
                "replaceInstanceValues(instance.getId(), fieldSpecs, values);");

        assertContains(source,
                "private void validateDraftValues",
                "validateKnownFields(fieldSpecs, values);",
                "validateNumberRange(fieldSpec, value);",
                "validateEnumOptions(fieldSpec, value);",
                "validateDateFormat(fieldSpec, value);",
                "validateValueTextLength(fieldSpec, value);");
        assertContains(source,
                "private void validateDateFormat",
                "LocalDate.parse",
                "DateTimeParseException",
                "PRO_EDHR_FORM_FIELD_DATE_INVALID");
        assertContains(source,
                "private void validateValueTextLength",
                "PRO_EDHR_FORM_FIELD_TEXT_TOO_LONG");
    }

    @Test
    void fieldSchemaMustRejectAmbiguousOrLossyDefinitions() throws Exception {
        String source = readText("src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormServiceImpl.java");

        assertContains(source,
                "Set<String> fieldKeys = new HashSet<>();",
                "fieldKeys.add(field.getKey())",
                "field.getMin().compareTo(field.getMax()) > 0",
                "normalizeEnumOptions(field)",
                "PRO_EDHR_FORM_FIELD_SCHEMA_INVALID");
        assertFalse(source.contains("return text.length() > 1000 ? text.substring(0, 1000) : text"),
                "独立表单值不得在 valueText 转换时静默截断，超长值必须提前校验失败");
    }

    private static String readText(String relativePath) throws Exception {
        return Files.readString(Path.of(System.getProperty("user.dir")).resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static void assertContains(String source, String... tokens) {
        for (String token : tokens) {
            assertTrue(source.contains(token), "缺少契约片段：" + token);
        }
    }

    private static void assertInOrder(String source, String... tokens) {
        int cursor = -1;
        for (String token : tokens) {
            int index = source.indexOf(token, cursor + 1);
            assertTrue(index > cursor, "契约片段顺序不正确或缺失：" + token);
            cursor = index;
        }
    }
}
