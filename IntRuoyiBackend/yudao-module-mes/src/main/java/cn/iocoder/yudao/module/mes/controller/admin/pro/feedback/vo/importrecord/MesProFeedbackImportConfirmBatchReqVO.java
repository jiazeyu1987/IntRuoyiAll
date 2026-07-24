package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 导入报工整批确认 Request VO")
@Data
public class MesProFeedbackImportConfirmBatchReqVO {

    @Schema(description = "本次导入记录编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "导入记录不能为空")
    private List<Long> importRecordIds;

    @Schema(description = "行内补齐字段")
    @Valid
    private List<Row> rows;

    @Schema(description = "管理后台 - MES 导入报工整批确认行 Request VO")
    @Data
    public static class Row {

        @Schema(description = "导入记录编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "导入记录编号不能为空")
        private Long importRecordId;

        @Schema(description = "报工人编号")
        private Long feedbackUserId;

        @Schema(description = "报工时间")
        private LocalDateTime feedbackTime;

        @Schema(description = "当前审批人编号")
        private Long approveUserId;

        @Schema(description = "备注")
        private String remark;
    }
}
