# Verification Report

## Result

PASS。真实页面 E2E 已确认同一订单、同一产品、同一工序下，首检与巡检设备展示差异来自发布 QA 规程项目级设备绑定差异，不是前端随机隐藏。

## Evidence

- Command: `node --check doc\tasks\20260808-pqc-first-patrol-equipment-diagnosis\pqc-first-patrol-equipment-diagnosis.e2e.cjs` -> PASS。
- Command: `node doc\tasks\20260808-pqc-first-patrol-equipment-diagnosis\pqc-first-patrol-equipment-diagnosis.e2e.cjs` -> PASS。
- Result JSON: `output\playwright\20260808-pqc-first-patrol-equipment-diagnosis\pqc-first-patrol-equipment-diagnosis.json`。
- Screenshot: `output\playwright\20260808-pqc-first-patrol-equipment-diagnosis\FIRST-RRM-PPV21-QA-004-RP928613.png`。
- Screenshot: `output\playwright\20260808-pqc-first-patrol-equipment-diagnosis\PATROL-RRM-PPV21-QA-005-RP928613.png`。

## Target Data

- Actor: `芋道源码/admin`。
- Order: `workOrderId=923889`, `workOrderCode=881MO090889`, `routeId=922119`, product `球囊扩张压力泵`。
- Process: `routeProcessId=980649`, `processId=922989`, `processName=组装Ⅰ工序`。
- FIRST: `pqcTaskId=304`, `regulationVersionId=43`, item `RRM-PPV21-QA-004-RP928613 / 组装I-外观-首检`, `equipmentRequired=true`, `equipmentOptionCount=0`, UI expected no equipment cards.
- PATROL: `pqcTaskId=305`, `regulationVersionId=43`, item `RRM-PPV21-QA-005-RP928613 / 组装I-外观-抽检`, `equipmentRequired=true`, `equipmentOptionCount=1`, device `球囊成型机 / A03190`, UI expected equipment cards.

## Root Cause

后端按发布 QA 规程版本中的 `inspectionType + itemCode` 聚合项目设备选项，再只取当前任务检验类型的项目。因此首检项目和巡检项目即使在同一个产品、同一个工序，也不会自动共享设备绑定。

## Request Evidence

- `submitWriteRequests=[]`，未提交 PQC 检验结果。
- `pageErrors=[]`。
- `targetFailures=[]`。
- `targetBadResponses=[]`。
- 页面初始化触发 `POST /admin-api/mes/pro/feedback/frontline/device-account/pqc/switch-employee` 上下文请求，已作为非提交业务请求记录。
