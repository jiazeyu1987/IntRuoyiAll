package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrix.ApprovalSourceMode;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrixService.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Calls the actual independent domain service; no Workflow/DB/personnel port is simulated. */
class DccApplicationCustomMatrixResolverTest {
    private final DccApplicationCustomMatrixService service = new DccApplicationCustomMatrixService();
    private final DccApplicationCustomMatrix actual = new DccApplicationCustomMatrix(9223372036854775702L, 9223372036854775703L);
    private final List<Long> departments = List.of(9223372036854775701L, 51L);
    private SourceContext context() { return new SourceContext(ApprovalSourceMode.APPLICATION_CUSTOM,1L,5L,2L,6L,99L,20L,3,
            "UPLOAD","scope-中文-\"\\","dcc-controlled-file-upload:6:exact-definition","a".repeat(64)); }
    private String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private void rejects(String code, String chinese, org.junit.jupiter.api.function.Executable operation) {
        var error=assertThrows(IllegalArgumentException.class,operation);
        assertTrue(error.getMessage().startsWith(code+"："),error.getMessage());
        assertTrue(error.getMessage().contains(chinese),error.getMessage());
    }

    @Test void compileUsesEveryDepartmentAndTwoExplicitActorsWithoutInventingSupervisorOrPublicIdentity() {
        var source=new ArrayList<>(departments); var nodes=service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,actual,source);
        assertEquals(List.of("MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW"),nodes.stream().map(Node::stageCode).toList());
        assertEquals(List.of(1,2,3),nodes.stream().map(Node::stageOrder).toList());
        assertEquals(List.of(51L,9223372036854775701L),nodes.get(0).candidateSourceIds());
        assertEquals("DEPT",nodes.get(0).candidateSourceType()); assertEquals("ALL",nodes.get(0).approveMethod());
        assertEquals(100,nodes.get(0).approveRatio()); assertTrue(nodes.get(0).requireAllApprovals());
        assertEquals(List.of(actual.approverUserId()),nodes.get(1).candidateSourceIds());
        assertEquals(List.of(actual.documentControlUserId()),nodes.get(2).candidateSourceIds());
        for(var node:nodes.subList(1,3)) { assertEquals("USER",node.candidateSourceType());assertEquals("ANY",node.approveMethod());assertNull(node.approveRatio());assertFalse(node.requireAllApprovals()); }
        assertEquals(departments,source); assertThrows(UnsupportedOperationException.class,()->nodes.clear());
        assertThrows(UnsupportedOperationException.class,()->nodes.get(0).candidateSourceIds().clear());
    }

    @Test void draftNullsAndEmptyDepartmentsRoundTripWithoutManufacturingReadyActors() {
        assertNull(service.validateDraft(null));
        for(List<Long> ids:Arrays.asList(null,List.<Long>of())) {
            var encoded=service.canonicalize(context(),null,ids);var saved=service.readCanonical(encoded.canonical(),encoded.digest(),context());
            assertNull(saved.applicationCustomMatrix()); assertEquals(ids,saved.selectedSignoffDepartmentIds());
        }
        var missing=new DccApplicationCustomMatrix(null,null);assertEquals(missing,service.validateDraft(missing));
        var encoded=service.canonicalize(context(),missing,departments);
        assertEquals(missing,service.readCanonical(encoded.canonical(),encoded.digest(),context()).applicationCustomMatrix());
        rejects("CUSTOM_MATRIX_APPROVER_REQUIRED","请选择本次批准人",()->service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,missing,departments));
    }

    @Test void canonicalHasExactStringLongsStableDepartmentOrderAndOwnSourceFormat() throws Exception {
        var first=service.canonicalize(context(),actual,departments);
        var second=service.canonicalize(context(),actual,List.of(51L,9223372036854775701L));
        assertEquals(first,second);assertEquals(hash(first.canonical()),first.digest());
        var restored=service.readCanonical(first.canonical(),first.digest(),context());assertEquals(actual,restored.applicationCustomMatrix());
        assertEquals(List.of(51L,9223372036854775701L),restored.selectedSignoffDepartmentIds());
        assertTrue(first.canonical().contains("\"approverUserId\":\"9223372036854775702\""));
        assertTrue(first.canonical().contains("\"approvalSourceMode\":\"APPLICATION_CUSTOM\""));
        assertTrue(first.canonical().contains(DccApplicationCustomMatrixService.FORMAT));
        assertFalse(first.canonical().contains("routeId"));assertFalse(first.canonical().contains("sourceNodeId"));
    }

    @Test void publicNullAndUnknownSourceCannotCompileOrCreateCustomSnapshot() {
        for(ApprovalSourceMode mode:Arrays.asList(null,ApprovalSourceMode.PUBLIC_MATRIX)) {
            rejects("CUSTOM_MATRIX_SOURCE_REQUIRED","当前正式类别不使用本次自定义矩阵",()->service.compile(mode,actual,departments));
            var c=context();var wrong=new SourceContext(mode,c.tenantId(),c.projectId(),c.categoryId(),c.taxonomyId(),c.requesterId(),c.controlledFileId(),c.attributeRound(),c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest());
            rejects("CUSTOM_MATRIX_SOURCE_REQUIRED","当前正式类别不使用本次自定义矩阵",()->service.canonicalize(wrong,actual,departments));
        }
    }

    @Test void missingSubmissionActorsAndEmptyRosterHaveDistinctBusinessErrors() {
        for(List<Long> empty:Arrays.asList(null,List.<Long>of())) rejects("CUSTOM_MATRIX_SIGNOFF_REQUIRED","请至少选择一个会签部门",()->service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,actual,empty));
        rejects("CUSTOM_MATRIX_APPROVER_REQUIRED","请选择本次批准人",()->service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,null,departments));
        rejects("CUSTOM_MATRIX_APPROVER_REQUIRED","请选择本次批准人",()->service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,new DccApplicationCustomMatrix(null,88L),departments));
        rejects("CUSTOM_MATRIX_DOCUMENT_CONTROL_REQUIRED","请选择本次文控审核人",()->service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,new DccApplicationCustomMatrix(88L,null),departments));
    }

    @Test void nonpositiveDuplicateAndNullIdsNeverDisappearDuringNormalization() {
        for(Long invalid:List.of(0L,-1L)) {
            rejects("CUSTOM_MATRIX_ACCOUNT_INVALID","批准人",()->service.validateDraft(new DccApplicationCustomMatrix(invalid,88L)));
            rejects("CUSTOM_MATRIX_ACCOUNT_INVALID","文控审核人",()->service.validateDraft(new DccApplicationCustomMatrix(88L,invalid)));
        }
        rejects("CUSTOM_MATRIX_DEPARTMENT_DUPLICATE","会签部门不能重复",()->service.canonicalize(context(),actual,List.of(51L,51L)));
        for(var ids:List.of(List.of(0L),List.of(-1L),Arrays.asList(51L,null))) {
            rejects("CUSTOM_MATRIX_DEPARTMENT_INVALID","会签部门身份无效",()->service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,actual,ids));
        }
    }

    @Test void everyContextIdentityRoundAndPreparedDefinitionIsBoundByRecovery() {
        var saved=service.canonicalize(context(),actual,departments);var c=context();
        var variants=new ArrayList<SourceContext>();
        for(int i=0;i<6;i++) {
            var ids=new Long[]{c.tenantId(),c.projectId(),c.categoryId(),c.taxonomyId(),c.requesterId(),c.controlledFileId()};ids[i]=ids[i]+1;
            variants.add(new SourceContext(c.approvalSourceMode(),ids[0],ids[1],ids[2],ids[3],ids[4],ids[5],c.attributeRound(),c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest()));
        }
        variants.add(new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,4,c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest()));
        variants.add(new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,"REVISION",c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest()));
        variants.add(new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,c.applicationType(),"new-scope",c.processDefinitionId(),c.definitionDigest()));
        variants.add(new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,c.applicationType(),c.applicationContextKey(),"new-exact-definition",c.definitionDigest()));
        variants.add(new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),"b".repeat(64)));
        for(var wrong:variants) {
            rejects("CUSTOM_MATRIX_CONTEXT_MISMATCH","本次申请身份或轮次不一致",()->service.readCanonical(saved.canonical(),saved.digest(),wrong));
            assertNotEquals(saved.digest(),service.canonicalize(wrong,actual,departments).digest());
        }
    }

    @Test void changedActorsRosterAndMissingOrWrongDigestCannotBecomeSameFrozenSelection() {
        var saved=service.canonicalize(context(),actual,departments);
        for(var changed:List.of(new DccApplicationCustomMatrix(100L,actual.documentControlUserId()),new DccApplicationCustomMatrix(actual.approverUserId(),100L)))
            assertNotEquals(saved.digest(),service.canonicalize(context(),changed,departments).digest());
        assertNotEquals(saved.digest(),service.canonicalize(context(),actual,List.of(51L)).digest());
        for(String digest:Arrays.asList(null,"", "bad", "b".repeat(64))) rejects("CUSTOM_MATRIX_DIGEST_INVALID","保存摘要不一致或缺失",()->service.readCanonical(saved.canonical(),digest,context()));
    }

    @Test void absentReservedIdentityBadRoundTypeDigestAndInvalidUnicodeContextAreExplicitFailures() {
        var c=context();
        for(Long id:Arrays.asList(null,0L,-1L)) {
            var wrong=new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,id,3,"UPLOAD",c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest());
            rejects("CUSTOM_MATRIX_CONTEXT_INVALID","controlledFileId",()->service.canonicalize(wrong,actual,departments));
        }
        for(var wrong:List.of(
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,0,"UPLOAD",c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest()),
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,"OBSOLETE",c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest()),
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,"UPLOAD"," \ud800",c.processDefinitionId(),c.definitionDigest()),
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,"UPLOAD",c.applicationContextKey(),c.processDefinitionId(),"bad")))
            assertThrows(IllegalArgumentException.class,()->service.canonicalize(wrong,actual,departments));
    }

    @Test void duplicateUnknownMissingTrailingAndNoncanonicalJsonRejectEvenWithMatchingHash() throws Exception {
        var saved=service.canonicalize(context(),actual,departments);String json=saved.canonical();
        var variants=List.of(" "+json,json+" {}",json.replace("\"formatVersion\":", "\"unknown\":true,\"formatVersion\":"),
                json.replace("\"formatVersion\":", "\"formatVersion\":\"DCC_APPLICATION_CUSTOM_MATRIX_V1\",\"formatVersion\":"),
                json.replace("\"approverUserId\":\"9223372036854775702\"","\"approverUserId\":9223372036854775702"),
                json.replace("\"approverUserId\":\"9223372036854775702\"","\"approverUserId\":\"9223372036854775808\""),
                json.replace("\"approverUserId\":\"9223372036854775702\",",""),
                json.replace("\"selectedSignoffDepartmentIds\":[\"51\",\"9223372036854775701\"]","\"selectedSignoffDepartmentIds\":[\"51\",\"51\"]"),
                json.replace("\"selectedSignoffDepartmentIds\":[\"51\",\"9223372036854775701\"]","\"selectedSignoffDepartmentIds\":[\"9223372036854775701\",\"51\"]"));
        for(String bad:variants) assertThrows(IllegalArgumentException.class,()->service.readCanonical(bad,hash(bad),context()),bad);
    }

    @Test void rawMatrixChangesNeverMutateSavedSelectionAndSingleSameActorIsStillTwoIndependentStages() {
        var same=new DccApplicationCustomMatrix(99L,99L);var nodes=service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,same,List.of(51L));
        assertEquals(List.of(99L),nodes.get(1).candidateSourceIds());assertEquals(List.of(99L),nodes.get(2).candidateSourceIds());
        var ids=new ArrayList<>(departments);var encoded=service.canonicalize(context(),actual,ids);ids.clear();
        var decoded=service.readCanonical(encoded.canonical(),encoded.digest(),context());assertEquals(2,decoded.selectedSignoffDepartmentIds().size());
        assertThrows(UnsupportedOperationException.class,()->decoded.selectedSignoffDepartmentIds().clear());
    }

    @Test void canonicalFieldWhitelistCoversNestedObjectsAndNullScalarsWithoutDiscardingCorruption() throws Exception {
        var encoded=service.canonicalize(context(),actual,departments);String json=encoded.canonical();
        for(String bad:List.of("null","[]","{}",
                json.replace("\"approverUserId\":", "\"oldRouteId\":\"900\",\"approverUserId\":"),
                json.replace("\"attributeRound\":3","\"attributeRound\":null"),
                json.replace("\"attributeRound\":3","\"attributeRound\":3.0"),
                json.replace("\"context\":{","\"context\":{\"processInstanceId\":\"another-round\","),
                json.replace("\"documentControlUserId\":\"9223372036854775703\"","\"documentControlUserId\":\"0\""),
                json.replace("\"selectedSignoffDepartmentIds\":[\"51\",\"9223372036854775701\"]","\"selectedSignoffDepartmentIds\":[null]")))
            assertThrows(IllegalArgumentException.class,()->service.readCanonical(bad,hash(bad),context()),bad);
    }

    @Test void reservedContextNeverUsesMissingTenantProjectCategoryTaxonomyRequesterOrPlaceholderDefinition() {
        var c=context();
        for(int i=0;i<5;i++) {
            var ids=new Long[]{c.tenantId(),c.projectId(),c.categoryId(),c.taxonomyId(),c.requesterId(),c.controlledFileId()};ids[i]=null;
            var bad=new SourceContext(c.approvalSourceMode(),ids[0],ids[1],ids[2],ids[3],ids[4],ids[5],3,"UPLOAD",c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest());
            assertThrows(IllegalArgumentException.class,()->service.canonicalize(bad,actual,departments));
        }
        for(String definition:Arrays.asList(null,""," "," exact ","exact\nnext","\ud800")) {
            var bad=new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,"UPLOAD",c.applicationContextKey(),definition,c.definitionDigest());
            rejects("CUSTOM_MATRIX_CONTEXT_INVALID","processDefinitionId",()->service.canonicalize(bad,actual,departments));
        }
        assertThrows(IllegalArgumentException.class,()->service.canonicalize(null,actual,departments));
    }

    @Test void partialSingleAccountDraftPreservesMissingOtherAccountAndEmptyIsDifferentFromNull() {
        var one=new DccApplicationCustomMatrix(88L,null);
        var encoded=service.canonicalize(context(),one,List.of());
        assertEquals(one,service.readCanonical(encoded.canonical(),encoded.digest(),context()).applicationCustomMatrix());
        assertNotEquals(encoded.digest(),service.canonicalize(context(),one,null).digest());
        assertNotEquals(service.canonicalize(context(),null,List.of()).digest(),service.canonicalize(context(),new DccApplicationCustomMatrix(null,null),List.of()).digest());
        rejects("CUSTOM_MATRIX_DOCUMENT_CONTROL_REQUIRED","请选择本次文控审核人",()->service.compile(ApprovalSourceMode.APPLICATION_CUSTOM,one,List.of(51L)));
    }

    @Test void maxLongAndUnicodeContextRoundTripWithoutUsingApplicationDefaultSerializer() {
        var c=context();var exact=new SourceContext(c.approvalSourceMode(),Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,
                Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Integer.MAX_VALUE,"REVISION","精确🙂\\scope\"",
                "dcc-controlled-file-revision:6:exact","c".repeat(64));
        var value=new DccApplicationCustomMatrix(Long.MAX_VALUE,Long.MAX_VALUE);
        var encoded=service.canonicalize(exact,value,List.of(Long.MAX_VALUE));
        var restored=service.readCanonical(encoded.canonical(),encoded.digest(),exact);
        assertEquals(value,restored.applicationCustomMatrix());assertEquals(List.of(Long.MAX_VALUE),restored.selectedSignoffDepartmentIds());
        assertEquals(encoded,service.canonicalize(exact,restored.applicationCustomMatrix(),restored.selectedSignoffDepartmentIds()));
    }

    @Test void malformedCanonicalAndUppercaseDigestHaveNoSilentRecovery() {
        var encoded=service.canonicalize(context(),actual,departments);
        rejects("CUSTOM_MATRIX_CANONICAL_INVALID","保存原配置缺失",()->service.readCanonical(null,encoded.digest(),context()));
        rejects("CUSTOM_MATRIX_CANONICAL_INVALID","保存原配置缺失",()->service.readCanonical("",encoded.digest(),context()));
        rejects("CUSTOM_MATRIX_DIGEST_INVALID","保存摘要不一致或缺失",()->service.readCanonical(encoded.canonical(),encoded.digest().toUpperCase(Locale.ROOT),context()));
    }
    private PreviewSourceContext preview() {
        var c=context();return new PreviewSourceContext(c.approvalSourceMode(),c.tenantId(),c.projectId(),c.categoryId(),c.taxonomyId(),
                c.requesterId(),c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest());
    }
    @Test void unallocatedPreviewIsItsOwnExactShapeWithoutPlaceholderFileRoundOrPublicRoute() {
        var result=service.canonicalizePreview(preview(),actual,departments);
        assertTrue(result.canonical().contains(DccApplicationCustomMatrixService.PREVIEW_FORMAT));
        assertFalse(result.canonical().contains("controlledFileId"));assertFalse(result.canonical().contains("attributeRound"));
        assertFalse(result.canonical().contains("routeId"));assertFalse(result.canonical().contains("sourceNodeId"));
        assertEquals(actual,service.readPreviewCanonical(result.canonical(),result.digest(),preview()).applicationCustomMatrix());
        assertEquals(List.of(51L,9223372036854775701L),service.readPreviewCanonical(result.canonical(),result.digest(),preview()).selectedSignoffDepartmentIds());
        assertEquals(result,service.canonicalizePreview(preview(),actual,List.of(51L,9223372036854775701L)));
    }
    @Test void bindingPreviewToRealReservedIdentityProducesDifferentFrozenDigestAndBothFormatsAreNotInterchangeable() {
        var p=service.canonicalizePreview(preview(),actual,departments);
        var reserved=service.bindPreviewToReserved(p.canonical(),p.digest(),preview(),context());
        assertEquals(service.canonicalize(context(),actual,departments),reserved);assertNotEquals(p.digest(),reserved.digest());
        assertEquals(actual,service.readCanonical(reserved.canonical(),reserved.digest(),context()).applicationCustomMatrix());
        assertThrows(IllegalArgumentException.class,()->service.readCanonical(p.canonical(),p.digest(),context()));
        assertThrows(IllegalArgumentException.class,()->service.readPreviewCanonical(reserved.canonical(),reserved.digest(),preview()));
    }
    @Test void bindRejectsEveryChangedSharedIdentityAndNeverUsesDefaultReservedIdOrRound() {
        var p=service.canonicalizePreview(preview(),actual,departments);var c=context();
        for(int index=0;index<5;index++) {
            var ids=new Long[]{c.tenantId(),c.projectId(),c.categoryId(),c.taxonomyId(),c.requesterId()};ids[index]++;
            var changed=new SourceContext(c.approvalSourceMode(),ids[0],ids[1],ids[2],ids[3],ids[4],20L,3,c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest());
            rejects("CUSTOM_MATRIX_CONTEXT_MISMATCH","预检与本次已保留申请身份不一致",()->service.bindPreviewToReserved(p.canonical(),p.digest(),preview(),changed));
        }
        for(var changed:List.of(
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,"REVISION",c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest()),
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,c.applicationType(),"another-context",c.processDefinitionId(),c.definitionDigest()),
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,c.applicationType(),c.applicationContextKey(),"different-definition",c.definitionDigest()),
                new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,20L,3,c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),"b".repeat(64))))
            rejects("CUSTOM_MATRIX_CONTEXT_MISMATCH","预检与本次已保留申请身份不一致",()->service.bindPreviewToReserved(p.canonical(),p.digest(),preview(),changed));
        var unallocated=new SourceContext(c.approvalSourceMode(),1L,5L,2L,6L,99L,null,null,"UPLOAD",c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest());
        assertThrows(IllegalArgumentException.class,()->service.bindPreviewToReserved(p.canonical(),p.digest(),preview(),unallocated));
    }
    @Test void previewCorruptionExtraReservedFieldsAndChangedActorsCannotReuseSelectedDigest() throws Exception {
        var p=service.canonicalizePreview(preview(),actual,departments);String raw=p.canonical();
        for(String bad:List.of(raw.replace("\"context\":{","\"context\":{\"controlledFileId\":\"20\","),
                raw.replace("\"context\":{","\"context\":{\"attributeRound\":3,"),raw+" {}"))
            assertThrows(IllegalArgumentException.class,()->service.readPreviewCanonical(bad,hash(bad),preview()));
        var changed=service.canonicalizePreview(preview(),new DccApplicationCustomMatrix(100L,actual.documentControlUserId()),departments);
        assertNotEquals(p.digest(),changed.digest());
        rejects("CUSTOM_MATRIX_DIGEST_INVALID","保存摘要不一致或缺失",()->service.readPreviewCanonical(changed.canonical(),p.digest(),preview()));
    }
    @Test void missingPreviewIdentityModeAndExactDefinitionRejectWithoutInventingReservedApplication() {
        var p=preview();
        for(var invalid:List.of(
                new PreviewSourceContext(ApprovalSourceMode.PUBLIC_MATRIX,1L,5L,2L,6L,99L,"UPLOAD",p.applicationContextKey(),p.processDefinitionId(),p.definitionDigest()),
                new PreviewSourceContext(p.approvalSourceMode(),1L,5L,2L,6L,null,"UPLOAD",p.applicationContextKey(),p.processDefinitionId(),p.definitionDigest()),
                new PreviewSourceContext(p.approvalSourceMode(),1L,5L,2L,6L,99L,"UPLOAD",p.applicationContextKey(),null,p.definitionDigest())))
            assertThrows(IllegalArgumentException.class,()->service.canonicalizePreview(invalid,actual,departments));
        assertThrows(IllegalArgumentException.class,()->service.canonicalizePreview(null,actual,departments));
    }
    @Test void previewDraftNullsStayUnfilledAndDifferentScopeCannotReadAnEarlierPreview() {
        var empty=service.canonicalizePreview(preview(),null,null);
        var restored=service.readPreviewCanonical(empty.canonical(),empty.digest(),preview());
        assertNull(restored.applicationCustomMatrix());assertNull(restored.selectedSignoffDepartmentIds());
        var p=preview();var changed=new PreviewSourceContext(p.approvalSourceMode(),p.tenantId(),p.projectId(),p.categoryId(),p.taxonomyId(),
                p.requesterId(),p.applicationType(),"new-scope",p.processDefinitionId(),p.definitionDigest());
        rejects("CUSTOM_MATRIX_CONTEXT_MISMATCH","预检身份或正式定义不一致",()->service.readPreviewCanonical(empty.canonical(),empty.digest(),changed));
    }
    @Test void previewReservedAndReworkNewReservedContextsHaveDistinctDigestsAndNoInheritedRound() {
        var p=service.canonicalizePreview(preview(),actual,departments);
        var first=service.bindPreviewToReserved(p.canonical(),p.digest(),preview(),context());var c=context();
        var newRound=new SourceContext(c.approvalSourceMode(),c.tenantId(),c.projectId(),c.categoryId(),c.taxonomyId(),c.requesterId(),
                21L,4,c.applicationType(),c.applicationContextKey(),c.processDefinitionId(),c.definitionDigest());
        var next=service.bindPreviewToReserved(p.canonical(),p.digest(),preview(),newRound);
        assertNotEquals(first.digest(),next.digest());
        assertEquals(actual,service.readCanonical(next.canonical(),next.digest(),newRound).applicationCustomMatrix());
        rejects("CUSTOM_MATRIX_CONTEXT_MISMATCH","申请身份或轮次不一致",()->service.readCanonical(first.canonical(),first.digest(),newRound));
    }
}
