package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import org.apache.ibatis.annotations.Mapper;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import java.util.Collection;
import java.util.List;

/**
 * ERP 产品 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface ErpProductMapper extends BaseMapperX<ErpProductDO> {

    default int updateErpMaterialSnapshot(ErpProductDO row) {
        return update(new ErpProductDO(), new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpProductDO>()
                .eq(ErpProductDO::getId, row.getId())
                .set(ErpProductDO::getName, row.getName())
                .set(ErpProductDO::getCategoryId, row.getCategoryId())
                .set(ErpProductDO::getUnitId, row.getUnitId())
                .set(ErpProductDO::getStatus, row.getStatus())
                .set(ErpProductDO::getStandard, row.getStandard()));
    }


    default PageResult<ErpProductDO> selectPage(ErpProductPageReqVO reqVO) {
        return selectPage(reqVO, apply(reqVO, new LambdaQueryWrapperX<ErpProductDO>()
                .eqIfPresent(ErpProductDO::getBarCode, reqVO.getSyncCode())
                .eqIfPresent(ErpProductDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpProductDO::getName, reqVO.getName())
                .likeIfPresent(ErpProductDO::getBarCode, reqVO.getBarCode())
                .eqIfPresent(ErpProductDO::getCategoryId, reqVO.getCategoryId())
                .betweenIfPresent(ErpProductDO::getCreateTime, reqVO.getCreateTime())
                .and(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getKeyword()), q -> q
                        .like(ErpProductDO::getName, reqVO.getKeyword())
                        .or().like(ErpProductDO::getBarCode, reqVO.getKeyword()))
                .orderByDesc(ErpProductDO::getId), listFilterFields()));
    }

    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(ErpProductDO::getCategoryId, categoryId);
    }

    default Long selectCountByUnitId(Long unitId) {
        return selectCount(ErpProductDO::getUnitId, unitId);
    }

    default List<ErpProductDO> selectListByStatus(Integer status) {
        return selectList(ErpProductDO::getStatus, status);
    }

    default ErpProductDO selectByBarCode(String barCode) {
        return selectOne(ErpProductDO::getBarCode, barCode);
    }

    default List<ErpProductDO> selectListByBarCodes(Collection<String> barCodes) {
        return selectList(ErpProductDO::getBarCode, barCodes);
    }


    static java.util.Map<String, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field> listFilterFields() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("id", number("erp_product.id")),
                java.util.Map.entry("name", text("erp_product.name")),
                java.util.Map.entry("barCode", text("erp_product.bar_code")),
                java.util.Map.entry("categoryId", number("erp_product.category_id")),
                java.util.Map.entry("unitId", number("erp_product.unit_id")),
                java.util.Map.entry("status", number("erp_product.status")),
                java.util.Map.entry("standard", text("erp_product.standard")),
                java.util.Map.entry("remark", text("erp_product.remark")),
                java.util.Map.entry("expiryDay", number("erp_product.expiry_day")),
                java.util.Map.entry("weight", number("erp_product.weight")),
                java.util.Map.entry("purchasePrice", number("erp_product.purchase_price")),
                java.util.Map.entry("salePrice", number("erp_product.sale_price")),
                java.util.Map.entry("minPrice", number("erp_product.min_price")),
                java.util.Map.entry("createTime", dateTime("erp_product.create_time")),
                java.util.Map.entry("categoryName", text("(SELECT c.name FROM erp_product_category c WHERE c.id=erp_product.category_id AND c.deleted=0 AND c.tenant_id=erp_product.tenant_id)")),
                java.util.Map.entry("unitName", text("(SELECT u.name FROM erp_product_unit u WHERE u.id=erp_product.unit_id AND u.deleted=0 AND u.tenant_id=erp_product.tenant_id)")));
    }
}
