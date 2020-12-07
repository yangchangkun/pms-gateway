var browserH = $(window).height(); //浏览器当前窗口可视区域高度
var browserW = $(window).width(); //浏览器当前窗口可视区域高度
var dialogH = (browserH - 50) + "px";
var dialogW = (browserW - 50) + "px";

function loadGridData(queryParam) {
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    $.ajax({
        type: "POST",
        url: baseURL + "app/contact/list",
        data: queryParam,
        success: function (r) {
            layer.close(layerIndex);
            if (r.code == 0) {
                vm.gridDatas = r.gridDatas;
                vm.deptList = r.deptList;
            } else {
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        }
    });
}

var vm = new Vue({
    el: '#vueApp',
    data: {
        dialogIndex: null,

        title: null,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        entity: {
            userId: '',
            name: '',
            sex: '',
            photo: '',
            icType: '',
            icNum: '',
            icAddress: '',
            birthday: '',
            politicCountenance: '',
            maritalStatus: '',
            fertilityStatus: '',
            residence: '',
            eduType: '',
            eduSchool: '',
            eduMajor: '',
            eduDate: '',
            workBeginDate: '',
            joinCompanyId: '',
            joinCompanyName: '',
            joinDeptId: '',
            joinDeptName: '',
            joinDate: '',
            joinAddr: '',
            departureDate: '',
            contractType: '',
            postName: '',
            postRank: '',
            levelId: '',
            contractBeginDate: '',
            contractFinalDate: '',
            insureAddr: '',
            insureBegin: '',
            insureAccount: '',
            insureBank: '',
            insureTaxPolicy: '',
            linkName: '',
            linkPhone: '',
            linkRelation: ''
        },

        queryParam: {
            searchKey: '',
            deptId:''
        },

        gridDatas: [],

        deptList: []

    },
    methods: {
        resetQueryParams: function () {
            vm.queryParam = {
                searchKey: '',
                deptId:''
            }
            vm.queryList();
        },

        queryList: function () {
            loadGridData(vm.queryParam);
        },

        search: function () {
            /*if(isBlank(vm.queryParam.searchKey)){
                layer.open({
                    content: '请输入您要搜索的关键字'
                    ,btn: '我知道了'
                });
                return ;
            }*/
            loadGridData(vm.queryParam);
        },

        deptClick:function(obj){
            /**
             * 关闭侧边栏
             */
            sidebar.close();

            /**
             * 重载数据
             */
            vm.queryParam.deptId = obj.deptId;
            loadGridData(vm.queryParam);
        },

        toDetail: function (obj) {
            var url = "../contact/contactDetail.html?id="+obj.userId;
            openWin(url);
        },

        photoHandler:function(photo){
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }
            return photo;
        }
    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        var queryParam = {
            searchKey: '',
            deptId:''
        }
        loadGridData(queryParam);
    }
});

