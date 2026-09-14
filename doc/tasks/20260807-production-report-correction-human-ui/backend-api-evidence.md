# Backend API Evidence

## Scope

- Add a read-only production-report revision log endpoint and query service.
- Reuse persisted revision and diff records; do not add schema or derive audit identity from the client.

## Contract

- `GET /mes/pro/process-pool/event-revision/production-report-logs?eventId=<id>`
- Response items expose only `modifiedByName`, `modifiedAt`, `changeReason`, `signatureConfirmed`, and readable `changes`.
- Response must not expose payloads, signature snapshots, technical IDs, or field codes.

## Auth And Validation

- Permission: `mes:pro-process-pool-team-leader:query`；日志读取不要求修改权限，写接口仍使用独立修改权限。
- Actor: current login user from the controller.
- Scope: production team-leader employee scope against the event's actual employee.
- Invalid event, non-production event, malformed signature evidence, or malformed legacy structured difference fails fast.

## Data And Migration

- Source: existing `mes_pro_process_pool_event_revision` and `mes_pro_process_pool_event_revision_diff`.
- Migration: none.

## BDD

- Given effective revisions and diffs / When an authorized leader queries / Then return latest first with snapshot actor name and readable values.
- Given another team's event / When queried / Then reject before returning revision contents.
- Given malformed persisted signature evidence / When queried / Then fail explicitly instead of inventing a display identity.

## RED / GREEN

- RED: `mvn -pl yudao-module-mes '-Dtest=MesProcessPoolProductionReportRevisionLogServiceTest,MesProcessPoolProductionReportRevisionLogContractTest' test` -> FAIL，缺少查询服务和响应 VO。
- GREEN: pending.

## Observability

- Existing request and service exception logging applies; no exception swallowing or empty-list downgrade.

## Blockers

- None identified.
