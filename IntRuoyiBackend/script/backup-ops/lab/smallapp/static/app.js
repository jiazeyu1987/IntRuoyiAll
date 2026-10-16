'use strict';
const $ = (id) => document.getElementById(id);
let session = null, editing = null, attachmentEditing = null, records = [], busy = false, confirmAnswer = null;
const labels = { LOGIN_REQUIRED:'请重新登录。', LOGIN_DENIED:'用户名或密码错误。', LOGIN_RATE_LIMITED:'登录尝试过多，请五分钟后再试。', WRITES_PAUSED:'当前写入已暂停。', RECORD_VERSION_CONFLICT:'记录已发生变化，请刷新后重新操作。', FILE_SIZE_LIMIT:'文件大小须为 1 字节至 64 KiB。', DEPENDENCY_FAILED:'存储服务操作失败，写入已暂停。', OBJECT_OPERATION_PENDING:'附件操作未完成，写入已暂停。', CSRF_DENIED:'登录状态校验失败，请重新登录。', BACKEND_UNAVAILABLE:'服务暂时无法连接。' };
function message(value, error=false) { $('message').textContent=value; $('message').classList.toggle('error',error); $('attachment-message').textContent=$('attachment-dialog').open ? value : ''; }
function signedHeaders(extra={}) { return {...extra, 'X-CSRF-Token':session.csrf}; }
async function request(path, method='GET', data=null, extra={}) {
  const options={method,credentials:'same-origin',headers:extra};
  if (data !== null) { options.body=JSON.stringify(data); options.headers={...extra,'Content-Type':'application/json'}; }
  if (method!=='GET' && session) options.headers=signedHeaders(options.headers);
  const response=await fetch(path,options);
  const result=await response.json();
  if (!response.ok) { const error=new Error(labels[result.errorCode] || `操作失败：${result.errorCode}`); error.code=result.errorCode; throw error; }
  return result;
}
function showLogin() { session=null; $('workspace').hidden=true; $('login-panel').hidden=false; }
function showWorkspace() { $('tenant-identity').textContent=String(session.tenantId); $('username-identity').textContent=session.username; $('login-panel').hidden=true; $('workspace').hidden=false; }
function resetForm() { editing=null; $('record-form').reset(); $('form-heading').textContent='新增记录'; $('cancel-edit').hidden=true; }
async function action(work) {
  if (busy) return;
  busy=true;
  $('records').hidden=true; $('records').dataset.ready='false';
  document.querySelectorAll('button').forEach((button)=>{button.disabled=true;});
  try { await work(); }
  catch(error) { message(error.message,true); if (error.code==='LOGIN_REQUIRED') showLogin(); }
  finally { busy=false; $('records').hidden=false; $('records').dataset.ready='true'; document.querySelectorAll('button').forEach((button)=>{button.disabled=false;}); }
}
function confirmDelete(value) { $('confirm-text').textContent=value; $('confirm-yes').disabled=false; $('confirm-no').disabled=false; $('confirm-dialog').showModal(); return new Promise((resolve)=>{confirmAnswer=resolve;}); }
function answer(value) { $('confirm-dialog').close(); const resolve=confirmAnswer; confirmAnswer=null; if(resolve)resolve(value); }
function element(tag,text,className) { const node=document.createElement(tag); if(text!==undefined) node.textContent=text; if(className) node.className=className; return node; }
function button(text,testId,run) { const node=element('button',text); node.type='button'; node.dataset.testid=testId; node.addEventListener('click',()=>action(run)); return node; }
function encodedName(value) { return encodeURIComponent(value).replace(/[!'()*]/g,(char)=>`%${char.charCodeAt(0).toString(16).toUpperCase()}`); }
async function refresh() {
  const result=await request('/api/records'); records=result.records;
  $('state').textContent=`租户 122 · ${result.state==='OPEN'?'允许写入':'写入已暂停'} · ${records.length} 条记录`;
  $('records').replaceChildren(); $('empty').hidden=records.length!==0;
  for(const record of records) {
    const card=element('article',undefined,'record'); card.dataset.testid='record'; card.dataset.recordId=String(record.id); card.dataset.version=String(record.version);
    const key=element('p',record.record_key,'meta');key.dataset.testid='row-key';
    const title=element('h3',record.title,'record-title');title.dataset.testid='row-title';
    const body=element('p',record.content);body.dataset.testid='row-body';
    card.append(key,title,body,element('p',`记录 ${record.id} · 版本 ${record.version}`,'meta'));
    const controls=element('div',undefined,'actions');
    controls.append(button('编辑','edit-record',async()=>{editing={id:record.id,version:record.version}; $('record-form').elements.key.value=record.record_key; $('record-form').elements.title.value=record.title; $('record-form').elements.content.value=record.content; $('form-heading').textContent=`编辑记录 ${record.id}`; $('cancel-edit').hidden=false; $('record-form').elements.title.focus();}),button('删除记录','delete-record',async()=>{if(!await confirmDelete(`删除记录“${record.title}”及其附件？`))return; await request(`/api/records/${record.id}`,'DELETE',{expectedVersion:record.version}); if(editing?.id===record.id) resetForm(); await refresh(); message('记录已删除。');}));
    card.append(controls);
    card.append(button(record.filename?'替换附件':'上传附件',record.filename?'replace-attachment':'upload-attachment',async()=>{attachmentEditing={id:record.id,version:record.version};$('attachment-form').reset();$('attachment-message').textContent='';$('attachment-heading').textContent=record.filename?'替换附件':'上传附件';$('attachment-dialog').showModal();}));
    if(record.filename) {
      const name=element('p',record.filename);name.dataset.testid='attachment-name';card.append(name);
      const reference=record.bucket+'/'+record.object_key;
      const metadata=element('p',`${record.size_bytes} 字节 · SHA-256 ${record.sha256}\n对象 ${reference}`,'meta'); metadata.dataset.testid='attachment-metadata'; card.append(metadata);
      const attachmentActions=element('div',undefined,'actions'); const download=element('a','下载附件','download'); download.href=`/api/records/${record.id}/attachment`; download.dataset.testid='download-attachment'; download.setAttribute('download','');
      download.dataset.objectReference=reference;
      attachmentActions.append(download,button('删除附件','delete-attachment',async()=>{if(!await confirmDelete(`删除附件“${record.filename}”？`))return; await request(`/api/records/${record.id}/attachment`,'DELETE',{expectedVersion:record.version}); await refresh();message('附件已删除。');})); card.append(attachmentActions);
    }
    $('records').append(card);
  }
}
$('login-form').addEventListener('submit',(event)=>{event.preventDefault();const form=event.currentTarget;action(async()=>{
  const token=await request('/api/login-token');
  const password=form.elements.password.value;
  try { session=await request('/api/login','POST',{tenantId:Number(form.elements.tenantId.value),username:form.elements.username.value,password,csrf:token.csrf}); }
  finally { form.elements.password.value=''; }
  showWorkspace(); resetForm(); await refresh();message('登录成功。');
});});
$('record-form').addEventListener('submit',(event)=>{event.preventDefault();const form=event.currentTarget;action(async()=>{
  const data={key:form.elements.key.value,title:form.elements.title.value,content:form.elements.content.value};
  if(editing) await request(`/api/records/${editing.id}`,'PUT',{...data,expectedVersion:editing.version});
  else await request('/api/records','POST',data);
  resetForm(); await refresh();message('记录已保存。');
});});
$('cancel-edit').addEventListener('click',resetForm);
$('add-record').addEventListener('click',()=>{if(busy)return;resetForm();$('record-form').elements.key.focus();});
$('confirm-yes').addEventListener('click',()=>answer(true));
$('confirm-no').addEventListener('click',()=>answer(false));
$('confirm-dialog').addEventListener('cancel',(event)=>{event.preventDefault();answer(false);});
$('cancel-attachment').addEventListener('click',()=>{$('attachment-dialog').close();attachmentEditing=null;});
$('attachment-dialog').addEventListener('cancel',()=>{attachmentEditing=null;});
$('attachment-form').addEventListener('submit',(event)=>{event.preventDefault();const file=event.currentTarget.elements.file.files[0];const selected=attachmentEditing;action(async()=>{
  if(!selected || !file)throw new Error('请先选择文件。');if(file.size<1 || file.size>65536)throw new Error(labels.FILE_SIZE_LIMIT);
  const response=await fetch(`/api/records/${selected.id}/attachment`,{method:'PUT',credentials:'same-origin',headers:signedHeaders({'Content-Type':'application/octet-stream','X-File-Name':encodedName(file.name),'X-Expected-Version':String(selected.version)}),body:file});
  const result=await response.json();if(!response.ok){const error=new Error(labels[result.errorCode] || `附件操作失败：${result.errorCode}`);error.code=result.errorCode;throw error;}
  $('attachment-dialog').close();attachmentEditing=null;await refresh();message('附件已保存并校验。');
});});
$('refresh').addEventListener('click',()=>action(refresh));
$('logout').addEventListener('click',()=>action(async()=>{await request('/api/logout','POST',{});showLogin();resetForm();message('已退出登录。');}));
action(async()=>{try{session=await request('/api/session');showWorkspace();await refresh();}catch(error){if(error.code==='LOGIN_REQUIRED'){showLogin();return;}throw error;}});
