package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - eDHR批记录版本迁移项确认 Response VO")
@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionMigrationConfirmRespVO {

    @Schema(description = "版本ID")
    private Long versionId;

    @Schema(description = "已确认迁移项ID")
    private List<Long> confirmedItemIds;

    @Schema(description = "确认人")
    private Long confirmedBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedAt;

    @Schema(description = "确认意见")
    private String confirmComment;

    @Schema(description = "幂等键")
    private String idempotencyKey;
}
