# 任务：修复展厅图片再次不可用

## 任务目标

- 诊断并修复本机展厅图片再次不可用的问题。
- 优先确认受保护默认文件配置 `infra_file_config.id=28`、bucket `yudao`、domain `http://127.0.0.1:9000/yudao` 是否漂移。
- 禁止用临时回填、改写默认展厅媒体 URL、切换 bucket/domain 或 mock 成功掩盖问题。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：待诊断后确认；如发现受保护配置或媒体 URL 漂移，必须先阻塞并记录，除非用户明确授权修复范围。
- 是否存在临时补丁或绕过：否

## BDD 场景

- BDD: 展厅默认文件配置必须保持受保护值 -> Given 本机展厅媒体依赖 `infra_file_config.id=28` / When 检查当前运行库文件配置 / Then bucket 必须为 `yudao` 且 domain 必须为 `http://127.0.0.1:9000/yudao`。
- BDD: 展厅媒体 URL 不得漂移到非默认域 -> Given `infra_file` 中 `config_id=28` 且 `path LIKE 'showroom/%'` 的媒体记录 / When 检查媒体 URL / Then URL 必须以 `http://127.0.0.1:9000/yudao/showroom/` 开头。
- BDD: 图片接口必须返回真实图片内容 -> Given 展厅页面引用的公司或产品图片 URL / When 通过后台文件代理请求该 URL / Then 响应必须为 `image/*`，不得返回缺失对象 JSON、404 或 HTML。

## 里程碑

- [x] M1：建立任务记录、BDD 场景和约束。
- [x] M2：复现图片不可用并定位断点。
- [x] M3：补充可复现回归测试或诊断证据。
- [x] M4：在授权范围内实施正式修复。
- [x] M5：完成真实接口/页面验证、收尾清理和提交。

## 预期验证

- 只读检查 `infra_file_config.id=28` 与 `infra_file config_id=28 path LIKE 'showroom/%'`。
- 请求至少一个产品/公司图片后台代理 URL，确认返回 `image/*`。
- 如涉及前端页面，使用 Playwright 真实登录并打开展厅相关页面验证图片加载。

## 当前状态

状态：已完成；本机后端已恢复到正确的本地 Docker MySQL/Redis 路由，图片代理返回 `image/png`。

## Current Status

completed
