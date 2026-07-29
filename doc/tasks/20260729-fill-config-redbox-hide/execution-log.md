# Execution Log

- USER: 截图反馈“红框内的内容不显示”，目标为填写配置 / 辅助表单映射页面隐藏截图标注的顶部操作组、左侧原表单说明栏和中央辅助表单预览说明栏。
- RULES: 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`bug-regression-fix-loop` 与 `frontend-feature-delivery` 技能及其 evidence contract。
- PREFLIGHT: `docs/experience-index.md` 存在，适用门禁为前端静态契约隔离、PowerShell/UTF-8、无 fallback。
- BDD: 隐藏填写配置红框区域 -> Given 用户打开填写配置的辅助表单映射页面 / When 页面渲染原表格、辅助表格和右侧映射控制栏 / Then 顶部右侧操作组、左侧原表单说明栏和中央辅助表单预览说明栏不显示，右侧映射控制栏、辅助表格卡片和必要配置控件仍可见。
- M1: in_progress -> 正在定位截图对应组件和既有静态合同。
