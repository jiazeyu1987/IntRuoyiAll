package cn.iocoder.yudao.module.system.dal.mysql.dept;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface PostMapper extends BaseMapperX<PostDO> {

    /**
     * 在全局岗位写锁内读取最新编码，包含逻辑删除记录，避免并发重号及删除后复用编号。
     */
    @Select("SELECT code FROM system_post WHERE UPPER(code) LIKE 'GW-%' ORDER BY id FOR UPDATE")
    List<String> selectGeneratedCodesForUpdate();

    default List<PostDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<PostDO>()
                .in(PostDO::getId, ids).orderByAsc(PostDO::getId).last("FOR UPDATE"));
    }

    default List<PostDO> selectList(Collection<Long> ids, Collection<Integer> statuses) {
        return selectList(new LambdaQueryWrapperX<PostDO>()
                .inIfPresent(PostDO::getId, ids)
                .inIfPresent(PostDO::getStatus, statuses));
    }

    default PageResult<PostDO> selectPage(PostPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<PostDO>()
                .likeIfPresent(PostDO::getCode, reqVO.getCode())
                .likeIfPresent(PostDO::getName, reqVO.getName())
                .eqIfPresent(PostDO::getStatus, reqVO.getStatus())
                .orderByDesc(PostDO::getId), java.util.Map.ofEntries(
                java.util.Map.entry("id", number("id")),
                java.util.Map.entry("name", text("name")),
                java.util.Map.entry("code", text("code")),
                java.util.Map.entry("sort", number("sort")),
                java.util.Map.entry("status", number("status")),
                java.util.Map.entry("remark", text("remark")),
                java.util.Map.entry("createTime", dateTime("create_time"))
        )));
    }

    default PostDO selectByName(String name) {
        return selectOne(PostDO::getName, name);
    }

    default PostDO selectByCode(String code) {
        return selectOne(PostDO::getCode, code);
    }

}
