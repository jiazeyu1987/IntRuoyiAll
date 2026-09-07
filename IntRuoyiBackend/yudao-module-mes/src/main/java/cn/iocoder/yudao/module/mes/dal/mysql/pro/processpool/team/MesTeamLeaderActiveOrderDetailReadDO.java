package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderDetailReadDO {

    private Long snapshotId;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String batchCode;
    private BigDecimal workOrderQuantity;
    private String productCode;
    private String productName;
    private String productSpecification;
    private LocalDateTime workOrderCreateTime;
    private String routeName;
    private Long routeProcessId;
    private Long processId;
    private String processCode;
    private String processName;
    private BigDecimal requiredQuantity;
    private Long eventId;
    private BigDecimal submittedQuantity;
    private String submitterName;
    private String reviewerName;
    private LocalDateTime submittedAt;
    private String originalPayloadJson;
    private Long eventDeviceId;
    private String eventDeviceCode;
    private String eventDeviceName;
}
