# PQC 弹框使用 QA 工序列数据

## Task Goal

修正一线 PQC“接收标准 / 检验方法”弹框的数据来源：弹框正文必须对应 QA 工序表格中当前工序与当前检验项目行的“接收标准”和“检验方法”列数据，而不是默认首检摘要、默认判定文案或其它替代字段。

## Milestones

- [x] 建立任务文档与 BDD 验收口径。
- [x] 定位当前前端字段映射、QA 工序列字段和弹框展示逻辑。
- [x] 先补聚焦静态合同并取得 RED。
- [x] 实现 QA 工序列数据优先进入卡片与弹框。
- [x] 运行 GREEN、类型检查、相邻合同和收尾验证。

## Expected Verification

- 聚焦静态合同：`PqcInspectionItem` 保留 QA 工序列的 `acceptanceStandard` 与 `processInspectionMethod` 字段，并用于弹框正文。
- 相邻静态合同：PQC 项目级设备/标准/方法合同、PQC active title 方法展示合同保持通过。
- `pnpm ts:check`。
- 后端 `yudao-module-mes` 编译。
- `git diff --check`。

## Current Status

completed

追加真实页面验证状态：BLOCKED。2026-08-08 本机 `48081` health 为 `UP`、`8081` 为 HTTP 200，但真实页面待检 PQC 工序接口仍未返回 `acceptanceStandard/processInspectionMethod`，说明当前运行态尚未加载本次后端 VO 映射；需刷新运行态后再执行真实点击验收。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；若 QA 工序列缺失，不用默认首检规则或其它字段冒充对应列。
- `是否从根因和长期维护角度解决`：是；把 QA 工序列字段建模到前端正式展示对象，弹框和摘要统一读取该来源。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 命中 `docs/backend-development.md#MES PQC 项目级检验快照门禁`：PQC 检验方法、接收标准、上下限、单位和精度必须来自发布 QA 规程 / 项目级快照；本轮进一步锁定 QA 工序表格列字段，不使用默认首检文案替代。
- 命中 `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：本轮使用任务专用静态合同做 RED/GREEN，不把静态合同冒充真实页面 E2E。
