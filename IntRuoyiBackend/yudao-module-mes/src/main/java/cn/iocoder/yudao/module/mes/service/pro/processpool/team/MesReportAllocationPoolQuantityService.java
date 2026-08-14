package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ROOT_EVENT_REQUIRED;

@Service
public class MesReportAllocationPoolQuantityService {

    public BigDecimal requirePoolQuantity(MesProProcessPoolEventDO event) {
        if (event == null || !Objects.equals(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT,
                event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ROOT_EVENT_REQUIRED,
                    event == null ? null : event.getId());
        }
        if (StrUtil.isBlank(event.getRawPayload())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
        }
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(event.getRawPayload()).get("outputQuantity");
            if (node == null || !node.isNumber() || node.decimalValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
            }
            return node.decimalValue();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
        }
    }
}
