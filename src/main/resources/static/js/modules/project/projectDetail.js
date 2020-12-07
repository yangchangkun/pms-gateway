
function loadDetail(id){
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });
    var url=baseURL + "app/pro/proDetails";

    $.ajax({
        type: "POST",
        url: url,
        data: {
            id:id
        },
        success: function (r) {
            layer.close(layerIndex);
            if(r.code == 0){
                vm.proBase = r.pro;
                vm.teamsDatas = r.teams;
                vm.wbsDatas = r.wbs;
                vm.changesDatas = r.changes;
                //项目附件信息
                vm.proAttachmentDatas = r.attas;
                //合同信息
                vm.pactDatas = r.pacts;

                vm.budgetCostDatas = r.budgetCosts;
                vm.budgetOtherDatas = r.budgetOthers;
                vm.budget = r.budget;
            }else{
                layer.open({
                    content:r.msg,
                    btn:'我知道了'
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
    })
}



var vm = new Vue({
    el:'#vueApp',
    data:{
        showList: true,
        title:null,
        query:{
            proKey:'',
            proType:'',
            createFromDay:'',
            createToDay:'',
            proAmountRange:'',
            proDepartId:'',
            proDepartName:'',
            proLeadName:'',
            proChiefName:'',
            proManagerName:'',
            proLifeCycle:''
        },
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
        //项目信息
        proBase:{
            proDudget: 0,
            proExpand:{
                id : '',
                proSerial : '',
                pactSerial : '',
                proPurpose : '',
                proUse : '',
                proMeaning : '',
                proScope : '',
                proObjective : '',
                proBackground : '',
                proActuality : '',
                proCompete : '',
                proRival : '',
                proCopyright : '',
                proFeasibility : '',
                proRoute : '',
                proImplement : '',
                proBuyList : '',
                proExpensesSoft : 0,
                proExpensesHardware : 0,
                proExpensesCultivate : 0
            }
        },
        //项目组成员
        teamsDatas:[],
        //项目组wbs
        wbsDatas:[],
        //项目变更历史
        changesDatas:[],
        //项目附件
        proAttachmentDatas:[],
        //项目合同信息
        pactDatas:[],

        //项目预算(人力)信息
        budgetCostDatas:[],
        //项目预算(其他)信息
        budgetOtherDatas:[],
        //项目预算汇总
        budget:0,

        /**
         * 当前操作的附件信息
         */
        attrInfo:{
            id:'',
            name: '',
            description: '',
            url: '',
            filePath: '',
            fileName: '',

            createUserId:'',
            createUserName:'',
            createTime:'',

            proId:'',
            canDeleteTag: '1'
        }
    },
    filters:{
        //价格万分位过滤器
        priceFormat:function(value){
            return priceSegmentation(value);
        },
        //价格转大写
        priceCvr:function(value){
            return priceConversion(value);
        }
    },
    methods: {
        init: function(){
            loadGridData("", "", "", "", "", "", "", "", "", "", 1);
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
        queryList: function (page) {
            vm.showList = true;
            loadGridData(vm.query.proKey, vm.query.proType,
                vm.query.createFromDay, vm.query.createToDay,
                vm.query.proAmountRange,
                vm.query.proDepartId, vm.query.proLeadName, vm.query.proChiefName, vm.query.proManagerName,
                vm.query.proLifeCycle,
                page);
        },
        toDetails: function(proId){
            vm.showList = false;
            vm.title = "指派项目经理";

            vm.user = {
                proManager:''
            };

            //获取项目基本信息
            this.getProBaseEntity(proId);

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
                    if(tag=="query.createFromDay"){
                        vm.query.createFromDay = day;
                    } else if(tag=="query.createToDay"){
                        vm.query.createToDay = day;
                    }

                    return true;
                }
            });
        },

        downloadAttachment: function (filePath, fileName){
            var fp = encodeURIComponent(encodeURIComponent(filePath));
            var fn = encodeURIComponent(encodeURIComponent(fileName));
            var action = baseURL + 'system/oss/downloadFile?token=' + globalToken + "&filePath="+fp+"&fileName="+fn;
            window.location.href= action;
        },

        goBack: function () {
            vm.showList = true;
        },
    },
    created: function(){
        // this.init();
        // this.getDeptTree();
    },
    updated: function(){

    },
    mounted: function () {
        var urlParams = getQueryString();
        printLog("urlParams="+JSON.stringify(urlParams));
        var id = urlParams.id;
        printLog("id="+id);

        loadDetail(id);
    }
});