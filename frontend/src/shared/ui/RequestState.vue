<script setup lang="ts">
import type { ApiError } from '../api/apiError'
withDefaults(defineProps<{ status: 'idle' | 'loading' | 'success' | 'error'; error?: ApiError | null; retryable?: boolean }>(), { error: null, retryable: true })
defineEmits<{ retry: [] }>()
</script>
<template>
  <p v-if="status === 'loading'" class="request-notice" role="status">주변 충전소를 조회하고 있습니다.</p>
  <div v-else-if="status === 'error' && error" class="request-notice request-error" role="alert">
    <p>{{ error.kind === 'validation' ? '검색 조건을 확인해 주세요' : error.message }}</p>
    <ul v-if="Object.keys(error.fields).length"><li v-for="(message, field) in error.fields" :key="field">{{ message }}</li></ul>
    <button v-if="retryable" type="button" class="secondary-button" id="retry" @click="$emit('retry')">다시 조회</button>
  </div>
</template>
