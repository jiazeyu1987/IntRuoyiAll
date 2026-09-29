package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

/** Server-owned identity; numeric IDs are meaningful only within their declared domain. */
@Component
public class MesFrontlineAuditIdentity {
    private final MesProcessPoolTeamEmployeeProfileMapper profileMapper;
    private final AdminUserApi adminUserApi;

    public MesFrontlineAuditIdentity(MesProcessPoolTeamEmployeeProfileMapper profileMapper, AdminUserApi adminUserApi) {
        this.profileMapper = profileMapper;
        this.adminUserApi = adminUserApi;
    }

    public String production(MesFrontlineSubmitIdentityTrace identity) {
        if (identity == null || identity.actualEmployeeId() == null
                || !Objects.equals(identity.actualEmployeeId(), identity.signatureEmployeeId())) {
            throw invalid();
        }
        Long actorId = identity.actualEmployeeId();
        List<MesProcessPoolTeamEmployeeProfileDO> profiles = profileMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getEnabled, true)
                        .and(q -> q.eq(MesProcessPoolTeamEmployeeProfileDO::getSystemUserId, actorId)
                                .or(p -> p.eq(MesProcessPoolTeamEmployeeProfileDO::getId, actorId)
                                        .isNull(MesProcessPoolTeamEmployeeProfileDO::getSystemUserId))));
        if (profiles == null || profiles.size() != 1) throw invalid();
        MesProcessPoolTeamEmployeeProfileDO profile = profiles.get(0);
        if (profile == null || !Boolean.TRUE.equals(profile.getEnabled())) throw invalid();
        if (profile.getSystemUserId() != null) {
            if (!Objects.equals(actorId, profile.getSystemUserId())) throw invalid();
            var user = adminUserApi.getUser(actorId);
            if (user == null || !Objects.equals(actorId, user.getId())
                    || !CommonStatusEnum.isEnable(user.getStatus())) throw invalid();
            return encode(actorId, "SYSTEM_USER", user.getNickname(), user.getUsername());
        }
        // An unlinked profile and a system account must never share an ambiguous effective identity.
        if (!Objects.equals(actorId, profile.getId()) || adminUserApi.getUser(actorId) != null) throw invalid();
        return encode(actorId, "MES_EMPLOYEE_PROFILE", profile.getEmployeeName(), profile.getEmployeeCode());
    }

    public static String pqc(MesFrontlineEmployeeCandidate employee) {
        if (employee == null) throw invalid();
        return encode(employee.userId(), "SYSTEM_USER", employee.nickname(), employee.username());
    }

    public static String persistedPqc(Object snapshot, Long actorId) {
        if (!(snapshot instanceof Map<?, ?> identity) || actorId == null
                || !Objects.equals(String.valueOf(actorId), identity.get("actorId"))
                || !"SYSTEM_USER".equals(identity.get("actorType"))
                || !(identity.get("displayName") instanceof String name)
                || !(identity.get("username") instanceof String username)) throw invalid();
        return encode(actorId, "SYSTEM_USER", name, username);
    }

    private static String encode(Long actorId, String actorType, String name, String username) {
        if (actorId == null || actorId <= 0 || StrUtil.hasBlank(name, username)) throw invalid();
        Map<String, Object> result = new TreeMap<>();
        result.put("actorId", actorId.toString());
        result.put("actorType", actorType);
        result.put("displayName", name);
        result.put("username", username);
        return JsonUtils.toJsonString(result);
    }

    private static cn.iocoder.yudao.framework.common.exception.ServiceException invalid() {
        return exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "gxpAudit.formalEmployeeIdentity");
    }
}
