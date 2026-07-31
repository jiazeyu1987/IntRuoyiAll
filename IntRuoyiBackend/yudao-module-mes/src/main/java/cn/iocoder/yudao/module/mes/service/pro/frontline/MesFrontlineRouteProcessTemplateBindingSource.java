package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateCodes;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateTypes;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Formal template binding source for fixed frontline pages.
 *
 * <p>The route process `checkFlag` is the configured process-level quality-inspection marker:
 * quality-check route processes use the PQC simplified template; all other formal route processes
 * use the production simplified template.</p>
 */
@Service
public class MesFrontlineRouteProcessTemplateBindingSource implements MesFrontlineTemplateBindingSource {

    private final MesProRouteProcessMapper routeProcessMapper;

    public MesFrontlineRouteProcessTemplateBindingSource(MesProRouteProcessMapper routeProcessMapper) {
        this.routeProcessMapper = routeProcessMapper;
    }

    @Override
    public MesFrontlineTemplateDescriptor findTemplate(MesFrontlineTemplateRequest request) {
        if (request == null || request.routeProcessId() == null) {
            return null;
        }
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(request.routeProcessId());
        if (routeProcess == null || !matchesRequest(routeProcess, request)) {
            return null;
        }
        boolean pqcProcess = Boolean.TRUE.equals(routeProcess.getCheckFlag());
        return new MesFrontlineTemplateDescriptor(
                pqcProcess ? FrontlineTemplateCodes.PQC_SIMPLIFIED : FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED,
                pqcProcess ? FrontlineTemplateTypes.PQC : FrontlineTemplateTypes.PRODUCTION,
                routeProcess.getId(),
                routeProcess.getProcessId(),
                request.actualEmployeeId());
    }

    private static boolean matchesRequest(MesProRouteProcessDO routeProcess, MesFrontlineTemplateRequest request) {
        return Objects.equals(routeProcess.getRouteId(), request.routeId())
                && Objects.equals(routeProcess.getId(), request.routeProcessId())
                && Objects.equals(routeProcess.getProcessId(), request.processId());
    }

}
