package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesFrontlineSequenceSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MesFrontlineSequenceSettingService {
    private final MesFrontlineSequenceSettingMapper mapper;

    public boolean isEnabled() {
        Boolean enabled = mapper.getEnabled(TenantContextHolder.getRequiredTenantId());
        if (enabled == null) throw new IllegalStateException("当前租户工序顺序配置缺失，请检查租户及数据库迁移");
        return enabled;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(boolean enabled) {
        isEnabled();
        if (mapper.updateEnabled(TenantContextHolder.getRequiredTenantId(), enabled,
                String.valueOf(SecurityFrameworkUtils.getLoginUserId())) != 1) {
            throw new IllegalStateException("工序顺序配置保存失败");
        }
    }
}
