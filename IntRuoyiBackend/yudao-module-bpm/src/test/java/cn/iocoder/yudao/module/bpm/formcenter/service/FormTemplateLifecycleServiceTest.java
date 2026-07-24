package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImpact;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImpactCheckResult;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersion;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormTemplateLifecycleServiceTest {

    @Test
    void importDocxCreatesDraftVersionWithRequiredMetadata() {
        FormTemplateLifecycleService service = new FormTemplateLifecycleService(
                new FixedRecognizer(FormTemplateRecognition.success(List.of(
                        FormRecognizedField.required("changeReason", "Change Reason", "textarea")))),
                new FixedReferenceChecker(List.of()), new MemoryTemplateVersionStore());

        FormTemplateVersion version = service.importDoc(command("change-form.docx"));

        assertNotNull(version.getTemplateId());
        assertEquals("Change Form", version.getTemplateName());
        assertEquals("1.0.0", version.getVersionNo());
        assertEquals(FormTemplateStatus.DRAFT, version.getStatus());
        assertEquals("form remark", version.getRemark());
        assertNotNull(version.getUpdatedTime());
        assertEquals("changeReason", version.getRecognizedFields().get(0).getFieldCode());
    }

    @Test
    void importRejectsUnsupportedSourceType() {
        FormTemplateLifecycleService service = new FormTemplateLifecycleService(
                new FixedRecognizer(FormTemplateRecognition.success(List.of())),
                new FixedReferenceChecker(List.of()), new MemoryTemplateVersionStore());

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.importDoc(command("change-form.pdf")));

        assertEquals(FormCenterErrorCode.TEMPLATE_SOURCE_TYPE_UNSUPPORTED, ex.getErrorCode());
    }

    @Test
    void recognitionFailureBlocksTemplateCreation() {
        FormTemplateLifecycleService service = new FormTemplateLifecycleService(
                new FixedRecognizer(FormTemplateRecognition.failure("no fields")),
                new FixedReferenceChecker(List.of()), new MemoryTemplateVersionStore());

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.importDoc(command("change-form.docx")));

        assertEquals(FormCenterErrorCode.TEMPLATE_RECOGNITION_FAILED, ex.getErrorCode());
    }

    @Test
    void publishedVersionCannotOverwriteJimuSchema() {
        FormTemplateLifecycleService service = new FormTemplateLifecycleService(
                new FixedRecognizer(FormTemplateRecognition.success(List.of(
                        FormRecognizedField.required("changeReason", "Change Reason", "textarea")))),
                new FixedReferenceChecker(List.of()), new MemoryTemplateVersionStore());
        FormTemplateVersion version = service.importDoc(command("change-form.docx"));
        service.saveJimuSchema(version.getTemplateId(), version.getVersionNo(), "{\"fields\":[\"changeReason\"]}");
        service.publish(version.getTemplateId(), version.getVersionNo());

        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.saveJimuSchema(version.getTemplateId(), version.getVersionNo(), "{\"fields\":[]}"));

        assertEquals(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE, ex.getErrorCode());
    }

    @Test
    void disableImpactCheckBlocksTemplateReferencedByPublishedPolicy() {
        FormTemplateLifecycleService service = new FormTemplateLifecycleService(
                new FixedRecognizer(FormTemplateRecognition.success(List.of(
                        FormRecognizedField.required("changeReason", "Change Reason", "textarea")))),
                new FixedReferenceChecker(List.of(FormTemplateImpact.policyReference(10L, "upload-policy"))),
                new MemoryTemplateVersionStore());
        FormTemplateVersion version = service.importDoc(command("change-form.docx"));
        service.publish(version.getTemplateId(), version.getVersionNo());

        FormTemplateImpactCheckResult impact = service.disableImpactCheck(version.getTemplateId(),
                version.getVersionNo());
        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.disable(version.getTemplateId(), version.getVersionNo()));

        assertTrue(impact.isBlocked());
        assertEquals(1, impact.getImpacts().size());
        assertEquals(FormCenterErrorCode.TEMPLATE_POOL_IMPACT_BLOCKED, ex.getErrorCode());
        assertFalse(service.findVersion(version.getTemplateId(), version.getVersionNo()).isDisabled());
    }

    private static FormTemplateImportCommand command(String fileName) {
        return FormTemplateImportCommand.of("Change Form", "1.0.0", fileName, new byte[] {1, 2, 3}, "form remark");
    }

    private record FixedRecognizer(FormTemplateRecognition recognition) implements FormTemplateRecognizer {

        @Override
        public FormTemplateRecognition recognize(FormTemplateImportCommand command) {
            return recognition;
        }

    }

    private record FixedReferenceChecker(List<FormTemplateImpact> impacts) implements FormTemplateReferenceChecker {

        @Override
        public List<FormTemplateImpact> findPublishedPolicyImpacts(Long templateVersionId) {
            return impacts;
        }

    }

    private static final class MemoryTemplateVersionStore implements FormTemplateVersionStore {

        private final AtomicLong templateSequence = new AtomicLong(1);
        private final AtomicLong versionSequence = new AtomicLong(1);
        private final Map<String, FormTemplateVersion> versions = new LinkedHashMap<>();

        @Override
        public Long nextTemplateId() {
            return templateSequence.getAndIncrement();
        }

        @Override
        public Long nextVersionId() {
            return versionSequence.getAndIncrement();
        }

        @Override
        public void insert(FormTemplateVersion version) {
            versions.put(key(version.getTemplateId(), version.getVersionNo()), version);
        }

        @Override
        public void update(FormTemplateVersion version) {
            versions.put(key(version.getTemplateId(), version.getVersionNo()), version);
        }

        @Override
        public FormTemplateVersion findVersion(Long templateId, String versionNo) {
            return versions.get(key(templateId, versionNo));
        }

        private String key(Long templateId, String versionNo) {
            return templateId + ":" + versionNo;
        }

    }

}
