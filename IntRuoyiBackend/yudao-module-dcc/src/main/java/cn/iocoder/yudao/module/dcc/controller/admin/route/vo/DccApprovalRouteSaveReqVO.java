package cn.iocoder.yudao.module.dcc.controller.admin.route.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccApprovalRouteSaveReqVO {
    @NotNull(message = "生效时间不能为空")
    private LocalDateTime effectiveTime;
    private String remark;
    @Valid
    @NotNull(message = "路线节点不能为空")
    private List<DccApprovalRouteNodeSaveReqVO> nodes;
}
