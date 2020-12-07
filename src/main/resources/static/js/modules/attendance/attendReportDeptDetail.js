/**
 * 查询部门下成员的考勤情况
 * @param day
 * @param deptId
 * @param type 查询类型 0 全部， 1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；
 */
function loadReportData(day, deptId, type){
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    var params = {
        "day": day,
        "deptId": deptId,
    }

    var url = "app/attend/report/attendReportDeptDetail";
    $.ajax({
        url: baseURL + url,
        type: "POST",
        contentType: "application/x-www-form-urlencoded",
        data: params,
        success: function(r){
            layer.close(layerIndex);

            if(r.code == 0){
                vm.day = r.day;
                var attendCondition = r.attendCondition;
                printLog("attendCondition="+JSON.stringify(attendCondition))
                vm.rootDept = attendCondition.rootDept;
                if(type=='0'){
                   vm.staffAttendList = attendCondition.staffAttendList;
                } else {
                    if(attendCondition.staffAttendList!=null && attendCondition.staffAttendList.length>0){
                        for(var i=0;i<attendCondition.staffAttendList.length;i++){
                            var staffAttend = attendCondition.staffAttendList[i];
                            if(type=='1'){
                                //1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；
                                if(!isBlank(staffAttend.travelTag)){
                                    vm.staffAttendList.push(staffAttend);
                                }
                            } else if(type=='2'){
                                //1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；
                                if(!isBlank(staffAttend.leaveTag)){
                                    vm.staffAttendList.push(staffAttend);
                                }
                            } else if(type=='3'){
                                //1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；
                                if(!isBlank(staffAttend.egressTag)){
                                    vm.staffAttendList.push(staffAttend);
                                }
                            } else if(type=='4'){
                                //1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；
                                if(!isBlank(staffAttend.dkTag) && staffAttend.addrCnt=='0'){
                                    vm.staffAttendList.push(staffAttend);
                                }
                            } else if(type=='5'){
                                //1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；
                                if(!isBlank(staffAttend.dkTag) && staffAttend.signInType=='1'){
                                    vm.staffAttendList.push(staffAttend);
                                }
                            } else if(type=='6'){
                                //1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；
                                if(!isBlank(staffAttend.dkTag) && staffAttend.signInType=='3'){
                                    vm.staffAttendList.push(staffAttend);
                                }
                            } else if(type=='7'){
                                //1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；
                                if(isBlank(staffAttend.travelTag) && isBlank(staffAttend.egressTag) && isBlank(staffAttend.dkTag)){
                                    vm.staffAttendList.push(staffAttend);
                                }
                            }
                        }
                    }

                }
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
        deptId:'',
        type:'', //查询类型 0 全部， 1：出差；2：请假；3：外出；4：外勤；5：迟到；6：旷工；7：未打卡；

        rootDept:{
            deptId:"",
            parentId:'',
            ancestors:'',
            deptName:''
        },
        staffAttendList:[]


    },
    methods: {

        toStaffAttendDetail:function(userId){
            var date = vm.day.split("-");
            url = "../attendance/attendReportStaff.html?userId="+userId+"&year="+date[0]+"&month="+date[1];
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
                            loadReportData(vm.day, vm.deptId);
                        },
                        fallback:function(){
                            vm.day = currentDate();
                            loadReportData(vm.day, vm.deptId);
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
        this.day = day;
        printLog("day="+day);
        var deptId = urlParams.deptId;
        this.deptId = deptId;
        printLog("deptId="+deptId);
        var type = urlParams.type;
        this.type = type;
        printLog("type="+type);

        loadReportData(day, deptId, type);

    }
});

