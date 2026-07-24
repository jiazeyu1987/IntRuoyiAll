# 任务：NAS 权限转移恢复真实数据 E2E

## 任务目标

- 为 NAS 管理中的“转移到 DCC”与 NAS 权限快照/恢复能力补齐真实数据 E2E 用例。
- 在测试租户中执行会写数据的真实路径验证，包括选择 NAS 文件夹、转移、快照、身份映射、恢复预览、应用恢复和恢复状态轮询。
- 在芋道源码租户 admin 上只做验证，不修改源码租户数据；若验证失败，应回到测试租户修复后再复验。
- E2E 必须使用 Playwright 操作前端真实路径，API 仅用于登录后的最终校验和证据采集。

## 前序任务检查

- 前端上一同仓任务：`doc/tasks/20260526-nas-permission-snapshot-restore-implementation/`
- 状态：completed
- 已知缺口：上一任务仅完成代码级验证，真实路径 E2E 仍依赖测试服部署与真实 NAS ACL 测试数据。

## BDD 场景

- BDD: 测试租户完整 NAS 权限恢复 happy path -> Given 测试租户存在可安全转移的小型真实 NAS 文件夹和启用的 DCC 类别 `其他` / When 用户在 `NAS管理` 选择该文件夹并转移到 DCC / Then 系统创建真实转移任务，完成后展示权限快照、恢复预览，并允许显式应用恢复。
- BDD: 测试租户缺失前置条件 fail fast -> Given 测试租户缺少安全 NAS 路径、启用的 `其他`、权限管理权限或后端新接口 / When E2E 启动 / Then 用例必须失败并报告缺失前置条件，不得改用芋道源码租户或 mock 数据。
- BDD: 测试租户阻断路径可见 -> Given 快照存在未映射主体、DENY ACE、不支持权限或 ACL 采集失败 / When 用户打开权限恢复抽屉 / Then UI 必须展示真实阻断原因，并禁止应用恢复。
- BDD: 芋道源码 admin 只读验证 -> Given 芋道源码租户 admin 登录正式或指定验证环境 / When 打开 NAS 管理并检查转移弹窗和权限恢复入口 / Then 页面必须加载真实接口、默认类别规则仍为 `其他`，且用例不得提交转移、保存映射或应用恢复。

## 功能点到 E2E 覆盖矩阵

| 功能点 | 是否已有 E2E 用例 | 当前真实验证状态 |
| --- | --- | --- |
| 登录与租户选择 | 有，`admin-readonly` 和 `test-write` 均覆盖真实登录控件 | 测试租户写路径 PASS；测试租户只读 PASS；芋道源码/admin 只读 PASS |
| NAS 配置读取与连接测试 | 有，覆盖连接测试接口响应 | 测试租户写路径 PASS，包含真实连接测试 |
| NAS 目录刷新与真实文件夹选择 | 有，覆盖刷新根目录、展开子目录、选择显式 NAS 路径 | 测试租户写路径 PASS，安全路径 `9. 其他` |
| 转移弹窗默认模板类别 `其他` | 有，覆盖类别接口、默认选中和提交真实 `templateCategoryId` | 测试租户 PASS；芋道源码/admin 数据补齐后 PASS |
| 转移确认与任务创建 | 有，覆盖真实确认和 POST 请求体断言 | 测试租户 PASS，最新任务 `42` 完成 |
| 转移任务轮询完成/失败展示 | 有，覆盖任务状态轮询和失败 fail fast | 测试租户 PASS，最新任务 `42` 完成 |
| 权限快照摘要与目录行 | 有，脚本等待快照摘要和 items 列表 | 测试租户 PASS，恢复链路已读取快照并进入恢复 |
| 未映射主体列表与身份映射 | 有，脚本按真实未映射主体保存映射；无未映射时记录无阻断证据 | 测试租户专用分支 PASS，`test-mapping taskId=39, restoreId=10, unmapped=1, savedMappings=1, blockers=0` |
| 恢复预览与阻断项 | 有，脚本覆盖预览接口、`planHash`、阻断项 | 测试租户专用分支 PASS，`test-blocker taskId=41, unmapped=0, blockers=1`，且未提交恢复写请求 |
| 应用恢复与状态轮询 | 有，脚本覆盖显式应用、恢复 ID、状态轮询 | 测试租户 PASS，最新恢复 `restoreId=12` 完成 |
| 恢复后 DCC 权限生效 | 有，脚本用 API 做恢复后的最终规则校验 | 测试租户 PASS，最新 `directories=2`、`rules=47` |
| 芋道源码 admin 只读防写 | 有，脚本安装写请求拦截并断言无写操作 | 测试租户只读 PASS；芋道源码/admin 只读 PASS |

## 里程碑

- [x] M1：建立任务文档并确认前序任务缺口。
- [x] M2：新增真实数据 Playwright E2E 脚本，先以缺失部署/数据作为 RED。
- [x] M3：运行测试租户写路径验证，记录真实数据结果或阻塞项。
- [x] M4：运行芋道源码 admin 只读验证，记录结果。
- [x] M5：根据失败点回到测试租户修复并复验。
- [x] M6：更新 QA evidence 和收尾状态。

## Subagent-Driven Review

- Gate1 文档/覆盖审查：Pauli 只读审查前端 E2E、fixture 与文档，结论为 conditional go；指出 `test-mapping` 直接修改共享 identity mapping 有污染风险、admin 只读未 GREEN、subagent 记录不足。
- Gate2 后端逻辑审查：Archimedes 只读审查后端映射修复与证据，结论为 conditional go；确认 `INACTIVE` SID 重新激活逻辑自洽，release 阻塞为芋道源码/admin 缺少启用的 `其他`。
- 主 reviewer 处理：已将 `test-mapping` 改为克隆当前任务 descriptor/ACE 并使用任务专属合成 SID，不再修改共享 identity mapping；最终 `test-mapping`、`test-blocker` 与后续 `test-write` 均复验通过。

## 预期验证

- RED：未提供测试租户安全 NAS 路径或测试服未部署当前接口时，E2E fail fast。
- GREEN：`node tests/e2e/dcc-nas-permission-real-data.e2e.js --mode=test-write` -> PASS。
- GREEN：`node tests/e2e/dcc-nas-permission-real-data.e2e.js --mode=test-mapping` -> PASS。
- GREEN：`node tests/e2e/dcc-nas-permission-real-data.e2e.js --mode=test-blocker` -> PASS。
- GREEN：测试租户只读配置下 `node tests/e2e/dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> PASS。
- GREEN：芋道源码/admin 只读配置下 `node tests/e2e/dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> PASS。
- GREEN：`node tests/e2e/dcc-nas-permission-restore-static.spec.js` -> PASS。
- GREEN：`pnpm exec eslint tests/e2e/dcc-nas-permission-real-data.e2e.js` -> PASS。

## 当前状态

- 状态：completed。
- 已完成：测试租户完整真实写路径已通过，最新 `test-write` 返回 `PASS: test-write taskId=42, restoreId=12, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`；身份映射保存分支返回 `PASS: test-mapping taskId=39, restoreId=10, directories=2, rules=47, unmapped=1, savedMappings=1, blockers=0`；blocker/禁用应用恢复分支返回 `PASS: test-blocker taskId=41, unmapped=0, blockers=1`；测试租户只读路径已通过；前端静态检查、ESLint、后端目标回归和 SQL 断言均通过；测试服当前运行 frontend `sha256:340b5076a00425bc5a4612ac94f5ea8d9546fe8932586b02e5963845013e4a0d` 与 backend `sha256:f07215e41d818e117835ac185b5a63c93e864882c7b18f190d42d0eac423e5c4`。
- 阻塞：无。此前芋道源码/admin 缺少启用的 DCC 类别 `其他`，已由后端运行库补齐任务解除，并完成 `admin-readonly` 复验。

## Cleanup Keep

- `doc/tasks/20260527-nas-permission-real-e2e-suite/qa-test-suite-evidence.md`
- `doc/tasks/20260527-nas-permission-real-e2e-suite/verification-report.md`
