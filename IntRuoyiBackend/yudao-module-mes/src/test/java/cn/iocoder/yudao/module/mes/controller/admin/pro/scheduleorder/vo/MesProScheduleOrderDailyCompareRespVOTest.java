package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProScheduleOrderDailyCompareRespVOTest {
    @Test
    void planDateMustRemainIsoStringWithGlobalTimestampsEnabled() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MesProScheduleOrderDailyCompareRespVO response = new MesProScheduleOrderDailyCompareRespVO();
        response.setPlanDate(LocalDate.of(2026, 11, 25));
        assertEquals("2026-11-25", mapper.readTree(mapper.writeValueAsString(response)).get("planDate").textValue());
    }
}
