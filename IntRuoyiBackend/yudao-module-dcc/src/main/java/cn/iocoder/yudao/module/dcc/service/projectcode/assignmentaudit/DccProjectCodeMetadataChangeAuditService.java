package cn.iocoder.yudao.module.dcc.service.projectcode.assignmentaudit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentAuditRespVO;

import java.util.List;

public interface DccProjectCodeMetadataChangeAuditService {

    void recordMetadataChange(DccProjectCodeMetadataChangeCommand command);

    PageResult<DccProjectCodeAssignmentAuditRespVO> getAuditPage(DccProjectCodeAssignmentAuditPageReqVO reqVO);

    List<DccProjectCodeAssignmentAuditRespVO> getAuditChangeItems(Long changeId);

}
