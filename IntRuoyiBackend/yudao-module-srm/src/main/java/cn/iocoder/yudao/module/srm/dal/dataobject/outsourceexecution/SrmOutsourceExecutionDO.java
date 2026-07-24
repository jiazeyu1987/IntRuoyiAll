package cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("srm_outsource_execution")
@KeySequence("srm_outsource_execution_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmOutsourceExecutionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String executionNo;

    private Long sourcePurchaseOrderId;

    private String sourcePurchaseOrderNo;

    private Long sourcePlanId;

    private String sourcePlanNo;

    private Long supplierId;

    private String supplierName;

    private String executionStatus;

    private String simulationSource;

    private String simulationLabel;

    private String simulationRemark;

    private BigDecimal plannedQuantity;

    private String issueNoticeNo;

    private BigDecimal issueQuantity;

    private BigDecimal progressPercent;

    private String progressStage;

    private BigDecimal receivedQuantity;

    private BigDecimal qualifiedQuantity;

    private BigDecimal unitPrice;

    private Long issuedBy;

    private String issuedName;

    private LocalDateTime issuedTime;

    private Long deliveredBy;

    private String deliveredName;

    private LocalDateTime deliveredTime;

    private Long inspectedBy;

    private String inspectedName;

    private LocalDateTime inspectedTime;
}
