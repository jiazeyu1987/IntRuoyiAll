package cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogPageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccProductCatalogMapper extends BaseMapperX<DccProductCatalogDO> {

    default PageResult<DccProductCatalogDO> selectPage(DccProductCatalogPageReqVO reqVO) {
        LambdaQueryWrapperX<DccProductCatalogDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(DccProductCatalogDO::getCategoryLevel1, reqVO.getCategoryLevel1())
                .eqIfPresent(DccProductCatalogDO::getCategoryLevel2, reqVO.getCategoryLevel2())
                .eqIfPresent(DccProductCatalogDO::getProductStatus, reqVO.getProductStatus())
                .eqIfPresent(DccProductCatalogDO::getDataSource, reqVO.getDataSource());
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        if (keyword != null) {
            wrapper.and(item -> item.like(DccProductCatalogDO::getProduct, keyword)
                    .or().like(DccProductCatalogDO::getProductCode, keyword)
                    .or().like(DccProductCatalogDO::getRegistrationCertificateName, keyword)
                    .or().like(DccProductCatalogDO::getRegistrationCertificateNumber, keyword)
                    .or().like(DccProductCatalogDO::getCertificateHolder, keyword));
        }
        wrapper.orderByAsc(DccProductCatalogDO::getDataSource)
                .orderByAsc(DccProductCatalogDO::getOriginalRowNo);
        return selectPage(reqVO, wrapper);
    }

    default DccProductCatalogDO selectByRowKey(String dataSource, Integer originalRowNo) {
        return selectOne(new LambdaQueryWrapperX<DccProductCatalogDO>()
                .eq(DccProductCatalogDO::getDataSource, dataSource)
                .eq(DccProductCatalogDO::getOriginalRowNo, originalRowNo));
    }

    default List<DccProductCatalogDO> selectAllInDisplayOrder() {
        return selectList(new LambdaQueryWrapperX<DccProductCatalogDO>()
                .orderByAsc(DccProductCatalogDO::getDataSource)
                .orderByAsc(DccProductCatalogDO::getOriginalRowNo));
    }

    @Select("""
            SELECT COALESCE(MAX(original_row_no), 1)
            FROM dcc_product_catalog
            WHERE data_source = #{dataSource}
            """)
    Integer selectMaxOriginalRowNo(@Param("dataSource") String dataSource);
}
