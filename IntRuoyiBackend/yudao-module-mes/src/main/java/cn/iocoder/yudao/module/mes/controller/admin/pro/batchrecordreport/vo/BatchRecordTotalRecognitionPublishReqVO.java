package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 生产批记录总识别 JSON 发布 Request VO")
@Data
public class BatchRecordTotalRecognitionPublishReqVO {

    @Schema(description = "DCC 项目代码 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "DCC 项目代码不能为空")
    private Long dccProjectCodeId;

    @Schema(description = "当前编辑器中的生产批记录总识别 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生产批记录总识别 JSON 不能为空")
    private Object recognitionJson;
}
