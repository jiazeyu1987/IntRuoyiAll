package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateProjectProductMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProjectProduct;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
public class DccRegistrationCertificateProjectProductService {
    private final DccRegistrationCertificateProjectProductMapper mapper;

    public DccRegistrationCertificateProjectProductService(DccRegistrationCertificateProjectProductMapper mapper) {
        this.mapper = mapper;
    }

    public DccRegistrationCertificateProjectProduct resolve(DccProjectCodeDO project) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (TenantContextHolder.isIgnore() || project == null || project.getId() == null || project.isLibrary()
                || !Objects.equals(tenantId, project.getTenantId()) || !"ENABLE".equals(project.getStatus())) {
            throw invalidParamException("项目不存在、已停用或不属于当前租户");
        }
        var rows = mapper.selectBindings(tenantId, project.getId());
        if (rows.isEmpty()) throw invalidParamException("项目尚未绑定有效的DCC产品，请先维护产品关联");
        var first = rows.get(0);
        var sources = new HashSet<String>();
        for (var row : rows) {
            if (!Objects.equals(tenantId, row.tenantId()) || !Objects.equals(project.getId(), row.projectCodeId())
                    || row.productCatalogId() == null || row.relationId() == null
                    || !Objects.equals(first.productCatalogId(), row.productCatalogId())
                    || !sources.add(row.source())) {
                throw invalidParamException("项目的DCC产品关联不唯一或无效，请先维护为唯一有效关联");
            }
            if (row.productCode() == null || row.productCode().isBlank()
                    || row.productName() == null || row.productName().isBlank()) {
                throw invalidParamException("项目绑定的DCC产品缺少编码或名称，请先维护产品目录");
            }
        }
        return new DccRegistrationCertificateProjectProduct(project.getId(), first.productCatalogId(),
                first.productCode().trim(), first.productName().trim(), rows.stream()
                .map(row -> new DccRegistrationCertificateProjectProduct.Binding(row.source(), row.relationId())).toList());
    }
}
