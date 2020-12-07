/**
 * 加载日历
 * @param year
 * @param month
 */
function loadCalendarGridData(year, month){
    removeCalendarLayout();

    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    var param = {
        'year': year,
        'month':month
    };

    $.ajax({
        type: "POST",
        url: baseURL + "app/daily/apply/calendars",
        contentType: "application/x-www-form-urlencoded",
        data: param,
        success: function (r) {
            vm.ajaxLock = false;
            layer.close(layerIndex);

            if (r.code === 0) {
                var calendarGridDatas = JSON.parse(r.list);
                appendCalendarLayout(calendarGridDatas);
            }

        },
        error: function (data) {
            vm.ajaxLock = false;
            layer.close(layerIndex);
        }
    });

}

/**
 * 清空日历内容
 */
function removeCalendarLayout(){
    $('#calendarTbody').empty();
}

/**
 * 组装日历
 * @param calendarGridDatas
 */
function appendCalendarLayout(calendarGridDatas){
    removeCalendarLayout();
    if(calendarGridDatas!=null && calendarGridDatas.length>0){
        for(var i=0;i<calendarGridDatas.length;i++){
            var jo = calendarGridDatas[i];
            var weekJArr = jo.week;

            var _html = "<tr>";
            for(var j=0;j<weekJArr.length;j++){
                var dayJo = weekJArr[j];

                var _year = dayJo.year;
                var _month = dayJo.month;
                var _day = dayJo.day;
                var _date = dayJo.date;
                var _display = dayJo.display;//是否显示日期，0 不显示，1显示
                var _holiday = dayJo.holiday;//是否是假日(周末)，0是，1否
                var _state = dayJo.state; //-1未报工，0：审核中，1：驳回，2：确认

                var cssStr = "day-td";
                if(_display==0){

                } else if(_holiday==0){
                    cssStr += " day-td-holiday";
                } else {
                    if(_state==-1){
                        cssStr += " cursorCss day-td-status-def";
                    } else if(_state==0){
                        cssStr += " cursorCss day-td-status-0";
                    } else if(_state==1){
                        cssStr += " cursorCss day-td-status-1";
                    } else if(_state==2){
                        cssStr += " cursorCss day-td-status-2";
                    }
                }

                if(_display==0){
                    _html += "<td class='"+cssStr+"'></td>";
                } else if(_holiday==0){
                    _html += "<td class='"+cssStr+"'>"+_day+"</td>";
                } else {
                    _html += "<td class='"+cssStr+"' onclick='toDailyOperate(\""+_date+"\")'>"+_day+"</td>";
                }
            }
            _html += "</tr>";
            $('#calendarTbody').append(_html);
        }
    }

}

/**
 * 查询日报
 * @param day
 */
function toDailyOperate(day){
    vm.day = day;
    var url = "../daily/dailyOperate.html?day="+day;
    openWin(url);
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

        year:'',
        month:'',
        day:'', //当前选中的日期

    },
    methods: {

        init: function(){
            loadCalendarGridData(currentYear(), currentMonth());
        },

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
            loadCalendarGridData(vm.year, vm.month);
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
            loadCalendarGridData(vm.year, vm.month);
        },


        validator: function () {

            return false;
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
            WdatePicker({
                el: eleId,
                dateFmt:fmt,
                onpicked:function(){
                    var day = $dp.cal.getDateStr(fmt);
                    if(tag=="staff.birthday"){
                        vm.staff.birthday = day;
                    }

                    return true;
                },
                oncleared:function(){
                    if(tag=="staff.birthday"){
                        vm.staff.birthday = "";
                    }

                    return true;
                }
            });
        },

    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        this.year = currentYear();
        this.month = currentMonth();
        this.day = currentDate();

        this.init();
    }
});

