package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import lombok.Getter;

@Getter
final class MesTeamLeaderActiveOrderReleaseBlockedException extends RuntimeException {

    private final MesProcessPoolActiveOrderReleaseApplicationDO application;

    MesTeamLeaderActiveOrderReleaseBlockedException(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        super("Active order release dossier is blocked");
        this.application = application;
    }
}
