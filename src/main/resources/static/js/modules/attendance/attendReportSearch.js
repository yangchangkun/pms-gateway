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
        url: baseURL + "app/attend/report/attendReportAuthStaffList",
        data: queryParam,
        success: function(r){
            if(layerIndex!=null){
                layer.close(layerIndex);
            }
            if(r.code == 0){
                vm.today = r.today;
                vm.gridDatas = r.authStaffList;
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

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock:false,

        today:'',

        queryParam:{
            searchKey:''
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
        queryList: function () {
            loadGridData(true, vm.queryParam);
        },

        toStaffAttendDetail:function(userId){
            var date = vm.today.split("-");
            url = "../attendance/attendReportStaff.html?userId="+userId+"&year="+date[0]+"&month="+date[1];
            openWin(url);
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
        this.day = currentDate();

        this.queryParam = {
            searchKey:'',
        }
        loadGridData(false, this.queryParam);

    }
});

