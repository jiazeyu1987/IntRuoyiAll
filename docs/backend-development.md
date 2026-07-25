# IntRuoyi Backend Development Rules

## 触发场景

- 修改 `IntRuoyiBackend` 下的 Java、Spring Boot、Maven、接口、服务、Mapper、DO、配置、脚本或后端测试前，必须先读取本文件。
- 涉及 SQL、schema、菜单、权限、租户绑定或数据修复时，还必须读取 `docs/database-rules.md`。
- 涉及本机服务启动、端口或运行态验证时，还必须读取 `docs/local-runtime.md`。

## 项目边界

- 后端根目录：`E:\IntRuoyi\IntRuoyiBackend`。
- 使用 Java 17、Spring Boot、Maven 多模块结构。
- 主应用模块：`yudao-server`。
- 业务逻辑必须保留在所属模块内；跨模块移动或耦合必须有明确的设计理由和验证。
- 不得根据前端页面或历史实现猜测后端 schema、权限、接口或租户行为。

## 实施规则

- 先确认变更所属模块、现有 Controller/Service/Mapper/DO 边界和已有测试。
- 对功能、修复、重构和行为变更，先记录 BDD，再执行 RED -> GREEN -> REGRESSION。
- 缺少数据库、Redis、依赖、测试数据或运行配置时，必须 fail fast；不得切换数据源、返回 mock 成功或吞掉错误。
- 接口和服务错误必须通过真实响应、日志或测试暴露；不得用默认成功值掩盖失败。

## 验证方式

- 优先运行受影响模块的定向 Maven 测试，例如：
  - `mvn -pl yudao-module-mes -am test`
  - `mvn -pl yudao-server -am test`
- 如果指定测试类，记录 `-Dtest` 范围和 `surefire.failIfNoSpecifiedTests` 处理依据。
- 涉及 API 行为时，验证成功路径和失败路径。
- 涉及前端调用时，最后通过真实前端路径或已批准的 E2E 核对接口结果。

## eDHR 详情回填门禁

### 路线配置有值但详情接口为空

- Trigger: eDHR、批次详情、动态表单、损耗单、工艺路线绑定、填写人、`fillableUsers`、`routeBindingId`、配置页有值但详情接口为空。
- Preflight check: 先同时核对配置接口/表中的来源字段、执行任务快照字段、详情接口组装链路和既有优先级，不得只改前端显示文案。
- Blocker: 若详情任务没有可追溯的绑定 ID、快照字段或正式规则来源，必须阻塞并补齐后端数据链路；不得从当前登录人、创建人、更新人或角色 ID 推断填写人。
- Verification: 新增后端回归测试覆盖“仅路线绑定配置填写人”场景，并同时跑相邻优先级测试，确认有效工作任务和工序规则仍优先。
- Forbidden action: 禁止前端把 `未配置` 改成配置页名称、禁止把角色/部门 ID 当用户 ID、禁止用空列表兜底掩盖缺失来源。
- Evidence: 任务 `doc/tasks/20260724-edhr-route-form-filler-backfill/`，目标测试 `MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`。


## eDHR 批次任务配置来源门禁

### 当前配置与发布快照边界

- Trigger: eDHR 批次执行、路线发布快照、`routeSnapshotJson`、`batchUseConfigs`、记录本/批记录融合、当前路线配置缺失或陈旧绑定。
- Preflight check: 新建/返工批次前先同时检查当前 BATCH 工序配置是否存在、绑定是否归属启用工序配置、发布版本快照是否包含完整 `flowGraph.nodes` 与 `batchUseConfigs`。
- Blocker: 只要当前 BATCH 工序配置存在，就必须使用当前配置并严格校验绑定归属；不得因为当前绑定陈旧而静默回退到发布快照。
- Verification: 同时覆盖“当前配置存在优先当前绑定”“当前配置整体缺失时使用已发布快照”“陈旧绑定必须 fail fast”“legacy flat batchRecordReportId 快照可投影”的后端测试。
- Forbidden action: 禁止把发布快照作为通用 fallback；禁止用空绑定、默认 MAIN 或默认成功掩盖当前配置损坏。
- Evidence: `doc/tasks/merge-jiluben-worktree-20260724/verification-report.md`。
## 禁止做法

- 禁止跨模块复制业务逻辑来绕过现有服务边界。
- 禁止未核对 schema 就写运行 SQL。
- 禁止捕获异常后静默返回成功、空数据或默认数据。
- 禁止缺少依赖或测试数据时跳过验证并宣称完成。
## 2026-07-25 Maven Reactor 兄弟模块验证门禁

- Trigger: 多模块 Maven 项目中当前模块依赖兄弟模块，出现缺方法、缺字段、DO/DTO builder 不一致、或测试编译引用 sibling module 新接口时。
- Preflight check: 先确认失败符号所属模块；若符号来自同 reactor 兄弟模块，必须用 `mvn -pl <module> -am ...` 重跑，让 Maven 同时构建依赖模块。
- Blocker: `mvn -pl <module> ...` 因未构建兄弟模块而失败时，不得直接判定为产品代码阻塞；必须复验 `-am` 后再给结论。
- Verification: 任务日志同时记录窄范围失败、`-am` 复验命令、PASS/FAIL 结果和影响模块。
- Forbidden action: 禁止用旧本地产物、跳过编译、API-only、或改 unrelated sibling 代码来掩盖 reactor 构建边界问题。
