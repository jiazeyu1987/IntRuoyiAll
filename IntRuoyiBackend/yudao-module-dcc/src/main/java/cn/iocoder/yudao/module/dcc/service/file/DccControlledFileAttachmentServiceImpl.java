package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccControlledFileAttachmentServiceImpl implements DccControlledFileAttachmentService {

    @Resource
    private DccControlledFileAttachmentMapper attachmentMapper;

    @Override
    public void bindAttachments(Long controlledFileId, List<DccUploadTicketBoundFile> attachmentFiles) {
        if (controlledFileId == null || attachmentFiles == null || attachmentFiles.isEmpty()) {
            return;
        }
        int sortNo = 0;
        for (DccUploadTicketBoundFile file : attachmentFiles) {
            if (file == null || file.storageFileId() == null || StrUtil.isBlank(file.fileName())) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
            attachmentMapper.insert(DccControlledFileAttachmentDO.builder()
                    .controlledFileId(controlledFileId)
                    .storageFileId(file.storageFileId())
                    .originalFileName(file.fileName())
                    .contentType(file.contentType())
                    .fileSize(file.fileSize())
                    .sortNo(sortNo++)
                    .build());
        }
    }

    @Override
    public List<DccControlledFileAttachmentRespVO> listAttachments(Long controlledFileId) {
        return attachmentMapper.selectListByControlledFileId(controlledFileId).stream()
                .map(this::toRespVO)
                .toList();
    }

    @Override
    public DccControlledFileAttachmentBinary getAttachment(Long controlledFileId, Long attachmentId) {
        DccControlledFileAttachmentDO attachment = attachmentId == null ? null : attachmentMapper.selectById(attachmentId);
        if (attachment == null || !controlledFileId.equals(attachment.getControlledFileId())) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        return new DccControlledFileAttachmentBinary(attachment.getId(), attachment.getStorageFileId(),
                attachment.getOriginalFileName(), attachment.getContentType(), attachment.getFileSize());
    }

    @Override
    public List<Long> listControlledFileIdsByStorageFileId(Long storageFileId) {
        if (storageFileId == null) {
            return List.of();
        }
        return attachmentMapper.selectList(new LambdaQueryWrapperX<DccControlledFileAttachmentDO>()
                        .eq(DccControlledFileAttachmentDO::getStorageFileId, storageFileId))
                .stream()
                .map(DccControlledFileAttachmentDO::getControlledFileId)
                .distinct()
                .toList();
    }

    private DccControlledFileAttachmentRespVO toRespVO(DccControlledFileAttachmentDO attachment) {
        DccControlledFileAttachmentRespVO respVO = new DccControlledFileAttachmentRespVO();
        respVO.setAttachmentId(attachment.getId());
        respVO.setControlledFileId(attachment.getControlledFileId());
        respVO.setFileName(attachment.getOriginalFileName());
        respVO.setContentType(attachment.getContentType());
        respVO.setFileSize(attachment.getFileSize());
        respVO.setSortNo(attachment.getSortNo());
        respVO.setPreviewKind(DccControlledFilePreviewKindEnum
                .resolve(attachment.getOriginalFileName(), attachment.getContentType()).getCode());
        return respVO;
    }

}
