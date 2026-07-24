package cn.iocoder.yudao.module.showroom.foundation;

import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldDisplaySupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowroomCompanyFieldLabelContractTest {

    @Test
    void companyFieldLabelsShouldStayAlignedWithCompanyWorkbenchContract() {
        assertEquals("发展历程", ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "development_history"));
        assertEquals("发展历程(英文)", ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "development_history_en"));
        assertEquals("园区介绍", ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "park_introduction"));
        assertEquals("园区介绍(英文)", ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "park_introduction_en"));
        assertEquals("上市信息", ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "stock_info"));
        assertEquals("上市信息(英文)", ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "stock_info_en"));
        assertEquals("核心制造能力",
                ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "core_manufacturing_capability"));
        assertEquals("核心制造能力(英文)",
                ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "core_manufacturing_capability_en"));
        assertEquals("荣誉资质", ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "honors_awards"));
        assertEquals("荣誉资质(英文)", ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "honors_awards_en"));
    }
}
