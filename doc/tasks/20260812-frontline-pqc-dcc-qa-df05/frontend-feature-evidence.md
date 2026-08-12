# Frontend Feature Evidence - DF05

## Scope

QA regulation page API contract and DCC project code QA status column static contracts.

## Contract

- QA management uses dccProjectCodeId directly.
- DCC project list uses one current-page project-statuses batch request.
- Frontend must not construct QA requests from product, route, or MES process identities.

## BDD

- BDD: DCC直接管理QA -> Given 用户选择一个DCC项目代码, When 在QA管理页保存和发布规程, Then payload只包含dccProjectCodeId及完整rules/processes/items字段，不包含product/route/MES process身份，且同一DCC只形成一份QA规程。
- BDD: DCC列表批量组合QA状态 -> Given DCC项目代码列表当前页有多条项目, When 页面加载或翻页筛选, Then 前端仅按当前页dccProjectCodeId批量调用MES project-statuses并合并状态，过期响应不得覆盖新页。

## Verification

- RED: pending
- GREEN: pending

## Validation

- pending

## Blockers

- none
