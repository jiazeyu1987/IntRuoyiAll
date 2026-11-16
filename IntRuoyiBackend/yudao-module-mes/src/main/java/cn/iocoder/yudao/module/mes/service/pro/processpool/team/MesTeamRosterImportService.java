package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.github.promeg.pinyinhelper.Pinyin;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
public class MesTeamRosterImportService {
    private static final String INITIAL_PASSWORD = "Int@2025";
    @Resource private AdminUserService userService;
    @Resource private RoleService roleService;
    @Resource private PermissionService permissionService;
    @Resource private MesTeamLeaderRuntimeConfigService runtimeConfigService;

    public record ImportedPerson(String name, String role, String username, String result) {}

    /** 先由具有用户创建及角色分配权限的管理员导入组长，再由该组长导入班组成员。 */
    @Transactional(rollbackFor = Exception.class)
    public List<ImportedPerson> importLeader(List<MesTeamRosterRow> rows) {
        MesTeamRosterRow leader = validateRoster(rows);
        var roles = roleService.getRoleList().stream()
                .filter(r -> "生产组长权限角色".equals(r.getName())
                        && CommonStatusEnum.ENABLE.getStatus().equals(r.getStatus())).toList();
        if (roles.size() != 1) {
            throw invalidParamException("当前租户必须配置唯一且启用的“生产组长权限角色”");
        }
        var role = roles.get(0);
        if (!permissionService.hasAnyPermissionsInRoles(Set.of(role.getId()),
                "mes:pro-process-pool-team-leader:query")
                || !permissionService.hasAnyPermissionsInRoles(Set.of(role.getId()),
                "mes:pro-process-pool-team-leader:maintain")) {
            throw invalidParamException("生产组长角色缺少工作台查询或人员维护权限，请先配置角色");
        }
        AdminUserDO user = ensureUser(leader);
        Set<Long> ids = new HashSet<>(permissionService.getUserRoleIdListByUserId(user.getId()));
        if (ids.add(role.getId())) {
            permissionService.assignUserRole(user.getId(), ids,
                    "导入车间人员名单，授予生产组长角色：" + leader.getWorkshop(),
                    "team-roster:" + UUID.randomUUID());
        }
        return List.of(new ImportedPerson(leader.getName(), "组长", user.getUsername(), "已关联生产组长角色"));
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ImportedPerson> importMembers(Long currentLeaderId, List<MesTeamRosterRow> rows) {
        MesTeamRosterRow leader = validateRoster(rows);
        // 通过唯一账号核对归属，不接受前端指定其他组长 ID。
        AdminUserDO owner = findUser(leader);
        if (owner == null || !Objects.equals(owner.getId(), currentLeaderId)) {
            throw invalidParamException("名单中的组长与当前登录账号不一致，请由名单中的组长登录后导入");
        }
        List<MesProcessPoolTeamEmployeeProfileDO> existing = runtimeConfigService.listEmployeeProfiles(currentLeaderId, null);
        List<ImportedPerson> result = new ArrayList<>();
        for (MesTeamRosterRow row : rows) {
            if (row == leader) continue;
            String type = "正式工".equals(row.getEmploymentType()) ? "FORMAL" : "TEMPORARY";
            var profiles = existing.stream().filter(p -> row.getName().equals(p.getDisplayName())).toList();
            AdminUserDO user = "FORMAL".equals(type) ? ensureUser(row) : null;
            if (!profiles.isEmpty()) {
                var profile = profiles.get(0);
                if (profiles.size() != 1 || !type.equals(profile.getEmployeeType())
                        || !Boolean.TRUE.equals(profile.getEnabled())
                        || (user != null && !Objects.equals(user.getId(), profile.getSystemUserId()))) {
                    throw invalidParamException("人员“{}”已有档案的类型、状态或账号不一致，请先核对", row.getName());
                }
                result.add(new ImportedPerson(row.getName(), "组员", user == null ? null : user.getUsername(), "已存在，保留原密码"));
                continue;
            }
            if (user != null) {
                runtimeConfigService.linkFormalEmployee(MesTeamFormalEmployeeLinkReqBO.builder()
                        .leaderUserId(currentLeaderId).systemUserId(user.getId()).displayName(row.getName()).build());
            } else {
                runtimeConfigService.createTemporaryEmployee(MesTeamTemporaryEmployeeCreateReqBO.builder()
                        .leaderUserId(currentLeaderId).displayName(row.getName()).signaturePassword(INITIAL_PASSWORD).build());
            }
            result.add(new ImportedPerson(row.getName(), "组员", user == null ? null : user.getUsername(), "已导入人员管理"));
        }
        return result;
    }

    static MesTeamRosterRow validateRoster(List<MesTeamRosterRow> rows) {
        if (rows == null || rows.isEmpty()) throw invalidParamException("名单不能为空");
        Set<String> names = new HashSet<>();
        Set<String> usernames = new HashSet<>();
        Set<String> workshops = new HashSet<>();
        List<MesTeamRosterRow> leaders = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            MesTeamRosterRow row = rows.get(i);
            if (row == null) throw invalidParamException("第{}行数据为空", i + 2);
            row.setWorkshop(trim(row.getWorkshop()));
            row.setName(trim(row.getName()));
            row.setRole(trim(row.getRole()));
            row.setEmploymentType(trim(row.getEmploymentType()));
            row.setUsername(trim(row.getUsername()));
            if (row.getWorkshop().isEmpty() || row.getName().isEmpty() || row.getName().length() > 30) {
                throw invalidParamException("第{}行必须填写车间及有效姓名", i + 2);
            }
            if (!names.add(row.getName())) throw invalidParamException("名单存在重名“{}”，请核对后导入", row.getName());
            workshops.add(row.getWorkshop());
            if ("组长".equals(row.getRole())) {
                leaders.add(row);
            } else if (!"组员".equals(row.getRole()) || !Set.of("正式工", "临时工").contains(row.getEmploymentType())) {
                throw invalidParamException("第{}行人员角色必须为组长/组员，组员用工类型必须为正式工/临时工", i + 2);
            }
            if (!"临时工".equals(row.getEmploymentType()) || "组长".equals(row.getRole())) {
                String username = usernameFor(row);
                if (!username.matches("[a-zA-Z0-9]{4,30}") || !usernames.add(username.toLowerCase(Locale.ROOT))) {
                    throw invalidParamException("第{}行账号无效或与名单内其他人员冲突，请填写唯一的“用户账号”", i + 2);
                }
            }
        }
        if (workshops.size() != 1 || leaders.size() != 1) {
            throw invalidParamException("每份名单必须属于同一车间，并且恰好包含一名组长");
        }
        return leaders.get(0);
    }

    private AdminUserDO findUser(MesTeamRosterRow row) {
        String username = usernameFor(row);
        AdminUserDO user = userService.getUserByUsername(username);
        if (user != null && (!row.getName().equals(user.getNickname())
                || !CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus()))) {
            throw invalidParamException("账号“{}”已被其他姓名使用或已禁用，请核对“用户账号”列", username);
        }
        // 不以昵称猜测绑定已有账号。姓名相同但账号不同必须显式填写已有账号。
        if (user == null && userService.getUserListByNickname(row.getName()).stream()
                .anyMatch(u -> row.getName().equals(u.getNickname()))) {
            throw invalidParamException("人员“{}”已有其他系统账号，请在“用户账号”列填写该账号", row.getName());
        }
        return user;
    }

    private AdminUserDO ensureUser(MesTeamRosterRow row) {
        AdminUserDO user = findUser(row);
        if (user != null) return user;
        Long id = userService.createUser(new UserSaveReqVO().setUsername(usernameFor(row))
                .setNickname(row.getName()).setPassword(INITIAL_PASSWORD)
                .setRemark("车间人员名单导入：" + row.getWorkshop()));
        return userService.getUser(id);
    }

    static String usernameFor(MesTeamRosterRow row) {
        return row.getUsername() == null || row.getUsername().isBlank()
                ? Pinyin.toPinyin(row.getName(), "").toLowerCase(Locale.ROOT) : row.getUsername();
    }

    private static String trim(String value) { return value == null ? "" : value.trim(); }
}
