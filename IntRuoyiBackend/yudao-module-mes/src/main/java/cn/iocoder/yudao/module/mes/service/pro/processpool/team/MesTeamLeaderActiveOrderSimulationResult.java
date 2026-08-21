package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderSimulationResult {

    private Long activeOrderId;
    private Integer productionSubmitCount;
    private Integer productionReviewCount;
    private Integer pqcSubmitCount;
    private Integer pqcReviewCount;
    private BigDecimal productionProgressPercent;
    private BigDecimal inspectionProgressPercent;
}
