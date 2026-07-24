package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder
public class MesProBatchRecordParseCommand {

    private Path sourceFilePath;

    private String originalFileName;

    private String sourceExtension;
}
