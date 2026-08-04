# MES 物料产品选择工艺路线

## Task Goal

在现有 MES 物料产品维护入口中增加“工艺路线”反向维护能力，让用户既可以从工艺路线关联产品，也可以从产品侧选择或解除工艺路线；实现必须复用现有 `mes_pro_route_product` 绑定关系，不新增第二套产品路线关系源。

## Milestones

- [x] 建立任务文档、BDD 场景和验证计划
- [x] 梳理现有工艺路线-产品绑定接口、服务、Mapper 与 MES 物料产品页面入口
- [x] 先补 RED 静态/后端契约测试，锁定产品侧路线选择行为和唯一绑定约束
- [x] 实现后端产品侧查询/保存接口，复用 `MesProRouteProductService`
- [x] 实现前端 MES 物料产品表单中的工艺路线选择、回显、已启用路线锁定与解除
- [x] 运行定向 GREEN/REGRESSION 验证并归档证据
- [x] 运行产品侧工艺路线只读真实页面 E2E 并归档证据
- [ ] 完成经验沉淀、cleanup、提交和推送

## Expected Verification

- 前端任务专用静态契约先 RED 后 GREEN，证明 MES 物料产品表单存在“工艺路线”配置并调用 route-product 关系接口。
- 后端目标测试先 RED 后 GREEN，证明 `itemId` 可查询/保存/解除当前路线绑定，并保留单产品唯一工艺路线约束。
- 运行受影响前端静态契约、`pnpm ts:check` 或记录明确阻塞。
- 运行受影响 MES 后端 Maven 目标测试，必要时使用 `-pl yudao-module-mes -am`。
- 运行 `frontend-feature-delivery` 与 `backend-api-delivery` evidence validator。
- 追加真实 Playwright 只读页面验证：从本机前端打开 MES 物料产品，进入产品编辑弹窗的“工艺路线”页签，断言使用 `item-binding-list` 与 `get-by-item`，不调用 `simple-list`，且不发出 MES 写请求。

## Current Status

blocked

产品侧工艺路线绑定的后端接口、前端入口、静态契约、目标 JUnit、前端类型检查、真实页面只读 E2E、经验沉淀、cleanup 和提交已完成；最终完成状态提交 `6107745f0` 及本轮 E2E 文档更新仍需推送，当前阻塞点仍是 GitHub 443 网络不可达。

## Cleanup Keep

- doc/tasks/20260804-mes-item-route-selection/mes-md-item-route-selection-readonly-real.e2e.cjs

## Applicable Gates

- 工艺路线三类配置术语契约：本任务只处理工艺路线与 MES 物料产品的正式绑定关系，不使用 `formBindings`、表单槽位、工序开始或批记录表单链路替代。
- 前端静态契约隔离门禁：若既有大契约或 `ts:check` 先失败在无关历史问题上，必须用任务专用最小静态契约完成 RED/GREEN，并在日志记录剩余阻塞。
- 工艺路线启用状态门禁：产品侧选择列表不能使用只返回已启用路线的 `simple-list`；已启用路线只能作为当前绑定回显，新增、变更、解除仍由后端 `validateRouteNotEnable` fail fast。
- 提交后残余改动复扫门禁：每次基线、实现或收尾提交后都要复扫 `git status --short --branch` 与 `git diff --name-status`，避免把并行残余改动混入当前任务。
- 脏工作区基线门禁：本任务所在工作区存在大量并发未暂存改动，提交时必须只暂存本任务文件；不得回滚或混入 DCC、PQC、排产、多维筛选等无关文件。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用现有 `mes_pro_route_product` 权威关系和唯一性校验。
- `是否存在临时补丁或绕过`：否。
