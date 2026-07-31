package cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
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
    String COLUMN_CATEGORY_LEVEL1 = "category_level1";
    String COLUMN_CATEGORY_LEVEL2 = "category_level2";
    String COLUMN_PRODUCT_STATUS = "product_status";
    String COLUMN_DATA_SOURCE = "data_source";
    String COLUMN_PRODUCT = "product";
    String COLUMN_PRODUCT_CODE = "product_code";
    String COLUMN_REGISTRATION_CERTIFICATE_NAME = "registration_certificate_name";
    String COLUMN_REGISTRATION_CERTIFICATE_NUMBER = "registration_certificate_number";
    String COLUMN_CERTIFICATE_HOLDER = "certificate_holder";
    String COLUMN_ORIGINAL_ROW_NO = "original_row_no";
    String PROJECT_NAME_COLUMN = "project_name";
    String PROJECT_CODE_COLUMN = "project_code";
    String PROJECT_SORT_BLANK_LAST_EXPRESSION =
            "CASE WHEN %s IS NULL OR TRIM(%s) = '' THEN 1 ELSE 0 END";

    default PageResult<DccProductCatalogDO> selectPage(DccProductCatalogPageReqVO reqVO) {
        QueryWrapperX<DccProductCatalogDO> wrapper = new QueryWrapperX<>();
        wrapper.eqIfPresent(COLUMN_CATEGORY_LEVEL1, reqVO.getCategoryLevel1())
                .eqIfPresent(COLUMN_CATEGORY_LEVEL2, reqVO.getCategoryLevel2())
                .eqIfPresent(COLUMN_PRODUCT_STATUS, reqVO.getProductStatus())
                .eqIfPresent(COLUMN_DATA_SOURCE, reqVO.getDataSource());
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        if (keyword != null) {
            wrapper.and(item -> item.like(COLUMN_PRODUCT, keyword)
                    .or().like(COLUMN_PRODUCT_CODE, keyword)
                    .or().like(COLUMN_REGISTRATION_CERTIFICATE_NAME, keyword)
                    .or().like(COLUMN_REGISTRATION_CERTIFICATE_NUMBER, keyword)
                    .or().like(COLUMN_CERTIFICATE_HOLDER, keyword));
        }
        applyPageSort(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    private void applyPageSort(QueryWrapperX<DccProductCatalogDO> wrapper,
            DccProductCatalogPageReqVO reqVO) {
        String sortField = StrUtil.trimToEmpty(reqVO.getSortField());
        String sortOrder = StrUtil.trimToEmpty(reqVO.getSortOrder());
        boolean hasProjectSort = SORT_ORDER_ASC.equalsIgnoreCase(sortOrder)
                || SORT_ORDER_DESC.equalsIgnoreCase(sortOrder);
        if (hasProjectSort) {
            if (PROJECT_SORT_FIELD_NAME.equals(sortField)) {
                applyProjectFieldSort(wrapper, sortOrder, PROJECT_NAME_COLUMN);
            } else if (PROJECT_SORT_FIELD_CODE.equals(sortField)) {
                applyProjectFieldSort(wrapper, sortOrder, PROJECT_CODE_COLUMN);
            }
        }
        wrapper.orderByAsc(COLUMN_DATA_SOURCE)
                .orderByAsc(COLUMN_ORIGINAL_ROW_NO);
    }

    private void applyProjectFieldSort(QueryWrapperX<DccProductCatalogDO> wrapper, String sortOrder,
            String column) {
        wrapper.orderByAsc(blankLastOrderExpression(column));
        if (SORT_ORDER_ASC.equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column);
        } else {
            wrapper.orderByDesc(column);
        }
    }

    private String blankLastOrderExpression(String column) {
        return String.format(PROJECT_SORT_BLANK_LAST_EXPRESSION, column, column);
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
