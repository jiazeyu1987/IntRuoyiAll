package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 生产组长数据清理预检 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderDataCleanupPreviewRespVO {

    private Long leaderUserId;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> orderIds;
    private List<Integer> orderVersions;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> workOrderIds;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> batchExecutionIds;
    private Integer activeOrderCount;
    private Integer reportEventCount;
    private Integer batchExecutionCount;
    private Integer releaseApplicationCount;
    private Integer releaseTransactionCount;
}
