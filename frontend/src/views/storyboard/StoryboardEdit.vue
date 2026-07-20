<template>
  <a-card title="分镜编辑" :loading="loading">
    <template #extra>
      <a-space>
        <a-button @click="handleAutoSplit">AI 自动拆分</a-button>
        <a-button type="primary" @click="handleSave">批量保存</a-button>
      </a-space>
    </template>

    <a-table :dataSource="storyboards" :columns="columns" rowKey="sequence" :pagination="false">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'sequence'">{{ index + 1 }}</template>
        <template v-if="column.key === 'sceneType'">
          <a-select v-model:value="record.sceneType" style="width: 100px">
            <a-select-option value="medium">中景</a-select-option>
            <a-select-option value="closeup">特写</a-select-option>
            <a-select-option value="wide">远景</a-select-option>
            <a-select-option value="overview">全景</a-select-option>
          </a-select>
        </template>
        <template v-if="column.key === 'duration'">
          <a-input-number v-model:value="record.duration" :min="1" :max="30" />
        </template>
        <template v-if="['character', 'action', 'environment', 'camera', 'lighting', 'style'].includes(column.key)">
          <a-input v-model:value="record[column.key]" :title="fieldLabel(column.key, record[column.key])" />
        </template>
        <template v-if="column.key === 'aiPrompt'">
          <a-input v-model:value="record.aiPrompt" />
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getStoryboards, batchSaveStoryboards, autoSplitStoryboard } from '../../api/storyboard'

const route = useRoute()
const loading = ref(false)
const storyboards = ref<any[]>([])

const columns = [
  { title: '序号', key: 'sequence', width: 60 },
  { title: '类型', key: 'sceneType', width: 80 },
  { title: '角色', key: 'character' },
  { title: '动作', key: 'action' },
  { title: '环境', key: 'environment' },
  { title: '运镜', key: 'camera', width: 80 },
  { title: '灯光', key: 'lighting', width: 80 },
  { title: '风格', key: 'style', width: 80 },
  { title: '时长', key: 'duration', width: 70 },
  { title: 'AI 提示词', key: 'aiPrompt' },
]

function fieldLabel(field: string, value: string) {
  const labels: Record<string, Record<string, string>> = {
    character: { host: '主持人', guest: '嘉宾', narrator: '旁白', actor: '演员' },
    action: { introduction: '介绍', explanation: '讲解', demonstration: '演示', interview: '访谈', discussion: '讨论' },
    environment: { studio: '演播室', outdoor: '户外', office: '办公室', classroom: '教室', nature: '自然' },
    camera: { front: '正面', close: '近景', wide: '广角', top: '俯拍', low: '仰拍', aerial: '航拍' },
    lighting: { bright: '明亮', soft: '柔和', dark: '昏暗', colorful: '多彩', natural: '自然光' },
    style: { modern: '现代', classic: '经典', cartoon: '卡通', minimalist: '简约', futuristic: '未来' },
  }
  return labels[field]?.[value] || value
}

async function load() {
  loading.value = true
  try {
    const res: any = await getStoryboards(Number(route.params.taskId))
    storyboards.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  await batchSaveStoryboards(Number(route.params.taskId), storyboards.value)
  message.success('保存成功')
}

async function handleAutoSplit() {
  await autoSplitStoryboard(Number(route.params.taskId))
  message.success('已触发 AI 自动拆分')
  setTimeout(load, 2000)
}

onMounted(() => load())
</script>