<template>
 <ContentWrap class="mt-16px">
  <div class="mb-12px font-600">注册证办理负责人及期限</div>
  <el-alert title="新项目创建后自动向此负责人发送注册证上传任务；目标市场为“不适用”的项目不生成上传任务，也不要求此配置。证件失效后仍发送换证任务。负责人可在任务消息中授权他人办理。请先给负责人分配上传、延续上传及所属公司权限。" type="info" :closable="false" class="mb-16px" />
  <el-form v-loading="loading" label-width="150px">
   <el-form-item label="注册证负责人" required><el-select v-model="form.responsibleUserId" remote filterable :remote-method="search" :disabled="!canUpdate" placeholder="输入姓名查询"><el-option v-for="user in candidates" :key="user.id" :value="user.id" :label="user.name" /></el-select></el-form-item>
   <el-form-item label="项目上传期限（天）" required><el-input-number v-model="form.projectUploadDays" :min="1" :max="3650" :disabled="!canUpdate" /></el-form-item>
   <el-form-item label="失效换证期限（天）" required><el-input-number v-model="form.renewalDays" :min="1" :max="3650" :disabled="!canUpdate" /></el-form-item>
   <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-12px" />
   <el-button v-if="canUpdate" type="primary" :loading="saving" @click="save">保存办理配置</el-button>
  </el-form>
 </ContentWrap>
</template>
<script setup lang="ts">
import {getHandlingConfig,saveHandlingConfig,getHandlingCandidates} from '@/api/dcc/registrationCertificate/handlingTasks'
defineProps<{canUpdate:boolean}>()
const loading=ref(false),saving=ref(false),error=ref('')
const form=reactive<{responsibleUserId?:string;projectUploadDays?:number;renewalDays?:number}>({})
const candidates=ref<Array<{id:string;name:string}>>([])
const message=useMessage()
const search=async(keyword:string)=>{candidates.value=await getHandlingCandidates(keyword)}
const load=async()=>{loading.value=true;try{const value=await getHandlingConfig();if(value){Object.assign(form,value);candidates.value=[{id:value.responsibleUserId,name:value.responsibleName}]}else error.value='尚未配置：目标市场不是“不适用”的新建项目或失效换证任务，须先明确负责人和期限。'}catch(e){error.value=e instanceof Error?e.message:String(e)}finally{loading.value=false}}
const save=async()=>{error.value='';if(!form.responsibleUserId||!form.projectUploadDays||!form.renewalDays){error.value='请明确选择注册证负责人并填写两个办理期限';return}saving.value=true;try{await saveHandlingConfig({responsibleUserId:form.responsibleUserId,projectUploadDays:form.projectUploadDays,renewalDays:form.renewalDays});message.success('办理配置已保存')}catch(e){error.value=e instanceof Error?e.message:String(e)}finally{saving.value=false}}
onMounted(load)
</script>
