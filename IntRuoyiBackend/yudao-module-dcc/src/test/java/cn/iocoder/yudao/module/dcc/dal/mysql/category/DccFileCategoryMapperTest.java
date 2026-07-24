package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccFileCategoryMapperTest extends BaseDbUnitTest {

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DataSource dataSource;

    @Test
    void testInsertAndSelectById() {
        DccFileCategoryDO category = new DccFileCategoryDO();
        category.setId(randomLongId());
        category.setCode("SOP");
        category.setName("Work Instruction");
        category.setParentId(null);
        category.setActive(Boolean.TRUE);
        category.setSort(1);
        category.setSource("LOCAL");
        category.setRemark("seed");
        setField(category, "description", "Controlled work instruction");
        setField(category, "lifecycleStage", "PLAN");
        setField(category, "distributionRequired", Boolean.TRUE);
        setField(category, "trainingRequired", Boolean.TRUE);

        categoryMapper.insert(category);

        DccFileCategoryDO dbCategory = categoryMapper.selectById(category.getId());
        assertNotNull(dbCategory);
        assertPojoEquals(category, dbCategory, "createTime", "updateTime", "creator", "updater", "deleted");
        assertTrue("PLAN".equals(readField(dbCategory, "lifecycleStage")));
        assertTrue(Boolean.TRUE.equals(readField(dbCategory, "distributionRequired")));
        assertTrue(Boolean.TRUE.equals(readField(dbCategory, "trainingRequired")));
    }

    @Test
    void testSelectById_notExists() {
        assertNull(categoryMapper.selectById(randomLongId()));
    }

    @Test
    void testCategoryRuleTablesAcceptInserts() {
        DccFileCategoryDO category = new DccFileCategoryDO();
        category.setId(randomLongId());
        category.setCode("FORM");
        category.setName("Form");
        category.setParentId(null);
        category.setActive(Boolean.TRUE);
        category.setSort(2);
        category.setSource("LOCAL");
        category.setRemark("rules");
        setField(category, "description", "Controlled form");
        setField(category, "lifecycleStage", "OUTPUT");
        setField(category, "distributionRequired", Boolean.TRUE);
        setField(category, "trainingRequired", Boolean.TRUE);
        categoryMapper.insert(category);

        executeUpdate("""
                INSERT INTO dcc_file_category_permission_rule
                (id, category_id, action_type, subject_type, subject_id, active, remark,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 910L, category.getId(), "VIEW", "USER", 200L, true, "viewer", 0L, "1", "1", 0);
        executeUpdate("""
                INSERT INTO dcc_file_category_distribution_rule
                (id, category_id, department_id, active,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 920L, category.getId(), 300L, true, 0L, "1", "1", 0);
        executeUpdate("""
                INSERT INTO dcc_file_category_training_rule
                (id, category_id, department_id, active,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 930L, category.getId(), 301L, true, 0L, "1", "1", 0);

        assertEqualsOne("dcc_file_category_permission_rule", 910L);
        assertEqualsOne("dcc_file_category_distribution_rule", 920L);
        assertEqualsOne("dcc_file_category_training_rule", 930L);
    }

    private void assertEqualsOne(String tableName, Long id) {
        Integer count = queryInt("SELECT COUNT(1) FROM " + tableName + " WHERE id = ?", id);
        assertTrue(count != null && count == 1);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Missing field " + target.getClass().getSimpleName() + "." + fieldName, ex);
        }
    }

    private void executeUpdate(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, params);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed SQL update", ex);
        }
    }

    private Integer queryInt(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return ((Number) resultSet.getObject(1)).intValue();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed SQL query", ex);
        }
    }

    private void bindParameters(PreparedStatement statement, Object... params) throws Exception {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    private static Object readField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Missing field " + target.getClass().getSimpleName() + "." + fieldName, ex);
        }
    }
}
