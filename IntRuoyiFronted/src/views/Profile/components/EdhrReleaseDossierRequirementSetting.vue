<template>
  <div class="edhr-release-dossier-requirement-setting">
    <el-card shadow="never">
      <template #header>
        <div class="edhr-release-dossier-requirement-setting__header">
          <div class="edhr-release-dossier-requirement-setting__title">eDHR 放行资料（固定必填）</div>
        </div>
      </template>

      <el-alert
        title="以下四份资料由服务端固定校验，不能通过配置或前端开关关闭。"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="edhr-release-dossier-requirement-setting__grid">
        <div
          v-for="item in requiredItems"
          :key="item"
          class="edhr-release-dossier-requirement-setting__item"
        >
          <div class="edhr-release-dossier-requirement-setting__item-title">{{ item }}</div>
          <el-tag type="success" effect="plain">固定必填</el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
const requiredItems = [
  '来料检报告',
  '灭菌报告',
  '成品检报告',
  '成品检记录'
]
</script>

<style scoped>
.edhr-release-dossier-requirement-setting {
  max-width: 860px;
}

.edhr-release-dossier-requirement-setting__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.edhr-release-dossier-requirement-setting__title {
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;
}

.edhr-release-dossier-requirement-setting__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.edhr-release-dossier-requirement-setting__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 58px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.edhr-release-dossier-requirement-setting__item-title {
  color: #1f2d3d;
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .edhr-release-dossier-requirement-setting__grid {
    grid-template-columns: 1fr;
  }
}
</style>
