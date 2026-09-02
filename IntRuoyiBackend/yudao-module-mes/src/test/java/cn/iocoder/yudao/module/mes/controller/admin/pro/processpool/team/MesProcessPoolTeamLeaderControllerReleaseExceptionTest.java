package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProcessPoolTeamLeaderControllerReleaseExceptionTest {

    @Test
    void activeOrderControllerMustHandleReleaseBlockersBeforeGlobalFallback() {
        Method handler = Arrays.stream(MesProcessPoolTeamLeaderController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("handleReleaseFlowBlocker"))
                .findFirst()
                .orElseThrow();

        ExceptionHandler annotation = handler.getAnnotation(ExceptionHandler.class);
        assertTrue(annotation != null, "active-order release blocker handler must be an exception handler");
        assertTrue(Arrays.asList(annotation.value()).contains(MesReleaseFlowBlockerException.class),
                "active-order release blocker handler must handle MesReleaseFlowBlockerException");

        MesReleaseFlowBlockerException exception = new MesReleaseFlowBlockerException(
                "production release application does not exist", new MesReleaseFlowFailureRespVO()
                        .setStage("SP_1")
                        .setBlockers(java.util.List.of(new cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker()
                                .setBlockerType(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE)
                                .setObjectType("RELEASE_APPLICATION")
                                .setObjectId("45")
                                .setReason("production release application does not exist"))));
        MesProcessPoolTeamLeaderController controller = new MesProcessPoolTeamLeaderController(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        CommonResult<MesReleaseFlowFailureRespVO> result = controller.handleReleaseFlowBlocker(exception);
        assertTrue(result.getCode() != 0);
        assertEquals("SP_1", result.getData().getStage());
        assertEquals(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE,
                result.getData().getBlockers().get(0).getBlockerType());
    }
}
