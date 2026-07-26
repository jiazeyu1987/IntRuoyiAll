# eDHR 放行资料限制开关

## Task Goal

在个人中心“配置”页签新增 4 个金手指专用资料限制开关，并在 eDHR 放行预检与提交放行中严格校验对应特殊节点完成状态和已保存 `ADD` 附件；配置缺失、非法或预检后变更必须 fail fast。

## Milestones

- [ ] 任务门禁与经验规则核对
- [ ] BDD 场景与 RED 测试落地
- [ ] 后端配置接口、配置解析、放行预检与提交校验实现
- [ ] 数据库配置 seed 与静态合同实现
- [ ] 前端配置页签、开关确认保存、失败回滚和放行检查展示实现
- [ ] GREEN/回归验证与证据归档
- [ ] 收尾清理、经验沉淀、提交与推送

## Expected Verification

- 后端定向测试：`mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest" test`
- 前端静态合同：新增或更新的 eDHR 资料限制开关静态测试
- 前端类型检查：`pnpm ts:check`，若被无关历史问题阻塞则记录首个阻塞点
- SQL seed 静态测试：校验 `infra_config` 配置键、默认 JSON 全 false、无 `INSERT IGNORE`
- 若本机运行态、登录、租户和测试数据齐备，再执行真实 Playwright E2E，并记录全局开关恢复证据

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；配置缺失、JSON 非法、字段缺失、权限不足或证据不完整均按 fail fast/阻断处理。
- `是否从根因和长期维护角度解决`：是；以后端全局配置、预检快照 hash、提交一致性校验和前端配置入口形成正式链路。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/frontend-development.md#前端静态契约隔离门禁`：若既有大契约或 `pnpm ts:check` 失败在无关历史问题，必须新增任务专用最小静态合同并记录无关 blocker。
- `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：修改 `tests/e2e/*static.spec.js` 后重跑目标静态合同；不得为了通过合同改无关 DOM、文案或旧流程。
- `docs/e2e-rules.md#全局开关类-e2e-恢复门禁`：真实 E2E 切换共享配置前必须记录原始状态，并在 `finally` 恢复后独立复验。
- `docs/backend-development.md#2026-07-25-maven-reactor-兄弟模块验证门禁`：MES 模块测试必须使用 `mvn -pl yudao-module-mes -am ...`，避免兄弟模块旧本地产物误判。
- `docs/powershell-memory.md`：后续提交/推送前需复核 stale blocker、PowerShell Maven `-D` 参数加引号和选择性暂存门禁。
