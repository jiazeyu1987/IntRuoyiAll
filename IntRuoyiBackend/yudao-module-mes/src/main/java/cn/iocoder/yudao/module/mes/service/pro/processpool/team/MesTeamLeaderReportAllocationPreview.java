package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderReportAllocationPreview {

    private BigDecimal totalAllocatedQuantity;
    private List<MesTeamLeaderReportAllocationPreviewLine> lines;
}
