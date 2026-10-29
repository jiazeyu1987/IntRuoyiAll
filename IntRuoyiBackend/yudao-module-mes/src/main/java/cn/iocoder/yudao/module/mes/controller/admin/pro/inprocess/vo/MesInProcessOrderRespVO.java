package cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MesInProcessOrderRespVO {
    private Long id;
    private String workOrderCode;
    private String productCode;
    private String productName;
    private String batchCode;
    private Long leaderUserId;
    private String leaderName;
    private String routeName;
    private BigDecimal quantity;
    private BigDecimal productionProgressPercent;
    private BigDecimal inspectionProgressPercent;
    private LocalDateTime joinedAt;
    private String businessStatus;
    private Boolean abnormal;
    private String abnormalReason;
    private Boolean readBlocked;
    private String readBlockReason;
}
