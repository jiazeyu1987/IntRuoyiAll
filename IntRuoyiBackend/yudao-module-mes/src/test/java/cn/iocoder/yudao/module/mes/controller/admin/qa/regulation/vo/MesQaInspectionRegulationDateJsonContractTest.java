package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesQaInspectionRegulationDateJsonContractTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void commonParseSaveContract_keepsEffectiveDateAsIsoStringWithTimestampSerializationEnabled()
            throws Exception {
        MesQaInspectionRegulationSaveReqVO request = new MesQaInspectionRegulationSaveReqVO();
        request.setEffectiveDate(LocalDate.of(2026, 8, 10));

        String json = mapper.writeValueAsString(request);

        assertTrue(mapper.readTree(json).get("effectiveDate").isTextual());
        assertEquals("2026-08-10", mapper.readTree(json).get("effectiveDate").asText());
        assertEquals(request.getEffectiveDate(),
                mapper.readValue(json, MesQaInspectionRegulationSaveReqVO.class).getEffectiveDate());
    }

    @Test
    void publishedVersionContract_keepsEffectiveDateAsIsoStringWithTimestampSerializationEnabled()
            throws Exception {
        MesQaInspectionRegulationPublishedVersionRespVO response =
                MesQaInspectionRegulationPublishedVersionRespVO.builder()
                        .effectiveDate(LocalDate.of(2026, 8, 10))
                        .build();

        String json = mapper.writeValueAsString(response);

        assertTrue(mapper.readTree(json).get("effectiveDate").isTextual());
        assertEquals("2026-08-10", mapper.readTree(json).get("effectiveDate").asText());
    }
}
