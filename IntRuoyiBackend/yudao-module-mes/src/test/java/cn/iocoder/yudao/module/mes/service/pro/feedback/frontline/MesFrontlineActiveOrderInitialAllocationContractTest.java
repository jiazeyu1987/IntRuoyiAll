package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesFrontlineActiveOrderInitialAllocationContractTest {

    @Test
    void processPoolContextMustRequirePreciseActiveOrderId() {
        Field activeOrderId = findField(MesProFrontlineProcessPoolContextReqVO.class, "activeOrderId");

        assertNotNull(activeOrderId,
                "frontline submit context must expose activeOrderId instead of inferring an order from workOrderId");
        assertEquals(Long.class, activeOrderId.getType());
        assertNotNull(activeOrderId.getAnnotation(NotNull.class),
                "activeOrderId must be validated as required on the backend request contract");
    }

    @Test
    void selectedOrderAuthorizationMustUseActiveOrderIdAsPrimaryIdentity() throws Exception {
        String source = readBackendSource(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/"
                        + "MesProFrontlineFeedbackSubmitServiceImpl.java");

        assertTrue(source.contains(
                        "authorizeActiveOrder(loginUserId, context.getActiveOrderId(), context.getWorkOrderId(),"),
                "authorization must validate the exact selected activeOrderId together with its work-order context");
    }

    @Test
    void submitMustCreateFormalInitialAllocationWithFullOutputQuantity() throws Exception {
        String source = readBackendSource(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/"
                        + "MesProFrontlineFeedbackSubmitServiceImpl.java");
        int eventCreation = source.indexOf("createSubmitEvent(eventPayload)");
        int initialAllocation = source.indexOf("createInitialAllocation(", eventCreation);
        int response = source.indexOf("return new MesProFrontlineFeedbackSubmitRespVO()", eventCreation);

        assertTrue(eventCreation >= 0, "frontline submit must create the process-pool event");
        assertTrue(initialAllocation > eventCreation && initialAllocation < response,
                "the transactional submit flow must persist initial allocation before returning success");
        String allocationCall = source.substring(initialAllocation,
                Math.min(source.length(), initialAllocation + 600));
        assertTrue(allocationCall.contains("context.getActiveOrderId()"),
                "initial allocation must target the exact active order selected by frontline production");
        assertTrue(allocationCall.contains("getOutputQuantity()"),
                "initial allocation must persist the complete submitted output quantity without a capacity cap");
    }

    @Test
    void allocationSnapshotLineMustExposeOrderOverageAndAdjustmentState() throws Exception {
        Class<?> snapshotLine = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationSnapshotLine");
        Field overageQuantity = findField(snapshotLine, "overageQuantity");
        Field needsAdjustment = findField(snapshotLine, "needsAdjustment");

        assertNotNull(overageQuantity,
                "each current allocation line must expose its order-process overage quantity");
        assertEquals(BigDecimal.class, overageQuantity.getType());
        assertNotNull(needsAdjustment,
                "each current allocation line must expose whether the production leader needs to adjust it");
        assertEquals(Boolean.class, needsAdjustment.getType());
    }

    private static Field findField(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static String readBackendSource(String relativePath) throws Exception {
        return Files.readString(resolveBackendPath(relativePath), StandardCharsets.UTF_8);
    }

    private static Path resolveBackendPath(String relativePath) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.getParent().resolve(relativePath);
        }
        return cwd.resolve(relativePath);
    }
}
