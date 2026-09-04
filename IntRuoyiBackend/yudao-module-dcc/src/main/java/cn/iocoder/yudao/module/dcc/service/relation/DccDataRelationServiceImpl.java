package cn.iocoder.yudao.module.dcc.service.relation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.relation.vo.DccDataRelationCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.relation.DccDataRelationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.relation.DccDataRelationMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRegistrationSyncService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DATA_RELATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DATA_RELATION_TARGET_INVALID;

@Service
@Validated
public class DccDataRelationServiceImpl implements DccDataRelationService {

    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String SOURCE_MANUAL = "MANUAL";

    @Resource
    private DccDataRelationMapper relationMapper;
    @Resource
    private DccProductCatalogMapper productCatalogMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccRegistrationCertificateMapper registrationCertificateMapper;
    @Resource
    private DccProductCatalogRegistrationSyncService productCatalogRegistrationSyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccDataRelationDO createRelation(Long userId, DccDataRelationCreateReqVO reqVO) {
        DccProductCatalogDO catalog = productCatalogMapper.selectById(reqVO.getProductCatalogId());
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(reqVO.getProjectCodeId());
        DccRegistrationCertificateDO certificate = registrationCertificateMapper
                .selectById(reqVO.getRegistrationCertificateId());
        if (catalog == null || projectCode == null || certificate == null) {
            throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        }
        String catalogProjectCode = StrUtil.trimToNull(catalog.getProjectCode());
        if (catalogProjectCode != null && !StrUtil.equals(catalogProjectCode, StrUtil.trim(projectCode.getProjectCode()))) {
            throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        }
        if (!projectCode.getId().equals(certificate.getProjectCodeId())) {
            throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        }
        if (relationMapper.selectIdentity(catalog.getId(), projectCode.getId(), certificate.getId()) != null) {
            throw exception(DCC_DATA_RELATION_CONFLICT);
        }
        DccDataRelationDO relation = BeanUtils.toBean(reqVO, DccDataRelationDO.class);
        relation.setRelationStatus(STATUS_CONFIRMED);
        relation.setRelationSource(SOURCE_MANUAL);
        relation.setConfirmedBy(userId);
        relation.setConfirmedTime(LocalDateTime.now());
        relationMapper.insert(relation);
        productCatalogRegistrationSyncService.syncRelation(relation);
        return relation;
    }

    @Override
    public List<DccDataRelationDO> getByProductCatalogId(Long productCatalogId) {
        return relationMapper.selectByProductCatalogId(productCatalogId);
    }

    @Override
    public List<DccDataRelationDO> getByProjectCodeId(Long projectCodeId) {
        return relationMapper.selectByProjectCodeId(projectCodeId);
    }

    @Override
    public List<DccDataRelationDO> getByRegistrationCertificateId(Long registrationCertificateId) {
        return relationMapper.selectByRegistrationCertificateId(registrationCertificateId);
    }

    @Override
    public void deleteRelation(Long id) {
        relationMapper.deleteById(id);
    }
}
