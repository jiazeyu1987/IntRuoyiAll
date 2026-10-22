package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data @Accessors(chain = true)
@TableName("mes_edhr_managed_handoff_material_association")
public class MesEdhrManagedHandoffMaterialAssociationDO {
    @TableId private Long id;
    private Long tenantId;
    private String associationId;
    private Long applicationId;
    private Long releaseTransactionId;
    private Long managerWorkTaskId;
    private String sourceMethod;
    private Long sourceWorkTaskId;
    private Integer sourceExpectedApplicationVersion;
    private String sourceIdempotencyKey;
    private String handoffKey;
    private String handoffRequestJson;
    private String associationPayloadJson;
    private String associationHash;
    private Long handoffAuditEventId;
    private String handoffAuditEventHash;
}
