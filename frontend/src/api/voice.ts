import request from './request'

export function getVoice(taskId: number) {
  return request.get(`/voices/${taskId}`)
}

export function generateVoice(data: { taskId: number; voiceType?: string }) {
  return request.post('/voices/generate', data)
}

export function updateVoice(taskId: number, data: { voiceType?: string; speed?: number }) {
  return request.put(`/voices/${taskId}`, data)
}