package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.logger.OperateLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Import({DeptServiceImpl.class, DeptSupervisorService.class, TransactionAutoConfiguration.class})
class DeptSupervisorTransactionTest extends BaseDbUnitTest {
    @Resource private DeptServiceImpl service;
    @Resource private DeptMapper deptMapper;
    @Resource private AdminUserMapper userMapper;
    @Resource private OperateLogMapper logMapper;
    @MockitoBean private ApprovalAuthorityApi authority;

    @BeforeEach void setup() {
        when(authority.lockTenantApprovalAuthority(anyLong())).thenAnswer(i ->
                new ApprovalAuthorityRevisionDTO(i.getArgument(0), 1L));
    }

    private DeptDO dept(String name, Long leader) {
        DeptDO dept = new DeptDO().setName(name).setParentId(0L).setSort(1).setStatus(0).setLeaderUserId(leader);
        deptMapper.insert(dept);
        return dept;
    }

    @Test void explicitNullClearsSupervisorButOmissionPreservesIt() throws Exception {
        var dept = dept("清空主管", 91L);
        var mapper = new ObjectMapper();
        var omitted = mapper.readValue("{\"name\":\"改名称\",\"parentId\":0,\"sort\":1,\"status\":0}", DeptSaveReqVO.class)
                .setId(dept.getId());
        service.updateDept(omitted);
        assertEquals(91L, deptMapper.selectById(dept.getId()).getLeaderUserId());
        var clear = mapper.readValue("{\"name\":\"改名称\",\"parentId\":0,\"sort\":1,\"status\":0,\"leaderUserId\":null}",
                DeptSaveReqVO.class).setId(dept.getId());
        service.updateDept(clear);
        assertNull(deptMapper.selectById(dept.getId()).getLeaderUserId());
        var audit = logMapper.selectList().get(0);
        assertTrue(audit.getAction().contains("91"));
        assertNotNull(audit.getCreateTime());
        assertNotNull(audit.getUserId());
    }

    @Test void batchWithDisabledEmployeeRejectsAllDepartments() {
        var empty = dept("空部门", null);
        var occupied = dept("有停用员工", null);
        userMapper.insert(new AdminUserDO().setUsername("disabled-member").setCanonicalUsername("disabled-member")
                .setNickname("已停用员工").setStatus(1).setDeptId(occupied.getId()));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.deleteDeptList(List.of(empty.getId(), occupied.getId())));
        assertTrue(ex.getMessage().contains(occupied.getName()));
        assertNotNull(deptMapper.selectById(empty.getId()));
        assertNotNull(deptMapper.selectById(occupied.getId()));
    }

    @Test void batchWithChildDepartmentRejectsWholeBatch() {
        var empty = dept("空部门", null);
        var parent = dept("有下级", null);
        var child = dept("下级", null);
        deptMapper.updateById(new DeptDO().setId(child.getId()).setParentId(parent.getId()));
        assertThrows(ServiceException.class, () -> service.deleteDeptList(List.of(empty.getId(), parent.getId())));
        assertNotNull(deptMapper.selectById(empty.getId()));
        assertNotNull(deptMapper.selectById(parent.getId()));
    }

    @Test void emptyLeafDepartmentsCanBeDeletedTogether() {
        var a = dept("叶子 A", null);
        var b = dept("叶子 B", null);
        service.deleteDeptList(List.of(a.getId(), b.getId()));
        assertNull(deptMapper.selectById(a.getId()));
        assertNull(deptMapper.selectById(b.getId()));
    }
}
