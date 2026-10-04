package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class DccControlledFileRoutePreviewReqVO {

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    private String actionType;

    private List<Long> selectedSignoffUserIds;

    private List<@NotNull @Positive Long> selectedSignoffDepartmentIds;
}
