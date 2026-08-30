package cn.iocoder.yudao.module.mdm.service.companyscope;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopePageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopeRespVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmRoleCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmUserCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmRoleCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmUserCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmCompanyScopeQueryServiceTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private MdmUserCompanyScopeMapper userScopeMapper;
    @Mock
    private MdmRoleCompanyScopeMapper roleScopeMapper;
    @Mock
    private MdmEnterpriseService enterpriseService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private MdmCompanyScopeRecipientResolver recipientResolver;

    private MdmCompanyScopeServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        service = new MdmCompanyScopeServiceImpl();
        ReflectionTestUtils.setField(service, "userScopeMapper", userScopeMapper);
        ReflectionTestUtils.setField(service, "roleScopeMapper", roleScopeMapper);
        ReflectionTestUtils.setField(service, "enterpriseService", enterpriseService);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "roleApi", roleApi);
        ReflectionTestUtils.setField(service, "recipientResolver", recipientResolver);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getCompanyScopePageReturnsSeparateUserAndRoleMappingsWithFormalFacts() {
        when(userScopeMapper.selectList(any())).thenReturn(List.of(userScope(11L, 101L, 301L)));
        when(roleScopeMapper.selectList(any())).thenReturn(List.of(roleScope(12L, 201L, 302L)));
        when(adminUserApi.getUserList(List.of(101L))).thenReturn(List.of(user(101L)));
        when(roleApi.getRoleList(List.of(201L))).thenReturn(List.of(role(201L)));
        when(enterpriseService.getEnabledEnterprises(List.of(301L, 302L), Set.of("OWNED_COMPANY")))
                .thenReturn(List.of(company(301L, "INT-001", "甲公司"), company(302L, "INT-002", "乙公司")));

        MdmCompanyScopePageReqVO req = new MdmCompanyScopePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        var result = service.getCompanyScopePage(req);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of("USER", "ROLE"),
                result.getList().stream().map(MdmCompanyScopeRespVO::getScopeType).toList());
        assertEquals("用户一", result.getList().get(0).getPrincipalName());
        assertEquals("角色一", result.getList().get(1).getPrincipalName());
        assertEquals(List.of("甲公司", "乙公司"),
                result.getList().stream().map(MdmCompanyScopeRespVO::getCompanyName).toList());
    }

    private MdmUserCompanyScopeDO userScope(Long id, Long userId, Long companyId) {
        MdmUserCompanyScopeDO row = MdmUserCompanyScopeDO.builder()
                .id(id)
                .userId(userId)
                .companyId(companyId)
                .status("ENABLE")
                .revision(1)
                .build();
        row.setTenantId(TENANT_ID);
        row.setDeleted(false);
        row.setUpdateTime(LocalDateTime.of(2026, 8, 29, 12, 0));
        return row;
    }

    private MdmRoleCompanyScopeDO roleScope(Long id, Long roleId, Long companyId) {
        MdmRoleCompanyScopeDO row = MdmRoleCompanyScopeDO.builder()
                .id(id)
                .roleId(roleId)
                .companyId(companyId)
                .status("ENABLE")
                .revision(1)
                .build();
        row.setTenantId(TENANT_ID);
        row.setDeleted(false);
        row.setUpdateTime(LocalDateTime.of(2026, 8, 28, 12, 0));
        return row;
    }

    private AdminUserRespDTO user(Long id) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setNickname("用户一");
        user.setStatus(0);
        return user;
    }

    private RoleRespDTO role(Long id) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(id);
        role.setCode("role-" + id);
        role.setName("角色一");
        role.setStatus(0);
        return role;
    }

    private MdmEnterpriseDO company(Long id, String code, String name) {
        MdmEnterpriseDO company = MdmEnterpriseDO.builder()
                .id(id)
                .enterpriseCode(code)
                .name(name)
                .type("OWNED_COMPANY")
                .status("ENABLE")
                .revision(1)
                .build();
        company.setTenantId(TENANT_ID);
        company.setDeleted(false);
        return company;
    }
}
