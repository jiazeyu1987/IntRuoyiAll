package cn.iocoder.yudao.module.bpm.dal.mysql.definition;

import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserGroupPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmUserGroupDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import java.util.List;

/**
 * 用户组 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface BpmUserGroupMapper extends BaseMapperX<BpmUserGroupDO> {

    default PageResult<BpmUserGroupDO> selectPage(BpmUserGroupPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<BpmUserGroupDO>()
                .likeIfPresent(BpmUserGroupDO::getName, reqVO.getName())
                .eqIfPresent(BpmUserGroupDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BpmUserGroupDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BpmUserGroupDO::getId), Map.ofEntries(
                Map.entry("id", number("id")),
                Map.entry("name", text("name")),
                Map.entry("description", text("description")),
                Map.entry("status", number("status")),
                Map.entry("createTime", dateTime("create_time")),
                Map.entry("members", text("(SELECT GROUP_CONCAT(u.nickname ORDER BY u.id SEPARATOR ' ') FROM system_users u WHERE u.deleted = 0 AND u.status = 0 AND u.tenant_id = " + cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId() + " AND JSON_CONTAINS(bpm_user_group.user_ids, CAST(u.id AS JSON)))"))
        )));
    }

    default List<BpmUserGroupDO> selectListByStatus(Integer status) {
        return selectList(BpmUserGroupDO::getStatus, status);
    }

}
