package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 项目行锁覆盖检查到绑定的整个事务；禁用流程同样占用项目。 */
@Service
public class MesProRouteProjectGuard {
    @Resource private DccProjectCodeMapper projectMapper;
    @Resource private MesRouteDccProjectBindingMapper bindingMapper;
    @Resource private MesProRouteMapper routeMapper;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccess;

    @Transactional(rollbackFor = Exception.class)
    public void requireAvailable(Long projectId, Long ownRouteId) {
        if (projectId == null) throw invalidParamException("请选择工艺流程所属项目代码");
        projectAccess.assertProjectViewerOrAbove(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(), projectId);
        DccProjectCodeDO project = projectMapper.selectOne(new LambdaQueryWrapperX<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getId, projectId).last("FOR UPDATE"));
        if (project == null || !DccProjectCodeStatusConstants.ENABLE.equals(project.getStatus()))
            throw invalidParamException("所选项目代码不存在或已停用");
        List<MesRouteDccProjectBindingDO> bindings = bindingMapper.selectList(
                new LambdaQueryWrapperX<MesRouteDccProjectBindingDO>()
                        .eq(MesRouteDccProjectBindingDO::getDccProjectCodeId, projectId).last("FOR UPDATE"));
        for (MesRouteDccProjectBindingDO binding : bindings) {
            if (Objects.equals(binding.getRouteId(), ownRouteId)) continue;
            var route = routeMapper.selectById(binding.getRouteId());
            throw invalidParamException("项目 " + project.getProjectCode() + " 已存在工艺流程 "
                    + (route == null ? binding.getRouteId() : route.getName()) + "，请进入原流程维护");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindNewRoute(Long routeId, Long projectId) {
        requireAvailable(projectId, routeId);
        if (bindingMapper.selectCurrentByRouteId(routeId) != null)
            throw invalidParamException("工艺流程已有项目关联，不能重复创建绑定");
        Long lastVersion = bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(routeId);
        var binding = MesRouteDccProjectBindingDO.builder().routeId(routeId)
                .dccProjectCodeId(projectId).version(lastVersion == null ? 1L : lastVersion + 1).build();
        binding.setDeleted(false);
        bindingMapper.insert(binding);
    }

    public List<Map<String, Object>> options(String keyword) {
        var readable = projectAccess.listReadableProjectIds(
                cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());
        if (readable.isEmpty()) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        var bindingsByProject = bindingMapper.selectList().stream().collect(java.util.stream.Collectors.groupingBy(
                MesRouteDccProjectBindingDO::getDccProjectCodeId));
        for (var project : projectMapper.selectList(new LambdaQueryWrapperX<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getStatus, DccProjectCodeStatusConstants.ENABLE)
                .in(DccProjectCodeDO::getId, readable))) {
            String label = project.getProjectCode() + " / " + project.getProjectName();
            if (keyword != null && !label.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) continue;
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("status", project.getStatus());
            option.put("id", project.getId()); option.put("projectCode", project.getProjectCode());
            option.put("projectName", project.getProjectName());
            var bindings = bindingsByProject.getOrDefault(project.getId(), List.of());
            option.put("occupied", !bindings.isEmpty());
            option.put("routeIds", bindings.stream().map(MesRouteDccProjectBindingDO::getRouteId).toList());
            result.add(option);
        }
        return result;
    }

    public boolean isOccupied(Long projectId) {
        projectAccess.assertProjectViewerOrAbove(
                cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(), projectId);
        return !bindingMapper.selectCurrentListByDccProjectCodeId(projectId).isEmpty();
    }

    public String boundProjectCode(Long routeId) {
        var binding = bindingMapper.selectCurrentByRouteId(routeId);
        if (binding == null) return "";
        var project = projectMapper.selectById(binding.getDccProjectCodeId());
        if (project == null) throw invalidParamException("工艺流程关联的项目代码不存在：" + routeId);
        return project.getProjectCode();
    }

    public Long requireProjectCode(String code) {
        var projects = projectMapper.selectList(new LambdaQueryWrapperX<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getProjectCode, code));
        if (projects.size() != 1) throw invalidParamException("项目代码不存在或不唯一：" + code);
        return projects.get(0).getId();
    }
}
