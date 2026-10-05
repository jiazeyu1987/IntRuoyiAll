package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

public record DccLifecycleProjectionRepairRespVO(
        String fileId,String masterId,String versionRefId,String processInstanceId,String status,
        String canonicalStatus,String domainStatus,String sourceFactsHash,String preimageHash,
        String idempotencyKey,String auditEventId,String repairedAt) { }
