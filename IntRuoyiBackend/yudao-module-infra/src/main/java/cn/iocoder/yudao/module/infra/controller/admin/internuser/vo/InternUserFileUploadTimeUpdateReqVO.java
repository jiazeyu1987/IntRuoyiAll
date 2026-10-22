package cn.iocoder.yudao.module.infra.controller.admin.internuser.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 实习用户文件上传时间更新 Request VO")
@Data
@Accessors(chain = true)
public class InternUserFileUploadTimeUpdateReqVO {

    @Schema(description = "文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "文件编号不能为空")
    private Long id;

    @Schema(description = "上传时间", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-08-20 10:30:00")
    @NotNull(message = "上传时间不能为空")
    private LocalDateTime createTime;

}
