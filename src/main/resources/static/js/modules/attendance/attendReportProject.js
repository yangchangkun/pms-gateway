function loadReportData(day){
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    var params = {
        "day": day
    }

    var url = "app/attend/report/attendReportProject";
    $.ajax({
        url: baseURL + url,
        type: "POST",
        contentType: "application/x-www-form-urlencoded",
        data: params,
        success: function(r){
            layer.close(layerIndex);

            if(r.code == 0){
                vm.day = r.day;
                vm.proAttendList = r.proAttendList;

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

        proAttendList:[]

    },
    methods: {

        changeTab:function(tabType){

            var url = "";
            if(tabType=="dept"){
                url = "../attendance/attendReportDept.html?day=&rootDeptId=";
                openWin(url);
            } else if(tabType=="project"){

            } else if(tabType=="mine"){
                url = "../attendance/attendReport.html";
                openWin(url);
            }
        },

        toAttendReportProTeam:function(proId, type){
            url = "../attendance/attendReportProTeam.html?day="+vm.day+"&proId="+proId+"&type="+type;
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
                            loadReportData(vm.day);
                        },
                        fallback:function(){
                            vm.day = currentDate();
                            loadReportData(vm.day);
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
        var day = currentDate();
        this.day = day;

        loadReportData(day);

    }
});


function toAttendReportProTeam(proId, type){
    url = "../attendance/attendReportProTeam.html?day="+vm.day+"&proId="+proId+"&type="+type;
    openWin(url);
}

function fillAttendChart(){
    var attendChartContainer = $("#attendChartContainer");
    $("#attendChartContainer").empty()

    if(vm.proAttendList!=null && vm.proAttendList.length>0){
        for(var i=0;i<vm.proAttendList.length;i++){
            var obj = vm.proAttendList[i];

            var totalStaffCount = obj.shouldSignStaffCount;//应到人数
            var signStaffCount = obj.signStaffCount;//打卡人数
            var otherStaffCount = totalStaffCount - signStaffCount;

            var chartEleId = "chart"+i;
            var chartJqId = "#chart"+i;

            var ele  = " <div class=\"panel panel-default\" style=\"margin-bottom: 0px;\">"
                +" <div class=\"panel-heading\"><i class=\"fa fa-file-text\"></i> "+obj.proName+"</div>"
                +" <div class=\"panel-body\" >"

                +" <div class=\"row\" style=\"margin-top: 10px;\">"
                +" <div class=\"col-xs-12\">"
                /*+" <div class=\"text-center\" style=\"font-size: 50px;color: #4297e6;font-weight: bold;text-decoration:underline;\" @click=\"toAttendReportProTeam(obj.proId, '0')\">"
                + obj.signStaffCount +"/"+ obj.shouldSignStaffCount
                +" </div>"*/

                +" <div id='"+chartEleId+"' style='width: 100%;height: 320px;'></div>"
                
                +" <div class=\"row\" style=\"margin-top: 10px;\">" +
                "        <div class=\"col-xs-12\">" +
                "            <div class=\"text-center\" @click=\"toAttendReportDeptDetail('0')\">" +
                "                <span>" +
                "                    <span style=\"font-size: 14px;color: #bcbcbc;\">打卡人数/应到人数</span>" +
                "                    &nbsp;" +
                "                    <span style=\"font-size: 18px;font-weight: bold;\">"+signStaffCount+"/"+totalStaffCount+"</span>" +
                "                </span>" +
                "            </div>" +
                "        </div>" +
                "    </div>"
                
                +" </div>"
                +" </div>"
                +" <div class=\"row\" style=\"margin-top: 10px;\">"
                +" <div class=\"col-xs-4\">"
                +" <div class=\"btn btn-block btn-trans\" style=\"background: #F2F7FF; color: #1A8AFA;\" onclick=\"toAttendReportProTeam('"+obj.proId+"', '1')\">出差("+obj.tripStaffCount+")</div>"
                +" </div>"
                +" <div class=\"col-xs-4\">"
                +" <div class=\"btn btn-block btn-trans\" style=\"background: #E1FFFF; color: #10C2C1;\"  onclick=\"toAttendReportProTeam('"+obj.proId+"', '2')\">请假("+obj.leaveStaffCount+")</div>"
                +" </div>"
                +" <div class=\"col-xs-4\">"
                +" <div class=\"btn btn-block btn-trans\" style=\"background: #FFF9EC; color: #FFA800;\" onclick=\"toAttendReportProTeam('"+obj.proId+"', '3')\">外出("+obj.egressStaffCount+")</div>"
                +" </div>"
                +" </div>"
                +" <div class=\"row\" style=\"margin-top: 10px;\">"
                +" <div class=\"col-xs-4\">"
                +" <div class=\"btn btn-block btn-trans\" style=\"background: #FFF9EC; color: #FFA800;\" onclick=\"toAttendReportProTeam('"+obj.proId+"', '4')\">外勤("+obj.outsideStaffCount+")</div>"
                +" </div>"
                +" <div class=\"col-xs-4\">"
                +" <div class=\"btn btn-block btn-trans\" style=\"background: #FBF4F5; color: #F75251;\" onclick=\"toAttendReportProTeam('"+obj.proId+"', '5')\">迟到("+obj.lateStaffCount+")</div>"
                +" </div>"
                +" <div class=\"col-xs-4\">"
                +" <div class=\"btn btn-block btn-trans\" style=\"background: #F8F8F9; color: #686868;\" onclick=\"toAttendReportProTeam('"+obj.proId+"', '7')\">未打卡("+obj.withoutSignStaffCount+")</div>"
                +" </div>"
                +" </div>"
                +" </div>"
                +" </div>"

            attendChartContainer.append(ele);


            var myChart = echarts.init(document.getElementById(chartEleId));

            option = {
                title: {
                    text: '',
                    left: 'center'
                },
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
    } else {
        var ele = "<div class=\"text-center\"><h4>暂无您主导的项目</h4></div>";
        attendChartContainer.append(ele);
    }


}


