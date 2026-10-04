package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;

/** Uses the application's actual Jackson customization, including its shared timestamp parser. */
class DccRelationDeadlineJsonContractTest {
    ObjectMapper json;
    @BeforeEach void setup(){
        var configuration=new YudaoJacksonAutoConfiguration();
        var builder=new Jackson2ObjectMapperBuilder();
        configuration.ldtEpochMillisCustomizer().customize(builder);
        builder.modulesToInstall(configuration.timestampSupportModuleBean());
        json=builder.build();
    }
    String request(String deadline){return "{\"taskId\":\"signoff-real-round\",\"assigneeUserId\":7,\"password\":\"isolated-test\",\"reason\":\"明确整改期限\",\"relationArrangements\":[{\"relatedMasterId\":\"9223372036854775807\",\"assigneeUserId\":\"8\",\"dueAt\":"+deadline+"}]}";}
    @ParameterizedTest
    @ValueSource(strings={"\"2026-02-30 12:00:00\"","\"2026-04-31 12:00:00\"","\"2025-02-29 12:00:00\"",
            "\"2026-10-01 24:00:00\"","\"2026-10-01\"","\"2026-10-01 12:00\"","1790812800000",
            "\"2026-10-01 12:00:00.000\"","\"0000-10-01 12:00:00\"","\" 2026-10-01 12:00:00 \""})
    void invalidOrIncompleteDeadlineCannotBeNormalizedIntoASignedArrangement(String deadline){
        assertThrows(JsonProcessingException.class,()->json.readValue(request(deadline),DccSignoffAssignmentReqVO.class));
    }
    @ParameterizedTest
    @ValueSource(strings={"2024-02-29 12:00:00","2026-10-01 00:00:00","9999-12-31 23:59:59"})
    void explicitValidDeadlineAndStableIdentityRoundTripUnchanged(String deadline) throws Exception {
        var signed=json.readValue(request("\""+deadline+"\""),DccSignoffAssignmentReqVO.class);
        var arrangement=signed.getRelationArrangements().get(0);
        assertEquals(LocalDateTime.parse(deadline,DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")),arrangement.dueAt());
        assertEquals(Long.MAX_VALUE,arrangement.relatedMasterId());
        var serialized=json.readTree(json.writeValueAsString(signed)).get("relationArrangements").get(0);
        assertEquals(deadline,serialized.get("dueAt").textValue());assertEquals("9223372036854775807",serialized.get("relatedMasterId").textValue());
    }
}
