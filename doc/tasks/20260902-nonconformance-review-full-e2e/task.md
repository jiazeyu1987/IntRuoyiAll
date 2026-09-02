# 不合格审批完整链路 E2E

## Task Goal

在独立 `int_main` worktree 中，使用本机 `芋道源码/admin` 测试身份，通过 Playwright 走真实前端页面完成不合格审批链路全 E2E；允许通过真实页面创建任务自有模拟数据、为 admin 补齐缺失权限，并对链路问题先记录、后集中解决，直到至少一条完整处置链路从不合格触发、冻结、QA 评审到处置后页面状态与追溯闭环全部通过。

## Milestones

- [x] M0：创建独立 worktree，登记独立运行端口，建立任务记录。
- [x] M1：冻结当前不合格审批需求、页面入口、状态机和测试数据前置条件。
- [x] M2：启动 worktree 独立运行态并完成登录、菜单、权限与任务数据预检。
- [x] M3：Playwright 完成不合格触发、冻结限制、QA 待办和评审提交的首次真实页面执行。
- [x] M4：集中修复首次执行发现的问题，逐项补充 BDD、RED、GREEN 和回归测试。
- [x] M5：重跑完整真实页面链路并验证处置后页面、列表、状态和追溯。
- [ ] M6：整理验证报告，执行任务收尾门禁。

## Expected Verification

- `pwsh -NoProfile -File scripts/preflight/branch-runtime-port-guard.ps1`
- worktree 前端 `http://127.0.0.1:8088/` 返回 HTTP 200。
- worktree 后端 `http://127.0.0.1:48088/actuator/health` 返回 `UP`。
- Playwright 从真实登录页使用 `芋道源码/admin` 登录并从真实菜单进入目标页面。
- Playwright 通过真实页面创建或选取任务自有批次并触发不合格评审，验证活跃工单冻结。
- Playwright 在冻结状态下分别尝试生产报工、PQC 提交、PQC 放行，三项均被页面或正式业务错误禁止。
- Playwright 从 QA 页面看到待评审数量和冻结批次，上传评审材料、填写评审意见并完成电子签名。
- 至少一条处置链路完成后，QA 冻结列表移除该项，批次进入正确后续页面状态，追溯页显示不合格原因、评审报告、处置结论、QA 签名、冻结/解冻时间。
- 让步放行、返工、作废三条处置分支均使用任务自有批次验证，最终以作废终态收口。
- 任何生产代码修复均有对应 BDD、RED、GREEN 与相关回归验证。
- 任务自有数据可识别、可追踪；异常残留和清理状态必须明确记录。

## Design Constraints Check

- 只使用一套“不合格评审单”流程、同一入口和同一套代码，来源允许为 `PQC_SUBMISSION` 或 `PQC_RELEASE`。
- 冻结对象是包含批次信息的活跃工单；冻结期间禁止报工、PQC 提交和 PQC 放行。
- 评审材料、评审意见和电子签名为评审提交必填项。
- MVP 状态机保持最小：`frozen -> normal/voided`，评审单 `pending_review -> closed`；返工作为处置结论后直接回主流程。
- 返工处置确认后直接回主流程，不增加生产人员“返工完成确认”步骤。
- 作废批次只读追溯，禁止继续生产、检验、放行和生成合格指令。
- 不添加 fallback、mock 成功、兼容旁路或 API-only 用户路径。
- E2E 写入只发生在用户授权的本机 `芋道源码` 租户，任务标识统一使用 `NCR-E2E-20260902`。
- 不占用 `8081/48081`，不停止或重启其它 worktree 服务。
- Git 提交、推送、融合按当前项目规则需要当轮明确授权后执行。

## Current Status

completed

实现、主链路验证、冻结三操作验证和用户新增“双入口都要进行 E2E 验证”的专门验收点均已完成。entry-only 双入口 E2E 脚本和静态合同已补强：`PQC生产放行` 待放行按钮与 `PQC组长 > PQC管理` 行按钮都进入同一不合格评审页，且评审创建/处置写请求数为 `0`。任务实现提交已经落在当前 `int_main` HEAD `9ac7af7df`。重启后已在 `int_main` 主工作区端口复验：前端 `8081` HTTP 200，后端 `48081` health `UP`；Playwright `20260902-int-main-04-entry-both-after-reboot` 双入口真实页面验证 PASS，并导出 24 张逐步截图。`int_main` 已推送到 `origin/int_main`，cleanup preview/apply 均通过且没有删除文件；本任务完成。

## Cleanup Keep

- doc/tasks/20260902-nonconformance-review-full-e2e/task.md
- doc/tasks/20260902-nonconformance-review-full-e2e/execution-log.md
- doc/tasks/20260902-nonconformance-review-full-e2e/verification-report.md
