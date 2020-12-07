function loadStaffArchives(staffId){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/uc/staffArchives",
        data: {
            staffId:staffId
        },
        success: function(r){
            //layer.close(layerIndex);
            if(r.code == 0){
                vm.entity = r.entity;
                vm.jobDays = r.jobDays;
                vm.archivesRecord = r.archivesRecord; //成长记录
                vm.hasPermission = r.hasPermission;
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

        entity: {
            userId:'',
            name:'',
            sex:'',
            photo:'',
            icType:'',
            icNum:'',
            icValidity:'',
            icAddress:'',
            birthday:'',
            nation:'',
            politicCountenance:'',
            maritalStatus:'',
            fertilityStatus:'',
            residence:'',
            eduType:'',
            eduSchool:'',
            eduMajor:'',
            eduDate:'',
            workBeginDate:'',

            joinCompanyId:'',
            joinCompanyName:'',
            joinDeptId:'',
            joinDeptName:'',
            joinDate:'',
            joinAddr:'',
            positiveDate:'',
            probationPeriod:'',
            departureDate:'',

            postName:'',
            postRank:'',
            levelId:'',
            postCategory:'',
            postLevel:'',
            staffTier:'',

            contractType:'',
            contractBeginDate:'',
            contractFinalDate:'',

            insureAddr:'',
            insureBegin:'',
            insureAccount:'',
            insureBank:'',
            insureTaxPolicy:'',
            linkName:'',
            linkPhone:'',
            linkRelation:'',

            photoIcFront:'',
            photoIcVerso:'',
            photoDegree:'',
            photoEducation:'',
            photoFace:'',

            account:'',
            code:'',
            phonenumber:'',
            email:'',
            status:'',

            ancestorsDeptId:'',
            ancestorsDeptName:''

        },

        jobDays:0,

        archivesRecord:[],

        hasPermission:false

    },
    methods: {

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

        validator: function () {
            return false;
        },

    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        var urlParams = getQueryString();
        printLog("urlParams="+JSON.stringify(urlParams));
        var userId = urlParams.userId;
        printLog("userId="+userId);

        loadStaffArchives(userId);
    }
});

