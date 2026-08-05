# AC-M09 QA 检验规程发布闭环修复

## Task Goal

修复 `AC-M09 | QA | 维护检验规程` 当前只读/预览状态，补齐正式 QA 检验规程维护、草稿保存、发布、不可变版本与发布失败校验链路。

## Milestones

- [x] 建立后端 QA 规程保存草稿、发布、读取不可变版本的 API 与服务契约。
- [x] 增加后端发布完整性、冲突、已发布版本不可修改的 fail-fast 校验。
- [x] 接入前端 QA 规程页面正式保存草稿和发布调用，移除“未写入后台”的阻断提示。
- [ ] 补齐后端 JUnit 与前端静态契约 RED/GREEN 验证。
- [x] 记录验证、收尾和剩余阻塞。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest" test`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `pnpm ts:check` 如前端类型链路改动需要全量类型验证。

## Current Status

blocked：AC-M09 后端/前端修复已完成，`mvn -pl yudao-module-mes -am "-DskipTests" compile` 与前端 QA 静态契约已通过；目标 JUnit 被主工作区其它 Maven 进程持续写入同一 `yudao-module-mes/target` 导致 `target/classes` 缺失阻断，`pnpm ts:check` 超时且已停止本任务残留进程。未提交、未推送，不标记 completed。

## Baseline Commits

- `5486d9ba9`：保存进入本任务前的既有前后端与任务文档改动。
- `fc5e98ffe`：保存进入本任务前的残余岗位矩阵分析文档更新。
- `515798d74`：保存并发 AC 任务文档更新。

## Applicable Gates

- 后端修改已读取 `docs/backend-development.md`，适用“QA 规程配置状态必须来自产品级规程记录”和“PQC 检验项目事实必须来自发布规程和结构化 itemResults”。
- 前端修改已读取 `docs/frontend-development.md`，必须使用正式 API 错误展示，不得吞异常或默认成功。
- 数据库相关代码已读取 `docs/database-rules.md`，本次优先复用现有 QA 规程表，不新增运行 SQL。
- Git/PowerShell/收尾已读取 `docs/powershell-memory.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式后端状态机与前端写入链路。
- `是否存在临时补丁或绕过`：否。
