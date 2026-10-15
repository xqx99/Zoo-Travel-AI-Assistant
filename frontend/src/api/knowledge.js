import axios from 'axios'

export const listKnowledge = () =>
  axios.get('/api/knowledge/list').then(r => r.data.data)

export const createKnowledge = (data) =>
  axios.post('/api/knowledge', data).then(r => r.data)

export const updateKnowledge = (id, data) =>
  axios.put(`/api/knowledge/${id}`, data).then(r => r.data)

export const deleteKnowledge = (id) =>
  axios.delete(`/api/knowledge/${id}`).then(r => r.data)

export const reindex = () =>
  axios.post('/api/knowledge/reindex').then(r => r.data)
