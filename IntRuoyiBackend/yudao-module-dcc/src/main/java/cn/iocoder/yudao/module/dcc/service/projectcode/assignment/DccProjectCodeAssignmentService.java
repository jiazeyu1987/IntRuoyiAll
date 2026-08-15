package cn.iocoder.yudao.module.dcc.service.projectcode.assignment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCandidatePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCandidateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentRevokeReqVO;

public interface DccProjectCodeAssignmentService {

    DccProjectCodeAssignmentRespVO createAssignment(Long operatorUserId, Long projectCodeId,
                                                    DccProjectCodeAssignmentCreateReqVO reqVO);

    PageResult<DccProjectCodeAssignmentRespVO> getProjectCodeAssignmentPage(Long projectCodeId,
                                                                            DccProjectCodeAssignmentPageReqVO reqVO);

    PageResult<DccProjectCodeAssignmentRespVO> getMyAssignmentPage(Long userId,
                                                                   DccProjectCodeAssignmentPageReqVO reqVO);

    PageResult<DccProjectCodeAssignmentCandidateRespVO> getAssignmentCandidatePage(Long userId, Long projectCodeId,
                                                                                    DccProjectCodeAssignmentCandidatePageReqVO reqVO);

    PageResult<DccProjectCodeAssignmentFileRespVO> getAssignmentFilePage(Long userId, Long assignmentId,
                                                                         DccProjectCodeAssignmentFilePageReqVO reqVO);

    void revokeAssignment(Long operatorUserId, Long assignmentId, DccProjectCodeAssignmentRevokeReqVO reqVO);

    DccProjectCodeAssignmentAuthorization assertMetadataUpdateAllowed(Long userId, Long fileId, Long assignmentId);

    void markAssignmentFileChanged(Long assignmentId, Long controlledFileId, int changedFieldCount);

}
