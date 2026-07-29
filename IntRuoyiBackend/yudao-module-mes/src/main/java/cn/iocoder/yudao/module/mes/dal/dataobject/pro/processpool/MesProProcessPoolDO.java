package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool;

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

@TableName("mes_pro_process_pool")
@KeySequence("mes_pro_process_pool_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProProcessPoolDO extends TenantBaseDO {

    public static final String STATUS_ACTIVE = "ACTIVE";

    @TableId
    private Long id;

    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private Long deviceId;
    private Long workstationId;
    private String poolStatus;
    private Long latestEventId;
    private LocalDateTime latestSubmitTime;
    private Integer totalEventCount;
    private Long lastActualEmployeeId;
}
