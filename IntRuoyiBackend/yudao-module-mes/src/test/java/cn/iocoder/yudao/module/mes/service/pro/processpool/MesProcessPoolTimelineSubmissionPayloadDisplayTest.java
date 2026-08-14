package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesProcessPoolTimelineSubmissionPayloadDisplayTest {

    @Test
    void timelineResponseMustExposeStructuredProductionPayloadForTeamLeaderReport() throws Exception {
        assertField(ProcessPoolTimelineEventRespVO.class, "outputQuantity");
        assertField(ProcessPoolTimelineEventRespVO.class, "lossQuantity");
        assertField(ProcessPoolTimelineEventRespVO.class, "lossDetails");
        assertField(ProcessPoolTimelineEventRespVO.class, "selectedDevice");
        assertField(ProcessPoolTimelineEventRespVO.class, "deviceParameterReadings");
    }

    private static Field assertField(Class<?> targetClass, String fieldName) throws Exception {
        Field field = targetClass.getDeclaredField(fieldName);
        assertNotNull(field);
        return field;
    }
}
