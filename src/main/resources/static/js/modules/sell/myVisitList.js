
function loadGridData(params) {
    var layerIndex = layer.open({
        type: 2,
        content: '加载中'
    });
    $.ajax({
        type: "POST",
        contentType: "application/x-www-form-urlencoded",
        url: baseURL + "app/crmVisit/list",
        data:params,
        success: function (r) {
            layer.close(layerIndex);
            if (r.code == 0) {
                vm.gridDatas = r.gridDatas;
                vm.hasPermission = r.hasPermission;
                if(!vm.hasPermission){
                    layer.open({
                        content: '您尚未被赋予创建拜访记录的权限'
                        ,btn: ['我知道了']
                        ,yes: function(index){
                            layer.close(index);
                            onBackClick();
                        }
                    });
                }
            } else {
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

function initFileUpload(){
    new AjaxUpload('#upload', {
        action: baseURL + 'system/oss/upload?token=' + globalToken,
        name: 'file',
        data:{
            "resType":"project",
            "resKey":""
        },
        autoSubmit:true,
        responseType:"json",
        onSubmit:function(file, extension){
            /* if (!(extension && /^(jpg|jpeg|png|gif)$/.test(extension.toLowerCase()))){
                 alert('只支持jpg、png、gif格式的图片！');
                 return false;
             }*/
        },
        onComplete : function(file, r){
            if(r.code == 0){
                vm.attrInfo = r.oss;
                vm.attrInfo.createUserId = '';
                vm.attrInfo.createUserName = '';
                vm.attrInfo.createTime = '';
                vm.attrInfo.proId = vm.proBase.id;
                vm.attrInfo.canDeleteTag = '1';

                $('#modal-pro-attachment').modal('show');
            }else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        }
    });
}

var vm = new Vue({
    el:'#vueApp',
    data:{
        showList: true,
        title:null,
        hasPermission:false,
        webPage:{
            pageNum:0,
            pageSize:0,
            size:0,
            startRow:0,
            endRow:0,
            total:0,
            pages:0,
            prePage:0,
            nextPage:0,
            isFirstPage:false,
            isLastPage:false,
            hasPreviousPage:false,
            hasNextPage:false,
            navigatePages:'',
            navigatepageNums:[],
            navigateFirstPage:0,
            navigateLastPage:0
        },
        //列表信息
        gridDatas:[],
        type:'myList',
        query:{
            custShortName:'',
            createName:''
        },

    },
    filters:{

    },
    methods: {
        init: function(){
            var params = {
                custShortName:"",
                createName:"",
                type:"myList"
            };
            loadGridData(params);
        },
        emptyQuery:function(){
            vm.query.proKey = "";
            vm.query.proType = "";
            vm.query.createFromDay = "";
            vm.query.createToDay = "";
            vm.query.proAmountRange = "";
            vm.query.proDepartId = "";
            vm.query.proDepartName = "";
            vm.query.proLeadName = "";
            vm.query.proChiefName = "";
            vm.query.proManagerName = "";
            vm.query.proLifeCycle = "";
        },
        changeTab:function(type){
            vm.showList = true;
            vm.type = type;
            vm.gridDatas = [];
            var params = {
                custShortName:vm.query.custShortName,
                createName:vm.query.createName,
                type:vm.type
            };
            loadGridData(params);

        },
        toDetails: function(proId,visitState){
            if(visitState =='0'){
                // +"&visitState="+visitState
                var url = "../sell/myVisitApply.html?id="+proId;
                openWin(url);
            }else {
                var url = "../sell/myVisitDetail.html?id="+proId;
                openWin(url);
            }
        },
        //查询
        queryList: function(){
            var params = {
                custShortName:vm.query.custShortName,
                createName:vm.query.createName,
                type:vm.type
            };
            loadGridData(params);
        },
        toAdd: function(){
            var url = "../sell/myVisitApply.html";
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

            if(tag=="query.visitTime"){
                var iosSelect = new IosSelect(5,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '时间选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.query.visitTime = day;
                        },
                        fallback:function(){
                            vm.query.visitTime = "";
                        }
                    });
            }

        },

        goBack: function () {
            vm.showList = true;
        },
    },
    created: function(){
        this.init();
        // this.getDeptTree();
    },
    updated: function(){

    },
    mounted: function () {

        // loadGridData("", "", "", "", "", "", "", "", "", "", 1);
    }
});