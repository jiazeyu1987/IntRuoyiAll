# P0 生产执行主闭环 BDD/TDD 文档设计

## Task Goal

围绕“工序池提交事件”设计 P0 后续开发的 BDD 场景、TDD 顺序、E2E 路径和测试数据门禁，确保生产执行主闭环可以回答：谁、在哪台设备、做了哪个工序、做了多少、质量结果怎样、签名是谁、进入哪个生产工单、班组长是否复核、最后如何进入正式批记录追溯。

## Milestones

- [x] 建立任务审计边界并读取适用项目规则、技能规则和经验门禁。
- [x] 梳理现有工序池、报工、PQC、电子签名、班组长复核、FIFO 分配、批记录追溯证据。
- [x] 编写 P0 BDD 场景、失败场景和边界场景。
- [x] 编写严格 TDD 顺序、RED/GREEN 命令、期望失败原因和回归门禁。
- [x] 编写真实 E2E 路径、测试数据、阻塞条件和验证报告。
- [x] 优化 P0 文档的开发可执行性门禁，消除命令、脚本、事件身份、trace 完成条件和测试数据边界歧义。

## Expected Verification

- 使用 UTF-8 方式读取新增 Markdown 文档，确认中文未乱码。
- 运行 BDD/TDD acceptance plan validator 的 self-test，确认技能验证器可用。
- 对新增任务文档执行结构化文本检查，确认包含 Required sections、Given/When/Then、RED/GREEN 证据模板和 no-fallback 设计约束。
- 对 P0 文档执行实现可执行性检查，确认后端命令工作目录、前端脚本前置、trace 完成条件、测试数据清理边界和 no-fallback 门禁均显式可执行。
- 不运行生产代码构建或真实 E2E；本任务仅产出文档设计，后续实现任务再执行代码级 RED/GREEN。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；文档设计以“工序池提交事件”作为主事件事实源，不用零散页面或前端拼接替代正式数据链路。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 已读取 `docs/experience-index.md`；本任务命中 MES、工序池、报工、批记录表单、PQC、电子签名、班组长复核、FIFO 分配和真实 E2E 相关门禁。
- 当前文档设计必须遵守：批记录表单来源只能是工序设置中的逐工序正式批记录表单绑定，不得使用 `formBindings`、默认 `MAIN`、特殊开始节点配置或前端文案替代。
- 当前文档设计必须遵守：缺正式生产工单、正式工序、正式设备、正式质量结果、正式电子签名、正式复核记录或正式批记录追溯链路时，测试应阻塞，不得写默认成功。

## Deliverables

- `docs/acceptance/production-execution-main-loop/scope-contract.md`
- `docs/acceptance/production-execution-main-loop/bdd-scenarios.md`
- `docs/acceptance/production-execution-main-loop/tdd-plan.md`
- `docs/acceptance/production-execution-main-loop/e2e-plan.md`
- `docs/acceptance/production-execution-main-loop/test-data.md`
- `docs/acceptance/production-execution-main-loop/traceability-matrix.md`
- `docs/acceptance/production-execution-main-loop/implementation-readiness-gates.md`
- `doc/tasks/20260802-p0-production-execution-loop-bdd-tdd-design/verification-report.md`

## P0 Current Gap Summary

- PQC 正式提交必须补齐工序池事件链路；当前测试证据显示 PQC submit 仍未调用工序池事件创建服务。
- 班组长复核和报工确认必须补齐电子签名字段和服务校验；当前复核模型只记录 leader、状态、说明和时间。
- 需要新增统一闭环 trace；当前 trace 能分段查分配、订单工序和批记录，但尚不能按单个事件返回完整主闭环。
- 主提交需要补齐提交级幂等；当前记录本 payload 有幂等字段，但一线主提交请求没有统一幂等契约。

## Closeout Note

- 文档设计和结构验证已完成，详见 `verification-report.md`。
- cleanup preview / apply 已通过且无删除项。
- 当前不标记 `completed`，因为 commit 和 push 尚未执行；工作区存在大量非本任务改动，不能混入当前任务收尾。
- 2026-08-03 已完成文档优化并通过结构、语义、UTF-8 和 diff 检查；任务重新回到 `ready_for_closeout`。
