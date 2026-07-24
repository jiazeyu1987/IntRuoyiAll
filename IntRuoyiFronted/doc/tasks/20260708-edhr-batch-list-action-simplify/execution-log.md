# 执行日志：eDHR 批次列表操作按钮精简

BDD: 批次列表只显示三个主操作 -> Given 用户打开 eDHR 批次执行列表 / When 查看任意批次行右侧操作区 / Then 行内只暴露“填写”“追溯”“打印”三个主按钮，不再并列显示详情、流程追踪、操作轨迹、UX检查、预检、查看归档、下载打印版PDF。
BDD: 填写主入口承载填写与检查 -> Given 用户点击批次行“填写” / When 进入批次详情页 / Then 详情页继续提供填写、UX 检查和预检相关能力。
BDD: 追溯主入口承载追踪归档轨迹 -> Given 用户点击批次行“追溯” / When 打开追溯入口 / Then 可以继续查看流程追踪、操作轨迹和归档信息。
BDD: 打印主入口承载打印下载 -> Given 用户点击批次行“打印” / When 触发打印入口 / Then 继续复用打印版 PDF 下载能力。

GREEN: experience-preflight -> PASS, 已读取 PowerShell、前端样式、前端交付和中文文案门禁；本轮不执行真实 E2E、不操作服务器和数据库。

RED: node tests/e2e/edhr-batch-list-action-simplify-static.spec.js -> FAIL, 当前行操作仍并列显示详情、流程追踪、操作轨迹、UX检查、预检、查看归档等旧入口。
GREEN: node tests/e2e/edhr-batch-list-action-simplify-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-batch-row-readiness-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-final-archive-work-task-static.spec.js -> PASS
GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS
BLOCKER: node tests/e2e/edhr-p0-p2-ux-resolution-static.spec.js -> FAIL, 既有 `WorkTaskBoardPage.vue` 缺少“归档规则已保存，责任人与实际派发源一致”提示断言；与本次批次列表按钮精简无关，未修改该文件。
