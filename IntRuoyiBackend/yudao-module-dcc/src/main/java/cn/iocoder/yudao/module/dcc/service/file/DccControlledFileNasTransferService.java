package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportBatchReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportChunkReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportChunkRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportSessionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportUploadStateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasOriginalPathSyncReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportLocalWriteResultReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportSelectedReqVO;

public interface DccControlledFileNasTransferService {

    DccControlledFileNasTransferRespVO transfer(Long userId, DccControlledFileNasTransferReqVO reqVO);

    DccControlledFileNasTransferRespVO importLocalFolder(Long userId, DccControlledFileLocalFolderImportReqVO reqVO);

    DccControlledFileNasTransferRespVO createUncontrolledImportTask(
            Long userId, Long auditTaskId, DccNasUncontrolledImportSelectedReqVO reqVO);

    DccControlledFileNasTransferRespVO createOriginalPathSyncTask(
            Long userId, Long auditTaskId, DccNasOriginalPathSyncReqVO reqVO);

    void deleteOriginalPathSyncFile(Long userId, Long syncFileId);

    DccControlledFileBinary readUncontrolledImportContent(
            Long userId, Long importTaskId, Long auditFileId, String sourceSignature, String localRelativePath);

    DccControlledFileNasTransferRespVO recordUncontrolledImportLocalWriteResult(
            Long userId, Long importTaskId, Long auditFileId,
            DccNasUncontrolledImportLocalWriteResultReqVO reqVO);

    DccControlledFileNasTransferRespVO createLocalFolderImportSession(
            Long userId, DccControlledFileLocalFolderImportSessionCreateReqVO reqVO);

    DccControlledFileNasTransferRespVO uploadLocalFolderImportBatch(
            Long userId, Long taskId, DccControlledFileLocalFolderImportBatchReqVO reqVO);

    DccControlledFileLocalFolderImportUploadStateRespVO getLocalFolderImportUploadState(Long userId, Long taskId);

    DccControlledFileLocalFolderImportChunkRespVO uploadLocalFolderImportChunk(
            Long userId, Long taskId, DccControlledFileLocalFolderImportChunkReqVO reqVO);

    DccControlledFileNasTransferRespVO completeLocalFolderImportSession(Long userId, Long taskId);

    DccControlledFileNasTransferRespVO getTask(Long userId, Long taskId);

    void recoverInterruptedTasksOnStartup();

    void processWaitingTasks();
}
