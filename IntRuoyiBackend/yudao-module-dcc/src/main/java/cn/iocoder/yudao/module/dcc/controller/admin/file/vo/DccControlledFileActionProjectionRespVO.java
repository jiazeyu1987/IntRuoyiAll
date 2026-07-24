package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccControlledFileActionProjectionRespVO {

    private Boolean actionLocked;
    private String actionLockReason;
    private List<String> allowedActions;
    private Boolean canWithdraw;
    private Long pendingRequestId;
    private String pendingVersionNo;
}
