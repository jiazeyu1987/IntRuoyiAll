package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.bpm.approval.content.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseCheckItemMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static cn.iocoder.yudao.module.mes.approval.content.EdhrReleaseContentFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real projection/validators/precheck reader. Owner provenance and file APIs are explicit boundary doubles. */
class EdhrReleaseContentResolverTest {
    @Test void fourQuadrantsProjectExactOwnerReceiptAndFourVerifiedFileContents() throws Exception {
        for(boolean active:new boolean[]{true,false})for(boolean managed:new boolean[]{true,false}){
            var f=new Fixture(active,managed);var result=f.resolver.prepare(2L,"EDHR_WORK_TASK","10",null);
            assertEquals(ApprovalContentJson.canonical(f.root),result.canonicalBusinessContentJson());
            assertEquals(ApprovalContentJson.hash(f.root),result.contentHash());
            assertEquals(f.semantic.subjectVersion(f.root,guard()),result.subjectVersion());
            assertEquals(4,result.attachments().size());assertEquals(List.of("APPROVE"),result.allowedActions());
            if(active){verify(f.completion).requireVerifiedByReceiptId(80L,1L);verifyNoInteractions(f.independent);}
            else{verify(f.independent).getCreationVerifiedByReceiptId(1L,"independent-80","MANUAL",H);verifyNoInteractions(f.completion);}
            verify(f.rows).selectCurrentOpen(20L,false);
            for(int i=0;i<4;i++)verify(f.files).getFileContent(1L,"report-"+i);
            assertEquals(result.contentHash(),f.resolver.lockAndAuthorize(2L,result.identity()).contentHash());
            verify(f.rows).selectCurrentOpen(20L,true);
            verify(f.releases).readForApproval(10L,2L,true);
        }
    }
    @Test void changedFilesAndChangedMaterialManifestAreRejected() throws Exception {
        var f=new Fixture(true,false);when(f.files.getFileContent(1L,"report-0")).thenReturn("tampered".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var tampered=f;assertThrows(IllegalArgumentException.class,()->tampered.resolver.prepare(2L,"EDHR_WORK_TASK","10",null));
        f=new Fixture(false,true);when(f.materials.evaluate(30L)).thenReturn(new MesProEdhrFourMaterialGateResult("MATERIALS_READY",true,"b".repeat(64),f.attachments));
        var changed=f;assertThrows(IllegalArgumentException.class,()->changed.resolver.prepare(2L,"EDHR_WORK_TASK","10",null));
    }
    @Test void currentRowsMustMatchFrozenSnapshotAndPersistedCounts() throws Exception {
        var f=new Fixture(true,false);f.checks.get(0).setCheckResult("FAIL");var changed=f;
        assertThrows(IllegalArgumentException.class,()->changed.resolver.prepare(2L,"EDHR_WORK_TASK","10",null));
        f=new Fixture(true,false);f.release.setRequiredCheckCount(9);var wrongCount=f;
        assertThrows(IllegalArgumentException.class,()->wrongCount.resolver.prepare(2L,"EDHR_WORK_TASK","10",null));
    }
    @Test void unregisteredScopeAndDifferentTaskIdentityCannotBePrepared() throws Exception {
        var f=new Fixture(true,false);f.task.setResponsibilityScopeJson("{}");
        assertThrows(IllegalArgumentException.class,()->f.resolver.prepare(2L,"EDHR_WORK_TASK","10",null));
        assertThrows(IllegalArgumentException.class,()->f.resolver.prepare(2L,"EDHR_WORK_TASK","010",null));
        assertThrows(IllegalArgumentException.class,()->f.resolver.prepare(2L,"EDHR_WORK_TASK","10","bpm-1"));
    }
    @Test void formalLargeLongStringsAndIntegralIdsProjectWithoutPrecisionLoss() throws Exception {
        var f=new Fixture(true,false);
        var fact=f.completion.requireVerifiedByReceiptId(80L,1L);
        fact.receipt().setBatchRecordSourceIdsJson("[\"9223372036854775807\",82]");
        fact.receipt().setProcessInspectionSourceIdsJson("[\"9007199254740992\"]");
        var result=f.resolver.prepare(2L,"EDHR_WORK_TASK","10",null);
        var projected=ApprovalContentJson.parse(result.canonicalBusinessContentJson());
        assertEquals(a("9223372036854775807","82"),projected.at("/finalization/completionBackfillReceipt/batchRecordSourceIds"));
        assertEquals(a("9007199254740992"),projected.at("/finalization/completionBackfillReceipt/processInspectionSourceIds"));
        assertEquals(ApprovalContentJson.hash(projected),result.contentHash());
        assertEquals(H,projected.at("/finalization/completionBackfillReceipt/receiptHash").asText());
        assertEquals(FILE_HASH,projected.at("/finalization/materialManifest/materials/0/sha256").asText());
    }
    @Test void malformedFormalSourceIdArraysFailActualResolver() throws Exception {
        for(String raw:new String[]{"[\"0\"]","[\"01\"]","[\"-1\"]","[\"1.0\"]","[\"\"]","[\" 1\"]","[\"9223372036854775808\"]","[-1]","[0]","[1.5]","[9223372036854775808]","[null]","[true]","[{}]","{}","[]"}){
            var f=new Fixture(true,false);f.completion.requireVerifiedByReceiptId(80L,1L).receipt().setBatchRecordSourceIdsJson(raw);
            assertThrows(IllegalArgumentException.class,()->f.resolver.prepare(2L,"EDHR_WORK_TASK","10",null),raw);
        }
    }
    private static final class Fixture {
        final ObjectNode root;
        final MesProEdhrReleaseService releases=mock(MesProEdhrReleaseService.class);
        final MesProEdhrReleaseCheckItemMapper rows=mock(MesProEdhrReleaseCheckItemMapper.class);
        final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completion=mock(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class);
        final MesIndependentBatchPrerequisiteReceiptService independent=mock(MesIndependentBatchPrerequisiteReceiptService.class);
        final MesProEdhrFourMaterialGateService materials=mock(MesProEdhrFourMaterialGateService.class);
        final FileService files=mock(FileService.class);
        final EdhrReleaseContentSemanticValidator semantic=new EdhrReleaseContentSemanticValidator(new EdhrReleaseContentPolicy());
        final List<MesProEdhrReleaseCheckItemDO> checks=new ArrayList<>();
        final List<MesProBatchRecordExecutionAttachmentDO> attachments=new ArrayList<>();
        final MesProEdhrReleaseTransactionDO release;
        final MesProEdhrWorkTaskDO task;
        final EdhrReleaseContentResolver resolver;
        Fixture(boolean active,boolean managed) throws Exception {
            root=content(active,managed);
            var t=root.get("workTask").deepCopy();((ObjectNode)t).put("taskType","RELEASE_APPROVE");
            ((ObjectNode)t).put("candidateSourceType",managed?"ROLE_GROUP":"USER").put("candidateSourceId",2L).put("candidateUserSnapshot","2,3")
                .put("responsibilitySourceType","ROLE").put("responsibilitySourceKey",managed?"MES_MANAGEMENT_REPRESENTATIVE":"QA").put("responsibilitySourceVersion",H).put("responsibilitySourceDigest",H);
            task=map(t,MesProEdhrWorkTaskDO.class);
            var r=(ObjectNode)root.get("release").deepCopy();
            r.put("batchExecutionId",30L).put("workOrderId",70L).put("batchExecutionCode","EXEC-30").put("batchCode","LOT-30").put("workOrderCode","WO-70")
                .put("productId",92L).put("productCode","PRODUCT-92").put("productName","device").put("routeId",90L).put("routeCode","ROUTE-90").put("routeName","device route")
                .put("requiredCheckCount",10).put("failedCheckCount",0).put("blockingCheckCount",0);
            release=map(r,MesProEdhrReleaseTransactionDO.class);release.setLastPrecheckAt(LocalDateTime.of(2026,10,10,10,0));
            var rawItems=a();int index=0;
            for(String code:CHECKS){
                checks.add(map(o("id",100+index++,"releaseTransactionId",20,"checkCode",code,"checkResult","PASS","severity","INFO","responsibilityModule","MES","sourceObjectCode","report-1","sourceObjectType","REPORT","sourceObjectId","report-1","evidenceHash",H),MesProEdhrReleaseCheckItemDO.class));
                checks.get(checks.size()-1).setCheckedAt(release.getLastPrecheckAt());
                rawItems.add(o("checkCode",code,"checkResult","PASS","severity","INFO","responsibilityModule","MES","sourceObjectCode","report-1"));
            }
            release.setPrecheckSnapshotJson(o("batchExecutionId",30,"releaseStatus","PENDING_APPROVAL","checkedAt","2026-10-10T10:00:00","materialGateManifestHash",H,"items",rawItems).toString());
            when(rows.selectCurrentOpen(20L,false)).thenReturn(checks);when(rows.selectCurrentOpen(20L,true)).thenReturn(checks);
            var batch=map(guard(),MesProEdhrBatchExecutionDO.class);batch.setRouteSnapshotJson("{}");
            var origin=map(root.at("/finalization/origin"),MesProEdhrBatchExecutionOriginDO.class);
            var trace=map(root.at("/finalization/trace"),MesProEdhrBatchTraceSourcePrecheckRespVO.class);
            var materialReceipt=map(root.at("/finalization/materialManifest"),MesReleaseMaterialGateReceipt.class);
            var provenance=new MesEdhrSourceCreationEvidence("TEST_BOUNDARY",50L,"receipt-50","1",H,81L,H);
            var graph=new MesEdhrApprovalFinalizationGraph(1L,task,release,batch,managed,origin,trace,new MesEdhrVerifiedMaterialGateFact(materialReceipt,provenance),provenance,managed?95L:null,active?1L:null);
            when(releases.readForApproval(10L,2L,false)).thenReturn(graph);when(releases.readForApproval(10L,2L,true)).thenReturn(graph);
            if(active){
                var c=(ObjectNode)root.at("/finalization/completionBackfillReceipt").deepCopy();
                c.put("createdAt","2026-10-10T10:00:00").put("formalSourceSnapshotJson",c.get("formalSourceSnapshotJsonRaw").asText()).put("signatureSnapshotJson",c.get("signatureSnapshotJsonRaw").asText()).put("lossConditionFactsJson","[]").put("zeroLossConfirmationSnapshot",c.get("zeroLossConfirmationSnapshotRaw").asText()).put("batchRecordSourceIdsJson","[82]").put("processInspectionSourceIdsJson","[83]");
                when(completion.requireVerifiedByReceiptId(80L,1L)).thenReturn(new MesEdhrVerifiedCompletionReceiptFact(map(c,MesFlow6CompletionBackfillReceipt.class),provenance,"completion-1"));
            }else{
                var c=(ObjectNode)root.at("/finalization/independentPrerequisiteReceipt").deepCopy();
                c.put("issuedAt","2026-10-10T10:00:00").put("expiresAt","2026-10-11T10:00:00").put("canonicalPayload",c.get("canonicalPayloadRaw").asText());
                when(independent.getCreationVerifiedByReceiptId(1L,"independent-80","MANUAL",H)).thenReturn(new MesEdhrVerifiedIndependentReceiptFact(map(c,MesIndependentBatchPrerequisiteReceipt.class),provenance));
            }
            index=0;for(var m:root.at("/finalization/materialManifest/materials")){
                var n=(ObjectNode)m.deepCopy();n.put("id",200+index);
                attachments.add(map(n,MesProBatchRecordExecutionAttachmentDO.class));
                var file=map(o("id",300+index,"configId",1,"path","report-"+index,"size",BYTES.length,"url","https://files.invalid/report-"+index),FileDO.class);
                when(files.getFile(300L+index)).thenReturn(file);when(files.getFileContent(1L,"report-"+index)).thenReturn(BYTES);
                when(files.presignGetUrl(file.getUrl(),900)).thenReturn("https://files.invalid/signed-"+index++);
            }
            when(materials.evaluate(30L)).thenReturn(new MesProEdhrFourMaterialGateResult("MATERIALS_READY",true,H,attachments));
            resolver=new EdhrReleaseContentResolver(releases,new EdhrReleasePrecheckContentReader(rows),semantic,completion,independent,materials,files);
        }
    }
}
