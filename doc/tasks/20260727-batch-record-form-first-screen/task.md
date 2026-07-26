# 优化进入批记录表单的首屏时间

## Task Goal

优化用户进入 eDHR 批记录表单时的首屏时间，优先减少首屏阻塞请求和同步渲染成本，保持现有路由、权限、接口错误暴露和表单语义不变。

## Milestones

- [ ] 定位批记录表单入口、首屏阻塞链路和现有测试契约
- [ ] 记录 BDD 场景并建立 RED 静态契约
- [ ] 实施最小前端性能优化，不引入 fallback、mock 或默认成功
- [ ] 运行 GREEN 与回归验证，补充性能/前端证据
- [ ] 收尾：状态、验证报告、经验沉淀、清理与提交推送检查

## Expected Verification

- 聚焦静态契约先 RED 后 GREEN，证明首屏不再等待非首屏数据加载。
- 受影响前端静态测试或脚本通过。
- 如运行态前置齐备，再通过真实页面路径验证进入批记录表单首屏可见。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是拆分首屏关键路径与非首屏数据加载。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 前端静态契约隔离门禁

- Trigger: 需要 RED/GREEN 静态契约，但全量 `pnpm ts:check` 或既有大契约可能先失败在无关历史问题上。
- Preflight check: 先运行最接近的既有契约并冻结首个无关失败；若失败点不属于当前任务，新增任务专用最小静态契约覆盖当前行为。
- Blocker: 无法证明失败点与当前任务无关，或专用契约不能稳定先 RED 后 GREEN。
- Verification: `execution-log.md` 同时记录无关 blocker、专用契约 RED/GREEN 和全量回归剩余阻塞摘要。
- Forbidden action: 禁止修改无关大契约绕过历史失败，禁止跳过当前需求最小 RED/GREEN。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

### eDHR 批次执行数据库夹具与证据文件门禁

- Trigger: 运行 eDHR 批次执行真实 E2E 或复跑批记录表单真实路径。
- Preflight check: 默认从本机 Docker MySQL 读取授权租户、账号、批次执行、批次任务、工作任务和执行 ID；写型验证需记录原始值、影响行数和回滚方式。
- Blocker: 本地数据库不可达、授权租户/账号不存在、无当前账号可打开待办工作任务、证据路径覆盖非当前任务历史 PASS。
- Verification: 记录 E2E 命令、证据文件路径、入口 URL、租户/账号标签、数据库来源和 PASS/BLOCKED 结果。
- Forbidden action: 禁止 mock、API-only、默认成功、未授权租户或未记录数据库直改替代真实前端路径。
- Evidence: `docs/e2e-rules.md#edhr-批次执行数据库夹具与证据文件门禁`。

### eDHR 历史执行只读验证门禁

- Trigger: 从 eDHR 批次详情、批记录、记录本或执行记录入口打开 `/mes/pro/feedback/edhr-execution/form`。
- Preflight check: 区分当前活动填写与历史执行只读追踪；当前活动填写必须通过正式按钮或 `openTask` 返回上下文，历史只读必须使用 `viewMode=tracking`。
- Blocker: 页面提示非责任人、非当前活动表单或对象 VIEW 权限不足时停止并记录账号/租户标签。
- Verification: 只读 tracking E2E 断言追踪详情、追踪表单区域、返回参数保留和无 MES 写请求。
- Forbidden action: 禁止 API-only、管理员写入、旧 executionId 直连填写页或忽略对象级权限。
- Evidence: `docs/e2e-rules.md#edhr-历史执行只读验证门禁`。
