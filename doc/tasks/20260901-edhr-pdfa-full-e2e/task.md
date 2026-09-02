# eDHR PDF/A 完整链路 E2E

## Task Goal

在独立 worktree 运行环境中，以本机 `芋道源码/admin` 测试管理员身份，通过真实前端页面补齐任务所需权限和可追踪模拟数据，集中修复阻塞问题，最终完成从批次准备、管理者代表放行、最终归档、PDF/A 生成到历史追溯、下载与打印的完整 E2E。

## Milestones

1. `M1`：已完成 - 创建独立 worktree、任务记录并登记专属运行端口 `8087/48087`。
2. `M2`：已完成 - 独立前后端已启动，真实页面完成 Stage4/Stage5 准备并复现候选待办与审批上下文问题。
3. `M3`：已完成 - 通过真实管理员页面为 `admin` 临时补齐“管理者代表”角色，独立页面重新登录后可进入候选审核。
4. `M4`：已完成 - 以回归测试先行修复审批服务从放行事务恢复批次编号的问题，定向 Maven 回归通过。
5. `M5`：已完成 - 重建并重启独立后端，Playwright 已完成最新真实 PDF/A 全链路。
6. `M6`：进行中 - 汇总证据并执行任务收尾门禁。

## Expected Verification

- 独立 worktree 使用端口登记表分配的专属前后端端口，不占用 `8081/48081`。
- Playwright 从真实登录页使用 `芋道源码/admin` 身份登录，权限分配操作通过真实管理页面完成。
- 所有模拟数据包含任务标识 `20260901-edhr-pdfa-full-e2e`，可追踪且记录清理方式。
- 从批次准备到管理者代表最终放行、归档责任人最终归档均通过真实业务页面操作。
- 归档详情显示 PDF/A 校验通过，历史追溯可见，下载与打印入口可用。
- 生成的 PDF 文件通过程序校验和视觉渲染检查，不以静态合同或 API-only 代替真实 E2E。
- 新发现的产品问题先记录，再按 RED/GREEN 修复，并运行相关定向回归。

## Design Constraints

- 不引入 fallback、默认成功、API-only E2E 或静默权限绕过。
- `工序开始`、`批记录表单`、`formBindings` 三条链路保持独立，不互相推断或补齐。
- 密码、令牌、Cookie、数据库连接凭据不得写入任务记录或测试产物。
- 数据库写入只用于本机任务自有数据和用户明确授权的权限补齐，并记录可逆方式。

## Cleanup Keep

- doc/tasks/20260901-edhr-pdfa-full-e2e/task.md
- doc/tasks/20260901-edhr-pdfa-full-e2e/execution-log.md
- doc/tasks/20260901-edhr-pdfa-full-e2e/verification-report.md

## Current Status

ready_for_closeout - 最新真实链路已完成：批次 `900000001025` 已归档，最终归档待办 `2438` 已完成，归档 `33` 为 `SEALED`，PDF/A-1b 校验和历史追溯只读验证通过；任务分支已提交并 rebase 到 `int_main`，当前因主工作区 `E:\IntRuoyi` dirty 阻塞 ff-only 融合。
