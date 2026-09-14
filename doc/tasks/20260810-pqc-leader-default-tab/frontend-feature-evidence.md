# Frontend Feature Evidence

## Feature Goal

点击“PQC组长”菜单进入独立工作台时，默认展示“PQC管理”模块。

## Non-Goals

- 不调整动态菜单、后端权限、PQC 人员管理接口或生产组长默认页签。
- 不新增 fallback、兼容分支或隐藏式默认状态。

## Requirements And Acceptance

- REQ-1: PqcLeaderWorkbenchPage.vue 继续以 leader-type=PQC 和 show-pqc-module-tabs=true 进入共享工作台。
- REQ-2: PQC 模块页签默认值为 management。
- REQ-3: “人员管理”页签仍可点击进入，人员列表 gate 不变。
- REQ-4: 生产组长模块默认值不受影响。

## UI Entry Points

- Sidebar menu: PQC组长
- Wrapper: src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue
- Shared component: src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue

## API Contracts And Data States

- 本次仅修改前端初始化状态，不改变 API wrapper、请求参数、权限或数据结构。
- management 模块继续复用现有 PQC 管理列表加载逻辑。

## BDD Scenarios

- Given 用户从左侧菜单进入“PQC组长” / When 页面初始化 / Then 默认激活 “PQC管理”。
- Given 用户在“PQC组长” / When 点击“人员管理” / Then 人员管理列表仍可见。
- Given 用户进入“生产组长” / When 页面初始化 / Then 生产组长默认页签保持现状。

## RED Command

- pending

## GREEN Command

- pending

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- 本次不改布局和接口状态；静态合同锁定 Element Plus tab key 与 gate，不放宽权限或错误处理。

## E2E Or Component Verification Path

- 目标静态合同：tests/e2e/pqc-leader-default-management-tab-static.spec.js
- 相邻静态合同：tests/e2e/pqc-leader-module-tabs-static.spec.js、tests/e2e/pqc-leader-personnel-tab-static.spec.js

## Blockers And Follow-Up Skills

- none
