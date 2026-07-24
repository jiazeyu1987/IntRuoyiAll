# 20260602 运行控制台构建发布包并部署测试服媒体验证

## Task Goal

在用户明确授权访问测试服后，确认当前本机前后端运行状态为最新，通过真实 E2E 操作运行控制台构建发布包，使用构建出的发布包部署到测试服，并确认测试服展厅产品管理中的封面图片与中英音频没有加载失败。

## Milestones

- [x] M1: 建立任务记录并确认测试服授权、入口、账号和当前工作区状态。
- [x] M2: 确认本机前后端运行状态对应当前仓库最新提交，必要时重启。
- [x] M3: 通过 Playwright 真实操作运行控制台构建发布包并记录发布包标识。
- [x] M4: 部署该发布包到测试服并确认发布命令成功完成。
- [x] M5: 通过真实浏览器和 HTTP 抽样验证测试服产品管理封面图片与中英音频可加载。
- [x] M6: 记录完整验证证据，执行收尾清理并按任务范围提交。

## Expected Verification

- 本机前端 `http://localhost:8081` 与后端 `http://127.0.0.1:48081/actuator/health` 可访问。
- 运行控制台 E2E 构建发布包成功，得到明确 `releaseTag` / 包目录。
- 使用同一 `releaseTag` 部署到测试服成功，测试服 `http://172.30.30.58:48081/actuator/health` 与 `http://172.30.30.58:8081/` 可访问。
- Playwright 登录测试服产品管理页，页面 `加载失败` 数量为 0，封面图片无 failed request 且 `naturalWidth > 0`。
- 抽样产品中英音频通过 `8081/admin-api/infra/file/28/get/...` 返回 `HTTP 200` 与音频 Content-Type。

## Current Status

completed

## Notes

- 用户已在当前任务明确授权访问测试服。
- 正式服不在本任务范围内，不访问、不发布、不验证。
- 仓库已有未跟踪 `runtime/`，本任务不纳入。
- 本机后端 HEAD: `97695bebc486d0c3061529d03316d55e9387c278`，前端 HEAD: `d8606ffe12d6632d322efb01903ed36403710c45`。
- 2026-06-02 00:10 本机后端 `http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`，前端 `http://localhost:8081/` 返回 `HTTP 200 OK`。
- 构建发布包操作号：`37ce0b94-8327-4b46-89a3-401dd8787d51`；发布包编号：`26-06-02 00:12:30`；NAS 路径：`Backup/ReleasePackage/26-06-02_00-12-30`。
- 部署测试服操作号：`7d586c0a-d777-4c7c-8f6a-71296203338e`；部署参数 `releaseTag=26-06-02 00:12:30`；发布日志结果 `Publish completed for test.`。
- 测试服产品管理验证使用测试租户 `测试租户/aoteman`；第一页 `product_001` 至 `product_020` 共 20 条产品，20 张封面 `HTTP 200 image/png` 且无 `加载失败`；20 个中音频状态和 20 个英音频状态全部 `OK`。
- 音频抽样 `product_001` 至 `product_005`，中英共 10 条；前端代理全部 `HTTP 200 audio/vnd.wave`，MinIO 直链全部 `HTTP 200 audio/x-wav`。
- `infra_file_config.id=28` 当前为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`、`has_localhost=0`。
- 收尾清理预览仅删除本任务 `artifacts/` 下的临时验证脚本、日志、截图和 JSON；核心任务记录保留，无 blocked/warnings。
