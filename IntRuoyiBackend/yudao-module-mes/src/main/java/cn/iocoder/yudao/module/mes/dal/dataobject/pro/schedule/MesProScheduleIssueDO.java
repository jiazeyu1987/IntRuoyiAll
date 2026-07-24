package cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mes_pro_schedule_issue")
@KeySequence("mes_pro_schedule_issue_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleIssueDO extends BaseDO {

    @TableId
    private Long id;

    private String issueType;

    private String severity;

    private Long workOrderId;

    private Long taskId;

    private Long processId;

    private Long workstationId;

    private Long materialId;

    private LocalDateTime calendarDate;

    private Long shiftId;

    private BigDecimal requiredQty;

    private BigDecimal availableQty;

    private BigDecimal shortageQty;

    private String message;

    private Boolean resolved;

    private String status;

    private String sourceType;

    private Long sourceId;

    private String resolutionReason;

    private Long resolvedBy;

    private LocalDateTime resolvedAt;

}
