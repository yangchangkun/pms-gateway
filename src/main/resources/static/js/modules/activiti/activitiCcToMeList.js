
function loadGridData(queryParam){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/activiti/query/ccToMeList",
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
    el: '#vueApp',
    data: {

        dialogIndex: null,

        title: null,

        showList:true,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

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
        splitLine:function(value){
            return value.replace(new RegExp(',','g'), "<br/>");
        }
    },

    methods: {

        queryList: function (page) {
            vm.queryParam.pageNum = page;
            loadGridData(vm.queryParam);
        },

        reload:function(){
            if(vm.dialogIndex!=null){
                layer.close(vm.dialogIndex);
            }
            vm.queryList(vm.webPage.pageNum);
        },

        toDetail:function(obj){
            //将抄送给我的消息设置为已读
            $.ajax({
                type: "POST",
                url: baseURL + "app/activiti/query/setCcState",
                data: {id:obj.id},
                success: function(r){
                    if(r.code == 0){

                    }
                },
                error:function(data){

                }
            });

            if(obj.businessKey=='attendance_supplement'){
                var url = "../attendance/supplementDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='attendance_overtime'){
                var url = "../attendance/overTimeDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='attendance_leave'){
                var url = "../attendance/leaveDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='attendance_travel'){
                var url = "../attendance/travelDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='attendance_egress'){
                var url = "../attendance/egressDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='hr_join_apply'){
                var url = "../hr/joinApplyDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='hr_leave_apply'){
                var url = "../hr/leaveApplyDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='hr_leave_handover'){
                var url = "../hr/leaveHandoverDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='hr_post_adjustment'){
                var url = "../hr/postAdjustmentDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='hr_mac_subsidy'){
                var url = "../hr/macSubsidyDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='hr_visit_subsidy'){
                var url = "../hr/visitSubsidyDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='adm_seal_apply'){
                var url = "../adm/sealDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='adm_card_apply'){
                var url = "../adm/cardDetail.html?id=" + obj.businessId;
                openWin(url);
            } else if(obj.businessKey=='adm_passenger_ticket'){
                var url = "../adm/passengerTicketDetail.html?id=" + obj.businessId;
                openWin(url);
            }
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
        loadGridData(this.queryParam);
    }
});

