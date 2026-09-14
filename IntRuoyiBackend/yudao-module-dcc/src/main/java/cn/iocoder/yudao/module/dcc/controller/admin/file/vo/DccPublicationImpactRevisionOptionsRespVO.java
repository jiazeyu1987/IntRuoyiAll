package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.file.DccPublicationImpactRevisionOptions;
import lombok.Data;

import java.util.List;

@Data
public class DccPublicationImpactRevisionOptionsRespVO {
    private List<DccPublicationImpactRevisionOptionRespVO> sourceIterations;
    private DccPublicationImpactRevisionOptionRespVO openMajorRevision;

    public static DccPublicationImpactRevisionOptionsRespVO from(DccPublicationImpactRevisionOptions source) {
        DccPublicationImpactRevisionOptionsRespVO vo = new DccPublicationImpactRevisionOptionsRespVO();
        vo.setSourceIterations(source.sourceIterations().stream()
                .map(DccPublicationImpactRevisionOptionRespVO::from).toList());
        vo.setOpenMajorRevision(DccPublicationImpactRevisionOptionRespVO.from(source.openMajorRevision()));
        return vo;
    }
}
