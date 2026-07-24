package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;

import java.util.List;

@Data
public class MesProBatchRecordParseResult {

    private String sourceFileName;

    private String sourceExtension;

    private Integer tableCount;

    private List<MesProBatchRecordParseTable> tables;
}
