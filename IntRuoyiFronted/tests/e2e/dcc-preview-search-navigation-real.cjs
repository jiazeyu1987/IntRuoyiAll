// Run against the task-owned PREVIEW-20261113-01 application created through the upload page.
// DCC_PREVIEW_USERNAME and DCC_PREVIEW_PASSWORD must be supplied by the caller.
const { chromium } = require('playwright')
;(async () => {
  if (!process.env.DCC_PREVIEW_USERNAME || !process.env.DCC_PREVIEW_PASSWORD) throw new Error('DCC_PREVIEW_USERNAME and DCC_PREVIEW_PASSWORD are required')
  const browser = await chromium.launch({ headless: true })
  try {
    const pageContext = await browser.newContext({ viewport: { width: 1600, height: 1100 } })
    const p = await pageContext.newPage()
    const baseUrl = process.env.DCC_PREVIEW_BASE_URL || 'http://127.0.0.1:8063'
    await p.goto(baseUrl + '/login?redirect=/approval-center/my-initiated')
    await p.getByPlaceholder('请输入用户名', { exact: true }).fill(process.env.DCC_PREVIEW_USERNAME)
    await p.getByPlaceholder('请输入密码', { exact: true }).filter({ visible: true }).fill(process.env.DCC_PREVIEW_PASSWORD)
    await p.getByRole('button', { name: '登录', exact: true }).click()
    await p.getByRole('row').filter({ hasText: 'PREVIEW-20261113-01' }).getByRole('button', { name: '查看', exact: true }).click({ timeout: 60000 })
    await p.getByRole('textbox', { name: '文本正文关键字' }).waitFor({ timeout: 45000 })
const {expect}=require('playwright/test'),fs=require('fs');const out=require('path').resolve(__dirname,'../../../doc/tasks/20261113-document-preview');fs.mkdirSync(out,{recursive:true});
const tools=p.getByTestId('protected-viewer-text-tools');const query=p.getByRole('textbox',{name:'文本正文关键字'});const pageNo=p.getByRole('textbox',{name:'文本页码'});
await expect(p.getByRole('button',{name:'上一页',exact:true})).toBeDisabled();await query.fill('预览验收关键词');await p.getByRole('button',{name:'搜索文本正文',exact:true}).click();await expect(tools).toContainText('第 1 / 3 处匹配');await expect(p.locator('mark[data-active-match=true]')).toHaveText('预览验收关键词');await p.getByRole('button',{name:'下一处匹配',exact:true}).click();await expect(pageNo).toHaveValue('2');await expect(p.locator('.protected-viewer-text')).toContainText('正文第91行');await p.getByRole('button',{name:'下一处匹配',exact:true}).click();await expect(pageNo).toHaveValue('3');await expect(p.getByRole('button',{name:'下一页',exact:true})).toBeDisabled();await p.getByRole('button',{name:'上一处匹配',exact:true}).click();await expect(pageNo).toHaveValue('2');await p.getByRole('button',{name:'上一页',exact:true}).click();await expect(pageNo).toHaveValue('1');await p.getByRole('button',{name:'下一页',exact:true}).click();await expect(pageNo).toHaveValue('2');await pageNo.fill('3');await p.getByRole('button',{name:'跳转页码',exact:true}).click();await expect(pageNo).toHaveValue('3');await expect(p.locator('.protected-viewer-text')).toContainText('正文第161行');await p.screenshot({path:out+'/main-search.png',fullPage:true});await query.fill('不存在的任务关键词');await query.press('Enter');await expect(tools).toContainText('未找到匹配内容');
await p.getByText('附件',{exact:true}).click();await p.getByRole('button',{name:'在线查看',exact:true}).click();await p.locator('.protected-viewer-text').waitFor({timeout:45000});await expect(p.locator('.protected-viewer-text')).toHaveText('附件独有关键词\n附件第二行');await expect(p.getByRole('textbox',{name:'文本页码'})).toHaveValue('1');await p.getByRole('textbox',{name:'文本正文关键字'}).fill('预览验收关键词');await p.getByRole('button',{name:'搜索文本正文',exact:true}).click();await expect(p.getByTestId('protected-viewer-text-tools')).toContainText('未找到匹配内容');await p.getByRole('textbox',{name:'文本正文关键字'}).fill('附件独有关键词');await p.getByRole('textbox',{name:'文本正文关键字'}).press('Enter');await expect(p.locator('mark[data-active-match=true]')).toHaveText('附件独有关键词');await p.screenshot({path:out+'/attachment.png',fullPage:true});await p.getByText('主文档',{exact:true}).click();await expect(p.getByRole('textbox',{name:'文本页码'})).toHaveValue('1');await expect(p.getByRole('textbox',{name:'文本正文关键字'})).toHaveValue('');await expect(p.locator('.protected-viewer-text')).toContainText('正文第1行');
fs.writeFileSync(out+'/result.json',JSON.stringify({status:'PASS',url:p.url(),fileNumber:'PREVIEW-20261113-01',account:'DCC241016C12R3L1',linesPerPage:80,cases:['keyword click search','enter search','previous hit','next hit','cross-page hit','active highlight','previous page','next page','page jump','first/last disabled','no match','attachment identity and search isolation','return main reset']},null,2));console.log('PASS all real UI preview interactions');





  } finally { await browser.close() }
})().catch(error => { console.error(error); process.exitCode = 1 })
