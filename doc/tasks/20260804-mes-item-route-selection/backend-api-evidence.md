# Scope

后端新增产品侧工艺路线绑定接口，并复用现有 `MesProRouteProductService` 与 `mes_pro_route_product` 权威关系。

## Contract

- `GET /mes/pro/route-product/get-by-item?itemId=<id>`：按 MES 产品物料编号返回当前 route-product 绑定，权限 `mes:md-item:query`。
- `POST /mes/pro/route-product/save-by-item`：保存 `{ itemId, routeId }`；`routeId=null` 表示解除绑定，权限 `mes:md-item:update`。
- `GET /mes/pro/route/item-binding-list`：产品维护页路线选项接口，权限 `mes:md-item:query`，返回路线 `id/code/name/status`。

## Validation

- `itemId` 必填；缺失时按 Bean Validation fail fast。
- 新增、迁移和解除绑定都调用 `validateRouteNotEnable`，已启用路线不允许维护。
- 迁移或解除绑定会删除旧路线的产品 BOM 关联，避免旧 route-product BOM 残留。
- 不新增数据库字段，不改变 `MesMdItem` 主数据接口关系源。

## BDD

- BDD: 产品侧查询路线绑定 -> Given 产品已有 route-product 绑定 / When 调用 get-by-item / Then 返回同一正式绑定。
- BDD: 产品侧保存路线绑定 -> Given 产品未绑定路线 / When 调用 save-by-item / Then 插入默认 route-product 绑定。
- BDD: 产品侧更换路线绑定 -> Given 产品绑定路线 A / When 保存路线 B / Then 更新同一绑定并清理路线 A 的产品 BOM。
- BDD: 产品侧解除路线绑定 -> Given 产品已有绑定 / When 保存 `routeId=null` / Then 删除绑定和旧产品 BOM。

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> INTERRUPTED, expected reason not confirmed because command remained in long MES javac before surefire report.

## GREEN

- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS, Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。

## Verification

- `MesProRouteProductServiceImplTest` 覆盖产品侧新增默认绑定、迁移保留路线侧参数、迁移清理旧 BOM、解除绑定清理旧 BOM。
- `git diff --check -- <task-owned files>` PASS，仅有 LF/CRLF 工作区提示。

## Blockers

- 无后端实现阻塞。
