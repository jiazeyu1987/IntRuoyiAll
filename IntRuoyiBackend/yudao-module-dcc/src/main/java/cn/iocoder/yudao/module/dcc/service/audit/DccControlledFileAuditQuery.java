package cn.iocoder.yudao.module.dcc.service.audit;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccControlledFileAuditQuery extends PageParam {

    private String accessEventCode;
    private String watermarkTraceCode;
    private Long controlledFileId;
    private Long userId;
    private String actionType;
    private String result;
    private String failureCode;
    private String requestId;
    private LocalDateTime[] occurredAt;

}
