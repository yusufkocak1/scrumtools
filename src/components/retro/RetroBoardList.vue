<template>
  <ul class="p-2 flex justify-center lg:justify-start w-full flex-wrap gap-2 ">

    <li v-for="(board,index) in boardList"
        class=" flex flex-col my-6 justify-center items-center bg-white shadow-sm border border-slate-200 rounded-lg w-64 h-64">
      <div
          class="flex flex-col  my-2 justify-center items-center bg-white shadow-sm border border-slate-200 rounded-lg h-64">
        <div class="relative h-56 m-2.5 overflow-hidden text-white rounded-md"
             @click="openRetroBoard(board.id)">
          <img
              :src="getImageUrl(cardImageURLs[index%7]) "
              alt="card-image"/>
        </div>
        <div class="px-2  flex flex-row justify-center items-center w-full">
          <h6 class="mb-2 text-slate-800 text-xl font-semibold w-full justify-center items-center"
              @click="$emit('OpenRetroBoard',board.id)">
            {{ board.retroBoardName }}
          </h6>
          <div class="relative">
            <button
                @click="toggleDropdown(index)"
                class="p-1 rounded-full hover:bg-gray-100 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-300"
            >
              <svg class="w-5 h-5 text-gray-600" fill="currentColor" viewBox="0 0 16 16">
                <path d="M9.5 13a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm0-5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0zm0-5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0z"/>
              </svg>
            </button>

            <!-- Modern Dropdown -->
            <div v-if="showEdit[index]"
                 class="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
              <!-- Edit Option -->
              <button
                  @click="editBoard(board, index)"
                  class="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center transition-colors">
                <svg class="w-4 h-4 mr-3 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                </svg>
                Edit Board
              </button>

              <!-- Delete Option -->
              <button
                  @click="confirmDelete(board.id, index)"
                  class="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center transition-colors">
                <svg class="w-4 h-4 mr-3 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                </svg>
                Delete Board
              </button>
            </div>
          </div>
        </div>
      </div>
    </li>

    <!-- Create New Board -->
    <li class=" flex flex-col my-6 justify-center items-center bg-white shadow-sm border border-slate-200 rounded-lg w-64 h-64">
      <div @click="$emit('createBoard')" class="cursor-pointer hover:bg-gray-50 transition-colors rounded-lg p-8 w-full h-full flex items-center justify-center">
        <div class="flex flex-col justify-center items-center space-y-2">
          <span
              class="w-12 h-12 text-2xl font-extrabold border-4 border-slate-800 flex items-center justify-center rounded-full hover:border-blue-500 transition-colors">
            +
          </span>
          <span class="text-xl font-semibold text-gray-700">New Board</span>
        </div>
      </div>
    </li>
  </ul>

  <!-- Edit Modal -->
  <div v-if="showEditModal"
       class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60 backdrop-blur-sm transition-opacity duration-300">
    <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-md mx-4">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">Edit Board Name</h3>
      <input
          v-model="editingBoardName"
          type="text"
          class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
          placeholder="Enter board name"
          @keyup.enter="saveEditBoard"
      >
      <div class="flex justify-end space-x-3 mt-6">
        <button
            @click="cancelEdit"
            class="px-4 py-2 text-gray-600 hover:text-gray-800 transition-colors">
          Cancel
        </button>
        <button
            @click="saveEditBoard"
            class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
          Save
        </button>
      </div>
    </div>
  </div>

</template>

<script>
import CardBG1 from "../../assets/images/CardBG-1.png";
import CardBG2 from "../../assets/images/CardBG-2.png";
import CardBG3 from "../../assets/images/CardBG-3.png";
import CardBG4 from "../../assets/images/CardBG-4.png";
import CardBG5 from "../../assets/images/CardBG-5.png";
import CardBG6 from "../../assets/images/CardBG-6.png";
import CardBG7 from "../../assets/images/CardBG-7.png";
import { ref, reactive, watch } from 'vue'

export default {
  name: "RetroBoardList",
  props: {
    boardList: Array,
    teamId: String
  },
  setup(props, { emit }) {
    const cardImageURLs = [CardBG1, CardBG2, CardBG3, CardBG4, CardBG5, CardBG6, CardBG7]
    const showEdit = ref([])
    const showEditModal = ref(false)
    const editingBoardName = ref('')
    const boardToEdit = ref(null)

    // boardList değiştiğinde showEdit array'ini güncelle
    watch(() => props.boardList, (newBoardList) => {
      if (newBoardList) {
        showEdit.value = Array(newBoardList.length).fill(false)
      }
    }, { immediate: true })

    const getImageUrl = (url) => {
      return new URL(url, import.meta.url).href
    }

    const deleteBoard = (boardId) => {
      emit('deleteBoard', boardId)
    }

    const openRetroBoard = (boardId) => {
      // Router'ı inject etmek yerine, event emit edelim
      emit('openRetroBoard', boardId)
    }

    const toggleDropdown = (index) => {
      const newShowEdit = [...showEdit.value]
      newShowEdit[index] = !newShowEdit[index]
      showEdit.value = newShowEdit
    }

    const editBoard = (board, index) => {
      editingBoardName.value = board.retroBoardName
      boardToEdit.value = board.id
      showEditModal.value = true
      toggleDropdown(index)
    }

    const confirmDelete = (boardId, index) => {
      emit('confirmDelete', boardId)
      toggleDropdown(index)
    }

    const cancelEdit = () => {
      showEditModal.value = false
      editingBoardName.value = ''
      boardToEdit.value = null
    }

    const saveEditBoard = () => {
      if (editingBoardName.value.trim() === '') return
      emit('editBoard', { id: boardToEdit.value, name: editingBoardName.value })
      showEditModal.value = false
      editingBoardName.value = ''
      boardToEdit.value = null
    }

    return {
      cardImageURLs,
      showEdit,
      showEditModal,
      editingBoardName,
      boardToEdit,
      getImageUrl,
      deleteBoard,
      openRetroBoard,
      toggleDropdown,
      editBoard,
      confirmDelete,
      cancelEdit,
      saveEditBoard
    }
  }
}
</script>
