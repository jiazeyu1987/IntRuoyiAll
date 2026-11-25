package cn.iocoder.yudao.module.mes.service.kingdee;

import cn.iocoder.yudao.module.mes.dal.mysql.kingdee.MesKingdeeSourceEvidenceMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Executes actual mapper SQL; evidence includes deleted history and is tenant scoped. */
class MesKingdeeSourceEvidenceMapperTest {
    private JdbcTemplate jdbc;
    private MesKingdeeSourceEvidenceMapper mapper;
    @BeforeEach void prepare() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        String sql = MesKingdeeSourceEvidenceMapper.class.getMethod("hasEvidence", Long.class)
                .getAnnotation(Select.class).value()[0];
        var tables = Pattern.compile("FROM ([a-z_]+)").matcher(sql);
        while (tables.find()) jdbc.execute("CREATE TABLE " + tables.group(1)
                + " (id bigint PRIMARY KEY, tenant_id bigint, deleted int DEFAULT 0)");
        var config = new MybatisConfiguration();
        config.addMapper(MesKingdeeSourceEvidenceMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(config);
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(MesKingdeeSourceEvidenceMapper.class);
    }
    @Test void unknownProductHistoryBlocksOnlyItsTenantIncludingDeletedRows() {
        assertFalse(mapper.hasEvidence(1L));
        jdbc.update("INSERT INTO mes_kingdee_production_order_sync_record VALUES (1, 2, 0)");
        assertFalse(mapper.hasEvidence(1L));
        assertTrue(mapper.hasEvidence(2L));
        jdbc.update("INSERT INTO mes_kingdee_production_order_sync_record VALUES (2, 1, 1)");
        assertTrue(mapper.hasEvidence(1L));
    }
    @Test void watermarkAndSourceMappingsRemainEvidenceAfterSoftDeletion() {
        jdbc.update("INSERT INTO mes_kingdee_bom_list VALUES (1, 2, 1)");
        assertFalse(mapper.hasEvidence(1L));
        assertTrue(mapper.hasEvidence(2L));
        jdbc.update("INSERT INTO mes_kingdee_production_material_list VALUES (2, 1, 1)");
        assertTrue(mapper.hasEvidence(1L));
    }
}
