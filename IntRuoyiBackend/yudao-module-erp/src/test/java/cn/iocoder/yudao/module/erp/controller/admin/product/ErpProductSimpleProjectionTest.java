package cn.iocoder.yudao.module.erp.controller.admin.product;

import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ErpProductSimpleProjectionTest {
    @Test
    void remoteOptionsKeepSelectionFactsWithoutExposingFullProductDetails() throws Exception {
        var product=new ErpProductRespVO().setId(903L).setName("物料").setBarCode("P903")
                .setCategoryId(8L).setCategoryName("分类").setUnitId(2L).setUnitName("盒")
                .setPurchasePrice(BigDecimal.TEN).setSalePrice(new BigDecimal("12"))
                .setMinPrice(BigDecimal.ONE).setRemark("内部备注").setCreateTime(LocalDateTime.now());
        var method=ErpProductController.class.getDeclaredMethod("simpleProduct",ErpProductRespVO.class);
        method.setAccessible(true);
        var option=(ErpProductRespVO)method.invoke(null,product);
        assertEquals(903L,option.getId());assertEquals("盒",option.getUnitName());
        assertEquals(new BigDecimal("12"),option.getSalePrice());
        assertEquals(BigDecimal.TEN,option.getPurchasePrice());
        assertEquals(BigDecimal.ONE,option.getMinPrice());assertEquals(8L,option.getCategoryId());
        assertNull(option.getRemark());assertNull(option.getCreateTime());
    }
}
