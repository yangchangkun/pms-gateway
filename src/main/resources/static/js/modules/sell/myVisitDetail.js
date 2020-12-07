function loadDetail(id){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/
    $.ajax({
        type: "POST",
        url: baseURL + "app/crmVisit/detail",
        data: {
            id:id
        },
        success: function(r){
            //layer.close(layerIndex);
            if(r.code == 0){
                vm.entity = r.entity;
                vm.ossList = r.ossList;
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
            custId : '',
            custShortName : '',
            custStakeholderId : '',
            visitMode : '',
            visitBrief : '',
            visitParticipants : '0',
            visitDescribe : '',
            visitConclusion : '',
            visitPlan : '0',
            visitAdvise : '',
            visitState : '',
            createUserId : '',
            createTime:'',
            updateUserId : '',
            updateTime : '',
            createName:'',
            leaderName:'',
            holderName:'',
            visitTime:'',
            firstTime:'',
            weekNewCust:'',
            manyTimes:'',
            sellStatu:'',
            visitQuest:'',
            visitCompleteDay:'',
            memo:''
        },
        //班次详情列表
        details:[],
        //审批列表
        actHis:[],

        actImgHref:'#',
        actImgUrl:'',

        //当前审批候选人列表
        candidateList:[],

        //附件列表
        ossList:[],

        revokeTag: '0', //撤销标志:0 不可撤销，1 可撤销
        revokeMemo:'', //撤销原因
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

        revokeApply: function(){
            if(isBlank(vm.revokeMemo)){
                layer.open({
                    content: "请输入撤销原因",
                    btn: '我知道了'
                });
                return ;
            }

            var param = {
                id:vm.entity.id,
                memo:vm.revokeMemo
            };

            if(vm.ajaxLock){
                return ;
            }
            vm.ajaxLock = true;


            var layerIndex = layer.open({
                type: 2
                ,content: '正在提交...'
            });

            var url = "app/attend/travel/revoke";

            $.ajax({
                url: baseURL + url,
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: param,
                success: function(r){
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    if(r.code == 0){
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

        loadDetail(id);

    }
});

