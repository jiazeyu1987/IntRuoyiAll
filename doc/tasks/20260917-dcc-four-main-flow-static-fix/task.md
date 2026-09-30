# 20260917 DCC Four Main Flow Static Fix

## Goal

用 gpt-5.5 high 子线程静态检查 DCC 四个主流程：受控浏览、升版、文件生命周期、上传到受控保存。主线程复核子线程反馈，只修复确认属实的主流程代码逻辑问题，循环检查直到子线程反馈无静态逻辑问题。

## Milestones

- 启动四个 gpt-5.5 high 子线程分别检查四条主流程。
- 主线程复核每条反馈，区分真实缺陷与非问题。
- 对确认缺陷补 BDD、RED、GREEN，并最小范围修改代码。
- 复跑子线程或聚焦静态检查，直到无静态主流程逻辑问题。
- 完成验证报告与收尾状态更新。

## Expected Verification

- 静态子线程最终反馈四条主流程无剩余静态逻辑问题。
- 受影响后端定向 Maven 测试通过，或记录明确阻塞。
- 受影响前端定向静态合同 / 类型检查通过，或记录明确阻塞。
- `git diff --check` 通过。

## BDD

- BDD: 受控浏览权限分离 -> Given 用户只有名称查看权限，When 搜索、浏览或选择关联候选，Then 只能发现名称级信息，正文、详情、预览、下载必须另验内容权限。
- BDD: 受控浏览上下文隔离 -> Given 用户切换目录、文件或版本，When 旧请求晚于新请求返回，Then 页面和服务端结果仍绑定当前目录、文件、版本和权限上下文。
- BDD: 升版检出检入 -> Given 有权限人员检出已有文件，When 检入新内容和修改说明，Then 生成新版本、旧版本保留、新版本需审批和盖章 PDF 后才生效。
- BDD: 大版本关联通知 -> Given 已有关联指向旧版本，When 关联文件大版本生效，Then 仅通知有权限处理人且不自动切换关联；小版本变化不通知。
- BDD: 生命周期审批闭环 -> Given 版本进入审批，When 审批期间、驳回或最终批准，Then 内容锁定、驳回保留原因并要求重传，最终批准必须完成盖章 PDF 默认目录受控保存。
- BDD: 上传到受控保存 -> Given 上传人员选择项目和有效模板文件夹，When 首次上传并提交审批，Then 生成逻辑文件和初始版本，已有同身份不能通过上传入口创建新版本，最终批准即生效。

## Design Constraints Check

- 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、DCC 主流程文档。
- 不执行 E2E，除非用户当轮另行明确要求。
- 不提交、不推送、不操作远程服务器、不写数据库、不停止或重启 `int_main` 服务。
- 不回滚或覆盖工作区已有并行改动；只处理本任务确认范围。

## Current Status

blocked - 重复准备记录，已收口至 doc/tasks/20260917-dcc-main-flow-static-logic-audit/；以主任务 verification-report.md 为准，不独立声明完成。Git 提交推送及正式 cleanup 尚未执行。
