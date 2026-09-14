package cn.iocoder.yudao.module.mes.service.pro.frontline;

/**
 * Formal source for current process template binding after employee switch.
 */
public interface MesFrontlineTemplateBindingSource {

    MesFrontlineTemplateDescriptor findTemplate(MesFrontlineTemplateRequest request);

}
