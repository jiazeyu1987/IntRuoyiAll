package cn.iocoder.yudao.module.dcc.dal.mysql.route;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRouteListFilterSqlTest {

    @Test
    void localPositionLabelsRemainAvailableForBothCountAndPageFilters() {
        for (String sql : List.of(DccRouteListFilterSql.count(), DccRouteListFilterSql.page())) {
            String join = sql.substring(sql.indexOf("LEFT JOIN dcc_approval_position p"),
                    sql.indexOf("LEFT JOIN user_labels u")).trim();
            assertEquals("LEFT JOIN dcc_approval_position p ON p.id=candidate.id "
                    + "AND p.tenant_id=n.tenant_id AND p.deleted=0 AND p.active=1", join,
                    "Any active local position must supply its label, regardless of source or generated code");
            assertFalse(join.contains("p.source"));
            assertFalse(join.contains("p.code"));
            assertTrue(sql.contains("NULLIF(TRIM(p.name),'')"),
                    "Local position names must remain the route candidate display source");
        }
    }
}
