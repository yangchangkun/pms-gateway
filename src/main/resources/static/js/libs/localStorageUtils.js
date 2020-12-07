/*
* 本地存储工具类
* 存储localStorage时候最好是封装一个自己的键值，在这个值里存储自己的内容对象，封装一个方法针对自己对象进行操作。避免冲突也会在开发中更方便。
*/
var myStorage = (function myStorage(){
    var ms = "myStorage";

    var storage = window.localStorage;
    /*var isAndroid = (/android/gi).test(window.navigator.appVersion);
    if (isAndroid){
        storage = os.localStorage();
    }*/
    if(!storage){
        alert("您的系统不支持localStorage");
        return false;
    }

    var set = function(key,value){
        //存储
        var mydata = storage.getItem(ms);
        if(!mydata){
            init();
            mydata = storage.getItem(ms);
        }
        mydata = JSON.parse(mydata);
        mydata.data[key] = value;
        storage.setItem(ms,JSON.stringify(mydata));
        return mydata.data;

    };

    var get = function(key){
        //读取
        var mydata = storage.getItem(ms);
        if(!mydata){
            return false;
        }
        mydata = JSON.parse(mydata);

        return mydata.data[key];
    };

    var remove = function(key){
        //读取
        var mydata = storage.getItem(ms);
        if(!mydata){
            return false;
        }

        mydata = JSON.parse(mydata);
        delete mydata.data[key];
        storage.setItem(ms,JSON.stringify(mydata));
        return mydata.data;
    };

    var clear = function(){
        //清除对象
        storage.removeItem(ms);
    };

    var init = function(){
        storage.setItem(ms,'{"data":{}}');
    };


    /**
     * 以下为具体的业务设置
     */
    var setToken = function(token) {
        set('globalToken',token);
    };

    var getToken = function(){
        return get("globalToken");
    };

    var setLoginAccount = function(account) {
        set('account',account);
    };

    var getLoginAccount = function(){
        return get("account");
    };

    var setPmsUserId = function(pmsUserId) {
        set('pmsUserId',pmsUserId);
    };

    var getPmsUserId = function(){
        return get("pmsUserId");
    };

    var setMemberId = function(memberId) {
        set('memberId',memberId);
    };

    var getMemberId = function(){
        return get("memberId");
    };

    var setStaff = function(staff) {
        set('staffCacheInfo',JSON.stringify(staff));
    };

    var getStaff = function(){
        return get("staffCacheInfo");
    };

    var setFinger = function(finger) {
        set('fingerPrint',finger);
    };

    var getFinger = function(){
        return get("fingerPrint");
    };


    /**
     * 信用付是否授权的标识
     * @param creditFlag
     */
    var setCreditPayFlag = function(creditFlag) {
        set('creditFlag',creditFlag);
    };

    var getCreditPayFlag = function(){
        return get("creditFlag");
    };

    return {
        set : set,
        get : get,
        remove : remove,
        init : init,
        clear : clear,

        setToken : setToken,
        getToken : getToken,
        setLoginAccount : setLoginAccount,
        getLoginAccount : getLoginAccount,
        setPmsUserId : setPmsUserId,
        getPmsUserId : getPmsUserId,
        setMemberId : setMemberId,
        getMemberId : getMemberId,
        setStaff : setStaff,
        getStaff : getStaff,
        setFinger : setFinger,
        getFinger : getFinger,
        setCreditPayFlag : setCreditPayFlag,
        getCreditPayFlag : getCreditPayFlag
    };

})();

