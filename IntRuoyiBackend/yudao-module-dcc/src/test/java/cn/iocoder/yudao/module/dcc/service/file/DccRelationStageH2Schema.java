package cn.iocoder.yudao.module.dcc.service.file;

import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Explicit test-only DDL projection of the real IC07 migration; no production fallback. */
final class DccRelationStageH2Schema {
    private DccRelationStageH2Schema() {}
    static void create(JdbcTemplate jdbc) throws Exception {
        String sql=Files.readString(Path.of("../sql/mysql/20261011_dcc_relation_stage_snapshots.sql"));
        var matcher=Pattern.compile("CREATE TABLE IF NOT EXISTS dcc_relation_stage_(?:snapshot|member) \\(.*?\\) ENGINE=InnoDB[^;]*;",Pattern.DOTALL).matcher(sql);
        int count=0;
        while(matcher.find()) {
            String ddl=matcher.group().replaceAll(" ENGINE=InnoDB[^;]*;",";")
                    .replaceAll(" CHARACTER SET utf8mb4 COLLATE utf8mb4_bin","")
                    .replaceAll("UNIQUE KEY (\\w+) \\(","CONSTRAINT $1 UNIQUE (")
                    .replaceAll(",\\s*KEY \\w+ \\([^)]*\\)","");
            jdbc.execute(ddl);count++;
        }
        if(count!=2)throw new IllegalStateException("IC07 fixture requires both real migration table definitions");
        addNullableColumns(jdbc,"20261011_dcc_relation_stage_application_identity.sql");
    }
    static void addNullableColumns(JdbcTemplate jdbc,String file) throws Exception {
        var sql=Files.readString(Path.of("../sql/mysql/"+file));
        var columns=Pattern.compile("ALTER TABLE (\\w+) ADD COLUMN (\\w+) ([^;]+) NULL;").matcher(sql);
        int count=0;
        while(columns.find()) {
            String type=columns.group(3).replace(" CHARACTER SET utf8mb4 COLLATE utf8mb4_bin","");
            jdbc.execute("ALTER TABLE "+columns.group(1)+" ADD COLUMN IF NOT EXISTS "+columns.group(2)+" "+type+" NULL");count++;
        }
        if(count==0)throw new IllegalStateException("IC14 actual nullable migration has no fields: "+file);
    }
    static void createRoundTable(JdbcTemplate jdbc) throws Exception {
        String sql=Files.readString(Path.of("../sql/mysql/20261001_dcc_application_round_link.sql"));
        var table=Pattern.compile("CREATE TABLE IF NOT EXISTS dcc_application_round_link \\(.*?\\) ENGINE=InnoDB[^;]*;",Pattern.DOTALL).matcher(sql);
        if(!table.find())throw new IllegalStateException("IC14 requires actual formal Round table migration");
        String ddl=table.group().replaceAll(" ENGINE=InnoDB[^;]*;",";")
                .replace(" COLLATE utf8mb4_bin","").replaceAll("UNIQUE KEY (\\w+)\\(","CONSTRAINT $1 UNIQUE(");
        jdbc.execute(ddl);
        // Exact H2 projection of the existing reserved-draft contract; no fake process id.
        jdbc.execute("ALTER TABLE dcc_application_round_link ALTER COLUMN bpm_round DROP NOT NULL");
        addNullableColumns(jdbc,"20261011_dcc_application_round_relation_source.sql");
    }
}
