# PQC 红框区域 UI 原型设计

## Task Goal

基于用户截图中 PQC 填写页“检验内容”卡片内红框区域，先完成独立 HTML 原型设计，探索与现有一线操作端大字号、绿色边框、触控式按钮风格一致的替代 UI；本轮不修改正式 Vue 页面。

## Milestones

- [x] 定位 PQC 页面现有入口与红框区域源码。
- [x] 读取前端样式与任务规则，识别适用设计约束。
- [x] 产出独立 HTML 原型文件。
- [x] 完成结构性验证与 UTF-8 读取验证。

## Expected Verification

- 使用 UTF-8 方式读取原型与任务文档，确认中文无乱码。
- 静态检查原型包含推荐设计的关键结构：设备选择、设备编号、接收标准、检验方法、逐件选择。
- 不运行前端构建或 E2E；本轮仅为 HTML 原型，不触碰正式 Vue/TS 实现。

## Current Status

blocked

原型交付物已完成并通过静态验证；项目级 Git 收尾仍被阻塞，因为当前 `int_main` 已领先 `origin/int_main` 13 个提交且存在大量非本任务脏改动。为避免混入并发任务改动，本轮未提交或推送。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；本轮仅做静态原型，不引入运行时代码路径。
- 是否从根因和长期维护角度解决：是；根因是红框内原生控件密度和视觉语言脱离 PQC 操作端主风格，原型改为同源的触控式信息条。
- 是否存在临时补丁或绕过：否；原型独立保存，未改正式页面。

## Applicable Gates

- 前端样式门禁：红框区域属于前端页面局部 UI 设计，需遵循现有 PQC 操作端的粗边框、大字号、绿色操作台风格，不扩大成整页重设计。
- UTF-8 门禁：任务文档和 HTML 原型包含中文，读写必须保持 UTF-8。
- Git 门禁：主仓已有大量既有脏改动与 ahead 状态，本任务只新增当前任务目录文件，不提交或推送既有并发改动。

## Cleanup Keep

- doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.html
- doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.png
