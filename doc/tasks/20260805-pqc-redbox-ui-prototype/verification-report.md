# PQC 红框区域 UI 原型设计验证报告

## Scope

本轮验证仅覆盖独立 HTML 原型结构、中文编码和任务范围边界，不覆盖正式 PQC Vue 页面、不运行前端构建、不执行真实 E2E。

## Result

- 原型文件已创建：`doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.html`。
- 预览截图已生成：`doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.png`。
- 原型采用与 PQC 操作端一致的视觉语言：浅绿背景、白色工作面、3px 低饱和边框、20px 圆角、粗体大字号、深绿激活态。
- 红框旧控件被重新组织为同一信息层级的“质检信息条”：检验设备、设备编号、接收标准、检验方法。
- 按用户追加截图反馈，左侧检验内容区已扩大，右侧填检验区已缩小。
- 按用户确认后的建议，质检信息条已从一行四格改为两行两列，优先保证内容显示完整，避免整体缩小为难以触控的字体。
- 按用户第三次反馈，左侧“长度/外观/密封/压力”已改为页签式切换，只展开当前检验项详情，不再纵向展开多个列表卡片。
- 按用户第四次反馈，顶部原型说明条、左侧“检验内容”标题和右侧“填检验”标题已移除。
- 按用户第五次反馈，当前检验项详情不再用一张外层卡片包裹，已融入左侧面板背景，仅保留内部独立触控控件。
- 未修改正式 Vue/TS/CSS 源码。

## Verification

- `python -X utf8` 读取任务目录文件：PASS。
- 原型关键结构断言：PASS。
- Edge headless 截图生成：PASS。
- 调整后左右比例 CSS 断言：PASS。
- 两行两列质检信息条结构断言：PASS。
- 检验项页签结构断言：PASS。
- 黄框内容移除断言：PASS。
- 当前项详情融入背景断言：PASS。
- `git diff --check -- doc/tasks/20260805-pqc-redbox-ui-prototype`：PASS。
- `task_closeout.py --task-id 20260805-pqc-redbox-ui-prototype --mode preview/apply`：PASS，未删除任何交付物。

## Follow-Up

如用户认可该方向，下一步应在正式 `FrontlineFixedTemplatePanel.vue` 中以最小范围改造 `.frontline-pqc-equipment-controls` 与 `.frontline-pqc-fact-actions`，并新增静态合同覆盖红框区域不再出现未样式化原生 select/button。

## Closeout Blocker

项目级 Git 收尾未完成：当前 `int_main` 已领先 `origin/int_main` 13 个提交且存在大量非本任务脏改动。本轮未提交或推送，避免把并发任务改动混入当前 HTML 原型任务。
