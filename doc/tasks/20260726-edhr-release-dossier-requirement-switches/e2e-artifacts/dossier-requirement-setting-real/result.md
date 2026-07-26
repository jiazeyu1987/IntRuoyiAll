# eDHR 放行资料限制开关真实 E2E

- Status: FAIL
- Base URL: http://127.0.0.1:8081
- Backend URL: http://127.0.0.1:48081
- Tenant/User: 芋道源码/admin
- Original: {}
- Changed: {}
- Restore: {}
- RED: real-profile-config-dossier-switch -> FAIL，AssertionError [ERR_ASSERTION]: GET /mes/pro/edhr-release-setting/dossier-requirements 业务响应必须成功：请求地址不存在:admin-api/mes/pro/edhr-release-setting/dossier-requirements

404 !== 0

    at apiRequest (E:\IntRuoyi\IntRuoyiFronted\tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js:235:10)
    at async main (E:\IntRuoyi\IntRuoyiFronted\tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js:417:23)
