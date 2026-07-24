package cn.iocoder.yudao.module.srm.service.contract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractCancelReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractSaveReqVO;
import jakarta.validation.Valid;

public interface SrmProcurementContractService {

    SrmProcurementContractRespVO createContract(@Valid SrmProcurementContractSaveReqVO createReqVO);

    void cancelContract(@Valid SrmProcurementContractCancelReqVO cancelReqVO);

    void deleteContract(Long id);

    SrmProcurementContractRespVO getContract(Long id);

    PageResult<SrmProcurementContractRespVO> getContractPage(SrmProcurementContractPageReqVO pageReqVO);
}
