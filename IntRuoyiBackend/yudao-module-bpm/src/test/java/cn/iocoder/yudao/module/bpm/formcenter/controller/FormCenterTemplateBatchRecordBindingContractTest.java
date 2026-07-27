package cn.iocoder.yudao.module.bpm.formcenter.controller;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormCenterTemplateBatchRecordBindingContractTest {

    private static final String[] BINDING_FIELDS = {
            "batchRecordReportId",
            "batchRecordReportName",
            "batchRecordName",
            "batchRecordVersionNo",
            "batchRecordFormSlotType",
            "batchRecordBindingStatus",
            "batchRecordBindingError"
    };

    @Test
    void templatePoolResponseExposesExplicitBatchRecordBindingSummary() {
        assertFields(FormCenterTemplateRespVO.class);
    }

    @Test
    void templateVersionPersistsExplicitBatchRecordBindingSummary() {
        assertFields(FormTemplateVersionDO.class);
    }

    @Test
    void runtimeMapsBindingSummaryWithoutMesCouplingOrNameGuessing() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));

        for (String field : BINDING_FIELDS) {
            String accessor = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            assertTrue(source.contains("respVO.set" + accessor + "(version.get" + accessor + "()"),
                    "template pool response must map " + field + " from the persisted template version");
        }

        assertFalse(source.contains("MesProBatchRecordReport"),
                "BPM form-center runtime must not depend on MES batch-record report internals");
        assertFalse(source.contains("mes_pro_batch_record_report"),
                "BPM form-center runtime must not query MES tables by name");
        assertFalse(source.contains("getTemplateName().equals"),
                "batch-record binding must not be guessed from template name equality");
        assertFalse(source.contains("getSourceFileName().equals"),
                "batch-record binding must not be guessed from source file name equality");
    }

    private static void assertFields(Class<?> type) {
        for (String fieldName : BINDING_FIELDS) {
            Field field = findField(type, fieldName);
            assertEquals(String.class, field.getType(), type.getSimpleName() + "." + fieldName + " must be String");
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(type.getSimpleName() + " missing " + fieldName));
    }
}
