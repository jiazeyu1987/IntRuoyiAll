package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureImageRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import org.springframework.web.multipart.MultipartFile;

public interface DccElectronicSignatureImageService {

    DccElectronicSignatureImageRespVO getMySignatureImage(Long userId);

    /** Initialize only users with no image history; preserve uploaded and disabled versions. */
    DccElectronicSignatureImageRespVO initializeMyDefaultSignatureImage(Long userId);

    DccElectronicSignatureImageRespVO uploadMySignatureImage(Long userId, MultipartFile file, Long operatorId, String reason);

    DccElectronicSignatureImageRespVO enableMySignatureImage(Long userId, Long imageId, Long operatorId, String reason);

    DccElectronicSignatureImageRespVO disableMySignatureImage(Long userId, Long operatorId, String reason);

    /** Read only: never initialize a new image for historical evidence queries. */
    DccElectronicSignatureImageSnapshot requireActiveSnapshot(Long userId);

    /** Capture for a new signature, creating a persisted default only when image history is empty. */
    DccElectronicSignatureImageSnapshot requireSigningSnapshot(Long userId);

    /** Read-only readiness check for a user who has never configured an image. */
    boolean canInitializeDefaultSignatureImage(Long userId);

    DccElectronicSignatureImageSnapshot requireActiveSnapshotLocked(Long userId);

    void markReferencedLocked(Long userId, Long imageId);

    DccElectronicSignatureImageSnapshot verifySignatureSnapshot(DccControlledFileSignatureDO signature);

    DccElectronicSignatureImageSnapshot verifySignatureSnapshot(DccElectronicSignatureImageSnapshot signature);

    void markReferenced(Long imageId);
}
