function loadGridData(businessType){
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    var params = {
        businessType: businessType
    }

    var url = "app/attend/approval/todoList";
    $.ajax({
        url: baseURL + url,
        type: "POST",
        contentType: "application/x-www-form-urlencoded",
        data: params,
        success: function(r){
            layer.close(layerIndex);

            if(r.code == 0){
                vm.gridDatas = r.gridDatas;

                vm.myTodoCntMap = r.myTodoCntMap;
                vm.myTodoCnt = r.myTodoCnt;
            } else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        },
        error:function(data){
            layer.close(layerIndex);
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

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        showList:true,

        /**
         * 业务类型
         * 外出：attendance_egress
         * 出差：attendance_travel
         * 加班：attendance_overtime
         * 请假：attendance_leave
         * 补卡：attendance_supplement
         */
        businessType: 'attendance_egress',

        actEntity:{
            taskId : '', // 任务编号
            taskName : '', // 任务名称
            procInsId : '', // 流程实例ID
            procDefId : '', // 流程定义ID
            title : '', // 任务标题
            businessType : '',// 业务绑定类型
            businessId : '',// 业务绑定ID
            businessMemo : '',    //业务备注
            comment : '', // 任务意见，
            opinion : '', // 意见状态， 0：同意，1：驳回，2：退回
            assignee : '' //任务处理人
        },

        gridDatas:[],

        myTodoCntMap:{
            attendance_supplement: 0,
            attendance_overtime: 0,
            attendance_leave: 0,
            attendance_travel: 0,
            attendance_egress: 0,

            hr_join_apply: 0,
            hr_leave_apply: 0,
            hr_leave_handover: 0,
            hr_post_adjustment: 0,
            hr_mac_subsidy: 0,
            hr_visit_subsidy: 0,

            adm_seal_apply: 0,
            adm_card_apply: 0,
            adm_passenger_ticket: 0,

            attendance_cnt: 0,
            hr_cnt: 0,
            adm_cnt: 0
        },
        myTodoCnt:0,
    },

    filters:{
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        splitLine:function(value){
            if(isBlank(value)){
                return "";
            }
            return value.replace(new RegExp(',','g'), "<br/>");
        },
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        newLine:function(value){
            if(isBlank(value)){
                return "";
            }
            return value.replace(/\n/g, "<br/>");
        }
    },

    methods: {

        changeTab:function(businessType){
            vm.showList = true;

            vm.businessType = businessType;
            vm.gridDatas = [];

            loadGridData(vm.businessType);

        },

        toApproval:function(obj){
            var url = "";
            if(obj.businessType=="attendance_egress"){
                url = "../attendance/egressApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
            } else if(obj.businessType=="attendance_travel"){
                url = "../attendance/travelApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
            } else if(obj.businessType=="attendance_overtime"){
                url = "../attendance/overTimeApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
            } else if(obj.businessType=="attendance_leave"){
                url = "../attendance/leaveApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
            } else if(obj.businessType=="attendance_supplement"){
                url = "../attendance/supplementApproval.html?id="+obj.businessId+"&taskId="+obj.taskId;
            }

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
        this.businessType = "attendance_egress";

        loadGridData("attendance_egress");
    }
});
