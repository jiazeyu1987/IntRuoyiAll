package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccPositionAssignmentSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;

import java.util.List;

public interface DccApprovalPositionAdminService {
    List<DccApprovalPositionDO> getPositionList();
    DccApprovalPositionDO createPosition(String name, String changeReason);
    DccApprovalPositionImportResult importPositionsFromIntAuth();
    List<DccPositionAssignmentDO> getAssignments(Long positionId);
    List<DccPositionAssignmentDO> replaceAssignments(Long positionId, List<DccPositionAssignmentSaveReqVO> reqVOList);
}
