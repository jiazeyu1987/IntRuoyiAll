package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccApprovedProductIdentityMapper;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;

/** One typed server product projection; declared MDM never falls back to the DCC catalog. */
public final class DccProjectProductIdentityResolver {
    public static final String MDM_MASTER="MDM_MASTER", DCC_CATALOG="DCC_CATALOG", UNBOUND="UNBOUND";
    private DccProjectProductIdentityResolver() { }

    public record Product(String source, Long masterId, Long catalogId, Long relationId, Long requestId,
                          String code, String name) {
        public boolean bound() { return MDM_MASTER.equals(source) || DCC_CATALOG.equals(source); }
    }

    public static Product resolve(DccProjectCodeDO project, MdmProductApi mdm,
                                  DccApprovedProductIdentityMapper approved) {
        if (project==null || project.getId()==null || StrUtil.isBlank(project.getProjectCode())
                || StrUtil.isBlank(project.getProjectName())) throw invalid();
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if (TenantContextHolder.isIgnore() || !Objects.equals(tenant,project.getTenantId())) throw invalid();
        if (project.getProductMasterId()!=null) {
            var product=mdm.getEnabledDccProduct(project.getProductMasterId());
            if (product==null || !Objects.equals(product.getId(),project.getProductMasterId())
                    || StrUtil.isBlank(product.getDccProductCode()) || StrUtil.isBlank(product.getNameCn()))
                throw invalid();
            return new Product(MDM_MASTER,product.getId(),null,null,null,
                    product.getDccProductCode().trim(),product.getNameCn().trim());
        }
        var rows=approved.selectApprovedProduct(tenant,project.getId());
        if (rows.size()==1) {
            var row=rows.get(0);
            if (!Objects.equals(row.projectId(),project.getId()) || row.catalogId()==null || row.relationId()==null
                    || row.requestId()==null || StrUtil.isBlank(row.productCode()) || StrUtil.isBlank(row.productName())
                    || !Objects.equals(row.productCode(),row.catalogCode()) || !Objects.equals(row.productName(),row.catalogName())
                    || approved.countDeclaredProductEvidence(tenant,project.getId())!=2)
                throw invalid();
            return new Product(DCC_CATALOG,null,row.catalogId(),row.relationId(),row.requestId(),row.productCode(),row.productName());
        }
        if (!rows.isEmpty() || approved.countDeclaredProductEvidence(tenant,project.getId())!=0) throw invalid();
        return new Product(UNBOUND,null,null,null,null,null,null);
    }
    private static RuntimeException invalid() { return exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING); }
}
