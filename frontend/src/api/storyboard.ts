import request from './request'

export function getStoryboards(taskId: number) {
  return request.get(`/tasks/${taskId}/storyboard`)
}

export function batchSaveStoryboards(taskId: number, storyboards: any[]) {
  return request.post(`/tasks/${taskId}/storyboard`, storyboards)
}

export function autoSplitStoryboard(taskId: number) {
  return request.post(`/tasks/${taskId}/storyboard/auto-split`)
}