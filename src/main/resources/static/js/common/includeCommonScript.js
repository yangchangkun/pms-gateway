var baseURL = "/pms/";

//var globalJsVer = '202004032235';
var globalJsVer = new Date().getTime();

//document.write("");
document.write("<script src='"+baseURL+"js/framework/jquery.min.js'></script>");
document.write("<script src='"+baseURL+"bootstrap/js/bootstrap.min.js'></script>");
document.write("<script src='"+baseURL+"js/framework/vue.min.js'></script>");

document.write("<script src='"+baseURL+"hplus/plugins/layer/mobile/layer.js'></script>");
document.write("<script src='"+baseURL+"hplus/plugins/iosselect/iosSelect.js'></script>");

document.write("<script src='"+baseURL+"js/libs/ajaxupload.js'></script>");

document.write("<script src='"+baseURL+"js/libs/localStorageUtils.js?t=" + globalJsVer + "'></script>");
document.write("<script src='"+baseURL+"js/libs/function.js?t=" + globalJsVer + "'></script>");
document.write("<script src='"+baseURL+"js/libs/datePickerHelp.js?t=" + globalJsVer + "'></script>");

document.write("<script src='"+baseURL+"js/common/common.js?t=" + globalJsVer + "'></script>");






