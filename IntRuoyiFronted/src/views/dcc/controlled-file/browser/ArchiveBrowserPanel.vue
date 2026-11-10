<template>
  <ContentWrap>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-form inline @submit.prevent="search">
        <el-form-item label="项目"><el-select v-model="projectId" filterable remote clearable :remote-method="loadProjects" placeholder="搜索原项目"><el-option v-for="project in projects" :key="project.id" :value="project.id" :label="`${project.projectName} / ${project.projectCode}`" /></el-select></el-form-item>
        <el-form-item label="名称或编号"><el-input v-model="keyword" clearable @keyup.enter="search" /></el-form-item>
        <el-button @click="search">检索</el-button>
      </el-form>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="fileName" label="名称" min-width="180" /><el-table-column prop="fileNumber" label="编号" />
        <el-table-column prop="projectName" label="原项目" /><el-table-column prop="directoryPath" label="原目录" />
        <el-table-column prop="projectFolderName" label="文件夹" /><el-table-column prop="versionNo" label="版本" />
        <el-table-column label="作废时间" min-width="170"><template #default="{ row }">{{ formatDateTimeValue(row.obsoletedTime, '未记录') }}</template></el-table-column><el-table-column prop="obsoleteReason" label="原因" min-width="180" />
        <el-table-column prop="supersededByFileId" label="替换新版ID" />
        <el-table-column label="操作" width="180"><template #default="{ row }">
          <el-button link @click="detail(row)">只读详情</el-button><el-button link @click="download(row.id)">下载原件</el-button>
        </template></el-table-column>
      </el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/config/axios'
import { ElMessageBox } from 'element-plus'
import { downloadControlledFileVariantWithName } from '@/api/dcc/controlledFile/workflow'
import { buildControlledFileTraceabilityPath } from '../view/presentation'
import { downloadByData } from '@/utils/filt'
import { formatDateTimeValue } from '@/utils/formatTime'

const page=ref(1), total=ref(0), rows=ref<any[]>([]), loading=ref(false), error=ref('')
const keyword=ref(''), projectId=ref(''), projects=ref<any[]>([])
const loadProjects=async()=>{projects.value=await request.get({url:'/dcc/controlled-files/archive-projects'})}
const router=useRouter(), route=useRoute()
const load=async()=>{
  loading.value=true; error.value=''; rows.value=[]
  try {
    const result=await request.get({url:'/dcc/controlled-files/archive-page',
      params:{pageNo:page.value,pageSize:20,keyword:keyword.value||undefined,dccProjectCodeId:projectId.value||undefined}})
    rows.value=result.list;total.value=result.total
  } catch(e:any) { error.value=e.message || '读取失败' } finally {loading.value=false}
}
const search=()=>{page.value=1;return load()}
const detail=(row:any)=>router.push(buildControlledFileTraceabilityPath(row.id,'browser',route.fullPath,'trace'))
const download=async(id:string)=>{
  await ElMessageBox.confirm('下载后的副本为非受控文件，是否继续？','下载确认')
  const result=await downloadControlledFileVariantWithName(id,'archive')
  downloadByData(result.blob,result.fileName,result.blob.type||'application/octet-stream')
}
onMounted(async()=>{
  await loadProjects()
  await load()
})
</script>
