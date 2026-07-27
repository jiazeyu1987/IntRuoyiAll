# Verification Report

## Scope

将表单模板“打开 / 编辑 / 填写”的页面级行为与批记录管理对齐，同时保持 FormCenter 与 MES 批记录数据领域完全独立。

## Root Cause

上一轮仅把三个弹窗改成 `index.vue` 内部条件工作区，并使用自定义 `mode=workspace`。批记录管理实际使用 DesignerWrapper 页面模式和独立模拟填写页面，因此交互壳层仍不一致；模拟填写初版还会因新旧页面实例同时响应路由而重复加载模板版本。

## Implemented Behavior

- `打开/编辑`使用当前 `/mdm/form-center/template` 路由的 `mode=designer`，分别携带 `templateMode=preview|edit`。
- 新增 FormCenter 专属 `FormTemplateDesignerWrapper.vue`。
- `填写`进入独立 `FormTemplateSimulatePage.vue`。
- 三个动作都按 `templateId + versionNo` 精确读取模板版本。
- 独立填写页通过显式 `simulationOnly` 属性隔离，避免旧列表实例重复请求。
- 删除不再使用的 `TemplateViewDialog.vue`。
- 未引入 `reportId`、批记录绑定状态、MES 路由或批记录模拟 API。

## Verification

- 聚焦三按钮静态合同：PASS。
- 独立领域静态合同：PASS。
- 表单/批记录顶部页签相邻合同：PASS。
- ESLint：PASS。
- Vue SFC 编译检查：PASS。
- `pnpm ts:check`：PASS。
- `git diff --check`：PASS。
- BPM 定向 Maven 回归：PASS，13 tests（本任务前序已完成，本次未改后端）。
- 宽合同：本任务相关断言通过，仅剩无关策略菜单 `activeMenu` 历史断言失败。

## Real E2E

- 入口：`http://127.0.0.1:8081/mdm/form-center/template`。
- 身份标签：`芋道源码/admin`。
- 样本：`templateId=28`，`versionNo=V3.0`。
- 打开：进入 `mode=designer&templateMode=preview`，显示只读 DesignerWrapper。
- 编辑：进入 `mode=designer&templateMode=edit`，显示规则编辑工作区。
- 填写：进入 `/mdm/form-center/template/simulate`，显示独立模拟填写工作区。
- 三个动作均只请求一次精确模板版本接口。
- 可见弹窗 0，FormCenter 写请求 0，批记录绑定错误 0，console/page error 0。

## Result

功能与用户要求一致，状态为 `ready_for_closeout`。剩余工作仅为任务经验沉淀、cleanup、提交和推送。
