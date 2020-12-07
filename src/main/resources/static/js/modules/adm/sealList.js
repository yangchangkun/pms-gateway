/**
 * 加载数据
 * @param loadTag 是否要显示加载框 true 是， false 否。
 * 一般，页面初始进入的时候不需加载框，因为webview会有自带的进度条。后续的刷新数据操作才需要
 * @param queryParam
 */
function loadGridData(loadTag, queryParam){
    var layerIndex = null;
    if(loadTag){
        layerIndex = layer.open({
            type: 2
            ,content: '加载中...'
        });
    }

    $.ajax({
        type: "POST",
        url: baseURL + "app/adm/sealApply/list",
        data: queryParam,
        success: function(r){
            if(layerIndex!=null){
                layer.close(layerIndex);
            }
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
            if(layerIndex!=null){
                layer.close(layerIndex);
            }
            layer.open({
                content: "请求出错，请稍后再试！",
                btn: '我知道了'
            });
        }
    });
}

var vm = new Vue({
    el: '#vueApp',
    data: {

        dialogIndex:null,

        title: null,

        showList:true,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock:false,

        queryParam:{
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
            vm.showList = true;
            vm.queryParam.pageNum = page;
            loadGridData(true, vm.queryParam);
        },

        reload:function(){
            if(vm.dialogIndex!=null){
                layer.close(vm.dialogIndex);
            }
            vm.queryList(vm.webPage.pageNum);
        },

        toDetail:function(id){
            var url = "../adm/sealDetail.html?id="+id;
            openWin(url);

        },


        /**
         * 图片预览
         * @param imgUrl
         */
        imgPreview: function(imgUrl){
            openImgPreviewWin(imgUrl);
        },

        toList:function(){
            vm.showList = true;
        },

        validator: function () {
            return false;
        },

    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        this.queryParam = {
            pageNum:1,
            pageSize:10
        }
        loadGridData(false, this.queryParam);

    }
});

