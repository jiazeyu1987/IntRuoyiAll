package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import java.time.LocalDateTime;

/** Immutable submitted form evidence; persistence only appends new rows. */
public record DccObsoleteFormRuntimeSnapshotDO(
    Long id,
    Long tenantId,
    Long formInstanceId,
    Long controlledFileId,
    Long projectId,
    Long applicantUserId,
    String formInstanceCode,
    String controlledFileVersionNo,
    String bpmProcessInstanceId,
    Integer attributeRound,
    String preparedProcessDefinitionId,
    String preparedDefinitionDigest,
    String executionContract,
    String canonicalPreparationJson,
    String preparationDigest,
    LocalDateTime createdAt) { }
