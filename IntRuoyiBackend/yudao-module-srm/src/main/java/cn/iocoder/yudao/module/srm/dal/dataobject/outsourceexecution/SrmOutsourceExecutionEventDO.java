package cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("srm_outsource_execution_event")
@KeySequence("srm_outsource_execution_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmOutsourceExecutionEventDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String eventNo;

    private Long executionId;

    private String eventType;

    private String beforeStatus;

    private String afterStatus;

    private String simulationSource;

    private Long operatorId;

    private String operatorName;

    private String eventRemark;

    private String eventPayload;

    private LocalDateTime eventTime;
}
