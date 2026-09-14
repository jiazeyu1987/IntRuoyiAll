# EDHR-STATIC-015 生产更正同步正式数据

## Task Goal

修复 EDHR-STATIC-015：生产组长签名更正报工损耗数量后，正式反馈、物料事实、完工损耗条件和损耗回填必须读取同一组已签核更正后的数量；未签核或不完整更正不得影响正式证据。

## Scope

- 只处理 EDHR-STATIC-015。
- 不处理 EDHR-STATIC-016 及其它缺陷。
- 不编辑共享缺陷总表，最终答复仅给出建议更新内容。
- 不执行 Playwright/E2E、数据库写入、服务启动/停止/重启、远程服务器操作、git commit 或 git push。

## Milestones

- [x] M0 读取仓库规则、收尾规则、eDHR 与生产/损耗/回填相关文档。
- [x] M1 写入 BDD/TDD 验收合同。
- [x] M2 定位生产更正、正式反馈、完工损耗判断和损耗 writer 的正式来源链路。
- [x] M3 先补 RED 静态/定向合同，证明当前更正未同步正式反馈数量。
- [x] M4 最小化修复生产更正通过后的正式反馈和物料事实同步。
- [x] M5 执行非 E2E 定向验证，记录 GREEN 与风险。
- [x] M6 标记 ready_for_closeout，执行 cleanup preview；因用户禁止提交/推送，按项目规则记录最终状态。

## Expected Verification

- 静态合同或定向单元测试覆盖：原损耗 0 更正为 2 后，下游正式反馈数量、完工损耗条件和损耗 writer 均读取 2。
- 静态合同或定向单元测试覆盖：旧损耗 5 更正为 2 后，后续读取不再使用旧 5。
- 负向覆盖：缺签名、未批准或更正明细不完整时，不更新正式反馈或物料事实。
- 定向检查无新增 fallback、吞异常、默认成功或仅展示 JSON 修复。
- 不执行 E2E、数据库写入、服务启停、远程操作、git commit/push。

## Current Status

ready_for_closeout

实现和非 E2E 定向验证已完成，用户于 2026-09-14 后续明确要求本地 git commit 并融合进 `int_main`；当前进入本地提交/融合收尾。Git push 仍未获当轮明确授权，完成状态需按项目推送门禁另行核对。

## Design Constraints Check

- No fallback：更正同步必须更新正式来源或明确失败，不读取旧值兜底。
- Fail fast：缺少签名、更正字段、目标反馈或物料事实时必须抛出明确业务错误。
- 正式来源一致：事件 rawPayload、管理摘要、`MesProFeedbackDO` 和 `MesProFeedbackMaterialDO` 的数量事实在同一事务内保持一致。
- 审计保留：更正仍通过现有 revision/correction 证据保留原值，不覆盖原始事件审计。
- 范围边界：不放宽完工、损耗 writer 或放行读取的一致性检查。
- 验证边界：只运行静态代码逻辑检查和必要非 E2E 定向验证。

## Cleanup Candidates
