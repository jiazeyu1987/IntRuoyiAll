// Contract tests inspect migration text and render its template; they do not execute SQL.
const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const sql=fs.readFileSync('../IntRuoyiBackend/sql/mysql/20261120_dcc_registration_certificate_neutral_event_notify_template.sql','utf8')
test('release migration updates existing shared template with neutral certificate wording',()=>{
 const update=sql.match(/UPDATE `system_notify_template`([\s\S]*?);/)[1]
 assert.match(update,/WHERE `code` = 'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT'/)
 assert.doesNotMatch(update,/NOT EXISTS/)
 const content=update.match(/`content` = '([^']+)'/)[1]
 const original='产品《{productName}》的国内注册证{eventTitle}。注册证号：{certificateNo}，生效日期：{effectiveDate}，有效期至：{expiryDate}。'
 const existing={code:'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT',content:original,status:1,nickname:'自定义发送人',type:8,params:'custom-params'}
 const unchanged={status:existing.status,nickname:existing.nickname,type:existing.type,params:existing.params}
 existing.content=content
 assert.deepEqual({status:existing.status,nickname:existing.nickname,type:existing.type,params:existing.params},unchanged)
 const assignments=update.split('WHERE')[0];assert.deepEqual([...assignments.matchAll(/`(\w+)`\s*=/g)].map(m=>m[1]),['name','content'])
 assert.doesNotMatch(sql,/INSERT INTO/)
 assert.match(sql,/active_template_count <> 1 THEN[\s\S]*SIGNAL SQLSTATE/)
 const params={productName:'CE产品',eventTitle:'变更已生效，请确认',certificateNo:'CE-001',effectiveDate:'证件未载明',expiryDate:'2030-01-01'}
 const rendered=existing.content.replace(/\{(\w+)\}/g,(_,key)=>{assert.ok(Object.hasOwn(params,key));return params[key]})
 assert.match(rendered,/CE产品.*注册证变更已生效/)
 assert.doesNotMatch(rendered,/国内/)
 assert.match(sql,/IF NOT EXISTS[\s\S]*content[\s\S]*SIGNAL SQLSTATE/)
})
test('initial setup uses the same neutral template contract',()=>{
 const initial=fs.readFileSync('../IntRuoyiBackend/sql/mysql/20260901_dcc_registration_certificate_business_event_notify_template.sql','utf8')
 assert.doesNotMatch(initial,/国内注册证/)
 const expected=sql.match(/`content` = '([^']+)'/)[1]
 assert.ok(initial.includes(expected))
})
