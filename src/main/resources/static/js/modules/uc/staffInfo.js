
var fileUploadLayerIndex;

function initFileUpload(){

    new AjaxUpload('#uploadPhotoIcFront', {
        action: baseURL + 'app/common/upload?token=' + globalToken,
        name: 'file',
        data:{
            "resType":"certificate",
            "resKey":""
        },
        autoSubmit:true,
        responseType:"json",
        onSubmit:function(file, extension){
            if (!(extension && /^(jpg|jpeg|png|gif)$/.test(extension.toLowerCase()))){
                layer.open({
                    content: '只支持jpg、png、gif格式的图片',
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
                vm.staff.photoIcFront = r.oss.url;
            }else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        }
    });

    new AjaxUpload('#uploadPhotoIcVerso', {
        action: baseURL + 'app/common/upload?token=' + globalToken,
        name: 'file',
        data:{
            "resType":"certificate",
            "resKey":""
        },
        autoSubmit:true,
        responseType:"json",
        onSubmit:function(file, extension){
            if (!(extension && /^(jpg|jpeg|png|gif)$/.test(extension.toLowerCase()))){
                layer.open({
                    content: '只支持jpg、png、gif格式的图片',
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
                vm.staff.photoIcVerso = r.oss.url;
            }else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        }
    });

    new AjaxUpload('#uploadPhotoDegree', {
        action: baseURL + 'app/common/upload?token=' + globalToken,
        name: 'file',
        data:{
            "resType":"certificate",
            "resKey":""
        },
        autoSubmit:true,
        responseType:"json",
        onSubmit:function(file, extension){
            if (!(extension && /^(jpg|jpeg|png|gif)$/.test(extension.toLowerCase()))){
                layer.open({
                    content: '只支持jpg、png、gif格式的图片',
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
                vm.staff.photoDegree = r.oss.url;
            }else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        }
    });

    new AjaxUpload('#uploadPhotoEducation', {
        action: baseURL + 'app/common/upload?token=' + globalToken,
        name: 'file',
        data:{
            "resType":"certificate",
            "resKey":""
        },
        autoSubmit:true,
        responseType:"json",
        onSubmit:function(file, extension){
            if (!(extension && /^(jpg|jpeg|png|gif)$/.test(extension.toLowerCase()))){
                layer.open({
                    content: '只支持jpg、png、gif格式的图片',
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
                vm.staff.photoEducation = r.oss.url;
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
        stepTag: 1,

        dialogIndex: null,

        title: null,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        staff: {
            userId: '',
            name: '',
            sex: '0',
            birthday: '',

            politicCountenance: '',
            maritalStatus: '',
            fertilityStatus: '',

            icType: '',
            icNum: '',
            icAddress: '',
            residence: '',

            eduType: '',
            eduSchool: '',
            eduMajor: '',
            eduDate: '',
            workBeginDate: '',

            photo: '',
            photoIcFront: '',
            photoIcVerso: '',
            photoDegree: '',
            photoEducation: '',

            insureAccount: '',
            insureBank: '',

            insureAddr: '',
            insureBegin: '',
            insureTaxPolicy: '',

            linkName: '',
            linkPhone: '',
            linkRelation: '',

            contractType: '',
            contractBeginDate: '',
            contractFinalDate: '',

            joinCompanyId: '',
            joinCompanyName: '',
            joinDeptId: '',
            joinDeptName: '',
            joinDate: '',
            joinAddr: '',
            departureDate: '',
            postName: '',
            postRank: '',
            levelId: '',

            account: '',
            code: '',
            phonenumber: '',
            email: '',
        },

        user:{
            userId : '',

            account : '',
            userName : '',
            code : '',
            email : '',
            phonenumber : '',

            deptId : '',
            deptName: '',

        },

    },
    methods: {

        toMine: function () {
            var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });

            $.ajax({
                type: "POST",
                url: baseURL + "app/uc/mine",
                data: {

                },
                success: function (r) {
                    layer.close(layerIndex);
                    if (r.code == 0) {
                        vm.staff = r.staff;
                        vm.user = r.user;
                    } else {
                        layer.open({
                            content: r.msg,
                            btn: '我知道了'
                        });
                    }
                }
            });

        },

        saveInfo: function () {
            if (vm.validator()) {
                return;
            }
            if (vm.ajaxLock) {
                return;
            }
            vm.staff.phonenumber = vm.user.phonenumber
            vm.ajaxLock = true;
            var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });

            var url = "app/uc/edit";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/x-www-form-urlencoded",
                data: vm.staff,
                success: function (r) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);

                    if (r.code === 0) {
                        myStorage.setIntactFlag(0);
                        myStorage.setStaff(r.staffCacheInfo);

                        layer.open({
                            content: '完善资料成功'
                            ,btn: ['我知道了']
                            ,yes: function(index){
                                layer.close(index);
                                onBackClick();
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
                        content: '请求出错，请稍后再试！',
                        btn: '我知道了'
                    });
                }
            });
        },

        validator: function () {
            if(isBlank(vm.staff.name)){
                layer.open({
                    content: '请输入姓名',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.sex)){
                layer.open({
                    content: '请选择性别',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.birthday)){
                layer.open({
                    content: '请选择出生日期',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.politicCountenance)){
                layer.open({
                    content: '请选择政治面貌',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.maritalStatus)){
                layer.open({
                    content: '请选择欢迎状况',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.fertilityStatus)){
                layer.open({
                    content: '请选择生育状况',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.icType)){
                layer.open({
                    content: '请选择户籍类型',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.icNum)){
                layer.open({
                    content: '请填写身份证号码',
                    btn: '我知道了'
                });
                return true;
            }

            if(isBlank(vm.staff.icAddress)){
                layer.open({
                    content: '请填写户籍地址',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.residence)){
                layer.open({
                    content: '请填写现住址！',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.eduType)){
                layer.open({
                    content: '请选择最高学历类型',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.eduSchool)){
                layer.open({
                    content: '请填写毕业院校',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.eduMajor)){
                layer.open({
                    content: '请填写所学专业',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.eduDate)){
                layer.open({
                    content: '请选择毕业日期',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.workBeginDate)){
                layer.open({
                    content: '请选择开始工作日期',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.linkName)){
                layer.open({
                    content: '请填写紧急联系人姓名',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.linkPhone)){
                layer.open({
                    content: '请填写紧急联系人电话',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.linkRelation)){
                layer.open({
                    content: '请填写紧急联系人关系',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.insureAccount)){
                layer.open({
                    content: '请填写工资卡号',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.insureBank)){
                layer.open({
                    content: '请填写工资卡开户行',
                    btn: '我知道了'
                });
                return true;
            }

            // if(isBlank(vm.staff.photo)){
            //     layer.open({
            //         content: '请上传照片',
            //         btn: '我知道了'
            //     });
            //     return true;
            // }
            if(isBlank(vm.staff.photoIcFront)){
                layer.open({
                    content: '请上传身份证人像面',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.photoIcVerso)){
                layer.open({
                    content: '请上传身份证国徽面',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.photoDegree)){
                layer.open({
                    content: '请上传学位证书照片',
                    btn: '我知道了'
                });
                return true;
            }
            if(isBlank(vm.staff.photoEducation)){
                layer.open({
                    content: '请上传学历证书照片',
                    btn: '我知道了'
                });
                return true;
            }
            return false;
        },

        photoHandler:function(photo){
            if(isBlank(photo)){
                return "../img/webuploader.png";
            }
            return photo;
        },

        toStep1:function(){
            vm.stepTag = 1;
        },

        toStep2:function(){
            vm.stepTag = 2;
        },

        toStep3:function(){
            vm.stepTag = 3;
        },

        toUcIndex:function(){
            parent.tabClick("ucTab");
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

            if(tag=="staff.birthday"){
                var iosSelect = new IosSelect(3,
                [yckYearData, yckMonthData, yckDateData],
                {
                    title: '生日',
                    itemHeight: 35,
                    oneLevelId: yckNowYear,
                    twoLevelId: yckNowMonth,
                    threeLevelId: yckNowDate,
                    showLoading: true,
                    callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                        var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                        vm.staff.birthday = day;
                    },
                    fallback:function(){
                        vm.staff.birthday = "";
                    }
                });
            } else if(tag=="staff.eduDate"){
                var iosSelect = new IosSelect(2,
                [yckYearData, yckMonthData],
                {
                    title: '日期选择',
                    itemHeight: 35,
                    oneLevelId: yckNowYear,
                    twoLevelId: yckNowMonth,
                    showLoading: true,
                    callback: function (selectOneObj, selectTwoObj) {
                        var day = selectOneObj.value + '-' + selectTwoObj.value;
                        vm.staff.eduDate = day;
                    },
                    fallback:function(){
                        vm.staff.eduDate = "";
                    }
                });
            } else if(tag=="staff.workBeginDate"){
                var iosSelect = new IosSelect(2,
                [yckYearData, yckMonthData],
                {
                    title: '日期选择',
                    itemHeight: 35,
                    oneLevelId: yckNowYear,
                    twoLevelId: yckNowMonth,
                    showLoading: true,
                    callback: function (selectOneObj, selectTwoObj) {
                        var day = selectOneObj.value + '-' + selectTwoObj.value;
                        vm.staff.workBeginDate = day;
                    },
                    fallback:function(){
                        vm.staff.workBeginDate = "";
                    }
                });
            }

        },

    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        this.toMine();
    }
});

