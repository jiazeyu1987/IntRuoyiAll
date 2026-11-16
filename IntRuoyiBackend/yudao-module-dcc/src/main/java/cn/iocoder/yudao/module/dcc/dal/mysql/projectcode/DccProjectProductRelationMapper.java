package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductRelationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DccProjectProductRelationMapper extends BaseMapperX<DccProjectProductRelationDO> {

    @Select("""
            SELECT * FROM dcc_project_product_relation
            WHERE project_code_id = #{projectCodeId} AND product_catalog_id = #{productCatalogId}
              AND deleted = 0
            FOR UPDATE
            """)
    DccProjectProductRelationDO selectByPairForUpdate(@Param("projectCodeId") Long projectCodeId,
                                                       @Param("productCatalogId") Long productCatalogId);

    default List<DccProjectProductRelationDO> selectActiveByProductCatalogIds(Collection<Long> productCatalogIds) {
        if (productCatalogIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<DccProjectProductRelationDO>()
                .in(DccProjectProductRelationDO::getProductCatalogId, productCatalogIds)
                .eq(DccProjectProductRelationDO::getRelationStatus, "ACTIVE"));
    }

    default DccProjectProductRelationDO selectByProjectCodeId(Long projectCodeId) {
        return selectOne(new LambdaQueryWrapperX<DccProjectProductRelationDO>()
                .eq(DccProjectProductRelationDO::getProjectCodeId, projectCodeId));
    }

    default DccProjectProductRelationDO selectByProductCatalogId(Long productCatalogId) {
        return selectOne(new LambdaQueryWrapperX<DccProjectProductRelationDO>()
                .eq(DccProjectProductRelationDO::getProductCatalogId, productCatalogId));
    }
}
