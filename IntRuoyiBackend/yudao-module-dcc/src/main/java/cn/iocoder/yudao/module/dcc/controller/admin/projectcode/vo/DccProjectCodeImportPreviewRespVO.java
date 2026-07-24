package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectCodeImportPreviewRespVO {

    private Long batchId;
    private String status;
    private Integer totalCount;
    private Integer createCount;
    private Integer updateCount;
    private Integer disableCount;
    private Integer unchangedCount;
    private Integer failureCount;
    private List<DccProjectCodeImportRowRespVO> rows;
}
