# 20260603-website-assets-cache-autoupdate

## 任务目标

在保留自动更新能力的前提下恢复并固化 Website 静态资源缓存策略：HTML 入口继续 `no-store`，带 hash 的 `/assets/` JS/CSS 资源必须强缓存，发布后读回门禁必须验证该缓存头。

## BDD 场景

BDD: hashed assets 可长期缓存且自动更新 -> Given Website 入口 HTML 每次重新读取 / When 入口引用新的 hash 资源文件名 / Then 浏览器可长期缓存旧 hash 资源，同时新发布通过新的 hash URL 自动更新。

BDD: 发布后必须验证 assets 缓存头 -> Given `publish-int-ruoyi.ps1 -Mode deploy-release -Component website` 已重建 Website / When 脚本读回入口 JS bundle / Then bundle 响应必须包含 `Cache-Control: public, max-age=31536000, immutable`，否则发布失败。

## 里程碑

- [x] M1：创建任务记录并确认上个任务已完成。
- [x] M2：补充 RED 测试，证明当前 `/assets/` 缺少 immutable 缓存合同。
- [x] M3：更新 Nginx 模板与发布读回门禁。
- [x] M4：运行目标测试并通过真实发布包部署测试服与正式服。
- [x] M5：更新任务记录、运行 closeout 预览/清理并提交。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "asset_cache or entry_bundle" -q`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q`
- 测试服与正式服 `/assets/index-*.js` 返回 `Cache-Control: public, max-age=31536000, immutable`。

## Current Status

completed

## 最终结果

已保留 HTML 入口 `no-store`，并为 `/assets/` 下的 hash JS/CSS 增加 `Cache-Control: public, max-age=31536000, immutable`。自动更新逻辑依赖入口 HTML 每次重新读取并引用新的 hash 文件名；旧 hash 文件可以安全长期缓存。

发布读回门禁已同步检查入口 JS bundle 的 immutable 缓存头，缺失时发布失败。

## 最终验证结果

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，46 passed。
- 发布包 `20260603_website_assets_cache_immutable` 已部署测试服并通过 read-back gate。
- 同一 tested 发布包已部署正式服。
- 正式服 `/` -> `Cache-Control: no-store, no-cache, must-revalidate, max-age=0`。
- 正式服 `/assets/index-B1lPB_BO.js` 与 `/assets/index-DbLfBTKE.css` -> `Cache-Control: public, max-age=31536000, immutable`。
