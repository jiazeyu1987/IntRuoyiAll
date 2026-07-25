# 20260725 Full E2E Admin Validation

## Task Goal

在 `E:\IntRuoyi` 的 `int_main` 本地运行环境中，使用真实前端入口和已授权的本机身份标签 `芋道源码/admin` 执行一次从创建批次执行、填写、放行到追溯的真实数据 Playwright E2E 全流程；验证过程中如发现属于当前融合结果的问题，按项目规则修复并复验。

## Milestones

1. 建立验证任务边界与证据目录，读取 E2E、登录、运行端口、worktree、编码和收尾规则。
2. 确认 `int_main` 本地前端 `8081`、后端 `48081` 的真实运行状态和归属。
3. 盘点现有 Playwright 真实 E2E 脚本，区分可安全运行、写入型需前置数据、阻塞或不适用于本次身份的用例。
4. 使用真实前端路径运行登录、创建批次执行、填写、放行和追溯验证，记录命令、入口、身份标签、断言和失败信息。
5. 对验证中暴露且属于当前融合范围的问题执行 BDD/TDD 修复和复验；不属于当前范围或缺前置条件的路径记录为 blocker。
6. 汇总验证报告，运行必要的静态/结构性检查，进入收尾状态。

## Expected Verification

- `http://127.0.0.1:48081/actuator/health` 返回后端健康状态。
- `http://127.0.0.1:8081/login?redirect=/index` 可访问并完成真实前端登录。
- 批次执行创建、填写、放行和追溯真实 Playwright E2E 在 `芋道源码/admin` 身份下运行并记录 PASS / FAIL / BLOCKED。
- 若修改代码，必须有对应 BDD、RED、GREEN 和回归验证证据。
- 凭据只通过临时环境变量或运行时输入使用，不写入任务文档、日志、提交信息或命令记录。

## Current Status

ready_for_closeout

## Scope Notes

- 当前工作区在开始本任务前已有其他线程留下的脏改动和未跟踪任务目录；本任务默认不修改、不回滚、不提交这些非任务自有文件。
- 本任务的自有证据目录为 `doc/tasks/20260725-full-e2e-admin-validation/`。
- 写入型 E2E 仅在具备已确认测试数据、可追踪任务标识和清理责任时运行；否则记录阻塞原因，不用 API-only 或 mock 替代。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；验证失败时优先定位真实前端路径、运行态、权限或实现根因。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 真实 E2E 登录与端口门禁

- Trigger: 使用 Playwright 通过本机前端执行登录后真实用户路径验证。
- Preflight check: 先确认 `8081` 前端入口、`48081` 后端健康检查、端口归属和 `芋道源码/admin` 身份标签；密码不得写入日志或文档。
- Blocker: 登录失败、端口未监听、端口归属不明、后端 health 非 UP、或前端入口不可达时停止，不得切换端口、账号、租户或远端环境。
- Verification: 记录 health、登录页 HTTP 状态、Playwright 命令、入口 URL、身份标签和关键断言。
- Forbidden action: 禁止 API-only 替代前端路径，禁止 mock 数据，禁止静默换端口或切换环境。
- Evidence: `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`。

### 任务专用 E2E 证据门禁

- Trigger: 运行真实 E2E 脚本，尤其是会写入业务数据或默认写入历史 evidence 的脚本。
- Preflight check: 显式设置本任务证据文件或任务 ID，并确认脚本所需租户、账号、工单、批次、路线和签名等前置数据均来自已授权测试范围。
- Blocker: 任一必需环境变量缺失、证据路径会覆盖非当前任务历史证据、或目标路径需要写入受保护业务数据时停止。
- Verification: 记录显式证据文件、入口 URL、身份标签、目标业务数据标识和 PASS / FAIL / BLOCKED。
- Forbidden action: 禁止使用默认任务 ID 覆盖历史证据，禁止用 API-only 或 mock 替代真实页面操作。
- Evidence: `docs/e2e-rules.md#任务专用-e2e-环境变量与证据文件门禁`。

### eDHR 只读与填写人显示门禁

- Trigger: 验证 eDHR 批次详情、记录本、执行记录、单据卡片或填写人显示。
- Preflight check: 区分当前活动填写与历史只读 tracking；页面断言填写人前先读取同一登录会话详情接口的 `fillableUsers` 当前显示值。
- Blocker: 权限不足、页面提示非当前任务责任人、`fillableUsers` 为空或页面显示与详情接口不一致时停止并记录。
- Verification: 同时记录批次标识、命中任务、接口显示值、页面可见文本和无 MES 写请求检查。
- Forbidden action: 禁止把旧 executionId 直连填写页、旧配置名称或 API-only 断言当成真实 E2E 通过。
- Evidence: `docs/e2e-rules.md#edhr-历史执行只读验证门禁`、`docs/e2e-rules.md#edhr-单据填写人显示值门禁`。
## Cleanup Keep

- doc/tasks/20260725-full-e2e-admin-validation/edhr-batch-execution-real-e2e-final.md
- doc/tasks/20260725-full-e2e-admin-validation/admin-preview-e2e-output/
- doc/tasks/20260725-full-e2e-admin-validation/form-fill-log-e2e-output/
- doc/tasks/20260725-full-e2e-admin-validation/edhr-release-check-report-final.json
