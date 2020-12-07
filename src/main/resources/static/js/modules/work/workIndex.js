function loadData() {
    $.ajax({
        type: "POST",
        url: baseURL + "app/common/init",
        data: {
            "memberId":myStorage.getMemberId()
        },
        success: function (r) {
            if (r.code == 0) {
                vm.currDate = r.currDate;
                vm.intactFlag = r.intactFlag;
                vm.staffInfo = r.staffInfo;

                vm.myTodoCntMap = r.myTodoCntMap;
                vm.myTodoCnt = r.myTodoCnt;
                vm.unReaderNoticeCnt = r.unReaderNoticeCnt;

                vm.appModuleList = r.appModuleList;
            }else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        },
        error:function(data){
            // layer.close(layerIndex);
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
        dialogIndex: null,

        title: null,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        staffInfo: {
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

        currDate:"",

        appModuleList: [],

        myTodoCntMap:{
            attendance_supplement: 0,
            attendance_overtime: 0,
            attendance_leave: 0,
            attendance_travel: 0,
            attendance_egress: 0,

            hr_join_apply: 0,
            hr_leave_apply: 0,
            hr_leave_handover: 0,
            hr_post_adjustment: 0,
            hr_mac_subsidy: 0,
            hr_visit_subsidy: 0,

            adm_seal_apply: 0,
            adm_card_apply: 0,
            adm_passenger_ticket: 0,

            attendance_cnt: 0,
            hr_cnt: 0,
            adm_cnt: 0
        },
        myTodoCnt:0,
        unReaderNoticeCnt:0,
    },
    methods: {
        toForward:function(url){
            parent.forward(url);
        },

        openBanner:function(bannerObj){
            openWin(bannerObj.uri);
        },

        forwardSceneCloud:function(){
            var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });

            var params = {

            }

            var url = "app/scene/cloud/getHomeUri";
            $.ajax({
                url: baseURL + url,
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: params,
                success: function(r){
                    layer.close(layerIndex);

                    if(r.code == 0){
                        vm.sceneCloudHomeUrl = r.sceneCloudHomeUrl;
                        window.open(vm.sceneCloudHomeUrl);
                    } else{
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
        },

        validator: function () {
            return false;
        }
    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        this.currDate = currentDate() +" " + currentWeek();

        loadData();
    }
});

