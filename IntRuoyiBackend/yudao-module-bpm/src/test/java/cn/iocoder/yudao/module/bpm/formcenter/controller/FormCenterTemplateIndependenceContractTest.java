package cn.iocoder.yudao.module.bpm.formcenter.controller;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormCenterTemplateIndependenceContractTest {

    private static final String DESIGNER_REPORT_ID_FIELD = "designerReportId";

    private static final String[] BATCH_RECORD_BINDING_FIELDS = {
            "batchRecordReportId",
            "batchRecordReportName",
            "batchRecordName",
            "batchRecordVersionNo",
            "batchRecordFormSlotType",
            "batchRecordBindingStatus",
            "batchRecordBindingError"
    };

    @Test
    void formCenterTemplateContractExposesDesignerReportIdWithoutPersistingItOnVersionDo() {
        assertFieldPresent(FormCenterTemplateRespVO.class, DESIGNER_REPORT_ID_FIELD);
        assertFieldAbsent(FormTemplateVersionDO.class, DESIGNER_REPORT_ID_FIELD);
        assertFieldsAbsent(FormCenterTemplateRespVO.class);
        assertFieldsAbsent(FormTemplateVersionDO.class);
    }

    @Test
    void runtimeMapsDesignerReportIdFromTemplateVersionId() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));

        assertTrue(source.contains("setDesignerReportId"),
                "form-center runtime must map designer report id onto template response without batch-record naming");
        assertTrue(source.contains("FORMTPL:"),
                "form-center runtime must use the formal FORM template report prefix");
        assertTrue(source.contains("version.getId()"),
                "form-center runtime must derive designer report id from the exact template version id");

        for (String field : BATCH_RECORD_BINDING_FIELDS) {
            String accessor = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            assertFalse(source.contains("set" + accessor),
                    "form-center runtime must not map batch-record field " + field);
            assertFalse(source.contains("get" + accessor),
                    "form-center runtime must not read batch-record field " + field);
        }
    }

    private static void assertFieldPresent(Class<?> type, String fieldName) {
        assertTrue(Arrays.stream(type.getDeclaredFields())
                .anyMatch(field -> field.getName().equals(fieldName)),
                type.getSimpleName() + " must expose " + fieldName);
    }

    private static void assertFieldAbsent(Class<?> type, String fieldName) {
        assertFalse(Arrays.stream(type.getDeclaredFields())
                .anyMatch(field -> field.getName().equals(fieldName)),
                type.getSimpleName() + " must not expose " + fieldName);
    }

    private static void assertFieldsAbsent(Class<?> type) {
        for (String fieldName : BATCH_RECORD_BINDING_FIELDS) {
            assertFieldAbsent(type, fieldName);
        }
    }
}
