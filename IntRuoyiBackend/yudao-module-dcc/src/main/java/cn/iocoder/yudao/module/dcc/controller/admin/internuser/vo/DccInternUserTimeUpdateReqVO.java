package cn.iocoder.yudao.module.dcc.controller.admin.internuser.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 实习用户 DCC 时间更新 Request VO")
@Data
@Accessors(chain = true)
public class DccInternUserTimeUpdateReqVO {

    @Schema(description = "受控文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "受控文件编号不能为空")
    private Long controlledFileId;

    @Schema(description = "目标时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标时间不能为空")
    private LocalDateTime targetTime;

}
