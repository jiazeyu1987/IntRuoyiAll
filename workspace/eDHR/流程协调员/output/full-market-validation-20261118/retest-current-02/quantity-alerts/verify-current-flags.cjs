const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { createRequire } = require('node:module');
const crypto = require('node:crypto');
const root = 'D:/ProjectPackage/IntRuoyi/IntRuoyiAll';
const req = createRequire(path.join(root, 'IntRuoyiFronted/package.json'));
const Vue = req('vue');
const { parse } = req('vue/compiler-sfc');
const { renderToString } = req('vue/server-renderer');
const { ElTag } = req('element-plus');
const filename = path.join(root, 'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue');
const source = fs.readFileSync(filename, 'utf8');
const { descriptor, errors } = parse(source); assert.deepEqual(errors, []);
const marker = descriptor.template.content.indexOf('class="team-leader-workbench__active-order-code-cell"');
const start = descriptor.template.content.lastIndexOf('<div', marker);
const end = descriptor.template.content.indexOf('</div>', marker) + '</div>'.length;
const template = descriptor.template.content.slice(start, end);
assert.ok(template.includes('data-team-leader-active-order-quantity-conflict'));
assert.ok(template.includes('data-team-leader-active-order-parameter-abnormal'));
const render = Vue.compile(template);
(async () => {
const results = [];
for (const [name, quantity, parameter] of [['quantity-only',true,false],['parameter-only',false,true],['both',true,true],['neither',false,false]]) {
const row = {workOrderCode:'SSR-FLAGS-ONLY-NOT-BUSINESS-DATA', hasQuantityConflict:quantity, quantityConflict:quantity, hasParameterAbnormal:parameter, overageQuantity:1};
const app=Vue.createSSRApp({data:()=>({row}), methods:{formatTraceQuantity:value=>String(value),openActiveOrderConflictDrawer:()=>{}},render});
app.component('ElTag',ElTag);
const html=await renderToString(app);
assert.equal(html.includes('数量超限（仅提示）'),quantity);
assert.equal(html.includes('参数异常（仅提示）'),parameter);
fs.writeFileSync(path.join(__dirname,name+'.html'),html);
results.push({name,quantityLabel:html.includes('数量超限（仅提示）'),parameterLabel:html.includes('参数异常（仅提示）'),pass:true});
}
const result={testedAt:new Date().toISOString(),mode:'Vue SSR of unchanged real order-code-cell template with actual Element Plus ElTag; flags supplied in memory, no business data or API. This is NOT E2E or layout verification.',sourceSha256:crypto.createHash('sha256').update(source).digest('hex'),results};
fs.writeFileSync(path.join(__dirname,'ssr-results.json'),JSON.stringify(result,null,2));
console.log(JSON.stringify(result,null,2));
})().catch(error=>{console.error(error);process.exitCode=1});
