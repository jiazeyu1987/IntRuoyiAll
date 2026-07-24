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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernanceCsvPackageServiceTest {

    private final SignatureGovernanceCsvService service = new SignatureGovernanceCsvServiceImpl();

    @Test
    void evaluatePackage_blocksMissingRequiredQualityPackageInputsWithTypedBlockers() {
        SignatureGovernanceCsvPackageCommand command = new SignatureGovernanceCsvPackageCommand(
                "csv-pkg-1", null, List.of(), List.of(), List.of(), List.of(),
                null, "recovery-evidence-1", true);

        SignatureGovernanceCsvPackageResult packageResult = service.evaluatePackage(command);
        SignatureGovernanceCsvReleaseGateResult gateResult = service.evaluateReleaseGate(
                new SignatureGovernanceCsvReleaseGateCommand("release-20260528", command));

        assertEquals(SignatureGovernanceCsvPackageStatus.BLOCKED, packageResult.status());
        assertNotEquals(SignatureGovernanceCsvReleaseGateStatus.GO, gateResult.status());
        assertBlockerCodes(packageResult.blockers(),
                SignatureGovernanceCsvBlockerCode.QUALITY_OWNER_MISSING,
                SignatureGovernanceCsvBlockerCode.URS_MISSING,
                SignatureGovernanceCsvBlockerCode.FRS_MISSING,
                SignatureGovernanceCsvBlockerCode.RISK_ASSESSMENT_MISSING,
                SignatureGovernanceCsvBlockerCode.IQ_MISSING,
                SignatureGovernanceCsvBlockerCode.OQ_MISSING,
                SignatureGovernanceCsvBlockerCode.PQ_MISSING,
                SignatureGovernanceCsvBlockerCode.TRACE_MATRIX_MISSING,
                SignatureGovernanceCsvBlockerCode.ELECTRONIC_SIGNATURE_SOP_MISSING,
                SignatureGovernanceCsvBlockerCode.TRAINING_RECORD_MISSING,
                SignatureGovernanceCsvBlockerCode.CHANGE_CONTROL_MISSING,
                SignatureGovernanceCsvBlockerCode.QA_APPROVAL_MISSING);
    }

    @Test
    void evaluatePackage_readyAndReleaseGateGoWhenQualityPackageIsComplete() {
        SignatureGovernanceCsvPackageCommand command = completeCommand(approvedTraceRelation());

        SignatureGovernanceCsvPackageResult packageResult = service.evaluatePackage(command);
        SignatureGovernanceCsvReleaseGateResult gateResult = service.evaluateReleaseGate(
                new SignatureGovernanceCsvReleaseGateCommand("release-20260528", command));

        assertEquals(SignatureGovernanceCsvPackageStatus.READY, packageResult.status());
        assertEquals(SignatureGovernanceCsvReleaseGateStatus.GO, gateResult.status());
        assertTrue(packageResult.blockers().isEmpty());
        assertEquals(SignatureGovernanceCsvMaterialType.requiredMaterials(), packageResult.materialTypes());
        assertEquals(1, packageResult.traceRelations().size());
        SignatureGovernanceCsvTraceRelation traceRow = packageResult.traceRelations().get(0);
        assertEquals("REQ-CSV-001", traceRow.requirementRef());
        assertEquals("DESIGN-CSV-001", traceRow.designRef());
        assertEquals("TEST-CSV-001", traceRow.testRef());
        assertEquals("EVIDENCE-CSV-001", traceRow.evidenceRef());
        assertEquals("qa-owner-1", traceRow.owner());
        assertEquals("NO_OPEN_BLOCKER", traceRow.blockerRef());
        assertEquals("quality-approval-ref-1", traceRow.qualityApprovalRef());
    }

    @Test
    void evaluatePackage_blocksWhenTraceMatrixRowMissesAnyRequiredLink() {
        for (TraceRelationCase relationCase : traceRelationsMissingOneRequiredLink()) {
            SignatureGovernanceCsvPackageCommand command = completeCommand(relationCase.relation());

            SignatureGovernanceCsvPackageResult result = service.evaluatePackage(command);

            assertEquals(SignatureGovernanceCsvPackageStatus.BLOCKED, result.status(), relationCase.label());
            assertBlockerCodes(result.blockers(), SignatureGovernanceCsvBlockerCode.TRACE_RELATION_MISSING);
        }
    }

    @Test
    void evaluatePackageReturnsImmutableCollections() {
        SignatureGovernanceCsvPackageCommand command = completeCommand(approvedTraceRelation());
        SignatureGovernanceCsvPackageResult packageResult = service.evaluatePackage(command);
        SignatureGovernanceCsvReleaseGateResult gateResult = service.evaluateReleaseGate(
                new SignatureGovernanceCsvReleaseGateCommand("release-20260528", command));

        assertThrows(UnsupportedOperationException.class, () -> packageResult.materialTypes().clear());
        assertThrows(UnsupportedOperationException.class, () -> packageResult.traceRelations().add(approvedTraceRelation()));
        assertThrows(UnsupportedOperationException.class, () -> packageResult.blockers().add(testBlocker()));
        assertThrows(UnsupportedOperationException.class, () -> gateResult.blockers().add(testBlocker()));
    }

    private static SignatureGovernanceCsvPackageCommand completeCommand(SignatureGovernanceCsvTraceRelation traceRelation) {
        return new SignatureGovernanceCsvPackageCommand("csv-pkg-1", "qa-owner-1", approvedMaterials(),
                List.of(traceRelation), List.of(approvedTrainingRecord()), List.of(approvedChangeControl()),
                approvedQaApproval(), "recovery-evidence-1", true);
    }

    private static List<SignatureGovernanceCsvMaterial> approvedMaterials() {
        return SignatureGovernanceCsvMaterialType.requiredMaterials().stream()
                .map(SignatureGovernanceCsvPackageServiceTest::approvedMaterial)
                .toList();
    }

    private static SignatureGovernanceCsvMaterial approvedMaterial(SignatureGovernanceCsvMaterialType type) {
        return new SignatureGovernanceCsvMaterial(type, type.name().toLowerCase() + "-doc",
                "v1.0", SignatureGovernanceCsvMaterialStatus.APPROVED, "qa-owner-1",
                List.of("reviewer-1"), List.of("approver-1"), type.name().toLowerCase() + "-evidence",
                "cc-20260528", "ELECTRONIC_SIGNATURE_GOVERNANCE_APPROVAL");
    }

    private static List<TraceRelationCase> traceRelationsMissingOneRequiredLink() {
        return List.of(
                new TraceRelationCase("missing requirement",
                        traceRelation(null, "DESIGN-CSV-001", "TEST-CSV-001", "EVIDENCE-CSV-001", "qa-owner-1",
                                "NO_OPEN_BLOCKER", "quality-approval-ref-1")),
                new TraceRelationCase("missing design",
                        traceRelation("REQ-CSV-001", null, "TEST-CSV-001", "EVIDENCE-CSV-001", "qa-owner-1",
                                "NO_OPEN_BLOCKER", "quality-approval-ref-1")),
                new TraceRelationCase("missing test",
                        traceRelation("REQ-CSV-001", "DESIGN-CSV-001", null, "EVIDENCE-CSV-001", "qa-owner-1",
                                "NO_OPEN_BLOCKER", "quality-approval-ref-1")),
                new TraceRelationCase("missing evidence",
                        traceRelation("REQ-CSV-001", "DESIGN-CSV-001", "TEST-CSV-001", null, "qa-owner-1",
                                "NO_OPEN_BLOCKER", "quality-approval-ref-1")),
                new TraceRelationCase("missing owner",
                        traceRelation("REQ-CSV-001", "DESIGN-CSV-001", "TEST-CSV-001", "EVIDENCE-CSV-001", null,
                                "NO_OPEN_BLOCKER", "quality-approval-ref-1")),
                new TraceRelationCase("missing blocker",
                        traceRelation("REQ-CSV-001", "DESIGN-CSV-001", "TEST-CSV-001", "EVIDENCE-CSV-001",
                                "qa-owner-1", null, "quality-approval-ref-1")),
                new TraceRelationCase("missing quality approval",
                        traceRelation("REQ-CSV-001", "DESIGN-CSV-001", "TEST-CSV-001", "EVIDENCE-CSV-001",
                                "qa-owner-1", "NO_OPEN_BLOCKER", null)));
    }

    private static SignatureGovernanceCsvTraceRelation approvedTraceRelation() {
        return traceRelation("REQ-CSV-001", "DESIGN-CSV-001", "TEST-CSV-001", "EVIDENCE-CSV-001",
                "qa-owner-1", "NO_OPEN_BLOCKER", "quality-approval-ref-1");
    }

    private static SignatureGovernanceCsvTraceRelation traceRelation(String requirementRef, String designRef,
                                                                     String testRef, String evidenceRef, String owner,
                                                                     String blockerRef, String qualityApprovalRef) {
        return new SignatureGovernanceCsvTraceRelation(requirementRef, designRef, testRef, evidenceRef, owner,
                SignatureGovernanceCsvMaterialStatus.APPROVED, blockerRef, qualityApprovalRef);
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

    private static SignatureGovernanceCsvBlocker testBlocker() {
        return SignatureGovernanceCsvBlocker.of(SignatureGovernanceCsvBlockerCode.TRACE_RELATION_MISSING,
                "Trace relation is missing", "Release gate must stay blocked.");
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

    private record TraceRelationCase(String label, SignatureGovernanceCsvTraceRelation relation) {
    }
}
