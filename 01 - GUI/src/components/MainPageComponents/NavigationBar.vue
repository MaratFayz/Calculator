<template>
  <v-navigation-drawer absolute temporary v-model="this.isShown" width="350">
    <v-sheet class="pa-4 primary lighten-2">
      <v-text-field
        v-model="search"
        label="Поиск опций"
        dark
        flat
        solo-inverted
        hide-details
        clearable
        clear-icon="mdi-close-circle-outline"
      ></v-text-field>
      <v-checkbox
        v-model="caseSensitive"
        dark
        hide-details
        label="Case sensitive search"
      ></v-checkbox>
    </v-sheet>
    <v-card-text class="bar">
      <v-treeview
        :items="items"
        :search="search"
        :filter="filter"
        :open-all="true"
        open-on-click
        @update:active="choosePage"
      >
        <template v-slot:prepend="{ item }">
          <v-btn small :to="item.id" v-if="!item.children">
            <v-icon v-text="item.icon"></v-icon>
            {{ item.name2 }}
          </v-btn>
          <v-icon v-text="item.icon" v-if="item.children"></v-icon>
          <span v-if="item.children"> {{ item.name2 }} </span>
        </template>
      </v-treeview>
    </v-card-text>
  </v-navigation-drawer>
</template>

<script>
export default {
  name: "NavigationBar",
  data: function () {
    return {
      search: null,
      caseSensitive: false,
      items: [
        {
          id: "filesExcelUpload",
          name2: "Загрузка файлов Excel",
          icon: "mdi-folder",
          children: [
            {
              id: "filesExcelUpload:PSD",
              name2: "Загрузка ПСД",
              icon: "mdi-microsoft-excel",
            },
          ],
        },
        {
          id: "calculations",
          name2: "Расчет поправок",
          icon: "mdi-folder",
          children: [
            {
              id: "calculations:leasingDeposits",
              name2: "Лизинговые депозиты",
              icon: "mdi-calculator",
            },
          ],
        },
        {
          id: "adminPage",
          name2: "Админская страница",
          icon: "mdi-folder",
          children: [
            {
              id: "admin:adminPage",
              name2: "Страница администратора",
              icon: "mdi-calculator",
            },
          ],
        },
      ],
    };
  },
  computed: {
    filter() {
      return this.caseSensitive
        ? (item, search, textKey) => item[textKey].indexOf(search) > -1
        : undefined;
    },
    isShown() {
      console.log("this.$store.getters.showNavigationPanel =>" + this.$store.getters.showNavigationPanel);
      return this.$store.getters.showNavigationPanel;
    },
  },
  methods: {
    choosePage: function (selection) {
      console.log("selection => " + selection);
      console.log(selection);

      this.$store.dispatch("setInvertedShowNavigationPanel");
      this.$store.dispatch("setChosenPageOnMainPage", selection[0]);
    },
    makeFalseShowingNavigationBar: function () {
      this.$store.dispatch("setFalseShowNavigationPanel");
    },
  },
};
</script>

<style scoped>
</style>