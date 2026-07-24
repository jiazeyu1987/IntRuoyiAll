package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - eDHR批记录版本草稿重传 Request VO")
@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionDraftReuploadReqVO {

    @Schema(description = "来源文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "来源文件名不能为空")
    @Size(max = 255, message = "来源文件名长度不能超过 255 个字符")
    private String sourceFileName;

    @Schema(description = "来源文件 SHA256", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "来源文件 SHA256 不能为空")
    @Size(min = 64, max = 64, message = "来源文件 SHA256 必须为 64 位")
    private String sourceFileSha256;

    @Schema(description = "重传说明")
    @Size(max = 500, message = "重传说明长度不能超过 500 个字符")
    private String remark;
}
