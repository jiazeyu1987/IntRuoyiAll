package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import java.util.List;

/** Exact server facts; numeric business identities are serialized as decimal strings. */
public record DccLifecycleProjectionRepairPreviewRespVO(
        String fileId,String masterId,String versionRefId,String tenantId,String versionNo,
        String dccStatus,String canonicalStatus,String domainStatus,String processInstanceId,
        String controlledTime,String activatedTime,String approvedTime,String publishedTime,String effectiveDate,
        String publishedFileId,String stampedFileId,List<String> signatureIds,String latestControlledFileId,
        String currentActiveControlledFileId,String sourceFactsHash,String preimageHash,boolean canRepair,
        String expectedTargetStatus,List<String> expectedActions) { }
