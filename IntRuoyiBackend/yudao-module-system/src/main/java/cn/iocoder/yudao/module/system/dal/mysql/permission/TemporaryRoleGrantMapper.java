package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TemporaryRoleGrantMapper extends BaseMapperX<TemporaryRoleGrantDO> {

    default PageResult<TemporaryRoleGrantDO> selectPage(TemporaryRoleGrantPageReqVO reqVO) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiringSoonDeadline = now.plusHours(24);
        LambdaQueryWrapperX<TemporaryRoleGrantDO> wrapper = new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eqIfPresent(TemporaryRoleGrantDO::getUserId, reqVO.getUserId())
                .eqIfPresent(TemporaryRoleGrantDO::getRoleId, reqVO.getRoleId())
                .eqIfPresent(TemporaryRoleGrantDO::getStatus, reqVO.getStatus());
        if ("ACTIVE".equals(reqVO.getReviewCategory())) {
            wrapper.eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                    .gt(TemporaryRoleGrantDO::getExpireTime, expiringSoonDeadline);
        } else if ("EXPIRING_SOON".equals(reqVO.getReviewCategory())) {
            wrapper.eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                    .gt(TemporaryRoleGrantDO::getExpireTime, now)
                    .le(TemporaryRoleGrantDO::getExpireTime, expiringSoonDeadline);
        } else if ("OVERDUE".equals(reqVO.getReviewCategory())) {
            wrapper.eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                    .le(TemporaryRoleGrantDO::getExpireTime, now);
        }
        return selectPage(reqVO, wrapper.orderByDesc(TemporaryRoleGrantDO::getCreateTime));
    }

    default List<TemporaryRoleGrantDO> selectActiveListByUserId(Long userId, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eq(TemporaryRoleGrantDO::getUserId, userId)
                .eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                .le(TemporaryRoleGrantDO::getEffectiveTime, now)
                .gt(TemporaryRoleGrantDO::getExpireTime, now));
    }

    default List<TemporaryRoleGrantDO> selectOpenListByUserIdAndRoleId(Long userId, Long roleId, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eq(TemporaryRoleGrantDO::getUserId, userId)
                .eq(TemporaryRoleGrantDO::getRoleId, roleId)
                .in(TemporaryRoleGrantDO::getStatus, List.of("PENDING", "ACTIVE"))
                .gt(TemporaryRoleGrantDO::getExpireTime, now));
    }

    default List<TemporaryRoleGrantDO> selectOverdueActiveList(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                .le(TemporaryRoleGrantDO::getExpireTime, now));
    }

    default List<TemporaryRoleGrantDO> selectExpiringSoonUnremindedList(LocalDateTime now, LocalDateTime deadline) {
        return selectList(new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                .gt(TemporaryRoleGrantDO::getExpireTime, now)
                .le(TemporaryRoleGrantDO::getExpireTime, deadline)
                .isNull(TemporaryRoleGrantDO::getRemindTime));
    }

    default Long selectActiveCountAfter(LocalDateTime deadline) {
        return selectCount(new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                .gt(TemporaryRoleGrantDO::getExpireTime, deadline));
    }

    default Long selectExpiringSoonCount(LocalDateTime now, LocalDateTime deadline) {
        return selectCount(new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                .gt(TemporaryRoleGrantDO::getExpireTime, now)
                .le(TemporaryRoleGrantDO::getExpireTime, deadline));
    }

    default Long selectOverdueCount(LocalDateTime now) {
        return selectCount(new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eq(TemporaryRoleGrantDO::getStatus, "ACTIVE")
                .le(TemporaryRoleGrantDO::getExpireTime, now));
    }

}
