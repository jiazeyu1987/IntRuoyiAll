# 统一审批平台 Phase5 退休清单

## 唯一正式入口

Phase8 后，`/approval-center` 是 BPM、DCC、eDHR、Showroom、SRM、MES 报工审批任务列表的唯一正式入口。业务模块仍保留自己的详情页、业务校验、特殊签名、发布、归档、生效回调和历史处理页面。统一审批中心只负责聚合、筛选、摘要、轨迹和跳转，不替代模块处理页。

后续新模块上线前必须完成 `ApprovalTaskProvider` 与 `ApprovalModuleIntegrationDeclarations` 接入，并通过 `ApprovalModuleIntegrationGuard` 启动门禁。不得再自建审批中心、私有待办中心或临时兼容桥接。

## 已接入模块

| 模块 | 统一来源 | 正式处理边界 | Phase5 处置 |
| --- | --- | --- | --- |
| BPM | `bpm-native-approval` | `/bpm/process-instance/detail` | 待办、已办、我发起的、抄送我的进入统一审批中心；旧待办、已办、我的流程菜单隐藏。 |
| DCC | `dcc-controlled-file-approval` | `/dcc/controlled-file/detail` | 文控审批待办进入统一审批中心；DCC 工作台和个人中心不再暴露旧 `approval-tasks` 入口。 |
| eDHR | `edhr-work-task-approval` | `/mes/pro/edhr-work-task` | 工作任务继续由模块页面处理；旧 `feedback/edhr-approval` 列表菜单隐藏。 |
| Showroom | `showroom-approval` | `/showroom/approval` | 统一中心可按 Showroom 来源筛选并进入正式处理页；Showroom 内部审批中心菜单隐藏。 |
| SRM | `srm-supplier-portal-approval` | `/srm/supplier-portal-review` | 供应商门户审核进入统一审批中心；SRM 正式页保留通过、驳回、主档生成和准入档案生成。 |
| MES 报工审批 | `mes-feedback-approval` | `/mes/pro/feedback` | 报工审批待办、已办和我发起的进入统一审批中心；MES 正式页保留报工审批、检验和工单状态流转。 |

## 退休入口

| 入口 | 处置 | 原因 |
| --- | --- | --- |
| BPM 待办任务 `bpm/task/todo/index` | 隐藏菜单 | 待办由统一审批中心 `moduleCode=BPM&viewType=TODO` 承接。 |
| BPM 已办任务 `bpm/task/done/index` | 隐藏菜单 | 已办由统一审批中心 `moduleCode=BPM&viewType=DONE` 承接。 |
| BPM 我的流程 `bpm/processInstance/index` | 隐藏菜单 | 我发起的由统一审批中心 `moduleCode=BPM&viewType=MY_INITIATED` 承接。 |
| DCC 审批任务 `controlled-file/approval-tasks` | 隐藏菜单 | DCC 待办由统一审批中心 `moduleCode=DCC&viewType=TODO` 承接。 |
| eDHR 审批列表 `feedback/edhr-approval` | 隐藏菜单 | eDHR 工作任务由统一审批中心摘要和模块正式页承接。 |
| Showroom 审批中心 `ShowroomAdminApproval` | 隐藏菜单 | Showroom 审批从统一中心进入正式处理页。 |
| SRM 供应商门户审核 | 保留正式处理页，不新增私有审批中心 | 统一中心只负责 SRM 任务摘要、轨迹和 `/srm/supplier-portal-review?applicationId=...` 跳转。 |

## Phase Final 分类结果

| 对象 | 当前状态 | 处置 |
| --- | --- | --- |
| ERP 审核/反审核状态动作 | 已分类为领域状态动作 | 当前没有独立审批任务队列和审批责任人任务源；不得为这些动作新建私有审批中心。若后续产品冻结真实 ERP 审批任务合同，再按统一 provider 标准接入。 |
| CRM 合同/回款审批 | 私有 backlog 审核列表已退役 | 合同、回款审批提交创建 BPM 流程实例，统一审批中心通过 BPM provider 展示待办、已办和我发起。 |

以上分类不得被解释为 fallback 或临时桥接。后续任何新增审批任务源必须先声明模块码、provider、正式处理页、能力边界和测试证据，再进入统一审批平台。
