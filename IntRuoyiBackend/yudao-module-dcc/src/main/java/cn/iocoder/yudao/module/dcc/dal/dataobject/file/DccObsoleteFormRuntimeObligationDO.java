package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import java.time.LocalDateTime;

/** Immutable submitted form evidence; persistence only appends new rows. */
public record DccObsoleteFormRuntimeObligationDO(
    Long id,
    Long tenantId,
    Long headId,
    Long formInstanceId,
    Long controlledFileId,
    String bpmProcessInstanceId,
    String obligationId,
    String stageCode,
    String nodeInstanceId,
    Integer stageNo,
    Integer stageOrder,
    Long departmentId,
    String candidateSourceType,
    String candidateSourceIdsJson,
    String frozenUserIdsJson,
    String approveMethod,
    Integer approveRatio,
    Boolean requireAllApprovals,
    LocalDateTime createdAt) { }
