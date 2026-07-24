package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 电子批记录报表批量删除 Request VO")
@Data
public class BatchRecordReportBatchDeleteReqVO {

    @Schema(description = "积木报表 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "积木报表 ID 列表不能为空")
    private List<@NotBlank(message = "积木报表 ID 不能为空") String> reportIds;

    @Schema(description = "是否先解除工艺路线/用途绑定后删除")
    private Boolean forceUnbind = false;

}
