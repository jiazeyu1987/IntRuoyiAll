package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewSubmitReqVO;

public interface DccExternalFileReviewService {

    Long submitExternalReview(Long userId, DccExternalFileReviewSubmitReqVO reqVO);

    void approveTask(Long userId, Long id, DccExternalFileReviewApproveTaskReqVO reqVO);

    void rejectTask(Long userId, Long id, DccControlledFileRejectTaskReqVO reqVO);

    void returnTask(Long userId, Long id, DccControlledFileReturnTaskReqVO reqVO);

    void transferTask(Long userId, Long id, DccControlledFileTransferTaskReqVO reqVO);

    void createSignTask(Long userId, Long id, DccControlledFileCreateSignTaskReqVO reqVO);
}
