package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import com.alibaba.fastjson.JSON;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MesProEdhrSpecialNodeAttachmentHasher {

    private static final String RETENTION_PREFIX = "EDHR_SPECIAL_NODE_ATTACHMENT_V1:RETENTION\n";
    private static final String LEDGER_PREFIX = "EDHR_SPECIAL_NODE_ATTACHMENT_V1:LEDGER\n";

    private MesProEdhrSpecialNodeAttachmentHasher() {
    }

    public static String retentionHash(String retentionJson) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(RETENTION_PREFIX
                + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(retentionJson));
    }

    public static String attachmentHash(MesProBatchRecordExecutionAttachmentDO attachment) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("executionId", attachment.getExecutionId());
        root.put("batchExecutionId", attachment.getBatchExecutionId());
        root.put("batchTaskId", attachment.getBatchTaskId());
        root.put("workTaskId", attachment.getWorkTaskId());
        root.put("rowIndex", attachment.getRowIndex());
        root.put("columnIndex", attachment.getColumnIndex());
        root.put("fieldKey", attachment.getFieldKey());
        root.put("fieldPath", attachment.getFieldPath());
        root.put("attachmentType", attachment.getAttachmentType());
        root.put("attachmentGroupKey", attachment.getAttachmentGroupKey());
        root.put("attachmentAction", attachment.getAttachmentAction());
        root.put("versionNo", attachment.getVersionNo());
        root.put("fileId", attachment.getFileId());
        root.put("fileUrl", attachment.getFileUrl());
        root.put("storageConfigId", attachment.getStorageConfigId());
        root.put("storagePath", attachment.getStoragePath());
        root.put("fileName", attachment.getFileName());
        root.put("contentType", attachment.getContentType());
        root.put("fileSize", attachment.getFileSize());
        root.put("sha256", attachment.getSha256());
        root.put("storageRetentionHash", attachment.getStorageRetentionHash());
        root.put("previousAttachmentHash", attachment.getPreviousAttachmentHash());
        root.put("operatorId", attachment.getOperatorId());
        root.put("operatorName", attachment.getOperatorName());
        root.put("operatedAt", attachment.getOperatedAt().toString());
        root.put("reasonCategory", attachment.getReasonCategory());
        root.put("reasonText", attachment.getReasonText());
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(LEDGER_PREFIX
                + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(JSON.toJSONString(root)));
    }
}
