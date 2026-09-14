package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

public interface MesProductionReleaseBatchExecutionPort {

    Long openOrCreate(MesProductionReleaseBatchExecutionCommand command);
}
