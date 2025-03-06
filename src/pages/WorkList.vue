<template>
  <h1>Preview version</h1>
<Backlog :teamId="teamId"></Backlog>
</template>

<script>
import Backlog from "../components/work/Backlog.vue";
export default {
  name: "WorkList",
  components: {Backlog},
  props: {
    teamId: String
  }
}
</script>