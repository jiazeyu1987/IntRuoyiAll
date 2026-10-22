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
 * <p>MES route processes execute production reporting, including processes marked for quality
 * inspection. QA task execution resolves its own PQC template independently.</p>
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
        if (request.routeProcessCheckFlag() != null) {
            return toTemplateDescriptor(request.routeProcessId(), request.processId(),
                    request.actualEmployeeId());
        }
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectByIdIgnoreDeleted(request.routeProcessId());
        if (routeProcess == null || !matchesRequest(routeProcess, request)) {
            return null;
        }
        return toTemplateDescriptor(routeProcess.getId(), routeProcess.getProcessId(),
                request.actualEmployeeId());
    }

    private static MesFrontlineTemplateDescriptor toTemplateDescriptor(Long routeProcessId,
                                                                       Long processId,
                                                                       Long actualEmployeeId) {
        return new MesFrontlineTemplateDescriptor(
                FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED,
                FrontlineTemplateTypes.PRODUCTION,
                routeProcessId,
                processId,
                actualEmployeeId);
    }

    private static boolean matchesRequest(MesProRouteProcessDO routeProcess, MesFrontlineTemplateRequest request) {
        return Objects.equals(routeProcess.getRouteId(), request.routeId())
                && Objects.equals(routeProcess.getId(), request.routeProcessId())
                && Objects.equals(routeProcess.getProcessId(), request.processId());
    }

}
