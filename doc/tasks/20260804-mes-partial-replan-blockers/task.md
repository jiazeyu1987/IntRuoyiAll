# 自动重排逐工单阻断可视化

## Task Goal

自动重排遇到可归因到单个工单的阻断时，不再中止整批重排；没有阻断的工单继续完成重排，有阻断的工单保留任务并标红，同时可查看阻断原因。全局前置错误、无法归因错误和日历 token 等应用门禁仍必须 fail fast。

## Milestones

- [ ] 建立 BDD/TDD 证据，复现“任一阻断导致整批不应用”的当前缺陷。
- [ ] 后端按工单区分可应用与阻断范围，持久化阻断 issue 并返回汇总字段。
- [ ] 排产工单列表接口返回阻断数量和最新原因，供前端标红展示。
- [ ] 前端允许部分应用、展示阻断工单红色状态与原因。
- [ ] 运行目标后端、前端静态合同和类型检查验证，并记录证据。
- [ ] 收尾前执行经验沉淀、清理预览/应用、提交并推送。

## Expected Verification

- 后端 RED/GREEN：目标 `MesProAutoScheduleAlgorithmContractTest` 覆盖混合可排/阻断工单的部分应用行为。
- 后端回归：重排应用、夜间重排和 issue 持久化相关目标测试通过。
- 前端 RED/GREEN：新增静态合同覆盖不因局部阻断禁用整批应用、阻断行标红和原因可见。
- 前端回归：相邻重排静态合同通过，`pnpm ts:check` 通过或记录与本任务无关的既有阻塞。
- Evidence validators：bug、backend、frontend evidence 通过。

## Applicable Gates

- `docs/backend-development.md`：MES 排产算法与应用落库必须保留预览/应用重算、日历 token、issue 暴露和无静默降级。
- `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md`：应用重排必须重新计算并显式处理阻断 issue；缺路线、缺日历、缺产能等仍不能默认成功。
- `docs/frontend-development.md`：前端请求失败、后端阻断和页面状态必须可见，不得吞异常或静默成功。
- `docs/e2e-rules.md`：真实用户路径不可被 API-only 替代；本任务先以静态合同锁定 UI 行为。
- `docs/powershell-memory.md`：脏工作区先独立基线提交，Maven `-D` 参数整体加引号，逐条记录测试退出码。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务把“逐工单阻断不中止整批”建模为正式业务行为；全局/不可归因阻断仍 fail fast。
- `是否从根因和长期维护角度解决`：是。后端计算、应用、issue 持久化、列表响应和前端展示统一按正式阻断 issue 链路处理。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

已创建任务骨架，待完成脏工作区基线提交、RED 测试、实现和验证。
