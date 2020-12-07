function loadReportData(userId, year, month){
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    var params = {
        "userId": userId,
        "month": year+"-"+month,
    }

    var url = "app/attend/report/summary";
    $.ajax({
        url: baseURL + url,
        type: "POST",
        contentType: "application/x-www-form-urlencoded",
        data: params,
        success: function(r){
            layer.close(layerIndex);

            if(r.code == 0){
                vm.userEntity = r.userEntity;
                vm.shouldDays = r.shouldDays; //应出勤天数
                vm.actualDays = r.actualDays; //实际出勤天数
                vm.qqCount = r.qqCount; //缺勤次数
                vm.qkCount = r.qkCount; //缺卡次数
                vm.cdCount = r.cdCount; //迟到次数
                vm.kgCount = r.kgCount; //旷工次数
                vm.ztCount = r.ztCount; //早退次数
                vm.egressHours = r.egressHours; //外出时长（小时）
                vm.travelDays = r.travelDays; //出差天数（天）
                vm.leaveDays = r.leaveDays; //请假天数（天）
                vm.overtimeHours = r.overtimeHours; //加班时间（小时）
                vm.bkCount = r.bkCount;//补卡次数
                vm.attendSnapshotList = r.attendSnapshotList; //日考勤快照
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

        userId:'',
        year:'',
        month:'',

        userEntity:{
            userId:'',
            account:'',
            userName:''
        },

        shouldDays:0, //应出勤天数
        actualDays:0, //实际出勤天数
        qqCount:0, //缺勤次数
        qkCount:0, //缺卡次数
        cdCount:0, //迟到次数
        kgCount:0, //旷工次数
        ztCount:0, //早退次数
        egressHours:0, //外出时长（小时）
        travelDays:0, //出差天数（天）
        leaveDays:0, //请假天数（天）
        overtimeHours:0, //加班时间（小时）
        bkCount:0,//补卡次数
        attendSnapshotList:[], //日考勤快照
    },
    methods: {

        preMonth: function(){
            var m = parseInt(vm.month.toString());
            if(m==1){
                vm.year = vm.year-1;
                vm.month = 12;
            } else {
                vm.month = m-1;
            }
            if(vm.month<10){
                vm.month = "0"+vm.month;
            }


            loadReportData(vm.userId, vm.year, vm.month);
        },

        nextMonth: function(){
            var m = parseInt(vm.month.toString());
            if(m==12){
                vm.year = vm.year+1;
                vm.month = 1;
            } else {
                vm.month = m+1;
            }
            if(vm.month<10){
                vm.month = "0"+vm.month;
            }
            loadReportData(vm.userId, vm.year, vm.month);
        },

        photoHandler:function(photo){
            if(photo==null){
                return "../img/me_avatar.png";
            }
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }
            return photo;
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
        var urlParams = getQueryString();
        printLog("urlParams="+JSON.stringify(urlParams));

        var userId = urlParams.userId;
        this.userId = userId;
        printLog("userId="+userId);

        var year = urlParams.year;
        if(isBlank(year)){
            year = currentYear();
        }
        this.year = year;
        printLog("year="+year);

        var month = urlParams.month;
        if(isBlank(month)){
            month = currentMonth();
        }
        this.month = month;
        printLog("month="+month);

        loadReportData(userId, year, month);

    }
});

