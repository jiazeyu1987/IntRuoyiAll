# Verification

- RED：新增缓存路由测试 2 FAIL / 2 PASS，暴露离开上传页仍请求。
- GREEN：`node --test tests/unit/dcc-upload-cached-route.test.cjs tests/unit/dcc-upload-project-product-identity.test.cjs tests/unit/dcc-upload-long-contract.test.cjs tests/unit/dcc-upload-a32-contract.test.cjs`，在 IntRuoyiFronted 执行，22 PASS / 0 FAIL。
- 覆盖切换项目代码、受控文件目录、系统用户页面不触发上传刷新；返回和查询参数变化继续刷新；外来评审会签限制保留；产品身份及长 ID 合同与审批矩阵回归。
- A32 原有测试包含真实上传 SFC script/template 联合编译并 PASS。
- `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` PASS，仅换行规范提示。
- 证据边界：代码和回调单元层定位并修复跨页签误请求；未执行真实页面 E2E，未证明用户本次每个弹错都来自该请求；未执行全量类型检查/构建；未重启服务、未修改数据。
- Git 提交/推送未获当轮授权；不宣称已发布或完成运行态验收。
