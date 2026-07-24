# 执行日志：NAS 配置页只显示当前已设置值

- BDD: 已设置字段显示真实值 -> Given 后端返回某个 NAS 字段已有真实配置值 / When 页面加载配置 / Then 该字段显示当前值，而不是空占位。
- BDD: 未设置的可选字段不显示 -> Given 后端返回某个可选 NAS 字段为空 / When 页面加载配置 / Then 页面不渲染该输入项，不显示空值或仅占位提示。
- BDD: 未设置的可选字段默认收起但仍可主动补录 -> Given 当前 NAS 可选字段没有真实配置值 / When 用户需要补录端口或域 / Then 页面提供显式展开入口，展开后可填写并保存可选参数。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs` -> FAIL，当前 `port` 默认显示 `445`，`domain` 始终渲染且使用“可选，默认留空”占位，未满足“没设置就不显示”。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs` -> PASS，确认 `port` 仅在当前配置存在真实值时显示，`domain` 仅在有值时渲染，页面不再显示空占位。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs` -> PASS，确认页面新增“补充连接参数”展开入口；未设置的 `port/domain` 默认不显示，但仍可主动展开补录并参与保存、测试连接。
- IMPLEMENTATION: `src/views/system/nas/index.vue` -> 新增“补充连接参数”入口、`shouldShowOptionalFields`、`shouldShowPortField`、`shouldShowDomainField`、`buildNasConfigPayload`；未设置可选参数默认收起，展开后可补录；提交与测试连接仅按当前展示语义发送可选字段。
