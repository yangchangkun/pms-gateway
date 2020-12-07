

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
            photo: '',
            icType: '',
            icNum: '',
            icAddress: '',
            birthday: '',
            politicCountenance: '',
            maritalStatus: '',
            fertilityStatus: '',
            residence: '',
            eduType: '',
            eduSchool: '',
            eduMajor: '',
            eduDate: '',
            workBeginDate: '',
            joinCompanyId: '',
            joinCompanyName: '',
            joinDeptId: '',
            joinDeptName: '',
            joinDate: '',
            joinAddr: '',
            departureDate: '',
            contractType: '',
            postName: '',
            postRank: '',
            levelId: '',
            contractBeginDate: '',
            contractFinalDate: '',
            insureAddr: '',
            insureBegin: '',
            insureAccount: '',
            insureBank: '',
            insureTaxPolicy: '',
            linkName: '',
            linkPhone: '',
            linkRelation: ''
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

        toDetail: function (userId) {
            var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });

            $.ajax({
                type: "POST",
                url: baseURL + "app/contact/detail",
                data: {
                    "userId": userId
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

        photoHandler:function(photo){
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }
            return photo;
        },

        /**
         * 从url里面分析出对应的参数
         * @param name
         * @returns {string}
         */
        getQueryString: function () {
            var queryObj={};
            var reg=/[?&]([^=&#]+)=([^&#]*)/g;
            var querys=window.location.search.match(reg);
            if(querys){
                for(var i in querys){
                    var query=querys[i].split('=');
                    var key=query[0].substr(1),
                        value=query[1];
                    queryObj[key]?queryObj[key]=[].concat(queryObj[key],value):queryObj[key]=value;
                }
            }
            return queryObj;

        },


    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        var urlParams = this.getQueryString();
        printLog("urlParams="+JSON.stringify(urlParams));
        var id = urlParams.id;
        printLog("id="+id);

        this.toDetail(id);
    }
});

