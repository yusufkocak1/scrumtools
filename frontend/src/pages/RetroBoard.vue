<template>
  <div class="flex flex-row w-full bg-gray-50 min-h-screen pb-20 lg:pb-0">
    <SideBar/>
    <!-- Main Content -->
    <div class="flex-1 w-full min-w-0 overflow-auto">
      <div class="p-4 sm:p-6 w-full h-full">
        <!-- Header Component -->
        <RetroBoardHeader
            :anonymous-mode="anonymousMode"
            :board-name="board?.retroBoardName || ''"
            :member-count="team?.members?.length || 0"
            @toggle-anonymous="handleToggleAnonymous"
            @export-results="handleExportResults"
            @open-settings="handleOpenSettings"
        />
        <div v-if="isLoading" class="fixed inset-0 bg-white bg-opacity-75 flex items-center justify-center z-[10000]">
          <div class="animate-spin rounded-full h-32 w-32 border-b-2 border-gray-900"></div>
        </div>
        <div v-else :class="['grid grid-cols-1 gap-4 sm:gap-5 items-start w-full', gridColsClass]">
          <RetroColumn
              v-for="(column, index) in board?.columns"
              :key="column"
              :accent-index="index"
              :board-id="boardId"
              :column="column"
              :is-admin="isAdmin"
              :members="team?.members"
              :team-id="teamId"
              :items="itemsByColumn[column] || []"
              @addItem="addItem"
              @openDetail="openDetail"
          />
        </div>
      </div>

      <!-- Item Detail Modal: mobilde bottom-sheet, masaüstünde ortalanmış modal -->
      <div v-if="showItemDetail"
           class="fixed inset-0 z-[999] flex items-end sm:items-center justify-center bg-black bg-opacity-60 backdrop-blur-sm p-0 sm:p-4 transition-opacity duration-300"
           @click.self="showItemDetail = false">
        <RetroItemDetail
            ref="itemDetailRef"
            :board-id="boardId"
            :is-admin="isAdmin"
            :item="selectedItem"
            :members="team?.members"
            :team-id="teamId"
            @close="showItemDetail = false"
        />
      </div>

      <!-- Board Settings Modal: mobilde bottom-sheet, masaüstünde ortalanmış modal -->
      <div v-if="showSettings"
           class="fixed inset-0 z-[999] flex items-end sm:items-center justify-center bg-black bg-opacity-60 backdrop-blur-sm p-0 sm:p-4 transition-opacity duration-300"
           @click.self="showSettings = false">
        <RetroBoardSettings
            :board="board"
            :board-id="boardId"
            :team-id="teamId"
            @close="showSettings = false"
            @saved="handleSettingsSaved"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { getBoard, createItem, getItems } from "../api/RetroBoardApi.js";
import { getTeamById } from "../api/TeamApi.js";
import { connect, subscribe, unsubscribe } from "../api/websocket.js";
import { createBoardSync } from "../api/retroBoardSync.js";
import RetroColumn from "../components/retro/RetroColumn.vue";
import RetroItem from "../components/retro/RetroItem.vue";
import RetroItemDetail from "../components/retro/RetroItemDetail.vue";
import RetroBoardHeader from "../components/retro/RetroBoardHeader.vue";
import RetroBoardSettings from "../components/retro/RetroBoardSettings.vue";
import SideBar from "../components/SideBar.vue";
import { createToast } from "mosha-vue-toastify";
import { useTeamContext } from "../composables/useTeamContext.js";

export default {
  name: "RetroBoard",
  components: { SideBar, RetroBoardHeader, RetroBoardSettings, RetroItemDetail, RetroItem, RetroColumn },
  props: {
    boardId: { type: String, required: true },
    teamId: { type: String, required: true }
  },
  setup(props) {
    // Paylaşılan retro linkindeki takım merkezi context'e adopte edilir
    useTeamContext().adoptTeam(props.teamId)
  },
  data() {
    return {
      board: null,
      team: null,
      anonymousMode: false,
      showItemDetail: false,
      showSettings: false,
      selectedItem: null,
      allRetroItems: [],
      debounceTimer: null,
      isLoading: false,
      lastUpdateTime: Date.now(),
      // Kolon adı → items array haritası (reaktif)
      itemsByColumn: {},
      // WS sync motoru instance
      syncEngine: null,
      // WS topic
      wsTopic: null
    };
  },
  created() {
    this.initializeBoard();
  },
  beforeUnmount() {
    this.cleanup();
  },
  methods: {
    initializeBoard() {
      this.isLoading = true;
      this.wsTopic = `/topic/retro/${this.teamId}/${this.boardId}`;

      getBoard(this.teamId, this.boardId)
        .then(board => {
          this.board = board || {};
          return this.loadAllColumnItems(board?.columns || []);
        })
        .then(() => {
          // Board ve tüm kolonlar yüklendikten sonra WS sync'i başlat
          this.initSync();
        })
        .catch(e => {
          console.error('Board yüklenemedi', e);
          this.board = {};
        })
        .finally(() => { this.isLoading = false; });

      getTeamById(this.teamId)
        .then(team => { this.team = team || {}; })
        .catch(e => console.warn('Team yüklenemedi', e));
    },

    /**
     * Verilen kolon listesi için paralel getItems çağrısı yapar.
     * itemsByColumn reactive haritasını günceller.
     */
    async loadAllColumnItems(columns) {
      if (!columns || columns.length === 0) return;

      const results = await Promise.allSettled(
        columns.map(col =>
          getItems(this.teamId, this.boardId, col).then(items => ({ col, items }))
        )
      );

      const updated = { ...this.itemsByColumn };
      results.forEach(res => {
        if (res.status === 'fulfilled') {
          const { col, items } = res.value;
          updated[col] = this.prepareColumnItems(items, col);
        }
      });
      this.itemsByColumn = updated;

      // allRetroItems export için senkronize et
      this.allRetroItems = Object.values(this.itemsByColumn).flat();
    },

    /**
     * Item'ları eklenme sırasına göre (eski → yeni) sıralar ve column alanını damgalar.
     * Yeni eklenen item her zaman kolonun sonunda görünür.
     * API response'unda columnName var ama frontend item.column bekliyor.
     */
    prepareColumnItems(items, column) {
      if (!Array.isArray(items)) return [];
      return [...items]
        .sort((a, b) => {
          const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return ta - tb;
        })
        .map(i => (i.column ? i : { ...i, column: i.columnName || column }));
    },

    /**
     * Debounce sync motorunu başlatır ve WS subscription açar.
     */
    initSync() {
      const currentUser = localStorage.getItem('user') || '';

      // Sync motoru: flush geldiğinde etkilenen kolonları yenile
      this.syncEngine = createBoardSync(this.teamId, this.boardId, async (columns, events) => {
        // Board yapısı değiştiyse (isim/kolonlar) önce board'u tazele
        if (events.some(e => e.event === 'BOARD_UPDATED')) {
          try {
            const fresh = await getBoard(this.teamId, this.boardId);
            if (fresh) {
              this.board = fresh;
              // Silinen/yeniden adlandırılan kolonların eski anahtarlarını temizle
              const valid = new Set(fresh.columns || []);
              const pruned = {};
              Object.keys(this.itemsByColumn).forEach(k => {
                if (valid.has(k)) pruned[k] = this.itemsByColumn[k];
              });
              this.itemsByColumn = pruned;
            }
          } catch (e) {
            console.warn('Board yenilenemedi', e);
          }
        }

        // Hangi kolonlar etkilendi?
        const allColumns = this.board?.columns || [];
        const toRefresh = columns.has('__ALL__') ? allColumns : [...columns].filter(c => allColumns.includes(c));

        if (toRefresh.length === 0) return;

        // Sadece etkilenen kolonları getir
        const results = await Promise.allSettled(
          toRefresh.map(col =>
            getItems(this.teamId, this.boardId, col).then(items => ({ col, items }))
          )
        );

        const updated = { ...this.itemsByColumn };
        results.forEach(res => {
          if (res.status === 'fulfilled') {
            const { col, items } = res.value;
            updated[col] = this.prepareColumnItems(items, col);
          }
        });
        this.itemsByColumn = updated;
        this.allRetroItems = Object.values(this.itemsByColumn).flat();

        // Açık olan item detail modal'ını da güncelle
        const commentEvents = ['COMMENT_ADDED', 'COMMENT_DELETED'];
        const hasCommentEvent = events.some(e => commentEvents.includes(e.event));
        if (hasCommentEvent && this.showItemDetail && this.selectedItem) {
          const changesForCurrentItem = events.some(
            e => commentEvents.includes(e.event) && e.itemId === this.selectedItem.id
          );
          if (changesForCurrentItem && this.$refs.itemDetailRef) {
            this.$refs.itemDetailRef.fetchComments(true);
          }
        }

        // Başka birinin değişikliği ise toast göster
        const foreignEvent = events.find(e => e.triggeredBy && e.triggeredBy !== currentUser);
        if (foreignEvent) {
          createToast("Board updated by a team member.", {
            type: "info",
            position: "top-right",
            timeout: 3000,
            showIcon: true
          });
        }
      });

      // Tek WS subscription: tüm board değişiklikleri buraya gelir
      connect(() => {
        subscribe(this.wsTopic, (msg) => {
          // Tartışma zamanlayıcısı olayları veri taşır: debounce/refetch beklemeden
          // anında uygula ki sayaç tüm istemcilerde senkron aksın
          if (msg?.event && msg.event.startsWith('DISCUSSION_')) {
            this.applyDiscussionEvent(msg);
            return;
          }
          this.syncEngine.push(msg);
        });
      });
    },

    /**
     * WS'den gelen DISCUSSION_STARTED / DISCUSSION_EXTENDED / DISCUSSION_STOPPED
     * olayındaki zamanlayıcı alanlarını ilgili item'a anında yazar.
     */
    applyDiscussionEvent(msg) {
      const itemId = msg?.itemId;
      if (!itemId) return;
      const endsAt = msg.discussionEndsAt || null;
      const duration = msg.discussionDurationSeconds ? Number(msg.discussionDurationSeconds) : null;

      Object.values(this.itemsByColumn).forEach(items => {
        const target = (items || []).find(i => i.id === itemId);
        if (target) {
          target.discussionEndsAt = endsAt;
          target.discussionDurationSeconds = duration;
        }
      });
      if (this.selectedItem?.id === itemId) {
        this.selectedItem.discussionEndsAt = endsAt;
        this.selectedItem.discussionDurationSeconds = duration;
      }
    },

    cleanup() {
      if (this.debounceTimer) clearTimeout(this.debounceTimer);
      if (this.syncEngine) {
        this.syncEngine.destroy();
        this.syncEngine = null;
      }
      if (this.wsTopic) {
        unsubscribe(this.wsTopic);
        this.wsTopic = null;
      }
    },

    addItem(item) {
      if (!item) return;
      if (this.debounceTimer) clearTimeout(this.debounceTimer);

      this.debounceTimer = setTimeout(() => {
        const owner = this.anonymousMode ? "Anonymous" : localStorage.getItem("user");

        // Optimistik ekleme: WS debounce süresi dolmadan kendi item'ını anında göster
        const tempItem = {
          id: `temp-${Date.now()}`,
          value: item.value,
          column: item.column,
          owner,
          votes: [],
          comments: [],
          createdAt: new Date().toISOString(),
          _isOptimistic: true
        };
        const updated = { ...this.itemsByColumn };
        updated[item.column] = [...(updated[item.column] || []), tempItem];
        this.itemsByColumn = updated;

        createItem(this.teamId, this.boardId, item.column, item.value, owner)
          .then((createdItem) => {
            // Temp item'ı gerçek item ile değiştir
            const col = item.column;
            const colItems = [...(this.itemsByColumn[col] || [])];
            const tempIdx = colItems.findIndex(i => i.id === tempItem.id);
            if (tempIdx !== -1 && createdItem) {
              colItems[tempIdx] = { ...createdItem, column: col };
            } else if (tempIdx !== -1) {
              colItems.splice(tempIdx, 1);
            }
            this.itemsByColumn = { ...this.itemsByColumn, [col]: colItems };
            this.allRetroItems = Object.values(this.itemsByColumn).flat();
            createToast("Item created.", { type: "success", position: "top-center" });
          })
          .catch((error) => {
            console.error("Error creating item:", error);
            // Optimistik item'ı geri al
            const col = item.column;
            const colItems = (this.itemsByColumn[col] || []).filter(i => i.id !== tempItem.id);
            this.itemsByColumn = { ...this.itemsByColumn, [col]: colItems };
            createToast("Error creating item. Please try again.", { type: "error", position: "top-center" });
          });
      }, 300);
    },

    openDetail(item, column) {
      if (!item) return;
      const now = Date.now();
      if (now - this.lastUpdateTime < 120) return;
      this.lastUpdateTime = now;
      // API item'larında column alanı olmayabilir — kolondan gelen bilgiyle tamamla
      this.selectedItem = item.column ? item : { ...item, column: item.columnName || column };
      this.showItemDetail = true;
    },

    handleToggleAnonymous(value) {
      this.anonymousMode = value;
    },

    handleExportResults() {
      if (this.allRetroItems.length === 0) {
        createToast("No retro items to export!", { type: "warning", position: "top-center" });
        return;
      }

      const exportData = {
        board: {
          id: this.boardId,
          name: this.board?.retroBoardName || "Untitled Board",
          teamId: this.teamId,
          columns: this.board?.columns || []
        },
        team: {
          id: this.teamId,
          name: this.team?.teamName || "Unknown Team",
          memberCount: this.team?.members?.length || 0
        },
        exportInfo: {
          exportDate: new Date().toISOString(),
          totalItems: this.allRetroItems.length,
          exportedBy: localStorage.getItem("user") || "Unknown User"
        },
        retroItems: this.allRetroItems.map(item => ({
          id: item.id || null,
          value: item.value || "",
          owner: item.owner || "Unknown",
          column: item.column || "",
          status: item.status || "pending",
          createdAt: item.createdAt || null,
          votes: Array.isArray(item.votes) ? item.votes.length : 0,
          comments: item.comments || []
        }))
      };

      try {
        const jsonString = JSON.stringify(exportData, null, 2);
        const blob = new Blob([jsonString], { type: "application/json" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `retro-board-${(this.board?.retroBoardName || 'export').replace(/[^a-z0-9]/gi, '-').toLowerCase()}-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
      } catch (e) {
        console.error("Export failed", e);
        createToast("Export failed.", { type: "error", position: "top-center" });
      }
    },

    handleOpenSettings() {
      this.showSettings = true;
    },

    /**
     * Settings modalı kaydedildiğinde: board'u güncelle,
     * kolon yapısı değişmiş olabileceği için tüm item'ları baştan yükle.
     */
    handleSettingsSaved(updatedBoard) {
      this.showSettings = false;
      if (!updatedBoard) return;
      this.board = updatedBoard;
      this.itemsByColumn = {};
      this.loadAllColumnItems(updatedBoard.columns || []);
    }
  },
  computed: {
    columns() {
      return this.board?.columns;
    },
    /** Kolon sayısına göre responsive grid düzeni */
    gridColsClass() {
      const n = this.board?.columns?.length || 0;
      if (n <= 1) return 'sm:grid-cols-1';
      if (n === 2) return 'sm:grid-cols-2';
      if (n === 3) return 'sm:grid-cols-2 xl:grid-cols-3';
      return 'sm:grid-cols-2 xl:grid-cols-4';
    },
    isAdmin() {
      return this.team?.adminEmail === localStorage.getItem("user");
    }
  }
};
</script>
