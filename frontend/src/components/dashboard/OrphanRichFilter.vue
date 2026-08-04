<template>
  <div class="flex flex-col items-center justify-center text-center gap-2 py-8">
    <span class="text-2xl">⚠</span>
    <p class="text-sm text-gray-600">Bu widget'ın zengin filtresi bulunamadı.</p>
    <p class="text-[11px] text-gray-400 max-w-xs">
      Filtre silinmiş ya da paylaşımı kaldırılmış olabilir. Widget'ı kaldırabilir
      veya başka bir zengin filtre seçebilirsiniz.
    </p>
    <router-link to="/rich-filters" class="text-[11px] text-purple-600 hover:underline">
      Zengin filtreler
    </router-link>
  </div>
</template>

<script setup>
/**
 * Kaynağı çözülemeyen zengin filtre widget'ının yerine geçen kart.
 *
 * Widget'ı sessizce silmek, kullanıcının dashboard düzenini haber vermeden
 * bozardı; bu yüzden yerinde kalır ve durumu açıkça söyler
 * (bkz. RICH_FILTER_PLAN.md — K15).
 */
</script>
