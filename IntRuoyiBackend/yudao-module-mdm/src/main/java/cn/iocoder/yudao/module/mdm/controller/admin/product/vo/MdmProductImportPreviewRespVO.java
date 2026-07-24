package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductImportPreviewRespVO {

    private Long batchId;
    private String status;
    private Integer totalCount;
    private Integer createCount;
    private Integer updateCount;
    private Integer disableCount;
    private Integer unchangedCount;
    private Integer failureCount;
    private List<MdmProductImportRowRespVO> rows;

}
