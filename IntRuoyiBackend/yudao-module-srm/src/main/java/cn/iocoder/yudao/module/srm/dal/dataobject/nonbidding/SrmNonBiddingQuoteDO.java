package cn.iocoder.yudao.module.srm.dal.dataobject.nonbidding;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("srm_non_bidding_quote")
@KeySequence("srm_non_bidding_quote_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmNonBiddingQuoteDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long projectId;

    private Long supplierId;

    private String supplierName;

    private BigDecimal quoteAmount;

    private String quoteStatus;

    private String attachmentUrl;

    private Long quotedBy;

    private String quotedName;

    private LocalDateTime quotedTime;
}
