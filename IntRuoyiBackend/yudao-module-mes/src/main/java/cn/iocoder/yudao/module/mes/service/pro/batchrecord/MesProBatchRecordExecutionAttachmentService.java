package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;

import java.util.List;

public interface MesProBatchRecordExecutionAttachmentService {

    MesProBatchRecordExecutionAttachmentPrepareUploadResult prepareUpload(
            MesProBatchRecordExecutionAttachmentPrepareUploadCommand command);

    Long bindAttachment(MesProBatchRecordExecutionAttachmentBindCommand command);

    Long replaceAttachment(MesProBatchRecordExecutionAttachmentBindCommand command);

    Long voidAttachment(MesProBatchRecordExecutionAttachmentVoidCommand command);

    List<MesProBatchRecordExecutionAttachmentDO> listByExecution(Long executionId);

    List<MesProBatchRecordExecutionAttachmentDO> listByField(Long executionId, String fieldPath, String fieldKey);

    MesProBatchRecordExecutionAttachmentChainVerifyResult verifyAttachmentChain(Long executionId);
}
