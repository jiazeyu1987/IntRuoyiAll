# DCC 历史文件升版状态矛盾核验

## Task Goal

确认账号“赵海辰”在 `/dcc/controlled-file/upload` 选择“按压式球囊扩充压力泵 · IDI · 1 / 技术文档 / 设计和开发策划阶段 / 技术调研报告”历史文件后，页面是否同时出现升版、新建 master、可提交和编号冲突错误等互相矛盾状态，并核对用户列出的相关配置与校验问题是否存在。

## Milestones

- [x] 建立核验门禁与证据范围
- [x] 静态核对前端上传页与后端版本链校验逻辑
- [x] 在当前本机运行态执行只读真实页面核验
- [x] 输出 verification-report.md，逐项标记存在 / 不存在 / 阻塞
- [x] 根据用户确认“未分类允许发布”输出 optimization-plan.md

## Expected Verification

- 读取并遵守 DCC 上传、E2E、登录、运行态、数据库与 PowerShell/编码规则。
- 只读核验当前页面状态、接口响应、控制台与网络错误；不提交业务数据。
- 对无法通过本机运行态确认的项，记录精确前置缺口与影响，不用 API-only 或静态合同冒充真实页面通过。

## Current Status

completed

## Verification Result

PARTIAL / BLOCKED：当前本机 `int_main` 运行态可确认多项配置与预检问题，但目标历史文件“按压式球囊扩充压力泵技术调研报告.pdf”在 `tenant_id=1/zhaohaichen` 的当前本机数据中不存在可选历史候选，无法完整复现“选择唯一历史文件后同时显示升版、新建 master、可提交并提交报英文冲突”的页面闭环。

补充优化结论：用户已确认文控文件允许发布到“未分类”，因此技术调研报告自动落位“未分类”不作为缺陷处理；其余问题已整理到 `optimization-plan.md`，按历史升版状态冲突、编号冲突预检、版本/日期校验、类别配置和数据质量分级处理。

## Closeout Evidence

- Cleanup preview：PASS，保留 task/execution-log/verification-report/Playwright JSON/截图/验证脚本，删除项 0，阻塞 0。
- Cleanup apply：PASS，删除项 0，阻塞 0；当前不是 linked worktree，无 merge/remove 操作。
- Experience consolidation：已按技能规则检查；本次主要是一次性 DCC 数据/环境漂移和页面缺陷核验，不新增长期经验文档。
- Optimization document：已新增 `optimization-plan.md`，并完成 UTF-8 结构性读取校验。

## Applicable Gates

- DCC 文控上传和版本链核验必须使用真实页面路径；API/DB 只能作为辅助或最终核验。
- 版本链、master 当前版本、受控目录和类别映射缺失不得用 fallback、默认成功或“未分类”静默掩盖。
- 缺登录、租户、端口、运行态、正式数据或页面入口时必须 fail fast 并记录影响。

## Cleanup Keep

- `doc/tasks/20260808-dcc-history-version-conflict-verification/playwright-verification-result.json`
- `doc/tasks/20260808-dcc-history-version-conflict-verification/upload-page-history-conflict.png`
- `doc/tasks/20260808-dcc-history-version-conflict-verification/verify-dcc-history-conflict.cjs`
- `doc/tasks/20260808-dcc-history-version-conflict-verification/optimization-plan.md`

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：本任务为核验，不修改业务实现；若发现问题，将定位到具体链路和证据。
- 是否存在临时补丁或绕过：否。
