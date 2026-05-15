<template>
  <div class="max-w-3xl mx-auto">
    <div class="bg-white rounded-xl shadow-md border border-gray-200 p-6">
      <h2 class="text-2xl font-bold text-gray-900 mb-6">
        {{ isEditing ? 'Şablonu Düzenle' : 'Yeni Quiz Şablonu' }}
      </h2>

      <!-- Şablon Bilgileri -->
      <div class="space-y-4 mb-8">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Başlık *</label>
          <input v-model="form.title" type="text" placeholder="Örn: Sprint Bilgi Yarışması"
                 class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
          <textarea v-model="form.description" rows="2" placeholder="Kısa bir açıklama..."
                    class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"></textarea>
        </div>
      </div>

      <!-- Sorular -->
      <div class="mb-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-gray-800">Sorular</h3>
          <button @click="addQuestion"
                  class="px-3 py-1.5 bg-indigo-100 text-indigo-700 rounded-lg hover:bg-indigo-200 transition-colors text-sm font-medium">
            + Soru Ekle
          </button>
        </div>

        <div v-for="(q, qi) in form.questions" :key="qi"
             class="mb-6 p-5 bg-gray-50 rounded-xl border border-gray-200">
          <div class="flex items-center justify-between mb-3">
            <span class="text-sm font-bold text-indigo-600">Soru {{ qi + 1 }}</span>
            <button v-if="form.questions.length > 1" @click="removeQuestion(qi)"
                    class="text-red-400 hover:text-red-600 text-sm">✕ Sil</button>
          </div>

          <!-- Soru Metni -->
          <div class="mb-3">
            <input v-model="q.questionText" type="text" placeholder="Soruyu yazın..."
                   class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" />
          </div>

          <!-- Süre Ayarı -->
          <div class="mb-3 flex items-center gap-3">
            <label class="text-sm text-gray-600 whitespace-nowrap">⏱ Süre:</label>
            <input v-model.number="q.timeLimitSeconds" type="number" min="5" max="120"
                   class="w-20 px-3 py-1.5 border border-gray-300 rounded-lg text-center focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" />
            <span class="text-sm text-gray-500">saniye</span>
          </div>

          <!-- Seçenekler -->
          <div class="space-y-2 mb-3">
            <div v-for="(opt, oi) in q.options" :key="oi" class="flex items-center gap-2">
              <button @click="q.correctOptionIndex = oi"
                      :class="[
                        'w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-all border-2',
                        q.correctOptionIndex === oi
                          ? 'bg-green-500 text-white border-green-500'
                          : 'bg-white text-gray-400 border-gray-300 hover:border-green-400'
                      ]">
                {{ ['A','B','C','D','E','F'][oi] }}
              </button>
              <input v-model="q.options[oi]" type="text" :placeholder="'Seçenek ' + ['A','B','C','D','E','F'][oi]"
                     class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" />
              <button v-if="q.options.length > 2" @click="removeOption(qi, oi)"
                      class="text-red-400 hover:text-red-600 text-sm px-2">✕</button>
            </div>
          </div>
          <button v-if="q.options.length < 6" @click="addOption(qi)"
                  class="text-sm text-indigo-600 hover:text-indigo-800 font-medium">
            + Seçenek Ekle
          </button>
          <p class="text-xs text-gray-400 mt-2">💡 Yeşil daire = doğru cevap. Tıklayarak değiştirin.</p>
        </div>
      </div>

      <!-- Butonlar -->
      <div class="flex justify-end gap-3">
        <button @click="$emit('cancel')"
                class="px-6 py-2.5 bg-gray-100 text-gray-700 rounded-xl hover:bg-gray-200 transition-colors font-medium">
          İptal
        </button>
        <button @click="save" :disabled="saving"
                class="px-6 py-2.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-medium disabled:opacity-50">
          {{ saving ? 'Kaydediliyor...' : (isEditing ? 'Güncelle' : 'Kaydet') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { createTemplate, updateTemplate } from '../../api/QuizApi.js'
import { createToast } from 'mosha-vue-toastify'

export default {
  name: 'QuizTemplateForm',
  props: {
    teamId: String,
    template: { type: Object, default: null }
  },
  emits: ['saved', 'cancel'],
  data() {
    return {
      saving: false,
      form: this.initForm()
    }
  },
  computed: {
    isEditing() {
      return !!this.template
    }
  },
  methods: {
    initForm() {
      if (this.template) {
        return {
          title: this.template.title,
          description: this.template.description || '',
          questions: this.template.questions.map(q => ({
            questionText: q.questionText,
            options: [...q.options],
            correctOptionIndex: q.correctOptionIndex,
            timeLimitSeconds: q.timeLimitSeconds
          }))
        }
      }
      return {
        title: '',
        description: '',
        questions: [this.newQuestion()]
      }
    },
    newQuestion() {
      return {
        questionText: '',
        options: ['', '', '', ''],
        correctOptionIndex: 0,
        timeLimitSeconds: 20
      }
    },
    addQuestion() {
      this.form.questions.push(this.newQuestion())
    },
    removeQuestion(index) {
      this.form.questions.splice(index, 1)
    },
    addOption(qi) {
      this.form.questions[qi].options.push('')
    },
    removeOption(qi, oi) {
      const q = this.form.questions[qi]
      q.options.splice(oi, 1)
      if (q.correctOptionIndex >= q.options.length) {
        q.correctOptionIndex = 0
      }
    },
    validate() {
      if (!this.form.title.trim()) {
        createToast('Başlık zorunludur', { type: 'warning' })
        return false
      }
      for (let i = 0; i < this.form.questions.length; i++) {
        const q = this.form.questions[i]
        if (!q.questionText.trim()) {
          createToast(`Soru ${i + 1} metni boş olamaz`, { type: 'warning' })
          return false
        }
        if (q.options.some(o => !o.trim())) {
          createToast(`Soru ${i + 1} seçenekleri boş olamaz`, { type: 'warning' })
          return false
        }
        if (q.timeLimitSeconds < 5 || q.timeLimitSeconds > 120) {
          createToast(`Soru ${i + 1} süre 5-120 saniye arası olmalı`, { type: 'warning' })
          return false
        }
      }
      return true
    },
    async save() {
      if (!this.validate()) return
      this.saving = true
      try {
        if (this.isEditing) {
          await updateTemplate(this.teamId, this.template.id, this.form)
          createToast('Şablon güncellendi', { type: 'success' })
        } else {
          await createTemplate(this.teamId, this.form)
          createToast('Şablon oluşturuldu', { type: 'success' })
        }
        this.$emit('saved')
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.saving = false
    }
  },
  watch: {
    template() {
      this.form = this.initForm()
    }
  }
}
</script>

