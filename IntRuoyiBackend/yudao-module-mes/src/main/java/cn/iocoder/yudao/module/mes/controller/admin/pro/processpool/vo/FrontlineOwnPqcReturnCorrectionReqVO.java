package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class FrontlineOwnPqcReturnCorrectionReqVO {
    @NotNull @Min(1) private Long activeOrderId;
    @NotNull @Min(1) private Long rejectedReviewId;
    @NotNull @Min(0) private Long expectedRevisionId;
    @Valid @NotNull private ProcessPoolPqcInspectionCorrectionReqVO correction;
}
