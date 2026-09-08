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
        return selectPage(reqVO, new LambdaQueryWrapperX<TemporaryRoleGrantDO>()
                .eqIfPresent(TemporaryRoleGrantDO::getUserId, reqVO.getUserId())
                .eqIfPresent(TemporaryRoleGrantDO::getRoleId, reqVO.getRoleId())
                .eqIfPresent(TemporaryRoleGrantDO::getStatus, reqVO.getStatus())
                .orderByDesc(TemporaryRoleGrantDO::getCreateTime));
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

}
