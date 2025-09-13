<template>
  <div class="flex flex-row w-screen">
    <SideBar :team-id="teamId"></SideBar>
    <div class="flex-1">
      <h1>Preview version</h1>
      <Backlog :teamId="teamId"></Backlog>
    </div>
  </div>
</template>

<script>
import Backlog from "../components/work/Backlog.vue";
import SideBar from "../components/SideBar.vue";

export default {
  name: "WorkList",
  components: {Backlog, SideBar},
  props: {
    teamId: String
  }
}
</script>
