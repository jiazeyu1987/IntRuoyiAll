package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCurrentVersionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileWithdrawReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignatureActionRespVO;

import java.util.List;

public interface DccControlledFileWorkflowService {

    DccControlledFileRouteReadinessRespVO previewRoute(Long userId, Long categoryId,
                                                       List<Long> selectedSignoffUserIds);

    DccControlledFileCurrentVersionRespVO getCurrentVersionByFileNumber(Long userId, String fileNumber);

    Long submitControlledFile(Long userId, DccControlledFileSubmitReqVO reqVO);

    Long submitControlledFileWithoutApproval(Long userId, DccControlledFileSubmitReqVO reqVO);

    PageResult<DccControlledFileRespVO> getUploadRevisionCandidates(Long userId, Long dccProjectCodeId,
                                                                    Long fileTypeTaxonomyId, String keyword,
                                                                    Integer pageNo, Integer pageSize);

    Long submitControlledFileWithoutApproval(Long userId, DccControlledFileSubmitReqVO reqVO,
                                             String approvalProcessInstanceId, String platformEventKey);

    PageResult<DccControlledFileRespVO> getControlledFilePage(Long userId, DccControlledFilePageReqVO reqVO);

    DccControlledFileRespVO getControlledFile(Long id);

    void withdrawControlledFile(Long userId, Long id, DccControlledFileWithdrawReqVO reqVO);

    void deleteWithdrawnControlledFile(Long userId, Long id);

    Long resubmitWithdrawnControlledFile(Long userId, Long id);

    void uploadTrainingRecord(Long userId, Long id, DccControlledFileTrainingRecordReqVO reqVO);

    DccControlledFileTaskReadinessRespVO getTaskActionReadiness(Long userId, Long id,
                                                               DccControlledFileTaskReadinessReqVO reqVO);

    DccSignatureActionRespVO approveTask(Long userId, Long id, DccControlledFileApproveTaskReqVO reqVO);

    DccSignatureActionRespVO rejectTask(Long userId, Long id, DccControlledFileRejectTaskReqVO reqVO);

    void returnTask(Long userId, Long id, DccControlledFileReturnTaskReqVO reqVO);

    void transferTask(Long userId, Long id, DccControlledFileTransferTaskReqVO reqVO);

    void createSignTask(Long userId, Long id, DccControlledFileCreateSignTaskReqVO reqVO);
}
