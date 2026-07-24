package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCaseCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRemediateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRetestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqStepSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOqPqService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrOqPqContractTest {

    @Test
    void oqPqControllerMappings_matchExecutionAndDeviationContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrOqPqController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-oq-pq"}, requestMapping.value());

        assertGet("getCasePage", "mes:pro-edhr-oq-pq:query", "/case/page", MesProEdhrOqPqCasePageReqVO.class);
        assertPost("createCase", "mes:pro-edhr-oq-pq:create", "/case/create", MesProEdhrOqPqCaseCreateReqVO.class);
        assertGet("getRunPage", "mes:pro-edhr-oq-pq:query", "/run/page", MesProEdhrOqPqRunPageReqVO.class);
        assertPost("createRun", "mes:pro-edhr-oq-pq:create", "/run/create", MesProEdhrOqPqRunCreateReqVO.class);
        assertPost("submitStepResult", "mes:pro-edhr-oq-pq:execute", "/run/submit-step", MesProEdhrOqPqStepSubmitReqVO.class);
        assertPost("completeRun", "mes:pro-edhr-oq-pq:execute", "/run/complete", Long.class);
        assertGet("getDeviationPage", "mes:pro-edhr-oq-pq:query", "/deviation/page", MesProEdhrOqPqDeviationPageReqVO.class);
        assertPost("remediateDeviation", "mes:pro-edhr-oq-pq:retest", "/deviation/remediate", MesProEdhrOqPqDeviationRemediateReqVO.class);
        assertPost("retestDeviation", "mes:pro-edhr-oq-pq:retest", "/deviation/retest", MesProEdhrOqPqDeviationRetestReqVO.class);
        assertPost("closeDeviation", "mes:pro-edhr-oq-pq:close", "/deviation/close", MesProEdhrOqPqDeviationCloseReqVO.class);
    }

    @Test
    void serviceContract_declaresOqPqExecutionAndDeviationMethods() throws Exception {
        MesProEdhrOqPqService.class.getDeclaredMethod("getCasePage", MesProEdhrOqPqCasePageReqVO.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("createCase", MesProEdhrOqPqCaseCreateReqVO.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("getRunPage", MesProEdhrOqPqRunPageReqVO.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("createRun", MesProEdhrOqPqRunCreateReqVO.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("submitStepResult", MesProEdhrOqPqStepSubmitReqVO.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("completeRun", Long.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("getDeviationPage", MesProEdhrOqPqDeviationPageReqVO.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("remediateDeviation", MesProEdhrOqPqDeviationRemediateReqVO.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("retestDeviation", MesProEdhrOqPqDeviationRetestReqVO.class);
        MesProEdhrOqPqService.class.getDeclaredMethod("closeDeviation", MesProEdhrOqPqDeviationCloseReqVO.class);
    }

    private void assertGet(String methodName, String permission, String path, Class<?> parameterType) throws Exception {
        Method method = MesProEdhrOqPqController.class.getDeclaredMethod(methodName, parameterType);
        assertArrayEquals(new String[]{path}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')", method.getAnnotation(PreAuthorize.class).value());
    }

    private void assertPost(String methodName, String permission, String path, Class<?> parameterType) throws Exception {
        Method method = MesProEdhrOqPqController.class.getDeclaredMethod(methodName, parameterType);
        assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')", method.getAnnotation(PreAuthorize.class).value());
    }
}
