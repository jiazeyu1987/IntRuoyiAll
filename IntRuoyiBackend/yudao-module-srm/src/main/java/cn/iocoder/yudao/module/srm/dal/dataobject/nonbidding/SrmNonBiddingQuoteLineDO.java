package cn.iocoder.yudao.module.srm.dal.dataobject.nonbidding;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("srm_non_bidding_quote_line")
@KeySequence("srm_non_bidding_quote_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmNonBiddingQuoteLineDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long quoteId;

    private Long projectId;

    private Long projectLineId;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal unitPrice;

    private BigDecimal lineAmount;
}
