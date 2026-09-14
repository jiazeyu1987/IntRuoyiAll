# M0 验证报告

## Result

PASS。V4 的 M0 接口、writer、fixture、运行时顺序和工程决策已冻结；未启动 A1-A6，未修改生产代码、数据库或运行环境。

## Verified Scope

- `m0-contract-freeze.md` 包含 M0-01、M0-02、M0-03、M0-04 和明确通过结论。
- 请求、响应、blocker、申请状态和双幂等与当前 Controller/VO/前端类型/申请表基线对齐；当前缺口已分配给 A1/A2。
- 三类 writer 均冻结正式来源、正式目标、输入输出、完成条件、blocker 和禁止替代来源。
- fixture manifest 字段完整，配置/造数入口和真实页面路径已列明。
- 双 100%、`AO_RELEASE_SOURCE_V1`、原子事务、独立 BLOCKED 持久化和 `RELEASE_APPROVE` 唯一来源无歧义。
- 项目长期经验已存在对应门禁，本次没有新增通用经验文档或重复改写长期规则。

## Commands And Evidence

| 检查 | 结果 |
| --- | --- |
| `node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs` | PASS |
| `node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-schema-static.spec.cjs` | PASS |
| `node src/api/mes/pro/processpool/teamLeaderReleaseApplication.static.spec.cjs` | PASS |
| UTF-8 严格读取 task/execution-log/M0 contract | PASS |
| M0 六个必需章节定位 | PASS |
| 15 个关键契约 token 检查 | PASS |
| `git diff --check -- doc/tasks/20260809-active-order-release-dossier-m0` | PASS |

首次 schema 命令误写为不存在的 `mes-active-order-release-application-schema-static.spec.cjs`，Node 按预期报 `MODULE_NOT_FOUND`；通过 `rg --files` 定位仓库实际文件 `mes-team-leader-active-order-release-application-schema-static.spec.cjs` 后重跑通过。该失败是验证命令文件名错误，不是产品或 M0 契约失败。

## Residual Implementation Gaps

以下是 A1-A6 后续 BDD/TDD 范围，不影响 M0 契约冻结通过：

- A1/A2：blocker 四个可选定位字段、canonical hash、CONFIRMED PQC 进度、事务拆分。
- A2-A5：三 writer 真正接入、完成性检查、成功签名证据。
- A3：现有 backfill 适配当前 eDHR batch/task。
- A4/A5：传统正式过程检验/损耗报表 writer 和字段映射 source type。
- A6：任务自有 fixture manifest、真实页面 E2E 和最终只读核验。

## Final Gate

M0 可作为 A1-A6 的唯一开发输入。任何后续实现若缺正式来源、映射、签名、负责人或真实页面入口，必须阻塞，不能用 fallback、mock、直接 SQL、默认 `MAIN` 或动态 `formBindings` 替代。

Closeout preview/apply 均通过：4 个正式任务文档全部保留，无删除项、阻塞项或警告；当前为主工作区，不涉及 worktree 合并或移除。
