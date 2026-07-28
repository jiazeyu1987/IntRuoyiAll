# 测试管理节点串首节点连续性误判修复

## Task Goal

修复在 `系统管理 > 测试管理` 中从第 1 节点开始执行仍提示 `Runner 回写结果不符合结构化契约：节点串必须从第 1 节点开始连续选择` 的回归问题，确保真实页面选择第 1 节点起连续节点串时能够按契约启动并完成回写校验。

## Milestones

1. 记录 BDD 场景与现有回归证据，定位节点串连续性校验的实际输入和误判点。
2. 编写最小 RED 回归测试，稳定复现第 1 节点连续选择被误判。
3. 实施最小根因修复，不引入 fallback、吞异常或默认成功。
4. 运行 GREEN、相邻回归与真实 Playwright E2E，验证测试管理页签真实执行路径。
5. 记录验证报告、风险、阻塞项和收尾状态。

## Expected Verification

- 静态或单元回归测试先 RED 后 GREEN，覆盖第 1 节点起连续节点串。
- 相邻测试管理 / Codex Runner 合同回归通过。
- 真实 Playwright 从 `http://127.0.0.1:8081` 登录 `芋道源码/admin`，进入测试管理页签并执行一次第 1 节点起连续节点串，不再出现该结构化契约错误。
- 任务证据通过 `bug-regression-fix-loop` 证据校验。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正节点串排序/连续性契约的正式校验或真实 payload 生成链路。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Codex Runner 自动测试门禁：真实执行前确认本机前端/后端入口、测试租户账号、Runner token、注册探针、heartbeat、Runner 执行状态，禁止用 API-only、Runner 离线跳过或默认成功冒充 E2E。
- Element Plus 表格选择门禁：表格行复选框必须限定可见 body 行并断言选中业务唯一键集合，禁止误点表头全选、数组下标或坐标猜测。
- 测试管理 schema 迁移门禁：访问测试管理系统异常或修改相关接口前必须核对 `system_codex_test_case` 真实 schema，禁止前端隐藏错误或后端默认 project 掩盖缺字段。
- 前端静态契约隔离门禁：若既有大契约存在无关历史失败，使用本任务聚焦最小契约证明 RED/GREEN，同时记录无关 blocker。

## Cleanup Keep

E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\node-chain-first-node-real.e2e.cjs
E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\node-chain-first-node-real-int-main.e2e.cjs

## Milestone Status

- 1. 已完成：BDD 场景、规则门禁与当前测试管理首节点报错已记录。
- 2. 已完成：新增 `startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain`，RED 稳定复现只选第 1 节点被误判。
- 3. 已完成：后端节点串校验改为校验已选节点序号是否为 `1..N` 连续前缀，不再要求选择完整节点串。
- 4. 已完成：目标回归、相邻后端测试、前端静态合同和真实 Playwright E2E 均通过。
- 5. 已完成：验证报告与 bug 回归证据已补齐；等待后续 closeout/提交边界处理。

## Remaining Closeout Notes

- 当前主工作区存在多项无关脏改动；本任务不回滚、不提交无关改动。
- 本任务真实 E2E 在 worktree 端口 `8083/48083` 完成，任务自有测试项已通过页面清理，当前活跃 `Codex首节点契约-%` 测试项数量为 0。
- 已融合进 `E:\IntRuoyi` 的 `int_main` 运行态：`8081/48081` 主端口真实 E2E 通过，执行批次 `29`，任务自有测试项 `53/54` 已通过页面清理，当前活跃 `Codex首节点契约-%` 测试项数量为 0。
