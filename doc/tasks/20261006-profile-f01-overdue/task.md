# 个人中心F01逾期待办修复

## Task Goal
按影响排序，在独立worktree只修复第一项F01：仍可办理的本人/正式候选eDHR OVERDUE任务在个人工作台可见且计入角标；静态代码验证与真实页面E2E通过后融合主代码。

## Priority
1. F01/P1：逾期生产任务漏行/漏数，影响正式任务办理入口。
2. F02/P1：多来源首批截断，影响大量任务查询、筛选及隐藏恢复。
3. F04/P2：改密重复提交和成功后敏感字段残留。
4. F03/P2：缺联系方式账号无法只改昵称。

## Scope
只改变个人中心eDHR待办的状态集合为TODO与OVERDUE，保留本人/候选、租户、终态批次/ARCHIVE例外、正式身份与导航。其他三项不实施。以本轮用户“第一个修复功能、不要扩大范围”为准，原总方案F01/F02同批开发要求在此任务中拆开；不引入五来源聚合、全局分页/快照/容量改造。

## F01最小实现决定
- 在现有my-page增加显式includeOverdue选项；true时只取TODO与OVERDUE，使用同一个分页请求，COUNT和列表共用状态过滤；不引入F02快照机制。
- 未设置选项时保持原单状态TODO默认；显式status与includeOverdue同时传入时拒绝歧义请求。
- Mapper复用原本人/候选及批次可见性条件，不新增聚合接口或改变50条加载上限。
- 个人中心列表和角标都显式传includeOverdue=true；不修改其他工作任务看板调用。
- 回归验证包含默认TODO、单OVERDUE、联合状态、非开放状态排除、候选token精确匹配、终态及ARCHIVE例外。

## Milestones
- [x] M0：核对当前代码及真实E2E入口/数据前提（admin逾期样本为空）、建立独立worktree和预约运行槽位。
- [x] M1：确定F01最小实现，添加对应定向测试并修复（测试执行属于M2）。
- [x] M2：完成静态代码验证及真实前端E2E；记录证据，不用API/mock代替业务动作。
- [ ] M3：主Agent审核范围与结果，清理并融合int_main，完成任务记录。

## Expected Verification
- TODO与OVERDUE同时出现并计数；DOING/DONE/CANCELED不加入。
- 现有服务端权限/本人候选、批次终态及ARCHIVE例外和旧接口默认行为不变。
- OVERDUE正式行经页面入口进入原业务页面；隐藏/恢复不修改业务任务状态。
- 定向静态合同/行为回归及目标ESLint；真实E2E使用任务自有数据，具备OVERDUE状态及前端进入/隐藏恢复证据；按用户最新授权使用模拟夹具并明确披露。
- 用户本轮仅要求第一项定向静态与E2E门禁，不执行F02聚合/容量测试、F03/F04或全量回归；修改后端时增加对应定向验证。

## 设计约束检查
- 独立worktree；主工作区无关改动不进入本次提交或融合。
- 先预约槽位、检查端口归属，禁止影响int_main后端或其他任务进程。
- 禁止fallback、吞异常或mock业务请求。用户最新明确授权“没有数据，你来模拟”：仅准备用于F01的任务自有模拟数据库夹具，前端与后端仍使用实际实现；验收动作由Playwright页面完成，最后精确清理夹具。
- 用户授权真实E2E及验证通过后融合主代码；仅提交任务自有改动，不额外推送或发布。
- 根AGENTS覆盖关联旧默认BDD/TDD、提交脏工作区基线及推送要求。

## Current Status
ready_for_closeout：F01已放行并快进融合int_main，定向静态和真实页面E2E通过，授权模拟数据残留0；主工作区五个并行文件字节未变。核心附属文件cleanup通过；托管worktree归档因快照文件检查超时而未完成，物理目录/依赖残留/slot7保留，尚不标completed。

## E2E数据授权及结果
用户“没有数据，你来模拟”明确允许准备本任务模拟数据。新增五种状态的实际数据库任务，用真实前端8088、任务后端48088和指定测试账号验收；没有mock页面请求，没有通过API代办被验收动作。TODO/OVERDUE显示且计数，其他三状态不加入；隐藏/恢复及正式入口均从页面完成。夹具清理只删除本任务新增行，两轮均残留0。

## 验证边界
全项目类型检查既有TS2677已通过HEAD对照确认，保留FAIL风险；不扩大F01范围修复。列表50条上限仍属于F02。此次只融合源码，不等于运行态发布；没有授权推送、发布或主后端重启。

## Cleanup Keep
- doc/tasks/20261006-profile-f01-overdue/task-state.json
- IntRuoyiFronted/tests/e2e/profile-edhr-overdue-static.spec.cjs
- IntRuoyiFronted/tests/e2e/profile-edhr-overdue-behavior.spec.cjs
- .runtime/profile-f01-overdue-aborted/partial-node_modules

## Cleanup Archive Pending
- .runtime/profile-f01-overdue-aborted/partial-node_modules

## Closeout约束
按最近根AGENTS保护主工作区并行改动；未重叠文件不阻止本任务融合，不做全脏基线提交或推送。cleanup使用worktree-closeout=off只处理任务附属文件，随后单独执行源码重叠/祖先/指纹检查和ff-only融合，最终使用Codex托管归档工具退役worktree。机器状态和三份核心报告保留，截图及原始运行证据位于任务专有D盘目录，不提交原始日志或凭据。

## 收尾继续条件
托管归档队列完成且实际E:/IntRuoyiWorktree/profile-f01-overdue/IntRuoyi及其.git/worktrees/IntRuoyi5元数据移除后，核验partial-node_modules残留不存在；按照端口登记互斥锁释放本任务slot7，更新completed及提交最终收尾记录。不要重复开发/测试已通过F01，不手工删托管worktree，不释放仍存在目录的槽位。
