package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Accessors(chain = true)
public class ApprovalProviderDescriptor {

    private ApprovalModuleCode moduleCode;

    private String moduleName;

    private String providerCode;

    private String providerVersion;

    private Set<ApprovalTaskViewType> supportedViewTypes = new LinkedHashSet<>();

    private Set<ApprovalTaskCapability> capabilities = new LinkedHashSet<>();
}
