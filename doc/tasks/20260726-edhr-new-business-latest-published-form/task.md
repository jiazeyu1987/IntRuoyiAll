# eDHR 新业务仅使用最新已发布批记录表单

## Task Goal

修复个人工作台把终态 eDHR 批次残留任务展示为可处理待办、点击后报“当前 eDHR 批次状态不允许该操作”的问题；保持 `openTask` 对关闭、归档、驳回、作废批次的 fail-fast 保护。同时确认新建批次、返工或其它新业务只能冻结并使用对应批记录定义下最新的已发布版本，历史业务继续使用既有快照，不回写或静默升级。

## Milestones

- [x] 定位工作台进入处理、批次创建/返工和批记录版本冻结链路
- [x] 核对终态待办过滤与最新已发布表单选择的 BDD/TDD 证据
- [x] 确认最小后端修复已合入并保持历史快照、终态批次 fail-fast
- [x] 运行后端定向回归、制品构建、运行态和真实前端登录验证
- [ ] 完成提交、推送和任务收尾

## Expected Verification

- 后端回归覆盖同一批记录定义存在多个已发布版本时，新业务只选择最新已发布版本
- 后端回归覆盖历史批次仍使用冻结版本，不发生运行时自动升级
- `openTask` 对可处理新业务成功，对关闭、归档、驳回、作废批次继续 fail-fast
- 必要时通过 Playwright 从个人工作台真实“进入/处理”路径验证
- `git diff --check`、技能证据校验、任务收尾 preview/apply、提交与推送通过

## Current Status

ready_for_closeout

## 经验门禁

- `eDHR 批次任务配置来源门禁`：新建/返工批次必须核对当前 BATCH 配置、绑定归属与发布快照；当前配置存在时不得静默回退历史快照。
- `eDHR 批记录版本治理规则运行态门禁`：运行态必须使用已发布且治理通过的批记录版本，不能绕过已发布状态或单元格规则确认。
- `eDHR 终态批次个人待办门禁`：终态批次待办必须从列表源头过滤，不能放松 `openTask` 的终态保护。
- `eDHR 批次执行数据库夹具与证据文件门禁`：真实路径验证必须走个人工作台页面，API 仅作只读辅助核验。
- `PowerShell Maven -D 参数引号门禁`：定向 Maven 测试的 `-Dtest` 和 `-Dsurefire.failIfNoSpecifiedTests` 参数整体加引号，并保留 `-am`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；终态批次在个人待办查询源头过滤，`openTask` 保持终态保护；版本选择在新业务创建边界统一收敛，历史业务继续尊重冻结快照。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

doc/tasks/20260726-edhr-new-business-latest-published-form/task.md
doc/tasks/20260726-edhr-new-business-latest-published-form/execution-log.md
doc/tasks/20260726-edhr-new-business-latest-published-form/verification-report.md
