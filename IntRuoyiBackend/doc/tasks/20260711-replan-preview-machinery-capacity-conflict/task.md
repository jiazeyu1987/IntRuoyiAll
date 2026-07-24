# 预览重排设备工序产能冲突修复

## Task Goal
修复预览重排接口报 设备工序产能存在冲突: machineryId=47, processId=922851 导致前端显示系统异常的问题，保持产能口径可追溯且不引入隐藏兜底。

## Current Status
completed

## Milestones
1. completed：已定位冲突来自旧工序别名产能行与当前目标工序产能行都被映射为同一 processId 后，后续按 `machineryId + processId` 合并触发冲突。
2. completed：已新增定向回归，复现设备 47 的旧别名与当前目标产能行重复映射问题。
3. completed：已在设备工序产能查询服务中按正式身份去重；显式当前目标产能优先于旧别名产能，同类非显式冲突仍 fail fast。
4. completed：定向 GREEN 与相关回归 19 tests 已通过。
5. completed：已重启本机后端并记录运行态证据；测试租户真实登录后的同载荷接口验证不再返回系统异常或设备工序产能冲突，因该载荷属于其他租户数据而返回业务校验错误。

## Expected Verification
- RED：定向测试先复现同设备同工序产能冲突误判。
- GREEN：定向测试通过，合法口径不再抛系统异常。
- REGRESSION：相关排产/产能/身份测试通过。
- RUNTIME：本机 48081 运行态健康，重排预览不再因该冲突报系统异常。

## Closeout Evidence
- Cleanup preview/apply：`task-closeout-cleanup` preview 与 apply 均通过，删除项为 `<none>`，blocked 为 `<none>`。
- Worktree：当前仓库为主工作区 `int_main`，非 linked worktree，无需融合或删除 worktree。
- Commit：实现提交 `6c24fe905d 任务: 修复预览重排设备工序产能冲突`。

## 经验门禁
- PowerShell：显式 UTF-8；不使用 &&。
- 排产身份：实时链路按稳定路线工序身份解析，不把可变旧工序 ID 当唯一事实源。
- 禁止 fallback：不得吞掉冲突、默认选第一条、按顺序覆盖或返回 mock 成功；必须明确正式产能唯一性口径。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，需定位设备产能与工序身份的正式唯一键。
- 是否存在临时补丁或绕过：否。
