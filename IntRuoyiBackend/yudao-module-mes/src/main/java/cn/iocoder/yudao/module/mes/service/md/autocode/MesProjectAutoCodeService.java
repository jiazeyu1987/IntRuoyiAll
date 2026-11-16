package cn.iocoder.yudao.module.mes.service.md.autocode;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.autocode.*;
import cn.iocoder.yudao.module.mes.dal.mysql.md.autocode.*;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 已登记的项目规则按需初始化；项目行锁串行化首次创建，已有配置不覆盖。 */
@Service
public class MesProjectAutoCodeService {
    private static final Map<String,String> PREFIXES = Map.of(
            "PRO_PROCESS_GX_CODE", "GX-", "PRO_ROUTE_CODE", "GYLC-");
    @Resource private DccProjectCodeMapper projectMapper;
    @Resource private DccProjectAccessService projectAccess;
    @Resource private MesMdAutoCodeRuleMapper ruleMapper;
    @Resource private MesMdAutoCodePartMapper partMapper;
    @Resource private JdbcTemplate jdbc;

    public static boolean supports(String code) { return code != null && PREFIXES.containsKey(code); }

    @Transactional(rollbackFor = Exception.class)
    public MesMdAutoCodeRuleDO ensure(Long projectId, String code) {
        if (projectId == null || projectId <= 0) throw invalidParamException("请选择编号所属项目");
        if (!supports(code)) throw invalidParamException("尚未登记项目编码规则：" + code);
        projectAccess.assertProjectViewerOrAbove(SecurityFrameworkUtils.getLoginUserId(), projectId);
        return ensureInternal(projectId, code);
    }

    @org.springframework.context.event.EventListener
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY, rollbackFor = Exception.class)
    public void initialize(cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCreatedEvent event) {
        for (String code : PREFIXES.keySet()) ensureInternal(event.projectId(), code);
    }

    private MesMdAutoCodeRuleDO ensureInternal(Long projectId, String code) {
        DccProjectCodeDO project = projectMapper.selectOne(new LambdaQueryWrapperX<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getId, projectId).last("FOR UPDATE"));
        if (project == null || !"ENABLE".equals(project.getStatus()))
            throw invalidParamException("编号所属项目不存在或已停用");
        MesMdAutoCodeRuleDO rule = ruleMapper.selectOne(new LambdaQueryWrapperX<MesMdAutoCodeRuleDO>()
                .eq(MesMdAutoCodeRuleDO::getProjectId, projectId).eq(MesMdAutoCodeRuleDO::getCode, code));
        if (rule == null) {
            String prefix = project.getProjectCode() + "-" + PREFIXES.get(code);
            if (project.getProjectCode() == null || project.getProjectCode().isBlank() || prefix.length() > 48)
                throw invalidParamException("项目代码为空或过长，无法初始化编号规则");
            long next = Math.addExact(highWatermark(prefix, code), 1);
            if (next > Integer.MAX_VALUE) throw invalidParamException("历史流水超出可初始化范围");
            rule = MesMdAutoCodeRuleDO.builder().projectId(projectId).code(code)
                    .name(project.getProjectCode() + (code.equals("PRO_ROUTE_CODE") ? "工艺流程编码" : "工序编码"))
                    .maxLength(64).padded(false).status(0).build();
            ruleMapper.insert(rule);
            partMapper.insert(MesMdAutoCodePartDO.builder().ruleId(rule.getId()).sort(1).type(3)
                    .length(prefix.length()).fixCharacter(prefix).build());
            partMapper.insert(MesMdAutoCodePartDO.builder().ruleId(rule.getId()).sort(2).type(4)
                    .length(4).serialStartNo((int) next).serialStep(1).cycleFlag(false).build());
        }
        if (!Integer.valueOf(0).equals(rule.getStatus())) throw invalidParamException("项目编码规则已停用：" + code);
        List<MesMdAutoCodePartDO> parts = partMapper.selectListByRuleId(rule.getId());
        if (parts.isEmpty()) throw invalidParamException("项目编码规则组成缺失：" + code);
        // 当前登记的两类编号均为固定前缀＋不循环流水；错误配置明确拒绝，不能补成默认配置。
        if (parts.size() != 2 || !Integer.valueOf(3).equals(parts.get(0).getType())
                || !Integer.valueOf(4).equals(parts.get(1).getType()))
            throw invalidParamException("项目编码规则必须由固定前缀和流水号组成：" + code);
        var fixed = parts.get(0); var serial = parts.get(1);
        if (fixed.getFixCharacter() == null || fixed.getFixCharacter().isBlank()
                || !Integer.valueOf(fixed.getFixCharacter().length()).equals(fixed.getLength())
                || serial.getLength() == null || serial.getLength() < 1 || serial.getLength() > 18
                || serial.getSerialStartNo() == null || serial.getSerialStartNo() < 1
                || serial.getSerialStep() == null || serial.getSerialStep() < 1
                || Boolean.TRUE.equals(serial.getCycleFlag()) || Boolean.TRUE.equals(rule.getPadded()))
            throw invalidParamException("项目编码规则配置无效，请检查前缀、流水号及补齐设置：" + code);
        return rule;
    }

    private long highWatermark(String prefix, String code) {
        String table = code.equals("PRO_ROUTE_CODE") ? "mes_pro_route" : "mes_pro_process";
        long tenant = TenantContextHolder.getRequiredTenantId();
        // 包含逻辑删除的业务和生成历史，重新初始化不能复用旧号码。
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(v, ?) AS UNSIGNED)),0) FROM ("
                + "SELECT code v FROM " + table + " WHERE tenant_id=? UNION ALL "
                + "SELECT result v FROM mes_md_auto_code_record WHERE tenant_id=?) h "
                + "WHERE LEFT(v,?)=? AND SUBSTRING(v,?) REGEXP '^[0-9]+$'";
        Long value = jdbc.queryForObject(sql, Long.class, prefix.length()+1, tenant, tenant,
                prefix.length(), prefix, prefix.length()+1);
        if (value == null) throw new IllegalStateException("项目历史流水查询失败");
        return value;
    }
}
