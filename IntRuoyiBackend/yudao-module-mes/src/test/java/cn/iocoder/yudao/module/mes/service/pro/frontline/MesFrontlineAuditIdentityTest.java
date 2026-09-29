package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesFrontlineAuditIdentityTest {
    private final MesProcessPoolTeamEmployeeProfileMapper profiles = mock(MesProcessPoolTeamEmployeeProfileMapper.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final MesFrontlineAuditIdentity identity = new MesFrontlineAuditIdentity(profiles, users);

    @Test
    void independentEmployeeUsesProfileDomainAndOnlyWhitelistedPublicFields() {
        var profile = profile().setSystemUserId(null).setSignaturePasswordHash("secret-hash");
        when(profiles.selectList(any(Wrapper.class))).thenReturn(List.of(profile));
        Map<?, ?> actual = JsonUtils.parseObject(identity.production(trace(9102L)), Map.class);
        assertEquals(Map.of("actorId", "9102", "actorType", "MES_EMPLOYEE_PROFILE",
                "displayName", "正式员工", "username", "EMP-9102"), actual);
        assertFalse(identity.production(trace(9102L)).contains("secret"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "duplicate", "disabled", "wrong-profile", "missing-user", "wrong-user", "disabled-user", "ambiguous-domain", "missing-name", "signature-mismatch"})
    void rejectsMissingOrMismatchedFormalIdentity(String invalid) {
        var profile = profile();
        var user = new AdminUserRespDTO().setId(9102L).setStatus(0).setNickname("正式员工").setUsername("employee.9102");
        if ("disabled".equals(invalid)) profile.setEnabled(false);
        if ("wrong-profile".equals(invalid)) profile.setSystemUserId(9999L);
        if ("wrong-user".equals(invalid)) user.setId(9999L);
        if ("disabled-user".equals(invalid)) user.setStatus(1);
        if ("ambiguous-domain".equals(invalid)) profile.setSystemUserId(null);
        if ("missing-name".equals(invalid)) user.setNickname(" ");
        when(profiles.selectList(any(Wrapper.class))).thenReturn("missing".equals(invalid) ? List.of()
                : "duplicate".equals(invalid) ? List.of(profile, profile) : List.of(profile));
        when(users.getUser(9102L)).thenReturn("missing-user".equals(invalid) ? null : user);
        assertThrows(ServiceException.class, () -> identity.production(trace(
                "signature-mismatch".equals(invalid) ? 9001L : 9102L)));
    }

    private MesProcessPoolTeamEmployeeProfileDO profile() {
        return MesProcessPoolTeamEmployeeProfileDO.builder().id(9102L).systemUserId(9102L)
                .enabled(true).employeeName("正式员工").employeeCode("EMP-9102").build();
    }

    private MesFrontlineSubmitIdentityTrace trace(Long signer) {
        return new MesFrontlineSubmitIdentityTrace(9001L, 9102L, signer,
                1L, 2L, 3L, 4L, 5L, "production", "snapshot", "hash", null);
    }
}
