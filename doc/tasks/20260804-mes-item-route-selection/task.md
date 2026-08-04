# MES 物料产品选择工艺路线

## Task Goal

在现有 MES 物料产品维护入口中增加“工艺路线”反向维护能力，让用户既可以从工艺路线关联产品，也可以从产品侧选择或解除工艺路线；实现必须复用现有 `mes_pro_route_product` 绑定关系，不新增第二套产品路线关系源。

## Milestones

- [ ] 建立任务文档、BDD 场景和验证计划
- [ ] 梳理现有工艺路线-产品绑定接口、服务、Mapper 与 MES 物料产品页面入口
- [ ] 先补 RED 静态/后端契约测试，锁定产品侧路线选择行为和唯一绑定约束
- [ ] 实现后端产品侧查询/保存接口，复用 `MesProRouteProductService`
- [ ] 实现前端 MES 物料产品表单中的工艺路线选择、回显与解除
- [ ] 运行定向 GREEN/REGRESSION 验证并归档证据
- [ ] 完成经验沉淀、cleanup、提交和推送

## Expected Verification

- 前端任务专用静态契约先 RED 后 GREEN，证明 MES 物料产品表单存在“工艺路线”配置并调用 route-product 关系接口。
- 后端目标测试先 RED 后 GREEN，证明 `itemId` 可查询/保存/解除当前路线绑定，并保留单产品唯一工艺路线约束。
- 运行受影响前端静态契约、`pnpm ts:check` 或记录明确阻塞。
- 运行受影响 MES 后端 Maven 目标测试，必要时使用 `-pl yudao-module-mes -am`。
- 运行 `frontend-feature-delivery` 与 `backend-api-delivery` evidence validator。

## Current Status

in_progress

已创建任务目录。开始进入 RED 前的代码结构核对。

## Applicable Gates

- 工艺路线三类配置术语契约：本任务只处理工艺路线与 MES 物料产品的正式绑定关系，不使用 `formBindings`、表单槽位、工序开始或批记录表单链路替代。
- 前端静态契约隔离门禁：若既有大契约或 `ts:check` 先失败在无关历史问题上，必须用任务专用最小静态契约完成 RED/GREEN，并在日志记录剩余阻塞。
- 提交后残余改动复扫门禁：每次基线、实现或收尾提交后都要复扫 `git status --short --branch` 与 `git diff --name-status`，避免把并行残余改动混入当前任务。
- 脏工作区基线门禁：本任务开始前已有脏工作区，已由用户确认冲突修复后完成独立基线提交；当前仍残留一个无关文件，必须避开并记录。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用现有 `mes_pro_route_product` 权威关系和唯一性校验。
- `是否存在临时补丁或绕过`：否。

