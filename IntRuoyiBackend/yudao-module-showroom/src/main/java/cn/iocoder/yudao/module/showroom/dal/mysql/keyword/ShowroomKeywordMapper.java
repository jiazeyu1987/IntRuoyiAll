package cn.iocoder.yudao.module.showroom.dal.mysql.keyword;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShowroomKeywordMapper extends BaseMapperX<ShowroomKeywordDO> {

    @Delete("DELETE FROM showroom_keyword WHERE tenant_id = #{tenantId}")
    int deleteByTenantId(@Param("tenantId") Long tenantId);

    default PageResult<ShowroomKeywordDO> selectPageByKeyword(PageParam pageParam, String keyword) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return selectPage(pageParam, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.apply(pageParam, new LambdaQueryWrapperX<ShowroomKeywordDO>()
                .eq(ShowroomKeywordDO::getTenantId, tenantId)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(ShowroomKeywordDO::getNameZh, keyword.trim())
                        .or()
                        .like(ShowroomKeywordDO::getNameEn, keyword.trim()))
                .orderByAsc(ShowroomKeywordDO::getId), java.util.Map.of(
                        "nameZh", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text("name_zh"),
                        "nameEn", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text("name_en"),
                        "updateTime", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.dateTime("update_time"))));
    }

    default List<ShowroomKeywordDO> selectListOrdered() {
        return selectList(new LambdaQueryWrapperX<ShowroomKeywordDO>()
                .eq(ShowroomKeywordDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .orderByAsc(ShowroomKeywordDO::getId));
    }

    default ShowroomKeywordDO selectByNameZh(String nameZh) {
        return selectOne(new LambdaQueryWrapperX<ShowroomKeywordDO>()
                .eq(ShowroomKeywordDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomKeywordDO::getNameZh, nameZh));
    }

}
