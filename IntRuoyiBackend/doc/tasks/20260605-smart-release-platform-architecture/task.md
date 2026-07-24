# 任务：Smart Release 长期架构文档完善

## 任务目标

在已放行的 `smart-release` 发布方案基础上，补齐长期可维护的架构文档包，避免把后续实现做成 `publish-int-ruoyi.ps1` 的临时补丁。文档必须明确领域模型、状态机、数据契约、实施路线、测试策略和迁移边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；文档必须坚持 fail fast，不允许 smart-release 因缺缓存、缺 manifest、缺 SQL、缺资源快照而静默全量构建或继续发布。
- `是否从根因和长期维护角度解决`：是；本任务以发布平台/发布契约为长期方向，不在现有脚本上继续堆临时逻辑。
- `是否存在临时补丁或绕过`：否；本任务只完善架构文档，不修改发布脚本、Runtime Control 代码或数据库。

## BDD 场景

- BDD: 长期架构不是脚本补丁 -> Given 当前发布链路由 PowerShell 脚本承担大量逻辑 / When 设计长期方案 / Then 文档必须定义 manifest 驱动的发布平台边界，而不是继续让脚本自行猜测发布行为。
- BDD: 本地变化必须先进入 release-intake -> Given 开发者手工修改本地表结构、必要数据或资源引用 / When 构建发布包 / Then release-intake 必须生成差异报告并要求绑定 migration、data registry 或资源快照，否则构建失败。
- BDD: 更快打包依赖可信缓存 -> Given 本次只变更一个组件 / When 执行 smart-release / Then 未变化组件只能从 immutable artifact cache 复用，并校验 digest、依赖闭包和工具链 hash。
- BDD: 大文件不进入普通发布搬运 -> Given DCC 有 10000 个文件且只新增少量文件 / When 普通发布包不携带文件实体 / Then 发布只检查资源引用和已验证 resourceDelta，真实文件同步由备份/恢复 delta 负责。
- BDD: 状态机阻止假成功 -> Given precheck、deploy、verify、rollback 任一步失败 / When Runtime Control 展示发布状态 / Then 状态必须停在明确失败或 `deployed_not_verified`，不得标记成功。

## 里程碑

- [x] M1：建立任务文档和 BDD 验收标准。
- [x] M2：补 ADR，记录从脚本式发布迁移到 manifest 驱动发布平台的架构决策。
- [x] M3：补系统设计、领域模型、状态机和数据契约。
- [x] M4：补实施路线、测试计划和迁移计划。
- [x] M5：完成文档门禁、收尾预览和提交。

## 预期验证

- 文档必须包含 ADR、系统设计、领域模型/数据契约、状态机、实施路线、测试计划、迁移计划。
- 文档必须明确 fail-fast 边界、禁止项、阶段性验收门禁和从当前链路迁移方式。
- 不修改发布脚本、Runtime Control 代码或数据库。

## 完成证据

- ADR：`adr-0001-manifest-driven-release-platform.md`。
- 系统设计：`system-design.md`。
- 领域模型与契约：`domain-model-and-contracts.md`。
- 状态机：`state-machine.md`。
- 实施路线：`implementation-roadmap.md`。
- 测试计划：`test-plan.md`。
- 迁移计划：`migration-plan.md`。
- 治理与未决决策：`governance-and-open-decisions.md`。
- 文档索引：`architecture-index.md`。
- 文档门禁：所有关键文档和关键约束关键词检查通过。

## 当前状态

completed

## Current Status

completed
