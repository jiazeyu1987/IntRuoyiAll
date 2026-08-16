package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureImageRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureImageDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureImageMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_IMAGE_HASH_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_IMAGE_PERSIST_FAILED;

@Service
@Validated
public class DccElectronicSignatureImageServiceImpl implements DccElectronicSignatureImageService {

    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String VERIFY_STATUS_VALID = "VALID";
    private static final long MAX_IMAGE_BYTES = 2L * 1024L * 1024L;
    private static final String STORAGE_DIRECTORY = "dcc/signature-images";

    @Resource
    private DccElectronicSignatureImageMapper signatureImageMapper;
    @Resource
    private FileService fileService;

    @Override
    public DccElectronicSignatureImageRespVO getMySignatureImage(Long userId) {
        return toRespVO(signatureImageMapper.selectActiveByUserId(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccElectronicSignatureImageRespVO uploadMySignatureImage(Long userId, MultipartFile file,
                                                                    Long operatorId, String reason) {
        validateUploadFile(file);
        try {
            byte[] content = file.getBytes();
            validateImageContent(content);
            String contentType = normalizeContentType(file.getContentType(), file.getOriginalFilename());
            String sha256 = sha256(content);
            Long fileId = fileService.createFileAndReturnId(content, normalizedFileName(file.getOriginalFilename()),
                    STORAGE_DIRECTORY, contentType);
            FileDO storedFile = requireStoredFile(fileId);
            DccElectronicSignatureImageDO image = DccElectronicSignatureImageDO.builder()
                    .userId(userId)
                    .versionNo(nextVersionNo(userId))
                    .fileId(fileId)
                    .fileUrl(buildAdminFileAccessUrl(storedFile))
                    .storagePath(storedFile.getPath())
                    .fileName(storedFile.getName())
                    .contentType(contentType)
                    .fileSize(storedFile.getSize())
                    .sha256(sha256)
                    .imageStatus(STATUS_UPLOADED)
                    .active(Boolean.FALSE)
                    .uploadedBy(operatorId)
                    .uploadedAt(LocalDateTime.now())
                    .referencedCount(0)
                    .build();
            if (signatureImageMapper.insert(image) <= 0 || image.getId() == null) {
                throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_PERSIST_FAILED);
            }
            return toRespVO(image);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccElectronicSignatureImageRespVO enableMySignatureImage(Long userId, Long imageId, Long operatorId,
                                                                    String reason) {
        if (StrUtil.isBlank(reason)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
        }
        DccElectronicSignatureImageDO image = signatureImageMapper.selectById(imageId);
        if (image == null || !userId.equals(image.getUserId())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
        verifyStoredImage(image);
        LocalDateTime now = LocalDateTime.now();
        signatureImageMapper.deactivateActiveByUserId(userId, imageId, now);
        DccElectronicSignatureImageDO update = DccElectronicSignatureImageDO.builder()
                .id(imageId)
                .imageStatus(STATUS_ACTIVE)
                .active(Boolean.TRUE)
                .enabledAt(now)
                .disabledAt(null)
                .disableReason(null)
                .build();
        if (signatureImageMapper.updateById(update) <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_PERSIST_FAILED);
        }
        return toRespVO(signatureImageMapper.selectById(imageId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccElectronicSignatureImageRespVO disableMySignatureImage(Long userId, Long operatorId, String reason) {
        if (StrUtil.isBlank(reason)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
        }
        DccElectronicSignatureImageDO image = signatureImageMapper.selectActiveByUserId(userId);
        if (image == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
        DccElectronicSignatureImageDO update = DccElectronicSignatureImageDO.builder()
                .id(image.getId())
                .imageStatus(STATUS_DISABLED)
                .active(Boolean.FALSE)
                .disabledAt(LocalDateTime.now())
                .disableReason(StrUtil.trim(reason))
                .build();
        if (signatureImageMapper.updateById(update) <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_PERSIST_FAILED);
        }
        return toRespVO(signatureImageMapper.selectById(image.getId()));
    }

    @Override
    public DccElectronicSignatureImageSnapshot requireActiveSnapshot(Long userId) {
        DccElectronicSignatureImageDO image = signatureImageMapper.selectActiveByUserId(userId);
        if (image == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
        byte[] content = verifyStoredImage(image);
        return toSnapshot(image, VERIFY_STATUS_VALID, content);
    }

    @Override
    public DccElectronicSignatureImageSnapshot verifySignatureSnapshot(DccControlledFileSignatureDO signature) {
        if (signature == null || signature.getSignatureImageId() == null || signature.getSignatureImageFileId() == null
                || StrUtil.hasBlank(signature.getSignatureImageSha256(), signature.getSignatureImageStatusSnapshot())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
        DccElectronicSignatureImageDO image = signatureImageMapper.selectById(signature.getSignatureImageId());
        if (image == null || !signature.getSignatureImageFileId().equals(image.getFileId())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
        byte[] content = verifyStoredImage(image);
        if (!StrUtil.equalsIgnoreCase(signature.getSignatureImageSha256(), sha256(content))) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_HASH_MISMATCH);
        }
        return DccElectronicSignatureImageSnapshot.builder()
                .imageId(signature.getSignatureImageId())
                .versionNo(signature.getSignatureImageVersionNo())
                .fileId(signature.getSignatureImageFileId())
                .fileUrl(signature.getSignatureImageFileUrl())
                .fileName(image.getFileName())
                .contentType(signature.getSignatureImageContentType())
                .fileSize(signature.getSignatureImageFileSize())
                .sha256(signature.getSignatureImageSha256())
                .imageStatus(signature.getSignatureImageStatusSnapshot())
                .verifiedStatus(VERIFY_STATUS_VALID)
                .content(content)
                .build();
    }

    @Override
    public DccElectronicSignatureImageSnapshot verifySignatureSnapshot(DccElectronicSignatureImageSnapshot signature) {
        if (signature == null || signature.getImageId() == null || signature.getFileId() == null
                || StrUtil.hasBlank(signature.getSha256(), signature.getImageStatus())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
        DccElectronicSignatureImageDO image = signatureImageMapper.selectById(signature.getImageId());
        if (image == null || !signature.getFileId().equals(image.getFileId())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
        byte[] content = verifyStoredImage(image);
        if (!StrUtil.equalsIgnoreCase(signature.getSha256(), sha256(content))) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_HASH_MISMATCH);
        }
        return DccElectronicSignatureImageSnapshot.builder()
                .imageId(signature.getImageId())
                .versionNo(signature.getVersionNo())
                .fileId(signature.getFileId())
                .fileUrl(signature.getFileUrl())
                .fileName(image.getFileName())
                .contentType(signature.getContentType())
                .fileSize(signature.getFileSize())
                .sha256(signature.getSha256())
                .imageStatus(signature.getImageStatus())
                .verifiedStatus(signature.getVerifiedStatus())
                .content(content)
                .build();
    }

    @Override
    public void markReferenced(Long imageId) {
        if (imageId == null) {
            return;
        }
        signatureImageMapper.update(null, new LambdaUpdateWrapper<DccElectronicSignatureImageDO>()
                .eq(DccElectronicSignatureImageDO::getId, imageId)
                .setSql("referenced_count = COALESCE(referenced_count, 0) + 1"));
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0 || file.getSize() > MAX_IMAGE_BYTES) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
        }
        normalizeContentType(file.getContentType(), file.getOriginalFilename());
    }

    private void validateImageContent(byte[] content) {
        if (content == null || content.length == 0 || content.length > MAX_IMAGE_BYTES) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(content));
        } catch (IOException | RuntimeException ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
        }
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
        }
    }

    private String normalizeContentType(String rawContentType, String fileName) {
        String contentType = StrUtil.trimToEmpty(rawContentType).toLowerCase(Locale.ROOT);
        String lowerFileName = StrUtil.trimToEmpty(fileName).toLowerCase(Locale.ROOT);
        if ("image/png".equals(contentType) || lowerFileName.endsWith(".png")) {
            return "image/png";
        }
        if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType)
                || lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
    }

    private String normalizedFileName(String fileName) {
        String normalized = StrUtil.trimToNull(fileName);
        return normalized == null ? "signature-image.png" : normalized;
    }

    private Integer nextVersionNo(Long userId) {
        Integer maxVersionNo = signatureImageMapper.selectMaxVersionNoByUserId(userId);
        return maxVersionNo == null ? 1 : maxVersionNo + 1;
    }

    private FileDO requireStoredFile(Long fileId) {
        FileDO storedFile = fileService.getFile(fileId);
        if (storedFile == null || storedFile.getConfigId() == null || StrUtil.isBlank(storedFile.getPath())) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
        return storedFile;
    }

    private static String buildAdminFileAccessUrl(FileDO file) {
        return "/admin-api/infra/file/" + file.getConfigId() + "/get/"
                + UriUtils.encodePath(file.getPath(), StandardCharsets.UTF_8);
    }

    private byte[] verifyStoredImage(DccElectronicSignatureImageDO image) {
        try {
            FileDO file = requireStoredFile(image.getFileId());
            byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
            if (content == null || content.length == 0) {
                throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
            }
            String actualHash = sha256(content);
            if (!StrUtil.equalsIgnoreCase(image.getSha256(), actualHash)) {
                throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_HASH_MISMATCH);
            }
            return content;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_MISSING);
        }
    }

    private DccElectronicSignatureImageSnapshot toSnapshot(DccElectronicSignatureImageDO image, String verifiedStatus,
                                                           byte[] content) {
        return DccElectronicSignatureImageSnapshot.builder()
                .imageId(image.getId())
                .versionNo(image.getVersionNo())
                .fileId(image.getFileId())
                .fileUrl(image.getFileUrl())
                .fileName(image.getFileName())
                .contentType(image.getContentType())
                .fileSize(image.getFileSize())
                .sha256(image.getSha256())
                .imageStatus(image.getImageStatus())
                .verifiedStatus(verifiedStatus)
                .content(content)
                .build();
    }

    private DccElectronicSignatureImageRespVO toRespVO(DccElectronicSignatureImageDO image) {
        if (image == null) {
            return null;
        }
        DccElectronicSignatureImageRespVO respVO = new DccElectronicSignatureImageRespVO();
        respVO.setId(image.getId());
        respVO.setUserId(image.getUserId());
        respVO.setVersionNo(image.getVersionNo());
        respVO.setFileId(image.getFileId());
        respVO.setFileUrl(buildAdminFileAccessUrl(requireStoredFile(image.getFileId())));
        respVO.setFileName(image.getFileName());
        respVO.setContentType(image.getContentType());
        respVO.setFileSize(image.getFileSize());
        respVO.setSha256(image.getSha256());
        respVO.setSha256Short(shortHash(image.getSha256()));
        respVO.setImageStatus(image.getImageStatus());
        respVO.setActive(image.getActive());
        respVO.setUploadedBy(image.getUploadedBy());
        respVO.setUploadedAt(image.getUploadedAt());
        respVO.setEnabledAt(image.getEnabledAt());
        respVO.setDisabledAt(image.getDisabledAt());
        respVO.setDisableReason(image.getDisableReason());
        respVO.setReferencedCount(image.getReferencedCount());
        return respVO;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_SIGNATURE_IMAGE_INVALID);
        }
    }

    private static String shortHash(String hash) {
        if (StrUtil.isBlank(hash)) {
            return "";
        }
        return hash.length() <= 12 ? hash.toLowerCase(Locale.ROOT) : hash.substring(0, 12).toLowerCase(Locale.ROOT);
    }
}
