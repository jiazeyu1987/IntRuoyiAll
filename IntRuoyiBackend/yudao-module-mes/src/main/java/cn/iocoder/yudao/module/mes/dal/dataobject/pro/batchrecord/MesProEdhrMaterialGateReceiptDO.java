package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@TableName("mes_pro_edhr_material_gate_receipt")
@KeySequence("mes_pro_edhr_material_gate_receipt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesProEdhrMaterialGateReceiptDO extends BaseDO {

    @TableId
    private Long id;
    private String receiptId;
    private Long tenantId;
    private Long batchExecutionId;
    private String gateStatus;
    private String materialTypeKeysJson;
    private String manifestHash;
    private String sourceSnapshotHash;
    private String materialVersionSetHash;
    private String receiptHash;
    private Long issuedBy;
    private String auditEventId;
    private Integer version;

    public static MesProEdhrMaterialGateReceiptDO from(MesReleaseMaterialGateReceipt receipt) {
        return new MesProEdhrMaterialGateReceiptDO()
                .setReceiptId(receipt.getReceiptId())
                .setBatchExecutionId(receipt.getBatchExecutionId())
                .setGateStatus(receipt.getGateStatus())
                .setManifestHash(receipt.getManifestHash())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setMaterialVersionSetHash(receipt.getMaterialVersionSetHash())
                .setReceiptHash(receipt.getReceiptHash())
                .setIssuedBy(receipt.getIssuedBy())
                .setAuditEventId(receipt.getAuditEventId())
                .setVersion(receipt.getVersion());
    }
}
