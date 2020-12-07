
function init(supplementTime, supplementType){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/attend/supplement/init",
        data: {},
        success: function(r){
            //layer.close(layerIndex);
            if(r.code == 0){
                vm.entity = r.entity;
                vm.entity.oss = "";
                vm.entity.ccAccounts = "";
                vm.myJoinProList = r.myJoinProList;

                if (!isBlank(supplementTime)){
                    vm.entity.supplementTime = supplementTime;
                }
                if (!isBlank(supplementType)){
                    vm.entity.supplementType = supplementType;
                }
                //loadCcStaffList();
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
 * 加载默认抄送人信息
 */
function loadCcStaffList(){
    vm.ccStaffList = [];

    var param = {
        businessType : 'attendance_supplement',
        proId : vm.entity.proId,
        days : ''
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

/**
 * 用户选择回调
 * @param userObj
 */
function contactChoiceCallback (userObj){
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

var fileUploadLayerIndex;

function initFileUpload(){
    new AjaxUpload('#uploadOss', {
        action: baseURL + 'app/common/upload?token=' + globalToken ,
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

var vm = new Vue({
    el: '#vueApp',
    data: {

        dialogIndex: null,

        title: null,

        showList:true,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        entity:{
            id : '',
            proId : '',
            supplementDay : '',
            supplementType : '0',
            supplementTime : '',
            reason : '',
            state : '0',
            workflowId : '',
            createUserId : '',
            createTime : '',
            updateUserId : '',
            updateTime : '',

            oss:'',
            ccAccounts:'',
        },

        myJoinProList:[],

        ossList:[],

        currOssImgUrl:'', //当前预览附件图片地址
        currOssImgIndex: 0, //当前预览附件图片的角标

        /**
         * 审批流程图的url
         */
        processInstanceImgUrl:baseURL + "app/common/act/getActivitiProccessDefinitionImage?key=attendance_supplement",

        /**
         * 抄送人员(流程预定义的抄送人员)
         */
        ccStaffList:[],
        /**
         * 抄送人员(自定义添加的抄送人员)
         */
        ccContactList:[],
    },
    methods: {
        loadMyJoinProLIst:function(){
            loadProGridData();
        },

        /**
         * 提交补卡
         * @param confirmType 提交类型 0 需要判断时间， 1 二次确认提交（不需要判断时间）
         */
        saveEntity: function(confirmType){
            if(isBlank(vm.entity.supplementTime)){
                layer.open({
                    content: '请选择补卡时间',
                    btn: '我知道了'
                });
                return ;
            }
            if(confirmType==0){
                var hour24 = parseInt((vm.entity.supplementTime.split(" ")[1]).split(":")[0]);
                if(vm.entity.supplementType=='0' && hour24>9){
                    vm.showUnNormalSupplementDialog('您当前的补卡信息为非常规班次的补卡,您确定要补'+hour24+'点的上班卡吗?');
                    return ;
                } else if(vm.entity.supplementType=='1' && hour24<18){
                    vm.showUnNormalSupplementDialog('您当前的补卡信息为非常规班次的补卡,您确定要补'+hour24+'点的上班卡吗?');
                    return ;
                }
            }

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

            var url = "app/attend/supplement/add";
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

        showUnNormalSupplementDialog:function(tips){
            layer.open({
                content: tips
                ,btn: ['继续提交', '放弃补卡'] ,
                yes: function(index){
                    layer.close(index);
                    vm.saveEntity(1);
                },
                no: function(index){
                    layer.close(index);
                }
            });
        },

        validator: function () {

            return false;
        },

        toList:function(){
            openWin("../attendance/supplementList.html");
        },

        removeOss:function(index){
            vm.ossList.splice(index,1);
        },

        /**
         * 移除抄送人
         * @param index
         * @param type
         */
        removeCcContactList : function(index){
            vm.ccContactList.splice(index,1);
        },

        photoHandler:function(photo){
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }
            return photo;
        },

        /**
         * 流程图片预览
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

            if(tag=="entity.supplementTime"){
                var iosSelect = new IosSelect(5,
                    [yckYearData, yckMonthData, yckDateData, yckHourData, yckMinuteData],
                    {
                        title: '日期选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        fourLevelId: 1,
                        fiveLevelId: 1,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj, selectFourObj, selectFiveObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value + ' ' + selectFourObj.value + ':' + selectFiveObj.value+":00";
                            vm.entity.supplementTime = day;
                        },
                        fallback:function(){
                            vm.entity.supplementTime = "";
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
        'entity.supplementType': function (newVal, oldQuestion) {
            if(newVal!=""){
                if(newVal == '0'){
                    var supplementDay = vm.entity.supplementTime.split(" ")[0];
                    vm.entity.supplementTime = supplementDay + " 09:00:00";
                } else if(newVal == '1'){
                    var supplementDay = vm.entity.supplementTime.split(" ")[0];
                    vm.entity.supplementTime = supplementDay + " 18:00:00";
                }
                loadCcStaffList();
            }
        },
    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {

        var urlParams = getQueryString();
        printLog("urlParams="+JSON.stringify(urlParams));
        var supplementDay = urlParams.supplementDay;
        if(isBlank(supplementDay)){
            supplementDay = currentDate()
        }
        printLog("supplementDay="+supplementDay);
        var supplementType = urlParams.supplementType;
        if(isBlank(supplementType)){
            supplementType = "0";
        }
        printLog("supplementType="+supplementType);
        var supplementTime = "";
        if(supplementType=="0"){
            supplementTime = supplementDay+" 09:00:00";
        } else {
            supplementTime = supplementDay+" 18:00:00";
        }
        printLog("supplementTime="+supplementTime);

        init(supplementTime, supplementType);

    }
});

