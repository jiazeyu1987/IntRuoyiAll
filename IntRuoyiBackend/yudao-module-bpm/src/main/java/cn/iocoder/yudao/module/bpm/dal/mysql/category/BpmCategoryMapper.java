package cn.iocoder.yudao.module.bpm.dal.mysql.category;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.category.BpmCategoryPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import java.util.Collection;
import java.util.List;

/**
 * BPM 流程分类 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface BpmCategoryMapper extends BaseMapperX<BpmCategoryDO> {

    default PageResult<BpmCategoryDO> selectPage(BpmCategoryPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<BpmCategoryDO>()
                .likeIfPresent(BpmCategoryDO::getName, reqVO.getName())
                .likeIfPresent(BpmCategoryDO::getCode, reqVO.getCode())
                .eqIfPresent(BpmCategoryDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BpmCategoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(BpmCategoryDO::getSort), Map.ofEntries(
                Map.entry("id", number("id")),
                Map.entry("name", text("name")),
                Map.entry("code", text("code")),
                Map.entry("description", text("description")),
                Map.entry("status", number("status")),
                Map.entry("sort", number("sort")),
                Map.entry("createTime", dateTime("create_time"))
        )));
    }

    default BpmCategoryDO selectByName(String name) {
        return selectOne(BpmCategoryDO::getName, name);
    }

    default BpmCategoryDO selectByCode(String code) {
        return selectOne(BpmCategoryDO::getCode, code);
    }

    default List<BpmCategoryDO> selectListByCode(Collection<String> codes) {
        return selectList(BpmCategoryDO::getCode, codes);
    }

    default List<BpmCategoryDO> selectListByStatus(Integer status) {
        return selectList(BpmCategoryDO::getStatus, status);
    }

}