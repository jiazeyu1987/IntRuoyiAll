<template>
 <div><el-button @click="load">刷新申请</el-button><el-alert v-if="error" :title="error" type="error" :closable="false"/>
 <el-table :data="requests" v-loading="loading" border class="mt-12px">
  <el-table-column prop="id" label="申请编号" width="100"/><el-table-column label="产品"><template #default="{row}">{{summary(row).productName}}</template></el-table-column>
  <el-table-column label="证件类型"><template #default="{row}">{{factsOf(row).certificateType}}</template></el-table-column><el-table-column prop="status" label="审批状态"/>
  <el-table-column prop="requestedAt" label="提交时间"/><el-table-column prop="rejectReason" label="驳回原因"/>
  <el-table-column label="操作"><template #default="{row}"><el-button link @click="show(row)">申请内容</el-button><el-button v-if="row.status==='REJECTED'" link type="primary" @click="retry(row)">修改后重提</el-button></template></el-table-column>
 </el-table>
 <el-dialog v-model="showDetail" title="本次申请留档" width="900px"><el-form label-width="140px"><FactsFields v-if="selectedFacts" v-model="selectedFacts" readonly/></el-form><el-table :data="selectedFiles"><el-table-column prop="originalName" label="提交文件"/><el-table-column prop="status" label="本次审批结果"/></el-table></el-dialog>
 <UploadDialog v-model="editing" :request-id="sourceId" @saved="load"/>
 <ChangeDialog v-model="changing" :request-id="sourceId" @saved="load"/>
 </div>
</template>
<script setup lang="ts">
import {onMounted,ref}from 'vue'
import {getMyRegistrationCertificateUploads,getMyRegistrationCertificateUpload}from '@/api/dcc/registrationCertificate'
import FactsFields from '../shared/FactsFields.vue'
import UploadDialog from './UploadDialog.vue'
import ChangeDialog from '../change/ChangeDialog.vue'
import type{CertificateFacts}from '../shared/facts'
const requests=ref<any[]>([]),loading=ref(false),error=ref(''),editing=ref(false),changing=ref(false),sourceId=ref<number|string>(),showDetail=ref(false),selectedFacts=ref<CertificateFacts>(),selectedFiles=ref<any[]>([])
const summary=(row:any)=>JSON.parse(row.detailJson)
const factsOf=(row:any)=>{const detail=summary(row);return detail.factsJson?JSON.parse(detail.factsJson):{certificateType:'历史申请（完整字段迁移前）'}}
const load=async()=>{loading.value=true;error.value='';try{requests.value=await getMyRegistrationCertificateUploads()}catch(e){error.value=e instanceof Error?e.message:String(e)}finally{loading.value=false}}
const retry=(row:any)=>{error.value='';try{const frozen=summary(row);sourceId.value=row.id;if(frozen.operation==='CHANGE_CERTIFICATE')changing.value=true;else if(frozen.operation==='UPLOAD_CERTIFICATE'&&frozen.draft)editing.value=true;else if(frozen.operation==='RENEWAL_CERTIFICATE')throw new Error('续证申请请从对应注册证详情重新发起续证');else throw new Error('该历史申请未保存可重提内容，请联系注册部核对')}catch(e){error.value=e instanceof Error?e.message:String(e)}}
const show=async(row:any)=>{try{const data=await getMyRegistrationCertificateUpload(row.id);const frozen=JSON.parse(data.request.detailJson);if(!frozen.factsJson)throw new Error('该历史申请未保存完整字段，请查看原审批归档');selectedFacts.value=JSON.parse(frozen.factsJson);selectedFiles.value=data.files;showDetail.value=true}catch(e){error.value=e instanceof Error?e.message:String(e)}}
onMounted(load)
</script>
