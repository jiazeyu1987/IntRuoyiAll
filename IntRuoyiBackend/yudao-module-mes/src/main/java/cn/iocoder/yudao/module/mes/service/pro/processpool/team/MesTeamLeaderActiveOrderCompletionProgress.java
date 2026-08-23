package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/** Locked, authoritative progress snapshot used by the completion gate. */
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderCompletionProgress {

    private BigDecimal productionProgressPercent;
    private BigDecimal inspectionProgressPercent;

    public boolean isDoubleComplete() {
        return productionProgressPercent != null
                && inspectionProgressPercent != null
                && productionProgressPercent.compareTo(BigDecimal.valueOf(100)) == 0
                && inspectionProgressPercent.compareTo(BigDecimal.valueOf(100)) == 0;
    }
}
