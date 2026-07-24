package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureImageRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import org.springframework.web.multipart.MultipartFile;

public interface DccElectronicSignatureImageService {

    DccElectronicSignatureImageRespVO getMySignatureImage(Long userId);

    DccElectronicSignatureImageRespVO uploadMySignatureImage(Long userId, MultipartFile file, Long operatorId, String reason);

    DccElectronicSignatureImageRespVO enableMySignatureImage(Long userId, Long imageId, Long operatorId, String reason);

    DccElectronicSignatureImageRespVO disableMySignatureImage(Long userId, Long operatorId, String reason);

    DccElectronicSignatureImageSnapshot requireActiveSnapshot(Long userId);

    DccElectronicSignatureImageSnapshot verifySignatureSnapshot(DccControlledFileSignatureDO signature);

    DccElectronicSignatureImageSnapshot verifySignatureSnapshot(DccElectronicSignatureImageSnapshot signature);

    void markReferenced(Long imageId);
}
