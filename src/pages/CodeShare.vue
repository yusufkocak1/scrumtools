<template>
  <div class="flex flex-row w-screen">
    <SideBar :team-id="teamId"></SideBar>
    <div class="flex-1 p-4">
      <div class="container mx-auto">
        <div class="flex justify-between m-4 space-x-2">
          <input
              v-model="tag"
              class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500"
              placeholder="Tag giriniz"
              required
              type="text"
          />
          <button class="px-4 py-2 bg-green-600 text-white rounded-md" @click="get">
            Getir
          </button>
          <button class="px-4 py-2 bg-blue-500 text-white rounded-md" @click="save">
            Paylaş
          </button>
        </div>

        <CodeEditor
            ref="codeEditor"
            v-model="data"
            @blur="save"
            width="100%"
            height="1200px"
            theme="github-dark"
            :line-nums="true"
            :languages="[['java','Java'],['javascript','JS'],['cpp','C++'],['python','Python'],['php','PHP']]"
            :header="true"
        ></CodeEditor>
      </div>
    </div>
  </div>
</template>

<script>
import hljs from 'highlight.js';
import CodeEditor from 'simple-code-editor';
import SideBar from "../components/SideBar.vue";
import { getCodeShare, saveCodeShare } from "../firebase/codeShareService.js";

export default {
  name: "CodeShare",
  components: {
    CodeEditor,
    SideBar
  },
  props: {
    teamId: String
  },
  data() {
    return {
      data: "",
      tag: ""
    }
  },
  created() {
    const savedTag = localStorage.getItem("tag");
    if(savedTag) {
      this.tag = savedTag;
      this.get();
    }
  },
  methods: {
    async save() {
      try {
        await saveCodeShare(this.teamId, this.data , this.tag);
        this.refreshHighlight();
      } catch (error) {
        console.error("Kaydetme hatası:", error);
      }
    },
    async get() {
      try {
        await getCodeShare(this.teamId, this.tag, (response) => {
          if(response) {
            localStorage.setItem("tag", this.tag);
            this.data = response.data;
            this.refreshHighlight();
          }
        });
      } catch (error) {
        console.error("Veri çekme hatası:", error);
        this.data = "";
      }
    },
    refreshHighlight() {
      this.$nextTick(() => {
        const editor = this.$refs.codeEditor?.$el;
        if(editor) {
          const codeElement = editor.querySelector('code');
          if(codeElement) {
            delete codeElement.dataset.highlighted;
            hljs.highlightElement(codeElement);
          }
        }
      });
    },
    watch: {
      data() {
        console.log("Data changed");
        this.refreshHighlight();
      }
    }
  }
}
</script>
