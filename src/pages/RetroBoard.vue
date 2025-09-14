<template>
  <div class="flex h-screen bg-gray-50">
    <!-- SideBar -->
    <SideBar :team-id="teamId" class="hidden lg:flex" />

    <!-- Main Content -->
    <div class="flex-1 overflow-auto">
      <div class="p-6">
        <!-- Header Component -->
        <RetroBoardHeader
          :board-name="board?.retroBoardName || ''"
          :member-count="team?.members?.length || 0"
          :anonymous-mode="anonymousMode"
          @toggle-anonymous="handleToggleAnonymous"
          @export-results="handleExportResults"
          @open-settings="handleOpenSettings"
        />

        <div class="bg-white rounded-2xl shadow-sm border border-gray-200">
          <div class="flex flex-wrap gap-4  justify-center">
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
      allRetroItems: [] // Tüm retro item'larını tutacak
    }
  },
  created() {
    getRetroBoard(this.teamId, this.boardId, (board) => {
      this.board = board
      this.loadAllRetroItems()
    })
    getTeamById(this.teamId, (team) => {
      this.team = team
    })
  },
  methods: {
    getItems(column) {
      return this.items[column]
    },
    addItem(item) {
      const newItem = {
        ...item,
        owner: this.anonymousMode ? "Anonymous" : localStorage.getItem("user")
      }
      createRetroItem(this.teamId, this.boardId, newItem.column, newItem).then(()=>{
        createToast('Item created. ',{type:'success',position:'top-center'})
      })

    },
    openDetail(item) {
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

      this.allRetroItems = [];
      const promises = [];

      this.board.columns.forEach(column => {
        const promise = new Promise((resolve) => {
          getRetroItems(this.teamId, this.boardId, column.id, (items) => {
            if (items) {
              this.allRetroItems = [...this.allRetroItems, ...items];
            }
            resolve();
          });
        });
        promises.push(promise);
      });

      Promise.all(promises).then(() => {
        // Tüm item'lar yüklendi, progress hesaplanabilir
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
    totalRetroItems() {
      return this.allRetroItems.length;
    },
    processedRetroItems() {
      // Status'u 'processed', 'done', 'completed' olanları say
      return this.allRetroItems.filter(item =>
        item.status
      ).length;
    }
  }
}
</script>
