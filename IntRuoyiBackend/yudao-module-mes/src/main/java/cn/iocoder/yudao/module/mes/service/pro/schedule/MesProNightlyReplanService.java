package cn.iocoder.yudao.module.mes.service.pro.schedule;

import java.time.LocalDateTime;

public interface MesProNightlyReplanService {

    MesProNightlyReplanResult executeNightlyReplan(LocalDateTime startTime);

}
