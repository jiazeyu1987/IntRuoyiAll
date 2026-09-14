package cn.iocoder.yudao.module.dcc.service.relation;

import cn.iocoder.yudao.module.dcc.controller.admin.relation.vo.DccDataRelationCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.relation.DccDataRelationDO;

import java.util.List;

public interface DccDataRelationService {

    DccDataRelationDO createRelation(Long userId, DccDataRelationCreateReqVO reqVO);

    List<DccDataRelationDO> getByProductCatalogId(Long productCatalogId);

    List<DccDataRelationDO> getByProjectCodeId(Long projectCodeId);

    List<DccDataRelationDO> getByRegistrationCertificateId(Long registrationCertificateId);

    void deleteRelation(Long id);
}
