package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrNonconformanceReviewControllerPermissionTest {

    @Test
    void activeOrderListUsesCreatePermission() throws Exception {
        var method = MesProEdhrNonconformanceReviewController.class
                .getDeclaredMethod("getActiveOrderList");
        assertEquals("@ss.hasPermission('mes:pro-edhr-nonconformance-review:create')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals("/active-order-list", method.getAnnotation(GetMapping.class).value()[0]);
    }

    @Test
    void allReviewPageUsesQueryPermission() throws Exception {
        var method = MesProEdhrNonconformanceReviewController.class
                .getDeclaredMethod("getPage", MesProEdhrNonconformanceReviewPageReqVO.class);
        assertEquals("@ss.hasPermission('mes:pro-edhr-nonconformance-review:query')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals("/page", method.getAnnotation(GetMapping.class).value()[0]);
    }
}
