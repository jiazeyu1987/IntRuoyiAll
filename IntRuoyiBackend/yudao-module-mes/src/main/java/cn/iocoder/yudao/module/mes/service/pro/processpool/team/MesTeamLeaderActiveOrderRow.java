package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产组长活跃订单列表读模型。
 */
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRow {

    private Long id;
    private Long leaderUserId;
    private Long workOrderId;
    private Long routeId;
    private String routeName;
    private Long routeVersionId;
    private String routeVersionNo;
    private BigDecimal erpFixedQuantitySnapshot;
    private String activeStatus;
    private String businessStatus;
    private LocalDateTime joinedAt;
    private LocalDateTime removedAt;
    private Integer version;
    private Boolean abnormal;
    private String abnormalReason;
    private LocalDateTime abnormalReportedAt;
}
