<template>
  <div class="flex flex-col lg:flex-row min-h-screen w-full bg-gray-50/80 pb-20 lg:pb-0">
    <SideBar :team-id="teamId"></SideBar>
    <div class="flex flex-col flex-1 w-full min-w-0">

      <!-- ── Header ── -->
      <div class="bg-white border-b border-gray-200/80 px-4 sm:px-8 py-4 sticky top-0 z-10 backdrop-blur-sm bg-white/95">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div class="flex items-center gap-4 min-w-0">
            <!-- Back -->
            <button
              @click="$router.go(-1)"
              class="inline-flex items-center justify-center w-9 h-9 rounded-lg border border-gray-200 text-gray-500 bg-white hover:bg-gray-50 hover:text-gray-700 hover:border-gray-300 transition-all shadow-sm"
              title="Geri"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
              </svg>
            </button>

            <!-- Type Badge + Title -->
            <div class="flex items-center gap-3 min-w-0">
              <div class="flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center"
                   :class="issueTypeBadgeClass">
                <span class="text-base">{{ issueTypeIcon }}</span>
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <h1 class="text-xl sm:text-2xl font-bold text-gray-900 leading-tight truncate max-w-[320px] sm:max-w-none">
                    {{ task?.title || 'Loading...' }}
                  </h1>
                  <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold tracking-wide uppercase"
                        :class="statusBadgeClass">
                    {{ task?.status || '' }}
                  </span>
                </div>
                <div class="flex items-center gap-2 mt-0.5">
                  <span class="text-xs font-mono text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">{{ task?.customId || taskId }}</span>
                  <span v-if="task?.issueType" class="text-xs text-gray-400">·</span>
                  <span v-if="task?.issueType" class="text-xs text-gray-500 capitalize">{{ task.issueType }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex items-center gap-2">
            <button
              @click="editTask"
              class="inline-flex items-center px-4 py-2 rounded-lg border border-gray-200 text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm"
            >
              <svg class="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
              Düzenle
            </button>
            <button
              @click="cancelTask"
              class="inline-flex items-center px-4 py-2 rounded-lg text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 border border-red-200 hover:border-red-300 transition-all"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/>
              </svg>
              İptal Et
            </button>
          </div>
        </div>
      </div>

      <!-- ── Loading State ── -->
      <div v-if="loading" class="flex-1 px-4 sm:px-8 py-8">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div class="lg:col-span-2 space-y-6">
            <div class="bg-white rounded-xl border border-gray-200 p-6 animate-pulse">
              <div class="h-5 bg-gray-200 rounded w-32 mb-4"></div>
              <div class="space-y-3">
                <div class="h-3 bg-gray-200 rounded w-full"></div>
                <div class="h-3 bg-gray-200 rounded w-4/5"></div>
                <div class="h-3 bg-gray-200 rounded w-3/5"></div>
              </div>
            </div>
            <div class="bg-white rounded-xl border border-gray-200 p-6 animate-pulse">
              <div class="h-5 bg-gray-200 rounded w-28 mb-4"></div>
              <div class="h-20 bg-gray-100 rounded-lg"></div>
            </div>
          </div>
          <div class="space-y-6">
            <div class="bg-white rounded-xl border border-gray-200 p-6 animate-pulse">
              <div class="h-5 bg-gray-200 rounded w-24 mb-4"></div>
              <div class="space-y-4">
                <div class="h-8 bg-gray-100 rounded-lg"></div>
                <div class="h-8 bg-gray-100 rounded-lg"></div>
                <div class="h-8 bg-gray-100 rounded-lg"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Not Found State ── -->
      <div v-else-if="!task" class="flex items-center justify-center min-h-[400px] px-4">
        <div class="text-center max-w-sm">
          <div class="w-20 h-20 mx-auto mb-6 bg-gray-100 rounded-2xl flex items-center justify-center">
            <svg class="w-10 h-10 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-gray-900 mb-2">Görev Bulunamadı</h3>
          <p class="text-gray-500 text-sm mb-6">Aradığınız görev mevcut değil veya silinmiş olabilir.</p>
          <button @click="$router.go(-1)" class="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors shadow-sm">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
            Geri Dön
          </button>
        </div>
      </div>

      <!-- ── Main Content ── -->
      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-5 sm:gap-6 px-4 sm:px-8 py-6">

        <!-- LEFT COLUMN -->
        <div class="lg:col-span-2 space-y-5">

          <!-- Description Card -->
          <div class="task-card group">
            <div class="task-card-accent accent-blue"></div>
            <div class="task-card-body">
              <div class="flex items-center justify-between mb-4">
                <h2 class="task-section-title">
                  <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"/>
                  </svg>
                  Açıklama
                </h2>
                <div class="flex items-center space-x-2">
                  <span v-if="savingDescription" class="text-xs text-blue-500 animate-pulse flex items-center gap-1">
                    <svg class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>
                    Kaydediliyor...
                  </span>
                  <span v-else-if="descriptionSavedFlash" class="text-xs text-green-600 flex items-center gap-1">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    Kaydedildi
                  </span>
                  <button
                    v-if="!editingDescription"
                    @click="startEditingDescription"
                    class="inline-flex items-center px-2.5 py-1.5 text-xs font-medium text-gray-500 bg-gray-50 rounded-lg hover:bg-gray-100 hover:text-gray-700 transition-all"
                  >
                    <svg class="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                    Düzenle
                  </button>
                  <template v-if="editingDescription">
                    <button @click="forceSaveDescription" class="inline-flex items-center px-3 py-1.5 text-xs font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm">Kaydet</button>
                    <button @click="cancelDescriptionEdit" class="inline-flex items-center px-3 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors">Vazgeç</button>
                  </template>
                </div>
              </div>

              <!-- Edit Mode -->
              <div v-if="editingDescription" ref="descriptionEditArea" class="rounded-lg border-2 border-blue-200 bg-blue-50/30 transition-all overflow-hidden">
                <TiptapEditor
                  v-model="tempDescription"
                  :uploadHandler="taskUploadHandler"
                  placeholder="Açıklama yazın..."
                />
              </div>
              <!-- View Mode -->
              <div v-else @click="startEditingDescription" class="cursor-text min-h-[60px] rounded-lg hover:bg-gray-50/50 transition-colors p-1 -m-1">
                <RichContentViewer v-if="task.description" :content="task.description" />
                <div v-else class="flex flex-col items-center justify-center py-8 text-gray-400 border-2 border-dashed border-gray-200 rounded-lg hover:border-gray-300 hover:text-gray-500 transition-all">
                  <svg class="w-8 h-8 mb-2 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                  </svg>
                  <span class="text-sm font-medium">Açıklama eklemek için tıklayın</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Attachments -->
          <AttachmentList v-if="teamId && task?.id" :teamId="teamId" :taskId="task.id" />

          <!-- Subtasks -->
          <SubtaskList v-if="teamId && task?.id" :teamId="teamId" :taskId="task.id" :subtasks="task.subtasks || []" @update="refreshTask" @open="openSubtask" />

          <!-- Task Links -->
          <TaskLinkList v-if="teamId && task?.id" :teamId="teamId" :taskId="task.id" :links="taskLinks" @update="refreshLinks" @open="openLinkedTask" />

          <!-- Task History -->
          <TaskHistory v-if="teamId && task?.id" :teamId="teamId" :taskId="task.id" />

          <!-- Activity / Comments -->
          <div class="task-card">
            <div class="task-card-accent accent-violet"></div>
            <div class="task-card-body">
              <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-5 gap-2">
                <h2 class="task-section-title">
                  <svg class="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
                  </svg>
                  Aktivite
                </h2>
                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
                  {{ (task.comments?.length || 0) }} yorum
                </span>
              </div>

              <!-- Add Comment -->
              <div class="mb-6">
                <div class="flex items-start gap-3">
                  <div class="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0 shadow-sm">
                    {{ getCurrentUserInitials() }}
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="rounded-xl border border-gray-200 focus-within:border-blue-300 focus-within:ring-2 focus-within:ring-blue-100 transition-all overflow-hidden">
                      <TiptapEditor v-model="newComment" :uploadHandler="taskUploadHandler" placeholder="Yorumunuzu yazın..." :compact="true" />
                    </div>
                    <div class="mt-2 flex justify-end">
                      <button
                        @click="addComment"
                        :disabled="!newComment.trim() || newComment === '<p></p>'"
                        class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed transition-all shadow-sm"
                      >
                        Yorum Yap
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Comments List -->
              <div class="relative">
                <!-- Timeline line -->
                <div v-if="task.comments?.length" class="absolute left-[17px] top-0 bottom-0 w-px bg-gray-200"></div>

                <div class="space-y-4">
                  <div
                    v-for="comment in task.comments || []"
                    :key="comment.id || Math.random()"
                    class="flex items-start gap-3 relative"
                  >
                    <div class="w-9 h-9 rounded-full bg-gradient-to-br from-gray-400 to-gray-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0 shadow-sm z-10 ring-4 ring-white">
                      {{ getInitials(comment.author) }}
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="bg-gray-50 rounded-xl p-4 border border-gray-100 hover:border-gray-200 transition-colors">
                        <div class="flex flex-wrap items-center gap-x-2 gap-y-1 mb-2">
                          <span class="text-sm font-semibold text-gray-900">{{ comment.author }}</span>
                          <span class="text-[11px] text-gray-400">{{ formatRelativeTime(comment.createdAt || comment.timestamp) }}</span>
                        </div>
                        <div class="prose-sm text-gray-700">
                          <RichContentViewer :content="comment.text" />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="!task.comments?.length" class="text-center py-10">
                  <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-2xl flex items-center justify-center">
                    <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
                    </svg>
                  </div>
                  <p class="text-sm font-medium text-gray-500">Henüz yorum yok</p>
                  <p class="text-xs text-gray-400 mt-1">İlk yorumu siz yapın</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- RIGHT SIDEBAR -->
        <div class="space-y-5">

          <!-- Watchers -->
          <WatcherList v-if="teamId && task?.id" :teamId="teamId" :taskId="task.id" :watchers="task.watchers || []" @update="refreshTask" />

          <!-- Details Card -->
          <div class="task-card">
            <div class="task-card-accent accent-emerald"></div>
            <div class="task-card-body">
              <h3 class="task-section-title mb-5">
                <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
                </svg>
                Detaylar
              </h3>

              <div class="space-y-5">
                <!-- Status -->
                <div class="detail-row">
                  <label class="detail-label">Durum</label>
                  <select
                    v-model="task.status"
                    @change="updateTaskField('status', task.status)"
                    class="detail-select"
                    :class="statusSelectClass"
                  >
                    <option value="To Do">To Do</option>
                    <option value="In Progress">In Progress</option>
                    <option value="Done">Done</option>
                  </select>
                </div>

                <!-- Type -->
                <div class="detail-row">
                  <label class="detail-label">Tür</label>
                  <div class="flex items-center gap-2">
                    <div class="w-6 h-6 rounded-md flex items-center justify-center text-sm" :class="issueTypeBadgeClass">
                      {{ issueTypeIcon }}
                    </div>
                    <span class="text-sm font-medium text-gray-700 capitalize">{{ task.issueType || 'Task' }}</span>
                  </div>
                </div>

                <!-- Priority -->
                <div class="detail-row">
                  <label class="detail-label">Öncelik</label>
                  <select
                    v-model="task.priority"
                    @change="updateTaskField('priority', task.priority)"
                    class="detail-select"
                    :class="prioritySelectClass"
                  >
                    <option value="Low">🟢 Düşük</option>
                    <option value="Medium">🟡 Orta</option>
                    <option value="High">🟠 Yüksek</option>
                    <option value="Critical">🔴 Kritik</option>
                  </select>
                </div>

                <!-- Assignee -->
                <div class="detail-row">
                  <label class="detail-label">Atanan</label>
                  <div v-if="task.assignee" class="flex items-center gap-2.5">
                    <div class="w-7 h-7 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-[11px] font-bold shadow-sm">
                      {{ getInitials(task.assignee) }}
                    </div>
                    <span class="text-sm font-medium text-gray-700">{{ task.assignee }}</span>
                  </div>
                  <div v-else class="text-sm text-gray-400 italic">Atanmadı</div>
                </div>

                <!-- Story Points -->
                <div v-if="task.storyPoints" class="detail-row">
                  <label class="detail-label">Story Points</label>
                  <div class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-gradient-to-br from-amber-400 to-orange-500 text-white text-sm font-bold shadow-sm">
                    {{ task.storyPoints }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Team Assignment -->
          <div v-if="task.developer || task.analyst || task.tester" class="task-card">
            <div class="task-card-accent accent-cyan"></div>
            <div class="task-card-body">
              <h3 class="task-section-title mb-5">
                <svg class="w-5 h-5 text-light-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
                </svg>
                Ekip Ataması
              </h3>

              <div class="space-y-4">
                <div v-if="task.developer" class="flex items-center gap-3 p-2.5 rounded-lg bg-green-50/50 border border-green-100">
                  <div class="w-8 h-8 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center text-white text-xs font-bold shadow-sm">
                    {{ getInitials(task.developer) }}
                  </div>
                  <div>
                    <div class="text-[10px] uppercase tracking-wider font-semibold text-green-600">Geliştirici</div>
                    <div class="text-sm font-medium text-gray-700">{{ task.developer }}</div>
                  </div>
                </div>

                <div v-if="task.analyst" class="flex items-center gap-3 p-2.5 rounded-lg bg-blue-50/50 border border-blue-100">
                  <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-indigo-600 flex items-center justify-center text-white text-xs font-bold shadow-sm">
                    {{ getInitials(task.analyst) }}
                  </div>
                  <div>
                    <div class="text-[10px] uppercase tracking-wider font-semibold text-blue-600">Analist</div>
                    <div class="text-sm font-medium text-gray-700">{{ task.analyst }}</div>
                  </div>
                </div>

                <div v-if="task.tester" class="flex items-center gap-3 p-2.5 rounded-lg bg-purple-50/50 border border-purple-100">
                  <div class="w-8 h-8 rounded-full bg-gradient-to-br from-purple-400 to-purple-600 flex items-center justify-center text-white text-xs font-bold shadow-sm">
                    {{ getInitials(task.tester) }}
                  </div>
                  <div>
                    <div class="text-[10px] uppercase tracking-wider font-semibold text-purple-600">Test Uzmanı</div>
                    <div class="text-sm font-medium text-gray-700">{{ task.tester }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Labels -->
          <div v-if="task.labels && task.labels.length" class="task-card">
            <div class="task-card-accent accent-pink"></div>
            <div class="task-card-body">
              <h3 class="task-section-title mb-4">
                <svg class="w-5 h-5 text-pink-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/>
                </svg>
                Etiketler
              </h3>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="label in task.labels"
                  :key="label"
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-gradient-to-r from-indigo-50 to-blue-50 text-indigo-700 border border-indigo-200/50"
                >
                  {{ label }}
                </span>
              </div>
            </div>
          </div>

          <!-- Timestamps -->
          <div class="task-card">
            <div class="task-card-accent accent-gray"></div>
            <div class="task-card-body">
              <h3 class="task-section-title mb-4">
                <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                Zaman Bilgisi
              </h3>
              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <span class="text-xs text-gray-400">Oluşturulma</span>
                  <span class="text-sm text-gray-600 font-medium">{{ formatDate(task.createdAt) }}</span>
                </div>
                <div v-if="task.updatedAt" class="flex items-center justify-between">
                  <span class="text-xs text-gray-400">Son Güncelleme</span>
                  <span class="text-sm text-gray-600 font-medium">{{ formatRelativeTime(task.updatedAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Edit Task Form -->
      <AddTaskForm
        :is-open="showEditForm"
        :task="task"
        :teamId="teamId || ''"
        @close="showEditForm = false"
        @updateTask="onTaskUpdate"
      />
    </div>
  </div>
</template>

<script>
import { updateTask, searchByCustomId, getLinks } from '../api/WorkApi.js';
import { addComment, uploadAttachment } from '../api/WorkApi.js';
import AddTaskForm from '../components/work/AddTaskForm.vue';
import AttachmentList from '../components/work/AttachmentList.vue';
import SubtaskList from '../components/work/SubtaskList.vue';
import TaskLinkList from '../components/work/TaskLinkList.vue';
import TaskHistory from '../components/work/TaskHistory.vue';
import WatcherList from '../components/work/WatcherList.vue';
import TiptapEditor from '../components/docs/TiptapEditor.vue';
import RichContentViewer from '../components/work/RichContentViewer.vue';
import SideBar from "../components/SideBar.vue";

export default {
  name: 'TaskDetailPage',
  components: {
    SideBar,
    AddTaskForm,
    AttachmentList,
    SubtaskList,
    TaskLinkList,
    TaskHistory,
    WatcherList,
    TiptapEditor,
    RichContentViewer,
  },
  props: {
    taskId: String
  },
  data() {
    return {
      task: null,
      teamId: null,
      teamData: null,
      loading: true,
      newComment: '',
      showEditForm: false,
      taskLinks: [],
      // Inline description edit state
      editingDescription: false,
      tempDescription: '',
      savingDescription: false,
      descriptionSavedFlash: false
    };
  },
  computed: {
    issueTypeIcon() {
      const type = (this.task?.issueType || 'task').toLowerCase();
      const icons = { bug: '🐛', story: '📖', epic: '⚡', task: '✅', subtask: '📌' };
      return icons[type] || '✅';
    },
    issueTypeBadgeClass() {
      const type = (this.task?.issueType || 'task').toLowerCase();
      const classes = {
        bug: 'bg-red-100 text-red-700',
        story: 'bg-green-100 text-green-700',
        epic: 'bg-purple-100 text-purple-700',
        task: 'bg-blue-100 text-blue-700',
        subtask: 'bg-amber-100 text-amber-700',
      };
      return classes[type] || 'bg-blue-100 text-blue-700';
    },
    statusBadgeClass() {
      const status = (this.task?.status || '').toLowerCase();
      if (status === 'done') return 'bg-green-100 text-green-700 border border-green-200';
      if (status === 'in progress') return 'bg-blue-100 text-blue-700 border border-blue-200';
      if (status === 'cancelled') return 'bg-red-100 text-red-700 border border-red-200';
      return 'bg-gray-100 text-gray-600 border border-gray-200';
    },
    statusSelectClass() {
      const status = (this.task?.status || '').toLowerCase();
      if (status === 'done') return 'border-green-300 bg-green-50 text-green-700';
      if (status === 'in progress') return 'border-blue-300 bg-blue-50 text-blue-700';
      return 'border-gray-300 bg-white text-gray-700';
    },
    prioritySelectClass() {
      const p = (this.task?.priority || '').toLowerCase();
      if (p === 'critical') return 'border-red-300 bg-red-50 text-red-700';
      if (p === 'high') return 'border-orange-300 bg-orange-50 text-orange-700';
      if (p === 'medium') return 'border-yellow-300 bg-yellow-50 text-yellow-700';
      return 'border-green-300 bg-green-50 text-green-700';
    },
  },
  async mounted() {
    await this.loadTask();
    document.addEventListener('mousedown', this.handleDescriptionClickOutside);
  },
  beforeUnmount() {
    document.removeEventListener('mousedown', this.handleDescriptionClickOutside);
  },
  methods: {
    async loadTask() {
      try {
        this.loading = true;
        const result = await searchByCustomId(this.taskId);
        if (result) {
          this.task = result;
          this.teamId = result.teamId;
          this.teamData = null;
          await this.refreshLinks();
        } else {
          this.task = null;
        }
      } catch (error) {
        console.error('Error loading task:', error);
        this.task = null;
      } finally {
        this.loading = false;
      }
    },
    async refreshTask() {
      if (this.teamId && this.task?.id) {
        try {
          const result = await searchByCustomId(this.taskId);
          if (result) this.task = result;
        } catch (e) { console.error(e); }
      }
    },
    async refreshLinks() {
      if (this.teamId && this.task?.id) {
        try {
          this.taskLinks = await getLinks(this.teamId, this.task.id);
        } catch (e) { this.taskLinks = []; }
      }
    },
    openSubtask(sub) {
      this.$router.push({ name: 'TaskDetail', params: { taskId: sub.customId } });
    },
    openLinkedTask(taskId) {
      // ID ile customId araması için search endpoint'i kullan
      this.$router.push({ name: 'TaskDetail', params: { taskId } });
    },
    async editTask() {
      this.showEditForm = true;
    },
    cancelTask(){
      this.updateTaskField('status', "Cancelled");
    },
    // TipTap upload handler for task attachments
    async taskUploadHandler(file) {
      if (!this.teamId || !this.task?.id) return null
      const result = await uploadAttachment(this.teamId, this.task.id, file)
      return { downloadUrl: result.downloadUrl, fileName: result.fileName || file.name }
    },
    // Inline Description Editing
    startEditingDescription() {
      if (!this.task) return;
      if (this.editingDescription) return;
      this.editingDescription = true;
      this.tempDescription = this.task.description || '';
    },
    async saveDescription() {
      if (!this.editingDescription) return;
      const newValue = this.tempDescription.trim();
      if (this.task && this.task.description !== newValue) {
        if(!this.teamId || !this.task?.id){
          console.error('Missing teamId or task.id for description update', this.teamId, this.task?.id);
          this.editingDescription = false;
          return;
        }
        this.savingDescription = true;
        const oldValue = this.task.description;
        this.task.description = newValue;
        try {
          await updateTask(this.teamId, this.task.id, { description: newValue, updatedAt: new Date().toISOString() });
          this.descriptionSavedFlash = true;
          setTimeout(() => { this.descriptionSavedFlash = false; }, 1500);
        } catch (e) {
          console.error('Error updating description', e);
          alert('Description could not be saved.');
          this.task.description = oldValue;
        } finally {
          this.savingDescription = false;
        }
      }
      this.editingDescription = false;
    },
    cancelDescriptionEdit() {
      this.editingDescription = false;
      this.tempDescription = '';
    },
    forceSaveDescription() {
      this.saveDescription();
    },
    handleDescriptionClickOutside(event) {
      if (!this.editingDescription) return;
      const editArea = this.$refs.descriptionEditArea;
      if (editArea && !editArea.contains(event.target)) {
        // Check if clicked on description header buttons (Save/Cancel)
        const headerButtons = event.target.closest('.flex.items-center.space-x-2');
        if (headerButtons && headerButtons.closest('.bg-white.rounded-lg.shadow-sm')) return;
        this.saveDescription();
      }
    },
    async addComment() {
      if (!this.newComment.trim() || this.newComment === '<p></p>') return;
      try {
        if(!this.teamId || !this.task?.id){
          console.error('Missing teamId or task.id for adding comment', this.teamId, this.task?.id);
          return;
        }
        const updatedTask = await addComment(this.teamId, this.task.id, this.newComment.trim());
        this.task.comments = updatedTask.comments || this.task.comments;
        this.newComment = '';
      } catch (error) {
        console.error('Error adding comment:', error);
        alert('Failed to add comment. Please try again.');
      }
    },
    updateTaskField(field, value) {
      if (this.task) {
        this.task[field] = value;
        if(!this.teamId || !this.task?.id){
          console.error('Missing teamId or task.id for updating field', this.teamId, this.task?.id);
          return;
        }
        updateTask(this.teamId, this.task.id, { [field]: value, updatedAt: new Date().toISOString() });
      }
    },
    onTaskUpdate(updatedTask) {
      this.task = { ...this.task, ...updatedTask };
    },
    getCurrentUserName() {
      return localStorage.getItem('user');
    },
    getCurrentUserInitials() {
      return this.getInitials(this.getCurrentUserName());
    },
    getInitials(emailOrName) {
      if (!emailOrName) return '?';
      // email ise @ öncesini al
      const name = emailOrName.includes('@') ? emailOrName.split('@')[0] : emailOrName;
      const parts = name.toUpperCase().replace(/[._-]/g, ' ').split(' ');
      return parts.length > 1 ? parts[0][0] + parts[1][0] : (parts[0][0] || '?');
    },
    formatDate(dateString) {
      if (!dateString) return '';
      const options = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
      return new Date(dateString).toLocaleString('en-US', options);
    },
    formatRelativeTime(dateString) {
      if (!dateString) return '';
      const now = new Date();
      const date = new Date(dateString);
      const diffMs = now - date;
      const diffSec = Math.floor(diffMs / 1000);
      const diffMin = Math.floor(diffSec / 60);
      const diffHour = Math.floor(diffMin / 60);
      const diffDay = Math.floor(diffHour / 24);

      if (diffSec < 60) return 'az önce';
      if (diffMin < 60) return `${diffMin} dk önce`;
      if (diffHour < 24) return `${diffHour} saat önce`;
      if (diffDay < 7) return `${diffDay} gün önce`;
      return this.formatDate(dateString);
    }
  }
};
</script>

<style scoped>
/* ── Card System ── */
.task-card {
  @apply bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden transition-shadow duration-200;
}
.task-card:hover {
  @apply shadow-md;
}
.task-card-accent {
  @apply h-1 w-full;
}
.task-card-body {
  @apply p-5 sm:p-6;
}

/* Accent colors */
.accent-blue { @apply bg-gradient-to-r from-blue-500 to-indigo-500; }
.accent-emerald { @apply bg-gradient-to-r from-green-400 to-green-600; }
.accent-violet { @apply bg-gradient-to-r from-purple-500 to-purple-700; }
.accent-cyan { @apply bg-gradient-to-r from-light-blue-400 to-blue-500; }
.accent-pink { @apply bg-gradient-to-r from-pink-400 to-pink-600; }
.accent-gray { @apply bg-gradient-to-r from-gray-300 to-gray-400; }

/* Section titles */
.task-section-title {
  @apply text-base font-semibold text-gray-800 flex items-center gap-2;
}

/* Detail rows */
.detail-row {
  @apply space-y-1.5;
}
.detail-label {
  @apply block text-[11px] font-semibold text-gray-400 uppercase tracking-wider;
}
.detail-select {
  @apply w-full px-3 py-2 rounded-lg text-sm font-medium transition-all focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-blue-400 cursor-pointer;
}
</style>
