
function loadGridData(queryParam){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/adm/notice/list",
        data: queryParam,
        success: function(r){
            //layer.close(layerIndex);
            if(r.code == 0){
                vm.gridDatas = r.gridDatas;
                vm.webPage = r.webPage;
            }else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        },
        error:function(data){
            //layer.close(layerIndex);
            layer.open({
                content: "请求出错，请稍后再试！",
                btn: '我知道了'
            });
        }
    });
}

var vm = new Vue({
    el:'#vueApp',
    data:{
        dialogIndex:null,

        title: null,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock:false,

        queryParam:{
            searchKey: '',

            pageNum:1,
            pageSize:10
        },

        webPage:{
            pageNum:0,
            pageSize:0,
            size:0,
            startRow:0,
            endRow:0,
            total:0,
            pages:0,
            prePage:0,
            nextPage:0,
            isFirstPage:false,
            isLastPage:false,
            hasPreviousPage:false,
            hasNextPage:false,
            navigatePages:'',
            navigatepageNums:[],
            navigateFirstPage:0,
            navigateLastPage:0
        },

        gridDatas:[],

    },

    filters:{
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        newLine:function(value){
            return value.replace(/\n/g, "<br/>");
        }
    },

    methods: {
        queryList: function (page) {
            vm.queryParam.pageNum = page;
            loadGridData(vm.queryParam);
        },

        search: function () {
            vm.queryParam.pageNum = 0;
            loadGridData(vm.queryParam);
        },

        toDetail: function (obj) {
            var url = "../notice/noticeDetail.html?id="+obj.id;
            openWin(url);
        },

        validator: function () {
            return  false;
        },

    },
    created: function(){

    },
    updated: function(){

    },
    mounted: function(){
        this.queryParam = {
            searchKey: '',

            pageNum:1,
            pageSize:10
        }
        loadGridData(this.queryParam);
    }
});

