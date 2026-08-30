package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormCenterRuntimeImportRecognitionFlowContractTest {

    @Test
    void importDocRecognizesAndPersistsRulesBeforeApproval() throws IOException {
        String runtime = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));
        String importDoc = methodBlock(runtime, "public FormCenterTemplateImportRespVO importDoc");

        int recognizeIndex = importDoc.indexOf("templateRecognizer.recognize(command)");
        int insertIndex = importDoc.indexOf("templateVersionMapper.insert(insertObj)");
        int approvalIndex = importDoc.indexOf("submitTemplateUpgradeApproval");

        assertTrue(recognizeIndex > 0, "Word import must invoke the formal template recognizer");
        assertTrue(insertIndex > recognizeIndex,
                "Word import must recognize the document before inserting the version");
        assertTrue(approvalIndex > insertIndex,
                "Word import must persist the recognized version before entering approval or publish flow");
        assertTrue(importDoc.contains(".recognizedSchemaJson(JsonUtils.toJsonString(recognition.getFields()))"),
                "Word import must persist recognized fields on the version");
        assertTrue(importDoc.contains("requireRecognizedVisualSchema(recognition.getJimuSchemaJson())"),
                "Word import must fail fast when recognition did not produce sheetLayoutJson rows and cellRules");
        assertTrue(importDoc.contains(".jimuSchemaJson(recognizedJimuSchemaJson)"),
                "Word import must persist the validated visual schema and cell rules on the version");
        assertTrue(importDoc.contains("approvalRequest.getResultState()"),
                "Word import response must expose the direct-publish or pending-approval result state");
        assertFalse(importDoc.contains("autoDetectTemplateFillRules"),
                "Word import must not call manual fill-rule recognition as a fallback path");
        assertFalse(importDoc.toLowerCase().contains("codex"),
                "Word import must use code recognition and must not depend on Codex CLI");
    }

    @Test
    void importDocFailsFastWhenVisualSchemaRowsOrRulesAreMissing() throws IOException {
        String runtime = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"));

        assertTrue(runtime.contains("Template schema rows are missing"),
                "Missing sheetLayoutJson rows must produce the same explicit failure seen by the user");
        assertTrue(runtime.contains("Template cell rules are missing"),
                "Missing cellRules must fail before creating an unusable template version");
        assertTrue(runtime.contains("TEMPLATE_RECOGNITION_FAILED"),
                "Missing visual schema must be treated as recognition failure, not as a draft users must repair manually");
    }

    @Test
    void wordDocxImportBuildsTypedCellRulesFromSourceTable() throws IOException {
        String recognizer = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/DefaultWordFormTemplateRecognizer.java"));
        String builder = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/WordTableVisualSchemaBuilder.java"));

        assertTrue(recognizer.contains("WordTableVisualSchemaBuilder.build(document.getTables().get(0))"),
                "DOCX import must build the Jimu schema directly from the source Word table");
        assertTrue(builder.contains("cellRules"),
                "Word table builder must emit cellRules during import");
        assertTrue(builder.contains("signatureCellMarkers"),
                "Word table builder must emit electronic signature markers during import");
        assertTrue(builder.contains("\"input-number\""),
                "Word table builder must identify number cells without AI");
        assertTrue(builder.contains("\"date\""),
                "Word table builder must identify date cells without AI");
        assertTrue(builder.contains("\"signature\""),
                "Word table builder must identify signature cells without AI");
        assertTrue(builder.contains("\"radio-group\""),
                "Word table builder must identify choice cells without AI");
    }

    @Test
    void upgradeApprovalPublishesDirectlyOrAfterManualApproval() throws IOException {
        String executor = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/formcenter/service/FormTemplateUpgradeBusinessApprovalEffectExecutor.java"));

        String executeDirect = methodBlock(executor, "public BusinessApprovalEffectResult executeDirect");
        String markPending = methodBlock(executor, "public BusinessApprovalEffectResult markPending");
        String executeApproved = methodBlock(executor, "public BusinessApprovalEffectResult executeApproved");

        assertTrue(executeDirect.contains("requireVersionWithStatus(context, FormTemplateStatus.DRAFT)"),
                "Direct approval must start from the imported draft version");
        assertTrue(executeDirect.contains("updateStatus(version, FormTemplateStatus.PUBLISHED)"),
                "Direct approval must publish the imported version");
        assertTrue(markPending.contains("updateStatus(version, FormTemplateStatus.PENDING_APPROVAL)"),
                "Manual approval mode must leave the imported version pending approval");
        assertTrue(executeApproved.contains("requireVersionWithStatus(context, FormTemplateStatus.PENDING_APPROVAL)"),
                "Manual approval completion must start from pending approval");
        assertTrue(executeApproved.contains("updateStatus(version, FormTemplateStatus.PUBLISHED)"),
                "Manual approval completion must publish the imported version");
    }

    private static String methodBlock(String source, String methodSignature) {
        int start = source.indexOf(methodSignature);
        assertTrue(start >= 0, "method not found: " + methodSignature);
        int nextOverride = source.indexOf("\n    @Override", start + methodSignature.length());
        if (nextOverride < 0) {
            nextOverride = source.indexOf("\n    private ", start + methodSignature.length());
        }
        if (nextOverride < 0) {
            nextOverride = source.length();
        }
        return source.substring(start, nextOverride);
    }
}
