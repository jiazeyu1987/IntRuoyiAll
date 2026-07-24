package cn.iocoder.yudao.module.srm.dal.dataobject.tender;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("srm_tender_winning_result")
@KeySequence("srm_tender_winning_result_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmTenderWinningResultDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long projectId;

    private Long candidateId;

    private Long supplierId;

    private String supplierName;

    private BigDecimal winningAmount;

    private String winningRemark;

    private Long confirmedBy;

    private String confirmedName;

    private LocalDateTime confirmedTime;
}
