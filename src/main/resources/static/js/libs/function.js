function IsNotNull(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		return true;
	}else{
		return false;
	}
}

//判断是否为空
function isBlank(value) {
    //return !value || !/\S/.test(value)
    /*if (!value && typeof(value)!="undefined" && value!=0){
        return true;
    }
    if(typeof(value)!="undefined" && value.toString().length<=0){
        return true;
    }
    return false;*/

    if(typeof (value) =='undefined'){
        return true;
    }
    if (!value && typeof(value)!='undefined' && value!=0)
    {
        return true;
    }
    if(value.trim().length<=0){
        return true;
    }
    return false;
}

function IsDate(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		var reg = /^(\d{1,4})(-|\/)(\d{1,2})\2(\d{1,2})$/;
		var r = test_v.match(reg);
		if (r == null) {
			return false;
		}else{
			return true;
		}
	}else{
		return false;
	}
}     
    
function IsDateTime(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		var reg = /^(\d{1,4})(-|\/)(\d{1,2})\2(\d{1,2}) (\d{1,2}):(\d{1,2}):(\d{1,2})$/;
		var r = test_v.match(reg);
		if (r == null) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     
    
function IsTime(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /^((20|21|22|23|[0-1]\d)\:[0-5][0-9])(\:[0-5][0-9])?$/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     
   
function IsDigital(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		// reg = /^[0-9]+$/;
		reg=/^\d{8}$/
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}

function IsLetter(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /^[a-zA-Z]+$/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     
 
function IsInteger(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /^[-+]?\d*$/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     
   
function IsDouble(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		//reg = /^[-\+]?\d+(\.\d+)?$/;
		reg = /^(0|[1-9]\d*)\.(\d+)$/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     
 
function IsPrice(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		if(IsInteger(test_v)){
			return true;
		}else{
			return IsDouble(test_v);
		}
	}else{
		return false;
	}
}     

function IsString(str) {
	if(typeof(str) == "undefined"){
		return false
	}
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /^[a-zA-Z0-9_-]+$/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     
  
function IsChinese(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /^[\u0391-\uFFE5]+$/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     
  
function IsEmail(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     

function isMobile(str) {    
	var reg0 = /^13\d{5,9}$/;   
	var reg1 = /^15\d{5,9}$/;
    var reg2 = /^17\d{5,9}$/;
    var reg3 = /^18\d{5,9}$/;
	var my = false;   
	if (reg0.test(str))my=true;   
	if (reg1.test(str))my=true;   
	if (reg2.test(str))my=true;
    if (reg3.test(str))my=true;
    if (!my){   
       return false;
    }else{
    	return true;
    }  
}  

function IsZIP(str) {
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /^\d{6}$/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}     

function IsIP(str){
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /(^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])(\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])){3}$)/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}

//比例
function IsScale(str){
	var test_v = str.trim();
	if (test_v.length != 0) {
		reg = /\d+\:\d+/;
		if (!reg.test(test_v)) {
			return false;
		}else{
			return true;
		} 
	}else{
		return false;
	}
}

/**
 * 将金额按万分位分割
 * @param n
 * @returns {string}
 */
function priceSegmentation(value){
    if (!value && typeof(value)!="undefined" && value!=0){
        return "0";
    }
    if(value.toString().length<=0){
        return "0";
    }

    var _number = value.toString();        // 数字转成字符串
    var _decimal = '';
    if(_number.indexOf(".")!=-1){
        _number = value.toString().split(".")[0];
        _decimal = value.toString().split(".")[1];
    }
    var result = '';            // 转换后的字符串
    var counter = '';
    for (var i = _number.length - 1; i >= 0; i--) {
        counter++;
        result = _number.charAt(i) + result;
        if (!(counter % 3) && i != 0) {
            result = ',' + result;
        }
    }
    if(_decimal!=''){
        result+="."+_decimal;
    }
    return result;
}

/**
 * 将金额小写转换为大写
 * @param n
 * @returns {*}
 * @constructor
 */
function priceConversion(n){
    if (!/^(0|[1-9]\d*)(\.\d+)?$/.test(n)){
        return "";
    }
    var unit = "仟佰拾亿仟佰拾万仟佰拾元角分", str = "";
    n += "00";
    var p = n.indexOf('.');
    if (p >= 0){
        n = n.substring(0, p) + n.substr(p+1, 2);
    }
    unit = unit.substr(unit.length - n.length);
    for (var i=0; i < n.length; i++){
        str += '零壹贰叁肆伍陆柒捌玖'.charAt(n.charAt(i)) + unit.charAt(i);
    }
    return str.replace(/零(仟|佰|拾|角)/g, "零").replace(/(零)+/g, "零").replace(/零(万|亿|元)/g, "$1").replace(/(亿)万|壹(拾)/g, "$1$2").replace(/^元零?|零分/g, "").replace(/元$/g, "元整");
}

/**
 * 生成指定长度的随机数
 * @param len
 */
function getRandom(len) {
    var result = [];
    for(var i =0;i<len;i++){//循环len次
        var num = Math.random()*9;//Math.random();每次生成(0-1)之间的数;
        num = parseInt(num,10);
        result.push(num);
    }
    return result.join("-");
}

function currentDate(){
	var myD = new Date();
	var y = myD.getFullYear();
	var m = (myD.getMonth() + 1);
	if(m<10){
		m="0"+m;
	}
	var d = myD.getDate();
	if(d<10){
		d="0"+d;
	}
	return y+"-"+m+"-"+d;
}

function currentTime(){
    var myD = new Date();
    var h = myD.getHours();
    var m = myD.getMinutes();
    var s = myD.getSeconds();
    if(h<10){
        h="0"+h;
    }
    if(m<10){
        m="0"+m;
    }
    if(s<10){
        s="0"+s;
    }

    return h+":"+m+":"+s;
}

function currentYear(){
    var myD = new Date();
    var y = myD.getFullYear();
    return y;
}

function currentMonth(){
    var myD = new Date();
    var m = (myD.getMonth() + 1);
    if(m<10){
        m="0"+m;
    }
    return m;
}

function currentWeek(){
    var myD = new Date();
    var myDay = myD.getDay();//获取存储当前日期
    var weekDays = ["星期日","星期一","星期二","星期三","星期四","星期五","星期六"];
    return weekDays[myDay];
}

/**
 * 判断开始日期(字符串)是否大于结束日期(字符串)的大小
 * @param startDate 格式：yyyy-MM-dd
 * @param endDate 格式：yyyy-MM-dd
 */
function compareDate(startDate, endDate) {
	if(!startDate || !/\S/.test(startDate)){
        return true;
	}
    if(!endDate || !/\S/.test(endDate)){
        return true;
    }

    var d1 = new Date(startDate.replace(/\-/g, "\/"));
    var d2 = new Date(endDate.replace(/\-/g, "\/"));
    if(d1 > d2){
		return true;
	}
	return false;
}

/**
 * 判断两个日期之间相差的天数
 * @param sDate1 格式：yyyy-MM-dd
 * @param sDate2 格式：yyyy-MM-dd
 */
function  dateDiff(sDate1,  sDate2){
    var  startDate=Date.parse(sDate1.replace(/\-/g, "\/"));
    var  endDate=Date.parse(sDate2.replace(/\-/g, "\/"));
    var diffDate=(endDate-startDate)+1*24*60*60*1000;
    var days=diffDate/(1*24*60*60*1000);
    return  parseInt(days);
}

/**
 * 判断两个日期之间相差的月数
 * @param sDate1 格式：yyyy-MM-dd
 * @param sDate2 格式：yyyy-MM-dd
 */
function  monthDiff(sDate1,  sDate2){
    var startDate=new Date(sDate1.replace(/\-/g, "\/"));
    var endDate=new Date(sDate2.replace(/\-/g, "\/"));

    var m = (endDate.getFullYear()-startDate.getFullYear())*12+endDate.getMonth()-startDate.getMonth();

    return  m;
}

// 日期，在原有日期基础上，增加days天数，默认增加1天
function addDate(date, days) {
    if (days == undefined || days == '') {
        days = 1;
    }
    var date = new Date(date);
    date.setDate(date.getDate() + days);
    var month = date.getMonth() + 1;
    var day = date.getDate();
    return date.getFullYear() + '-' + getFormatDate(month) + '-' + getFormatDate(day);
}

// 日期月份/天的显示，如果是1位数，则在前面加上'0'
function getFormatDate(arg) {
    if (arg == undefined || arg == '') {
        return '';
    }

    var re = arg + '';
    if (re.length < 2) {
        re = '0' + re;
    }

    return re;
}

/**
 * 深度克隆
 * @param obj
 * @returns {*}
 */
function deepClone(obj) {
    var result;
    var oClass=isClass(obj);
    if(oClass==="Object"){
        result={};
    }else if(oClass==="Array"){
        result=[];
    }else{
        return obj;
    }
    for(var key in obj){
        var copy=obj[key];
        if(isClass(copy)=="Object"){
            result[key]=arguments.callee(copy);//递归调用
        }else if(isClass(copy)=="Array"){
            result[key]=arguments.callee(copy);
        }else{
            result[key]=obj[key];
        }
    }
    return result;
}

//判断对象的数据类型
function isClass(o){
    if(o===null) return "Null";
    if(o===undefined) return "Undefined";
    return Object.prototype.toString.call(o).slice(8,-1);
}

/**
 * 从url里面分析出对应的参数
 * @param name
 * @returns {string}
 */
function getQueryString() {
    var queryObj={};
    var reg=/[?&]([^=&#]+)=([^&#]*)/g;
    var querys=window.location.search.match(reg);
    if(querys){
        for(var i in querys){
            var query=querys[i].split('=');
            var key=query[0].substr(1),
                value=query[1];
            queryObj[key]?queryObj[key]=[].concat(queryObj[key],value):queryObj[key]=value;
        }
    }
    return queryObj;

}

String.prototype.trim = function () {
	return this.replace(/(^\s*)|(\s*$)/g, "");
};


function back(step){
	if(step){
		window.history.go(step);
	}else{
		window.history.go(-1);
	}
}

function saveExcel(){
	document.execCommand('SaveAs',false,'szty_hll_stat.xls');
}

function printWeb(){
	window.print();
}

