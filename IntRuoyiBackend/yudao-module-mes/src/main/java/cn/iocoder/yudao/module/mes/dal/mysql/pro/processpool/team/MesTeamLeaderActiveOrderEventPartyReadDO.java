package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderEventPartyReadDO {

    private Long eventId;
    private String submitterName;
    private String reviewerName;
    private Long submitterSignatureId;
    private LocalDateTime submitterSignedAt;
    private Long reviewerSignatureId;
    private LocalDateTime reviewerSignedAt;
    private Integer scrapQuantity;
}
