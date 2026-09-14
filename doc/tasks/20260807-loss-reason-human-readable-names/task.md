# 20260807-loss-reason-human-readable-names

## Task Goal

将本机默认租户生产组长“工序配置”中以 `RLR0807M-*` 表示的占位损耗描述，改为与对应工序匹配、人员可理解的中文损耗原因。保留已有“黑点”等正常描述、内部损耗编码、启用状态和其它业务数据不变。

## Milestones

- [x] 建立任务目录并核对数据来源、登录、数据库、E2E 与维护权限门禁。
- [x] 通过真实页面只读确认目标租户、负责路线、工序和占位描述精确范围。
- [x] 为全部目标工序建立无兜底的显式中文原因映射，并完成 RED 证据。
- [x] 通过真实页面逐项修改目标损耗描述。
- [x] 通过页面和只读接口完成范围、字段保持和错误复验。
- [x] 完成经验沉淀检查和任务清理收尾。

## Expected Verification

- 真实登录身份为 `芋道源码/admin`，页面范围与用户截图中的 `球囊扩张导管` 一致。
- 仅修改该路线中 `reasonName` 匹配 `RLR0807M-*` 的记录，目标写请求全部为正式损耗原因修改请求且业务码为 `0`。
- 修改后该路线不再显示 `RLR0807M-*`，每条原因均为对应工序可理解的中文描述。
- 已有“黑点”等非目标描述、每条记录的内部编码、启用状态和记录 ID 保持不变。
- 页面错误、目标 HTTP 错误和非预期 MES 写请求均为 `0`。

## Current Status

completed

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否；发现未知工序、范围不唯一、目标接口失败或字段漂移时立即停止。
- `是否从根因和长期维护角度解决`：是；修改正式损耗原因描述字段，不在展示层将编码临时替换为硬编码文案。
- `是否存在临时补丁或绕过`：否；写入使用真实页面维护入口，不使用直接 SQL、API-only 写入或 mock。

## Experience Gate

- `docs/login-access.md#本机登录来源`：写入前必须由真实页面同时确认 `芋道源码/admin`、负责路线和目标列表范围，不得切换到其它测试租户。
- `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦`：使用正式 `process-config/list` 数据源和生产组长损耗维护入口，不使用一线设备账号链路替代。
- `docs/e2e-rules.md#写入型 E2E 任务自有模拟环境门禁`：本次仅维护用户明确指出的本机现有数据；禁止将 API-only、mock 或其它租户结果冒充页面写入通过。
- `docs/e2e-rules.md#表格行定位`：按页面可见的路线、工序和原描述定位具体表格行，不使用接口数组下标映射页面行。

## Data Safety

- 环境限定为本机 `int_main`：前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`。
- 写入范围必须同时满足：路线为用户截图对应路线、原描述匹配 `^RLR0807M-`、记录可从当前页面损耗维护弹窗定位。
- 修改前保存记录 ID、内部编码、原描述、启用状态和目标中文描述清单；失败时保留已完成清单并停止，不扩大范围。
- 不新增、不删除、不停用损耗原因，不修改数据库 schema 或直接执行 SQL。

## Current Range Evidence

- 真实身份：`芋道源码/admin`。
- 目标路线：`球囊扩张导管`，`routeId=900025`。
- 初始页面：`106` 行；目标路线 `23` 个工序、`64` 条损耗原因。
- 目标占位描述：`63` 条；初始非目标描述：`1` 条“黑点”。
- 并发保持：写入中另一个页面在 `routeProcessId=926786` 新增 `ID=566 / LOSS-926786-003 / 黑点`；已纳入非目标保持快照，不删除、不覆盖。
- 写入前计划校验：23 个工序均具有显式中文原因清单，目标数量 `63`，MES 写请求 `0`。

## Cleanup Candidates

- `doc/tasks/20260807-loss-reason-human-readable-names/loss-reason-human-readable.e2e.mjs`
- `doc/tasks/20260807-loss-reason-human-readable-names/inspection.json`
- `doc/tasks/20260807-loss-reason-human-readable-names/change-manifest.json`
- `doc/tasks/20260807-loss-reason-human-readable-names/apply-result.json`
- `doc/tasks/20260807-loss-reason-human-readable-names/final-verification.json`
- `doc/tasks/20260807-loss-reason-human-readable-names/final-process-config.png`
- `doc/tasks/20260807-loss-reason-human-readable-names/failure-diagnostic.json`
- `doc/tasks/20260807-loss-reason-human-readable-names/failure-diagnostic.png`

## Final Verification

- 新 Playwright 浏览器会话核验通过：23 个工序、65 条最终原因、63 条目标记录已改为中文描述、`RLR0807M-*` 占位描述为 `0`。
- 目标记录的 ID、内部编码、工序归属和启用状态保持不变；2 条非目标“黑点”保持不变；独立核验未产生 MES 写请求。
- 经验门禁已合并到 `docs/e2e-rules.md` 并登记到 `docs/experience-index.md`。
- 清理 preview/apply 均通过：删除 8 个本任务临时产物，保留 `task.md`、`execution-log.md`、`verification-report.md`，无阻塞和警告。
