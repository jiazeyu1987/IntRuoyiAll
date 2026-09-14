package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
public class DccProjectResolver {

    public static final ErrorCode ROUTE_DCC_BINDING_REQUIRED = new ErrorCode(1_040_501_502,
            "工艺路线缺少正式 DCC 项目代码关系：{}");
    public static final ErrorCode ROUTE_DCC_BINDING_AMBIGUOUS = new ErrorCode(1_040_501_503,
            "工艺路线存在多条正式 DCC 项目代码关系：{}");
    public static final ErrorCode DCC_PROJECT_REFERENCE_INVALID = new ErrorCode(1_040_501_504,
            "工艺路线引用的 DCC 项目代码无效：{}");

    private final MesRouteDccProjectBindingMapper bindingMapper;
    private final DccProjectCodeMapper projectCodeMapper;

    public DccProjectResolver(MesRouteDccProjectBindingMapper bindingMapper,
                              DccProjectCodeMapper projectCodeMapper) {
        this.bindingMapper = bindingMapper;
        this.projectCodeMapper = projectCodeMapper;
    }

    public ResolvedProject requireEnabledByRoute(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw exception(ROUTE_DCC_BINDING_REQUIRED, routeId);
        }
        List<MesRouteDccProjectBindingDO> bindings = bindingMapper.selectList(
                new LambdaQueryWrapperX<MesRouteDccProjectBindingDO>()
                        .eq(MesRouteDccProjectBindingDO::getRouteId, routeId));
        if (bindings == null || bindings.isEmpty()) {
            throw exception(ROUTE_DCC_BINDING_REQUIRED, routeId);
        }
        if (bindings.size() != 1) {
            throw exception(ROUTE_DCC_BINDING_AMBIGUOUS, routeId);
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesRouteDccProjectBindingDO binding = bindings.get(0);
        if (binding == null || Boolean.TRUE.equals(binding.getDeleted())
                || !Objects.equals(tenantId, binding.getTenantId())
                || binding.getDccProjectCodeId() == null || binding.getDccProjectCodeId() <= 0) {
            throw exception(DCC_PROJECT_REFERENCE_INVALID, routeId);
        }

        DccProjectCodeDO project = projectCodeMapper.selectById(binding.getDccProjectCodeId());
        if (project == null || Boolean.TRUE.equals(project.getDeleted())
                || !Objects.equals(tenantId, project.getTenantId())
                || !DccProjectCodeStatusConstants.ENABLE.equals(project.getStatus())) {
            throw exception(DCC_PROJECT_REFERENCE_INVALID, binding.getDccProjectCodeId());
        }
        return new ResolvedProject(project.getId(), project.getProjectCode(), project.getProjectName());
    }

    public record ResolvedProject(Long dccProjectCodeId, String projectCode, String projectName) {
    }
}
