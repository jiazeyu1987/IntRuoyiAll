package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesProFrontlineFeedbackSubmitRollbackTest {

    @Test
    void submit_hasSingleTransactionRollbackBoundary() throws Exception {
        Method submit = MesProFrontlineFeedbackSubmitServiceImpl.class.getDeclaredMethod("submit",
                cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO.class);

        Transactional transactional = submit.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertArrayEquals(new Class[]{Exception.class}, transactional.rollbackFor());
    }
}
