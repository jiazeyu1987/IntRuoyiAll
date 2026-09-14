# Database Fixture Evidence：批次执行真实 E2E

## Safety Analysis

- 本次只修改本机开发库中的一条 E2E 工作任务责任人，不修改 schema、索引、迁移或远端数据。
- 写入使用完整 WHERE guard，要求原责任人、租户、任务类型、状态和 deleted 标志全部匹配。
- 密码不写入脚本、证据、命令日志或提交内容。
- 失败时按 fail-fast 处理，不使用 mock、API-only 或默认成功替代。

## Data Change Goal

让 `edhr-batch-execution-real-flow.e2e.js` 不再依赖工单、批次、填写值或签名密码环境变量；脚本从本机数据库读取授权夹具，并在用户授权的 `芋道源码/admin` 下执行真实页面路径。

## Database Engine And Scope

- Engine：MySQL，容器 `int-ruoyi-mysql`，库 `ruoyi-vue-pro`。
- Scope：本机开发库，tenant_id `1`，账号 `admin`，不访问远端或生产环境。
- Affected entities：`system_tenant`、`system_users`、`mes_pro_edhr_batch_execution`、`mes_pro_edhr_batch_execution_task`、`mes_pro_edhr_work_task`。

## Data Change

- Schema/migration/index：无结构变更。
- Fixture write：`mes_pro_edhr_work_task.id=1139`，`assignee_user_id` 从 `810` 更新为 `1`，`updater='codex-e2e'`。
- Guard：`WHERE id=1139 AND tenant_id=1 AND assignee_user_id=810 AND task_type='FILL' AND status='TODO' AND deleted=b'0'`。
- Affected rows：1。
- Verification row：`1139 / batch_task_id=3394 / assignee_user_id=1 / status=TODO`。

## Migration Verification

- Migration：无迁移文件、无 schema 变更、无索引或约束变更。
- Schema verification：已核对 system_tenant、system_users、mes_pro_edhr_batch_execution、mes_pro_edhr_batch_execution_task、mes_pro_edhr_work_task 表结构和目标字段。
- Data verification：写入后只读核验 mes_pro_edhr_work_task.id=1139 的 assignee_user_id=1、status=TODO。

## Rollback Or Recovery

如需撤销本次本地 E2E 夹具写入，可执行受控回滚：

```sql
UPDATE mes_pro_edhr_work_task
SET assignee_user_id = 810,
    updater = 'codex-e2e-rollback',
    update_time = NOW()
WHERE id = 1139
  AND tenant_id = 1
  AND assignee_user_id = 1
  AND updater = 'codex-e2e'
  AND task_type = 'FILL'
  AND status = 'TODO'
  AND deleted = b'0';
```

## BDD Scenarios

- BDD: 数据库夹具发现 -> Given 本机数据库存在授权租户、账号和可打开批次工作任务 / When 执行真实 E2E / Then 脚本从数据库读取批次、任务和执行 ID，不要求人工注入业务环境变量。
- BDD: 责任人夹具写入 -> Given 当前授权账号没有待办填写任务 / When 用户授权在本地数据库准备夹具 / Then 只更新一条受控工作任务责任人，并记录原值和回滚 SQL。
- BDD: 真实页面打开填写 -> Given 授权账号是待办填写责任人 / When Playwright 从批次详情点击打开填写 / Then `/task/open` 返回成功并进入 eDHR 执行页。

## RED / GREEN Evidence

- RED: `node tests\e2e\edhr-batch-execution-filler-entry-static.spec.js` -> FAIL，真实 E2E 仍缺少 `queryLocalDatabase` / `resolveDatabaseFixture`。
- RED: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> FAIL，授权账号无可打开待办任务，详情接口 `allowedActions=[]`。
- GREEN: DB fixture update -> PASS，受控 SQL 影响 1 行，工作任务 `1139` 指派给 admin。
- GREEN: `node tests\e2e\edhr-batch-execution-filler-entry-static.spec.js` -> PASS。
- GREEN: `node --check tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS，证据见 `real-e2e-evidence.md`。

## Blockers

- 无当前授权范围内数据库阻塞。
- 注意：不得把本地夹具 SQL 推广到远端或生产；如需远端真实 E2E，需要单独授权并重新记录数据安全证据。