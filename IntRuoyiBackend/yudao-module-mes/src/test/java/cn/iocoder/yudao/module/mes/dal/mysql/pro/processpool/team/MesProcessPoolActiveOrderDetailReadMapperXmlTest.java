package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolActiveOrderDetailReadMapperXmlTest {

    private static final Path MAPPER_XML = Path.of("src", "main", "resources", "mapper", "pro",
            "processpool", "MesProcessPoolActiveOrderDetailReadMapper.xml");

    @Test
    void shouldReadSubmitterNameFromTeamEmployeeProfileWhenSubmissionUsesTemporaryEmployee() throws Exception {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);

        assertTrue(mapperXml.contains("mes_pro_process_pool_team_employee_profile submitter_profile"),
                "active order detail read model must join team employee profile for temporary production employees");
        assertTrue(mapperXml.contains("COALESCE(submitter.nickname, submitter_profile.display_name) AS submitterName"),
                "active order detail submitter name must use system user nickname or temporary employee display name");
        assertTrue(mapperXml.contains("submitter_profile.id = pool_event.actual_employee_id"),
                "temporary employee profile id must be matched against the production submit actual employee id");
    }
}
