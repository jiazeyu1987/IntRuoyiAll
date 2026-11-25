package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProjectProduct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
public class DccRegistrationCertificateProductMaintenanceService {
    @jakarta.annotation.Resource private DccRegistrationCertificateMapper certificates;
    @jakarta.annotation.Resource private DccRegistrationCertificateSnapshotMapper snapshots;
    @jakarta.annotation.Resource private DccRegistrationCertificateSnapshotEntrustedMapper entrusted;
    @jakarta.annotation.Resource private DccRegistrationCertificateAuditMapper audits;
    @jakarta.annotation.Resource private DccRegistrationCertificatePrerequisiteValidator validator;
    @jakarta.annotation.Resource private DccRegistrationCertificateProductBindingService bindings;
    @jakarta.annotation.Resource private DccRegistrationCertificateBusinessClock clock;

    @Transactional(rollbackFor=Exception.class)
    public void addProducts(Long tenantId, Long actorId, Long certificateId, Integer expectedRowVersion, List<Long> projectIds) {
        var certificate=certificates.selectByIdForUpdate(certificateId);
        if(certificate==null || !Objects.equals(tenantId,certificate.getTenantId())) throw invalidParamException("注册证不存在");
        validator.validateCompanyScope(actorId,certificate.getOwnerCompanyId());
        if(!"ACTIVE".equals(certificate.getStatus()) || certificate.getCurrentSnapshotId()==null
                || certificate.getPendingVersionId()!=null || certificates.countPendingChanges(tenantId,certificateId)>0
                || certificates.countPendingUploads(tenantId,certificateId)>0) throw invalidParamException("仅可为没有待审批或待生效变更的现行注册证追加产品");
        if(!Objects.equals(expectedRowVersion,certificate.getRowVersion())) throw invalidParamException("注册证已更新，请刷新后重试");
        var original=snapshots.selectById(certificate.getCurrentSnapshotId());
        if(original==null || !Objects.equals(original.getTenantId(),tenantId) || !Objects.equals(original.getVersionId(),certificate.getCurrentVersionId())) throw invalidParamException("现行注册证快照不一致");
        var facts=DccRegistrationCertificateFacts.parse(original.getFactsJson());
        if(!Boolean.TRUE.equals(facts.getOwnProduct()) || facts.getProjectProducts()==null || facts.getProjectProducts().isEmpty()) throw invalidParamException("请先完成注册证产品关联迁移；非瑛泰产品证件不能追加系统产品");
        var products=new ArrayList<>(facts.getProjectProducts());
        var ids=new HashSet<Long>(); products.forEach(p->ids.add(p.productCatalogId()));
        if(projectIds==null || projectIds.isEmpty() || projectIds.size()!=new HashSet<>(projectIds).size()) throw invalidParamException("请选择不重复的系统项目代码");
        for(var id:projectIds){
            var product=validator.resolveProjectProduct(validator.validateProjectCode(tenantId,actorId,id,null));
            if(!ids.add(product.productCatalogId())) throw invalidParamException("产品「"+product.productName()+"」已在该注册证中，请勿重复关联");
            products.add(product);
        }
        bindings.confirm(tenantId,certificateId,products,actorId);
        facts.setProjectProducts(products);
        // Append a new snapshot; the previous approved association remains available in history.
        var next=new DccRegistrationCertificateSnapshotDO();
        org.springframework.beans.BeanUtils.copyProperties(original,next);
        next.setId(null); next.setCreator(actorId.toString()); next.setUpdater(actorId.toString());
        next.setCreateTime(null); next.setUpdateTime(null); next.setSourceChangeId(null);
        next.setRevisionNo(original.getRevisionNo()+1); next.setFactsJson(facts.toJson());
        next.setProductName(products.stream().map(DccRegistrationCertificateProjectProduct::productName).collect(java.util.stream.Collectors.joining("、")));
        if(next.getProductName().length()>255) throw invalidParamException("关联产品名称总长度不能超过255个字符");
        if(snapshots.insert(next)!=1) throw invalidParamException("保存产品关联快照失败");
        for(var row:entrusted.selectListBySnapshotId(original.getId())){
            var copy=new DccRegistrationCertificateSnapshotEntrustedDO();
            org.springframework.beans.BeanUtils.copyProperties(row,copy);copy.setId(null);copy.setSnapshotId(next.getId());
            copy.setCreateTime(null);copy.setUpdateTime(null);
            if(entrusted.insert(copy)!=1) throw invalidParamException("保存受托企业快照失败");
        }
        certificate.setCurrentSnapshotId(next.getId());certificate.setRowVersion(certificate.getRowVersion()+1);
        if(certificates.updateById(certificate)!=1) throw invalidParamException("更新注册证失败");
        var audit=DccRegistrationCertificateAuditDO.builder().tenantId(tenantId).ownerCompanyId(certificate.getOwnerCompanyId())
                .certificateId(certificateId).versionId(certificate.getCurrentVersionId()).snapshotId(next.getId())
                .eventKey("PRODUCT_ADD:"+UUID.randomUUID()).eventType("PRODUCT_BINDING_ADD").actorId(actorId)
                .result("SUCCESS").resultCode("0").requestTraceId("PRODUCT_ADD:"+next.getId()).detailJson(facts.toJson()).occurredAt(clock.now()).build();
        if(audits.insert(audit)!=1) throw invalidParamException("保存关联审计失败");
    }
}
