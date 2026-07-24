package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProBatchRecordDocumentFrame {

    @Builder.Default
    private List<List<MesProBatchRecordParsedCell>> headerRows = List.of();

    @Builder.Default
    private List<List<MesProBatchRecordParsedCell>> footerRows = List.of();
}
