<template>
  <section class="showroom-version-snapshot-preview" data-version-center-preview>
    <div class="showroom-version-snapshot-preview__header">
      <div>
        <h3 class="showroom-version-snapshot-preview__title">版本预览</h3>
        <p class="showroom-version-snapshot-preview__tip">
          文字、内容图片、公开 preview asset 与双语语音都以 detail 合同为准。
        </p>
      </div>
      <div v-if="selectedVersion" class="showroom-version-snapshot-preview__meta">
        <el-tag type="info">V{{ selectedVersion.revisionNo }}</el-tag>
        <el-tag v-if="selectedVersion.currentContent" type="primary">当前内容</el-tag>
        <el-tag v-if="selectedVersion.currentPublic" type="success">当前线上</el-tag>
      </div>
    </div>

    <div v-if="!selectedVersion" class="showroom-version-snapshot-preview__empty">
      请选择左侧历史版本查看内容。
    </div>

    <template v-else>
      <section class="showroom-version-snapshot-preview__surface">
        <div class="showroom-version-snapshot-preview__name">
          <strong>{{ selectedVersion.title }}</strong>
          <span v-if="selectedVersion.titleEn">{{ selectedVersion.titleEn }}</span>
        </div>
        <div v-if="selectedVersion.companyType" class="showroom-version-snapshot-preview__company-type">
          {{ selectedVersion.companyType }}
        </div>
      </section>

      <section class="showroom-version-snapshot-preview__surface">
        <div class="showroom-version-snapshot-preview__section-head">
          <h4>内容图片</h4>
          <span>{{ selectedVersion.image.contentImage.source }}</span>
        </div>
        <img
          v-if="selectedVersion.image.contentImage.url"
          :src="selectedVersion.image.contentImage.url"
          :alt="selectedVersion.image.contentImage.alt || `${selectedVersion.title} 内容图片`"
          class="showroom-version-snapshot-preview__image"
        />
        <div v-else class="showroom-version-snapshot-preview__image-empty">未记录内容图片</div>
      </section>

      <section class="showroom-version-snapshot-preview__surface">
        <div class="showroom-version-snapshot-preview__section-head">
          <h4>公开 preview asset 摘要</h4>
          <span>
            {{
              selectedVersion.image.releasePreviewAsset
                ? selectedVersion.image.releasePreviewAsset.source
                : '未绑定'
            }}
          </span>
        </div>
        <template v-if="selectedVersion.image.releasePreviewAsset">
          <img
            v-if="selectedVersion.image.releasePreviewAsset.url"
            :src="selectedVersion.image.releasePreviewAsset.url"
            :alt="
              selectedVersion.image.releasePreviewAsset.alt || `${selectedVersion.title} 公开展示图`
            "
            class="showroom-version-snapshot-preview__image"
          />
          <div v-else class="showroom-version-snapshot-preview__image-empty">未记录公开预览图</div>
          <div class="showroom-version-snapshot-preview__asset-meta">
            <span>资产版本 #{{ selectedVersion.image.releasePreviewAsset.versionId ?? '未记录' }}</span>
            <span>文件 #{{ selectedVersion.image.releasePreviewAsset.fileId ?? '未记录' }}</span>
            <span>
              来源 revision #{{ selectedVersion.image.releasePreviewAsset.sourceRevisionId ?? '未记录' }}
            </span>
          </div>
        </template>
        <div v-else class="showroom-version-snapshot-preview__image-empty">
          当前 detail 合同没有返回公开 preview asset。
        </div>
      </section>

      <section class="showroom-version-snapshot-preview__surface">
        <div class="showroom-version-snapshot-preview__section-head">
          <h4>双语字段</h4>
          <span>{{ selectedVersion.fields.length }} 项</span>
        </div>
        <div class="showroom-version-snapshot-preview__field-grid">
          <article
            v-for="field in selectedVersion.fields"
            :key="field.fieldCode"
            class="showroom-version-snapshot-preview__field-card"
          >
            <h5>{{ field.label }} / {{ field.labelEn }}</h5>
            <p><strong>中文：</strong>{{ field.valueZh || '未填写' }}</p>
            <p><strong>English：</strong>{{ field.valueEn || 'Not filled' }}</p>
          </article>
        </div>
      </section>

      <section class="showroom-version-snapshot-preview__surface">
        <div class="showroom-version-snapshot-preview__section-head">
          <h4>双语语音</h4>
          <span>{{ selectedVersion.narrations.length }} 条</span>
        </div>
        <div class="showroom-version-snapshot-preview__narration-grid">
          <article
            v-for="narration in selectedVersion.narrations"
            :key="`${narration.language}-${narration.versionId ?? 'none'}`"
            class="showroom-version-snapshot-preview__narration-card"
          >
            <h5>{{ resolveVersionCenterNarrationLabel(narration.language) }}</h5>
            <p>语音版本：{{ narration.versionId ?? '未记录' }}</p>
            <p>Voice：{{ narration.voice || '未记录' }}</p>
            <p class="showroom-version-snapshot-preview__script">
              {{ narration.scriptText || '当前没有讲解稿内容' }}
            </p>
            <audio
              v-if="narration.audioUrl"
              :src="narration.audioUrl"
              controls
              preload="none"
            ></audio>
            <span v-else class="showroom-version-snapshot-preview__audio-empty">未生成音频</span>
          </article>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { resolveVersionCenterNarrationLabel, type VersionCenterSnapshotVO } from './contracts'

defineOptions({ name: 'VersionSnapshotPreview' })

defineProps<{
  selectedVersion: VersionCenterSnapshotVO | null
}>()
</script>

<style scoped>
.showroom-version-snapshot-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.showroom-version-snapshot-preview__surface {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-version-snapshot-preview__header,
.showroom-version-snapshot-preview__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.showroom-version-snapshot-preview__title,
.showroom-version-snapshot-preview__section-head h4 {
  margin: 0;
  color: #172033;
}

.showroom-version-snapshot-preview__tip,
.showroom-version-snapshot-preview__section-head span,
.showroom-version-snapshot-preview__asset-meta,
.showroom-version-snapshot-preview__company-type,
.showroom-version-snapshot-preview__empty,
.showroom-version-snapshot-preview__image-empty,
.showroom-version-snapshot-preview__audio-empty {
  color: #4b5563;
  font-size: 0.82rem;
  line-height: 1.5;
}

.showroom-version-snapshot-preview__meta,
.showroom-version-snapshot-preview__asset-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.showroom-version-snapshot-preview__name {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #172033;
}

.showroom-version-snapshot-preview__image,
.showroom-version-snapshot-preview__image-empty {
  width: 100%;
  min-height: 220px;
  object-fit: cover;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.showroom-version-snapshot-preview__image-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f9fc;
}

.showroom-version-snapshot-preview__field-grid,
.showroom-version-snapshot-preview__narration-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.showroom-version-snapshot-preview__field-card,
.showroom-version-snapshot-preview__narration-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.showroom-version-snapshot-preview__field-card h5,
.showroom-version-snapshot-preview__narration-card h5,
.showroom-version-snapshot-preview__field-card p,
.showroom-version-snapshot-preview__narration-card p {
  margin: 0;
}

.showroom-version-snapshot-preview__script {
  color: #263247;
  white-space: pre-wrap;
}

.showroom-version-snapshot-preview__narration-card audio {
  width: 100%;
}

@media (max-width: 960px) {
  .showroom-version-snapshot-preview__field-grid,
  .showroom-version-snapshot-preview__narration-grid {
    grid-template-columns: 1fr;
  }
}
</style>
