# 20260917 DCC Main Flow Static Audit Fixes

## Task Goal

静态检查并修复 DCC 四条主流程代码逻辑：受控浏览、升版、文件生命周期、上传到受控保存。使用用户授权的 gpt-5.5 high 子线程分别检查主流程代码逻辑；主线程复核每个反馈，确认真实问题后再修改，循环到子线程认为没有静态逻辑问题为止。

## Milestones

1. 读取仓库规则、DCC 主流程文档和相关源码位置。
2. 创建 4 个 gpt-5.5 high 子线程分别审计四条主流程。
3. 主线程复核子线程反馈，区分真实缺陷、待确认业务细节和误报。
4. 对确认缺陷补 RED 静态/单元测试，再修复到 GREEN。
5. 运行受影响范围回归和收尾清理检查。

## Expected Verification

- RED：为确认的静态逻辑缺陷补充最小失败测试或静态合同。
- GREEN：修复后同一测试或静态合同通过。
- REGRESSION：运行受影响后端 Maven 测试、前端静态合同/类型检查或 `git diff --check`，按实际改动范围确定。
- Closeout：补 `verification-report.md`，状态先到 `ready_for_closeout`，再执行 cleanup preview/apply；若本轮未授权 Git 提交/推送，按项目规则记录 blocker。

## BDD Scenarios

- BDD: 受控浏览权限分层 -> Given 用户只有名称查看权限, When 搜索、浏览或选择关联文件, Then 只能发现文件名称与允许公开的元数据, 不能获得正文、详情、预览或内容摘要。
- BDD: 受控浏览上下文隔离 -> Given 用户切换目录、文件或版本且旧请求未完成, When 任一请求返回, Then 结果必须重新匹配当前目录、文件、版本和权限上下文, 不能串用旧数据。
- BDD: 升版互斥与审批 -> Given 有权限人员检出已有受控文件, When 检入新内容并填写修改说明, Then 系统按版本规则形成新版本、保留旧版本且新版本需审批和盖章 PDF 后才生效。
- BDD: 大版本关联通知 -> Given 已有关联指向某个具体版本, When 被关联文件仅发生小版本变化, Then 不发送大版本通知且关联不自动切换；When 发生大版本变化到确认通知时点, Then 仅通知有权限处理人员并由接收人确认是否切换。
- BDD: 文件生命周期闭环 -> Given 新建或检入版本进入审批, When 审批驳回, Then 审批结束并保留原因, 申请人必须重新上传新文件；When 最终批准前盖章 PDF 与默认目录均有效, Then 批准即生效并保存为受控文件。
- BDD: 上传到受控保存 -> Given 上传人员选择项目和有效模板文件夹, When 首次上传源文件并提交审批, Then 保存逻辑文件与初始版本；已有同身份文件时上传入口不得创建新版本。

## Design Constraints Check

- 遵守 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`。
- 不执行 E2E，除非用户当轮另行明确要求。
- 不提交、不推送、不操作数据库写入、不停止或重启 `int_main` 后端服务。
- 不使用 fallback、降级、吞异常、模拟成功或兼容补丁。
- 子线程只做静态代码逻辑检查；生产代码修改由主线程复核后执行。

## Current Status

blocked - 重复准备记录，已收口至 doc/tasks/20260917-dcc-main-flow-static-logic-audit/；以主任务 verification-report.md 为准，不独立声明完成。Git 提交推送及正式 cleanup 尚未执行。
