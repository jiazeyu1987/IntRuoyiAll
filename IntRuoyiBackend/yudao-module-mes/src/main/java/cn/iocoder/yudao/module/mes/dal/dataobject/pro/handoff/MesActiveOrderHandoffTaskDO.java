package cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/** A handoff belongs to one frozen active execution and one formal source/round. */
@TableName("mes_active_order_handoff_task")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesActiveOrderHandoffTaskDO extends TenantBaseDO {
    @TableId private Long id;
    private Long activeOrderId;
    private Long workOrderId;
    private Long routeProcessId;
    private String taskType;
    private String sourceType;
    private Long sourceId;
    private Long roundId;
    private String candidateUserSnapshot;
    private String responsibilitySnapshotJson;
    private Long initiatedBy;
    private Boolean notificationOnly;
    private String status;
    private String actionUrl;
    private String reason;
    private Integer rowVersion;
    private Long completedBy;
    private Long completionSourceId;
    private LocalDateTime completedAt;
}
