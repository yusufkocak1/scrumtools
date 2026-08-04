<template>
  <div class="flex flex-row w-full min-h-screen bg-gray-50">
    <!-- Sol menü -->
    <aside class="w-60 shrink-0 bg-white border-r border-gray-200 flex flex-col">
      <div class="px-4 py-4 border-b border-gray-100">
        <div class="flex items-center gap-2">
          <span class="w-7 h-7 rounded-lg bg-purple-100 text-purple-600 flex items-center justify-center text-sm">◧</span>
          <div class="min-w-0">
            <p class="text-sm font-semibold text-gray-800 truncate">{{ filter?.name || 'Zengin filtre' }}</p>
            <p class="text-[11px] text-gray-400">Gelişmiş analiz filtresi</p>
          </div>
        </div>
      </div>

      <router-link
        to="/rich-filters"
        class="mx-3 mt-3 flex items-center gap-2 text-sm text-gray-600 hover:text-purple-700 px-2 py-1.5 rounded-lg hover:bg-purple-50 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/>
        </svg>
        Zengin filtre listesi
      </router-link>

      <p class="px-5 mt-5 mb-1.5 text-[10px] font-semibold tracking-wider text-gray-400 uppercase">Ayarlar</p>
      <nav class="px-3 space-y-0.5">
        <button
          v-for="item in sections"
          :key="item.key"
          class="w-full flex items-center justify-between gap-2 px-2 py-1.5 rounded-lg text-sm transition-colors"
          :class="sectionClass(item)"
          :disabled="!item.ready"
          @click="item.ready && (section = item.key)"
          :title="item.ready ? '' : 'Sonraki fazda gelecek'"
        >
          <span class="flex items-center gap-2">
            <span class="w-4 text-center text-xs">{{ item.icon }}</span>
            {{ item.label }}
          </span>
          <span v-if="item.key === 'smart' && smartFilters.length" class="text-[11px] text-gray-400">
            {{ smartFilters.length }}
          </span>
        </button>
      </nav>

      <p class="px-5 mt-6 mb-1.5 text-[10px] font-semibold tracking-wider text-gray-400 uppercase">İşlemler</p>
      <div class="px-3 space-y-0.5 pb-6">
        <button
          class="w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm text-gray-600 hover:bg-gray-50 transition-colors"
          @click="copyFilter"
        >
          <span class="w-4 text-center text-xs">⧉</span> Zengin filtreyi kopyala
        </button>
        <button
          v-if="filter?.owned"
          class="w-full flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm text-red-600 hover:bg-red-50 transition-colors"
          @click="removeFilter"
        >
          <span class="w-4 text-center text-xs">🗑</span> Sil
        </button>
      </div>
    </aside>

    <!-- İçerik -->
    <div class="flex-1 min-w-0 flex flex-col">
      <div v-if="loading" class="flex-1 flex items-center justify-center text-sm text-gray-400">
        Yükleniyor…
      </div>

      <div v-else-if="!filter" class="flex-1 flex items-center justify-center text-sm text-gray-400">
        Zengin filtre bulunamadı.
      </div>

      <div v-else class="flex-1 overflow-y-auto">
        <!-- ─── Genel ─────────────────────────────────────────────────── -->
        <section v-if="section === 'general'" class="max-w-3xl px-8 py-6">
          <h1 class="text-xl font-semibold text-gray-900">Genel</h1>
          <p class="text-sm text-gray-500 mt-1">
            Temel sorgu, bu zengin filtrenin çalıştığı görev kümesini belirler. Akıllı filtreler
            ve grafikler her zaman bu kümenin içinde çalışır.
          </p>

          <div class="mt-6 space-y-5 bg-white rounded-xl border border-gray-100 p-5">
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Ad</label>
              <input
                v-model="general.name"
                :disabled="!filter.owned"
                type="text"
                class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm disabled:bg-gray-50 focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
              />
              <p class="mt-1 text-[11px] text-gray-400">
                Sorgularda kullanılır: <code class="font-mono">{{ smartFieldName }} = "Test"</code>
              </p>
            </div>

            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Açıklama</label>
              <input
                v-model="general.description"
                :disabled="!filter.owned"
                type="text"
                class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm disabled:bg-gray-50 focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
              />
            </div>

            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Temel sorgu</label>
              <select
                v-model="general.baseFilterId"
                :disabled="!filter.owned"
                class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm disabled:bg-gray-50 focus:outline-none focus:border-purple-400"
              >
                <option :value="null">Kayıtlı filtre kullanma — sorguyu burada yaz</option>
                <option v-for="sf in savedFilters" :key="sf.id" :value="sf.id">{{ sf.name }}</option>
              </select>
              <p class="mt-1.5 text-[11px] text-gray-400">
                Kayıtlı filtre seçmek tercih edilir: filtre güncellenince buna bağlı her şey
                kendiliğinden güncellenir.
              </p>

              <div v-if="!general.baseFilterId" class="mt-3">
                <StqlInput
                  v-model="general.baseQuery"
                  :team-id="teamId"
                  :project-id="projectId"
                  placeholder="project = QUICKRES AND sprint = currentSprint()"
                />
              </div>
            </div>

            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Görünürlük</label>
              <select
                v-model="general.visibility"
                :disabled="!filter.owned"
                class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm disabled:bg-gray-50 focus:outline-none focus:border-purple-400"
              >
                <option value="PRIVATE">Özel — yalnız ben</option>
                <option value="TEAM">Takım — takımın üyeleri</option>
                <option value="PROJECT">Proje — projedeki tüm takımlar</option>
              </select>
            </div>

            <div v-if="filter.owned" class="flex items-center justify-end gap-2 pt-1">
              <span v-if="savedMessage" class="text-xs text-green-600">{{ savedMessage }}</span>
              <button
                class="text-sm bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white px-4 py-1.5 rounded-lg transition-colors"
                :disabled="savingGeneral"
                @click="saveGeneral"
              >
                {{ savingGeneral ? 'Kaydediliyor…' : 'Kaydet' }}
              </button>
            </div>
          </div>
        </section>

        <!-- ─── Akıllı filtreler ──────────────────────────────────────── -->
        <section v-else-if="section === 'smart'" class="px-8 py-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h1 class="text-xl font-semibold text-gray-900">Akıllı filtreler</h1>
              <p class="text-sm text-gray-500 mt-1 max-w-3xl">
                Her akıllı filtre, sorgusuna uyan görevleri bir renk ve etiketle işaretler.
                <strong>Sıra önemlidir:</strong> bir görev, kendisine uyan ilk kuralın kategorisine düşer —
                satırları sürükleyerek önceliği değiştirebilirsiniz.
              </p>
            </div>
            <button
              v-if="filter.owned"
              class="shrink-0 text-sm bg-purple-600 hover:bg-purple-700 text-white px-3 py-1.5 rounded-lg transition-colors"
              @click="openCreate"
            >
              Akıllı filtre oluştur
            </button>
          </div>

          <!-- Liste -->
          <div class="mt-6 bg-white rounded-xl border border-gray-100 overflow-hidden">
            <div class="grid grid-cols-[2rem_1fr_2fr_5rem] gap-3 px-4 py-2.5 border-b border-gray-100 bg-gray-50/60 text-[11px] font-semibold text-gray-500 uppercase tracking-wide">
              <span></span><span>Ad</span><span>Sorgu</span><span class="text-right">İşlem</span>
            </div>

            <div
              v-for="(element, index) in smartFilters"
              :key="element.id"
              class="grid grid-cols-[2rem_1fr_2fr_5rem] gap-3 px-4 py-3 border-b border-gray-50 last:border-0 items-center group hover:bg-gray-50/50 transition-colors"
              :class="{ 'opacity-40': dragIndex === index }"
              :draggable="filter.owned"
              @dragstart="dragIndex = index"
              @dragover.prevent
              @drop="dropOn(index)"
              @dragend="dragIndex = null"
            >
              <div class="flex items-center gap-2">
                <span v-if="filter.owned" class="cursor-grab text-gray-300 group-hover:text-gray-400 select-none">⠿</span>
              </div>

              <div class="flex items-center gap-2.5 min-w-0">
                <span class="w-5 h-5 rounded shrink-0" :style="{ backgroundColor: element.color || '#94A3B8' }"></span>
                <span class="text-sm text-gray-800 truncate">{{ element.name }}</span>
              </div>

              <code class="text-xs font-mono text-gray-600 truncate" :title="element.query">{{ element.query }}</code>

              <div class="flex items-center justify-end gap-1">
                <button
                  v-if="filter.owned"
                  class="p-1.5 rounded text-gray-400 hover:text-purple-600 hover:bg-purple-50 transition-colors"
                  title="Düzenle"
                  @click="openEdit(element)"
                >✎</button>
                <button
                  v-if="filter.owned"
                  class="p-1.5 rounded text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                  title="Sil"
                  @click="removeElement(element)"
                >🗑</button>
              </div>
            </div>

            <div v-if="!smartFilters.length" class="px-4 py-12 text-center">
              <p class="text-sm text-gray-500">Henüz akıllı filtre yok.</p>
              <p class="text-xs text-gray-400 mt-1 max-w-md mx-auto">
                Örnek: "Test" adında, <code class="font-mono">status in ("Ready to Test", Test)</code>
                sorgulu bir kural ekleyin — sonra grafiklerde bu kategoriyi görürsünüz.
              </p>
            </div>
          </div>

          <!-- Dağılım -->
          <div v-if="smartFilters.length" class="mt-6 bg-white rounded-xl border border-gray-100 p-5">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-sm font-semibold text-gray-700">Dağılım</h2>
                <p class="text-xs text-gray-400 mt-0.5">
                  Temel sorgudaki görevlerin bu kurallara göre sınıflandırması — ilk eşleşen kazanır.
                </p>
              </div>
              <button
                class="text-xs text-purple-600 hover:text-purple-700"
                :disabled="loadingBreakdown"
                @click="loadBreakdown"
              >
                {{ loadingBreakdown ? 'Hesaplanıyor…' : 'Yenile' }}
              </button>
            </div>

            <div v-if="breakdown.length" class="mt-4 space-y-2">
              <div v-for="bucket in breakdown" :key="bucket.key || 'unclassified'" class="flex items-center gap-3">
                <span class="w-3 h-3 rounded shrink-0" :style="{ backgroundColor: bucket.color || '#CBD5E1' }"></span>
                <span class="text-xs text-gray-600 w-40 truncate">{{ bucket.label }}</span>
                <div class="flex-1 h-2 rounded-full bg-gray-100 overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all"
                    :style="{ width: barWidth(bucket), backgroundColor: bucket.color || '#CBD5E1' }"
                  ></div>
                </div>
                <span class="text-xs font-medium text-gray-700 w-10 text-right">{{ bucket.value }}</span>
              </div>

              <p v-if="unclassifiedCount > 0" class="text-[11px] text-amber-600 pt-2">
                {{ unclassifiedCount }} görev hiçbir akıllı filtreye uymuyor — bir kural eksik olabilir.
              </p>
            </div>

            <p v-else-if="!loadingBreakdown" class="mt-4 text-xs text-gray-400">
              Dağılımı görmek için "Yenile"ye basın.
            </p>
          </div>

          <!-- Denetim: sıranın gerçekte ne yaptığı -->
          <div v-if="smartFilters.length" class="mt-6 bg-white rounded-xl border border-gray-100 p-5">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-sm font-semibold text-gray-700">Sınıflandırma denetimi</h2>
                <p class="text-xs text-gray-400 mt-0.5 max-w-2xl">
                  Bir kural kendi başına 60 göreve uyuyor olabilir ama önce gelen kurallar
                  yüzünden yalnız 12'sini alıyor olabilir. Ekranda görünen tek sayı 12'dir;
                  bu tablo aradaki farkı gösterir.
                </p>
              </div>
              <button
                class="text-xs text-purple-600 hover:text-purple-700 shrink-0"
                :disabled="loadingAudit"
                @click="loadAudit"
              >
                {{ loadingAudit ? 'Denetleniyor…' : 'Denetle' }}
              </button>
            </div>

            <div v-if="audit" class="mt-4 space-y-3">
              <div
                v-for="row in audit.clauses"
                :key="row.id"
                class="rounded-lg border px-3 py-2.5"
                :class="row.dead ? 'border-amber-200 bg-amber-50/50' : 'border-gray-100'"
              >
                <div class="flex items-center gap-2">
                  <span class="w-3 h-3 rounded shrink-0" :style="{ backgroundColor: row.color || '#94A3B8' }"></span>
                  <span class="text-xs font-medium text-gray-800 truncate">{{ row.name }}</span>
                  <span class="ml-auto text-xs text-gray-500 tabular-nums shrink-0">
                    <strong class="text-gray-800">{{ row.owned }}</strong> / {{ row.matched }}
                  </span>
                </div>

                <p v-if="row.dead && row.matched > 0" class="mt-1 text-[11px] text-amber-700">
                  Bu kural hiçbir görev sahiplenmiyor: uyduğu {{ row.matched }} görevin tamamını
                  önce gelen kurallar alıyor. Sırayı değiştirin ya da kuralı daraltın.
                </p>
                <p v-else-if="row.dead" class="mt-1 text-[11px] text-amber-700">
                  Bu kural hiçbir göreve uymuyor — sorgusu boş bir küme döndürüyor.
                </p>
                <p v-else-if="row.shadowed > 0" class="mt-1 text-[11px] text-gray-500">
                  {{ row.shadowed }} görev önce gelen kurallara gidiyor:
                  <span v-for="(taker, index) in row.takenBy" :key="taker.id">
                    <span class="text-gray-700">{{ taker.name }}</span> ({{ taker.count }}){{ index < row.takenBy.length - 1 ? ', ' : '' }}
                  </span>
                </p>
              </div>

              <!-- Sınıflandırılmayanlar: sayı tek başına eyleme dönüşmez, örnek gerekir -->
              <div
                v-if="audit.unclassified?.count > 0"
                class="rounded-lg border border-gray-100 px-3 py-2.5"
              >
                <div class="flex items-center gap-2">
                  <span class="w-3 h-3 rounded shrink-0 bg-gray-300"></span>
                  <span class="text-xs font-medium text-gray-800">Sınıflandırılmamış</span>
                  <span class="ml-auto text-xs text-gray-500 tabular-nums">{{ audit.unclassified.count }}</span>
                </div>

                <ul class="mt-2 space-y-0.5">
                  <li v-for="task in audit.unclassified.samples" :key="task.id">
                    <router-link
                      :to="`/task/${task.customId || task.id}`"
                      class="flex items-center gap-2 text-[11px] text-gray-500 hover:text-purple-700"
                    >
                      <span class="font-mono text-gray-400 w-16 shrink-0 truncate">{{ task.customId }}</span>
                      <span class="truncate">{{ task.title }}</span>
                    </router-link>
                  </li>
                </ul>

                <router-link
                  v-if="audit.unclassified.stql"
                  :to="{ path: `/workList/${teamId}`, query: { q: audit.unclassified.stql } }"
                  class="mt-2 inline-block text-[11px] text-purple-600 hover:text-purple-700"
                >
                  Tümünü listede aç ({{ audit.unclassified.count }})
                </router-link>
              </div>

              <p v-else class="text-[11px] text-green-600">
                Temel sorgudaki {{ audit.total }} görevin tamamı sınıflandırıldı.
              </p>
            </div>

            <p v-else-if="!loadingAudit" class="mt-4 text-xs text-gray-400">
              Kural sırasının gerçekte ne yaptığını görmek için "Denetle"ye basın.
            </p>
          </div>
        </section>

        <!-- ─── Sabit / dinamik filtreler, görünümler, zaman serileri ──── -->
        <section v-else-if="['static', 'dynamic', 'views', 'series', 'alerts'].includes(section)" class="px-8 py-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h1 class="text-xl font-semibold text-gray-900">{{ currentSection?.label }}</h1>
              <p class="text-sm text-gray-500 mt-1 max-w-3xl">{{ SECTION_HINTS[section] }}</p>
            </div>
            <button
              v-if="filter.owned && section !== 'views'"
              class="shrink-0 text-sm bg-purple-600 hover:bg-purple-700 text-white px-3 py-1.5 rounded-lg transition-colors"
              @click="openCreate"
            >
              {{ CREATE_LABEL[section] }}
            </button>
          </div>

          <div class="mt-6 bg-white rounded-xl border border-gray-100 overflow-hidden">
            <div
              v-for="element in sectionElements"
              :key="element.id"
              class="flex items-center gap-3 px-4 py-3 border-b border-gray-50 last:border-0 hover:bg-gray-50/50 transition-colors"
            >
              <div class="min-w-0 flex-1">
                <p class="text-sm text-gray-800 truncate">{{ element.name }}</p>
                <p class="text-[11px] text-gray-400 truncate">{{ describeElement(element) }}</p>
              </div>

              <!-- Uyarı kuralında anlık hesap: bildirim tetiklemeden sayıyı gör -->
              <span
                v-if="section === 'alerts' && alertPreview[element.id]"
                class="shrink-0 text-[11px] px-2 py-1 rounded"
                :class="alertPreview[element.id].tone"
              >
                {{ alertPreview[element.id].text }}
              </span>
              <button
                v-if="section === 'alerts'"
                class="shrink-0 text-[11px] px-2 py-1 rounded border border-gray-200 text-gray-500 hover:border-purple-300 hover:text-purple-700 transition-colors disabled:opacity-50"
                :disabled="previewing === element.id"
                @click="runAlertPreview(element)"
              >
                {{ previewing === element.id ? 'Hesaplanıyor…' : 'Şimdi hesapla' }}
              </button>

              <!-- Serilerde geçmiş kurgusu: son 180 gün görev tarihçesinden -->
              <button
                v-if="section === 'series' && filter.owned"
                class="shrink-0 text-[11px] px-2 py-1 rounded border border-gray-200 text-gray-500 hover:border-purple-300 hover:text-purple-700 transition-colors disabled:opacity-50"
                :disabled="backfilling === element.id"
                @click="runBackfill(element)"
              >
                {{ backfilling === element.id ? 'Kurgulanıyor…' : 'Geçmişi kur' }}
              </button>

              <!-- Görünümlerde varsayılan işareti: pano ilk açılışta buna düşer -->
              <button
                v-if="section === 'views' && filter.owned"
                class="shrink-0 text-[11px] px-2 py-1 rounded border transition-colors"
                :class="element.config?.default
                  ? 'border-purple-300 bg-purple-50 text-purple-700'
                  : 'border-gray-200 text-gray-500 hover:border-gray-300'"
                @click="toggleDefaultView(element)"
              >
                Varsayılan
              </button>

              <button
                v-if="filter.owned && section !== 'views'"
                class="shrink-0 p-1.5 rounded text-gray-400 hover:text-purple-600 hover:bg-purple-50 transition-colors"
                title="Düzenle"
                @click="openEdit(element)"
              >✎</button>

              <button
                v-if="filter.owned"
                class="shrink-0 p-1.5 rounded text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                title="Sil"
                @click="removeElement(element)"
              >🗑</button>
            </div>

            <div v-if="!sectionElements.length" class="px-4 py-12 text-center">
              <p class="text-sm text-gray-500">{{ SECTION_EMPTY[section] }}</p>
            </div>
          </div>

          <!-- Kurgu sonucu: "yapılamadı" da bir sonuçtur, sebebiyle söylenir -->
          <div
            v-if="section === 'series' && backfillResult"
            class="mt-4 rounded-xl border px-4 py-3 text-xs"
            :class="backfillResult.status === 'ok'
              ? 'border-green-100 bg-green-50 text-green-700'
              : 'border-amber-100 bg-amber-50 text-amber-700'"
          >
            <p class="font-medium">{{ backfillResult.title }}</p>
            <p v-if="backfillResult.detail" class="mt-0.5 opacity-90">{{ backfillResult.detail }}</p>
          </div>
        </section>

        <!-- ─── Henüz gelmeyen bölümler ───────────────────────────────── -->
        <section v-else class="max-w-3xl px-8 py-6">
          <h1 class="text-xl font-semibold text-gray-900">{{ currentSection?.label }}</h1>
          <p class="text-sm text-gray-500 mt-2">Bu bölüm sonraki fazda geliyor.</p>
        </section>
      </div>
    </div>

    <SmartFilterModal
      v-if="modalOpen && section === 'smart'"
      :team-id="teamId"
      :project-id="projectId"
      :rich-filter-name="filter?.name"
      :element="editing"
      :saving="savingElement"
      :error="elementError"
      @close="closeModal"
      @save="saveElement"
    />

    <StaticFilterModal
      v-if="modalOpen && section === 'static'"
      :team-id="teamId"
      :project-id="projectId"
      :element="editing"
      :saving="savingElement"
      :error="elementError"
      @close="closeModal"
      @save="saveElement"
    />

    <DynamicFilterModal
      v-if="modalOpen && section === 'dynamic'"
      :team-id="teamId"
      :element="editing"
      :saving="savingElement"
      :error="elementError"
      @close="closeModal"
      @save="saveElement"
    />

    <TimeSeriesModal
      v-if="modalOpen && section === 'series'"
      :smart-filters="smartFilters"
      :element="editing"
      :saving="savingElement"
      :error="elementError"
      @close="closeModal"
      @save="saveElement"
    />

    <RatioAlertModal
      v-if="modalOpen && section === 'alerts'"
      :smart-filters="smartFilters"
      :element="editing"
      :saving="savingElement"
      :error="elementError"
      @close="closeModal"
      @save="saveElement"
    />
  </div>
</template>

<script setup>
/**
 * Zengin filtre editörü — Jira'daki rich filter yönetim ekranının karşılığı.
 *
 * Bölümler: Genel, akıllı filtreler, sabit/dinamik filtreler, görünümler ve
 * zaman serileri.
 *
 * Kuyruklar ve oranlar burada yok: ikisi de zengin filtrenin tanımına değil,
 * panodaki widget'ın yapılandırmasına ait çıktı (bkz. RICH_FILTER_PLAN.md — K21).
 * Aynı akıllı filtreleri iki farklı panoda farklı kuyruk düzeniyle göstermek
 * isteyen kullanıcı, tanımı değiştirmek zorunda kalmıyor.
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StqlInput from '../components/work/StqlInput.vue'
import SmartFilterModal from '../components/richfilter/SmartFilterModal.vue'
import StaticFilterModal from '../components/richfilter/StaticFilterModal.vue'
import DynamicFilterModal from '../components/richfilter/DynamicFilterModal.vue'
import TimeSeriesModal from '../components/richfilter/TimeSeriesModal.vue'
import RatioAlertModal from '../components/richfilter/RatioAlertModal.vue'
import {
  getRichFilter, updateRichFilter, deleteRichFilter, duplicateRichFilter,
  addRichFilterElement, updateRichFilterElement, deleteRichFilterElement,
  reorderRichFilterElements, backfillRichFilterSeries, auditRichFilter,
  previewRichFilterAlert, smartField,
} from '../api/RichFilterApi.js'
import { getSavedFilters } from '../api/SavedFilterApi.js'
import { aggregateQuery } from '../api/QueryApi.js'
import { useTeamContext } from '../composables/useTeamContext.js'
import { useProjectContext } from '../composables/useProjectContext.js'

const props = defineProps({
  /** Router'dan gelir (props: true); doğrudan gezinmede route parametresine düşer. */
  id: { type: String, default: '' },
})

const route = useRoute()
const router = useRouter()
const { activeTeamId, loadTeams } = useTeamContext()
const { projectId: contextProjectId } = useProjectContext(() => activeTeamId.value)

const richFilterId = computed(() => props.id || route.params.id)
const teamId = computed(() => filter.value?.teamId || activeTeamId.value)
// Zengin filtrenin kendi projesi varsa kapsam odur; yoksa çalışma alanındaki aktif proje.
const projectId = computed(() => filter.value?.projectId || contextProjectId?.value || null)

const filter = ref(null)
const savedFilters = ref([])
const loading = ref(true)
const section = ref('general')

const sections = [
  { key: 'general', label: 'Genel', icon: '⚙', ready: true },
  { key: 'smart', label: 'Akıllı filtreler', icon: '◧', ready: true },
  { key: 'static', label: 'Sabit filtreler', icon: '▤', ready: true },
  { key: 'dynamic', label: 'Dinamik filtreler', icon: '☰', ready: true },
  { key: 'views', label: 'Görünümler', icon: '👁', ready: true },
  { key: 'series', label: 'Zaman serileri', icon: '📈', ready: true },
  { key: 'alerts', label: 'Uyarı kuralları', icon: '🔔', ready: true },
]

/** Bölüm başına öğe türü — liste ve modal seçimi buradan sürülür. */
const SECTION_KIND = {
  smart: 'SMART_FILTER',
  static: 'STATIC_FILTER',
  dynamic: 'DYNAMIC_FILTER',
  views: 'VIEW',
  series: 'TIME_SERIES',
  alerts: 'RATIO',
}

const SECTION_HINTS = {
  static: 'Yazarın tanımladığı seçeneklerden oluşan bir açılır kontrol. Her seçenek '
    + 'kendi sorgusunu taşır ve seçildiğinde temel sorguya eklenir.',
  dynamic: 'Seçenekleri veriden gelen kontrol: listede yalnız o an sonuçta bulunan '
    + 'değerler, görev sayılarıyla birlikte görünür.',
  views: 'Kayıtlı seçim kombinasyonları. Görünümler dashboard\'daki kontrolcüden '
    + '"Görünüm kaydet" ile oluşturulur; burada silinir ve varsayılan seçilir.',
  series: 'Her gece bir akıllı filtrenin sayısı ölçülüp saklanır; pano bu ölçümlerin '
    + 'seyrini çizer. Geçmiş, görev tarihçesinden geriye dönük kurgulanabilir — '
    + 'grafiğin dolması için haftalarca beklemeye gerek yok.',
  alerts: 'Oran hedefin dışına çıktığında bildirim gönderir. Her sabah 09:00\'da '
    + 'değerlendirilir ve yalnız durum değiştiğinde konuşur — aynı uyarı her gün '
    + 'tekrarlanmaz. Bildirim zengin filtreyi oluşturan kişiye gider.',
}

const SECTION_EMPTY = {
  static: 'Henüz sabit filtre yok.',
  dynamic: 'Henüz dinamik filtre yok.',
  views: 'Henüz görünüm yok — panodaki kontrolcüden bir seçim yapıp "Görünüm kaydet" deyin.',
  series: 'Henüz zaman serisi yok. Bir akıllı filtre seçip seri açın; ölçüm bu geceden başlar.',
  alerts: 'Henüz uyarı kuralı yok.',
}

const CREATE_LABEL = {
  static: 'Sabit filtre oluştur',
  dynamic: 'Dinamik filtre oluştur',
  series: 'Zaman serisi oluştur',
  alerts: 'Uyarı kuralı oluştur',
}

const currentSection = computed(() => sections.find(s => s.key === section.value))

function sectionClass(item) {
  if (!item.ready) return 'text-gray-300 cursor-not-allowed'
  return section.value === item.key
    ? 'bg-purple-50 text-purple-700 font-medium'
    : 'text-gray-600 hover:bg-gray-50'
}

// ─── Genel ────────────────────────────────────────────────────────────────

const general = ref({ name: '', description: '', baseFilterId: null, baseQuery: '', visibility: 'PRIVATE' })
const savingGeneral = ref(false)
const savedMessage = ref('')

const smartFieldName = computed(() => smartField(filter.value?.name || 'ad'))

async function saveGeneral() {
  savingGeneral.value = true
  savedMessage.value = ''
  try {
    filter.value = await updateRichFilter(teamId.value, richFilterId.value, {
      name: general.value.name,
      description: general.value.description,
      baseFilterId: general.value.baseFilterId,
      baseQuery: general.value.baseFilterId ? null : general.value.baseQuery,
      visibility: general.value.visibility,
      projectId: general.value.visibility === 'PROJECT' ? projectId.value : null,
    })
    savedMessage.value = 'Kaydedildi'
    setTimeout(() => (savedMessage.value = ''), 2500)
  } finally {
    savingGeneral.value = false
  }
}

// ─── Akıllı filtreler ─────────────────────────────────────────────────────

const smartFilters = computed(() =>
  (filter.value?.elements || []).filter(e => e.kind === 'SMART_FILTER')
)

/** Açık bölümün öğeleri — sabit/dinamik/görünüm listeleri aynı markup'ı paylaşır. */
const sectionElements = computed(() => {
  const kind = SECTION_KIND[section.value]
  return kind ? (filter.value?.elements || []).filter(e => e.kind === kind) : []
})

/** Liste satırının ikinci satırı: türe göre en anlamlı özet. */
function describeElement(element) {
  switch (element.kind) {
    case 'STATIC_FILTER': {
      const options = element.config?.options ?? []
      return options.length
        ? options.map(o => o.label).join(' · ')
        : 'Seçenek tanımlanmamış'
    }
    case 'DYNAMIC_FILTER':
      return `Alan: ${element.config?.field || '—'}`
    case 'TIME_SERIES': {
      const source = smartFilters.value.find(s => s.id === element.config?.smartFilterId)
      return source ? `Kaynak: ${source.name} · günlük sayım` : 'Kaynak: tüm sonuçlar · günlük sayım'
    }
    case 'RATIO': {
      const numerator = smartFilters.value.find(s => s.id === element.config?.numeratorId)
      const denominator = smartFilters.value.find(s => s.id === element.config?.denominatorId)
      const target = Math.round((element.config?.target ?? 0) * 100)
      const direction = element.config?.direction === 'lower_better' ? 'en fazla' : 'en az'
      return `${numerator?.name || '—'} / ${denominator?.name || 'tüm sonuçlar'} · hedef ${direction} %${target}`
    }
    case 'VIEW': {
      const selection = element.config?.selection ?? {}
      const parts = []
      if (selection.smart?.length) parts.push(`${selection.smart.length} akıllı filtre`)
      if (selection.text) parts.push(`arama: "${selection.text}"`)
      if (Object.keys(selection.static ?? {}).length) parts.push('sabit seçim')
      if (Object.keys(selection.dynamic ?? {}).length) parts.push('dinamik seçim')
      return parts.length ? parts.join(' · ') : 'Boş seçim'
    }
    default:
      return element.query || ''
  }
}

/**
 * Varsayılan görünüm tektir: ikincisi işaretlenince öncekinin işareti kalkar.
 * İki varsayılan olsaydı panonun hangisiyle açılacağı sıraya kalırdı.
 */
async function toggleDefaultView(element) {
  const makeDefault = !element.config?.default
  savingElement.value = true
  try {
    if (makeDefault) {
      for (const other of sectionElements.value) {
        if (other.id !== element.id && other.config?.default) {
          await updateRichFilterElement(teamId.value, richFilterId.value, other.id, {
            kind: 'VIEW',
            name: other.name,
            config: { ...(other.config ?? {}), default: false },
          })
        }
      }
    }
    await updateRichFilterElement(teamId.value, richFilterId.value, element.id, {
      kind: 'VIEW',
      name: element.name,
      config: { ...(element.config ?? {}), default: makeDefault },
    })
    await reload()
  } finally {
    savingElement.value = false
  }
}

const modalOpen = ref(false)
const editing = ref(null)
const savingElement = ref(false)
const elementError = ref('')

function openCreate() {
  editing.value = null
  elementError.value = ''
  modalOpen.value = true
}

function openEdit(element) {
  editing.value = element
  elementError.value = ''
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editing.value = null
}

async function saveElement(payload) {
  savingElement.value = true
  elementError.value = ''
  try {
    if (editing.value) {
      await updateRichFilterElement(teamId.value, richFilterId.value, editing.value.id, payload)
    } else {
      await addRichFilterElement(teamId.value, richFilterId.value, payload)
    }
    await reload()
    closeModal()
  } catch (e) {
    elementError.value = e?.response?.data?.message || 'Kaydedilemedi.'
  } finally {
    savingElement.value = false
  }
}

async function removeElement(element) {
  if (!confirm(`"${element.name}" silinsin mi?`)) return
  await deleteRichFilterElement(teamId.value, richFilterId.value, element.id)
  await reload()
}

// ─── Sınıflandırma denetimi ───────────────────────────────────────────────

const audit = ref(null)
const loadingAudit = ref(false)

async function loadAudit() {
  if (!teamId.value || !filter.value) return
  loadingAudit.value = true
  try {
    audit.value = await auditRichFilter(teamId.value, richFilterId.value, projectId.value)
  } catch (e) {
    console.error('Denetim alınamadı', e)
    audit.value = null
  } finally {
    loadingAudit.value = false
  }
}

// ─── Uyarı kuralları ──────────────────────────────────────────────────────

const previewing = ref(null)
/** Kural id → { text, tone } — anlık hesap sonucu. */
const alertPreview = ref({})

async function runAlertPreview(element) {
  previewing.value = element.id
  try {
    const result = await previewRichFilterAlert(teamId.value, richFilterId.value, element.id)
    alertPreview.value = { ...alertPreview.value, [element.id]: describeAlert(result) }
  } catch (e) {
    alertPreview.value = {
      ...alertPreview.value,
      [element.id]: { text: e?.response?.data?.message || 'Hesaplanamadı', tone: 'bg-red-50 text-red-700' },
    }
  } finally {
    previewing.value = null
  }
}

function describeAlert(result) {
  if (result.problem) return { text: result.problem, tone: 'bg-amber-50 text-amber-700' }

  const percent = Math.round((result.ratio ?? 0) * 100)
  const text = `${percent}% (${result.numerator}/${result.denominator})`
  return {
    text,
    tone: result.breached ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700',
  }
}

// ─── Zaman serisi geçmişi ─────────────────────────────────────────────────

const backfilling = ref(null)
const backfillResult = ref(null)

/** Kurgu sonucunun kullanıcıya söylenişi — "yapılamadı" da sebebiyle söylenir. */
const BACKFILL_TITLES = {
  ok: 'Geçmiş kurgulandı',
  unsupported: 'Geçmiş kurgulanamadı',
  too_large: 'Geçmiş kurgulanamadı',
  mismatch: 'Geçmiş kurgulanamadı',
}

async function runBackfill(element) {
  backfilling.value = element.id
  backfillResult.value = null
  try {
    const result = await backfillRichFilterSeries(teamId.value, richFilterId.value, element.id)
    backfillResult.value = {
      status: result.status,
      title: `${element.name}: ${BACKFILL_TITLES[result.status] || 'Sonuç alınamadı'}`,
      detail: result.status === 'ok'
        ? `${result.written} günün değeri görev tarihçesinden hesaplandı. `
          + 'Kurgulanan bölüm grafikte kesikli çizilir.'
        : result.reason,
    }
  } catch (e) {
    backfillResult.value = {
      status: 'error',
      title: `${element.name}: Geçmiş kurgulanamadı`,
      detail: e?.response?.data?.message || 'Sunucuya ulaşılamadı.',
    }
  } finally {
    backfilling.value = null
  }
}

// ─── Sıralama (sürükle-bırak) ─────────────────────────────────────────────

const dragIndex = ref(null)

async function dropOn(targetIndex) {
  const from = dragIndex.value
  dragIndex.value = null
  if (from === null || from === targetIndex) return

  const ordered = [...smartFilters.value]
  const [moved] = ordered.splice(from, 1)
  ordered.splice(targetIndex, 0, moved)

  await reorderRichFilterElements(teamId.value, richFilterId.value, ordered.map(e => e.id))
  await reload()
  // Sıra sınıflandırmayı değiştirir; eldeki dağılım artık geçersiz.
  breakdown.value = []
}

// ─── Dağılım ──────────────────────────────────────────────────────────────

const breakdown = ref([])
const loadingBreakdown = ref(false)

async function loadBreakdown() {
  if (!teamId.value || !filter.value) return
  loadingBreakdown.value = true
  try {
    breakdown.value = await aggregateQuery(teamId.value, {
      query: filter.value.effectiveQuery || '',
      groupBy: smartField(filter.value.name),
      projectId: projectId.value,
    })
  } catch {
    breakdown.value = []
  } finally {
    loadingBreakdown.value = false
  }
}

const maxBucketValue = computed(() =>
  breakdown.value.reduce((max, b) => Math.max(max, Number(b.value) || 0), 0)
)

const unclassifiedCount = computed(() =>
  Number(breakdown.value.find(b => !b.key)?.value || 0)
)

function barWidth(bucket) {
  const max = maxBucketValue.value
  if (!max) return '0%'
  return `${Math.max(2, (Number(bucket.value) / max) * 100)}%`
}

// ─── Filtre işlemleri ─────────────────────────────────────────────────────

async function copyFilter() {
  const copy = await duplicateRichFilter(teamId.value, richFilterId.value)
  router.push(`/rich-filters/${copy.id}`)
}

async function removeFilter() {
  if (!confirm(`"${filter.value.name}" zengin filtresi silinsin mi?`)) return
  await deleteRichFilter(teamId.value, richFilterId.value)
  router.push('/rich-filters')
}

// ─── Yükleme ──────────────────────────────────────────────────────────────

async function reload() {
  filter.value = await getRichFilter(teamId.value, richFilterId.value)
  general.value = {
    name: filter.value.name,
    description: filter.value.description || '',
    baseFilterId: filter.value.baseFilterId || null,
    baseQuery: filter.value.baseQuery || '',
    visibility: filter.value.visibility,
  }
}

onMounted(async () => {
  try {
    await loadTeams()
    await reload()
    savedFilters.value = await getSavedFilters(teamId.value, projectId.value)
  } catch (e) {
    console.error('Zengin filtre yüklenemedi', e)
  } finally {
    loading.value = false
  }
})

// Bölüm değişince dağılımı bir kez getir — kullanıcı ayrıca yenileyebilir.
watch(section, (value) => {
  if (value === 'smart' && smartFilters.value.length && !breakdown.value.length) loadBreakdown()
})
</script>
