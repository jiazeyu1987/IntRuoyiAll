package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - eDHR批记录版本草稿重传 Response VO")
@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionDraftReuploadRespVO {

    @Schema(description = "作废的旧草稿版本ID")
    private Long voidedVersionId;

    @Schema(description = "新草稿版本ID")
    private Long newVersionId;

    @Schema(description = "新版本号")
    private String versionNo;

    @Schema(description = "新状态")
    private String status;
}
