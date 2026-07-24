# DCC项目代码关联文件数排序

## 任务目标

在 DCC 项目代码列表中显示每个项目代码关联的受控文件数量，并支持按关联文件数升序、降序排序。关联文件数以 `dcc_controlled_file.dcc_project_code_id = dcc_project_code.id` 为准，未关联文件不计入具体项目，项目代码无关联文件时显示 0。

## 经验门禁

- PowerShell/Windows 命令：已按 `docs/powershell-memory.md` 使用显式 UTF-8 输入输出，不使用 `&&`。
- DCC 真实链路与 E2E：如执行真实 E2E，需先读取登录与 E2E 凭据文档并使用真实测试租户路径。
- 前端样式：保持 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 和现有文控基础数据列表风格，不做视觉重构。
- 测试数据：后端/前端测试必须表达真实业务行为，不用 mock 成功掩盖缺陷。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接在项目代码分页查询中提供可排序的关联文件计数字段。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 项目代码列表显示关联文件数 -> Given 项目代码 A 关联 2 个受控文件且项目代码 B 未关联文件, When 用户打开 DCC 项目代码列表, Then A 显示关联文件数 2 且 B 显示 0。
- BDD: 项目代码列表按文件数排序 -> Given 多个项目代码拥有不同关联文件数, When 用户按关联文件数升序或降序排序, Then 后端分页结果按该数量正确排序且前端向列表接口传递排序参数。

## 里程碑

1. 建立任务文档与 BDD/TDD 记录。
2. 添加失败测试覆盖关联文件数展示与排序。
3. 实现后端关联文件数统计与排序参数。
4. 实现前端列表列展示和排序请求。
5. 运行验证并提交本任务改动。

## 预期验证

- 后端定向测试：项目代码分页返回 `associatedFileCount`，支持升序/降序排序。
- 前端静态测试：DCC 项目代码基础数据列表包含“关联文件数”可排序列，并将排序参数传给接口。

## 当前状态

已完成：后端分页返回关联文件数并支持 fileCountSort 升序/降序；前端 DCC 项目代码列表新增“关联文件数”列并支持后端排序；定向后端与前端静态契约验证通过。

## 完成记录

- 状态：已完成。
- 后端验证：`mvn -pl yudao-module-dcc -Dtest=DccProjectCodeServiceImplTest test` 通过，11 tests。
- 前端验证：`node tests/e2e/dcc-project-code-basic-data-static.spec.js` 通过。
- 证据校验：backend-api-delivery 与 frontend-feature-delivery evidence 均通过。
