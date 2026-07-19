<template>
  <a-card title="配音管理">
    <template #extra>
      <a-space>
        <a-input-search v-model:value="searchTaskId" placeholder="任务 ID" style="width: 160px" @search="load" />
      </a-space>
    </template>

    <a-form layout="inline" style="margin-bottom: 16px">
      <a-form-item label="配音类型">
        <a-select v-model:value="voiceType" style="width: 140px">
          <a-select-option value="doubao">豆包</a-select-option>
          <a-select-option value="azure">Azure</a-select-option>
          <a-select-option value="aliyun">阿里云</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="语速">
        <a-slider v-model:value="speed" :min="0.5" :max="2.0" :step="0.05" style="width: 200px" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :loading="generating" @click="handleGenerate">生成配音</a-button>
      </a-form-item>
    </a-form>

    <a-descriptions v-if="voice" bordered :column="2">
      <a-descriptions-item label="任务 ID">{{ voice.taskId }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="voice.status === 'SUCCESS' ? 'green' : voice.status === 'FAILURE' ? 'red' : 'orange'">{{ voice.status }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="配音类型">{{ voice.voiceType || '-' }}</a-descriptions-item>
      <a-descriptions-item label="语速">{{ voice.speed || '1.05' }}</a-descriptions-item>
      <a-descriptions-item label="配音文件">
        <template v-if="voice.voiceUrl">
          <audio :src="voice.voiceUrl" controls style="height: 32px" />
        </template>
        <span v-else>-</span>
      </a-descriptions-item>
      <a-descriptions-item label="时长">{{ voice.duration ? `${voice.duration}s` : '-' }}</a-descriptions-item>
    </a-descriptions>

    <a-empty v-if="!voice && !loading" description="请输入任务 ID 查询配音状态" />
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { getVoice, generateVoice } from '../../api/voice'

const searchTaskId = ref('')
const voiceType = ref('doubao')
const speed = ref(1.05)
const voice = ref<any>(null)
const loading = ref(false)
const generating = ref(false)

async function load() {
  if (!searchTaskId.value) return
  const taskId = Number(searchTaskId.value)
  if (!taskId) { message.warning('请输入有效的任务 ID'); return }
  loading.value = true
  try {
    const res: any = await getVoice(taskId)
    voice.value = res.data || null
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  if (!searchTaskId.value) { message.warning('请输入任务 ID'); return }
  const taskId = Number(searchTaskId.value)
  if (!taskId) return
  generating.value = true
  try {
    await generateVoice({ taskId, voiceType: voiceType.value })
    message.success('配音生成任务已提交')
    load()
  } finally {
    generating.value = false
  }
}
</script>