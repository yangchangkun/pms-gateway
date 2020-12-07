/**
 * 改变容器高度的脚本
 * 适用于列表页面，让列表占据屏幕剩余可用高度的情形
 */
function setContainerHeight(){
    var browserH = $(window).height(); //浏览器当前窗口可视区域高度
    var searchDivH = $("#container_search").height();
    if(searchDivH==null){
        searchDivH = 0;
    }
    var gridDivH = $("#container_grid").height();
    if(gridDivH==null){
        gridDivH = 0;
    }
    var pageDivH = $("#container_page").height();
    if(pageDivH==null){
        pageDivH = 0;
    }
    var gridMaxH = browserH-searchDivH-pageDivH-25;
    $("#container_grid").height(gridMaxH);
};

window.onload=function(){
    setContainerHeight();
};
//当浏览器窗口大小改变时，设置显示内容的高度
window.onresize=function(){
    setContainerHeight();
};