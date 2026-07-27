# 工艺路线测试分类与测试项

## 任务目标

- 在 `系统管理 > 测试管理` 中新增可选分类 `工艺路线`。
- 根据工艺路线真实操作场景新增 4 个可执行的自然语言测试项。
- 通过后端契约测试、前端静态契约和真实前端页面验证分类与测试项可见、可保存、可回读。

## 里程碑

- [x] M1：读取项目规则、测试管理 schema、登录、E2E、前后端开发和运行态门禁。
- [x] M2：确认测试管理以 `project` 作为分类字段，确认工艺路线页面的新增、复制、版本、保存发布和状态操作场景。
- [x] M3：先完成后端/前端 RED，再实现 `工艺路线` 分类契约。
- [x] M4：通过真实前端页面新增 4 个工艺路线测试项并回读详情。
- [ ] M5：完成回归验证、清理、提交、推送和 worktree 收尾。

## 预期验证

- 隔离运行态前端：`http://127.0.0.1:8082`。
- 隔离运行态后端：`http://127.0.0.1:48082`。
- 使用 `芋道源码/admin` 登录本机测试管理页面。
- 分类下拉、项目筛选和项目列均可见 `工艺路线`。
- 4 个测试项可按精确名称检索，均为 `SEQUENTIAL`、`parallelSafe=false`、`ENABLE`，每项包含 4 个测试目标项。
- 真实前端保存后通过同一登录会话只读回读详情，确认方法项、测试数据和检查点完整。

## BDD 场景

- BDD: 工艺路线分类可维护 -> Given 测试管理页可访问，When 打开新增测试项并展开项目下拉，Then 可见并可选择 `工艺路线`，保存后列表项目列显示 `工艺路线`。
- BDD: 工艺路线基础信息与工序维护 -> Given 使用任务自有路线数据进入 MES 工艺路线页，When 新增路线并维护基础信息和工序后保存，Then 路线列表和详情显示编码、名称、工序配置且无默认成功。
- BDD: 工艺路线复制与产品绑定 -> Given 存在可复制的任务自有工艺路线，When 复制路线并绑定任务自有产品，Then 新路线编码/名称独立存在，产品绑定结果可在页面回读。
- BDD: 工艺路线候选版本编辑发布 -> Given 存在有 ACTIVE 版本的任务自有路线，When 创建候选版本、编辑流转关系并提交发布，Then 版本状态按业务流程进入审批/生效，草稿保存不提前发布。
- BDD: 工艺路线状态与删除约束 -> Given 存在已被业务数据引用和未被引用的任务自有路线，When 分别执行停用/启用和删除操作，Then 状态变化可回读，被引用路线删除被明确阻止，未引用路线按页面结果处理。

## 经验门禁

- 测试管理页面出现 `系统异常` 时，先核对 `system_codex_test_case.project` schema、迁移和分页接口，不隐藏错误或切换数据源。
- 测试管理写入必须使用真实前端路径；API 只用于保存后的只读详情核验。
- 真实页面使用隔离成对 URL `8082/48082`，不得切回 `8081/48081`。
- 写入前确认本机 `芋道源码/admin` 身份标签和任务数据标识；不写远端、生产租户或 admin 基线业务数据。
- Element Plus 项目下拉必须按可见文本 `工艺路线` 定位选项并点击，不用数组下标、隐藏 value 或坐标猜测。
- 测试项不执行 Runner；只验证测试项结构与页面落库结果，不把 Runner 离线当作新增成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；前后端共享同一项目枚举契约，避免只改页面造成保存时后端拒绝。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## 主工作区早期记录合并说明

- `int_main` 曾存在同任务早期草稿，目标是新增 1 个工艺路线测试项，且因为 `48081` 未监听停在预检阶段。
- 当前合并版采用 worktree 中完成后的正式范围：新增 `工艺路线` 分类并通过真实页面维护 4 个测试项；早期 blocker 仅作为历史预检信息保留，不代表当前任务状态。

## Cleanup Keep

- doc/tasks/20260726-codex-test-process-route-case/task.md
- doc/tasks/20260726-codex-test-process-route-case/execution-log.md
- doc/tasks/20260726-codex-test-process-route-case/backend-api-evidence.md
- doc/tasks/20260726-codex-test-process-route-case/frontend-feature-evidence.md
- doc/tasks/20260726-codex-test-process-route-case/verification-report.md
- doc/tasks/20260726-codex-test-process-route-case/ensure-process-route-codex-test-items.e2e.cjs
- doc/tasks/20260726-codex-test-process-route-case/artifacts/process-route-codex-test-items-summary.json
