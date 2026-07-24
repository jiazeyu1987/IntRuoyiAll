package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public interface MesProBatchRecordTemplateParser {

    boolean supports(String extension);

    MesProBatchRecordParseResult parse(MesProBatchRecordParseCommand command);
}
