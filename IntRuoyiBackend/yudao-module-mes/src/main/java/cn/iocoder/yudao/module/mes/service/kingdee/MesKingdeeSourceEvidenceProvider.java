package cn.iocoder.yudao.module.mes.service.kingdee;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeSourceEvidenceProvider;
import cn.iocoder.yudao.module.mes.dal.mysql.kingdee.MesKingdeeSourceEvidenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MesKingdeeSourceEvidenceProvider implements ErpKingdeeSourceEvidenceProvider {
    private final MesKingdeeSourceEvidenceMapper mapper;
    @Override public boolean hasEvidence(Long tenantId) {
        return mapper.hasEvidence(tenantId);
    }
}
