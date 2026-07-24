package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentPageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.STATUS_ACTIVE;

@Mapper
public interface DccProjectCodeAssignmentMapper extends BaseMapperX<DccProjectCodeAssignmentDO> {

    default PageResult<DccProjectCodeAssignmentDO> selectPage(Long projectCodeId, Long assigneeUserId,
                                                             DccProjectCodeAssignmentPageReqVO reqVO) {
        LambdaQueryWrapperX<DccProjectCodeAssignmentDO> wrapper =
                new LambdaQueryWrapperX<DccProjectCodeAssignmentDO>()
                        .eqIfPresent(DccProjectCodeAssignmentDO::getProjectCodeId, projectCodeId)
                        .eqIfPresent(DccProjectCodeAssignmentDO::getAssigneeUserId, assigneeUserId)
                        .eqIfPresent(DccProjectCodeAssignmentDO::getStatus, reqVO.getStatus())
                        .betweenIfPresent(DccProjectCodeAssignmentDO::getCreateTime, reqVO.getCreatedTime());
        wrapper.orderByDesc(DccProjectCodeAssignmentDO::getAssignedTime)
                .orderByDesc(DccProjectCodeAssignmentDO::getId);
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        if (keyword != null) {
            wrapper.and(item -> item.like(DccProjectCodeAssignmentDO::getAssignmentNo, keyword)
                    .or()
                    .like(DccProjectCodeAssignmentDO::getAssignmentReason, keyword));
        }
        if (reqVO.getAssigneeUserId() != null) {
            wrapper.eq(DccProjectCodeAssignmentDO::getAssigneeUserId, reqVO.getAssigneeUserId());
        }
        return selectPage(reqVO, wrapper);
    }

    default List<Long> selectActiveProjectCodeIdsByAssigneeUserId(Long assigneeUserId, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<DccProjectCodeAssignmentDO>()
                .eq(DccProjectCodeAssignmentDO::getAssigneeUserId, assigneeUserId)
                .eq(DccProjectCodeAssignmentDO::getStatus, STATUS_ACTIVE)
                .and(wrapper -> wrapper.isNull(DccProjectCodeAssignmentDO::getExpireTime)
                        .or().gt(DccProjectCodeAssignmentDO::getExpireTime, now))
                .orderByDesc(DccProjectCodeAssignmentDO::getAssignedTime)
                .orderByDesc(DccProjectCodeAssignmentDO::getId))
                .stream()
                .map(DccProjectCodeAssignmentDO::getProjectCodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    }

}
