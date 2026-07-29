package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolQuantityFragmentCreateDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class MesProcessPoolSubmitEventServiceImpl implements MesProcessPoolSubmitEventService {

    private static final String FEEDBACK_SOURCE_TYPE = "MES_PRO_FEEDBACK";
    private static final String RECORDBOOK_SOURCE_TYPE = "MES_PRO_EDHR_RECORD_BOOK_EVENT";
    private static final String QUANTITY_TYPE_OUTPUT = "OUTPUT";
    private static final String QUANTITY_TYPE_LOSS = "LOSS";

    private final MesProcessPoolEventService eventService;

    public MesProcessPoolSubmitEventServiceImpl(MesProcessPoolEventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public Long createSubmitEvent(MesProcessPoolSubmitEventCreateReqBO reqBO) {
        return eventService.createEvent(toCreateEventReq(reqBO));
    }

    private MesProcessPoolCreateEventReqDTO toCreateEventReq(MesProcessPoolSubmitEventCreateReqBO reqBO) {
        if (reqBO == null) {
            return null;
        }
        return MesProcessPoolCreateEventReqDTO.builder()
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(reqBO.getWorkOrderId())
                .routeId(reqBO.getRouteId())
                .routeProcessId(reqBO.getRouteProcessId())
                .processId(reqBO.getProcessId())
                .actualEmployeeId(reqBO.getActualEmployeeId())
                .deviceAccountId(reqBO.getDeviceAccountUserId())
                .deviceId(reqBO.getDeviceId())
                .workstationId(reqBO.getWorkstationId())
                .templateType(reqBO.getTemplateType())
                .feedbackSourceType(FEEDBACK_SOURCE_TYPE)
                .feedbackSourceId(reqBO.getFeedbackId())
                .recordbookSourceType(RECORDBOOK_SOURCE_TYPE)
                .recordbookSourceId(reqBO.getRecordbookEventId())
                .rawPayload(toJsonOrNull(reqBO.getRawPayload()))
                .clientSubmitTime(reqBO.getSubmittedAt())
                .signatureId(reqBO.getSignatureId())
                .signatureUserId(reqBO.getSignatureEmployeeId())
                .quantityFragments(buildQuantityFragments(reqBO))
                .build();
    }

    private List<MesProcessPoolQuantityFragmentCreateDTO> buildQuantityFragments(
            MesProcessPoolSubmitEventCreateReqBO reqBO) {
        List<MesProcessPoolQuantityFragmentCreateDTO> fragments = new ArrayList<>();
        addQuantityFragment(fragments, QUANTITY_TYPE_OUTPUT, reqBO.getOutputQuantity(), reqBO);
        addQuantityFragment(fragments, QUANTITY_TYPE_LOSS, reqBO.getLossQuantity(), reqBO);
        return fragments;
    }

    private void addQuantityFragment(List<MesProcessPoolQuantityFragmentCreateDTO> fragments,
                                     String sourceQuantityType,
                                     BigDecimal quantity,
                                     MesProcessPoolSubmitEventCreateReqBO reqBO) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Map<String, Object> fragmentPayload = new LinkedHashMap<>();
        fragmentPayload.put("sourceQuantityType", sourceQuantityType);
        fragmentPayload.put("feedbackId", reqBO.getFeedbackId());
        fragmentPayload.put("recordbookEntryId", reqBO.getRecordbookEntryId());
        fragmentPayload.put("recordbookEventId", reqBO.getRecordbookEventId());
        fragmentPayload.put("previousProcessInputQuantity", reqBO.getPreviousProcessInputQuantity());
        fragmentPayload.put("equipmentParameters", reqBO.getEquipmentParameters());
        fragments.add(MesProcessPoolQuantityFragmentCreateDTO.builder()
                .sourceQuantityType(sourceQuantityType)
                .qualityStatus(sourceQuantityType)
                .totalQuantity(quantity)
                .rawPayload(JsonUtils.toJsonString(fragmentPayload))
                .build());
    }

    private String toJsonOrNull(Map<String, Object> payload) {
        return payload == null ? null : JsonUtils.toJsonString(payload);
    }
}
