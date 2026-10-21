package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Declared confirmed TYPE_CHANGE rows in real H2; source selection uses actual existing services.
 * This first RED covers consuming the durable proof, not the not-yet-implemented confirmation action. */
class DccWorkingApplicationTypeChangeDatabaseTest extends DccRejectedApplicationMatrixBaselineDatabaseTest {
    static final String SOURCE="APPLICANT_TYPE_CHANGE",FORMAT="DCC_NEW_REJECTED_TYPE_CHANGE_V1",REQUEST="c10-type-change-22";
    @Resource DccControlledFileMetadataChangeMapper typeChanges;
    DccApprovalMatrixSourceService.Source targetMatrix;

    void targetType() throws Exception {
        jdbc.update("INSERT INTO dcc_file_category(id,tenant_id,code,name,active,source,approval_source_mode,lifecycle_stage,file_type_taxonomy_id,deleted) VALUES(3,1,'C10T2','Target type',1,'LOCAL','PUBLIC_MATRIX','DEVELOPMENT',8,0)");
        var raw=JsonUtils.parseObject(canonicalMatrix(1),Map.class);
        raw.put("categoryId","3");raw.put("fileTypeTaxonomyId","8");raw.put("routeId","44");
        var nodes=JsonUtils.parseArray(JsonUtils.toJsonString(raw.get("nodes")),cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.class);
        var root=cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder()
                .id(44L).categoryId(3L).versionNo(1).actionType("LEGACY").active(true).build();
        rawRoutes.insert(root);jdbc.update("UPDATE dcc_category_approval_route SET tenant_id=1 WHERE id=44");
        for(var node:nodes){
            node.setId(440L+node.getId());node.setRouteId(44L);
            if("APPROVAL".equals(node.getStageType())){node.setCandidateSourceIds("77");node.setSubjectId(77L);}
            rawNodes.insert(node);jdbc.update("UPDATE dcc_category_approval_route_node SET tenant_id=1 WHERE id=?",node.getId());
        }
        targetMatrix=matrixSources.read(3L);
    }

    Map<String,Object> state(Long id) {
        var f=files.selectById(id);var a=attributeRows.find("UPLOAD",id,1);
        var value=new LinkedHashMap<String,Object>();
        value.put("formatVersion",FORMAT);value.put("requestId",REQUEST);
        value.put("tenantId",1L);value.put("actorId",99L);value.put("masterId",f.getMasterId());
        value.put("controlledFileId",id);value.put("projectId",f.getDccProjectCodeId());
        value.put("fileNumber",f.getFileNumber());value.put("sourceOriginalFileName",f.getSourceOriginalFileName());
        value.put("changeType",f.getChangeType());value.put("directoryId",f.getDirectoryId());
        value.put("attributeRound",1);value.put("sourceApplicationId",a.getSourceApplicationId());
        value.put("sourceAttributeRound",a.getSourceApplicationRound());
        value.put("rejectedApplicationId",20L);value.put("rejectedAttributeRound",1);
        value.put("rejectedBpm",files.selectById(20L).getProcessInstanceId());
        value.put("categoryId",f.getCategoryId());value.put("taxonomyId",f.getFileTypeTaxonomyId());
        value.put("sourceFileId",f.getSourceFileId());value.put("sourceSha256",f.getSourceSha256());
        value.put("previousSourceSha256",f.getPreviousSourceSha256());
        value.put("approvalMatrixCanonical",f.getDraftApprovalMatrixJson());
        value.put("preparedWorkflowContract",f.getPreparedWorkflowContract());
        value.put("preparedProcessDefinitionId",f.getPreparedProcessDefinitionId());
        value.put("preparedDefinitionDigest",f.getPreparedDefinitionDigest());
        value.put("selectedSignoffDepartmentIdsJson",f.getSelectedSignoffDepartmentIdsJson());
        value.put("distributionDepartmentIdsJson",f.getDistributionDepartmentIdsJson());
        return value;
    }

    void confirmedState(boolean recordProof) throws Exception {
        rejectedCopies();targetType();
        // Read-port wiring is conditional only to keep the behavioral RED compiling before implementation.
        if(Arrays.stream(contracts.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("typeChanges")))
            wire(contracts,"typeChanges",typeChanges);
        var before=state(22L);
        tx().executeWithoutResult(s->{
            projects.selectByIdForUpdate(5L);masters.selectByIdForUpdate(10L);
            jdbc.update("UPDATE dcc_controlled_file SET category_id=3,file_type_taxonomy_id=8,draft_approval_matrix_json=? WHERE id=22",targetMatrix.canonical());
            jdbc.update("UPDATE dcc_controlled_file_master SET category_id=3,file_type_taxonomy_leaf_id=8 WHERE id=10");
            if(recordProof){
                var proof=DccControlledFileMetadataChangeDO.builder().projectCodeId(5L).controlledFileId(22L).masterId(10L)
                        .operatorUserId(99L).source(SOURCE).requestId(REQUEST).changeReason("Explicit applicant T1 to T2")
                        .changedFieldCount(3).beforeSnapshotJson(JsonUtils.toJsonString(before))
                        .afterSnapshotJson(JsonUtils.toJsonString(state(22L))).changedTime(LocalDateTime.now()).build();
                proof.setTenantId(1L);assertEquals(1,typeChanges.insert(proof));
            }
        });
    }

    DccControlledFileRoutePreviewReqVO targetRequest(Long id) {
        var r=new DccControlledFileRoutePreviewReqVO();r.setControlledFileId(id);r.setDccProjectCodeId(5L);
        r.setCategoryId(3L);r.setActionType("NEW");r.setApplicationContextKey("c10-target-body-"+id);return r;
    }

    DccApprovalMatrixBaselineService.Expectations targetExpectation(Long id) {
        var b=targetMatrix.baseline();
        var context=new DccApprovalMatrixTokenService.Context(1L,99L,5L,3L,8L,"NEW",id,"c10-target-body-"+id);
        return new DccApprovalMatrixBaselineService.Expectations(b.getRouteId(),b.getRouteVersionNo(),b.getMatrixDigest(),
                tokens.issueBaseline(context,targetMatrix.canonical()),null,false,"c10-target-body-"+id);
    }

    @Test void confirmedTypeChangeUsesTargetMatrixWithoutRewritingRejectedHistory() throws Exception {
        confirmedState(true);var old=original(20L);var intermediate=original(21L);
        var oldRows=routeSnapshots.selectListByControlledFileId(20L);var current=original(22L);
        var result=assertDoesNotThrow(()->tx().execute(s->baselines.preview(99L,targetRequest(22L))),
                "A registered exact TYPE_CHANGE must select target T2 rather than reject old T1 ancestry");
        assertEquals(targetMatrix.canonical(),result.beforeCanonical());assertEquals(targetMatrix.canonical(),result.after().canonical());
        assertFalse(result.changed());assertNull(result.confirmationToken());
        var confirmed=tx().execute(s->baselines.confirm(99L,5L,3L,22L,"NEW",targetExpectation(22L)));
        assertEquals(targetMatrix.canonical(),confirmed.source().canonical());
        assertEquals(old,original(20L));assertEquals(intermediate,original(21L));assertEquals(current,original(22L));
        assertEquals(oldRows,routeSnapshots.selectListByControlledFileId(20L));
        assertEquals(10L,files.selectById(22L).getMasterId());assertEquals("PREPARED-1",files.selectById(22L).getFileNumber());
        assertEquals(21L,attributeRows.find("UPLOAD",22L,1).getSourceApplicationId());
    }

    @Test void successiveWorkingCopyKeepsConfirmedTargetBaseline() throws Exception {
        confirmedState(true);var old=original(20L);
        add(23L,22L);jdbc.update("UPDATE dcc_controlled_file SET category_id=3,file_type_taxonomy_id=8 WHERE id=23");
        tx().executeWithoutResult(s->{initializer.initialize(99L,22L,23L,null);contracts.inheritPrepared(99L,23L,22L,1);});
        saveFactsToNewTarget(23L,facts(23L,22L,1));
        var result=assertDoesNotThrow(()->tx().execute(s->baselines.preview(99L,targetRequest(23L))),
                "A subsequent same-family copy must retain the registered target-type boundary");
        assertEquals(targetMatrix.canonical(),result.after().canonical());
        assertEquals(targetMatrix.canonical(),tx().execute(s->baselines.confirm(99L,5L,3L,23L,"NEW",targetExpectation(23L))).source().canonical());
        assertEquals(22L,attributeRows.find("UPLOAD",23L,1).getSourceApplicationId());assertEquals(old,original(20L));
    }

    @Test void crossTypeWithoutFormalProofStillFails() throws Exception {
        confirmedState(false);var old=original(20L);var current=original(22L);
        var preview=assertThrows(IllegalArgumentException.class,()->tx().execute(s->baselines.preview(99L,targetRequest(22L))));
        var confirm=assertThrows(IllegalArgumentException.class,()->tx().execute(s->baselines.confirm(99L,5L,3L,22L,"NEW",targetExpectation(22L))));
        assertEquals("EXECUTION_CONTRACT_SOURCE_IDENTITY_INVALID",preview.getMessage());
        assertEquals("EXECUTION_CONTRACT_SOURCE_IDENTITY_INVALID",confirm.getMessage());
        assertEquals(old,original(20L));assertEquals(current,original(22L));
    }

    @Test void laterTargetPublicMatrixChangeDoesNotReplaceExplicitlyConfirmedTarget() throws Exception {
        confirmedState(true);
        changeTargetPublicMatrix();
        assertEquals(45L,matrixSources.read(3L).baseline().getRouteId());
        var compared=tx().execute(s->baselines.preview(99L,targetRequest(22L)));
        assertEquals(targetMatrix.canonical(),compared.after().canonical());assertFalse(compared.changed());
        assertEquals(targetMatrix.canonical(),tx().execute(s->baselines.confirm(99L,5L,3L,22L,"NEW",targetExpectation(22L))).source().canonical());
        assertEquals(canonicalMatrix(2),routeSnapshots.selectListByControlledFileId(20L).get(0).getSourceApprovalMatrixJson(),
                "The T2 target was explicitly saved, never executed in the old T1 BPM");
    }

    void changeTargetPublicMatrix(){
        jdbc.update("UPDATE dcc_category_approval_route SET active=0 WHERE id=44");
        var route=cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder()
                .id(45L).categoryId(3L).versionNo(2).actionType("LEGACY").active(true).build();
        rawRoutes.insert(route);jdbc.update("UPDATE dcc_category_approval_route SET tenant_id=1 WHERE id=45");
        for(var node:targetMatrix.rawNodes()){
            var next=new cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO();
            org.springframework.beans.BeanUtils.copyProperties(node,next);next.setId(node.getId()+10);next.setRouteId(45L);
            if("APPROVAL".equals(next.getStageType())){next.setCandidateSourceIds("78");next.setSubjectId(78L);}
            rawNodes.insert(next);jdbc.update("UPDATE dcc_category_approval_route_node SET tenant_id=1 WHERE id=?",next.getId());
        }
    }

    @ParameterizedTest @ValueSource(strings={"ACTOR","ROUND","HASH","TARGET","REQUEST","OLD_TYPE"})
    void changedProofCannotAuthorizeTypeChange(String damage) throws Exception {
        confirmedState(true);var proof=typeChanges.selectListBySource(SOURCE).get(0);
        var after=JsonUtils.parseObject(proof.getAfterSnapshotJson(),Map.class);
        switch(damage){
            case "ACTOR" -> after.put("actorId",98L);
            case "ROUND" -> after.put("attributeRound",2);
            case "HASH" -> after.put("sourceSha256","b".repeat(64));
            case "TARGET" -> after.put("taxonomyId",9L);
            case "REQUEST" -> after.put("requestId","another-request");
            case "OLD_TYPE" -> {
                var before=JsonUtils.parseObject(proof.getBeforeSnapshotJson(),Map.class);before.put("taxonomyId",9L);
                proof.setBeforeSnapshotJson(JsonUtils.toJsonString(before));
            }
            default -> throw new AssertionError(damage);
        }
        proof.setAfterSnapshotJson(JsonUtils.toJsonString(after));assertEquals(1,typeChanges.updateById(proof));
        var current=original(22L);
        var preview=assertThrows(IllegalArgumentException.class,()->tx().execute(s->baselines.preview(99L,targetRequest(22L))));
        var confirm=assertThrows(IllegalArgumentException.class,()->tx().execute(s->baselines.confirm(99L,5L,3L,22L,"NEW",targetExpectation(22L))));
        assertEquals("EXECUTION_CONTRACT_TYPE_CHANGE_PROOF_INVALID",preview.getMessage());
        assertEquals("EXECUTION_CONTRACT_TYPE_CHANGE_PROOF_INVALID",confirm.getMessage());
        assertEquals(current,original(22L));
    }
}
