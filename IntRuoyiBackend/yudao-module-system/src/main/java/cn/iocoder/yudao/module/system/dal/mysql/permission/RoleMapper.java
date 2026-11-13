package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapperX<RoleDO> {

    default RoleDO selectPermissionSubjectForUpdate(Long tenantId, Long roleId) {
        return selectOne(new LambdaQueryWrapperX<RoleDO>()
                .eq(RoleDO::getTenantId, tenantId).eq(RoleDO::getId, roleId).last("FOR UPDATE"));
    }

    default PageResult<RoleDO> selectPage(RolePageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<RoleDO>()
                .likeIfPresent(RoleDO::getName, reqVO.getName())
                .likeIfPresent(RoleDO::getCode, reqVO.getCode())
                .eqIfPresent(RoleDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(RoleDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BaseDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(RoleDO::getSort), java.util.Map.ofEntries(
                java.util.Map.entry("id", number("id")),
                java.util.Map.entry("name", text("name")),
                java.util.Map.entry("code", text("code")),
                java.util.Map.entry("sort", number("sort")),
                java.util.Map.entry("status", number("status")),
                java.util.Map.entry("remark", text("remark")),
                java.util.Map.entry("type", number("type")),
                java.util.Map.entry("createTime", dateTime("create_time")),
                java.util.Map.entry("categoryName", text("(SELECT c.name FROM system_role_category c WHERE c.id=system_role.category_id AND c.tenant_id=system_role.tenant_id AND c.deleted=0)")),
                java.util.Map.entry("assignedUserCount", number("(SELECT COUNT(*) FROM system_user_role ur WHERE ur.role_id=system_role.id AND ur.tenant_id=system_role.tenant_id AND ur.deleted=0)"))
        )));
    }

    default RoleDO selectByName(String name) {
        return selectOne(RoleDO::getName, name);
    }

    default RoleDO selectByCode(String code) {
        return selectOne(RoleDO::getCode, code);
    }

    default List<RoleDO> selectListByStatus(@Nullable Collection<Integer> statuses) {
        return selectList(RoleDO::getStatus, statuses);
    }

    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(RoleDO::getCategoryId, categoryId);
    }

}
