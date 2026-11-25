package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSourceEvidenceMapper;
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
class ErpKingdeeSourceEvidenceMapperTest {
    private JdbcTemplate jdbc;
    private ErpKingdeeSourceEvidenceMapper mapper;
    @BeforeEach void prepare() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        String sql = ErpKingdeeSourceEvidenceMapper.class.getMethod("hasEvidence", Long.class)
                .getAnnotation(Select.class).value()[0];
        var tables = Pattern.compile("FROM ([a-z_]+)").matcher(sql);
        while (tables.find()) jdbc.execute("CREATE TABLE " + tables.group(1)
                + " (id bigint PRIMARY KEY, tenant_id bigint, deleted int DEFAULT 0)");
        var config = new MybatisConfiguration();
        config.addMapper(ErpKingdeeSourceEvidenceMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(config);
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(ErpKingdeeSourceEvidenceMapper.class);
    }
    @Test void unknownProductHistoryBlocksOnlyItsTenantIncludingDeletedRows() {
        assertFalse(mapper.hasEvidence(1L));
        jdbc.update("INSERT INTO erp_product VALUES (1, 2, 0)");
        assertFalse(mapper.hasEvidence(1L));
        assertTrue(mapper.hasEvidence(2L));
        jdbc.update("INSERT INTO erp_product VALUES (2, 1, 1)");
        assertTrue(mapper.hasEvidence(1L));
    }
    @Test void watermarkAndSourceMappingsRemainEvidenceAfterSoftDeletion() {
        jdbc.update("INSERT INTO erp_kingdee_sync_watermark VALUES (1, 2, 1)");
        assertFalse(mapper.hasEvidence(1L));
        assertTrue(mapper.hasEvidence(2L));
        jdbc.update("INSERT INTO erp_kingdee_purchase_order_sync_record VALUES (2, 1, 1)");
        assertTrue(mapper.hasEvidence(1L));
    }
}
