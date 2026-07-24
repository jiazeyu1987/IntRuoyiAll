package cn.iocoder.yudao.module.dcc.controller.admin.position.vo;

import lombok.Data;

@Data
public class DccApprovalPositionImportRespVO {

    private Integer totalCount;
    private Integer createdCount;
    private Integer adoptedCount;
    private Integer updatedCount;
    private Integer disabledCount;

}
