function loadReportData(day, rootDeptId){
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    var params = {
        "day": day,
        "rootDeptId": rootDeptId,
    }

    var url = "app/attend/report/attendReportDept";
    $.ajax({
        url: baseURL + url,
        type: "POST",
        contentType: "application/x-www-form-urlencoded",
        data: params,
        success: function(r){
            layer.close(layerIndex);

            if(r.code == 0){
                vm.day = r.day; //应出勤天数
                vm.attendCondition = r.attendCondition; //实际出勤情况

                fillAttendChart();
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

        day:'',
        rootDeptId:'',

        attendCondition:{
            rootDept:{
                deptId:"",
                parentId:'',
                ancestors:'',
                deptName:''
            },
            totalAttendMap:{
                totalAllStaffCount : '0', //汇总 全部职员
                totalLeaveStaffCount : '0',//汇总 请假职员数量
                totalTripStaffCount : '0',//汇总 出差职员数量
                totalEgressStaffCount : '0',//汇总 外出职员数量
                totalShouldSignStaffCount : '0',//汇总 应打卡职员数量（除请假和外出之外的所有人）
                totalOutsideStaffCount : '0',//汇总 外勤打卡职员数量
                totalSignStaffCount : '0',//汇总 已打卡职员数量（排除请假和外出之外的有打卡记录的职员）
                totalLateStaffCount : '0',//汇总 迟到职员数量
                totalLackStaffCount : '0',//汇总 旷工职员数量
                totalWithoutSignStaffCount : '0',//汇总 未签到职员数量（全部-已打卡-请假-外出）
            },
            deptAttendList:[]
        }

    },
    methods: {

        changeTab:function(tabType){

            var url = "";
            if(tabType=="dept"){

            } else if(tabType=="project"){
                url = "../attendance/attendReportProject.html";
                openWin(url);
            } else if(tabType=="mine"){
                url = "../attendance/attendReport.html";
                openWin(url);
            }
        },

        onDeptBackClick:function(){
            printLog("onDeptBackClick.rootDeptId="+vm.rootDeptId);
            if(vm.rootDeptId==""){
                printLog("onDeptBackClick.closeWin==========");
                closeWin();
            } else {
                printLog("onDeptBackClick.onBackClick==========");
                onBackClick();
            }
        },

        toDeptAttend:function(deptId){
            if(deptId!=vm.rootDeptId){
                var url = "../attendance/attendReportDept.html?day="+vm.day+"&rootDeptId="+deptId;
                openWin(url);
            }
        },

        toAttendReportDeptDetail:function(type){
            var url = "../attendance/attendReportDeptDetail.html?day="+vm.day+"&deptId="+vm.rootDeptId+"&type="+type;
            openWin(url);
        },

        /**
         * 跳转到考勤统计查询界面
         */
        toAttendReportSearch:function(){
            var url = "../attendance/attendReportSearch.html";
            openWin(url);
        },

        /**
         * 日期和时间控件选择器与vue.data绑定处理
         * @param ele 当前操作的dom对象
         * @param fmt 时间格式化，eg.yyyy-MM-dd or H:mm:ss
         * @param tag 标记，该参数用来标记日期选择器选择的值要赋值给vue.data里面的具体的值，逻辑需要编写
         * @param idx 拓展参数，如果要赋值的vue.data 是一个Array，则这个参数就是下标
         */
        datePickerChange: function(ele, fmt, tag, idx){
            var objEle = ele.target || ele.srcElement; //获取document 对象的引用
            var eleId = objEle.id;

            if(tag=="day"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '日期选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.day = day;
                            loadReportData(vm.day, vm.rootDeptId);
                        },
                        fallback:function(){
                            vm.day = currentDate();
                            loadReportData(vm.day, vm.rootDeptId);
                        }
                    });
            }

        },

    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        var urlParams = getQueryString();
        printLog("urlParams="+JSON.stringify(urlParams));
        var day = urlParams.day;
        if(isBlank(day)){
            day = currentDate();
        }
        this.day = day;
        printLog("day="+day);

        var rootDeptId = urlParams.rootDeptId;
        this.rootDeptId = rootDeptId;
        printLog("rootDeptId="+rootDeptId);


        loadReportData(day, rootDeptId);

    }
});


function fillAttendChart(){
    var totalStaffCount = vm.attendCondition.totalAttendMap.totalShouldSignStaffCount;//应到人数
    var signStaffCount = vm.attendCondition.totalAttendMap.totalSignStaffCount;//打卡人数
    var otherStaffCount = totalStaffCount - signStaffCount;

    var myChart = echarts.init(document.getElementById("attendChartContainer"));

    option = {

        tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b} : {c} ({d}%)'
        },
        legend: {
            orient: 'vertical',
            left: 'left',
            data: ['其它情况人数', '打卡人数']
        },
        series: [
            {
                name: '人数',
                type: 'pie',
                radius: ['40%', '60%'],
                data: [
                    {value: signStaffCount, name: '打卡人数'},
                    {value: otherStaffCount, name: '其它情况人数'}
                ]
            }
        ]
    };

    // 使用刚指定的配置项和数据显示图表。
    myChart.setOption(option);


}

