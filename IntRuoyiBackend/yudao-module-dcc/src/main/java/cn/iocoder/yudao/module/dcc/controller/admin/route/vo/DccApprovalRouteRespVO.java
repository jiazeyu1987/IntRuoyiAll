package cn.iocoder.yudao.module.dcc.controller.admin.route.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccApprovalRouteRespVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer versionNo;
    private Boolean active;
    private String statusLabel;
    private LocalDateTime effectiveTime;
    private String remark;
    private Integer nodeCount;
    private String nodeSummary;
    private List<DccApprovalRouteNodeRespVO> nodes;
}
