package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MdmProductExportExcelVOTest {

    @Test
    void fromShouldFormatUpdateTimeAsYearMonthDay() {
        MdmProductDO product = MdmProductDO.builder()
                .id(1L)
                .productCode("INT-1")
                .nameCn("产品一")
                .build();
        product.setUpdateTime(LocalDateTime.of(2026, 6, 7, 22, 35, 12));

        MdmProductExportExcelVO row = MdmProductExportExcelVO.from(product);

        assertEquals("2026-06-07", row.getUpdateDate());
    }

}
