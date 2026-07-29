package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import java.util.List;

public interface FrontlineTemplateService {

    List<FrontlineTemplateDefinition> listCatalog();

    FrontlineTemplateDefinition getTemplate(String templateCode);

    FrontlineTemplateDefinition resolveTemplate(FrontlineTemplateResolveCommand command);

    FrontlineTemplatePayload buildPayload(FrontlineTemplatePayloadCommand command);
}
