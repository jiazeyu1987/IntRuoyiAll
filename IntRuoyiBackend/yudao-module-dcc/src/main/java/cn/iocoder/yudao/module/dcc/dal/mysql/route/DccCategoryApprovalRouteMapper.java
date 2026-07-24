package cn.iocoder.yudao.module.dcc.dal.mysql.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Comparator;

/**
 * DCC category approval route mapper.
 */
@Mapper
public interface DccCategoryApprovalRouteMapper extends BaseMapperX<DccCategoryApprovalRouteDO> {

    default PageResult<DccCategoryApprovalRouteDO> selectPage(DccApprovalRoutePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DccCategoryApprovalRouteDO>()
                .eqIfPresent(DccCategoryApprovalRouteDO::getCategoryId, reqVO.getCategoryId())
                .orderByAsc(DccCategoryApprovalRouteDO::getCategoryId)
                .orderByDesc(DccCategoryApprovalRouteDO::getVersionNo)
                .orderByDesc(DccCategoryApprovalRouteDO::getId));
    }

    default DccCategoryApprovalRouteDO selectLatestActiveByCategoryId(Long categoryId) {
        return selectList(DccCategoryApprovalRouteDO::getCategoryId, categoryId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .max(Comparator.comparing(DccCategoryApprovalRouteDO::getVersionNo))
                .orElse(null);
    }

    @Select("""
            SELECT COALESCE(MAX(version_no), 0)
            FROM dcc_category_approval_route
            WHERE category_id = #{categoryId}
            """)
    Integer selectMaxVersionNoIncludingDeleted(@Param("categoryId") Long categoryId);
}
