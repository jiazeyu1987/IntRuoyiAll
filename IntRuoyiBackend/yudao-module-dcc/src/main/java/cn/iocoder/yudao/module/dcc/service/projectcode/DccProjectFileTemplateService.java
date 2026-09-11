package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateSaveReqVO;

public interface DccProjectFileTemplateService {

    DccProjectFileTemplateRespVO getProjectTemplate(Long projectCodeId);

    DccProjectFileTemplateRespVO replaceProjectTemplate(Long projectCodeId,
                                                        DccProjectFileTemplateSaveReqVO reqVO);

    void validateUploadSelection(Long projectCodeId, Long fileTypeTaxonomyId, String fileName);
}
