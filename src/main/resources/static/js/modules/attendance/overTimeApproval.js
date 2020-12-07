function loadDetail(id){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: baseURL + "app/attend/overtime/detail",
        data: {
            overtimeId:id
        },
        success: function(r){
            //layer.close(layerIndex);
            if(r.code == 0){
                vm.entity = r.entity;
                vm.details = r.details;
                vm.actHis = r.actHis;
                vm.ossList = r.ossList;

                vm.actImgHref = baseURL + "app/common/act/getActivitiProccessImage?processInstanceId="+vm.entity.workflowId;
                vm.actImgUrl = baseURL + "app/common/act/getActivitiProccessImage?processInstanceId="+vm.entity.workflowId;
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

        dialogIndex:null,

        title: null,

        showList:true,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock:false,


        entity:{
            id : '',
            proId : '',
            startTime : '',
            endTime : '',
            duration : '0',
            type : '',
            reason : '',
            state : '0',
            workflowId : '',
            createUserId : '',
            createTime : '',
            updateUserId : '',
            updateTime : '',
            proName:'',
            createUserName:''
        },
        //班次详情列表
        details:[],
        //审批列表
        actHis:[],

        actImgHref:'#',
        actImgUrl:'',

        //附件列表
        ossList:[],

        approvalEntity:{
            taskId:'',
            opinion:0,
            memo:''
        },
    },

    filters:{
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        splitLine:function(value){
            if(isBlank(value)){
                return "";
            }
            return value.replace(new RegExp(',','g'), "<br/>");
        },
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        newLine:function(value){
            if(isBlank(value)){
                return "";
            }
            return value.replace(/\n/g, "<br/>");
        }
    },

    methods: {

        approval: function () {
            if (vm.approvalEntity.opinion == 1 && isBlank(vm.approvalEntity.memo)) {
                layer.open({
                    content: "驳回时必须填写原因",
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

            var url = "app/attend/approval/handle";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                data: vm.approvalEntity,
                success: function (r) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    if (r.code == 0) {
                        layer.open({
                            content: '操作成功'
                            ,btn: ['我知道了']
                            ,yes: function(index){
                                layer.close(index);
                                /**
                                 * 关闭
                                 */
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
        var id = urlParams.id;
        printLog("id="+id);
        var taskId = urlParams.taskId;
        printLog("taskId="+taskId);

        this.approvalEntity.taskId = taskId;

        loadDetail(id);

    }
});

