
function loadGridData(queryParam){
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    $.ajax({
        type: "POST",
        url: baseURL + "app/daily/approval/myApprovalList",
        data: queryParam,
        success: function(r){
            layer.close(layerIndex);
            if(r.code == 0){
                vm.gridDatas = r.gridDatas;
                vm.webPage = r.webPage;
            }else{
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
}

/**
 * 加载默认抄送人信息
 */
function loadMyAuthProjectList(){
    var param = {

    }

    $.ajax({
        type: "POST",
        url: baseURL + "app/pro/authProjectList",
        data: param,
        success: function(r){
            if(r.code == 0){
                vm.myAuthProjectList = r.list;
            }
        }
    });
}

var vm = new Vue({
    el:'#vueApp',
    data:{
        dialogIndex:null,

        title: null,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock:false,

        queryParam:{
            day:'',
            proId:'',
            userKey:'',

            pageNum:1,
            pageSize:10,
        },

        webPage:{
            pageNum:0,
            pageSize:0,
            size:0,
            startRow:0,
            endRow:0,
            total:0,
            pages:0,
            prePage:0,
            nextPage:0,
            isFirstPage:false,
            isLastPage:false,
            hasPreviousPage:false,
            hasNextPage:false,
            navigatePages:'',
            navigatepageNums:[],
            navigateFirstPage:0,
            navigateLastPage:0
        },

        gridDatas:[],

        /**
         * 全选和反选的标识
         */
        checked:false,
        /**
         * 审批复选框数组
         */
        checkArray:[],
        /**
         * 审批驳回时的结论
         */
        memo:'',

        myAuthProjectList:[],
    },

    filters:{
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        newLine:function(value){
            return value.replace(/\n/g, "<br/>");
        }
    },

    methods: {
        resetQueryParams:function(){
            vm.queryParam = {
                day:'',
                proId:'',
                userKey:'',

                pageNum:1,
                pageSize:10,
            }
            vm.queryList(1);
        },

        queryList: function (page) {
            sidebar.close();

            vm.checkArray = [];
            vm.queryParam.pageNum = page;
            loadGridData(vm.queryParam);
        },

        reload:function(){
            if(vm.dialogIndex!=null){
                layer.close(vm.dialogIndex);
            }
            vm.queryList(1);
        },

        approval: function (state) {
            if(vm.checkArray==null || vm.checkArray.length<=0){
                layer.open({
                    content: "请选择要审批的日报",
                    btn: '我知道了'
                });
                return ;
            }
            if (state == 1 && isBlank(vm.memo)) {
                layer.open({
                    content: "驳回日报时必须填写原因",
                    btn: '我知道了'
                });
                return ;
            }

            if(vm.ajaxLock){
                return ;
            }
            vm.ajaxLock = true;

            var layerIndex = layer.open({
                type: 2
                ,content: '处理中'
            });

            var url = "app/daily/approval/batchApprove";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                data: {
                    state: state,
                    memo:vm.memo,
                    checkArray:vm.checkArray.toString()
                },
                success: function (r) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    if (r.code == 0) {
                        //提示
                        layer.open({
                            content: '操作成功'
                            ,skin: 'msg'
                            ,time: 2 //2秒后自动关闭
                        });

                        $('#rejectModal').modal('hide')

                        vm.reload();
                    } else {
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
                        content: '请求出错，请稍后再试！',
                        btn: '我知道了'
                    });
                }
            });

        },

        checkedAll: function() {
            if (vm.checked) { //实现反选
                vm.checkArray = [];
            } else { //实现全选
                vm.checkArray = [];
                if(vm.gridDatas!=null && vm.gridDatas.length>0){
                    for(var i=0;i<vm.gridDatas.length;i++){
                        vm.checkArray.push(vm.gridDatas[i].id);
                    }
                }
            }
        },

        validator: function () {
            return  false;
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

            if(tag=="queryParam.day"){
                var iosSelect = new IosSelect(3,
                    [yckYearData, yckMonthData, yckDateData],
                    {
                        title: '日期选择',
                        itemHeight: 35,
                        oneLevelId: yckNowYear,
                        twoLevelId: yckNowMonth,
                        threeLevelId: yckNowDate,
                        fourLevelId: 0,
                        showLoading: true,
                        callback: function (selectOneObj, selectTwoObj, selectThreeObj) {
                            var day = selectOneObj.value + '-' + selectTwoObj.value + '-' + selectThreeObj.value;
                            vm.queryParam.day = day;
                        },
                        fallback:function(){
                            vm.queryParam.day = "";
                        }
                    });
            }

        },

    },
    created: function(){

    },
    updated: function(){

    },
    mounted: function(){
        this.queryParam = {
            day:'',
            proId:'',
            userKey:'',

            pageNum:1,
            pageSize:10,
        }
        loadGridData(this.queryParam);

        loadMyAuthProjectList();
    }
});

