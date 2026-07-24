# 任务：展厅产品基础附件保存与查看修复

## 任务目标

修复展厅产品管理“基础”弹框中图片、视频、文本附件上传后的展示、查看和保存问题：

- 上传后在基础信息附件区显示真实文件名。
- 文件名可点击查看对应文件。
- 保存草稿或提交时携带附件列表，不再出现 `Cannot read properties of undefined (reading 'trim')`。

## Previous Task Check

- 已检查同仓库前序展厅附件任务：`doc/tasks/20260605-showroom-product-attachments/task.md`。
- 处理：该任务已标记 `completed`；本任务仅修复基础弹框附件展示/查看/保存回归。

## BDD 场景

- BDD: 附件上传后显示可点击文件名 -> Given 用户打开可编辑产品基础弹框 / When 上传图片、视频或文本附件成功 / Then 附件列表显示原始文件名，文件名可点击打开正式文件 URL。
- BDD: 附件保存不因可选字段缺失报错 -> Given 附件记录来自上传接口且可选字段可能为空 / When 用户点击保存草稿或提交 / Then 前端构建 payload 时不读取 undefined.trim，且请求体包含排序后的附件信息。
- BDD: 只读附件仍可查看 -> Given 产品基础弹框不可编辑 / When 附件列表存在文件 / Then 上传、排序和删除不可用，但文件名仍可点击查看。

## 里程碑

- [x] M1：检查前置任务状态并创建任务文档。
- [x] M2：复现并新增 RED 回归测试。
- [x] M3：实现附件字段归一化、文件名链接和保存 payload 修复。
- [x] M4：运行目标测试、类型检查和真实路径验证，记录证据。

## 预期验证

- `node scripts/showroom-product-attachments.test.mjs`
- `pnpm ts:check`
- Playwright 真实路径：登录测试租户，进入 `/showroom/product`，打开基础弹框，验证附件文件名可见可点击，保存不出现 `trim` 报错。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺正式文件 URL 或接口失败必须暴露，不生成假文件或静默保存成功。
- `是否从根因和长期维护角度解决`：是。通过统一附件字段归一化和保存 payload 契约解决 undefined 字段触发 trim 的根因。
- `是否存在临时补丁或绕过`：否。不绕过真实上传接口、不新增测试专用按钮、不改受保护文件配置。

## Current Status

completed

## 进展记录

- 2026-06-06：已实现附件 URL 契约、文件名点击预览、上传后保留 URL、保存 payload 归一化；附件脚本、TypeScript 检查和 diff 检查已通过。
- 2026-06-06：真实路径复核发现产品页未加载 `companyCurrent` 导致基础弹框无法打开，已补齐产品页公司当前版本加载并新增回归断言。
- 2026-06-06：真实上传复核发现 `request.upload` 返回 Axios 响应，附件上传 helper 未解包导致文件名和大小显示为 `undefined`，已修正为返回 `response.data`。
- 2026-06-06：目标测试、类型检查、后端契约测试、真实 Playwright 上传/点击/保存/重开验证均已通过。
