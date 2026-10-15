package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentRespVO;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccTrainingUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentUploadTicketReqVO;

import java.util.List;

public interface DccControlledFileAttachmentService {

    void bindAttachments(Long controlledFileId, List<DccUploadTicketBoundFile> attachmentFiles);

    /** Called before the surrounding check-in transaction releases its exact logical checkout. */
    void bindCheckinAttachments(Long actorId, Long baseFileId, Long workingFileId,
                               List<DccControlledFileAttachmentUploadTicketReqVO> tickets);

    /** Freeze only the selected body's ordinary facts and independent bytes; never rebind its tickets. */
    void freezeOrdinaryAttachments(Long actorId, Long selectedFileId, Long candidateFileId,
                                   List<Long> expectedOrdinaryAttachmentIds);

    /** Server-only, ordered immutable facts; both the byte digest and original uploader snapshots are verified. */
    OrdinaryAttachmentManifest readOrdinaryAttachmentManifest(Long actorId, Long selectedFileId);

    void requireFrozenOrdinaryAttachmentManifest(Long actorId, Long selectedFileId, Long candidateFileId,
                                                 OrdinaryAttachmentManifest expected);

    record OrdinaryAttachmentFact(Long attachmentId, Long controlledFileId, Long storageFileId, Integer sortNo,
            String purpose, String fileName, String contentType, Long fileSize, String fileSha256, Long uploadedBy,
            String uploadedUsername, String uploadedNickname, java.time.LocalDateTime uploadedAt) { }
    record OrdinaryAttachmentManifest(List<OrdinaryAttachmentFact> facts, String payloadHash) {
        public OrdinaryAttachmentManifest {
            facts = List.copyOf(facts);
            if (payloadHash == null || !payloadHash.matches("[a-f0-9]{64}"))
                throw new IllegalArgumentException("普通附件事实清单缺少正式服务器摘要");
        }
        public List<Long> attachmentIds() { return facts.stream().map(OrdinaryAttachmentFact::attachmentId).toList(); }
    }

    void bindApplicationRound(Long controlledFileId, String processInstanceId);

    Long bindTrainingRecord(Long actorId, Long controlledFileId, String processInstanceId,
                            DccTrainingUploadTicketBoundFile trainingFile);

    List<DccControlledFileAttachmentRespVO> listAttachments(Long controlledFileId);

    DccControlledFileAttachmentBinary getAttachment(Long controlledFileId, Long attachmentId);

    List<Long> listControlledFileIdsByStorageFileId(Long storageFileId);

}
