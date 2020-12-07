//请求前缀
var baseURL = "/pms/";

//登录token
var globalToken = myStorage.getToken();
if(isBlank(globalToken)){
    var urlParams = getQueryString();
    var token = urlParams.token;
    printLog("地址参数里面的 token ======"+token);
    if(!isBlank(token)){
        globalToken = token;
    }
}
if(isBlank(globalToken)){
    parent.location.href = baseURL + 'login.html';
}


//jquery全局配置
$.ajaxSetup({
    dataType: "json",
    cache: false,
    xhrFields: {
        withCredentials: true
    },

    beforeSend: function (xhr) { //可以设置自定义标头
        printLog("ajaxSetup.beforeSend.token="+globalToken);
        xhr.setRequestHeader('token', globalToken);

    },
    complete: function(xhr) {
        //token过期，则跳转到登录页面
        if(typeof(xhr.responseJSON)!="undefined" && xhr.responseJSON.code == 401){
            exitSystem();
        }
    }
});

/**
 * 向框架内调转
 * @param url
 */
function toFrameForward(url){
    var target = baseURL+url;
    if(self==top){
        $("iframe[name='iframe0']").attr('src', url);
    } else {
        $("iframe[name='iframe0']",parent.document.body).attr("src",target)
    }
}

/**
 * 打开新的窗口
 * @param url
 */
function openWin(url){
    printLog("openWin.url="+url);
    var u = "";
    if(url.indexOf("?")!=-1){
        u = url + "&token="+globalToken;
    } else {
        u = url + "?token="+globalToken;
    }
    window.open(url);
}

/**
 * 该方法要结合webview进行判断
 * 关闭窗口
 */
function closeWin(){
    //window.close();
    window.location.href = "selfevent:close";
}

/**
 * 将返回的控制权交由系统的webView进行处理
 * 回退
 */
function onBackClick(){
    window.location.href = "selfevent:goback";
    /*try{
        window.pmsWebView.onBackJsFunctionCalled();
    } catch (e) {
        window.location.href = "selfevent:goback";
    }*/
    /**
     * 如果不希望页面刷新可以设置下面的参数
     */
    //location.reload(false);
}

/**
 * 打开图片预览的窗口
 * 打开图片
 * @param url
 */
function openImgPreviewWin(url){
    printLog("openImgPreviewWin.url="+url);
    //var u = baseURL+"/tool/imgPreview.html?imgUrl="+encodeURIComponent(url);
    //window.open(u);
    var webSite = "";
    if(url.indexOf('http')==0){

    } else {
        webSite = window.location.protocol+"//"+window.location.host;
    }
    window.location.href = "selfevent:openurl:"+encodeURIComponent(webSite + url);
}

/**
 * 打开图片预览的窗口
 * @param url
 */
function openBannerWin(url){
    printLog("openBannerWin.url="+url);
    window.location.href = "selfevent:openurl:"+encodeURIComponent(url);
}

/**
 * 跳转
 * @param url
 */
function globalForwardPage(url){
    var uri = "";
    if(url.indexOf("?")!=-1){
        uri = url + "&token="+globalToken;
    } else {
        uri = url + "?token="+globalToken;
    }
    window.location.href = uri;
}

/**
 * 返回上一页并刷新页面
 * @param type 0 返回并刷新，1 返回不刷新
 */
function globalGoBackPre(type){
    printLog("globalGoBackPre.type="+type);
    if (document.referrer) {
        printLog("globalGoBackPre.document.referrer="+document.referrer);
        if(type==0){
            printLog("globalGoBackPre.返回并刷新");
            window.location.href = document.referrer;
            //history.go(-1);location.reload();
        } else {
            printLog("globalGoBackPre.返回上一页");
            window.history.back();
        }
    } else {
        printLog("globalGoBackPre.selfevent:goback");
        /*window.opener = null;
        window.open('', '_self');
        window.close();*/
        window.location.href = "selfevent:goback";
    }
}

function exitSystem(){
    //删除本地myStorage
    myStorage.remove("globalToken");
    myStorage.remove("account");
    myStorage.remove("pmsUserId");
    myStorage.remove("memberId");
    myStorage.remove("staffCacheInfo");

    //跳转到登录页面
    parent.location.href = baseURL + 'login.html';
}

/**
 * 日志输出
 * @type {boolean}
 */
var isDebug = true; //是否输出日志
function printLog(log){
    if(isDebug){
        console.log(log);
    }
}