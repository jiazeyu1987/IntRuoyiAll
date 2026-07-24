package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public interface MesProBatchRecordExecutionArchiveRenderer {

    String getArtifactType();

    MesProBatchRecordExecutionArchiveRenderResult render(MesProBatchRecordExecutionArchiveRenderContext context);
}
