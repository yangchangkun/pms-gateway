
function loadCountData(){
    $.ajax({
        type: "POST",
        url: baseURL + "app/activiti/query/count",
        data: {},
        success: function(r){
            if(r.code == 0){
                vm.myApplyCnt = r.myApplyCnt;
                vm.myTodoCnt = r.myTodoCnt;
                vm.myDoneCnt = r.myDoneCnt;
                vm.myUnReadCcCnt = r.myUnReadCcCnt;
            }
        }
    });
}

function loadGridData(queryParam){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/activiti/query/myTodoList",
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

        myApplyCnt:0, //我发起的任务数量
        myTodoCnt:0, //我的待办任务数量
        myDoneCnt:0, //我审批的任务数量
        myUnReadCcCnt:0, //我的未读抄送信息数
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
            if(obj.businessType=='attendance_supplement'){
                var url = "../attendance/supplementApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='attendance_overtime'){
                var url = "../attendance/overTimeApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='attendance_leave'){
                var url = "../attendance/leaveApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='attendance_travel'){
                var url = "../attendance/travelApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='attendance_egress'){
                var url = "../attendance/egressApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='hr_join_apply'){
                var url = "../hr/joinApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='hr_leave_apply'){
                var url = "../hr/leaveApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='hr_leave_handover'){
                var url = "../hr/leaveHandoverApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='hr_post_adjustment'){
                var url = "../hr/postAdjustmentApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='hr_mac_subsidy'){
                var url = "../hr/macSubsidyApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='hr_visit_subsidy'){
                var url = "../hr/visitSubsidyApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='adm_seal_apply'){
                var url = "../adm/sealApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='adm_card_apply'){
                var url = "../adm/cardApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
                openWin(url);
            } else if(obj.businessType=='adm_passenger_ticket'){
                var url = "../adm/passengerTicketApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
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

        loadCountData();
    }
});

