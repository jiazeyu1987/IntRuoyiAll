<template>
  <el-dialog v-model="visible" title="发布结果" width="min(1100px, 95vw)">
    <el-descriptions :column="2" border>
      <el-descriptions-item v-if="showProject" label="所属项目">{{ projectName }}</el-descriptions-item>
      <el-descriptions-item v-if="showProject" label="项目 ID">{{ result.dccProjectCodeId }}</el-descriptions-item>
      <el-descriptions-item label="规程名称">{{ result.regulationName }}</el-descriptions-item>
      <el-descriptions-item label="文件编号">{{ result.regulationCode }}</el-descriptions-item>
      <el-descriptions-item label="版本">{{ result.versionNo }}</el-descriptions-item>
      <el-descriptions-item label="发布版本 ID">{{
        result.publishedVersionId
      }}</el-descriptions-item>
    </el-descriptions>
    <p>已发布 {{ result.processes.length }} 道工序、{{ itemCount }} 个检验项目</p>
    <section v-for="process in result.processes" :key="process.processCode">
      <h3>{{ process.processName }}</h3>
      <el-table :data="process.items" border>
        <el-table-column prop="itemName" label="检验项目" min-width="120" />
        <el-table-column prop="standardText" label="接受标准" min-width="220" />
        <el-table-column prop="inspectionMethod" label="检验方法" min-width="220" />
        <el-table-column prop="inspectionTool" label="检验器具" min-width="120" />
        <el-table-column label="器具未绑定原因" min-width="180">
          <template #default="{ row }">
            <template v-if="row.equipmentUnboundReason">
              <p>{{ row.equipmentUnboundReason }}</p>
              <p>{{ EQUIPMENT_UNBOUND_NOTICE }}</p>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="samplingPlanText" label="抽样要求" min-width="160" />
      </el-table>
    </section>
    <template #footer><el-button @click="visible = false">关闭</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { QaInspectionRegulationPublishedVersionVO } from '@/api/mes/qc/template'
import { EQUIPMENT_UNBOUND_NOTICE } from '../simplePqc'

const visible = defineModel<boolean>({ required: true })
const props = withDefaults(defineProps<{
  result: QaInspectionRegulationPublishedVersionVO
  projectName?: string
  showProject?: boolean
}>(), { showProject: true })
const itemCount = computed(() => props.result.processes.reduce((sum, p) => sum + p.items.length, 0))
</script>
