package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 申请人培训记录上传 Request VO")
@Data
public class DccControlledFileTrainingRecordReqVO {

    @Schema(description = "上传会话编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "dcc-training-session-1")
    @NotBlank(message = "上传会话不能为空")
    private String sessionId;

    @Schema(description = "培训记录上传凭证", requiredMode = Schema.RequiredMode.REQUIRED, example = "UT-20260528-0001")
    @NotBlank(message = "培训记录上传凭证不能为空")
    private String trainingRecordUploadTicket;

}
