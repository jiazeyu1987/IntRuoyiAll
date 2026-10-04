package cn.iocoder.yudao.module.dcc.service.file;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.util.Map;
@Component
public class DccLegacyRegistrationSourceEnvironment {
    private final JdbcTemplate jdbc;
    public DccLegacyRegistrationSourceEnvironment(DataSource source){jdbc=new JdbcTemplate(source);}
    public Map<String,Object> identity(){var rows=jdbc.queryForList("SELECT DATABASE() AS database_name,@@server_uuid AS server_uuid");if(rows.size()!=1)throw new IllegalStateException("actual source environment missing");return rows.get(0);}
}
