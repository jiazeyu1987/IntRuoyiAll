package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseApplicationResult {

    private Long applicationId;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
    private Long pqcReleaseWorkTaskId;
    private String status;
    private String sourceSnapshotHash;
    private Integer version;
    private LocalDateTime appliedAt;
}
