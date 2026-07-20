<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header -->
    <div class="bg-white border-b border-gray-200 px-4 sm:px-6 py-4">
      <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center space-y-4 sm:space-y-0">
        <div class="flex flex-col sm:flex-row sm:items-center sm:space-x-4 space-y-2 sm:space-y-0">
          <h1 class="text-xl sm:text-2xl font-semibold text-gray-900">Backlog</h1>
          <div class="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs sm:text-sm text-gray-500">
            <template v-if="isLoading">
              <span class="inline-block h-3 w-14 rounded bg-gray-200 animate-pulse"></span>
              <span class="hidden sm:inline">•</span>
              <span class="inline-block h-3 w-14 rounded bg-gray-200 animate-pulse"></span>
            </template>
            <template v-else>
              <span>{{ tasks.length }} issues</span>
              <span class="hidden sm:inline">•</span>
              <span>{{ activeSprints.length }} sprints</span>
            </template>
          </div>
        </div>
        <div class="flex flex-wrap sm:flex-nowrap items-center gap-2 sm:space-x-3">
          <button
            class="flex-1 sm:flex-none inline-flex items-center justify-center px-3 sm:px-4 py-2 border border-gray-300 rounded-lg shadow-sm text-xs sm:text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 hover:border-gray-400 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            @click="toggleSprintForm"
          >
            <svg class="w-4 h-4 mr-1 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
            </svg>
            <span class="hidden xs:inline">Create Sprint</span>
            <span class="xs:hidden">Sprint</span>
          </button>
          <button
            class="flex-1 sm:flex-none inline-flex items-center justify-center px-3 sm:px-4 py-2 border border-transparent rounded-lg shadow-sm text-xs sm:text-sm font-medium text-white bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 transition-all focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            @click="showCreateTask = !showCreateTask"
          >
            <svg class="w-4 h-4 mr-1 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
            </svg>
            <span class="hidden xs:inline">Create Issue</span>
            <span class="xs:hidden">Issue</span>
          </button>
        </div>
      </div>
    </div>

    <div class="px-3 sm:px-6 py-6">
      <!-- Task Oluşturma/Düzenleme Formu -->
      <AddTaskForm
        :is-open="showCreateTask || !!editingTask"
        :task="editingTask"
        :teamId="teamId"
        :project-id="projectId"
        :projects="projects"
        @close="closeTaskForm"
        @addTask="handleTaskAdded"
        @updateTask="handleUpdateTask"
        @deleteTask="handleDeleteTask"
      ></AddTaskForm>

      <!-- Sprint Oluşturma/Düzenleme Formu -->
      <div v-if="showCreateSprint" ref="sprintForm" class="mb-6 bg-white rounded-xl shadow-sm border border-gray-200 p-4 space-y-3">
        <h3 v-if="editingSprint" class="text-sm font-semibold text-gray-700">"{{ editingSprint.name }}" sprintini düzenle</h3>
        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <input
            v-model="newSprintName"
            class="flex-1 px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            placeholder="Sprint adı"
          />
        </div>
        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div class="flex-1">
            <label class="block text-xs text-gray-500 mb-1">Başlangıç Tarihi</label>
            <input
              v-model="newSprintStartDate"
              type="date"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            />
          </div>
          <div class="flex-1">
            <label class="block text-xs text-gray-500 mb-1">Bitiş Tarihi</label>
            <input
              v-model="newSprintEndDate"
              type="date"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            />
          </div>
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">Sprint Hedefi</label>
          <textarea
            v-model="newSprintGoal"
            rows="2"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm resize-none"
            placeholder="Bu sprint'te neyi başarmayı hedefliyorsunuz?"
          ></textarea>
        </div>
        <div class="flex gap-2 justify-end">
          <button
            class="px-4 py-2 text-gray-600 hover:text-gray-800 text-sm"
            @click="closeSprintForm"
          >
            İptal
          </button>
          <button
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
            @click="saveSprint"
          >
            {{ editingSprint ? 'Kaydet' : 'Sprint Oluştur' }}
          </button>
        </div>
      </div>

      <!-- İskelet (skeleton) yükleyici — ilk veri gelene kadar boş/donuk ekran
           yerine gerçek kart yapısını taklit eden bir önizleme gösterir -->
      <div v-if="isLoading" class="space-y-8" aria-busy="true" aria-live="polite">
        <div v-for="block in skeletonBlocks" :key="block.key" class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="bg-gray-50 px-4 sm:px-6 py-3 sm:py-4 border-b border-gray-200 flex items-center gap-3">
            <div class="w-8 h-8 rounded-full bg-gray-200 animate-pulse flex-shrink-0"></div>
            <div class="h-4 w-32 rounded bg-gray-200 animate-pulse"></div>
            <div class="h-4 w-16 rounded-full bg-gray-200 animate-pulse"></div>
          </div>
          <div class="p-3 sm:p-4 space-y-2">
            <div v-for="row in block.rows" :key="row" class="bg-white border border-gray-100 rounded-xl p-3 sm:p-4 animate-pulse">
              <div class="flex items-start gap-3">
                <div class="hidden sm:block w-6 h-6 rounded bg-gray-200 flex-shrink-0"></div>
                <div class="flex-1 space-y-2.5">
                  <div class="flex items-center gap-2">
                    <div class="h-3 w-12 rounded bg-gray-200"></div>
                    <div class="h-4 w-16 rounded-full bg-gray-200"></div>
                  </div>
                  <div class="h-4 rounded bg-gray-200" style="max-width: 65%"></div>
                  <div class="h-3 w-1/4 rounded bg-gray-200"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Backlog + Sprint bölümleri — tek bir kart şablonu üzerinden render
           edilir; böylece iki bölüm arasında görünüm/renk tutarsızlığı oluşmaz -->
      <template v-else>
        <div
          v-for="section in sections"
          :key="section.key"
          class="mb-8"
        >
          <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <!-- Backlog Başlığı -->
            <div
              v-if="section.type === 'backlog'"
              class="bg-gray-50 px-4 sm:px-6 py-3 sm:py-4 border-b border-gray-200 flex items-center justify-between"
            >
              <div class="flex items-center space-x-2 sm:space-x-3">
                <div class="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center flex-shrink-0">
                  <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
                  </svg>
                </div>
                <h3 class="text-base sm:text-lg font-medium text-gray-900">Backlog</h3>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] sm:text-xs font-medium bg-gray-100 text-gray-800">
                  {{ section.tasks.length }} issues
                </span>
              </div>
            </div>

            <!-- Sprint Başlığı -->
            <div
              v-else
              class="px-4 sm:px-6 py-3 sm:py-4 border-b border-gray-200 flex flex-col sm:flex-row sm:items-center sm:justify-between cursor-pointer hover:bg-gray-50/70 transition-colors gap-3"
              :class="{ 'bg-gradient-to-r from-blue-50/60 to-indigo-50/40 border-blue-100': section.sprint.status === 'open' }"
              @click="showSprintDetails(section.sprint)"
            >
              <div class="flex items-start sm:items-center space-x-3 min-w-0">
                <div class="flex-shrink-0">
                  <div
                    class="w-8 h-8 rounded-full flex items-center justify-center"
                    :class="section.sprint.status === 'open' ? 'bg-green-100' : 'bg-gray-100'"
                  >
                    <svg
                      class="w-4 h-4"
                      :class="section.sprint.status === 'open' ? 'text-green-600' : 'text-gray-500'"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                    </svg>
                  </div>
                </div>
                <div class="min-w-0">
                  <h3 class="text-base sm:text-lg font-medium text-gray-900 hover:text-blue-600 transition-colors truncate">{{ section.sprint.name }}</h3>
                  <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs sm:text-sm text-gray-500 mt-1 sm:mt-0">
                    <span class="inline-flex items-center">
                      <span
                        class="w-2 h-2 rounded-full mr-1.5"
                        :class="section.sprint.status === 'open' ? 'bg-green-400 animate-pulse' : 'bg-gray-400'"
                      ></span>
                      {{ section.sprint.status === 'open' ? 'Active Sprint' : 'Sprint' }}
                    </span>
                    <span>{{ section.tasks.length }} issues</span>
                    <span v-if="section.sprint.status === 'open' && section.tasks.length" class="flex items-center gap-1.5">
                      <span class="w-14 h-1.5 bg-gray-200 rounded-full overflow-hidden inline-block align-middle">
                        <span
                          class="h-full block rounded-full transition-all duration-500"
                          :class="sprintCompletionPercent(section.sprint.id) === 100 ? 'bg-green-500' : 'bg-blue-500'"
                          :style="{ width: sprintCompletionPercent(section.sprint.id) + '%' }"
                        ></span>
                      </span>
                      <span class="text-[11px] font-medium text-gray-500">%{{ sprintCompletionPercent(section.sprint.id) }}</span>
                    </span>
                    <span v-if="section.sprint.startDate" class="text-xs text-gray-400">
                      {{ formatSprintDate(section.sprint.startDate) }} — {{ formatSprintDate(section.sprint.endDate) }}
                    </span>
                    <span v-if="section.sprint.status === 'open' && remainingDays(section.sprint) !== null"
                      class="text-[10px] font-semibold px-1.5 py-0.5 rounded-full"
                      :class="remainingDays(section.sprint) <= 2 ? 'text-red-700 bg-red-100' : 'text-gray-600 bg-gray-100'"
                    >
                      {{ remainingDays(section.sprint) > 0 ? remainingDays(section.sprint) + 'g' : 'Bugün!' }}
                    </span>
                  </div>
                  <p v-if="section.sprint.goal" class="text-xs text-indigo-600 mt-1 italic truncate">
                    <span class="font-semibold not-italic">Hedef:</span> {{ section.sprint.goal }}
                  </p>
                </div>
              </div>
              <div class="flex items-center space-x-2 flex-shrink-0" @click.stop>
                <button
                  class="inline-flex items-center px-2 sm:px-3 py-2 border border-gray-300 text-xs sm:text-sm leading-4 font-medium rounded-lg text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                  title="Sprinti düzenle"
                  @click="openEditSprint(section.sprint)"
                >
                  <svg class="w-4 h-4 sm:mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                  </svg>
                  <span class="hidden sm:inline">Düzenle</span>
                </button>
                <button
                  v-if="section.sprint.status !== 'open'"
                  class="inline-flex items-center px-2 sm:px-3 py-2 border border-transparent text-xs sm:text-sm leading-4 font-medium rounded-lg text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                  @click="startSprint(section.sprint.id)"
                >
                  <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M12 3v1m0 16v1m9-9h-1M3 12h1m15.364-6.364l-.707.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707"/>
                  </svg>
                  <span class="hidden xs:inline">Start Sprint</span>
                  <span class="xs:hidden">Start</span>
                </button>
                <button
                  v-else
                  class="inline-flex items-center px-2 sm:px-3 py-2 border border-transparent text-xs sm:text-sm leading-4 font-medium rounded-lg text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                  @click="finishSprint(section.sprint.id)"
                >
                  <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                  <span class="hidden xs:inline">Finish Sprint</span>
                  <span class="xs:hidden">Finish</span>
                </button>
              </div>
            </div>

            <!-- Görev Kartları (backlog ve sprint bölümleri aynı şablonu paylaşır) -->
            <div
              class="p-3 sm:p-4 space-y-2 transition-colors"
              :class="[
                section.type === 'backlog' ? 'min-h-[200px]' : 'min-h-[150px]',
                dragOverKey === section.key ? 'bg-blue-50/50' : ''
              ]"
              @drop="onDrop(section.sprint ? section.sprint.id : null, section.key)"
              @dragover.prevent
              @dragenter.prevent="onDragEnter(section.key)"
              @dragleave="onDragLeave(section.key)"
            >
              <div
                v-for="task in section.tasks"
                :key="task.id"
                class="group relative bg-white border border-gray-100 rounded-xl p-3 sm:p-4 pl-4 sm:pl-5 overflow-hidden hover:shadow-md hover:-translate-y-0.5 hover:border-gray-200 transition-all duration-150 cursor-pointer"
                :draggable="!isMobile"
                @dragstart="!isMobile && onDragStart(task)"
                @click="openTaskDetail(task)"
              >
                <!-- Öncelik renk çubuğu -->
                <div class="absolute left-0 top-0 bottom-0 w-[3px]" :class="priorityBarClass(task.priority)"></div>

                <div class="flex items-start space-x-3">
                  <span
                    class="hidden sm:flex flex-shrink-0 w-6 h-6 rounded items-center justify-center"
                    :class="issueTypeIconClass(task.issueType)"
                  >
                    <svg v-if="task.issueType === 'bug'" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M6.56 1.14a.75.75 0 01.177 1.045 3.989 3.989 0 00-.464.86c.185.17.382.329.59.473A3.993 3.993 0 0110 2.5c1.195 0 2.273.523 3.008 1.352.13-.07.258-.147.382-.229a3.99 3.99 0 00-.821-1.297.75.75 0 111.133-.984 5.49 5.49 0 011.046 1.724.75.75 0 01-.318.96 5.47 5.47 0 01-1.013.504A4 4 0 0114 6.5h.25a.75.75 0 010 1.5H14v.5c0 .058-.002.115-.005.172l1.83 1.83a.75.75 0 01-1.06 1.06l-1.453-1.452A4.002 4.002 0 0110 13.5a4.002 4.002 0 01-3.312-2.89L5.235 12.06a.75.75 0 01-1.06-1.06l1.83-1.83A4.025 4.025 0 016 8.5V8h-.25a.75.75 0 010-1.5H6A4 4 0 016.583 4.76a5.467 5.467 0 01-1.013-.504.75.75 0 01-.318-.96A5.488 5.488 0 016.38.572a.75.75 0 011.045-.177z" clip-rule="evenodd"/>
                    </svg>
                    <svg v-else-if="task.issueType === 'story'" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M5 3a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2V5a2 2 0 00-2-2H5zm0 2h10v7h-2l-1 2H8l-1-2H5V5z"/>
                    </svg>
                    <svg v-else-if="task.issueType === 'epic'" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z" clip-rule="evenodd"/>
                    </svg>
                    <svg v-else class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V7.414A2 2 0 0015.414 6L12 2.586A2 2 0 0010.586 2H6zm5 6a1 1 0 10-2 0v2H7a1 1 0 100 2h2v2a1 1 0 102 0v-2h2a1 1 0 100-2h-2V8z" clip-rule="evenodd"/>
                    </svg>
                  </span>
                  <div class="flex-1 min-w-0">
                    <div class="flex flex-wrap items-center gap-1.5">
                      <span class="text-[10px] sm:text-xs font-mono text-gray-400 font-medium">{{ task.customId || task.id }}</span>
                      <span
                        class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] sm:text-xs font-medium"
                        :class="statusBadgeClass(task.status)"
                      >
                        {{ task.status }}
                      </span>
                      <span
                        v-if="task.priority"
                        class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] sm:text-xs font-medium"
                        :class="priorityBadgeClass(task.priority)"
                      >
                        {{ task.priority }}
                      </span>
                      <span
                        v-if="task.storyPoints"
                        class="inline-flex items-center text-[10px] sm:text-xs font-bold bg-blue-50 text-blue-700 rounded-md px-1.5 py-0.5 border border-blue-100"
                      >
                        {{ task.storyPoints }}<span class="text-blue-400 ml-0.5 font-medium">SP</span>
                      </span>
                    </div>
                    <p class="mt-1.5 text-sm font-medium text-gray-900 line-clamp-2 group-hover:text-blue-900 transition-colors">{{ task.title }}</p>
                    <!-- Alt görevler (parent kartın içinde iç içe) -->
                    <div v-if="taskTree.childrenOf(task.id).length" class="mt-2 border-l-2 border-gray-100 pl-3 space-y-1">
                      <button
                        class="text-[10px] text-gray-400 hover:text-gray-600 font-medium"
                        @click.stop="toggleExpand(task.id)"
                      >
                        {{ isExpanded(task.id) ? '▾' : '▸' }} {{ taskTree.childrenOf(task.id).length }} alt görev
                      </button>
                      <template v-if="isExpanded(task.id)">
                        <div
                          v-for="sub in taskTree.childrenOf(task.id)"
                          :key="sub.id"
                          class="flex items-center gap-2 text-xs text-gray-600 cursor-pointer hover:text-blue-600"
                          @click.stop="openTaskDetail(sub)"
                        >
                          <span class="font-mono text-gray-400">{{ sub.customId }}</span>
                          <span class="truncate flex-1">{{ sub.title }}</span>
                          <span class="px-1.5 py-0.5 rounded-full shrink-0" :class="statusBadgeClass(sub.status)">{{ sub.status }}</span>
                        </div>
                      </template>
                    </div>
                    <div v-if="task.assignee" class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-[10px] sm:text-xs text-gray-500">
                      <span class="flex items-center space-x-1.5">
                        <span class="w-5 h-5 rounded-full bg-gradient-to-br from-blue-400 to-indigo-600 flex items-center justify-center text-white text-[9px] font-bold flex-shrink-0">
                          {{ initials(task.assignee) }}
                        </span>
                        <span class="truncate max-w-[140px]">{{ task.assignee }}</span>
                      </span>
                    </div>
                    <!-- Mobilde sprint seçimi -->
                    <div class="mt-3 sm:hidden">
                      <label class="block text-[10px] uppercase tracking-wide text-gray-400 mb-1">{{ section.type === 'backlog' ? 'Sprint' : 'Move To' }}</label>
                      <select
                        class="w-full bg-white border border-gray-300 rounded-md px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500"
                        :value="task.sprintId || ''"
                        @change="e => handleMobileSprintChange(task, e.target.value)"
                        @click.stop
                      >
                        <option value="">Backlog</option>
                        <option v-for="s in activeSprints" :key="s.id" :value="s.id">{{ s.name }}</option>
                      </select>
                    </div>
                  </div>
                  <div class="opacity-0 group-hover:opacity-100 transition-opacity hidden sm:block flex-shrink-0">
                    <svg class="w-4 h-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"/>
                    </svg>
                  </div>
                </div>
              </div>

              <!-- Boş durum -->
              <div v-if="section.tasks.length === 0" class="text-center py-10 sm:py-12 text-gray-400">
                <svg class="mx-auto h-10 w-10 sm:h-12 sm:w-12 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                <p class="mt-2 text-sm text-gray-500">
                  {{ section.type === 'backlog' ? 'No issues in backlog' : 'No issues planned for this sprint' }}
                </p>
                <p class="text-xs text-gray-400 mt-0.5">
                  {{ section.type === 'backlog' ? 'Create your first issue to get started' : 'Drag issues from the backlog' }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import {
  createSprint as addSprint,
  assignTaskToSprint as addTaskToSprint,
  getSprints,
  getTasks,
  updateSprint,
  updateSprintStatus,
  updateTask as updateTaskService,
  deleteTask as deleteTaskService
} from "../../api/WorkApi.js";
import AddTaskForm from "./AddTaskForm.vue";
import { buildTaskTree } from "../../utils/taskHierarchy.js";

export default {
  name: "Backlog",
  components: {
    AddTaskForm
  },
  props: {
    teamId: String,
    /**
     * Aktif proje context'i — backlog proje bazlı ayrılır. Sprintler takım
     * seviyesinde kaldığı için sprint listesi daralmaz, içindeki görevler filtrelenir.
     * null ise takımın tüm projeleri gösterilir.
     */
    projectId: { type: String, default: null },
    /** Takımın projeleri — sprint satırlarındaki proje kırılım rozeti için. */
    projects: { type: Array, default: () => [] }
  },
  data() {
    return {
      tasks: [],
      sprints: [],
      editingSprint: null,
      showCreateTask: false,
      showCreateSprint: false,
      newTaskTitle: "",
      newTaskDescription: "",
      newSprintName: "",
      newSprintStartDate: "",
      newSprintEndDate: "",
      newSprintGoal: "",
      editingTask: null,
      isMobile: false,
      draggedTask: null,
      dragOverKey: null,
      // İlk yükleme tamamlanana kadar true. Arka plandaki 10s'lik polling
      // (fetchData({silent:true})) bunu tekrar tetiklemez — aksi halde her
      // yenilemede iskelet görünüm titremesine (flicker) yol açardı.
      isLoading: true,
      skeletonBlocks: [
        { key: 'skeleton-backlog', rows: [1, 2, 3] },
        { key: 'skeleton-sprint', rows: [1, 2] },
      ],
      // Aç/kapa durumu görev id'siyle takip edilir (varsayılan açık) —
      // 10s'lik polling'de sıfırlanmaması için fetchData'da dokunulmaz
      expandedIds: {},
    };
  },
  computed: {
    activeSprints() {
      return this.sprints.filter(s => s.status !== 'done');
    },
    taskTree() {
      return buildTaskTree(this.tasks);
    },
    // Subtask'lar parent kartının içinde gösterilir; üst listelerde sadece top-level görevler yer alır
    backlogTasks() {
      return this.taskTree.topLevel.filter(t => !t.sprintId);
    },
    // Backlog ve her aktif sprint tek bir liste olarak modellenir; kart
    // şablonu template içinde tek yerde tanımlanır, iki bölüm arasında
    // (ör. status rozeti renkleri) tutarsızlık oluşmaz.
    sections() {
      return [
        { key: 'backlog', type: 'backlog', sprint: null, tasks: this.backlogTasks },
        ...this.activeSprints.map(sprint => ({
          key: sprint.id,
          type: 'sprint',
          sprint,
          tasks: this.sprintTasks(sprint.id),
        })),
      ];
    },
  },
  methods: {
    sprintTasks(sprintId) {
      return this.taskTree.topLevel.filter(t => t.sprintId === sprintId);
    },

    isExpanded(id) {
      return this.expandedIds[id] !== false;
    },

    toggleExpand(id) {
      this.expandedIds[id] = !this.isExpanded(id);
    },

    async saveSprint() {
      if(!this.newSprintName.trim()) return;
      if (this.editingSprint) {
        // Backend null alanları yoksayar; boş string alanı temizler
        await updateSprint(this.teamId, this.editingSprint.id, {
          name: this.newSprintName.trim(),
          startDate: this.newSprintStartDate,
          endDate: this.newSprintEndDate,
          goal: this.newSprintGoal.trim(),
        });
      } else {
        await addSprint(this.teamId, {
          name: this.newSprintName.trim(),
          startDate: this.newSprintStartDate || null,
          endDate: this.newSprintEndDate || null,
          goal: this.newSprintGoal.trim() || null,
        });
      }
      this.closeSprintForm();
      await this.fetchData({ silent: true });
    },

    openEditSprint(sprint) {
      this.editingSprint = sprint;
      this.newSprintName = sprint.name || "";
      this.newSprintStartDate = sprint.startDate ? sprint.startDate.slice(0, 10) : "";
      this.newSprintEndDate = sprint.endDate ? sprint.endDate.slice(0, 10) : "";
      this.newSprintGoal = sprint.goal || "";
      this.showCreateSprint = true;
      this.$nextTick(() => {
        this.$refs.sprintForm?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      });
    },

    toggleSprintForm() {
      const wasOpen = this.showCreateSprint;
      this.closeSprintForm();
      this.showCreateSprint = !wasOpen;
    },

    closeSprintForm() {
      this.showCreateSprint = false;
      this.editingSprint = null;
      this.newSprintName = "";
      this.newSprintStartDate = "";
      this.newSprintEndDate = "";
      this.newSprintGoal = "";
    },

    onDragStart(task) {
      this.draggedTask = task;
    },

    onDragEnter(key) {
      if (this.isMobile || !this.draggedTask) return;
      this.dragOverKey = key;
    },

    onDragLeave(key) {
      if (this.dragOverKey === key) this.dragOverKey = null;
    },

    async onDrop(sprintId, key) {
      this.dragOverKey = null;
      if(!this.draggedTask) return;
      await addTaskToSprint(this.teamId, this.draggedTask.id, sprintId);
      this.draggedTask = null;
      await this.fetchData({ silent: true });
    },

    async handleMobileSprintChange(task, sprintId) {
      // sprintId boş ise backlog'a gönder
      await addTaskToSprint(this.teamId, task.id, sprintId || null);
      await this.fetchData({ silent: true });
    },

    async startSprint(sprintId) {
      await updateSprintStatus(this.teamId, sprintId, "open");
      await this.fetchData({ silent: true });
    },

    async finishSprint(sprintId) {
      await updateSprintStatus(this.teamId, sprintId, "done");
      await this.fetchData({ silent: true });
    },

    showSprintDetails(sprint) {
      // sprintId'yi query'de taşıyoruz ki Scrum Board açıldığında tıklanan
      // sprint seçili gelsin (aksi halde her zaman ilk aktif sprint açılıyordu)
      this.$router.push({ path: `/workList/${this.teamId}`, query: { view: 'board', sprintId: sprint.id } });
    },

    openTaskDetail(task) {
      this.$router.push({
        name: 'TaskDetail',
        params: {
          taskId: task.customId || task.id
        }
      });
    },

    closeTaskForm() {
      this.showCreateTask = false;
      this.editingTask = null;
    },

    async handleTaskAdded() {
      this.closeTaskForm();
      await this.fetchData({ silent: true });
    },

    editTask(task) {
      this.editingTask = { ...task };
      this.showCreateTask = true;
    },

    async handleUpdateTask(updatedTask) {
      await updateTaskService(this.teamId, updatedTask.id, { ...updatedTask, updatedAt: new Date().toISOString() });
      this.closeTaskForm();
      await this.fetchData({ silent: true });
    },

    async handleDeleteTask(taskId) {
      await deleteTaskService(this.teamId, taskId);
      this.closeTaskForm();
      await this.fetchData({ silent: true });
    },

    handleResize() {
      this.isMobile = window.innerWidth < 640;
    },

    formatSprintDate(d) {
      if (!d) return '';
      return new Date(d).toLocaleDateString('tr-TR');
    },

    sprintCompletionPercent(sprintId) {
      const items = this.sprintTasks(sprintId);
      if (!items.length) return 0;
      const done = items.filter(t => t.status === 'Done').length;
      return Math.round((done / items.length) * 100);
    },

    remainingDays(sprint) {
      if (!sprint?.endDate) return null;
      const end = new Date(sprint.endDate);
      const now = new Date();
      const diff = Math.ceil((end - now) / (1000 * 60 * 60 * 24));
      return Math.max(0, diff);
    },

    statusBadgeClass(status) {
      return {
        'To Do':       'bg-gray-100 text-gray-700',
        'In Progress': 'bg-blue-100 text-blue-700',
        'Done':        'bg-green-100 text-green-700',
        'Cancelled':   'bg-red-100 text-red-600',
      }[status] || 'bg-gray-100 text-gray-700';
    },

    priorityBadgeClass(priority) {
      return {
        Critical: 'bg-red-50 text-red-700 border border-red-200',
        High:     'bg-orange-50 text-orange-700 border border-orange-200',
        Medium:   'bg-amber-50 text-amber-700 border border-amber-200',
        Low:      'bg-emerald-50 text-emerald-700 border border-emerald-200',
      }[priority] || 'bg-gray-50 text-gray-600 border border-gray-200';
    },

    priorityBarClass(priority) {
      return {
        Critical: 'bg-red-500',
        High:     'bg-orange-500',
        Medium:   'bg-amber-400',
        Low:      'bg-emerald-400',
      }[priority] || 'bg-gray-300';
    },

    issueTypeIconClass(type) {
      return {
        bug:   'bg-red-100 text-red-600',
        story: 'bg-green-100 text-green-600',
        epic:  'bg-purple-100 text-purple-600',
        task:  'bg-blue-100 text-blue-600',
      }[type] || 'bg-blue-100 text-blue-600';
    },

    initials(name) {
      if (!name) return '?';
      const base = name.includes('@') ? name.split('@')[0] : name;
      const parts = base.split(/[.\-_\s]/).filter(Boolean);
      if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
      return base.charAt(0).toUpperCase();
    },

    async fetchData({ silent = false } = {}) {
      // silent=true: arka plan yenilemesi (polling / kullanıcı aksiyonu sonrası) —
      // iskelet yükleyiciyi tekrar göstermeyiz, sadece veriyi sessizce tazeleriz.
      if (!silent) this.isLoading = true;
      try {
        // Görevler proje bazlı çekilir; sprintler takım bazlı olduğu için
        // filtresiz kalır (aynı sprint birden fazla projeye hizmet edebilir).
        const [tasks, sprints] = await Promise.all([
          getTasks(this.teamId, false, this.projectId).catch(() => []),
          getSprints(this.teamId).catch(() => [])
        ]);
        this.tasks = tasks;
        this.sprints = sprints;
      } finally {
        this.isLoading = false;
      }
    }
  },
  mounted() {
    this.handleResize();
    window.addEventListener('resize', this.handleResize);
    this.fetchData();
    this._pollInterval = setInterval(() => this.fetchData({ silent: true }), 10000);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
    clearInterval(this._pollInterval);
  },
  watch: {
    // teamId route seviyesinde değişebilir; WorkList aynı component
    // instance'ını farklı takımlar için yeniden kullanıyor (route: /workList/:teamId,
    // props:true — remount olmuyor). Bu watcher olmadan takım değiştirildiğinde
    // önceki takımın backlog verisi ekranda asılı kalıyordu.
    teamId() {
      this.fetchData();
    },
    // Proje değişince backlog o projenin görevlerine daralır.
    projectId() {
      this.fetchData();
    }
  }
};
</script>

<style scoped>
/* line clamp utility fallback if not available */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
