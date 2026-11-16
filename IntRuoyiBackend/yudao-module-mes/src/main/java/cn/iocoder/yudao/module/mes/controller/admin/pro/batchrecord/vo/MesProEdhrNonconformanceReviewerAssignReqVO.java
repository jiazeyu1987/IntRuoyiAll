package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Explicitly assigns only previously unassigned, pending reviews. */
@Data
public class MesProEdhrNonconformanceReviewerAssignReqVO {
    @NotNull @Positive
    private Long id;
    @NotNull @Positive
    private Long qaReviewerUserId;
}
