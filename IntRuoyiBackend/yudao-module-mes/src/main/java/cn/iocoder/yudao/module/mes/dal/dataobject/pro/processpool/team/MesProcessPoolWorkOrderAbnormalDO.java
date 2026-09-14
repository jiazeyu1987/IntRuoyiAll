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

import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_work_order_abnormal")
@KeySequence("mes_pro_process_pool_work_order_abnormal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolWorkOrderAbnormalDO extends TenantBaseDO {

    public static final String REPORT_STATUS_REPORTED = "REPORTED";
    public static final String REPORT_STATUS_PROCESSING = "PROCESSING";
    public static final String REPORT_STATUS_CLOSED = "CLOSED";

    @TableId
    private Long id;

    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private Long sourceEventId;
    private String abnormalReasonCode;
    private String abnormalDescription;
    private String reportStatus;
    private Long markerUserId;
    private LocalDateTime markedAt;
    private Long reporterUserId;
    private LocalDateTime reportedAt;
    private String closeRemark;
    private LocalDateTime closedAt;
}
