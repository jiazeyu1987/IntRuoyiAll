# 统一审批平台迁移清单

## 状态定义

- `Integrated`：已接入统一审批平台，并通过 provider、摘要、轨迹或明确能力合同。
- `Allowed Domain Page`：模块正式处理页，允许保留业务动作，但不得成为跨模块审批中心。
- `Classified Domain Action`：存在审核/审批语义，但当前没有独立审批人待办来源，保留在业务正式页并由治理扫描防止自建审批中心。

## 已接入基线

| 优先级 | 模块 | 状态 | 当前边界 |
| --- | --- | --- | --- |
| P0 | BPM 原生审批 | Integrated | 统一中心展示我发起的/抄送我的，正式页为 BPM 流程详情 |
| P0 | DCC 受控文件审批 | Integrated | 统一中心展示摘要和轨迹，正式页为 DCC 文件详情 |
| P0 | eDHR 工作任务审批 | Integrated | 统一中心展示摘要和轨迹，正式页为 eDHR 工作任务页 |
| P0 | Showroom 变更单审批 | Integrated | 统一中心展示摘要和轨迹，正式页为 `/showroom/approval` |
| P0 | MES 报工审批 | Integrated | 统一中心展示待办、已办、我发起的摘要和轨迹，正式页为 `/mes/pro/feedback?feedbackId=...`；报工审批动作仍由 MES 正式页执行 |
| P0 | CRM 合同/回款审批 | Integrated | 提交接口创建 BPM 流程实例，审批任务由 BPM 原生 provider 进入统一中心；CRM backlog 私有待审核列表退役 |

## 允许保留的领域页

| 优先级 | 模块 | 状态 | 原因 |
| --- | --- | --- | --- |
| P0 | DCC 审批任务/工作台页面 | Allowed Domain Page | 承载 DCC 特殊签名、文控校验、发布回调 |
| P0 | eDHR 审批详情页 | Allowed Domain Page | 承载批记录快照、签字格、归档和返工校验 |
| P0 | Showroom 审批工作台 | Allowed Domain Page | 承载差异预览、主管/企宣签名、发布回调 |
| P0 | MES 报工页面 | Allowed Domain Page | 承载报工审批、检验完成、任务/工单状态校验和领域回调 |

## 已分类领域动作

| 优先级 | 模块 | 分类证据 | 处理方式 |
| --- | --- | --- | --- |
| P1 | eDHR 统一变更/作废/重开/补充审批 | `EDHR_DOMAIN_ACTIONS_CLASSIFIED`：控制器动作直接落正式页业务校验和签名证据，无独立审批人待办列表 | 保留正式页领域动作；后续若产生真实待办来源再扩展 sourceTaskType |
| P1 | eDHR 发布审批 | `EDHR_DOMAIN_ACTIONS_CLASSIFIED`：发布审批承载发布/归档/生效回调 | 保留正式页领域动作 |
| P1 | SRM supplier-access/procurement-plan/tender expert | `SRM_DOMAIN_ACTIONS_CLASSIFIED`：现状为 SRM 业务状态动作和正式页校验 | 保留正式页领域动作；供应商门户审核继续由 SRM provider 接入 |
| P2 | ERP 出入库审批/反审批 | `ERP_DOMAIN_ACTIONS_CLASSIFIED`：ERP 审核/反审核是单据状态动作，没有统一审批任务 owner 队列 | 不创建 provider；禁止自建审批中心 |

## 禁止事项

- 不得新增私有跨模块审批中心。
- 不得将候选模块用空 provider、mock 轨迹或默认成功接入。
- 不得把业务详情页强行并入统一审批中心。
- 不得把业务特殊签名和发布/归档/生效回调改造成 BPM 通用按钮。
