<template>
  <div class="flex flex-row w-screen bg-gray-50 min-h-screen">
    <SideBar :team-id="teamId" class="hidden lg:flex"/>
    <!-- Main Content -->
    <div class="flex-1 w-screen overflow-auto">
      <div class="p-6 w-full h-full">
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
        <div v-else class="bg-white rounded-2xl shadow-sm border border-gray-200 w-full">
          <div class="flex flex-wrap gap-4 justify-center p-4">
            <!-- Hatalı :isAdmin=isAdmin bağlaması düzeltildi -->
            <RetroColumn
                v-for="(column, index) in board?.columns"
                :key="index"
                :board-id="boardId"
                :column="column"
                :is-admin="isAdmin"
                :members="team?.members"
                :team-id="teamId"
                @addItem="addItem"
                @openDetail="openDetail"
            />
          </div>
        </div>
      </div>

      <!-- Item Detail Modal -->
      <div v-if="showItemDetail"
           class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60 backdrop-blur-sm transition-opacity duration-300">
        <RetroItemDetail
            :board-id="boardId"
            :item="selectedItem"
            :members="team?.members"
            :team-id="teamId"
            class="absolute"
            @close="showItemDetail = false"
        />
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
    boardId: {type: String, required: true},
    teamId: {type: String, required: true}
  },
  data() {
    return {
      board: null,
      team: null,
      anonymousMode: false,
      showItemDetail: false,
      selectedItem: null,
      allRetroItems: [],
      debounceTimer: null,
      isLoading: false,
      lastUpdateTime: Date.now()
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
      console.log("Loading board:", this.boardId, "for team:", this.teamId);
      // Board verilerini yükle
      getRetroBoard(this.teamId, this.boardId, (board) => {
        this.board = board || {};
        this.loadAllRetroItems();
        this.isLoading = false;
      });

      // Team verilerini yükle
      getTeamById(this.teamId, (team) => {
        this.team = team || {};
      });
    },

    cleanup() {
      if (this.debounceTimer) clearTimeout(this.debounceTimer);
    },

    // Debounced item creation
    addItem(item) {
      if (!item) return;
      if (this.debounceTimer) clearTimeout(this.debounceTimer);

      this.debounceTimer = setTimeout(() => {
        const newItem = {
          ...item,
          owner: this.anonymousMode ? "Anonymous" : localStorage.getItem("user"),
          createdAt: new Date().toISOString(),
          votes: []
        };

        createRetroItem(this.teamId, this.boardId, newItem.column, newItem)
            .then(() => {
              createToast("Item created.", {type: "success", position: "top-center"});
            })
            .catch((error) => {
              console.error("Error creating item:", error);
              createToast("Error creating item. Please try again.", {type: "error", position: "top-center"});
            });
      }, 300); // 300ms debounce
    },

    // Rate limited detail opening
    openDetail(item) {
      if (!item) return;
      const now = Date.now();
      if (now - this.lastUpdateTime < 120) { // 120ms rate limit
        return;
      }
      this.lastUpdateTime = now;
      this.selectedItem = item;
      this.showItemDetail = true;
    },

    handleToggleAnonymous(value) {
      this.anonymousMode = value;
    },

    handleExportResults() {
      if (this.allRetroItems.length === 0) {
        createToast("No retro items to export!", {type: "warning", position: "top-center"});
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
        const blob = new Blob([jsonString], {type: "application/json"});
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
        createToast("Export failed.", {type: "error", position: "top-center"});
      }
    },

    loadAllRetroItems() {
      if (!this.board?.columns) return;
      this.allRetroItems = [];
      const columnNames = this.board.columns;

      const loadPromises = columnNames.map(columnName => new Promise(resolve => {
        getRetroItems(this.teamId, this.boardId, columnName, (items) => {
          if (Array.isArray(items)) {
            const normalized = items.map(it => ({...it, column: it.column || columnName}));
            this.allRetroItems = [
              ...this.allRetroItems.filter(existing => existing.column !== columnName),
              ...normalized
            ];
          }
          resolve();
        });
      }));

      Promise.all(loadPromises).catch(error => {
        console.error("Error loading retro items:", error);
        createToast("Error loading items. Please refresh the page.", {type: "error", position: "top-center"});
      });
    },
    handleOpenSettings(){
      createToast("Settings feature is coming soon!", {type: "info", position: "top-center"});
    }
  },
  computed: {
    columns() {
      return this.board?.columns;
    },
    isAdmin() {
      return this.team?.adminEmail === localStorage.getItem("user");
    }
  }
};
</script>
