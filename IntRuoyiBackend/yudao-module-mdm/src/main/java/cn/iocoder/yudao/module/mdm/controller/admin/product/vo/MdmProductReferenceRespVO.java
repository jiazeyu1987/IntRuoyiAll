package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductReferenceRespVO {

    private Long productId;
    private Long dccReferenceCount;
    private Long showroomReferenceCount;

}
