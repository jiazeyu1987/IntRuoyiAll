package cn.iocoder.yudao.module.bpm.approval.service.signature;

public interface ApprovalSignatureImageSnapshotProvider {

    ApprovalSignatureImageSnapshot requireActiveSnapshot(Long userId);

    ApprovalSignatureImageSnapshot requireActiveSnapshotLocked(Long userId);

    void markReferencedLocked(Long userId, Long imageId);

    void markReferenced(Long imageId);
}
