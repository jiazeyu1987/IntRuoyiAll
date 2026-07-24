# 20260630 排产专用后端验证入口执行日志

## BDD

- `BDD: 排产目标测试只编译排产链路测试 -> Given yudao-module-mes 同时存在排产与 eDHR 测试源码 / When 运行排产专用后端验证入口 / Then 测试编译阶段只覆盖排产链路相关测试源码。`
- `BDD: 排产 DB 测试按 MES 基包收敛扫描 -> Given BaseDbUnitTest 默认读取 application-unit-test.yaml 的大基包 / When 运行排产专用后端验证入口 / Then Spring 测试环境中的 yudao.info.base-package 固定为 cn.iocoder.yudao.module.mes。`
- `BDD: 全量 Maven 入口不被偷偷改弱 -> Given 仓库仍需保留完整后端回归能力 / When 未使用排产专用入口 / Then 现有全量 Maven 行为保持不变。`

## TDD Evidence

- `RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_schedule_targeted_test_profile.py -q -> FAIL, 初始缺少 mes-schedule-targeted-tests profile 与 run_mes_schedule_targeted_tests.py。`
- `GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_schedule_targeted_test_profile.py -q -> PASS`
- `GREEN: python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\run_mes_schedule_targeted_tests.py -> PASS, 129 tests, 0 failures, 0 errors。`

## Notes

- 首次真实 runner 失败原因为 Python 在 Windows 下未解析 `mvn.cmd`，已收敛为 runner 显式解析 `mvn.cmd/mvn`，不属于业务 fallback。
- 首次 targeted suite 失败后，暴露的仅为排产域自身测试问题：
  - `MesProAutoScheduleContractTest` 旧断言仍期待“忽略不可自动排产/不存在工单”，与当前 fail-fast 真实语义不一致。
  - `MesProAutoScheduleServiceImplTest` 存在不会执行到的多余 stub。
- 修正后 targeted suite 全量通过，未再出现 eDHR 编译/测试阻塞。
