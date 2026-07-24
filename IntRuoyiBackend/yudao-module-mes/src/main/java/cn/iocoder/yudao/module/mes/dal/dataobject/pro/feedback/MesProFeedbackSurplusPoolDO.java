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

@TableName("mes_pro_feedback_surplus_pool")
@KeySequence("mes_pro_feedback_surplus_pool_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProFeedbackSurplusPoolDO extends BaseDO {

    public static final String SOURCE_TYPE_CURRENT_ORDER_OVERPRODUCE = "CURRENT_ORDER_OVERPRODUCE";
    public static final String SOURCE_TYPE_EXTERNAL_OTHER_ORDER = "EXTERNAL_OTHER_ORDER";
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_ALLOCATED = "ALLOCATED";

    @TableId
    private Long id;

    private String sourceType;
    private Long sourceImportRecordId;
    private Long sourceFeedbackId;
    private Long sourceScheduleOrderId;
    private Long sourceScheduleOrderProcessId;
    private String sourceWorkOrderCode;
    private String sourceTaskCode;
    private Long processId;
    private String processCode;
    private String processName;
    private Long productId;
    private String itemCode;
    private String itemName;
    private String specification;
    private BigDecimal totalQuantity;
    private BigDecimal allocatedQuantity;
    private BigDecimal availableQuantity;
    private String status;
    private String remark;
}
