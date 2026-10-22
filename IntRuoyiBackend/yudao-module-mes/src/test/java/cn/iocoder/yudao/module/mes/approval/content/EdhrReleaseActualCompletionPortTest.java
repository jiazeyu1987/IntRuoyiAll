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
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import java.time.LocalDateTime;
import java.util.*;
import static cn.iocoder.yudao.module.mes.approval.content.EdhrReleaseContentFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Flow6 port -> real content resolver/policy; audit and upstream graph are explicit boundaries. */
class EdhrReleaseActualCompletionPortTest {
    @Test void actualFrozenPortProjectsBothApprovalBranchesWithoutCurrentOwnerEnrichment() throws Exception {
        for(boolean managed:new boolean[]{false,true}) {
            var f=new Fixture(true,managed);
            var prepared=f.resolver.prepare(2L,"EDHR_WORK_TASK","10",null);
            var projected=ApprovalContentJson.parse(prepared.canonicalBusinessContentJson());
            var receipt=projected.at("/finalization/completionBackfillReceipt");
            assertEquals("84",receipt.at("/pickListSources/0/pickListBindingId").asText());
            assertEquals("85",receipt.at("/pickListSources/0/pickListId").asText());
            assertEquals("1",receipt.at("/pickListSources/0/bindingVersion").asText());
            assertEquals(H,receipt.at("/pickListSources/0/sourceSnapshotHash").asText());
            assertEquals(f.owner.getFormalSourceSnapshotJson(),receipt.get("formalSourceSnapshotJsonRaw").asText());
            assertEquals(f.owner.getReceiptHash(),receipt.get("receiptHash").asText());
            assertEquals("actual-completion-transaction",receipt.get("completionTransactionId").asText());
            assertTrue(receipt.get("workOrderCode").isNull());
            assertTrue(receipt.get("sourceBundleHash").isNull());
            assertTrue(receipt.get("sourceEvidence").isNull());
            assertEquals(4,prepared.attachments().size());
            assertEquals(prepared.contentHash(),f.resolver.lockAndAuthorize(2L,prepared.identity()).contentHash());
            verify(f.receiptMapper,atLeastOnce()).selectByIdAndTenantId(80L,1L);
            verifyNoInteractions(f.independent);
        }
    }
    @Test void missingOrForeignFrozenBindingCannotBeReplacedByCurrentOwnerOrAnEmptyList() throws Exception {
        for(String replacement:new String[]{"[]","[{\"id\":84,\"tenantId\":2,\"activeOrderId\":60,\"workOrderId\":70,\"pickListId\":85,\"bindingVersion\":1,\"bindingStatus\":\"BOUND\",\"sourceSnapshotHash\":\""+H+"\"}]"}) {
            var f=new Fixture(true,false);
            var seed=(ObjectNode)ApprovalContentJson.parse(f.owner.getFormalSourceSnapshotJson());
            seed.set("pickListBindings",ApprovalContentJson.parse(replacement));
            f.owner.setFormalSourceSnapshotJson(seed.toString()); reseal(f.owner);
            assertThrows(IllegalStateException.class,()->f.resolver.prepare(2L,"EDHR_WORK_TASK","10",null));
        }
    }
    @Test void brokenCreationRejectsRealPortBeforeContentCanBePrepared() throws Exception {
        var f=new Fixture(true,false);
        when(f.creation.requireCompletionCreation(1L,80L)).thenThrow(new IllegalStateException("missing actual MAIN/PART"));
        assertThrows(IllegalStateException.class,()->f.resolver.prepare(2L,"EDHR_WORK_TASK","10",null));
    }
    private static MesProcessPoolActiveOrderCompletionReceiptDO ownerReceipt() {
        var seed=o("activeOrderBinding",o("id",60,"tenantId",1,"workOrderId",70,"routeId",90,"routeVersionId",91),
                "workOrderBinding",o("id",70,"productId",92,"batchCode","LOT-30"),
                "pickListBindings",a(o("id",84,"tenantId",1,"activeOrderId",60,"workOrderId",70,"pickListId",85,"bindingVersion",1,"bindingStatus","BOUND","sourceSnapshotHash",H)));
        var row=MesProcessPoolActiveOrderCompletionReceiptDO.builder().id(80L).activeOrderId(60L).workOrderId(70L)
                .batchCode("LOT-30").routeId(90L).routeVersionId(91L).leaderUserId(2L).requestIdempotencyKey("completion-key")
                .requestPayloadHash(H).formalSourceSnapshotJson(seed.toString()).signatureSnapshotJson("{\"signature\":81}")
                .expectedVersion(0).completedVersion(1).receiptStatus("BACKFILL_SUCCEEDED").completionStatus("SUCCESS")
                .batchRecordStatus("SUCCESS").processInspectionStatus("SUCCESS").batchRecordId(82L).processInspectionId(83L)
                .hasActualLoss(false).lossQuantity(java.math.BigDecimal.ZERO).lossReportStatus("NOT_REQUIRED")
                .zeroLossConfirmationSnapshot("{\"confirmed\":true}").lossConditionFactsJson("[]")
                .batchRecordSourceIdsJson("[84,82]").processInspectionSourceIdsJson("[83]").provisionHandoff("PENDING_FLOW6")
                .completedAt(LocalDateTime.of(2026,10,10,10,0)).completedBy(2L).build();
        row.setTenantId(1L); row.setCreateTime(row.getCompletedAt()); reseal(row); return row;
    }
    private static void reseal(MesProcessPoolActiveOrderCompletionReceiptDO row) {
        row.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.sourceSnapshotHash(row.getFormalSourceSnapshotJson(),row.getLossConditionFactsJson()));
        row.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(row));
    }
    private static void replaceSourceHash(com.fasterxml.jackson.databind.JsonNode node,String hash) {
        if(node.isObject()) { if(node.has("sourceSnapshotHash")) ((ObjectNode)node).put("sourceSnapshotHash",hash);node.forEach(value->replaceSourceHash(value,hash)); }
        else if(node.isArray())node.forEach(value->replaceSourceHash(value,hash));
    }
    private static final class Fixture {
        final ObjectNode root;
        final MesProEdhrReleaseService releases=mock(MesProEdhrReleaseService.class);
        final MesProEdhrReleaseCheckItemMapper rows=mock(MesProEdhrReleaseCheckItemMapper.class);
        final MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper=mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        final MesEdhrSourceCreationVerificationService creation=mock(MesEdhrSourceCreationVerificationService.class);
        final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completion=new MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl(receiptMapper,creation);
        final MesProcessPoolActiveOrderCompletionReceiptDO owner=ownerReceipt();
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
            // Match the graph's immutable receipt anchor to the actual owner algorithm, not an enriched DTO.
            ((ObjectNode)root.at("/finalization/origin")).put("completionBackfillReceiptHash",owner.getReceiptHash());
            replaceSourceHash(root,owner.getSourceSnapshotHash());
            when(receiptMapper.selectByIdAndTenantId(80L,1L)).thenReturn(owner);
            when(creation.requireCompletionCreation(1L,80L)).thenAnswer(invocation -> {
                new MesEdhrSourceOwnerIntegrity("", "").completion(owner);
                return new MesEdhrSourceCreationEvidence("COMPLETION_MAIN_V1",80L,"80","1",owner.getReceiptHash(),81L,H);
            });
            when(creation.requireCompletionTransactionId(1L,80L)).thenReturn("actual-completion-transaction");
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
