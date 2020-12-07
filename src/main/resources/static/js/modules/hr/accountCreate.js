
function init(){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/hr/accountCreate/init",
        data: {},
        success: function(r){
            //layer.close(layerIndex);
            if(r.code == 0){
                vm.entity = r.entity;
                vm.deptList = r.deptList;
                vm.hasPermission = r.hasPermission;

                if(!vm.hasPermission){
                    layer.open({
                        content: '您尚未被赋予创建账号的权限'
                        ,btn: ['我知道了']
                        ,yes: function(index){
                            layer.close(index);
                            onBackClick();
                        }
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
            account : '',
            password : '',
            confirmPass : '',
            code : '',
            userName : '',
            email:'@riveretech.com',

            deptId : '',
            joinCompanyName : '北京江融信科技有限公司',
            joinAddr : '',
            joinDate : '',
            positiveDate:'',
            probationPeriod:'3',
            postName : '',
            postCategory:'T',
            postLevel:'',

            postRank: '',
            levelId: '',

            contractType:'全职',
            contractBeginDate:'',
            contractFinalDate:''

        },

        deptList:[],

        hasPermission:false


    },
    methods: {

        saveEntity: function(){
            if(vm.ajaxLock){
                return ;
            }
            vm.ajaxLock = true;

            var layerIndex = layer.open({
                type: 2
                ,content: '正在提交...'
            });

            var url = "app/hr/accountCreate/add";

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
                                onBackClick();
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

            if(tag=="entity.joinDate"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.entity.joinDate = day;
                        },
                        fallback:function(){
                            vm.entity.joinDate = "";
                        }
                    });
            } else if(tag=="entity.positiveDate"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.entity.positiveDate = day;
                        },
                        fallback:function(){
                            vm.entity.positiveDate = "";
                        }
                    });
            } else if(tag=="entity.contractBeginDate"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.entity.contractBeginDate = day;
                        },
                        fallback:function(){
                            vm.entity.contractBeginDate = "";
                        }
                    });
            } else if(tag=="entity.contractFinalDate"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.entity.contractFinalDate = day;
                        },
                        fallback:function(){
                            vm.entity.contractFinalDate = "";
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
        init();
    }
});

