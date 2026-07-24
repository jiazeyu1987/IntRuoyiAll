package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;
import lombok.Data;

@Data
public class DccNasPrincipalMappingRespVO {

    private Long id;
    private String sourceSid;
    private String targetSubjectType;
    private Long targetSubjectId;
    private Boolean active;

    public static DccNasPrincipalMappingRespVO of(DccNasAclIdentityMappingDO mapping) {
        DccNasPrincipalMappingRespVO respVO = new DccNasPrincipalMappingRespVO();
        respVO.setId(mapping.getId());
        respVO.setSourceSid(mapping.getSid());
        respVO.setTargetSubjectType(mapping.getDccSubjectType());
        respVO.setTargetSubjectId(mapping.getDccSubjectId());
        respVO.setActive("MAPPED".equals(mapping.getMappingStatus()));
        return respVO;
    }
}
