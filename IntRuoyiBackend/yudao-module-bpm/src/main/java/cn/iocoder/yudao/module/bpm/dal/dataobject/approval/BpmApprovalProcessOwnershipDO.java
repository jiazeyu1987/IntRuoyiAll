package cn.iocoder.yudao.module.bpm.dal.dataobject.approval;

import lombok.Data;
import java.time.LocalDateTime;

/** Immutable INSERT/SELECT fact; no BaseDO, deleted, update or delete API. */
@Data
public class BpmApprovalProcessOwnershipDO {
    private Long id;
    private Long tenantId;
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String businessKey;
    private String projectionCode;
    private String authorityType;
    private String authorityId;
    private String authorityVersion;
    private LocalDateTime createdAt;
    private String writerCode;
    private String evidenceDigest;
}
