<template>
  <section v-loading="loading" data-testid="dcc-obsolete-impact">
    <el-alert title="作废前请核对关联文件、引用及受影响项目，并安排相应文件修订或引用调整。" type="warning" :closable="false" show-icon />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-button v-if="error" link @click="load">重新读取影响范围</el-button>
    <template v-if="relations && usage">
      <p>本版本主动关联文件 {{ relations.length }} 份；引用本版本的项目 {{ usage.referenceProjectCount }} 个。请同时核对文件所属项目：{{ projectName }}。</p>
      <el-table :data="relations" empty-text="本版本未关联文件" size="small">
        <el-table-column prop="fileName" label="主动关联文件" />
        <el-table-column prop="fileNumber" label="编号" />
        <el-table-column prop="versionNo" label="关联版本" />
      </el-table>
      <p>引用及受影响项目（按当前账号权限展示）</p>
      <el-alert v-if="usage.detailsRestricted" title="部分引用项目明细受权限限制，请联系对应项目负责人核对；总项目数包含受限项目。" type="warning" :closable="false" />
      <el-table :data="usage.list" empty-text="没有可展示的引用明细" size="small">
        <el-table-column prop="projectName" label="引用项目" />
        <el-table-column prop="folderName" label="引用文件夹" />
        <el-table-column prop="fileName" label="引用文件" />
        <el-table-column prop="versionNo" label="固定引用版本" />
      </el-table>
      <el-pagination v-if="usage.total > pageSize" :current-page="page" :page-size="pageSize" :total="usage.total"
        layout="prev, pager, next, total" @current-change="loadPage" />
    </template>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { getProjectReferenceUsage, getProjectReferenceUsagePage, listHistoricalRelations } from '@/api/dcc/controlledFile/relations'
import { referenceIdentity } from '../relations/project-reference-contract'
import type { ReferenceUsagePage } from '../relations/reference-usage'
const props = defineProps<{ fileId: string | number; projectName: string; active: boolean }>()
const userStore = useUserStore()
interface RelatedFile { controlledFileId: string; masterId: string; fileName: string; fileNumber: string; versionNo: string }
const loading = ref(false), error = ref(''), page = ref(1), pageSize = 10
const relations = ref<RelatedFile[]>(), usage = ref<ReferenceUsagePage>()
let generation = 0
const load = async () => {
  const token = ++generation, id = referenceIdentity(props.fileId)
  relations.value = undefined; usage.value = undefined; error.value = ''; page.value = 1
  if (!props.active) { loading.value = false; return }
  loading.value = true
  try {
    const [selectedRelations, summary] = await Promise.all([listHistoricalRelations(id), getProjectReferenceUsage(id)])
    if (token !== generation) return
    if (!Array.isArray(selectedRelations)) throw new Error('作废所选版本的冻结关联返回无效')
    const seen = new Set<string>()
    for (const row of selectedRelations) {
      const masterId = referenceIdentity(row.masterId)
      referenceIdentity(row.controlledFileId)
      if (seen.has(masterId) || !row.fileName || !row.fileNumber || !row.versionNo)
        throw new Error('作废关联文件身份或展示事实缺失')
      seen.add(masterId)
    }
    const references = await getProjectReferenceUsagePage(summary, 1, pageSize)
    if (token !== generation) return
    relations.value = selectedRelations; usage.value = references
  } catch (cause) {
    if (token === generation) error.value = `影响范围未核实：${cause instanceof Error ? cause.message : String(cause)}`
  } finally { if (token === generation) loading.value = false }
}
const loadPage = async (next: number) => {
  if (!usage.value || loading.value) return
  const token = ++generation, context = usage.value
  loading.value = true; error.value = ''
  try {
    const result = await getProjectReferenceUsagePage(context, next, pageSize)
    if (token !== generation) return
    usage.value = result; page.value = next
  } catch (cause) {
    if (token === generation) error.value = `引用明细未核实：${cause instanceof Error ? cause.message : String(cause)}`
  } finally { if (token === generation) loading.value = false }
}
watch(() => [props.active, String(props.fileId), String(getVisitTenantId() ?? getTenantId()), String(userStore.getUser?.id)], load, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { generation++ })
</script>
