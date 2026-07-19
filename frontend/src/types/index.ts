/** 统一 API 响应结构 */
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

/** 分页响应 */
export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 基础实体 */
export interface BaseEntity {
  id: number
  createdAt: string
  updatedAt: string
}

/** 选题 */
export interface Topic extends BaseEntity {
  title: string
  source?: string
  sourceUrl?: string
  hotScore?: number
  isAuto?: boolean
  status: string
  createdBy?: number
}

/** 任务 */
export interface Task extends BaseEntity {
  topicId?: number
  title: string
  scriptId?: number
  status: string
  progress: number
  errorMessage?: string
  createdBy?: number
}

/** 脚本 */
export interface Script extends BaseEntity {
  topicId?: number
  taskId?: number
  title?: string
  content: string
  subtitle?: string
  promptTemplateId?: number
  version: number
  status: string
  createdBy?: number
}

/** 分镜 */
export interface Storyboard extends BaseEntity {
  taskId: number
  sequence: number
  duration: number
  sceneType?: string
  character?: string
  action?: string
  environment?: string
  camera?: string
  lighting?: string
  style?: string
  aiPrompt?: string
}

/** 素材 */
export interface Material extends BaseEntity {
  taskId?: number
  storyboardId?: number
  type: string
  model?: string
  url?: string
  prompt?: string
  status: string
}

/** 配音 */
export interface Voice extends BaseEntity {
  taskId?: number
  voiceType?: string
  voiceUrl?: string
  speed?: number
  duration?: number
  status: string
}

/** 发布记录 */
export interface PublishLog extends BaseEntity {
  taskId: number
  platform: string
  accountId?: number
  title?: string
  coverUrl?: string
  tags?: string
  scheduledAt?: string
  publishedAt?: string
  status: string
  platformVideoId?: string
  errorMessage?: string
}

/** 平台账号 */
export interface PlatformAccount extends BaseEntity {
  platform: string
  accountName: string
  cookiesEncrypted?: string
  status: string
}

/** AI 模型配置 */
export interface AiModelConfig extends BaseEntity {
  modelName: string
  provider: string
  apiKeyEncrypted?: string
  endpoint?: string
  modelType: string
  defaultParams?: string
  rateLimit?: string
  enabled: boolean
  weight?: number
}

/** Prompt 模板 */
export interface PromptTemplate extends BaseEntity {
  name: string
  type: string
  content: string
  variables?: string
  enabled: boolean
}

/** 用户 */
export interface SysUser extends BaseEntity {
  username: string
  nickname?: string
  roleId?: number
  status: string
}

/** 角色 */
export interface SysRole extends BaseEntity {
  name: string
  code: string
  description?: string
}

/** 任务事件（时间线） */
export interface TaskEvent extends BaseEntity {
  taskId: number
  fromStatus?: string
  toStatus: string
  operator?: string
  comment?: string
}

/** 任务状态常量 */
export const TaskStatus = {
  WAIT: 'WAIT',
  SCRIPTING: 'SCRIPTING',
  SCRIPT_REVIEW: 'SCRIPT_REVIEW',
  STORYBOARD: 'STORYBOARD',
  GENERATING: 'GENERATING',
  VOICEOVER: 'VOICEOVER',
  EDITING: 'EDITING',
  REVIEW: 'REVIEW',
  READY: 'READY',
  PUBLISHED: 'PUBLISHED',
  CANCELLED: 'CANCELLED',
  ERROR: 'ERROR',
} as const

export type TaskStatusType = (typeof TaskStatus)[keyof typeof TaskStatus]

/** 任务状态对应进度 */
/**
 * 任务进度值映射 —— 与 Java 后端 TaskServiceImpl.updateStatus() 保持同步。
 *
 * 后端进度调用链路：
 *   createTask:     SCRIPTING=10
 *   script 回调:    SCRIPT_REVIEW=30
 *   approve:        STORYBOARD=40
 *   prompt 回调:    GENERATING=50
 *   video/image回调: VOICEOVER=60
 *   voice 回调:     EDITING=80
 *   ffmpeg 回调:    REVIEW=95
 *   发布:          PUBLISHED=100
 */
export const StatusProgress: Record<string, number> = {
  WAIT: 0,
  SCRIPTING: 10,
  SCRIPT_REVIEW: 30,
  STORYBOARD: 40,
  GENERATING: 50,
  VOICEOVER: 60,
  EDITING: 80,
  REVIEW: 95,
  READY: 95,
  PUBLISHED: 100,
}

/** 状态颜色映射 */
export const StatusColor: Record<string, string> = {
  WAIT: 'default',
  SCRIPTING: 'processing',
  SCRIPT_REVIEW: 'warning',
  STORYBOARD: 'processing',
  GENERATING: 'processing',
  VOICEOVER: 'processing',
  EDITING: 'processing',
  REVIEW: 'warning',
  READY: 'cyan',
  PUBLISHED: 'success',
  CANCELLED: 'default',
  ERROR: 'error',
  PENDING: 'default',
  PENDING_REVIEW: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  SUCCESS: 'success',
  FAILURE: 'error',
}