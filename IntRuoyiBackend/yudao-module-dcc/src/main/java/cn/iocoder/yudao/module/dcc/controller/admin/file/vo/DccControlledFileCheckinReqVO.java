package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFileCheckinReqVO {

    private String uploadTicket;

    private String sessionId;

    private String changeDescription;

    /** Optional metadata-only change. Only the remark field is mutable in P2. */
    private String remark;
}
