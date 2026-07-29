package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_BINDING_SOURCE_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_NOT_EXISTS;

@Service
public class MesFrontlineTemplateResolverImpl implements MesFrontlineTemplateResolver {

    private final ObjectProvider<MesFrontlineTemplateBindingSource> templateBindingSourceProvider;

    public MesFrontlineTemplateResolverImpl(ObjectProvider<MesFrontlineTemplateBindingSource> templateBindingSourceProvider) {
        this.templateBindingSourceProvider = templateBindingSourceProvider;
    }

    @Override
    public MesFrontlineTemplateDescriptor resolve(MesFrontlineTemplateRequest request) {
        requireRequest(request);
        MesFrontlineTemplateBindingSource templateBindingSource = templateBindingSourceProvider.getIfAvailable();
        if (templateBindingSource == null) {
            throw exception(PRO_FRONTLINE_TEMPLATE_BINDING_SOURCE_MISSING);
        }
        MesFrontlineTemplateDescriptor template = templateBindingSource.findTemplate(request);
        if (template == null || StrUtil.isBlank(template.templateNo())) {
            throw exception(PRO_FRONTLINE_TEMPLATE_NOT_EXISTS, request.actualEmployeeId(), request.processId());
        }
        if (!Objects.equals(template.actualEmployeeId(), request.actualEmployeeId())
                || !Objects.equals(template.routeProcessId(), request.routeProcessId())
                || !Objects.equals(template.processId(), request.processId())) {
            throw exception(PRO_FRONTLINE_TEMPLATE_MISMATCH, template.templateNo());
        }
        return template;
    }

    private static void requireRequest(MesFrontlineTemplateRequest request) {
        if (request == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "templateRequest");
        }
        requireValue(request.loginUserId(), "loginUserId");
        requireValue(request.actualEmployeeId(), "actualEmployeeId");
        requireValue(request.routeId(), "routeId");
        requireValue(request.routeProcessId(), "routeProcessId");
        requireValue(request.processId(), "processId");
    }

    private static void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

}
