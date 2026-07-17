<template>
  <div class="flex flex-col bg-white w-full sm:max-w-lg rounded-t-3xl sm:rounded-2xl shadow-2xl max-h-[92vh] sm:max-h-[85vh] overflow-hidden">
    <!-- Mobile drag handle -->
    <div class="sm:hidden flex justify-center pt-2 pb-1 flex-shrink-0">
      <div class="w-10 h-1 rounded-full bg-gray-300"></div>
    </div>

    <!-- Header -->
    <div class="flex items-center justify-between gap-2 px-4 py-3 border-b border-gray-200 flex-shrink-0">
      <div class="flex items-center gap-2.5 min-w-0">
        <div class="w-8 h-8 rounded-lg bg-indigo-100 flex items-center justify-center flex-shrink-0">
          <svg class="w-4 h-4 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/>
          </svg>
        </div>
        <h3 class="font-semibold text-gray-900 truncate">Board Settings</h3>
      </div>
      <button
        @click="$emit('close')"
        class="p-2 -mr-1 rounded-lg hover:bg-red-50 text-gray-400 hover:text-red-500 transition-all focus:outline-none focus:ring-2 focus:ring-red-500 flex-shrink-0"
        title="Close (Esc)"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- Body -->
    <div class="flex-1 overflow-y-auto px-4 py-4 space-y-5">
      <!-- Board name -->
      <div>
        <label class="block text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5">Board Name</label>
        <input
          v-model="boardName"
          type="text"
          maxlength="80"
          placeholder="Board name"
          class="w-full px-3.5 py-2.5 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
        />
      </div>

      <!-- Columns -->
      <div>
        <div class="flex items-center justify-between mb-1.5">
          <label class="block text-xs font-semibold text-gray-500 uppercase tracking-wide">Columns</label>
          <span class="text-xs text-gray-400">{{ cols.length }} column{{ cols.length === 1 ? '' : 's' }}</span>
        </div>

        <div class="space-y-2">
          <div
            v-for="(col, index) in cols"
            :key="col.key"
            class="flex items-center gap-1.5 group"
          >
            <!-- Reorder -->
            <div class="flex flex-col flex-shrink-0">
              <button
                :disabled="index === 0"
                @click="moveColumn(index, -1)"
                class="p-0.5 rounded text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 disabled:opacity-30 disabled:pointer-events-none transition-colors"
                title="Move up"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"/>
                </svg>
              </button>
              <button
                :disabled="index === cols.length - 1"
                @click="moveColumn(index, 1)"
                class="p-0.5 rounded text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 disabled:opacity-30 disabled:pointer-events-none transition-colors"
                title="Move down"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
              </button>
            </div>

            <!-- Column name -->
            <input
              v-model="col.name"
              type="text"
              maxlength="40"
              :placeholder="col.original || 'Column name'"
              class="flex-1 min-w-0 px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
            />

            <span
              v-if="!col.original"
              class="px-1.5 py-0.5 rounded-md bg-emerald-100 text-emerald-700 text-[10px] font-semibold uppercase flex-shrink-0"
            >
              New
            </span>

            <!-- Delete -->
            <button
              @click="removeColumn(index)"
              class="p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors flex-shrink-0"
              title="Remove column"
            >
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Add column -->
        <div class="flex items-center gap-2 mt-3">
          <input
            v-model="newColumn"
            @keydown.enter="addColumn"
            type="text"
            maxlength="40"
            placeholder="Add new column"
            class="flex-1 min-w-0 px-3 py-2 text-sm border border-dashed border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-shadow"
          />
          <button
            :disabled="!newColumn.trim()"
            @click="addColumn"
            class="px-3 py-2 rounded-xl bg-indigo-50 text-indigo-600 hover:bg-indigo-100 font-medium text-sm transition-colors disabled:opacity-40 disabled:pointer-events-none flex-shrink-0"
          >
            + Add
          </button>
        </div>

        <!-- Removed column warning -->
        <div
          v-if="removedColumns.length > 0"
          class="mt-3 flex items-start gap-2 px-3 py-2.5 rounded-xl bg-amber-50 border border-amber-200"
        >
          <svg class="w-4 h-4 text-amber-500 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
          </svg>
          <p class="text-xs text-amber-700 leading-relaxed">
            Removing <strong>{{ removedColumns.join(', ') }}</strong> will permanently delete all items in
            {{ removedColumns.length === 1 ? 'that column' : 'those columns' }}.
          </p>
        </div>

        <p v-if="validationError" class="mt-2 text-xs text-red-500">{{ validationError }}</p>
      </div>
    </div>

    <!-- Footer -->
    <div class="flex items-center justify-end gap-2 px-4 py-3 border-t border-gray-200 bg-gray-50/70 flex-shrink-0">
      <button
        @click="$emit('close')"
        class="px-4 py-2 rounded-xl text-sm font-medium text-gray-600 hover:bg-gray-100 transition-colors"
      >
        Cancel
      </button>
      <button
        :disabled="!!validationError || saving || !isDirty"
        @click="save"
        class="px-5 py-2 rounded-xl text-sm font-semibold bg-indigo-600 text-white hover:bg-indigo-700 transition-colors shadow-sm disabled:opacity-40 disabled:pointer-events-none flex items-center gap-2"
      >
        <svg v-if="saving" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
        {{ saving ? 'Saving…' : 'Save Changes' }}
      </button>
    </div>
  </div>
</template>

<script>
import { updateBoard } from "../../api/RetroBoardApi.js";
import { createToast } from "mosha-vue-toastify";

let colKeySeq = 0;

export default {
  name: "RetroBoardSettings",
  props: {
    board: { type: Object, required: true },
    teamId: { type: String, required: true },
    boardId: { type: String, required: true }
  },
  emits: ["close", "saved"],
  data() {
    return {
      boardName: this.board?.retroBoardName || "",
      // original: mevcut kolon adı (rename takibi için), null = yeni eklenen kolon
      cols: (this.board?.columns || []).map(name => ({ key: ++colKeySeq, original: name, name })),
      newColumn: "",
      saving: false
    };
  },
  computed: {
    trimmedNames() {
      return this.cols.map(c => c.name.trim());
    },
    /** Kaldırılan mevcut kolonlar (item kaybı uyarısı için) */
    removedColumns() {
      const originals = (this.board?.columns || []);
      const keptOriginals = this.cols.filter(c => c.original).map(c => c.original);
      return originals.filter(o => !keptOriginals.includes(o));
    },
    validationError() {
      if (!this.boardName.trim()) return "Board name cannot be empty.";
      if (this.cols.length === 0) return "Board must have at least one column.";
      if (this.trimmedNames.some(n => !n)) return "Column names cannot be empty.";
      const lower = this.trimmedNames.map(n => n.toLowerCase());
      if (new Set(lower).size !== lower.length) return "Column names must be unique.";
      return "";
    },
    isDirty() {
      if (this.boardName.trim() !== (this.board?.retroBoardName || "")) return true;
      const current = this.board?.columns || [];
      if (this.cols.length !== current.length) return true;
      return this.cols.some((c, i) => c.original !== current[i] || c.name.trim() !== current[i]);
    }
  },
  methods: {
    addColumn() {
      const name = this.newColumn.trim();
      if (!name) return;
      this.cols.push({ key: ++colKeySeq, original: null, name });
      this.newColumn = "";
    },
    removeColumn(index) {
      this.cols.splice(index, 1);
    },
    moveColumn(index, direction) {
      const target = index + direction;
      if (target < 0 || target >= this.cols.length) return;
      const [moved] = this.cols.splice(index, 1);
      this.cols.splice(target, 0, moved);
    },
    async save() {
      if (this.validationError || this.saving) return;
      this.saving = true;

      const columns = this.trimmedNames;
      const columnRenames = {};
      this.cols.forEach(c => {
        const name = c.name.trim();
        if (c.original && c.original !== name) columnRenames[c.original] = name;
      });

      try {
        const updated = await updateBoard(this.teamId, this.boardId, {
          retroBoardName: this.boardName.trim(),
          columns,
          columnRenames
        });
        createToast("Board updated.", { type: "success", position: "top-center" });
        this.$emit("saved", updated);
      } catch (e) {
        console.error("Board update failed", e);
        createToast("Failed to update board. Please try again.", { type: "danger", position: "top-center" });
      } finally {
        this.saving = false;
      }
    }
  }
};
</script>
