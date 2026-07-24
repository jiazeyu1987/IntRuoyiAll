package cn.iocoder.yudao.module.srm.dal.dataobject.contract;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("srm_procurement_contract")
@KeySequence("srm_procurement_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmProcurementContractDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String contractNo;

    private String contractTitle;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    private Long supplierId;

    private String supplierName;

    private BigDecimal contractAmount;

    private String currency;

    private LocalDate effectiveDate;

    private LocalDate expireDate;

    private String contractStatus;

    private Long createdBy;

    private String createdName;

    private LocalDateTime createdTime;

    private Long cancelledBy;

    private String cancelledName;

    private LocalDateTime cancelledTime;

    private String cancelReason;
}
