# 班组长工作台与班组维护开发任务

## Task Goal

在 `D:\IntRuoyiWorktree\20260730-banzuzhang` 中完成生产一线报工工序池文档中的班组长开发工作，聚焦 F9/F10：

- F9：生产班组长 / PQC 班组长按负责范围查看员工提交、复核员工提交、查看所有生产工单并标记异常上报。
- F10：班组级员工启停、不良原因列表、工序设备绑定、设备参数上下限维护。

## Milestones

- [x] 创建并登记目标 worktree
- [x] 创建任务文档并记录 BDD/TDD 门禁
- [x] 补充 Open Questions / Blockers 初始定义并挂接需求与验收文档
- [x] 审计当前工序池、报工、记录本、生产工单、设备和权限实现
- [x] RED：补齐 F9/F10 后端 schema / service / permission 测试
- [x] GREEN：实现 F9/F10 后端正式模型、接口和服务
- [x] RED：补齐 F9/F10 前端静态合同或 E2E 入口测试
- [x] GREEN：实现班组长工作台和维护入口
- [x] REGRESSION：运行定向后端、前端和可用 E2E 验证
- [x] 更新验证报告、经验沉淀、closeout 证据

## Expected Verification

- 后端：运行 F9/F10 对应 Maven 定向测试，覆盖负责范围、复核、异常上报、员工启停、不良原因、工序设备、参数上下限和越权失败。
- 前端：运行 F9/F10 静态合同或真实 E2E，覆盖班组长提交看板、生产工单异常列表、复核动作和维护页面入口。
- 数据库：新增 schema / 菜单 / 权限 SQL 需通过结构化检查和相关 schema 测试。
- 规则：不得引入 fallback、默认成功、静默降级；缺正式来源、权限范围、签名或测试数据时 fail fast。

## Current Status

completed；F9/F10 后端、前端、SQL 迁移门禁、静态验证、真实 Playwright 页面冒烟和经验沉淀均已完成。任务分支已推送到 `origin/codex/20260730-banzuzhang` 的 `4d6acc51`，cleanup preview 为 `ready`，随后 ff-only 合并到 `E:\IntRuoyi` 的 `int_main`。cleanup apply 已移除 Git worktree 登记；Windows 因本任务运行 Jar 被占用而未能删除物理目录，停止任务专属 PID `19088/36928`、释放 `48098/8098` 后已受控删除残留目录。`git worktree list` 不再包含本任务 worktree。

## Worktree / Runtime

- Worktree: `D:\IntRuoyiWorktree\20260730-banzuzhang`
- Branch: `codex/20260730-banzuzhang`
- Runtime profile: `int_main`
- Reserved slot: `17`
- Frontend port: `8098`
- Backend port: `48098`
- Final worktree state: 已从 Git worktree 列表和文件系统移除
- Final runtime state: PID `19088/36928` 已停止，`48098/8098` 已释放
- Final merge state: `codex/20260730-banzuzhang` 已 ff-only 合入 `int_main`
- Final slot state: `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 中 `20260730-banzuzhang` 已设为 `active=false`，slot `17` 于 `2026-07-30T23:35:46.1992651+08:00` 释放

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；当前任务必须缺前置即阻塞。
- `是否从根因和长期维护角度解决`：是；目标是正式模型、权限范围、审计日志和可验证接口/页面，不以备注、前端隐藏或 API-only 代替。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 工序池不能由现有 `mes_pro_feedback_surplus_pool` 替代。
- 班组长负责范围只能控制员工/PQC 提交和维护动作；全量生产工单异常列表不得扩大非负责员工提交明细权限。
- 复核、异常标记、基础维护不得改写一线原始 payload、记录本原始条目、工序池提交事件、FIFO 分配明细或电子签名。
- 一对多读模型必须先聚合再 JOIN，避免提交看板或时间轴重复主事件。
- E2E 脚本入口、页面入口、权限和测试数据缺失时记录 blocker，不得把静态合同或 API wrapper 冒充真实 E2E。
- Open Questions / Blockers 初始定义以 `docs/acceptance/production-line-process-pool/open-questions-blockers.md` 为当前编号来源，后续 BDD/TDD/实现日志按 OQ/BLK 编号引用。

## Cleanup Keep

- doc/tasks/20260730-banzuzhang/backend-api-evidence.md
- doc/tasks/20260730-banzuzhang/database-schema-evidence.md
- doc/tasks/20260730-banzuzhang/frontend-feature-evidence.md
- doc/tasks/20260730-banzuzhang/migration-policy-gate.json
