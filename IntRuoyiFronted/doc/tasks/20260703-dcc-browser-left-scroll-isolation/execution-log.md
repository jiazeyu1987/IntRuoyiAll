# 20260703 DCC 受控浏览左侧目录独立滚动修复

BDD: 左侧目录独立滚动不影响右侧列表 -> Given DCC 受控浏览页左侧目录树内容超过可视高度 / When 用户在左侧目录区域滚动或展开长目录 / Then 滚动应被限制在左侧目录容器内 / And 右侧文件列表区域不应被左侧内容撑高或带动滚动。

RED: static contract before layout fix -> FAIL, old browser page did not have bounded `browser-page-layout`, `browser-directory-scroll`, or card-body flex/overflow constraints.

GREEN: node tests/e2e/dcc-browser-tree-expand-scroll-static.spec.js -> PASS

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check -> PASS
