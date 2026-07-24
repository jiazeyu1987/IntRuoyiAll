package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApprovalTaskQuery {

    private ApprovalTaskViewType viewType = ApprovalTaskViewType.TODO;

    private ApprovalModuleCode moduleCode;

    private String keyword;

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
