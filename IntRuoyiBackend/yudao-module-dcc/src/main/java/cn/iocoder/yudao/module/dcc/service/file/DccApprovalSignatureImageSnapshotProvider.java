package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureImageSnapshot;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureImageSnapshotProvider;
import org.springframework.stereotype.Service;

@Service
public class DccApprovalSignatureImageSnapshotProvider implements ApprovalSignatureImageSnapshotProvider {

    private final DccElectronicSignatureImageService signatureImageService;

    public DccApprovalSignatureImageSnapshotProvider(DccElectronicSignatureImageService signatureImageService) {
        this.signatureImageService = signatureImageService;
    }

    @Override
    public ApprovalSignatureImageSnapshot requireActiveSnapshot(Long userId) {
        DccElectronicSignatureImageSnapshot snapshot = signatureImageService.requireActiveSnapshot(userId);
        return toApprovalSnapshot(snapshot);
    }

    @Override
    public ApprovalSignatureImageSnapshot requireActiveSnapshotLocked(Long userId) {
        return toApprovalSnapshot(signatureImageService.requireActiveSnapshotLocked(userId));
    }

    @Override
    public void markReferencedLocked(Long userId, Long imageId) {
        signatureImageService.markReferencedLocked(userId, imageId);
    }

    private static ApprovalSignatureImageSnapshot toApprovalSnapshot(DccElectronicSignatureImageSnapshot snapshot) {
        return ApprovalSignatureImageSnapshot.builder()
                .imageId(snapshot.getImageId())
                .versionNo(snapshot.getVersionNo())
                .fileId(snapshot.getFileId())
                .fileUrl(snapshot.getFileUrl())
                .sha256(snapshot.getSha256())
                .contentType(snapshot.getContentType())
                .fileSize(snapshot.getFileSize())
                .imageStatus(snapshot.getImageStatus())
                .verifiedStatus(snapshot.getVerifiedStatus())
                .build();
    }

    @Override
    public void markReferenced(Long imageId) {
        signatureImageService.markReferenced(imageId);
    }
}
