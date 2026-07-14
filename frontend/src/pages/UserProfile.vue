<template>
  <div class="min-h-screen bg-gray-50 p-4 sm:p-6">
    <div class="max-w-3xl mx-auto space-y-6">

      <!-- Başlık -->
      <div class="flex items-center gap-3">
        <div class="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center">
          <svg class="w-6 h-6 text-purple-600" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
          </svg>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">Profilim</h1>
          <p class="text-gray-500 text-sm mt-0.5">Hesap bilgilerinizi görüntüleyin ve düzenleyin</p>
        </div>
      </div>

      <!-- Yükleniyor -->
      <div v-if="loading" class="bg-white rounded-xl shadow border border-gray-200 p-12 text-center">
        <div class="w-10 h-10 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin mx-auto mb-3"></div>
        <p class="text-gray-500 text-sm">Profil yükleniyor...</p>
      </div>

      <template v-else-if="profile">

        <!-- Avatar & Kimlik Kartı -->
        <div class="bg-white rounded-xl shadow border border-gray-200 p-6">
          <div class="flex items-center gap-6">
            <!-- Avatar -->
            <div class="relative flex-shrink-0">
              <div v-if="profile.avatarUrl" class="w-24 h-24 rounded-2xl overflow-hidden border-2 border-gray-200 shadow-sm">
                <img :src="profile.avatarUrl" :alt="profile.name" class="w-full h-full object-cover" />
              </div>
              <div v-else class="w-24 h-24 rounded-2xl bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center text-white text-3xl font-bold shadow-sm">
                {{ profile.name?.charAt(0)?.toUpperCase() }}
              </div>
            </div>

            <!-- Bilgiler -->
            <div class="flex-1 min-w-0">
              <h2 class="text-xl font-bold text-gray-900 truncate">{{ profile.name }}</h2>
              <p class="text-gray-500 text-sm mt-0.5 flex items-center gap-1.5">
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z"/><path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z"/>
                </svg>
                {{ profile.email }}
              </p>
              <div class="flex items-center gap-2 mt-3 flex-wrap">
                <span class="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full font-medium" :class="roleBadge(profile.systemRole)">
                  <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
                  {{ roleLabel(profile.systemRole) }}
                </span>
                <span class="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full font-medium" :class="statusBadge(profile.status)">
                  <span class="w-1.5 h-1.5 rounded-full" :class="profile.status === 'ACTIVE' ? 'bg-green-500' : 'bg-red-500'"></span>
                  {{ statusLabel(profile.status) }}
                </span>
              </div>
            </div>

            <!-- Meta bilgiler -->
            <div class="hidden sm:flex flex-col gap-2 text-right text-xs text-gray-400 flex-shrink-0">
              <div v-if="profile.locale" class="flex items-center gap-1.5 justify-end">
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M7 2a1 1 0 011 1v1h3a1 1 0 110 2H9.578a18.87 18.87 0 01-1.724 4.78c.29.354.596.696.914 1.026a1 1 0 11-1.44 1.389c-.188-.196-.373-.396-.554-.6a19.098 19.098 0 01-3.107 3.567 1 1 0 01-1.334-1.49 17.087 17.087 0 003.13-3.733 18.992 18.992 0 01-1.487-3.754 1 1 0 111.93-.525c.248.917.55 1.796.92 2.616a16.801 16.801 0 001.19-3.346H3a1 1 0 110-2h3V3a1 1 0 011-1zm6 6a1 1 0 01.894.553l2.991 5.982a.869.869 0 01.02.037l.99 1.98a1 1 0 11-1.79.895L15.383 16h-4.764l-.724 1.447a1 1 0 11-1.788-.894l.99-1.98.019-.038 2.99-5.982A1 1 0 0113 8zm-1.382 6h2.764L13 11.236 11.618 14z" clip-rule="evenodd"/></svg>
                {{ profile.locale === 'tr' ? 'Türkçe' : 'English' }}
              </div>
              <div v-if="profile.timezone" class="flex items-center gap-1.5 justify-end">
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"/></svg>
                {{ profile.timezone }}
              </div>
            </div>
          </div>
        </div>

        <!-- Profil Düzenleme Formu -->
        <div class="bg-white rounded-xl shadow border border-gray-200">
          <div class="px-6 py-4 border-b border-gray-100">
            <h3 class="font-semibold text-gray-900">Profil Bilgileri</h3>
            <p class="text-sm text-gray-500 mt-0.5">Kişisel bilgilerinizi güncelleyin</p>
          </div>
          <form @submit.prevent="saveProfile" class="p-6 space-y-5">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label class="label">Ad Soyad</label>
                <input v-model="form.name" type="text" class="input-field" placeholder="Adınız" />
              </div>
              <div>
                <label class="label">Telefon</label>
                <input v-model="form.phone" type="tel" class="input-field" placeholder="+90 5xx xxx xx xx" />
              </div>
              <div>
                <label class="label">Dil</label>
                <select v-model="form.locale" class="input-field">
                  <option value="tr">🇹🇷 Türkçe</option>
                  <option value="en">🇬🇧 English</option>
                </select>
              </div>
              <div>
                <label class="label">Zaman Dilimi</label>
                <select v-model="form.timezone" class="input-field">
                  <option value="Europe/Istanbul">Istanbul (UTC+3)</option>
                  <option value="UTC">UTC</option>
                  <option value="Europe/London">London (UTC+1)</option>
                  <option value="America/New_York">New York (UTC-5)</option>
                </select>
              </div>
              <div class="sm:col-span-2">
                <label class="label">Avatar URL</label>
                <div class="flex gap-3 items-center">
                  <div v-if="form.avatarUrl" class="w-10 h-10 rounded-lg overflow-hidden flex-shrink-0 border border-gray-200">
                    <img :src="form.avatarUrl" class="w-full h-full object-cover" alt="Avatar önizleme" @error="$event.target.style.display='none'"/>
                  </div>
                  <input v-model="form.avatarUrl" type="url" class="input-field" placeholder="https://..." />
                </div>
              </div>
            </div>

            <div class="flex justify-end pt-2">
              <button type="submit" :disabled="saving"
                class="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium text-sm">
                <svg v-if="saving" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                </svg>
                {{ saving ? 'Kaydediliyor...' : 'Değişiklikleri Kaydet' }}
              </button>
            </div>
          </form>
        </div>

        <!-- Git Hesapları (kişisel PAT) -->
        <UserScmAccountsCard />

        <!-- Bekleyen Davetler -->
        <div class="bg-white rounded-xl shadow border border-gray-200">
          <div class="px-6 py-4 border-b border-gray-100">
            <h3 class="font-semibold text-gray-900">Bekleyen Davetler</h3>
            <p class="text-sm text-gray-500 mt-0.5">Kabul veya reddetmeniz gereken davetler</p>
          </div>
          <div class="p-6">
            <PendingInvitations />
          </div>
        </div>

      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import UserApi from '../api/UserApi.js'
import PendingInvitations from '../components/invitation/PendingInvitations.vue'
import UserScmAccountsCard from '../components/scm/UserScmAccountsCard.vue'

const profile = ref(null)
const loading = ref(false)
const saving = ref(false)

const form = ref({
  name: '',
  phone: '',
  locale: 'tr',
  timezone: 'Europe/Istanbul',
  avatarUrl: '',
})

async function loadProfile() {
  loading.value = true
  try {
    const res = await UserApi.getProfile()
    profile.value = res.data
    form.value = {
      name: res.data.name || '',
      phone: res.data.phone || '',
      locale: res.data.locale || 'tr',
      timezone: res.data.timezone || 'Europe/Istanbul',
      avatarUrl: res.data.avatarUrl || '',
    }
  } catch (e) {
    console.error('Profil yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    const res = await UserApi.updateProfile(form.value)
    profile.value = res.data
  } catch (e) {
    console.error('Profil kaydedilemedi:', e)
  } finally {
    saving.value = false
  }
}

function roleBadge(role) {
  if (role === 'SUPER_ADMIN') return 'bg-red-100 text-red-700'
  if (role === 'PLATFORM_ADMIN') return 'bg-orange-100 text-orange-700'
  return 'bg-blue-100 text-blue-700'
}

function roleLabel(role) {
  if (role === 'SUPER_ADMIN') return 'Süper Admin'
  if (role === 'PLATFORM_ADMIN') return 'Platform Admin'
  return 'Kullanıcı'
}

function statusBadge(status) {
  if (status === 'ACTIVE') return 'bg-green-100 text-green-700'
  if (status === 'SUSPENDED') return 'bg-red-100 text-red-700'
  return 'bg-gray-100 text-gray-600'
}

function statusLabel(status) {
  if (status === 'ACTIVE') return 'Aktif'
  if (status === 'SUSPENDED') return 'Askıya Alındı'
  return status
}

onMounted(loadProfile)
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
</style>
