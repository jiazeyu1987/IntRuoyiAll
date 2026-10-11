package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceiptPort;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesEdhrManagedHandoffMaterialAssociationStore;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class MesEdhrReleaseMaterialSelectionRegistry {
    private final MesEdhrReleaseSubmissionMaterialAssociationStore submissions;
    private final MesEdhrManagedHandoffMaterialAssociationStore managed;
    private final MesReleaseMaterialGateReceiptPort materials;
    public MesEdhrReleaseMaterialSelectionRegistry(MesEdhrReleaseSubmissionMaterialAssociationStore submissions,
            MesEdhrManagedHandoffMaterialAssociationStore managed, MesReleaseMaterialGateReceiptPort materials) {
        this.submissions=submissions;this.managed=managed;this.materials=materials;
    }
    public MesEdhrVerifiedMaterialSelection requireCurrentVerified(Long tenant, MesProEdhrReleaseTransactionDO tx, MesProEdhrWorkTaskDO task, boolean isManaged) {
        MesEdhrSourceCreationBindingStore.tenant(tenant);
        if(task == null || tx == null || !"RELEASE_APPROVE".equals(task.getTaskType()) || !"RELEASE_TRANSACTION".equals(task.getBusinessScopeType()) || !Objects.equals(task.getBusinessScopeId(),tx.getId()) || !Objects.equals(task.getBatchExecutionId(),tx.getBatchExecutionId()))
            throw new IllegalStateException("MATERIAL_SELECTION_TASK_MISMATCH");
        String receipt,source,precheck,hash; Long rowId,eventId; String eventHash;
        if(isManaged){
            var association=managed.requireCurrentVerified(tenant,tx,task.getId());
            var p=MesEdhrManagedHandoffMaterialAssociationStore.parse(association.getAssociationPayloadJson());
            receipt=p.get("materialReceiptId").textValue();source=p.get("sourceSnapshotHash").textValue();precheck=p.get("precheckSnapshotHash").textValue();
            hash=p.get("materialReceiptHash").textValue();rowId=p.get("materialSourceRowId").longValue();eventId=p.get("materialCreationEventId").longValue();eventHash=p.get("materialCreationEventHash").textValue();
        }else{
            var association=submissions.requireCurrentVerified(tenant,tx,task.getId());
            receipt=association.getMaterialReceiptId();source=association.getSourceSnapshotHash();precheck=association.getPrecheckSnapshotHash();
            hash=association.getMaterialReceiptHash();rowId=association.getMaterialSourceRowId();eventId=association.getMaterialCreationEventId();eventHash=association.getMaterialCreationEventHash();
        }
        var fact=materials.getCreationVerifiedByReceiptId(tenant,tx.getBatchExecutionId(),receipt,source);
        if(!Objects.equals(fact.provenance().sourceRowId(),rowId)||!Objects.equals(fact.provenance().sourceHash(),hash)
                ||!Objects.equals(fact.provenance().creationEventId(),eventId)||!Objects.equals(fact.provenance().creationEventHash(),eventHash))
            throw new IllegalStateException("MATERIAL_SELECTION_PROVENANCE_MISMATCH");
        return new MesEdhrVerifiedMaterialSelection(isManaged?"MANAGED":"GENERAL",tx.getId(),task.getId(),precheck,fact);
    }
}
