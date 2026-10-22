package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class GxpAuditEventPageQuery extends PageParam {

    private String scopeType;
    private Long scopeId;
    private String domain;
    private String subjectType;
    private String subjectId;
    private String operationId;
    private String action;
    private String resultStatus;
    private Long actorId;
    private Boolean signaturePresent;
    private LocalDateTime[] occurredAt;

    @Override
    public GxpAuditEventPageQuery setPageNo(Integer pageNo) {
        super.setPageNo(pageNo);
        return this;
    }

    @Override
    public GxpAuditEventPageQuery setPageSize(Integer pageSize) {
        super.setPageSize(pageSize);
        return this;
    }
}
