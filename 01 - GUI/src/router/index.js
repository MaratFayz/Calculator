import Vue from 'vue'
import Router from 'vue-router'
import FileExcelUploadPsd from "../components/MainPageComponents/FileExcelUploadPsd"
import EmptyPage from "../components/MainPageComponents/EmptyPage"
import AdminPage from "../components/MainPageComponents/AdminPage"
import CalculationsLeasingDeposits from "../components/MainPageComponents/CalculationsLeasingDeposits";

// использование маршрутизатора
Vue.use(Router)

export const router = new Router({
    routes: [
        {
            path: '/',
            name: 'EmptyPage',
            component: EmptyPage
        },
        {
            path: '/filesExcelUpload:PSD',
            name: 'FileExcelUploadPsd',
            component: FileExcelUploadPsd
        },
        {
            path: '/calculations:leasingDeposits',
            name: 'CalculationsLeasingDeposits',
            component: CalculationsLeasingDeposits
        },
        {
            path: '/admin:adminPage',
            name: 'AdminPage',
            component: AdminPage
        }
    ]
})