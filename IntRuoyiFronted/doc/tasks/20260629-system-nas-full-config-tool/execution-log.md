# 执行日志：系统 NAS 配置工具扩展为完整连接参数台（前端）

- BDD: NAS 管理页展示完整 SMB 参数表单 -> Given 用户打开 /system/nas / When 页面加载成功 / Then 页面除基础 4 项外，还应展示新增的 domain、port、连接模式等正式参数输入项。
- BDD: 提交与测试连接复用同一完整参数对象 -> Given 用户填写完整参数 / When 点击保存或测试连接 / Then 前端提交的请求体都携带同一组完整 NAS 参数。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs` -> FAIL，静态门禁要求页面出现 `NAS 端口`、`域` 和扩展 `NasConfigVO` 后，当前页面仍只有 4 项基础参数。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs` -> PASS，确认前端 API 契约与 `/system/nas` 页面已补齐 `port/domain`，且保持现有 NAS 管理页交互与结构。
- IMPLEMENTATION: `src/api/system/nas/index.ts` -> `NasConfigVO` 新增 `port:number`、`domain:string`。
- IMPLEMENTATION: `src/views/system/nas/index.vue` -> 新增 `NAS 端口`、`域` 表单项，默认端口 `445`，并将新增字段纳入 `hasCompleteNasConfig` 与校验规则。
