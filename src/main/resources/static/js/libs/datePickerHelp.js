/**
 * 初始化时间
 * @type {Date}
 */
var yckNow = new Date();
var yckNowYear = yckNow.getFullYear();
var yckNowMonth = yckNow.getMonth() + 1;
var yckNowDate = yckNow.getDate();

/**
 * 数据初始化
 * @param nowYear
 * @returns {Array}
 */
function yckFormatYear (nowYear) {
    var arr = [];
    for (var i = nowYear - 50; i <= nowYear + 5; i++) {
        arr.push({
            id: i + '',
            value: i + ''
        });
    }
    return arr;
}
function yckFormatMonth () {
    var arr = [];
    for (var i = 1; i <= 12; i++) {
    	var v = i;
    	if(v<10){
    		v = "0"+v;
		}
        arr.push({
            id: i + '',
            value: v + ''
        });
    }
    return arr;
}
function yckFormatDate (count) {
    var arr = [];
    for (var i = 1; i <= count; i++) {
        var v = i;
        if(v<10){
            v = "0"+v;
        }
        arr.push({
            id: i + '',
            value: v + ''
        });
    }
    return arr;
}


/**
 * 定义回调函数
 * @param callback
 */
var yckYearData = function(callback) {
    callback(yckFormatYear(yckNowYear))
}
var yckMonthData = function (year, callback) {
    callback(yckFormatMonth());
};
var yckDateData = function (year, month, callback) {
    if (/^(1|3|5|7|8|10|12)$/.test(month)) {
        callback(yckFormatDate(31));
    }
    else if (/^(4|6|9|11)$/.test(month)) {
        callback(yckFormatDate(30));
    }
    else if (/^2$/.test(month)) {
        if (year % 4 === 0 && year % 100 !==0 || year % 400 === 0) {
            callback(yckFormatDate(29));
        }
        else {
            callback(yckFormatDate(28));
        }
    }
    else {
        throw new Error('month is illegal');
    }
};

var yckHalfDayData = function(one, two, three, callback) {
    var hours = [];

    hours.push({
        id: 0,
        value: '上午'
    });
    hours.push({
        id: 1,
        value: '下午'
    });

    callback(hours);
};

var yckHourData = function(one, two, three, callback) {
    var hours = [];
    for (var i = 0,len = 24; i < len; i++) {
        var v = i;
        if(v<10){
            v = "0"+v;
        }

        hours.push({
            id: i,
            value: v + ''
        });
    }
    callback(hours);
};
var yckMinuteData = function(one, two, three, four, callback) {
    var minutes = [];
    for (var i = 0, len = 60; i < len; i++) {
        var v = i;
        if(v<10){
            v = "0"+v;
        }

        minutes.push({
            id: i,
            value: v + ''
        });
    }
    callback(minutes);
};
var yckSecondsData = function(one, two, three, four, five, callback) {
    var seconds = [];
    for (var i = 0, len = 60; i < len; i++) {
        var v = i;
        if(v<10){
            v = "0"+v;
        }

        seconds.push({
            id: i,
            value: v + ''
        });
    }
    callback(seconds);
};

/**
 * 班次使用的小时数，限定只能是半天
 * @param one
 * @param two
 * @param three
 * @param callback
 */
var yckDutyHourData = function(one, two, three, callback) {
    var hours = [];

    hours.push({
        id: 0,
        value: '09'
    });
    hours.push({
        id: 1,
        value: '14'
    });
    hours.push({
        id: 2,
        value: '18'
    });
    callback(hours);
};

