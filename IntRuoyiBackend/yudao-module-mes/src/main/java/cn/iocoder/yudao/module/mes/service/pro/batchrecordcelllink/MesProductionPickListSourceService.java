package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import java.util.List;

public interface MesProductionPickListSourceService {

    String SOURCE_TYPE = "PRODUCTION_PICK_LIST";
    String SOURCE_REPORT_ID = "PRODUCTION_PICK_LIST";
    String SOURCE_REPORT_NAME = "领料单数据";

    List<SourceField> listSourceFields(Long routeId);

    ResolvedValue resolveValue(ResolveCommand command);

    record SourceField(String fieldCode, String fieldName, String valueType, Long routeProcessId) {
    }

    record ResolveCommand(Long routeId, Long routeProcessId, Long productId, Long dccProjectCodeId,
                          Long pickListBindingId, String productionOrderNo, String sourceFieldCode) {
    }

    record ResolvedValue(Long pickListId, Long pickListItemId, Object value, String evidenceHash) {
    }
}
