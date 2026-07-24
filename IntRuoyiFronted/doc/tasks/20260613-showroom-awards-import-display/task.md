# 20260613 展厅奖项管理前端

## 任务目标

在展厅产品管理页面增加 `奖项` 页签，展示、编辑、发布奖项资料，并让展柜选择器与画布布局支持产品和奖项混合展项。

## 前置任务检查

- 最新目录 `20260612-edhr-attachment-prepare-upload-api` 未包含 `task.md`，视为非标准任务文档目录。
- 最近有效前端任务文档：`20260612-report-recognition-select-word-file`。
- 状态：`COMPLETED`。
- 结论：允许开始本任务，并记录最新目录缺少任务文档的事实。

## 里程碑

1. M1 审计：确认产品管理页面、导入弹窗、展柜选择器、画布布局和 API 类型。
2. M2 RED：新增奖项页签、导入结果和混合展项 payload 的前端测试。
3. M3 GREEN：实现奖项表格、编辑弹窗、导入结果展示、展柜混合项选择与布局保存。
4. M4 REGRESSION：运行目标 node 测试、类型检查和必要 UI 验证。
5. M5 收尾：记录证据、运行 task-closeout-cleanup 预览并提交本任务改动。

## 预期验证

- 产品管理内可切换 `产品` / `奖项`。
- 导入结果展示产品与奖项统计，额外图片 warning 不被吞掉。
- 展柜选择器提交 `items`，每项携带 `itemType` 与 `itemId`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；接口错误直接展示，不用空数据伪装成功。
- `是否从根因和长期维护角度解决`：是；前端类型与后端混合展项协议一致。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：奖项页签、奖项编辑弹窗、奖项中英文语音生成/预览入口、导入统计字段、奖项 warning 展示、展柜混合展项选择和画布 payload；奖项发布前强制要求中英文音频并提交音频来源修订版 `revisionId`；展柜列表、选择器和画布统一读取/保存 `itemMappings` / `items`。
- 验证证据：`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 通过；`node tests\e2e\showroom-award-audio-static.spec.js` 通过；默认 heap 下 `pnpm ts:check` 曾因 `vue-tsc` OOM 退出 134，使用 8GB heap 后通过。
- 真实 E2E：测试租户 `aoteman` 真实登录后奖项 `AWARD-001` 生成中文语音文件 `9198354891941`、英文语音文件 `9198354891942`，保存并发布修订版 `50`；展柜 `hall_id=10` 保存奖项 `AWARD-001`，画布截图 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\showroom-hall-award-canvas-saved.png`；公司信息工作台真实点击 `手动发布展厅` 发布 release `20260614T055216Z-cdf9733a057e-21bd7d57e98a`。
- 最终结论：管理端奖项导入、编辑、语音门禁、奖项发布、展柜混合选择与发布入口均已真实验证通过，无剩余阻塞。
