package cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@TableName("mes_pro_feedback_surplus_allocation")
@KeySequence("mes_pro_feedback_surplus_allocation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProFeedbackSurplusAllocationDO extends BaseDO {

    public static final String TARGET_TYPE_EXTERNAL_OTHER_ORDER = "EXTERNAL_OTHER_ORDER";
    public static final String TARGET_TYPE_POOL_CONSUME = "POOL_CONSUME";

    @TableId
    private Long id;

    private Long poolId;
    private Long importRecordId;
    private String targetType;
    private Long targetScheduleOrderId;
    private Long targetScheduleOrderProcessId;
    private String targetOrderLabel;
    private String targetProductLabel;
    private BigDecimal allocatedQuantity;
    private String remark;
}
