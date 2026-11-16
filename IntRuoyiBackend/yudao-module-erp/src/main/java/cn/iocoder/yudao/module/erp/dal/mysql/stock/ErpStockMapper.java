package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ERP 产品库存 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface ErpStockMapper extends BaseMapperX<ErpStockDO> {

    default PageResult<ErpStockDO> selectPage(ErpStockPageReqVO reqVO) {
        return selectPage(reqVO, apply(reqVO, new LambdaQueryWrapperX<ErpStockDO>()
                .eqIfPresent(ErpStockDO::getProductId, reqVO.getProductId())
                .eqIfPresent(ErpStockDO::getWarehouseId, reqVO.getWarehouseId())
                .orderByDesc(ErpStockDO::getId), listFilterFields()));
    }

    default ErpStockDO selectByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        return selectOne(ErpStockDO::getProductId, productId,
                ErpStockDO::getWarehouseId, warehouseId);
    }

    default int updateCountIncrement(Long id, BigDecimal count, boolean negativeEnable) {
        LambdaUpdateWrapper<ErpStockDO> updateWrapper = new LambdaUpdateWrapper<ErpStockDO>()
                .eq(ErpStockDO::getId, id);
        if (count.compareTo(BigDecimal.ZERO) > 0) {
            updateWrapper.setSql("count = count + " + count);
        } else if (count.compareTo(BigDecimal.ZERO) < 0) {
            if (!negativeEnable) {
                updateWrapper.ge(ErpStockDO::getCount, count.abs());
            }
            updateWrapper.setSql("count = count - " + count.abs());
        }
        return update(null, updateWrapper);
    }

    default BigDecimal selectSumByProductId(Long productId) {
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpStockDO>()
                .select("SUM(count) AS sum_count")
                .eq("product_id", productId));
        // 获得数量
        if (CollUtil.isEmpty(result)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(MapUtil.getDouble(result.get(0), "sum_count", 0D));
    }


    static java.util.Map<String, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field> listFilterFields() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("id", number("erp_stock.id")),
                java.util.Map.entry("productId", number("erp_stock.product_id")),
                java.util.Map.entry("warehouseId", number("erp_stock.warehouse_id")),
                java.util.Map.entry("count", number("erp_stock.count")),
                java.util.Map.entry("createTime", dateTime("erp_stock.create_time")),
                java.util.Map.entry("productName", text("(SELECT p.name FROM erp_product p WHERE p.id=erp_stock.product_id AND p.deleted=0 AND p.tenant_id=erp_stock.tenant_id)")),
                java.util.Map.entry("productBarCode", text("(SELECT p.bar_code FROM erp_product p WHERE p.id=erp_stock.product_id AND p.deleted=0 AND p.tenant_id=erp_stock.tenant_id)")),
                java.util.Map.entry("unitName", text("(SELECT x.name FROM erp_product p JOIN erp_product_unit x ON x.id=p.unit_id AND x.deleted=0 AND x.tenant_id=p.tenant_id WHERE p.id=erp_stock.product_id AND p.deleted=0 AND p.tenant_id=erp_stock.tenant_id)")),
                java.util.Map.entry("categoryName", text("(SELECT x.name FROM erp_product p JOIN erp_product_category x ON x.id=p.category_id AND x.deleted=0 AND x.tenant_id=p.tenant_id WHERE p.id=erp_stock.product_id AND p.deleted=0 AND p.tenant_id=erp_stock.tenant_id)")),
                java.util.Map.entry("warehouseName", text("(SELECT w.name FROM erp_warehouse w WHERE w.id=erp_stock.warehouse_id AND w.deleted=0 AND w.tenant_id=erp_stock.tenant_id)")));
    }
}
