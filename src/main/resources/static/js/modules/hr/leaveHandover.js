

function init(){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/hr/leaveHandover/init",
        data: {},
        success: function(r){
            //layer.close(layerIndex);
            if(r.code == 0){
                vm.entity = r.entity;
                vm.accList = r.accList;
                vm.admList = r.admList;

                vm.entity.oss = "";
                vm.entity.ccAccounts = "";

                //loadCcStaffList();

                if(isBlank(vm.entity.applyId)){
                    layer.open({
                        content: '您尚未提交离职申请,或您的离职申请正在审批中,不能办理离职交接',
                        btn: '我知道了'
                    });
                }
            }else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        },
        error:function(data){
            //layer.close(layerIndex);
            layer.open({
                content: "请求出错，请稍后再试！",
                btn: '我知道了'
            });
        }
    });
}


/**
 * 用户选择回调
 * @param userObj
 */
function contactChoiceCallback (userObj){
    if(vm.contactChoiceType==1){
        vm.entity.handoverUserId = userObj.userId;
        vm.entity.handoverUserName = userObj.name;
        $('#modal-cc-staff').modal("hide");
        return ;
    }


    if(!staffContain(vm.ccStaffList,userObj) && !staffContain(vm.ccContactList,userObj)){
        var staff = {
            userId : userObj.userId,
            account : userObj.account,
            name : userObj.name,
            photo : userObj.photo
        }
        vm.ccContactList.push(staff);

        var tips = userObj.name+"成功加入抄送人";
        //提示
        layer.open({
            content: tips,
            skin: 'msg',
            time: 2 //2秒后自动关闭
        });
        $('#modal-cc-staff').modal("hide");
    } else {
        var tips = userObj.name+"已经在抄送人范围内,不能重复加入";
        //提示
        layer.open({
            content: tips,
            skin: 'msg',
            time: 2 //2秒后自动关闭
        });
    }
}

/**
 * 用户重复判断
 * @param obj
 * @returns {boolean}
 */
function staffContain(array, obj){
    for (var i = 0; i < array.length; i++){
        if (array[i].userId == obj.userId){
            return true;
        }
    }
    return false;
}


function openAccModal() {
    $('#modal-handover-acc').modal("show");
}

function openAdmModal() {
    $('#modal-handover-adm').modal("show");
}

var fileUploadLayerIndex;

function initFileUpload(){
    new AjaxUpload('#uploadOss', {
        action: baseURL + 'app/common/upload?token=' + globalToken,
        name: 'file',
        data:{
            "resType":"workflow",
            "resKey":""
        },
        autoSubmit:true,
        responseType:"json",
        onSubmit:function(file, extension){
            if (!(extension && /^(jpg|jpeg|png|gif)$/.test(extension.toLowerCase()))){
                layer.open({
                    content: '只支持jpg、jpeg、png、gif格式的图片',
                    btn: '我知道了'
                });
                return false;
            }
            fileUploadLayerIndex = layer.open({
                type: 2
                ,content: '文件上传中...'
            });
        },
        onComplete : function(file, r){
            layer.close(fileUploadLayerIndex);
            if(r.code == 0){
                vm.ossList.push(r.oss);
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
 * 加载默认抄送人信息
 */
function loadCcStaffList(){
    vm.ccStaffList = [];

    var param = {
        businessType : 'hr_leave_handover',
        proId : vm.entity.proId
    }

    $.ajax({
        type: "POST",
        url: baseURL + "app/common/drools/getCcStaffList",
        data: param,
        success: function(r){
            if(r.code == 0){
                var sList = r.ccStaffList;
                if(sList!=null && sList.length>0){
                    for(var i=0;i<sList.length;i++){
                        var userObj = sList[i];
                        if(!staffContain(vm.ccStaffList,userObj) && !staffContain(vm.ccContactList,userObj)){
                            var staff = {
                                userId : userObj.userId,
                                account : userObj.account,
                                name : userObj.name,
                                photo : userObj.photo
                            }
                            vm.ccStaffList.push(staff);
                        }
                    }
                }
            }
        }
    });
}

var vm = new Vue({
    el: '#vueApp',
    data: {

        dialogIndex: null,

        title: null,
        modalCcStaffTitle:'选择抄送人',

        showList:true,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        entity:{
            id : '',
            applyId : '',
            applyUserId : '',
            joinDate : '',

            proId : '',
            lastWorkDate : '',
            lastSalaryDate : '',
            lastInsuranceDate : '',
            lastFundDate : '',
            handoverUserId : '',
            handoverContent : '',
            handoverAcc : '',
            handoverAdm : '',
            handoverFinance : '',
            memo : '',
            state : '',
            workflowId : '',
            createBy : '',
            createTime : '',
            updateBy : '',
            updateTime : '',

            applyUserName : '',
            handoverUserName : '',
            proName : '',

            oss:'',
            ccAccounts:'',
        },

        //账号交接事项
        accList:[],
        //已选择的账号交接事项
        accLabels:[],

        //行政交接事项
        admList:[],
        //已选择的账号交接事项
        admLabels:[],

        ossList:[],

        currOssImgUrl:'', //当前预览附件图片地址
        currOssImgIndex: 0, //当前预览附件图片的角标

        /**
         * 审批流程图的url
         */
        processInstanceImgUrl:baseURL + "app/common/act/getActivitiProccessDefinitionImage?key=hr_leave_handover",

        /**
         * 抄送人员(流程预定义的抄送人员)
         */
        ccStaffList:[],
        /**
         * 抄送人员(自定义添加的抄送人员)
         */
        ccContactList:[],

        /**
         * 用户选择标志
         * 0 抄送
         * 1 交接人
         */
        contactChoiceType:0,
    },

    filters:{
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        splitLine:function(value){
            return value.replace(new RegExp(',','g'), "<br/>");
        },
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        newLine:function(value){
            return value.replace(/\n/g, "<br/>");
        }
    },

    methods: {

        //提交
        saveEntity: function(){
            if(vm.ajaxLock){
                return ;
            }
            vm.ajaxLock = true;

            var oss = [];
            if(vm.ossList!=null && vm.ossList.length>0){
                for(i = 0,len=vm.ossList.length; i < len; i++) {
                    oss.push(vm.ossList[i].id);
                }
            }
            vm.entity.oss = oss.join(",");

            var ccAccounts = [];
            if(vm.ccContactList!=null && vm.ccContactList.length>0){
                for(i = 0,len=vm.ccContactList.length; i < len; i++) {
                    ccAccounts.push(vm.ccContactList[i].account);
                }
            }
            vm.entity.ccAccounts = ccAccounts.join(",");

            var layerIndex = layer.open({
                type: 2
                ,content: '正在提交...'
            });

            var url = "app/hr/leaveHandover/add";

            $.ajax({
                url: baseURL + url,
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: vm.entity,
                success: function(r){
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    if(r.code == 0){
                        layer.open({
                            content: '操作成功'
                            ,btn: ['我知道了']
                            ,yes: function(index){
                                layer.close(index);
                                /**
                                 * 跳转到列表
                                 */
                                vm.toList();
                            }
                        });

                    }else{
                        layer.open({
                            content: r.msg,
                            btn: '我知道了'
                        });
                    }
                },
                error:function(data){
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    layer.open({
                        content: "请求出错，请稍后再试！",
                        btn: '我知道了'
                    });
                }
            });
        },

        toList:function(){
            openWin("../hr/leaveHandoverList.html");
        },

        removeOss:function(index){
            vm.ossList.splice(index,1);
        },


        /**
         * 打开联系人选择对话框
         * @param type 0 抄送人员，1 印章使用人
         */
        openContactChoiceDialog(type){
            vm.contactChoiceType = type;
            if(type=="0"){
                vm.modalCcStaffTitle = "选择抄送人员";
            } else {
                vm.modalCcStaffTitle = "选择交接人员";
            }
            $('#modal-cc-staff').modal("show");
        },

        /**
         * 移除抄送人
         * @param index
         * @param type
         */
        removeCcContactList : function(index){
            vm.ccContactList.splice(index,1);
        },


        accChoiceOk : function(){
            if(vm.accLabels!=null && vm.accLabels.length>0){
                vm.entity.handoverAcc = vm.accLabels.toString();
                $('#modal-handover-acc').modal("hide");
            } else {
                layer.open({
                    content: '请选择具体的交接事项',
                    btn: '我知道了'
                });
            }
        },

        admChoiceOk : function(){
            if(vm.admLabels!=null && vm.admLabels.length>0){
                vm.entity.handoverAdm = vm.admLabels.toString();
                $('#modal-handover-adm').modal("hide");
            } else {
                layer.open({
                    content: '请选择具体的交接事项',
                    btn: '我知道了'
                });
            }
        },

        photoHandler:function(photo){
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }
            return photo;
        },

        /**
         * 图片预览
         * @param imgUrl
         */
        imgPreview: function(imgUrl){
            openImgPreviewWin(imgUrl);
        },

        /**
         * 附件图片预览
         * @param imgUrl
         */
        ossImgPreview: function(index, imgUrl){
            vm.currOssImgIndex = index;
            vm.currOssImgUrl = imgUrl;
            $('#modal-image').modal("show");
        },

        /**
         * 删除图片附件
         */
        ossImgDelete:function(){
            $('#modal-image').modal("hide");
            vm.ossList.splice(vm.currOssImgIndex,1);
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

            if(tag=="entity.lastWorkDate"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '时间选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        fourLevelId: 0,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.entity.lastWorkDate = day;
                        },
                        fallback:function(){
                            vm.entity.lastWorkDate = "";
                        }
                    });
            } else if(tag=="entity.lastSalaryDate"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '时间选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        fourLevelId: 0,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.entity.lastSalaryDate = day;
                        },
                        fallback:function(){
                            vm.entity.lastSalaryDate = "";
                        }
                    });
            } else if(tag=="entity.lastInsuranceDate"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '时间选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        fourLevelId: 0,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.entity.lastInsuranceDate = day;
                        },
                        fallback:function(){
                            vm.entity.lastInsuranceDate = "";
                        }
                    });
            } else if(tag=="entity.lastFundDate"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '时间选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        fourLevelId: 0,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.entity.lastFundDate = day;
                        },
                        fallback:function(){
                            vm.entity.lastFundDate = "";
                        }
                    });
            }

        },

    },
    watch: {
        // 如果 `entity.proId` 发生改变，这个函数就会运行
        'entity.proId': function (newVal, oldQuestion) {
            if(newVal!=""){
                loadCcStaffList();
            }
        },
        admLabels: function (newVal, oldVal) {
            if(newVal.indexOf("无借用任何公司用品")!=-1){
                if(newVal.length != oldVal.length){
                    this.admLabels = [];
                    this.admLabels.push("无借用任何公司用品");
                }
            }
        }

    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        init();

    }
});
