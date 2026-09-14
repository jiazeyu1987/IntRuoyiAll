package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.file.DccPublicationImpactRevisionOption;
import lombok.Data;

@Data
public class DccPublicationImpactRevisionOptionRespVO {
    private String controlledFileId;
    private String versionNo;
    private String status;
    private Boolean currentActive;

    public static DccPublicationImpactRevisionOptionRespVO from(DccPublicationImpactRevisionOption source) {
        if (source == null) return null;
        DccPublicationImpactRevisionOptionRespVO vo = new DccPublicationImpactRevisionOptionRespVO();
        vo.setControlledFileId(String.valueOf(source.controlledFileId()));
        vo.setVersionNo(source.versionNo());
        vo.setStatus(source.status());
        vo.setCurrentActive(source.currentActive());
        return vo;
    }
}
