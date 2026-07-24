package cn.iocoder.yudao.module.bpm.approval.service.signature;

public interface ApprovalSignatureImageSnapshotProvider {

    ApprovalSignatureImageSnapshot requireActiveSnapshot(Long userId);

    void markReferenced(Long imageId);
}
