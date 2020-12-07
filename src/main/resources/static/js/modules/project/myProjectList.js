
function loadGridData(proKey, proType, createFromDay, createToDay, proAmountRange, proDepartId, proLeadName, proChiefName, proManagerName, proLifeCycle, page) {
    var layerIndex = layer.open({
        type: 2,
        content: '加载中'
    });
    $.ajax({
        type: "POST",
        contentType: "application/x-www-form-urlencoded",
        url: baseURL + "app/pro/myProject",
        data: {
            'proKey': proKey,
            'proType': proType,
            'createFromDay': createFromDay,
            'createToDay': createToDay,
            'proAmountRange': proAmountRange,
            'proDepartId': proDepartId,
            'proLeadName': proLeadName,
            'proChiefName': proChiefName,
            'proManagerName': proManagerName,
            'proLifeCycle': proLifeCycle,
            'page': page
        },
        success: function (r) {
            layer.close(layerIndex);
            if (r.code == 0) {
                vm.gridDatas = r.list;
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
/**
 * 部门选择所需参数
 */
// var setting = {
//     data: {
//         simpleData: {
//             enable: true,
//             idKey: "deptId",
//             pIdKey: "parentId",
//             rootPId: ""
//         },
//         key: {
//             name : 'deptName',
//             url:"nourl"
//         }
//     }
// };
// var ztree;


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
            var url = "../project/projectDetail.html?id="+proId;
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

        //保存附件
        saveAttachment: function () {
            var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });

            var url = "pro/change/supplementProAttachment";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.attrInfo),
                success: function(r){
                    layer.close(layerIndex);
                    if(r.code == 0){
                        vm.proAttachmentDatas.push(r.atta);
                        $('#modal-pro-attachment').modal('hide');
                    }else{
                        layer.open({
                            content: r.msg,
                            btn: '我知道了'
                        });
                    }
                }
            });
        },
        //移除附件
        removeAttachment: function (nodeIndex){
            var atta = vm.proAttachmentDatas[nodeIndex];
            layer.confirm('确定要删除该附件吗？删除后不可找回', function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "pro/change/deleteProAttachment",
                    data: "id=" + atta.id,
                    success: function(r){
                        if(r.code === 0){
                            layer.open({
                                content: '操作成功',
                                function(index){
                                    layer.close(index);
                                    vm.proAttachmentDatas.splice(nodeIndex, 1);
                                },
                            });
                            // layer.alert('操作成功', function(index){
                            //     layer.close(index);
                            //     vm.proAttachmentDatas.splice(nodeIndex, 1);
                            // });

                        }else{
                            layer.open({
                                content:r.msg,
                                btn:'我知道了'
                            });
                        }
                    }
                });
            });
        },
        downloadAttachment: function (filePath, fileName){
            var fp = encodeURIComponent(encodeURIComponent(filePath));
            var fn = encodeURIComponent(encodeURIComponent(fileName));
            var action = baseURL + 'system/oss/downloadFile?token=' + globalToken + "&filePath="+fp+"&fileName="+fn;
            window.location.href= action;
        },

        /**
         * *******************************部门操作
         */
        // getDeptTree: function(){
        //     //加载部门树
        //     $.get(baseURL + "system/dept/list", function(r){
        //         ztree = $.fn.zTree.init($("#menuTree"), setting, r);
        //     })
        // },
        // openDeptTree: function(){
        //     layer.open({
        //         type: 1,
        //         offset: '50px',
        //         skin: 'layui-layer-molv',
        //         title: "选择部门",
        //         area: ['300px', '450px'],
        //         shade: 0,
        //         shadeClose: false,
        //         content: jQuery("#menuLayer"),
        //         btn: ['确定','清空', '取消'],
        //         btn1: function (index) {
        //             var node = ztree.getSelectedNodes();
        //             printLog("node="+JSON.stringify(node))
        //             //选择上级部门
        //             vm.query.proDepartId = node[0].deptId;
        //             vm.query.proDepartName = node[0].deptName;
        //
        //             layer.close(index);
        //         },
        //         btn2: function (index) {
        //             vm.query.proDepartId = "";
        //             vm.query.proDepartName = "";
        //         }
        //     });
        // },


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
        loadGridData("", "", "", "", "", "", "", "", "", "", 1);
    }
});