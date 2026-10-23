# DCC 主流程静态逻辑审计与修复

## Task Goal

用 gpt-5.5 high 子线程静态检查 4 个 DCC 主流程代码逻辑，主线程复核子线程反馈，只修复确认真实存在的主流程逻辑问题，并持续循环到子线程确认无静态逻辑问题。

## Flow Scope

- 受控浏览流程：名称查看权限、内容查看权限、关联候选、关联操作权限、目录/文件/版本切换隔离。
- 升版流程：检出独占、检入说明、大小版本判断、大版本通知、旧版本保留、新版本审批和盖章 PDF 生效、关联不自动切新版。
- 文件生命周期：配置、新建、审批、驳回、盖章 PDF、生效、受控、修改、替代、关联通知。
- 上传到受控保存流程：项目与模板文件夹选择、首次上传、初始版本、附件一致性、审批锁定、盖章 PDF、默认目录受控保存、生效失败真实原因。

## Milestones

- M1：读取仓库规则、DCC 产品文档和历史交接文档。
- M2：子线程完成第一轮主流程静态逻辑审计。
- M3：主线程逐项复核反馈，区分真实缺陷和误报。
- M4：对真实缺陷补 BDD/RED，完成代码修复和 GREEN 验证。
- M5：循环复审直到子线程确认无主流程静态逻辑问题。
- M6：补齐验证报告，状态进入 ready_for_closeout。

## Expected Verification

- 子线程静态审计结论记录：每轮问题、主线程复核结果、修复状态。
- 对每个确认缺陷补最小 Given/When/Then 和 RED/GREEN/REGRESSION 证据。
- 定向后端 Maven 或前端 pnpm/静态契约验证覆盖受影响代码。
- `git diff --check` 验证补丁无空白错误。
- 不执行 E2E，除非用户后续当轮明确要求。

## Design Constraints Check

- 先名称权限、再内容权限；仅名称权限不得泄漏正文、详情、预览或内容摘要。
- 浏览、搜索、关联候选、切目录、切版本必须绑定当前目录/版本上下文，不能串用旧响应。
- 上传入口只创建首次文件；已有同身份文件不得通过上传入口创建新版本。
- 已有文件变更必须通过检出/检入形成版本，新版本批准且盖章 PDF 存入默认目录成功后才生效。
- 小版本变化不触发关联大版本通知；大版本变化也不自动切换既有关联。
- 驳回后重新上传修正文件并重新审批，保留原审批和驳回原因。
- 禁止 fallback、降级、吞异常、模拟成功、默认目录猜测、权限绕过和 API-only 代替真实页面路径。

## BDD Scenarios

- BDD: controlled browsing name/content split -> Given 用户仅有名称查看权限, When 搜索或选择关联候选, Then 可发现文件名称/版本候选但不能查看正文、详情、预览或下载内容。
- BDD: checkout/checkin version lifecycle -> Given 已有受控文件需要修改且用户有编辑权限, When 检出后检入新内容并填写说明, Then 生成新版本、旧版本保留、新版本仍需审批和盖章 PDF 后才生效。
- BDD: upload to controlled save -> Given 上传人员已选择项目和有效模板文件夹, When 首次上传源文件并通过审批且合格盖章 PDF 存入默认目录, Then 文件生效并作为受控文件可被授权浏览。
- BDD: major version related notification -> Given 文件存在已关联的具体版本, When 关联对象发生小版本变化, Then 不通知也不自动切换；When 大版本按确认时点变化, Then 仅通知有权限处理人员并由接收人确认是否切换。

## Current Status

blocked - 重复准备记录，已收口至 doc/tasks/20260917-dcc-main-flow-static-logic-audit/；以主任务 verification-report.md 为准，不独立声明完成。Git 提交推送及正式 cleanup 尚未执行。

已读取任务收尾、后端、前端、数据库、PowerShell 编码和 DCC 产品/交接文档，准备启动第一轮 gpt-5.5 high 子线程静态审计。
