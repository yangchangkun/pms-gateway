
function loadGridData(queryParam) {
    $.ajax({
        type: "POST",
        url: baseURL + "app/contact/select",
        data: queryParam,
        success: function (r) {
            if (r.code == 0) {
                vm.gridDatas = r.gridDatas;
            }
        }
    });
}

/**
 * 用户选择之后的回调父iframe的方法
 * @param account
 */
function contactChoiceCallback(userObj){
    //在iframe中调用父页面中定义的方法
    parent.contactChoiceCallback(userObj);
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

        entity: {
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

        queryParam: {
            searchKey: '',
            status:''
        },

        gridDatas: []

    },
    methods: {

        search: function () {
            /*if(isBlank(vm.queryParam.searchKey)){
                layer.open({
                    content: '请输入您要搜索的关键字'
                    ,btn: '我知道了'
                });
                return ;
            }*/
            loadGridData(vm.queryParam);
        },

        toDetail: function (obj) {
            contactChoiceCallback(obj);
        },

        photoHandler:function(photo){
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }
            return photo;
        }
    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        var urlParams = getQueryString();
        printLog("urlParams="+JSON.stringify(urlParams));
        var status = urlParams.status;
        printLog("status="+status);

        var queryParam = {
            searchKey: '',
            status: status
        };
        this.queryParam = queryParam;

        loadGridData(queryParam);
    }
});

