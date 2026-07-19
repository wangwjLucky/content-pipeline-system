<template>
  <a-card :title="`脚本审核 #${script?.id}`" :loading="loading">
    <a-descriptions v-if="script" bordered :column="2">
      <a-descriptions-item label="标题">{{ script.title }}</a-descriptions-item>
      <a-descriptions-item label="版本">v{{ script.version }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="statusColor(script.status)">{{ script.status }}</a-tag>
      </a-descriptions-item>
    </a-descriptions>

    <a-divider>脚本正文</a-divider>
    <a-input v-model:value="editableContent" type="textarea" :rows="12" />

    <a-divider>字幕文本</a-divider>
    <a-input v-model:value="editableSubtitle" type="textarea" :rows="4" />

    <div style="margin-top: 16px; text-align: right">
      <a-space>
        <a-button @click="handleSave">保存草稿</a-button>
        <a-button @click="handleRegenerate" :loading="regenerating">重新生成</a-button>
        <a-button type="primary" v-if="script?.status === 'PENDING_REVIEW'" @click="handleApprove">批准</a-button>
        <a-button danger v-if="script?.status === 'PENDING_REVIEW'" @click="handleReject">驳回</a-button>
      </a-space>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getScript, updateScript, approveScript, rejectScript, generateScript } from '../../api/script'

const route = useRoute()
const script = ref<any>(null)
const loading = ref(false)
const regenerating = ref(false)
const editableContent = ref('')
const editableSubtitle = ref('')

function statusColor(s: string) {
  const map: Record<string, string> = { PENDING_REVIEW: 'purple', APPROVED: 'green', REJECTED: 'red' }
  return map[s] || 'default'
}

async function load() {
  loading.value = true
  try {
    const res: any = await getScript(Number(route.params.id))
    script.value = res.data
    editableContent.value = res.data?.content || ''
    editableSubtitle.value = res.data?.subtitle || ''
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  await updateScript(Number(route.params.id), { content: editableContent.value, subtitle: editableSubtitle.value })
  message.success('已保存')
}

async function handleRegenerate() {
  if (!script.value) return
  regenerating.value = true
  try {
    await generateScript({ taskId: script.value.taskId, topicTitle: script.value.title })
    message.success('重新生成任务已提交')
  } finally {
    regenerating.value = false
  }
}

async function handleApprove() {
  Modal.confirm({ title: '批准脚本后任务将进入分镜阶段，确定？', onOk: async () => { await approveScript(Number(route.params.id)); message.success('已批准'); load() } })
}

async function handleReject() {
  let reason = ''
  Modal.confirm({
    title: '驳回脚本',
    content: h('div', [
      h('p', '请输入驳回原因'),
      h('a-textarea', {
        value: reason,
        onChange: (e: any) => { reason = e.target?.value || '' },
      }),
    ]),
    onOk: async () => {
      await rejectScript(Number(route.params.id), reason)
      message.success('已驳回')
      load()
    },
  })
}

onMounted(() => load())
</script>