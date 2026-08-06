# 一线生产参考页面严格一致修正

## Task Goal

修正一线生产填写页与 `C:\Users\BJB110\Desktop\3\frontline-production-operator-1920.html` 的排版、大小、布局不一致问题：内部生产填写画布保持参考 HTML 的 `1920px × 1080px`、`130px 1fr 126px` 行布局、顶部 `1fr 1fr 240px`、主区 `1050px 1fr`、底部 `300px 1fr` 和原始字号；普通后台页面不得默认全屏或横向溢出，通过外层 `frontline-production-stage` 对整张参考画布等比例缩放适配；最新用户反馈的生产 picker 弹框及选项卡按 `1920:1080` / 16:9 比例显示。

## Milestones

- [x] 复核一线 PQC 最大化/主页切换逻辑与一线生产默认全屏差异
- [x] 建立一线生产显式最大化静态回归合同
- [x] 恢复一线生产显式 Fullscreen API 切换语义，默认不全屏
- [x] 按用户最新要求撤销局部响应式 grid 口径，恢复严格参考 HTML 内部画布
- [x] 用外层 stage 缩放防止普通页面横向溢出
- [x] 按最新反馈将生产 picker 弹框和选项卡改为 1920:1080 / 16:9 比例
- [x] 同步真实 E2E 脚本断言：stage 适配视口、内部 canvas 保持 1920×1080
- [x] 运行定向静态合同、相邻合同、真实 E2E 语法检查、类型检查和 diff check
- [x] 更新任务证据与 closeout 阻塞说明

## Expected Verification

- `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs`
- `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs`
- `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs`
- `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260806-frontline-production-fullscreen-logic\bug-regression-evidence.md`
- `git diff --check -- IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue IntRuoyiFronted\tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs IntRuoyiFronted\tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs IntRuoyiFronted\tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs IntRuoyiFronted\tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs IntRuoyiFronted\tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs doc\tasks\20260806-frontline-production-fullscreen-logic`

## Current Status

blocked

实现与定向验证已通过：一线生产内部画布保持参考 HTML 的严格 1920×1080 布局，普通页通过外层 stage 等比例缩放避免横向溢出，右上按钮仍按 PQC 逻辑显式“最大化/主页”切换；生产 picker 弹框和选项卡已按最新反馈改为 1920:1080 / 16:9 比例。但当前 `int_main` 工作区存在非本任务脏改动/未跟踪文件；按项目提交推送门禁，本轮不提交、不推送、不标记 completed。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，参考内部 canvas 仍由外层 stage scale 防溢出；picker 弹框比例作为最新明确反馈单独锁定为 16:9，避免再回到固定 760px / 112px。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 前端静态契约隔离门禁：本任务用一线生产专用静态合同先 RED 后 GREEN，避免无关大合同影响判断。
- 前端截图样式块静态契约门禁：用户要求“严格完全一致”时，合同必须锁定目标 selector 的关键 token，避免局部响应式重排冒充复刻完成。
- Element Plus 全屏弹框挂载门禁：一线生产默认不进入浏览器 fullscreen，只有用户点击“最大化”后才进入，退出后恢复“最大化”。