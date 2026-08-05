package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesPqcLeaderPersonnelLinkReqBO {

    private Long leaderUserId;
    private Long systemUserId;
}
