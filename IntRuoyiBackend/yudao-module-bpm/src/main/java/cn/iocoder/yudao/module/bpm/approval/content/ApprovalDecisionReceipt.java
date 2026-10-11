package cn.iocoder.yudao.module.bpm.approval.content;
public record ApprovalDecisionReceipt(String receiptId, String idempotencyKey, String prepareId,
        String signatureId, String evidenceHash, String resultCode, String resultingStatus, String committedAt) { }
