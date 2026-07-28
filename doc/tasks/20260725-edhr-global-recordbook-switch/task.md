# eDHR 金手指全局记录本开关

## Task Goal

实现 eDHR 金手指专用全局记录本开关：关闭后所有批次、所有工序、所有用户都只能走批记录流程，并隐藏批记录/记录本切换按钮。

## Milestones

- [x] 创建任务记录、完成经验门禁和脏工作区基线。
- [x] 增加全局配置 SQL、后端接口、服务和 fail-fast 校验。
- [x] 将全局开关接入批次详情、任务打开、执行详情和记录本写入防绕过链路。
- [x] 增加个人中心金手指配置页签和前端调用。
- [x] 增加后端、SQL、前端静态合同测试并运行可用验证。
- [x] 完成真实前端路径验证并恢复全局开关。
- [x] 完成收尾、经验沉淀、提交和推送。

## Expected Verification

- 后端：`mvn -pl yudao-module-mes -am test` 或定向等价命令。
- SQL：`python IntRuoyiBackend/script/tests/test_mes_edhr_recordbook_global_setting_sql.py`。
- 前端：`pnpm ts:check` 和相关 `node tests/e2e/*static.spec.js`。
- 真实 E2E：若本地运行态和授权测试账号可用，通过个人中心关闭/恢复开关并验证批次详情隐藏切换控件。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；配置缺失或非法值必须 fail fast。
- `是否从根因和长期维护角度解决`：是；通过统一全局配置服务和后端运行态门禁解决直接 URL/旧执行防绕过。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/backend-development.md#edhr-批次任务配置来源门禁`：运行态不得用发布快照或默认 MAIN 掩盖当前配置损坏；本任务只叠加全局开关，不修改路线/任务冻结来源。
- `docs/backend-development.md#edhr-批记录版本治理规则运行态门禁`：打开填写仍必须保留批记录版本治理 fail-fast；全局记录本关闭不得绕过 `CELL_RULE_RECONCILED` 等既有门禁。
- `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：前端行为变更需同步静态合同，真实 E2E 不得保留废弃路径。
- `docs/e2e-rules.md#edhr-批次执行数据库夹具与证据文件门禁`：真实写入 E2E 需确认本地数据库夹具、授权租户/账号和恢复证据；前置缺失则记录 BLOCKED。
- `docs/powershell-memory.md#任务提交推送前置门禁` 与 `#脏工作区基线门禁`：实现前必须基线提交既有脏改动，任务实现与收尾记录分开提交并推送。

## Verification Status

- PASS: SQL 合同 `python IntRuoyiBackend/script/tests/test_mes_edhr_recordbook_global_setting_sql.py`。
- PASS: 后端合同 `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordbookGlobalSettingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- PASS: 前端静态合同 `node IntRuoyiFronted/tests/e2e/edhr-recordbook-global-setting-static.spec.js`。
- PASS: 前端类型检查 `pnpm ts:check`。
- PASS: 真实前端路径验证使用本机 `芋道源码/admin`，在个人中心配置页签关闭全局记录本后，批次 `900000000819` / 任务 `5989` 详情接口返回有效 `recordbookEnabled=false`，批次详情隐藏填写载体控件，直连 `fillCarrier=RECORDBOOK&fillMode=RECORDBOOK_UNRESTRICTED` 显示关闭提示。
- PASS: 恢复验证已将全局开关恢复为 `true`，同一批次详情接口有效值回到 `recordbookEnabled=true`，页面重新显示“批记录/记录本”切换控件。
- PASS: 实现提交 `c45b97f509cb599d9affae8ca5240cde69c3e7f5` 已推送到 `origin/int_main`；提交前完成 mixed hunk 精准暂存和 `git diff --cached --check`。
