package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentRespVO;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;

import java.util.List;

public interface DccControlledFileAttachmentService {

    void bindAttachments(Long controlledFileId, List<DccUploadTicketBoundFile> attachmentFiles);

    List<DccControlledFileAttachmentRespVO> listAttachments(Long controlledFileId);

    DccControlledFileAttachmentBinary getAttachment(Long controlledFileId, Long attachmentId);

    List<Long> listControlledFileIdsByStorageFileId(Long storageFileId);

}
