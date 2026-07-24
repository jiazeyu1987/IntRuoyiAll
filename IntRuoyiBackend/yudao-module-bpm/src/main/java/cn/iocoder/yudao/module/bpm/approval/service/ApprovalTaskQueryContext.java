package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ApprovalTaskQueryContext {

    private Long loginUserId;

    private ApprovalTaskViewType viewType;

    private ApprovalModuleCode moduleCode;

    private String keyword;

    private Integer pageNo;

    private Integer pageSize;

    private boolean globalView;

    public static ApprovalTaskQueryContext of(Long loginUserId, ApprovalTaskViewType viewType,
                                              ApprovalModuleCode moduleCode, String keyword,
                                              Integer pageNo, Integer pageSize) {
        return new ApprovalTaskQueryContext(loginUserId, viewType, moduleCode, keyword, pageNo, pageSize, false);
    }

    public static ApprovalTaskQueryContext of(Long loginUserId, ApprovalTaskViewType viewType,
                                              ApprovalModuleCode moduleCode, String keyword,
                                              Integer pageNo, Integer pageSize, boolean globalView) {
        return new ApprovalTaskQueryContext(loginUserId, viewType, moduleCode, keyword, pageNo, pageSize,
                globalView);
    }
}
