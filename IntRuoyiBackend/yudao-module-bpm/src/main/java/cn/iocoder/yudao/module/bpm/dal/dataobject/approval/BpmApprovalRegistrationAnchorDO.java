package cn.iocoder.yudao.module.bpm.dal.dataobject.approval;

import lombok.Data;

/** Exact immutable-start binding and actual persisted registration request. */
@Data
public class BpmApprovalRegistrationAnchorDO {
    private Long requestId;
    private String requestKey;
    private String businessKey;
    private String detailJson;
}
