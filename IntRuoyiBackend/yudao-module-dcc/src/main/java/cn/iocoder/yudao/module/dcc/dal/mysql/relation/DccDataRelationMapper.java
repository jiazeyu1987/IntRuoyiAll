package cn.iocoder.yudao.module.dcc.dal.mysql.relation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.relation.DccDataRelationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DccDataRelationMapper extends BaseMapperX<DccDataRelationDO> {

    default DccDataRelationDO selectIdentity(Long productCatalogId, Long projectCodeId,
                                               Long registrationCertificateId) {
        return selectOne(new LambdaQueryWrapperX<DccDataRelationDO>()
                .eq(DccDataRelationDO::getProductCatalogId, productCatalogId)
                .eq(DccDataRelationDO::getProjectCodeId, projectCodeId)
                .eq(DccDataRelationDO::getRegistrationCertificateId, registrationCertificateId));
    }

    default List<DccDataRelationDO> selectByProductCatalogId(Long productCatalogId) {
        return selectList(new LambdaQueryWrapperX<DccDataRelationDO>()
                .eq(DccDataRelationDO::getProductCatalogId, productCatalogId)
                .orderByDesc(DccDataRelationDO::getId));
    }

    default List<DccDataRelationDO> selectByProductCatalogIds(Collection<Long> productCatalogIds) {
        return selectList(new LambdaQueryWrapperX<DccDataRelationDO>()
                .inIfPresent(DccDataRelationDO::getProductCatalogId, productCatalogIds)
                .orderByDesc(DccDataRelationDO::getId));
    }

    default List<DccDataRelationDO> selectByProjectCodeId(Long projectCodeId) {
        return selectList(new LambdaQueryWrapperX<DccDataRelationDO>()
                .eq(DccDataRelationDO::getProjectCodeId, projectCodeId)
                .orderByDesc(DccDataRelationDO::getId));
    }

    default List<DccDataRelationDO> selectByRegistrationCertificateId(Long registrationCertificateId) {
        return selectList(new LambdaQueryWrapperX<DccDataRelationDO>()
                .eq(DccDataRelationDO::getRegistrationCertificateId, registrationCertificateId)
                .orderByDesc(DccDataRelationDO::getId));
    }
}
