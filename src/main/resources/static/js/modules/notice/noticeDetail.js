

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
            id : '',
            title : '',
            issueBy : '',
            content : '',
            topFlag : '',
            secretFlag : '',
            accessoryFlag : '',
            state : '',
            createBy : '',
            createTime : '',
            updateBy : '',
            updateTime : ''
        },

        /**
         * 附件信息
         */
        attachmentDatas: []

    },

    filters:{
        /**
         * 替换换行符
         * 注意经过该函数替换后的文本要使用v-html输出才有效果
         * @param value
         * @returns {*}
         */
        newLine:function(value){
            return value.replace(/\n/g, "<br/><span style='margin-right: 25px;'></span>");
        }
    },

    methods: {

        toDetail: function (noticeId) {
            /*var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });*/

            $.ajax({
                type: "POST",
                url: baseURL + "app/adm/notice/detail",
                data: {
                    "noticeId": noticeId
                },
                success: function (r) {
                    //layer.close(layerIndex);
                    if (r.code == 0) {
                        vm.entity = r.entity;
                        vm.attachmentDatas = r.attachmentList;
                    } else {
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

