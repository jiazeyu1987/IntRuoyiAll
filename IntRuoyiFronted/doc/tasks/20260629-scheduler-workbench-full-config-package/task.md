# 任务：排产员工作台全量数据包按钮

- Task ID: `20260629-scheduler-workbench-full-config-package`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

在排产员工作台增加“导出全部数据包 / 导入全部数据包”按钮，接入后端正式聚合接口，让用户通过单个文件完成工作台所需全量配置迁移。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-system-nas-config-hide-empty-values\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成，当前新任务只在排产员工作台页面和对应 API 合同内增补全量包入口，不扩散到其他页面。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 前端静态脚本、任务文档和日志统一 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持当前排产工作台白底紧凑操作台风格，只在现有按钮区增补动作，不做无关重排。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按钮直接接正式聚合接口，不在前端串多个下载或上传请求伪装“全量包”。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工作台显示全量包按钮 -> Given 用户进入排产员工作台 / When 页面渲染设置区 / Then 可见导出全部数据包与导入全部数据包按钮。`
- `BDD: 工作台保留独立文件选择器合同 -> Given 用户点击导入全部数据包 / When 页面准备选择文件 / Then 页面仍通过隐藏文件选择器接收单个数据包文件并触发导入。`
- `BDD: 前端 API 指向正式聚合接口 -> Given 工作台执行全量包导出或导入 / When 前端发起请求 / Then 请求命中新的后端聚合接口，而不是前端自行串调多个已有接口。`

## Milestones

1. M1：创建任务文档并锁定按钮与 API 合同。`completed`
2. M2：补 RED 静态检查。`completed`
3. M3：实现页面按钮与 API 接入。`completed`
4. M4：运行静态验证并记录结果。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-scheduler-workbench-route-import-export-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-scheduler-workbench-static.spec.js`

## Final Result

- 排产员工作台设置区已新增“导出全部数据包 / 导入全部数据包”按钮，并保留现有紧凑操作台布局。
- 前端已新增 full-config API 合同与独立隐藏文件选择器，导入成功后会反馈用户角色绑定与分配角色数量。
- 前端静态验证已通过：
  - `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-scheduler-workbench-route-import-export-static.spec.js` -> PASS
  - `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-scheduler-workbench-static.spec.js` -> PASS
- 本机 `芋道源码/admin` 真实按钮链路已通过：
  - 点击“导出全部数据包”命中 `GET /admin-api/mes/pro/scheduler-workbench/full-config/export`，HTTP `200`
  - 点击“导入全部数据包”命中 `POST /admin-api/mes/pro/scheduler-workbench/full-config/import`，HTTP `200`
  - 页面 toast：`导入完成；用户角色绑定 27 条；分配角色 41 条`
- 真实导出文件与验证截图：
  - `D:\ProjectPackage\Int\IntRuoyi\output\playwright\admin-scheduler-workbench-full-config-export.json`
  - `D:\ProjectPackage\Int\IntRuoyi\output\playwright\admin-scheduler-workbench-full-config-import-result.png`
