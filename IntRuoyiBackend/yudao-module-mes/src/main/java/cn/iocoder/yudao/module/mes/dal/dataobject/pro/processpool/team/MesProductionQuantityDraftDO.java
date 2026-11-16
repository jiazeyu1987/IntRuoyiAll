package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_pro_production_quantity_draft")
public class MesProductionQuantityDraftDO extends TenantBaseDO {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long operatorUserId;
    private Long employeeId;
    private String identityDomain;
    private Long leaderUserId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private String submitKeyHash;
    private String requestHash;
    private String requestJson;
    private BigDecimal requestedQuantity;
    private BigDecimal previousQuantity;
    private BigDecimal upperQuantity;
    private String status;
    private Integer version;
    private String decision;
    private String reviewReason;
    private Long reviewSignatureId;
    private LocalDateTime reviewedAt;
    private Long submittedEventId;
    private String resubmittedRequestHash;
}
