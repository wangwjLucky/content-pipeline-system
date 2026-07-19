import request from './request'

export function compileEdit(data: { taskId: number }) {
  return request.post('/edits/compile', data)
}

export function getEditPreview(taskId: number) {
  return request.get(`/edits/${taskId}/preview`)
}

export function regenerateEdit(taskId: number) {
  return request.post(`/edits/${taskId}/regenerate`)
}