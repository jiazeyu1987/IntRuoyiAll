package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * MES 工作站 Service 接口
 */
public interface MesMdWorkstationService {

    Long createWorkstation(@Valid MesMdWorkstationSaveReqVO createReqVO);

    void updateWorkstation(@Valid MesMdWorkstationSaveReqVO updateReqVO);

    void deleteWorkstation(Long id);

    MesMdWorkstationDO validateWorkstationExists(Long id);

    MesMdWorkstationDO validateWorkstationExistsAndEnable(Long id);

    MesMdWorkstationDO getWorkstation(Long id);

    PageResult<MesMdWorkstationDO> getWorkstationPage(MesMdWorkstationPageReqVO pageReqVO);

    List<MesMdWorkstationDO> getWorkstationListByStatus(Integer status);

    Long getWorkstationCountByWarehouseId(Long warehouseId);

    Long getWorkstationCountByLocationId(Long locationId);

    Long getWorkstationCountByAreaId(Long areaId);

    Long getWorkstationCountByWorkshopId(Long workshopId);

    List<MesMdWorkstationDO> getWorkstationList(Collection<Long> ids);

    List<MesMdWorkstationDO> getWorkstationListByProcessIds(Collection<Long> processIds);

    List<MesMdWorkstationDO> getWorkstationListByProcessIds(Collection<Long> processIds, Integer status);

    default Map<Long, MesMdWorkstationDO> getWorkstationMap(Collection<Long> ids) {
        return convertMap(getWorkstationList(ids), MesMdWorkstationDO::getId);
    }
}
