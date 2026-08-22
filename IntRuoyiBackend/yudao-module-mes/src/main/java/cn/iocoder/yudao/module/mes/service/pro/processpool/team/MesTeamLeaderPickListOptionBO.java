package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MesTeamLeaderPickListOptionBO {
    private Long pickListId;
    private String sourceFid;
    private String sourceBillNo;
    private String documentStatus;
    private LocalDateTime sourceModifyTime;
    private String productionOrderNo;
    private int detailCount;
    private List<Long> detailIds;
    private String candidateSnapshotHash;
    private boolean selectable;
    private String blockerCode;
}
