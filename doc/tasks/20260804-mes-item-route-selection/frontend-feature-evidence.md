# Feature

MES 物料产品维护页新增产品侧“工艺路线”页签，允许产品查看、选择、变更或解除正式工艺路线绑定；关系源只使用 `mes_pro_route_product`。

## Acceptance

- MES 物料产品编辑/详情页在产品项下展示“工艺路线”页签。
- 前端调用 route-product 查询/保存接口，不在 `MdItemApi` 增加 `routeId` 或第二套关系字段。
- 产品侧路线选项使用产品维护权限下的专用列表接口；已启用路线只回显并禁用变更，未启用路线可选择。
- 不使用 `formBindings`、表单槽位、批记录表单或工序开始链路。

## BDD

- BDD: 产品侧查看当前工艺路线 -> Given 产品已有 route-product 绑定 / When 打开 MES 物料产品编辑页 / Then 工艺路线页签回显当前路线。
- BDD: 产品侧选择工艺路线 -> Given 产品未绑定路线且存在未启用路线 / When 用户选择路线并保存 / Then 前端提交 `itemId + routeId` 到 route-product API。
- BDD: 已启用路线只回显不可维护 -> Given 当前绑定路线已启用 / When 打开产品侧工艺路线页签 / Then 页面显示锁定提示并禁用变更入口。

## RED

- RED: `node tests/e2e/mes-md-item-route-selection-static.spec.js` -> FAIL, expected reason: 缺少独立 `MdItemRouteForm` 与产品侧路线绑定入口。

## GREEN

- GREEN: `node tests/e2e/mes-md-item-route-selection-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Verification

- 静态契约覆盖 `MdItemForm.vue` 产品专属页签、`MdItemRouteForm.vue` 查询/保存调用、`MdItemApi` 不新增路线字段、禁用 `getRouteSimpleList`、禁用工艺路线三类配置混用。
- 类型检查覆盖新增 Vue SFC、API wrapper 和常量引用。

## Blockers

- 无前端实现阻塞。
