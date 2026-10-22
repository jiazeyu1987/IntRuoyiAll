package cn.iocoder.yudao.module.system.service.profileworkbench;

import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.WorkbenchVisibilityVersionMapper;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WorkbenchVisibilitySqlParsingTest {
    @Test
    void sharedLockQueryCanPassThroughTenantSqlParser() throws Exception {
        String sql = WorkbenchVisibilityVersionMapper.class
                .getMethod("shared", long.class, long.class)
                .getAnnotation(Select.class).value()[0]
                .replace("#{tenant}", "?").replace("#{user}", "?");
        assertDoesNotThrow(() -> CCJSqlParserUtil.parse(sql));
    }
}
