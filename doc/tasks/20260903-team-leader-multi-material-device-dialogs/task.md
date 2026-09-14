# 生产组长多物料设备弹框改造

## Task Goal
在附加 worktree 中改造生产组长工作台的修改弹框和分配弹框，让它们按一线生产同源数据支持多物料、设备、设备参数展示与提交边界，并通过真实前端 Playwright E2E 验证。

## Milestones
- [x] 读取项目规则、worktree/runtime/e2e 规则和相关技能。
- [x] 记录 BDD/TDD，并补生产组长弹框静态合同 RED。
- [x] 实现修改弹框和分配弹框的多物料、设备、设备参数展示。
- [ ] 运行静态合同、类型/格式检查和真实前端 Playwright E2E。
- [ ] 形成验证报告，保留截图/trace 证据。

## Expected Verification
- `node IntRuoyiFronted\tests\e2e\team-leader-multi-material-device-dialogs-static.spec.cjs`
- `pnpm exec vue-tsc --noEmit --pretty false` 或记录正式前置阻塞
- Playwright 真实前端打开生产组长页面，进入修改弹框与分配弹框，截图验证多物料、设备和设备参数可见

## Current Status
ready_for_manual_validation - 修改弹框和分配弹框已实现多物料、设备、设备参数展示；静态合同、后端目标测试、格式检查和 evidence 门禁已通过。worktree 前后端已启动在 8092/48092。真实前端 E2E 使用 `芋道源码/admin` 打开生产组长页面并点击事件 `8474` 的“修改”和“分配”弹框通过；当前样本无真实设备参数值，设备参数区域为空态/占位文本，待用户在 `int_main` 手动复核有设备参数值的样本。

## 设计约束检查
- 不用接口直接替代验收动作；E2E 必须通过真实前端页面操作。
- 修改弹框展示必须从当前报工/原始 payload 的正式快照读取，不用编码或默认值冒充数据。
- 分配弹框只使用生产组长正式活跃订单和当前报工分配快照，不跨工序推断。
- 默认禁止 fallback、降级、吞异常、模拟成功和兼容补丁。
