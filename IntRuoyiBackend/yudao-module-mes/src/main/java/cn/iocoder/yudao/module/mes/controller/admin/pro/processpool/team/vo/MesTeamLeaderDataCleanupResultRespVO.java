package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长数据清理 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderDataCleanupResultRespVO {

    private Integer activeOrderCount;
    private Integer reportEventCount;
    private Integer batchExecutionCount;
    private Integer batchRecordExecutionCount;
    private Integer releaseApplicationCount;
    private Integer releaseTransactionCount;
}
