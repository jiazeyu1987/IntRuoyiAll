# Verification Report

## Scope

- MES 物料产品维护页新增产品专属“工艺路线”页签。
- 产品侧查询、选择、变更和解除工艺路线绑定复用 `mes_pro_route_product`。
- 后端按 `itemId` 提供查询/保存接口，并保持单产品唯一路线绑定语义。

## Results

- `node tests/e2e/mes-md-item-route-selection-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- `mvn -pl yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test`：PASS，10 tests，0 failures，0 errors。
- `git diff --check -- <task-owned files>`：PASS，仅有 LF/CRLF 工作区换行提示。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-mes-item-route-selection/frontend-feature-evidence.md`：PASS。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-mes-item-route-selection/backend-api-evidence.md`：PASS。

## Notes

- 前端未在 `MdItemApi` 增加 `routeId` 或第二套路线关系字段。
- 产品侧解除路线传递 `routeId=null`，由后端删除正式 route-product 绑定和旧路线产品 BOM 关联。
- 产品侧路线下拉使用 `/mes/pro/route/item-binding-list`，避免 `simple-list` 只返回已启用路线而与后端“已启用路线不可维护”校验冲突。
- 已启用路线可作为当前绑定回显，但前端禁用变更入口；后端继续 fail fast，不引入降级。
- 未引入 fallback、默认成功或表单槽位/批记录链路替代关系源。
