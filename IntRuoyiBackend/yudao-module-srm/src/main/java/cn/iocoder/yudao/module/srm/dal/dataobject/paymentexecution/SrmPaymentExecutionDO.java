package cn.iocoder.yudao.module.srm.dal.dataobject.paymentexecution;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("srm_payment_execution")
@KeySequence("srm_payment_execution_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmPaymentExecutionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String paymentNo;

    private Long reconciliationId;

    private String reconciliationNo;

    private Long executionId;

    private String executionNo;

    private Long contractId;

    private String contractNo;

    private Long supplierId;

    private String supplierName;

    private String paymentStatus;

    private String simulationSource;

    private String simulationLabel;

    private String paymentStage;

    private BigDecimal paymentRatio;

    private LocalDate dueDate;

    private String paymentTermSummary;

    private BigDecimal reconciliationAmount;

    private BigDecimal applyAmount;

    private String paymentRemark;

    private Long submittedBy;

    private String submittedName;

    private LocalDateTime submittedTime;

    private Long approvedBy;

    private String approvedName;

    private LocalDateTime approvedTime;

    private Long rejectedBy;

    private String rejectedName;

    private LocalDateTime rejectedTime;

    private String rejectRemark;

    private Long pushedBy;

    private String pushedName;

    private LocalDateTime pushedTime;

    private String pushRemark;
}
