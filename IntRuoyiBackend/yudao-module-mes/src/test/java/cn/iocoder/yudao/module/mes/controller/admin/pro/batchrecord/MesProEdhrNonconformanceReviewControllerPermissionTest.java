package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrNonconformanceReviewControllerPermissionTest {

    @Test
    void assigningPreviouslyUnassignedReviewRequiresCreatePermission() throws Exception {
        var method = MesProEdhrNonconformanceReviewController.class.getDeclaredMethod("assignReviewer",
                cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewerAssignReqVO.class);
        assertEquals("@ss.hasPermission('mes:pro-edhr-nonconformance-review:create')", method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void activeOrderListUsesCreatePermission() throws Exception {
        var method = MesProEdhrNonconformanceReviewController.class
                .getDeclaredMethod("getActiveOrderList");
        assertEquals("@ss.hasPermission('mes:pro-edhr-nonconformance-review:create')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals("/active-order-list", method.getAnnotation(GetMapping.class).value()[0]);
    }

    @Test
    void reviewReadsRequireLoginWithoutRolePermissions() {
        var readMethods = java.util.Set.of("getPage", "getPendingPage", "get", "getSummary", "getActiveOrderDetail", "getBatchList");
        var methods = java.util.Arrays.stream(MesProEdhrNonconformanceReviewController.class.getDeclaredMethods())
                .filter(method -> readMethods.contains(method.getName())).toList();
        assertEquals(6, methods.size());
        for (var method : methods) {
            assertEquals("isAuthenticated()", method.getAnnotation(PreAuthorize.class).value(), method.getName());
        }
    }

    @Test
    void creationAndDispositionStillRequireTheirOwnPermissions() throws Exception {
        var controller = MesProEdhrNonconformanceReviewController.class;
        assertEquals("@ss.hasPermission('mes:pro-edhr-nonconformance-review:create')",
                controller.getDeclaredMethod("create", cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-nonconformance-review:dispose')",
                controller.getDeclaredMethod("dispose", cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
    }
}
