# 任务：手动发布展厅带出展柜中英文描述

## 任务目标

修复 IntRuoyi showroom 手动发布展厅链路：点击手动发布展厅后，新生成的 Website release `website-index` 中 8 个展柜的 `description/descriptionEn` 必须带出展柜自身中英文描述，使 Website 展柜页右侧栏能显示展柜描述，而不是空描述。

## 前序任务检查

- Website 任务 `20260601-showroom-hall-description-right-panel` 已完成并提交，前台已经改为读取展柜 `description/descriptionEn`。
- IntRuoyi 上一任务 `20260601-e2e-build-release-yudao-admin` 已记录为 `blocked_on_running_release_operation`：运行控制操作仍为 `running`，本任务不改动其已有发布脚本与运行产物。
- 当前 IntRuoyi 仓库存在旧任务未提交改动：`script/deploy/publish-int-ruoyi.ps1`、`script/tests/test_publish_int_ruoyi_to_test_tooling.py`、`runtime/`、旧任务文档。本任务提交时不得混入这些无关改动。

## BDD 场景

BDD: 手动发布展厅输出展柜中英文描述 -> Given 8 个展柜已经配置中英文描述 / When 管理端点击手动发布展厅并生成 scoped release / Then release 的 `website-index.showrooms[*].description` 与 `descriptionEn` 必须包含对应展柜描述。

BDD: 缺少展柜描述失败快查 -> Given 发布源中任一展柜缺少中文或英文描述 / When 手动发布展厅 / Then 发布应暴露明确缺失字段错误，不得用空字符串、产品讲解或默认成功掩盖。

## 里程碑

- [x] M1：建立任务文档与 BDD 场景。
- [x] M2：定位手动发布展厅 release 组装逻辑与当前空描述根因。
- [x] M3：补充失败回归测试，复现 `description/descriptionEn` 为空或被吞掉。
- [x] M4：最小实现发布链路输出 8 个展柜中英文描述。
- [x] M5：运行目标测试、必要回归、真实发布/接口验证。
- [x] M6：记录证据、收尾清理预览并提交本任务改动。

## 预期验证

- RED: 目标 showroom release 发布测试先失败，证明展柜描述未进入 `website-index`。
- GREEN: 目标测试通过，8 个展柜 description/descriptionEn 均非空并匹配预期。
- REGRESSION: 运行受影响的 showroom release 测试。
- REAL VERIFY: 在本机测试环境触发手动发布展厅或调用等价后端发布入口后，读取 `GET /showroom/sites/yingtai-showroom/stages/TEST/release/current` 与 `website-index`，确认 8 个展柜中英文描述非空。

## 当前状态

Completed: 已确认当前手动发布生成的新 TEST scoped release `20260601T081746Z-be276b74dfa8-b111cad3b49c` 中 8 个展柜 `description/descriptionEn` 均非空；Website 端通过真实 release 映射后可在展柜讲解区域显示展柜描述。

## Current Status

completed: 当前手动发布生成的新 TEST scoped release 已带出 8 个展柜中英文描述，Website 端已通过真实 release 映射与渲染验证。

## 阻塞

None.
