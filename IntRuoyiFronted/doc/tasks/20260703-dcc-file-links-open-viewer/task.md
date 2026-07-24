# DCC 文件链接统一跳转受控预览页

## 任务目标

将 DCC 相关页面中用户点击文件链接、文件编号或文件详情入口的行为统一改为打开受控预览页，不再跳普通文件详情页。目标复用现有 `buildControlledFileViewerPath(id, from, returnTo)` viewer 模式，并保留来源页面返回路径。

## 经验门禁

- PowerShell/Windows 命令：已读取根仓 `docs/powershell-memory.md`，本轮命令设置 UTF-8 输入输出，不使用 `&&`。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次只改链接行为与静态契约，不做视觉重构。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，按 BDD + RED/GREEN 记录证据。
- 收尾清理：已读取 `task-closeout-cleanup` 与 closeout 规则，完成后运行 preview。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，抽出共享 viewer 导航 helper，统一入口行为。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 项目代码关联文档打开受控预览 -> Given 用户在 DCC 项目代码详情抽屉点击关联文档文件名 / When 点击文件链接 / Then 当前页跳转到带 `viewer=1` 的受控预览页，并带 `returnTo` 返回当前页面。
- BDD: DCC 文件列表入口统一预览 -> Given 用户在受控浏览、我的文件、审计、签名、培训、工作台等页面点击文件链接 / When 点击文件编号或文件详情入口 / Then 不进入普通文件详情页，而进入受控预览页。
- BDD: 详情页内部流程操作保留详情 -> Given 用户在文件详情页内点击版本历史或撤回重提生成的新流程 / When 执行内部流程类跳转 / Then 仍保留普通详情页跳转以承载流程操作。

## 里程碑

1. 建立任务文档与经验门禁。- 已完成
2. 补充/更新静态 RED 契约覆盖统一 viewer 跳转。- 已完成
3. 实现共享 viewer 导航 helper 并替换文件链接入口。- 已完成
4. 运行静态验证并记录 GREEN。- 已完成
5. 收尾预览并提交本任务前端改动。- 已完成

## 预期验证

- 受影响静态测试先 RED 后 GREEN。
- 覆盖项目代码关联文档、受控浏览文件编号、我的文件、审计、签名、培训、工作台和 BPM DCC 审批入口。
- 不修改后端接口、不操作服务器、不做真实数据写入。

## 当前状态

- 状态：completed
- 当前里程碑：已完成。

## 实现结果

- 新增共享 helper `src/views/dcc/controlled-file/shared/viewer-navigation.ts`，统一用 `buildControlledFileViewerPath(id, from, route.fullPath)` 生成受控预览导航。
- 项目代码关联文档、受控浏览文件编号、我的文件、审计、签名、培训、工作台、BPM DCC 审批入口已改为跳受控预览页。
- 详情页内部版本历史、撤回重提新流程等需要承载详情操作的普通详情页跳转保持不变。

## 验证结果

- RED：旧静态契约在实现前失败，首先暴露 `dcc-audit-file-detail-link-static.spec.js` 仍要求跳普通详情页。
- GREEN：受影响静态契约全部通过，包括 audit、browser、mine、project-code、signature、training、workbench、bpm DCC approval。
- GREEN：`node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` 通过。
- GREEN：`task_closeout.py --task-id 20260703-dcc-file-links-open-viewer --mode preview` 通过，无 blocked/warnings。
