# Bug Regression Evidence

## Bug Summary

批次执行详情页的辅助模式将辅助表格映射扁平化为字段卡片列表，无法复现配置页的责任主体和行列布局。

## Expected Behavior

详情页辅助模式按正式辅助格 rowKey 渲染只读网格，保留责任主体、配置坐标和未映射格子。

## Reproduction

- 打开批次执行详情。
- 选择已配置辅助表格的工序。
- 切换右侧“辅助模式”。
- 当前结果为字段列表；目标结果为配置页同构网格。

## Root Cause

详情页仅标准化 `assistRows` 中映射的原表字段，然后直接遍历 `selectedPreviewAssistFields` 生成卡片列表；没有解析辅助格 rowKey、没有按责任主体分组，也没有生成未映射格子。

## Regression Test

`IntRuoyiFronted/tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js`

## RED

`node tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js` -> FAIL，缺少辅助格 rowKey 解析与网格模型。

## GREEN

Pending.

## Risk And Scope

- 仅影响批次详情只读辅助预览。
- 不改变保存、提交、签名、上传、API 或后端数据契约。

## Blockers

None currently.
