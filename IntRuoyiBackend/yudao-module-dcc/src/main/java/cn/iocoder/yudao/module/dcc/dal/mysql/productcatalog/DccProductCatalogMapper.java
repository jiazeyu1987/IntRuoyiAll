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

    String PROJECT_SORT_FIELD_NAME = "projectName";
    String PROJECT_SORT_FIELD_CODE = "projectCode";
    String SORT_ORDER_ASC = "asc";
    String SORT_ORDER_DESC = "desc";

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
        applyPageSort(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    private void applyPageSort(LambdaQueryWrapperX<DccProductCatalogDO> wrapper,
            DccProductCatalogPageReqVO reqVO) {
        String sortField = StrUtil.trimToEmpty(reqVO.getSortField());
        String sortOrder = StrUtil.trimToEmpty(reqVO.getSortOrder());
        boolean hasProjectSort = SORT_ORDER_ASC.equalsIgnoreCase(sortOrder)
                || SORT_ORDER_DESC.equalsIgnoreCase(sortOrder);
        if (hasProjectSort) {
            if (PROJECT_SORT_FIELD_NAME.equals(sortField)) {
                applyProjectFieldSort(wrapper, sortOrder, DccProductCatalogDO::getProjectName);
            } else if (PROJECT_SORT_FIELD_CODE.equals(sortField)) {
                applyProjectFieldSort(wrapper, sortOrder, DccProductCatalogDO::getProjectCode);
            }
        }
        wrapper.orderByAsc(DccProductCatalogDO::getDataSource)
                .orderByAsc(DccProductCatalogDO::getOriginalRowNo);
    }

    private void applyProjectFieldSort(LambdaQueryWrapperX<DccProductCatalogDO> wrapper, String sortOrder,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<DccProductCatalogDO, ?> column) {
        if (SORT_ORDER_ASC.equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column);
        } else {
            wrapper.orderByDesc(column);
        }
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
