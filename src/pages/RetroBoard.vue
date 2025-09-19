<template>
  <div class="flex flex-row w-screen bg-gray-50 min-h-screen">
    <SideBar :team-id="teamId" class="hidden lg:flex" />    <!-- SideBar -->
    <!-- Main Content -->
    <div class="flex-1 w-screen overflow-auto">
      <div class="p-6 w-full h-full">
        <!-- Header Component -->
        <RetroBoardHeader
          :board-name="board?.retroBoardName || ''"
          :member-count="team?.members?.length || 0"
          :anonymous-mode="anonymousMode"
          @toggle-anonymous="handleToggleAnonymous"
          @export-results="handleExportResults"
          @open-settings="handleOpenSettings"
        />

        <div class="bg-white rounded-2xl shadow-sm border border-gray-200 w-full">
          <div class="flex flex-wrap gap-4 justify-center p-4">
            <RetroColumn :isAdmin=isAdmin v-for="(column, index) in board?.columns" @addItem="addItem" :members="team?.members" :key="index" :column="column" :teamId="teamId" :boardId="boardId" @openDetail="openDetail"/>
          </div>
        </div>
      </div>

      <!-- Item Detail Modal -->
      <div v-if="showItemDetail" class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60 backdrop-blur-sm transition-opacity duration-300">
        <RetroItemDetail class="absolute" @close="showItemDetail = false" :item="RetroItemDetail" :board-id="boardId" :team-id="teamId" :members="team?.members"/>
      </div>
    </div>
  </div>
</template>

<script>
import {createRetroItem, getRetroBoard, getRetroItems} from "../firebase/RetroBoardService.js";
import {getTeamById} from "../firebase/TeamService.js";
import RetroColumn from "../components/retro/RetroColumn.vue";
import RetroItem from "../components/retro/RetroItem.vue";
import RetroItemDetail from "../components/retro/RetroItemDetail.vue";
import RetroBoardHeader from "../components/retro/RetroBoardHeader.vue";
import SideBar from "../components/SideBar.vue";
import {createToast} from "mosha-vue-toastify";

export default {
  name: "RetroBoard",
  components: {SideBar, RetroBoardHeader, RetroItemDetail, RetroItem, RetroColumn},
  props: {
    boardId: "",
    teamId: ""
  },
  data() {
    return {
      board: null,
      team: null,
      anonymousMode: false,
      item: Object,
      items: Array,
      showItemDetail: false,
      RetroItemDetail: {},
      allRetroItems: [],
      unsubscribeFunctions: [], // Listener cleanup için
      debounceTimer: null, // Debouncing için
      isLoading: false, // Loading state
      lastUpdateTime: Date.now(), // Rate limiting için
      updateQueue: [] // Batch update için
    }
  },
  created() {
    this.initializeBoard()
  },
  beforeUnmount() {
    this.cleanup()
  },
  methods: {
    initializeBoard() {
      this.isLoading = true

      // Board verilerini yükle
      getRetroBoard(this.teamId, this.boardId, (board) => {
        this.board = board
        this.loadAllRetroItems()
        this.isLoading = false
      })

      // Team verilerini yükle
      getTeamById(this.teamId, (team) => {
        this.team = team
      })
    },

    cleanup() {
      // Tüm listener'ları temizle
      this.unsubscribeFunctions.forEach(unsubscribe => {
        if (typeof unsubscribe === 'function') {
          unsubscribe()
        }
      })
      this.unsubscribeFunctions = []

      // Timer'ları temizle
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer)
      }
    },

    // Debounced item creation
    addItem(item) {
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer)
      }

      this.debounceTimer = setTimeout(() => {
        const newItem = {
          ...item,
          owner: this.anonymousMode ? "Anonymous" : localStorage.getItem("user"),
          createdAt: new Date().toISOString()
        }

        createRetroItem(this.teamId, this.boardId, newItem.column, newItem).then(()=>{
          createToast('Item created. ',{type:'success',position:'top-center'})
        }).catch(error => {
          console.error('Error creating item:', error)
          createToast('Error creating item. Please try again.',{type:'error',position:'top-center'})
        })
      }, 300) // 300ms debounce
    },

    // Rate limited detail opening
    openDetail(item) {
      const now = Date.now()
      if (now - this.lastUpdateTime < 100) { // 100ms rate limit
        return
      }
      this.lastUpdateTime = now

      this.RetroItemDetail = item
      this.showItemDetail = true
    },

    handleToggleAnonymous(value) {
      this.anonymousMode = value;
    },
    handleExportResults() {
      if (this.allRetroItems.length === 0) {
        createToast('No retro items to export!', {type: 'warning', position: 'top-center'});
        return;
      }

      // Board ve team bilgileriyle birlikte export data hazırla
      const exportData = {
        board: {
          id: this.boardId,
          name: this.board?.retroBoardName || 'Untitled Board',
          teamId: this.teamId,
          columns: this.board?.columns || []
        },
        team: {
          id: this.teamId,
          name: this.team?.teamName || 'Unknown Team',
          memberCount: this.team?.members?.length || 0
        },
        exportInfo: {
          exportDate: new Date().toISOString(),
          totalItems: this.allRetroItems.length,
          exportedBy: localStorage.getItem('user') || 'Unknown User'
        },
        retroItems: this.allRetroItems.map(item => ({
          id: item.id || null,
          title: item.title || '',
          description: item.description || '',
          owner: item.owner || 'Unknown',
          column: item.column || '',
          status: item.status || 'pending',
          createdAt: item.createdAt || null,
          votes: item.votes || 0,
          comments: item.comments || []
        }))
      };

      // JSON dosyası olarak indir
      const jsonString = JSON.stringify(exportData, null, 2);
      const blob = new Blob([jsonString], { type: 'application/json' });
      const url = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      link.download = `retro-board-${this.board?.retroBoardName?.replace(/[^a-z0-9]/gi, '-').toLowerCase() || 'export'}-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

    },
    loadAllRetroItems() {
      if (!this.board?.columns) return;

      // Hepsini sıfırla
      this.allRetroItems = [];

      const columnNames = this.board.columns; // String dizisi

      const loadPromises = columnNames.map(columnName => {
        return new Promise((resolve) => {
          // HATALI: columnName.id kullanılıyordu (undefined). Doğru: direkt columnName
          getRetroItems(this.teamId, this.boardId, columnName, (items) => {
            if (items && Array.isArray(items)) {
              // Gelen item'ların column field'ı yoksa ekleyelim
              const normalized = items.map(it => ({ ...it, column: it.column || columnName }))
              // Aynı kolona ait eski kayıtları çıkar, yenileri ekle (duplicate engelleme)
              this.allRetroItems = [
                ...this.allRetroItems.filter(existing => existing.column !== columnName),
                ...normalized
              ]
            }
            resolve();
          });
        });
      });

      Promise.all(loadPromises).then(() => {
        this.$nextTick(() => { /* gerekirse post-processing */ })
      }).catch(error => {
        console.error('Error loading retro items:', error)
        createToast('Error loading items. Please refresh the page.',{type:'error',position:'top-center'})
      });
    },
  },
  computed: {
    columns() {
      return this.board?.columns
    },
    isAdmin() {
      return this.team?.adminEmail === localStorage.getItem('user')
    },
    // Memoized computed properties
    totalRetroItems() {
      return this.allRetroItems?.length || 0;
    },
    processedRetroItems() {
      if (!this.allRetroItems) return 0
      return this.allRetroItems.filter(item =>
        item.status && ['processed', 'done', 'completed'].includes(item.status.toLowerCase())
      ).length;
    }
  }
}
</script>
