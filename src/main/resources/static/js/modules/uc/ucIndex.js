var fileUploadLayerIndex;

function initFileUpload(){
    new AjaxUpload('#uploadPhoto', {
        action: baseURL + 'app/common/avatarUpload?token=' + globalToken ,
        name: 'file',
        accept:"image/jpg,image/jpeg,image/png,image/gif",
        capture:"camera",
        data:{
            "resType":"avatar",
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
                vm.staff.photo = r.oss.url;
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

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        staff: {
            userId: '',
            name: '',
            sex: '',
            birthday: '',

            photo: '',
            photoIcFront: '',
            photoIcVerso: '',
            photoDegree: '',
            photoEducation: '',

            joinCompanyId: '',
            joinCompanyName: '北京江融信科技有限公司',
            joinDeptId: '',
            joinDeptName: '',
            joinDate: '',
            joinAddr: '',
            departureDate: '',
            postName: '',
            postRank: '',
            levelId: '',
            contractBeginDate:'',
            contractFinalDate:'',

            account: '',
            code: '',
            phonenumber: '',
            email: '',
        },

    },
    methods: {

        photoHandler:function(photo){
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }
            return photo;
        },

        toEditPass:function(){
            openWin("../uc/passUpdate.html");
        },

        toEditStaffInfo:function(){
            openWin("../uc/staffInfo.html");
        },

        logOut:function(){
            exitSystem();
        },


    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        this.staff = JSON.parse(staffCacheInfo);
    }
});

