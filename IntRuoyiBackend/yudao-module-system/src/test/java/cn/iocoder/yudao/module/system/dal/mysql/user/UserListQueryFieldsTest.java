package cn.iocoder.yudao.module.system.dal.mysql.user;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UserListQueryFieldsTest {
    @Test void derivedRoleSortIsOrderedAndNotSubjectToGroupConcatTruncation() {
        UserPageReqVO request = new UserPageReqVO();
        request.setSortKey("roleNamesText"); request.setSortOrder("ascending");
        var query = new LambdaQueryWrapperX<AdminUserDO>();
        UserListQueryFields.apply(request, query);
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("JSON_ARRAYAGG"));
        assertTrue(sql.contains("ORDER BY l.role_id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING"));
        assertTrue(sql.contains("l.tenant_id=system_users.tenant_id"));
        assertTrue(sql.contains("n.deleted=0"));
        assertFalse(sql.contains("GROUP_CONCAT"));
        assertTrue(sql.endsWith("system_users.id ASC"));
    }
    @Test void sortCannotInjectSql() {
        UserPageReqVO request = new UserPageReqVO();
        request.setSortKey("id; DROP TABLE system_users"); request.setSortOrder("ascending");
        assertThrows(RuntimeException.class, () -> UserListQueryFields.apply(request, new LambdaQueryWrapperX<AdminUserDO>()));
    }
    @Test void postDisplayFilteringUsesAssignedLinksAndRetainsDeletedPostLabels() {
        UserPageReqVO request = new UserPageReqVO();
        request.setSortKey("postNamesText"); request.setSortOrder("descending");
        request.setListFilters("[{\"key\":\"postNamesText\",\"operator\":\"contains\",\"value\":\"已删除岗位\"}]");
        var query = new LambdaQueryWrapperX<AdminUserDO>();
        UserListQueryFields.apply(request, query);
        assertTrue(query.getSqlSegment().contains("system_user_post"));
        assertTrue(query.getSqlSegment().contains("已删除岗位"));
        assertTrue(query.getParamNameValuePairs().containsValue("%已删除岗位%"));
    }
}
