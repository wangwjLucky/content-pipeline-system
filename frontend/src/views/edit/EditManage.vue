<template>
  <a-card title="剪辑合成">
    <template #extra>
      <a-space>
        <a-input-search v-model:value="searchTaskId" placeholder="任务 ID" style="width: 160px" @search="load" />
      </a-space>
    </template>

    <a-form layout="inline" style="margin-bottom: 16px">
      <a-form-item>
        <a-button type="primary" :loading="compiling" @click="handleCompile">启动剪辑合成</a-button>
      </a-form-item>
      <a-form-item>
        <a-button :disabled="!previewUrl" @click="handleRegenerate">重新剪辑</a-button>
      </a-form-item>
    </a-form>

    <a-descriptions v-if="editInfo" bordered :column="2">
      <a-descriptions-item label="任务 ID">{{ editInfo.taskId }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="statusColor">{{ editInfo.status || 'PENDING' }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="预览地址" :span="2">
        <template v-if="previewUrl">
          <video :src="previewUrl" controls style="max-width: 100%; max-height: 300px" />
        </template>
        <span v-else>-</span>
      </a-descriptions-item>
      <a-descriptions-item label="字幕状态" :span="2">
        <a-tag :color="subtitleStatus === 'SUCCESS' ? 'green' : 'orange'">{{ subtitleStatus || '未生成' }}</a-tag>
      </a-descriptions-item>
    </a-descriptions>

    <a-empty v-if="!editInfo && !loading" description="请输入任务 ID 查询剪辑状态" />
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { compileEdit, getEditPreview, regenerateEdit } from '../../api/edit'

const searchTaskId = ref('')
const editInfo = ref<any>(null)
const previewUrl = ref('')
const subtitleStatus = ref('')
const loading = ref(false)
const compiling = ref(false)
const regenerating = ref(false)

const statusColor = computed(() => {
  const s = editInfo.value?.status
  if (!s) return 'default'
  if (s === 'SUCCESS' || s === 'COMPLETED') return 'green'
  if (s === 'FAILURE' || s === 'ERROR') return 'red'
  return 'orange'
})

async function load() {
  if (!searchTaskId.value) return
  const taskId = Number(searchTaskId.value)
  if (!taskId) { message.warning('请输入有效的任务 ID'); return }
  loading.value = true
  try {
    const res: any = await getEditPreview(taskId)
    if (res.data) {
      editInfo.value = res.data
      previewUrl.value = res.data.previewUrl || ''
      subtitleStatus.value = res.data.subtitleStatus || ''
    } else {
      editInfo.value = { taskId, status: 'PENDING' }
    }
  } catch {
    editInfo.value = { taskId: Number(searchTaskId.value), status: 'PENDING' }
  } finally {
    loading.value = false
  }
}

async function handleCompile() {
  if (!searchTaskId.value) { message.warning('请输入任务 ID'); return }
  const taskId = Number(searchTaskId.value)
  if (!taskId) return
  compiling.value = true
  try {
    await compileEdit({ taskId })
    message.success('剪辑合成任务已提交')
    setTimeout(() => load(), 1000)
  } finally {
    compiling.value = false
  }
}

async function handleRegenerate() {
  if (!searchTaskId.value) return
  regenerating.value = true
  try {
    await regenerateEdit(Number(searchTaskId.value))
    message.success('重新剪辑已触发')
  } finally {
    regenerating.value = false
  }
}
</script>