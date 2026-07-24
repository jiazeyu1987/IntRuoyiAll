package cn.iocoder.yudao.module.dcc.signature.csv;

import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvBlocker;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvChangeControl;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvMaterial;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvMaterialStatus;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvMaterialType;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvPackageCommand;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvPackageResult;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvPackageStatus;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvQaApproval;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvReleaseGateCommand;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvReleaseGateResult;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvReleaseGateStatus;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvService;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvServiceImpl;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvTraceRelation;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvTrainingRecord;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernanceCsvServiceTest {

    private final SignatureGovernanceCsvService service = new SignatureGovernanceCsvServiceImpl();

    @Test
    void evaluatePackage_blocksMissingOwnerMaterialsTrainingChangeControlTraceAndQaApproval() {
        SignatureGovernanceCsvPackageCommand command = new SignatureGovernanceCsvPackageCommand(
                "csv-pkg-1", null, List.of(), List.of(), List.of(), List.of(),
                null, null, true);

        SignatureGovernanceCsvPackageResult result = service.evaluatePackage(command);

        assertEquals(SignatureGovernanceCsvPackageStatus.BLOCKED, result.status());
        assertBlockerCodes(result.blockers(),
                SignatureGovernanceCsvBlockerCode.QUALITY_OWNER_MISSING,
                SignatureGovernanceCsvBlockerCode.URS_MISSING,
                SignatureGovernanceCsvBlockerCode.FRS_MISSING,
                SignatureGovernanceCsvBlockerCode.RISK_ASSESSMENT_MISSING,
                SignatureGovernanceCsvBlockerCode.IQ_MISSING,
                SignatureGovernanceCsvBlockerCode.OQ_MISSING,
                SignatureGovernanceCsvBlockerCode.PQ_MISSING,
                SignatureGovernanceCsvBlockerCode.TRACE_MATRIX_MISSING,
                SignatureGovernanceCsvBlockerCode.ELECTRONIC_SIGNATURE_SOP_MISSING,
                SignatureGovernanceCsvBlockerCode.EVIDENCE_INDEX_MISSING,
                SignatureGovernanceCsvBlockerCode.TRACE_RELATION_MISSING,
                SignatureGovernanceCsvBlockerCode.TRAINING_RECORD_MISSING,
                SignatureGovernanceCsvBlockerCode.CHANGE_CONTROL_MISSING,
                SignatureGovernanceCsvBlockerCode.QA_APPROVAL_MISSING,
                SignatureGovernanceCsvBlockerCode.RECOVERY_EVIDENCE_MISSING);
        assertTrue(result.blockers().stream().allMatch(blocker -> !blocker.message().isBlank()));
        assertTrue(result.blockers().stream().allMatch(blocker -> !blocker.impact().isBlank()));
    }

    @Test
    void evaluatePackage_blocksEveryMissingRequiredMaterialWithTypedCode() {
        for (SignatureGovernanceCsvMaterialType missingType : SignatureGovernanceCsvMaterialType.requiredMaterials()) {
            SignatureGovernanceCsvPackageCommand command = completeCommandWithout(missingType);

            SignatureGovernanceCsvPackageResult result = service.evaluatePackage(command);

            assertEquals(SignatureGovernanceCsvPackageStatus.BLOCKED, result.status(), missingType.name());
            assertBlockerCodes(result.blockers(), missingMaterialCode(missingType));
        }
    }

    @Test
    void evaluateReleaseGate_blocksEngineeringVerificationWithoutQaApproval() {
        SignatureGovernanceCsvPackageCommand command = completeCommand(null, true);

        SignatureGovernanceCsvReleaseGateResult gate = service.evaluateReleaseGate(
                new SignatureGovernanceCsvReleaseGateCommand("release-20260528", command));

        assertEquals(SignatureGovernanceCsvReleaseGateStatus.BLOCKED, gate.status());
        assertFalse(gate.qaApproved());
        assertTrue(gate.engineeringVerificationPassed());
        assertBlockerCodes(gate.blockers(), SignatureGovernanceCsvBlockerCode.QA_APPROVAL_MISSING);
    }

    @Test
    void evaluatePackage_blocksMissingTraceRelationEvenWhenQualityMaterialsAreApproved() {
        SignatureGovernanceCsvPackageCommand command = new SignatureGovernanceCsvPackageCommand(
                "csv-pkg-1", "qa-owner-1", approvedMaterials(), List.of(),
                List.of(approvedTrainingRecord()), List.of(approvedChangeControl()),
                approvedQaApproval(), "recovery-evidence-1", true);

        SignatureGovernanceCsvPackageResult result = service.evaluatePackage(command);

        assertEquals(SignatureGovernanceCsvPackageStatus.BLOCKED, result.status());
        assertBlockerCodes(result.blockers(), SignatureGovernanceCsvBlockerCode.TRACE_RELATION_MISSING);
    }

    @Test
    void evaluateReleaseGate_goOnlyWhenQualityPackageIsCompleteAndQaApproved() {
        SignatureGovernanceCsvPackageCommand command = completeCommand(approvedQaApproval(), true);

        SignatureGovernanceCsvPackageResult packageResult = service.evaluatePackage(command);
        SignatureGovernanceCsvReleaseGateResult gate = service.evaluateReleaseGate(
                new SignatureGovernanceCsvReleaseGateCommand("release-20260528", command));

        assertEquals(SignatureGovernanceCsvPackageStatus.READY, packageResult.status());
        assertTrue(packageResult.blockers().isEmpty());
        assertEquals(SignatureGovernanceCsvReleaseGateStatus.GO, gate.status());
        assertTrue(gate.blockers().isEmpty());
        assertTrue(gate.qaApproved());
        assertEquals(SignatureGovernanceCsvMaterialType.requiredMaterials(), packageResult.materialTypes());
        assertEquals("quality-approval-ref-1", packageResult.traceRelations().get(0).qualityApprovalRef());
        assertEquals("NO_OPEN_BLOCKER", packageResult.traceRelations().get(0).blockerRef());
    }

    private static SignatureGovernanceCsvPackageCommand completeCommand(SignatureGovernanceCsvQaApproval qaApproval,
                                                                        boolean engineeringVerificationPassed) {
        return new SignatureGovernanceCsvPackageCommand("csv-pkg-1", "qa-owner-1", approvedMaterials(),
                List.of(approvedTraceRelation()), List.of(approvedTrainingRecord()), List.of(approvedChangeControl()),
                qaApproval, "recovery-evidence-1", engineeringVerificationPassed);
    }

    private static SignatureGovernanceCsvPackageCommand completeCommandWithout(
            SignatureGovernanceCsvMaterialType missingType) {
        List<SignatureGovernanceCsvMaterial> materials = approvedMaterials().stream()
                .filter(material -> material.type() != missingType)
                .toList();
        return new SignatureGovernanceCsvPackageCommand("csv-pkg-1", "qa-owner-1", materials,
                List.of(approvedTraceRelation()), List.of(approvedTrainingRecord()), List.of(approvedChangeControl()),
                approvedQaApproval(), "recovery-evidence-1", true);
    }

    private static List<SignatureGovernanceCsvMaterial> approvedMaterials() {
        return SignatureGovernanceCsvMaterialType.requiredMaterials().stream()
                .map(SignatureGovernanceCsvServiceTest::approvedMaterial)
                .toList();
    }

    private static SignatureGovernanceCsvMaterial approvedMaterial(SignatureGovernanceCsvMaterialType type) {
        return new SignatureGovernanceCsvMaterial(type, type.name().toLowerCase() + "-doc",
                "v1.0", SignatureGovernanceCsvMaterialStatus.APPROVED, "qa-owner-1",
                List.of("reviewer-1"), List.of("approver-1"), type.name().toLowerCase() + "-evidence",
                "cc-20260528", "ELECTRONIC_SIGNATURE_GOVERNANCE_APPROVAL");
    }

    private static SignatureGovernanceCsvTraceRelation approvedTraceRelation() {
        return new SignatureGovernanceCsvTraceRelation("URS-001", "FRS-001", "IQ-001",
                "evidence-001", "qa-owner-1", SignatureGovernanceCsvMaterialStatus.APPROVED,
                "NO_OPEN_BLOCKER", "quality-approval-ref-1");
    }

    private static SignatureGovernanceCsvTrainingRecord approvedTrainingRecord() {
        return new SignatureGovernanceCsvTrainingRecord("training-1", "user-1",
                "electronic-signature-sop-doc", "training-evidence-1", true);
    }

    private static SignatureGovernanceCsvChangeControl approvedChangeControl() {
        return new SignatureGovernanceCsvChangeControl("cc-20260528", SignatureGovernanceCsvMaterialStatus.APPROVED,
                "change-evidence-1");
    }

    private static SignatureGovernanceCsvQaApproval approvedQaApproval() {
        return new SignatureGovernanceCsvQaApproval("quality-approval-ref-1", "qa-approver-1",
                SignatureGovernanceCsvMaterialStatus.APPROVED, "qa-signature-evidence-1");
    }

    private static SignatureGovernanceCsvBlockerCode missingMaterialCode(SignatureGovernanceCsvMaterialType type) {
        return switch (type) {
            case URS -> SignatureGovernanceCsvBlockerCode.URS_MISSING;
            case FRS -> SignatureGovernanceCsvBlockerCode.FRS_MISSING;
            case RISK_ASSESSMENT -> SignatureGovernanceCsvBlockerCode.RISK_ASSESSMENT_MISSING;
            case IQ -> SignatureGovernanceCsvBlockerCode.IQ_MISSING;
            case OQ -> SignatureGovernanceCsvBlockerCode.OQ_MISSING;
            case PQ -> SignatureGovernanceCsvBlockerCode.PQ_MISSING;
            case TRACE_MATRIX -> SignatureGovernanceCsvBlockerCode.TRACE_MATRIX_MISSING;
            case ELECTRONIC_SIGNATURE_SOP -> SignatureGovernanceCsvBlockerCode.ELECTRONIC_SIGNATURE_SOP_MISSING;
            case EVIDENCE_INDEX -> SignatureGovernanceCsvBlockerCode.EVIDENCE_INDEX_MISSING;
        };
    }

    private static void assertBlockerCodes(List<SignatureGovernanceCsvBlocker> blockers,
                                           SignatureGovernanceCsvBlockerCode... expectedCodes) {
        Set<SignatureGovernanceCsvBlockerCode> actual = blockers.stream()
                .map(SignatureGovernanceCsvBlocker::code)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SignatureGovernanceCsvBlockerCode.class)));
        for (SignatureGovernanceCsvBlockerCode expectedCode : expectedCodes) {
            assertTrue(actual.contains(expectedCode), () -> "Missing blocker code " + expectedCode + " in " + actual);
        }
    }
}
