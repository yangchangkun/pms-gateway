function loadDetail(id) {
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/
    $.ajax({
        type: "POST",
        url: baseURL + "app/crmVisit/detail",
        data: {
            id: id
        },
        success: function (r) {
            //layer.close(layerIndex);
            if (r.code == 0) {
                vm.entity = r.entity[0];
                vm.infoList = r.infoList;
                vm.ossList = r.ossList;
                vm.personData.custId = vm.entity.custId;
                for (var i = 0; i < vm.infoList.length; i++) {
                    if (vm.entity.custId == vm.infoList[i].id) {
                        vm.stakeholder = vm.infoList[i].stakeholder;
                    }
                }
                loadCcStaffList();
            } else {
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        },
        error: function (data) {
            //layer.close(layerIndex);
            layer.open({
                content: "请求出错，请稍后再试！",
                btn: '我知道了'
            });
        }
    });
}

function init() {
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/crmVisit/init",
        data: {},
        success: function (r) {
            //layer.close(layerIndex);
            if (r.code == 0) {
                vm.entity = r.entity;
                vm.entity.oss = "";
                vm.entity.ccAccounts = "";
                vm.infoList = r.infoList;

                loadCcStaffList();
            } else {
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        },
        error: function (data) {
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
function loadCcStaffList() {
    vm.ccStaffList = [];

    var param = {
        businessType: 'crm_customer_visit',
        proId: vm.entity.proId,
        days: ''
    }

    $.ajax({
        type: "POST",
        url: baseURL + "app/common/drools/getCcStaffList",
        data: param,
        success: function (r) {
            if (r.code == 0) {
                var sList = r.ccStaffList;
                if (sList != null && sList.length > 0) {
                    for (var i = 0; i < sList.length; i++) {
                        var userObj = sList[i];
                        if (!staffContain(vm.ccStaffList, userObj) && !staffContain(vm.ccContactList, userObj)) {
                            var staff = {
                                userId: userObj.userId,
                                account: userObj.account,
                                name: userObj.name,
                                photo: userObj.photo
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
function contactChoiceCallback(userObj) {
    if (!staffContain(vm.ccStaffList, userObj) && !staffContain(vm.ccContactList, userObj)) {
        var staff = {
            userId: userObj.userId,
            account: userObj.account,
            name: userObj.name,
            photo: userObj.photo
        }
        vm.ccContactList.push(staff);

        var tips = userObj.name + "成功加入抄送人";
        //提示
        layer.open({
            content: tips,
            skin: 'msg',
            time: 2 //2秒后自动关闭
        });
        $('#modal-cc-staff').modal("hide");
    } else {
        var tips = userObj.name + "已经在抄送人范围内,不能重复加入";
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
function staffContain(array, obj) {
    for (var i = 0; i < array.length; i++) {
        if (array[i].userId == obj.userId) {
            return true;
        }
    }
    return false;
}

var fileUploadLayerIndex;

function initFileUpload() {
    new AjaxUpload('#uploadOss', {
        action: baseURL + 'app/common/upload?token=' + globalToken,
        name: 'file',
        data: {
            "resType": "workflow",
            "resKey": ""
        },
        autoSubmit: true,
        responseType: "json",
        onSubmit: function (file, extension) {
            if (!(extension && /^(jpg|jpeg|png|gif)$/.test(extension.toLowerCase()))) {
                layer.open({
                    content: '只支持jpg、jpeg、png、gif格式的图片',
                    btn: '我知道了'
                });
                return false;
            }
            fileUploadLayerIndex = layer.open({
                type: 2
                , content: '文件上传中...'
            });
        },
        onComplete: function (file, r) {
            layer.close(fileUploadLayerIndex);
            if (r.code == 0) {
                vm.ossList.push(r.oss);
            } else {
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

        showList: true,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        entity: {
            id: '',
            custId: '',
            custShortName: '',
            custLeaderId:'',
            leaderName:'',
            custStakeholderId: '',
            holderName: '',
            visitMode: '',
            visitBrief: '',
            visitParticipants: '0',
            visitDescribe: '',
            visitConclusion: '',
            visitPlan: '0',
            visitAdvise: '',
            visitState: '',
            createUserId: '',
            createTime: '',
            updateUserId: '',
            updateTime: '',
            createName: '',
            visitTime: '',
            firstTime: '',
            weekNewCust: '',
            manyTimes: '',
            sellStatus: '',
            visitQuest: '',
            visitCompleteDay: '',
            memo: '',
            flag: '1'
        },
        personData: {
            id: '',
            custId: '',
            postName: '',
            name: '',
            deptName: '',
            parentId: '',
            gender: '',
            phonenumber: '',
            memo: '',
            bankStructure: '',
            businessDept: '',
            cardOssId: '',
            flag: '1',
            leaderPostName:'',
            leaderName:''
        },
        custBase:{
            id:'',
            custFullName:'',
            custProvince:'',
            custShortName:'',
            custCategory:'',
            custRegion:'',
            custAssets:'',
            businessScope:'',
            stakeholder:[]
        },
        infoList: [],
        stakeholder: [],

        ossList: [],

        /**
         * 审批流程图的url
         */
        processInstanceImgUrl: baseURL + "app/common/act/getActivitiProccessDefinitionImage?key=hr_join_apply",

        /**
         * 抄送人员(流程预定义的抄送人员)
         */
        ccStaffList: [],
        /**
         * 抄送人员(自定义添加的抄送人员)
         */
        ccContactList: [],

    },
    methods: {
        reset: function (flag) {
            vm.personData = {
                id: '',
                custId: vm.personData.custId,
                postName: '',
                name: '',
                deptName: '',
                parentId: '',
                gender: '',
                phonenumber: '',
                memo: '',
                bankStructure: '',
                businessDept: '',
                cardOssId: '',
                flag: flag,
                leaderPostName:'',
                leaderName:''
            }
        },
        saveEntity: function (flag) {
            if(vm.entity.flag=='1'){
                vm.entity.custLeaderId='';
            }
            if (vm.ajaxLock) {
                return;
            }
            vm.ajaxLock = true;

            var oss = [];
            if (vm.ossList != null && vm.ossList.length > 0) {
                for (var i = 0; i < vm.ossList.length; i++) {
                    oss.push(vm.ossList[i].id);
                }
            }
            vm.entity.oss = oss.join(",");

            var ccAccounts = [];
            if (vm.ccContactList != null && vm.ccContactList.length > 0) {
                for (var i = 0; i < vm.ccContactList.length; i++) {
                    ccAccounts.push(vm.ccContactList[i].account);
                }
            }
            if (vm.ccStaffList != null && vm.ccStaffList.length > 0) {
                for (var i = 0; i < vm.ccStaffList.length; i++) {
                    ccAccounts.push(vm.ccStaffList[i].account);
                }
            }
            vm.entity.ccAccounts = ccAccounts.join(",");
            vm.entity.visitState = flag;
            if (flag == '2') {
                vm.entity.visitState = '1';
            }
            // if (isBlank(vm.entity.custId)) {
            //     layer.open({content:"银行不能为空",btn: '我知道了'});
            //     return;
            // }
            // if (isBlank(vm.entity.visitTime)) {
            //     layer.open({content:"拜访时间不能为空",btn: '我知道了'});
            //     return;
            // }
            // if (isBlank(vm.entity.custStakeholderId)) {
            //     layer.open({content:"接触人不能为空",btn: '我知道了'});
            //     return ;
            // }
            // if (isBlank(vm.entity.firstTime)) {
            //     layer.open({content:"初次接触时间不能为空",btn: '我知道了'});
            //     return false;
            // }
            // if(flag!='0'){
            //     if (isBlank(vm.entity.visitBrief)) {
            //         layer.open({content:"拜访的目的不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.visitMode)) {
            //         layer.open({content:"拜访的方式不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.firstTime)) {
            //         layer.open({content:"初次接触不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.weekNewCust)) {
            //         layer.open({content:"是否是本周新增扩展客户不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.manyTimes)) {
            //         layer.open({content:"本次是第几次拜访不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.visitParticipants)) {
            //         layer.open({content:"拜访纪要的客户需求不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.visitDescribe)) {
            //         layer.open({content:"拜访纪要的客户跟踪详细记录不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.sellStatus)) {
            //         layer.open({content:"销售状态不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.visitConclusion)) {
            //         layer.open({content:"机会分析不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.visitPlan)) {
            //         layer.open({content:"拜访的下一步计划不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.visitAdvise)) {
            //         layer.open({content:"拜访面临的问题及需要的支持不能为空",btn: '我知道了'});
            //         return ;
            //     }
            //     if (isBlank(vm.entity.visitQuest)) {
            //         layer.open({content:"存在问题不能为空",btn: '我知道了'});
            //         return ;
            //     }
                // if (isBlank(vm.entity.visitCompleteDay)) {
                //     layer.open({content:"结项日期不能为空",btn: '我知道了'});
                //     return ;
                // }
            // }

            var layerIndex = layer.open({
                type: 2,
                content: '正在提交...'
            });
            var url = vm.entity.id == null ? "app/crmVisit/add" : "app/crmVisit/update";

            $.ajax({
                url: baseURL + url,
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: vm.entity,
                success: function (r) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    if (r.code == 0) {
                        layer.open({
                            content: '操作成功'
                            , btn: ['我知道了']
                            , yes: function (index) {
                                layer.close(index);
                                if (flag == '2') {
                                    //再重新添加
                                    vm.toRepeat();
                                } else {
                                    /**
                                     * 跳转到列表
                                     */
                                    vm.toList();
                                }

                            }
                        });

                    } else {
                        layer.open({
                            content: r.msg,
                            btn: '我知道了'
                        });
                    }
                },
                error: function (data) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    layer.open({
                        content: "请求出错，请稍后再试！",
                        btn: '我知道了'
                    });
                }
            });
        },
        toEdit:function (id,flag){
            // var id = vm.entity.custLeaderId;
            // if(id==""){
            //     layer.open({content:"没有可修改的人员信息",btn: '我知道了'});
            //     return ;
            // }
            for(var i =0;i<vm.stakeholder.length;i++){
                if(id == vm.stakeholder[i].id){
                    vm.personData = vm.stakeholder[i];
                    vm.personData.flag = flag;

                }
            }
        },
        addPer: function () {
            // if (isBlank(vm.personData.postName)) {
            //     layer.open({content:"任职情况不能为空",btn: '我知道了'});
            //     return ;
            // }
            // if (isBlank(vm.personData.name)) {
            //     layer.open({content:"姓名不能为空",btn: '我知道了'});
            //     return ;
            // }
            // if (isBlank(vm.personData.gender)) {
            //     layer.open({content:"性别不能为空",btn: '我知道了'});
            //     return ;
            // }
            // if (isBlank(vm.personData.phonenumber)) {
            //     layer.open({content:"电话号码不能为空",btn: '我知道了'});
            //     return ;
            // }

            $.ajax({
                url: baseURL + "app/crmVisit/addPerson",
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: vm.personData,
                success: function (r) {
                    vm.ajaxLock = false;
                    if (r.code == 0) {
                        if(vm.personData.id==""){
                            layer.open({
                                content: '添加成功'
                                , btn: ['我知道了']
                                , yes: function (index) {
                                    layer.close(index);
                                    vm.personData.id = r.id;
                                    vm.stakeholder.push(vm.personData);
                                }
                            });
                        }else {
                            layer.open({
                                content: '修改成功',
                                btn: ['我知道了']
                            });
                        }


                    } else {
                        layer.open({
                            content: r.msg,
                            btn: '我知道了'
                        });
                    }
                },
                error: function (data) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    layer.open({
                        content: "请求出错，请稍后再试！",
                        btn: '我知道了'
                    });
                }
            });
        },
        addCustomer:function(){
            // if(vm.ajaxLock){
            //     return ;
            // }
            // vm.ajaxLock = true;
            $.ajax({
                type: "POST",
                url: baseURL + "app/crmVisit/addCustomer",
                contentType: "application/x-www-form-urlencoded",
                data: vm.custBase,
                success: function(r){
                    if(r.code == 0){
                        vm.ajaxLock = false;
                        layer.open({
                            content: '添加成功'
                            , btn: ['我知道了']
                            , yes: function (index) {
                                layer.close(index);
                                vm.custBase.id = r.id;
                                vm.infoList.push(vm.custBase);
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
        selectBank: function (ele) {
            var objEle = ele.target || ele.srcElement; //获取document 对象的引用
            if (!isBlank(objEle.value)) {
                for (var i = 0; i < vm.infoList.length; i++) {
                    if (objEle.value == vm.infoList[i].id) {
                        vm.stakeholder = vm.infoList[i].stakeholder;
                        vm.personData.custId = vm.infoList[i].id;
                    }
                }
            }
        },
        toList: function () {
            openWin("../sell/myVisitList.html");
        },
        toRepeat: function () {
            vm.entity = {
                id: '',
                custId: '',
                custShortName: '',
                custLeaderId:'',
                leaderName:'',
                custStakeholderId: '',
                holderName: '',
                visitMode: '',
                visitBrief: '',
                visitParticipants: '',
                visitDescribe: '',
                visitConclusion: '',
                visitPlan: '',
                visitAdvise: '',
                visitState: '',
                createUserId: '',
                createTime: '',
                updateUserId: '',
                updateTime: '',
                createName: '',
                visitTime: '',
                firstTime: '',
                weekNewCust: '',
                manyTimes: '',
                sellStatus: '',
                visitQuest: '',
                visitCompleteDay: '',
                memo: '',
                bankStructure:'',
                businessDept:''
            };
            vm.personData = {
                id: '',
                custId: '',
                postName: '',
                name: '',
                deptName: '',
                parentId: '',
                gender: '',
                phonenumber: '',
                memo: '',
                bankStructure: '',
                businessDept: '',
                cardOssId: '',
                flag: '1',
                leaderPostName:'',
                leaderName:''
            }
            vm.ossList = [];
            /**
             * 抄送人员(自定义添加的抄送人员)
             */
            vm.ccContactList = []
            // openWin("../sell/myVisitApply.html");
        },

        // validator: function () {
        //     if (isBlank(vm.entity.custId)) {
        //         layer.open({content:"银行不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.visitTime)) {
        //         layer.open({content:"拜访时间不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.custStakeholderId)) {
        //         layer.open({content:"接触不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.visitBrief)) {
        //         layer.open({content:"拜访的目的不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.visitMode)) {
        //         layer.open({content:"拜访的方式不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.firstTime)) {
        //         layer.open({content:"初次接触不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.weekNewCust)) {
        //         layer.open({content:"是否是本周新增扩展客户不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.manyTimes)) {
        //         layer.open({content:"本次是第几次拜访不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.visitParticipants)) {
        //         layer.open({content:"拜访纪要的客户需求不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.visitDescribe)) {
        //         layer.open({content:"拜访纪要的客户跟踪详细记录不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.sellStatus)) {
        //         layer.open({content:"销售状态不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.visitConclusion)) {
        //         layer.open({content:"机会分析不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.visitPlan)) {
        //         layer.open({content:"拜访的下一步计划不能为空",btn: '我知道了'});
        //         return true;
        //     }
        //     if (isBlank(vm.entity.visitAdvise)) {
        //         layer.open({content:"拜访面临的问题及需要的支持不能为空",btn: '我知道了'});
        //         return true;
        //     }
            // if (isBlank(vm.entity.weekNewCust)) {
            //     layer.alert("是否是本周新增扩展客户不能为空");
            //     return true;
            // }
            // if (isBlank(vm.entity.manyTimes)) {
            //     layer.alert("本次是第几次拜访不能为空");
            //     return true;
            // }
            // if (isBlank(vm.entity.sellStatu)) {
            //     layer.alert("销售状态不能为空");
            //     return true;
            // }
            // if (isBlank(vm.entity.visitQuest)) {
            //     layer.open({content:"存在问题不能为空",btn: '我知道了'});
            //     return true;
            // }
            // if (isBlank(vm.entity.visitCompleteDay)) {
            //     layer.open({content:"结项日期不能为空",btn: '我知道了'});
            //     return true;
            // }
        // },
        //移除附件
        removeOss: function (id, index) {
            if (isBlank(id)) {
                vm.ossList.splice(index, 1);
            } else {
                layer.open({
                    content: '确定要删除该附件吗？删除后不可找回'
                    , btn: ['确定']
                    , yes: function () {
                        $.ajax({
                            type: "POST",
                            url: baseURL + "app/crmVisit/deleteAttachment",
                            data: "id=" + id,
                            success: function (r) {
                                if (r.code === 0) {
                                    layer.open({
                                        content: '操作成功'
                                        , btn: ['我知道了']
                                        , yes: function (i) {
                                            layer.close(i);
                                            vm.ossList.splice(index, 1);
                                        }
                                    });
                                } else {
                                    layer.open({
                                        content: r.msg,
                                        btn: '我知道了'
                                    });
                                }
                            }
                        });
                    }
                });
            }
        },


        /**
         * 移除抄送人
         * @param index
         * @param type
         */
        removeCcContactList: function (index) {
            vm.ccContactList.splice(index, 1);
        },

        photoHandler: function (photo) {
            if (isBlank(photo)) {
                return "../img/me_avatar.png";
            }
            return photo;
        },

        // validator: function () {
        //
        //     return false;
        // },

        /**
         * 图片预览
         * @param imgUrl
         */
        imgPreview: function (imgUrl) {
            openImgPreviewWin(imgUrl);
        },

        /**
         * 日期和时间控件选择器与vue.data绑定处理
         * @param ele 当前操作的dom对象
         * @param fmt 时间格式化，eg.yyyy-MM-dd or H:mm:ss
         * @param tag 标记，该参数用来标记日期选择器选择的值要赋值给vue.data里面的具体的值，逻辑需要编写
         * @param idx 拓展参数，如果要赋值的vue.data 是一个Array，则这个参数就是下标
         */
        datePickerChange: function (ele, fmt, tag, idx) {
            var objEle = ele.target || ele.srcElement; //获取document 对象的引用
            var eleId = objEle.id;

            if (tag == "entity.visitTime") {
                var iosSelect = new IosSelect(5,
                    [yckYearData, yckMonthData, yckDateData, yckHourData, yckMinuteData],
                    {
                        title: '时间选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        fourLevelId: 1,
                        fiveLevelId: 1,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj, selectFourObj, selectFiveObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value + ' ' + selectFourObj.value + ':' + selectFiveObj.value + ":00";
                            vm.entity.visitTime = day;
                        },
                        fallback: function () {
                            vm.entity.visitTime = "";
                        }
                    });
            } else if (tag == "entity.firstTime") {
                var iosSelect = new IosSelect(5,
                    [yckYearData, yckMonthData, yckDateData, yckHourData, yckMinuteData],
                    {
                        title: '时间选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        fourLevelId: 1,
                        fiveLevelId: 1,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj, selectFourObj, selectFiveObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value + ' ' + selectFourObj.value + ':' + selectFiveObj.value + ":00";
                            vm.entity.firstTime = day;
                        },
                        fallback: function () {
                            vm.entity.firstTime = "";
                        }
                    });
            } else if (tag == "entity.visitCompleteDay") {
                var iosSelect = new IosSelect(3,
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
                            vm.entity.visitCompleteDay = day;
                        },
                        fallback: function () {
                            vm.entity.visitCompleteDay = "";
                        }
                    });
            }

        },

    },
    watch: {
        // 如果 `entity.proId` 发生改变，这个函数就会运行
        'entity.proId': function (newVal, oldQuestion) {
            if (newVal != "") {
                loadCcStaffList();
            }
        }
    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        var urlParams = getQueryString();
        printLog("urlParams=" + JSON.stringify(urlParams));
        var id = urlParams.id;
        printLog("id=" + id);
        if (typeof(id) == 'undefined') {
            init();
        } else {
            loadDetail(id);
        }

    }
});

