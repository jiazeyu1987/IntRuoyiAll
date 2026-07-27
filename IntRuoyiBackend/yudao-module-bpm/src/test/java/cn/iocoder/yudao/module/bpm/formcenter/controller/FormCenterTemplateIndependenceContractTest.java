package cn.iocoder.yudao.module.bpm.formcenter.controller;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplateRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FormCenterTemplateIndependenceContractTest {

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
    void formCenterTemplateContractDoesNotExposeBatchRecordBinding() {
        assertFieldsAbsent(FormCenterTemplateRespVO.class);
        assertFieldsAbsent(FormTemplateVersionDO.class);
    }

    @Test
    void runtimeDoesNotMapBatchRecordBinding() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));

        for (String field : BATCH_RECORD_BINDING_FIELDS) {
            String accessor = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            assertFalse(source.contains("set" + accessor),
                    "form-center runtime must not map batch-record field " + field);
            assertFalse(source.contains("get" + accessor),
                    "form-center runtime must not read batch-record field " + field);
        }
    }

    private static void assertFieldsAbsent(Class<?> type) {
        for (String fieldName : BATCH_RECORD_BINDING_FIELDS) {
            assertFalse(Arrays.stream(type.getDeclaredFields())
                            .anyMatch(field -> field.getName().equals(fieldName)),
                    type.getSimpleName() + " must not expose " + fieldName);
        }
    }
}
