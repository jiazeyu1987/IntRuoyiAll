package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomyRespVO;
import lombok.Data;

import java.util.List;

@Data
public class DccProjectFileTemplateRespVO {

    private Long projectCodeId;
    private List<DccFileTypeTaxonomyRespVO> taxonomyOptions;
    private List<DccProjectFileTemplateItemRespVO> items;
}
