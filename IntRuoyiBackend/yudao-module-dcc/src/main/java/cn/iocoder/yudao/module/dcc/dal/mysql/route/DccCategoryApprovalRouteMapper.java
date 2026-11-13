package cn.iocoder.yudao.module.dcc.dal.mysql.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * DCC category approval route mapper.
 */
@Mapper
public interface DccCategoryApprovalRouteMapper extends BaseMapperX<DccCategoryApprovalRouteDO> {

    default PageResult<DccCategoryApprovalRouteDO> selectPage(DccApprovalRoutePageReqVO reqVO) {
        var wrapper = new LambdaQueryWrapperX<DccCategoryApprovalRouteDO>()
                .eqIfPresent(DccCategoryApprovalRouteDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(DccCategoryApprovalRouteDO::getActionType, reqVO.getActionType())
                .orderByAsc(DccCategoryApprovalRouteDO::getCategoryId)
                .orderByAsc(DccCategoryApprovalRouteDO::getActionType)
                .orderByDesc(DccCategoryApprovalRouteDO::getVersionNo)
                .orderByDesc(DccCategoryApprovalRouteDO::getId);
        ListFilterQueryUtils.apply(reqVO, wrapper, DccRouteListFilterSql.fields());
        long count=countFiltered(TenantContextHolder.getRequiredTenantId(),wrapper);
        if (count==0) return PageResult.empty(0L);
        return new PageResult<>(selectFilteredPage(TenantContextHolder.getRequiredTenantId(),wrapper,
                reqVO.getPageSize(),Math.multiplyExact((long)reqVO.getPageNo()-1,reqVO.getPageSize())),count);
    }

    // The displayed stage string may exceed MySQL's 1024-byte GROUP_CONCAT default.
    // This read-only statement hint keeps the complete value for all nested aggregates.
    @SelectProvider(type=DccRouteListFilterSql.class,method="count")
    long countFiltered(@Param("tenant") Long tenant,@Param(Constants.WRAPPER) LambdaQueryWrapper<DccCategoryApprovalRouteDO> wrapper);
    @SelectProvider(type=DccRouteListFilterSql.class,method="page")
    List<DccCategoryApprovalRouteDO> selectFilteredPage(@Param("tenant") Long tenant,@Param(Constants.WRAPPER) LambdaQueryWrapper<DccCategoryApprovalRouteDO> wrapper,
            @Param("size") int size,@Param("offset") long offset);

    default DccCategoryApprovalRouteDO selectLatestActiveByCategoryId(Long categoryId) {
        return selectLatestActiveByCategoryId(categoryId, LocalDateTime.now());
    }

    default DccCategoryApprovalRouteDO selectLatestActiveByCategoryIdAndActionType(Long categoryId, String actionType) {
        return selectLatestActiveByCategoryIdAndActionType(categoryId, actionType, LocalDateTime.now());
    }

    default DccCategoryApprovalRouteDO selectLatestActiveByCategoryIdAndActionType(Long categoryId, String actionType,
                                                                                   LocalDateTime effectiveAt) {
        LocalDateTime selectionTime = effectiveAt == null ? LocalDateTime.now() : effectiveAt;
        return selectList(DccCategoryApprovalRouteDO::getCategoryId, categoryId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .filter(item -> StrUtil.equals(actionType, item.getActionType()))
                .filter(item -> isEffectiveAt(item, selectionTime))
                .max(Comparator.comparing(DccCategoryApprovalRouteDO::getVersionNo)
                        .thenComparing(DccCategoryApprovalRouteDO::getId))
                .orElse(null);
    }

    default DccCategoryApprovalRouteDO selectLatestActiveByCategoryId(Long categoryId, LocalDateTime effectiveAt) {
        LocalDateTime selectionTime = effectiveAt == null ? LocalDateTime.now() : effectiveAt;
        return selectList(DccCategoryApprovalRouteDO::getCategoryId, categoryId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .filter(item -> isLegacyActionType(item.getActionType()))
                .filter(item -> isEffectiveAt(item, selectionTime))
                .max(Comparator.comparing(DccCategoryApprovalRouteDO::getVersionNo)
                        .thenComparing(DccCategoryApprovalRouteDO::getId))
                .orElse(null);
    }

    private static boolean isLegacyActionType(String actionType) {
        return StrUtil.isBlank(actionType) || StrUtil.equals(actionType, "LEGACY");
    }

    private static boolean isEffectiveAt(DccCategoryApprovalRouteDO route, LocalDateTime effectiveAt) {
        return route.getEffectiveTime() == null || !route.getEffectiveTime().isAfter(effectiveAt);
    }

    @Select("""
            SELECT COALESCE(MAX(version_no), 0)
            FROM dcc_category_approval_route
            WHERE category_id = #{categoryId}
            """)
    Integer selectMaxVersionNoIncludingDeleted(@Param("categoryId") Long categoryId);

    @Select("""
            SELECT COALESCE(MAX(version_no), 0)
            FROM dcc_category_approval_route
            WHERE category_id = #{categoryId}
              AND action_type = #{actionType}
            """)
    Integer selectMaxVersionNoIncludingDeletedByActionType(@Param("categoryId") Long categoryId,
                                                           @Param("actionType") String actionType);
}
