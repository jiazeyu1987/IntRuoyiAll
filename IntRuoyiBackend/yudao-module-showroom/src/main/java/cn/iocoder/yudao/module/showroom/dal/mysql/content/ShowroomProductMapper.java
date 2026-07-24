package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductDO;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShowroomProductMapper extends BaseMapperX<ShowroomProductDO> {

    String PRODUCT_LEGACY_ORDER_SQL = "ORDER BY CASE WHEN legacy_product_code IS NULL OR legacy_product_code = '' "
            + "THEN 1 ELSE 0 END, legacy_product_code ASC, id ASC";

    default ShowroomProductDO selectByProductCode(String productCode) {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductDO>()
                .eq(ShowroomProductDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomProductDO::getProductCode, productCode)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT id, product_master_id, current_revision_id, current_revision_no, product_code, legacy_product_code,
                   incomplete_flag, status, creator, create_time, updater, update_time, deleted, tenant_id
            FROM showroom_product
            WHERE tenant_id = #{tenantId}
              AND product_code = #{productCode}
            LIMIT 1
            """)
    ShowroomProductDO selectAnyByTenantIdAndProductCode(Long tenantId, String productCode);

    @Update("""
            UPDATE showroom_product
            SET deleted = 0,
                product_master_id = #{product.productMasterId},
                current_revision_id = #{product.currentRevisionId},
                current_revision_no = #{product.currentRevisionNo},
                product_code = #{product.productCode},
                legacy_product_code = #{product.legacyProductCode},
                incomplete_flag = #{product.incompleteFlag},
                status = #{product.status},
                updater = #{product.updater},
                update_time = #{product.updateTime}
            WHERE id = #{product.id}
            """)
    int reviveById(@Param("product") ShowroomProductDO product);

    default List<ShowroomProductDO> selectListOrdered() {
        return selectList(new LambdaQueryWrapperX<ShowroomProductDO>()
                .eq(ShowroomProductDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .last(PRODUCT_LEGACY_ORDER_SQL));
    }

    default PageResult<ShowroomProductDO> selectPageOrdered(PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<ShowroomProductDO>()
                .eq(ShowroomProductDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .last(PRODUCT_LEGACY_ORDER_SQL));
    }

    default ShowroomProductDO selectByLegacyProductCode(String legacyProductCode) {
        return selectOne(new LambdaQueryWrapperX<ShowroomProductDO>()
                .eq(ShowroomProductDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(ShowroomProductDO::getLegacyProductCode, legacyProductCode)
                .last("LIMIT 1"));
    }

}
