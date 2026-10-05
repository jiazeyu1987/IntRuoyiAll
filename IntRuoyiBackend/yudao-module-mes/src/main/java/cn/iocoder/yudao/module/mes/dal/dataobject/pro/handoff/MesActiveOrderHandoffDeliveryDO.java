package cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

@TableName("mes_active_order_handoff_delivery")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesActiveOrderHandoffDeliveryDO extends TenantBaseDO {
    @TableId private Long id;
    private Long handoffTaskId;
    private Long userId;
    private String businessKey;
    private String templateParamsJson;
    private String status;
    private Integer attemptCount;
    private Integer rowVersion;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;
    private Long systemMessageId;
    private String lastErrorSummary;
}
