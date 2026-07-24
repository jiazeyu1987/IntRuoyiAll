# QA Evidence：NAS 权限转移恢复真实数据 E2E

## Scope

NAS 管理中的 DCC 转移、NAS 权限快照、身份映射、恢复预览、应用恢复和恢复状态轮询。

## Requirement-To-Test Matrix

| Requirement | Evidence |
| --- | --- |
| 测试租户使用真实前端路径完成写数据验证 | Covered: `node tests/e2e/dcc-nas-permission-real-data.e2e.js --mode=test-write` -> `PASS: test-write taskId=42, restoreId=12, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0` |
| 芋道源码 admin 只读验证，不修改源码租户数据 | PASS: `PASS: admin-readonly baseUrl=http://172.30.30.58:8081`；只读 guard 未发现写请求 |
| 缺失安全 NAS 路径、接口或权限时 fail fast | Covered: 缺少 `--mode`、写入确认、安全路径、NAS 菜单权限、启用 `其他` 均 fail fast |
| 不使用 mock 或 API 绕过前端主流程 | Covered in script design: Playwright 操作真实前端；API 仅用于登录后最终规则校验与接口响应证据读取 |
| 测试写入不得误连正式后端 | Covered: `test-write` 使用 `NAS_PERMISSION_E2E_TEST_API_ORIGIN` 校验并 abort 非预期 `/admin-api` 请求 |
| 转移提交携带真实 `其他` ID | Covered in script assertion: POST `/dcc/controlled-files/nas-transfer` 的 `templateCategoryId` 必须等于类别接口返回的启用 `其他` ID |
| 权限快照列表可见 | Covered: test-write 真实路径已通过快照、items、映射、预览、应用恢复和最终 DCC 目录权限校验 |
| 恢复任务后台执行 | Covered: `DccNasPermissionRestoreExecutionSchedulerTest` 和真实恢复 `restoreId=10/12`；调度器异常路径已改为 fail-fast |
| DCC 目录访问规则恢复后可读取 | Covered: `DccDirectoryControllerTest` 和 test-write 最终规则校验 |
| 身份映射保存分支 | Covered: `PASS: test-mapping taskId=39, restoreId=10, directories=2, rules=47, unmapped=1, savedMappings=1, blockers=0` |
| blocker UI 与应用恢复禁用 | Covered: `PASS: test-blocker taskId=41, unmapped=0, blockers=1`，并安装写请求 guard 确认未提交恢复 |

## Test Data And Fixtures

- 测试租户：`测试租户` / `aoteman`。
- 只读验证租户：`芋道源码` / `admin`。
- 测试租户安全 NAS 路径：`NAS_PERMISSION_E2E_TEST_NAS_PATH=9. 其他`，禁止默认选择大目录。

## RED Evidence

- RED: coverage audit -> FAIL，当前仅有静态源码断言，没有真实浏览器 E2E。
- RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js` -> FAIL，缺少 `--mode` 时 fail fast。
- RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> FAIL，未设置测试租户写入确认变量。
- RED: `$env:NAS_PERMISSION_E2E_ALLOW_TEST_WRITE='1'; node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> FAIL，缺少安全 NAS 路径 `NAS_PERMISSION_E2E_TEST_NAS_PATH`。
- RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> FAIL，芋道源码/admin 真实 DCC 类别接口缺少启用的 `其他`。
- RED: 测试环境只读登录探测 -> FAIL，`http://172.30.30.58:8081` 登录页历史选项没有 `测试租户`；已修正脚本使用真实 `allow-create` 租户输入。
- RED: 测试环境只读 NAS 页面探测 -> FAIL，`测试租户/aoteman` 能登录，但 `/system/nas` 返回 404；缺失测试租户 NAS 管理菜单/路由权限或测试环境菜单部署。
- RED: 芋道源码/admin 只读复验 -> FAIL，2026-05-27 复验仍缺少启用的 DCC 类别 `其他`。
- RED: 独立子 agent 覆盖审查 -> FAIL，真实写入前缺少测试写入 API origin guard、转移提交真实 `其他` ID 断言、权限快照 items 列表断言。
- RED: 测试环境只读 NAS 页面复验 -> FAIL，加固后仍为 `/system/nas` 404。
- RED: 芋道源码/admin 只读复验 -> FAIL，加固后仍为缺少启用的 DCC 类别 `其他`。
- RED: `NasPermissionRestorePanel.vue` 直接使用 `crypto.randomUUID()` -> FAIL，HTTP 测试环境应用恢复按钮报 `crypto.randomUUID is not a function`。
- RED: NAS ACL 恢复表结构 -> FAIL，真实版本值超过 `varchar(32)`，真实 `sha256:` 前缀 hash 超过 `char(64)`。
- RED: 恢复调度器缺失 -> FAIL，恢复计划进入 `READY` 后不会自动执行。
- RED: DCC 目录访问规则接口读取恢复结果 -> FAIL，`subjectType=USER` 转换为 `Integer` 时 500。
- RED: 前端目录访问规则下拉仍使用数字主体类型 -> FAIL，无法与恢复写入的字符串主体类型保持契约一致。
- RED: 恢复调度器 catch-and-continue -> FAIL，独立 reviewer 指出该行为违背本任务 no-fallback 口径；新增 fail-fast 单测后失败。
- RED: 身份映射分支真实 E2E -> FAIL，保存未映射 SID 时后端唯一键 `uk_dcc_nas_acl_identity_sid` 冲突，证明 `INACTIVE` 同 SID 映射未被重新激活。
- RED: blocker fixture 共享 descriptor 污染 -> FAIL，旧 fixture 直接更新 `dcc_nas_acl_ace`，导致后续 `test-write` 捕获到 3 条 `DCC_NAS_ACL_DENY_UNSUPPORTED`。
- RED: subagent Pauli 前端复核 -> FAIL，指出 `test-mapping` 直接修改共享 `dcc_nas_acl_identity_mapping` 存在失败后污染风险。
- RED: 克隆式 mapping fixture SQL -> FAIL，合成 SID hash 变量 collation 与表字段不一致，MySQL 报 `Illegal mix of collations`。
- RED: 克隆式 mapping fixture hash 大小写 -> FAIL，ACE 使用 MySQL `SHA2()` 小写 hash，而后端保存映射使用大写 hash，恢复预览仍报 `DCC_NAS_PRINCIPAL_UNMAPPED`。

## GREEN Evidence

- GREEN: `pnpm add -D playwright@1.60.0 --ignore-scripts` -> PASS，测试运行时依赖已声明。
- GREEN: `node --check tests\e2e\dcc-nas-permission-real-data.e2e.js` -> PASS。
- GREEN: `pnpm exec eslint tests\e2e\dcc-nas-permission-real-data.e2e.js` -> PASS。
- GREEN: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> PASS。
- GREEN: `pnpm exec eslint src\api\dcc\controlledFile\workflow.ts src\views\system\nas\index.vue src\views\system\nas\components\NasPermissionRestorePanel.vue tests\e2e\dcc-nas-permission-restore-static.spec.js tests\e2e\dcc-nas-permission-real-data.e2e.js` -> PASS。
- GREEN: 真实写入前门禁加固静态验证 -> PASS，`test-write` API origin guard、转移 POST `templateCategoryId` 断言、权限快照 items 断言已实现并通过 `node --check`、ESLint、静态断言和 QA evidence 校验。
- GREEN: 测试租户只读 NAS 页面探测 -> PASS，测试租户能打开真实 `/system/nas`。
- GREEN: 测试租户真实写路径最终复验 -> PASS，`PASS: test-write taskId=42, restoreId=12, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。
- GREEN: 恢复面板 UUID 静态断言 -> PASS，应用恢复幂等键使用项目 `generateUUID()`。
- GREEN: SQL 断言 -> PASS，`python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py script\tests\test_dcc_directory_access_rule_subject_type_contract_sql.py -q`，共 3 个测试通过。
- GREEN: 后端目标回归 -> PASS，相关 80 个测试通过。
- GREEN: 恢复调度器 no-fallback -> PASS，异常直接抛出并停止本次调度。
- GREEN: 测试租户完整写路径 -> PASS，`PASS: test-write taskId=42, restoreId=12, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。
- GREEN: 测试租户只读路径 -> PASS，`PASS: admin-readonly baseUrl=http://172.30.30.58:8081`。
- GREEN: 测试租户身份映射保存分支 -> PASS，`PASS: test-mapping taskId=39, restoreId=10, directories=2, rules=47, unmapped=1, savedMappings=1, blockers=0`。
- GREEN: 测试租户 blocker/禁用恢复分支 -> PASS，`PASS: test-blocker taskId=41, unmapped=0, blockers=1`。
- GREEN: mapping fixture 非污染复验 -> PASS，`inactive_old_mapping_fixture_rows=0`，task `39` 含任务专属合成 SID，后续 task `40/42` 未出现合成 SID 且 `blockers=0`。
- GREEN: 芋道源码/admin 只读路径 -> PASS，`PASS: admin-readonly baseUrl=http://172.30.30.58:8081`。

## Verification

- 新增真实浏览器 E2E 脚本：`tests/e2e/dcc-nas-permission-real-data.e2e.js`。
- 运行模式：
  - `--mode=test-write`：测试租户写路径，要求显式安全路径和写入确认。
  - `--mode=test-mapping`：测试租户写路径后注入当前任务未映射 SID，并从 UI 保存真实 DCC 主体映射后应用恢复。
  - `--mode=test-blocker`：测试租户写路径后克隆当前任务 ACL descriptor 注入 DENY blocker，验证 UI 禁用应用恢复且不提交写请求。
  - `--mode=admin-readonly`：只读验证，默认用于芋道源码/admin；也可通过 `NAS_PERMISSION_E2E_ADMIN_*` 环境变量验证测试租户只读路径，禁止 DCC/NAS 权限恢复写请求。
  - `--mode=all`：依次跑测试租户 `test-write`、`test-mapping`、`test-blocker`，再跑芋道源码/admin 只读验证。

## Blockers

- 无。此前芋道源码/admin 缺少启用的 DCC 类别 `其他`，已由后端运行库补齐任务解除并通过只读 E2E。

## Release Recommendation

- GO for code merge into integration branch: 测试租户完整写路径、测试租户只读路径、芋道源码/admin 只读路径、静态检查、构建、SQL 断言和后端目标回归均已通过。
- GO for this E2E/data gate: 发布前仍需按常规发布清单复核目标环境和待合并 diff。
