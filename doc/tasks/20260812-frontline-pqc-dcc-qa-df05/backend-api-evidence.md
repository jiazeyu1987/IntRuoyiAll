# Backend API Evidence - DF05

## Scope

MesQaInspectionRegulationServiceImpl save/publish behavior only where needed for DCC direct QA relation.

## Contract

- Input relation: dccProjectCodeId.
- Forbidden identities: productId, routeId, routeVersionId, routeProcessId, processId.
- Failure behavior: missing or invalid DCC/QA/version data fails fast through existing service validation.

## BDD

- BDD: 后端拒绝旧推算身份 -> Given save/publish请求携带DCC ID和QA完整业务字段, When 服务端保存或发布, Then regulation.dccProjectCodeId是唯一关系来源，服务不读取productId/routeId/routeProcessId/processId来推算QA归属。

## Verification

- RED: pending
- GREEN: pending

## Validation

- pending

## Blockers

- none

