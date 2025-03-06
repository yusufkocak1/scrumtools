<template>
  <li
      :key="task.id"
      class="pointer-events-none opacity-50 text-slate-800 flex w-full items-center rounded-md p-3 transition-all hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100"
  >
    <h3 class="font-semibold">{{ task.title }}</h3>
    <p>{{ task.description }}</p>
    <div class="mt-2 text-sm text-gray-500">Durum: {{ task.status }}</div>
  </li>
</template>

<script>
export default {
  name: "BacklogListItem",
  props: {
    task: {}
  },
  data() {
    return {};
  },
  methods: {}
}
</script>
