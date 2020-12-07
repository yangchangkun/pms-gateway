

/**
 * ************************* 时间 刷新定时器
 */

var timeInterval = null;//计时器

function startTimer(){
    if(timeInterval!=null){
        clearInterval(timeInterval);
        timeInterval=null;
    }
    timeInterval = setInterval(runTimer,1000);
}

function runTimer(){
    var currTime = currentTime();
    $("#timer").text(currTime);
}

function stopTimer(){
    clearInterval(timeInterval);
    timeInterval = null;
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

        today:'', //当前日期

        gpsRunState : -1, //gps 运行状态，0 定位中，1 定位成功，2 定位失败
        //gps信息
        gpsInfo:{
            lat:'',
            lng:'',
            addr:''
        },

        allowTag:false,//是否允许打卡的标志
        tips: '', //提示信息

        /**
         * 考勤组信息
         */
        attendGroup:{
            groupId : '',
            name : '--',
            signNormalTime : '--:--',
            signElasticTime : '--:--',
            outTime : '--:--',
            type : 0,
            state : 0
        },

        /**
         * 考勤组对应的考勤范围
         */
        pointList:[],

        /**
         * 上班考勤记录
         */
        inAttendRecordDetail:{
            id : '',
            userId : '',
            attendTime : '--',
            attendLng : 0,
            attendLat : 0,
            attendAddr : '--',
            fingerPrint : '--',

            attendGroupId : '',
            attendGroupName : '--'
        },
        /**
         * 下班考勤记录
         */
        outAttendRecordDetail:{
            id : '',
            userId : '',
            attendTime : '--',
            attendLng : 0,
            attendLat : 0,
            attendAddr : '--',
            fingerPrint : '--',

            attendGroupId : '',
            attendGroupName : '--'
        },

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

    },
    methods: {
        initLoad: function(){
            var params = {
                "lat": vm.gpsInfo.lat,
                "lng": vm.gpsInfo.lng,
            }

            var url = "app/attend/record/init";
            $.ajax({
                url: baseURL + url,
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: params,
                success: function(r){
                    if(r.code == 0){
                        vm.allowTag = r.allowTag;
                        vm.tips = r.tips;
                        vm.attendGroup = r.hitGroup;
                        vm.pointList = r.pointList;
                        vm.inAttendRecordDetail = r.signInEntity;
                        vm.outAttendRecordDetail = r.signOutEntity;

                        /**
                         * 将考勤组范围标记在地图上
                         */
                        addRange(vm.pointList);

                    } else {
                        vm.tips = r.msg;
                    }
                },
                error:function(data){
                    vm.tips = "请求出错，请稍后再试！";
                }
            });
        },

        sign: function(){
            if(!vm.allowTag){
                return;
            }
            if (vm.ajaxLock) {
                return;
            }
            vm.ajaxLock = true;
            var layerIndex = layer.open({
                type: 2
                ,content: '提交中...'
            });

            var params = {
                "lat": vm.gpsInfo.lat,
                "lng": vm.gpsInfo.lng,
                "addr": vm.gpsInfo.addr,
                "fingerPrint": myStorage.getFinger()
            }

            var url = "app/attend/record/sign";
            $.ajax({
                url: baseURL + url,
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: params,
                success: function(r){
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    if (r.code === 0) {
                        layer.open({
                            content: '打卡成功'
                            ,btn: ['我知道了']
                            ,yes: function(index){
                                layer.close(index);
                                vm.initLoad();
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

        photoHandler:function(photo){
            if(photo==null){
                return "../img/me_avatar.png";
            }
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }
            return photo;
        },

        fingerPrintHandler:function(){
            if(vm.allowTag){
                return "../img/app/icon_fingerprint_gray.png";
            } else {
                return "../img/app/icon_fingerprint.png";
            }
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
        this.today = currentDate();

        this.staff = JSON.parse(staffCacheInfo);

        startTimer();

    },
    beforeDestroy: function () {
        // 每次离开当前界面时，清除定时器
        stopTimer();
        clearWatch();
    }
});

