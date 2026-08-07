package cn.iocoder.yudao.module.mes.service.pro.processpool;

public interface MesProcessPoolEventRevisionService {

    Long updateOriginalRecord(MesProcessPoolEventRevisionUpdateReqBO reqBO);

    Long updateProductionReportRecord(MesProcessPoolEventRevisionUpdateReqBO reqBO);
}
