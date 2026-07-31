package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolFifoOrchestrationAllocateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolFifoOrchestrationAllocateRespVO;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesProcessPoolFifoOrchestrationControllerTest {

    @Test
    void shouldExposeFormalFifoOrchestrationWriteEndpoint() throws Exception {
        RequestMapping classMapping =
                MesProcessPoolFifoOrchestrationController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/mes/pro/process-pool/fifo-orchestration"}, classMapping.value());

        Method method = MesProcessPoolFifoOrchestrationController.class.getMethod(
                "allocateAvailableOutput", ProcessPoolFifoOrchestrationAllocateReqVO.class);
        assertEquals(CommonResult.class, method.getReturnType());

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertArrayEquals(new String[]{"/allocate-available-output"}, postMapping.value());
        assertEquals("@ss.hasPermission('mes:pro-process-pool-fifo:allocate')",
                method.getAnnotation(PreAuthorize.class).value());

        Parameter body = method.getParameters()[0];
        assertNotNull(body.getAnnotation(RequestBody.class));
        assertNotNull(body.getAnnotation(Valid.class));
        assertEquals(ProcessPoolFifoOrchestrationAllocateRespVO.class,
                method.getGenericReturnType().getTypeName().contains(
                        ProcessPoolFifoOrchestrationAllocateRespVO.class.getSimpleName())
                        ? ProcessPoolFifoOrchestrationAllocateRespVO.class
                        : null);
    }
}
