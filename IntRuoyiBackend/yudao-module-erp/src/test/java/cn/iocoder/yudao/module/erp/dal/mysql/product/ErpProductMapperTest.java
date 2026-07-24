package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ErpProductMapperTest extends BaseDbUnitTest {

    @Resource
    private ErpProductMapper productMapper;

    @Test
    void selectPage_filtersByBarCode() {
        productMapper.insert(buildProduct("PTCA球囊扩张导管A", "YXN.037.011.1004"));
        productMapper.insert(buildProduct("PTCA球囊扩张导管B", "YXN.037.011.1005"));
        productMapper.insert(buildProduct("清洗剂", "A001.04.01.001"));

        ErpProductPageReqVO reqVO = new ErpProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setBarCode("YXN.037.011");

        PageResult<ErpProductDO> pageResult = productMapper.selectPage(reqVO);

        assertEquals(2L, pageResult.getTotal());
        assertEquals(Set.of("YXN.037.011.1004", "YXN.037.011.1005"),
                pageResult.getList().stream().map(ErpProductDO::getBarCode).collect(toSet()));
    }

    private static ErpProductDO buildProduct(String name, String barCode) {
        ErpProductDO product = new ErpProductDO();
        product.setName(name);
        product.setBarCode(barCode);
        product.setCategoryId(1L);
        product.setUnitId(1L);
        product.setStatus(0);
        product.setStandard("规格");
        product.setRemark("备注");
        product.setExpiryDay(365);
        product.setWeight(BigDecimal.ONE);
        product.setPurchasePrice(BigDecimal.ONE);
        product.setSalePrice(BigDecimal.ONE);
        product.setMinPrice(BigDecimal.ONE);
        return product;
    }

}
