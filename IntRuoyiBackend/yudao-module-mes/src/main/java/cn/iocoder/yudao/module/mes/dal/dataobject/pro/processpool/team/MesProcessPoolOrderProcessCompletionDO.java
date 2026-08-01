package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_order_process_completion")
@KeySequence("mes_pro_process_pool_order_process_completion_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolOrderProcessCompletionDO extends TenantBaseDO {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String BACKFILL_STATUS_NOT_REQUIRED = "NOT_REQUIRED";
    public static final String BACKFILL_STATUS_SUCCESS = "SUCCESS";

    @TableId
    private Long id;

    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal targetQuantity;
    private BigDecimal confirmedQuantity;
    private String completionStatus;
    private LocalDateTime completedAt;
    private String backfillStatus;
    private Long backfillExecutionId;
    private String backfillError;
    private Long lastEventId;
    private Long lastReviewId;
}
