# 20260808 PQC 首检/巡检设备差异诊断

## Task Goal

回答用户“同一个产品的工序，为什么巡检有设备、首检没设备”的问题，基于真实页面和正式接口只读核对订单 `881MO090889`、产品 `球囊扩张压力泵`、工序 `组装 I 工序` 的首检与巡检 QA 项目设备配置差异。

## Milestones

- [x] 建立任务记录和适用门禁
- [x] 通过真实前端路径进入一线 PQC 页面并读取目标订单/工序
- [x] 核对首检、巡检任务及检验项目的设备选项数量
- [x] 输出原因、证据和是否属于配置问题或代码问题

## Expected Verification

- Playwright 真实页面只读路径进入本机 `http://127.0.0.1:8081`。
- 只读采集 `/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-orders` 与 `/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-order/processes` 响应。
- 证据包含目标订单、目标工序、检验类型、PQC 任务 ID、项目编码/名称、`equipmentOptions.length`。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按发布 QA 规程项目级设备绑定解释设备显示差异。
- 是否存在临时补丁或绕过：否。

## Applicable Gates

- PQC 项目设备事实必须来自发布 QA 规程项目、`equipmentRequired` 和项目级设备表；禁止用整单设备、同产品同工序默认共享或前端文案替代正式项目级快照。
- 一线 PQC 检验方法详情区有正式设备选项时显示“检验设备/设备编号”，无正式设备选项时隐藏两张卡片且不显示占位文案。
- 真实 E2E 使用 Playwright 操作真实前端页面；本轮未提交 PQC 检验结果，页面初始化触发的 `switch-employee` 上下文 POST 已单独记录。

## Cleanup Keep

- doc/tasks/20260808-pqc-first-patrol-equipment-diagnosis/pqc-first-patrol-equipment-diagnosis.e2e.cjs
