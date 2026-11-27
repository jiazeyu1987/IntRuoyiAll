package cn.iocoder.yudao.module.dcc.service.position;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccPositionAssignmentSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionAdminErrors.NAME_EXISTS;
import static cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionAdminErrors.WRITE_INCOMPLETE;

@Service
@Validated
public class DccApprovalPositionAdminServiceImpl implements DccApprovalPositionAdminService {

    @Resource
    private DccApprovalPositionMapper positionMapper;
    @Resource
    private DccPositionAssignmentMapper assignmentMapper;

    @Override
    public List<DccApprovalPositionDO> getPositionList() {
        return positionMapper.selectList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @com.mzt.logapi.starter.annotation.LogRecord(type = "DCC审批角色", subType = "新增审批角色",
            bizNo = "{{#name}}", success = "新增审批角色 {{#name}}，原因：{{#changeReason}}")
    public DccApprovalPositionDO createPosition(String name, String changeReason) {
        String normalizedName = name.trim();
        if (positionMapper.selectCount(DccApprovalPositionDO::getName, normalizedName) > 0) {
            throw exception(NAME_EXISTS);
        }
        DccApprovalPositionDO position = DccApprovalPositionDO.builder()
                .code("LOCAL-" + IdUtil.fastSimpleUUID())
                .name(normalizedName)
                .active(Boolean.TRUE)
                .source("LOCAL")
                .remark(changeReason.trim())
                .build();
        if (positionMapper.insert(position) != 1 || position.getId() == null) {
            throw exception(WRITE_INCOMPLETE);
        }
        return position;
    }

    @Override
    public List<DccPositionAssignmentDO> getAssignments(Long positionId) {
        validatePositionExists(positionId);
        return assignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, positionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DccPositionAssignmentDO> replaceAssignments(Long positionId, List<DccPositionAssignmentSaveReqVO> reqVOList) {
        DccApprovalPositionDO position = requirePosition(positionId);
        if (DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position) && !reqVOList.isEmpty()) {
            throw exception(APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED, position.getName());
        }
        for (DccPositionAssignmentSaveReqVO row : reqVOList) {
            DccPositionAssignmentTypes.validate(row.getAssignmentType(), row.getUserId(), row.getSystemPostId());
        }
        assignmentMapper.delete(DccPositionAssignmentDO::getPositionId, positionId);
        if (DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position)) {
            return List.of();
        }
        List<DccPositionAssignmentDO> assignments = CollectionUtils.convertList(reqVOList, reqVO -> {
            DccPositionAssignmentDO assignment = BeanUtils.toBean(reqVO, DccPositionAssignmentDO.class);
            assignment.setPositionId(positionId);
            return assignment;
        });
        assignments.forEach(assignmentMapper::insert);
        return assignments;
    }

    private void validatePositionExists(Long positionId) {
        requirePosition(positionId);
    }

    private DccApprovalPositionDO requirePosition(Long positionId) {
        DccApprovalPositionDO position = positionMapper.selectById(positionId);
        if (position == null) {
            throw exception(APPROVAL_POSITION_NOT_EXISTS);
        }
        return position;
    }

}
