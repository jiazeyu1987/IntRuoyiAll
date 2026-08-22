package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_active_order")
@KeySequence("mes_pro_process_pool_active_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolActiveOrderDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long leaderUserId;
    private Long workOrderId;
    private Long routeId;
    private Long routeVersionId;
    private Long dccProjectCodeId;
    private Long qaRegulationId;
    private Long qaRegulationVersionId;
    private BigDecimal erpFixedQuantitySnapshot;
    private String activeStatus;
    private String businessStatus;
    private LocalDateTime joinedAt;
    private Long sortOrder;
    private LocalDateTime removedAt;
    private Long releaseDecisionId;
    private Long releasedBy;
    private LocalDateTime releasedAt;
    @Version
    private Integer version;
}
