function loadDetail(id){
    /*var layerIndex = layer.open({
        type: 2
        ,content: '加载中...'
    });*/

    $.ajax({
        type: "POST",
        url: "/pms/app/adm/cardApply/myCard",
        data: {
            id:id
        },
        success: function(r){
            //layer.close(layerIndex);
            //console.log("success.r="+JSON.stringify(r));

            if(r.code == 0){
                vm.entity = r.entity;
                vm.platCompany = r.platCompany;

                document.title = vm.entity.cardNameCn+"的名片,敬请惠存.";
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
            applyUserId : '',
            applyDate : '',

            cardNameCn : '',
            cardNameEn : '',
            deptName : '',
            deptNameEn : '',
            postName : '',
            postNameEn : '',
            phone : '',
            email:'',
            printPeriod:'0',
            printQuantity:'1',
            memo:'',

            state : '0',
            workflowId : '',
            qrCodeUrl: '',

            createBy : '',
            createTime : '',
            updateBy : '',
            updateTime : '',

            applyUserName : '',


        },

        platCompany:{
            companyId : '',
            companyName : '',
            companyShortName : '',

            website : '',
            webHost : '',
            gatewayHost : '',

            logoIcon : '',
            guideIcon : '',
            slogan : '',
            industry : '',
            province : '',
            city : '',
            registerDay : '',
            registerAddr : '',
            registerAmount : '',
            businessScope : '',
            businessState : '',
            linkName : '',
            linkPhone : '',
            introduce : ''
        },


    },

    filters:{
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

        phoneHandler:function(phone){
            if(isBlank(phone)){
                return "#";
            }
            return "tel:"+phone;
        },

        emailHandler:function(phone){
            if(isBlank(phone)){
                return "#";
            }
            return "mailto:"+phone;
        },

        photoHandler:function(photo){
            if(isBlank(photo)){
                return "../img/me_avatar.png";
            }

            return photo;
        },

        /**
         * 图片预览
         * @param imgUrl
         */
        imgPreview: function(imgUrl){
            openWin(imgUrl);
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
        console.log("urlParams="+JSON.stringify(urlParams));
        var id = urlParams.id;
        console.log("id="+id);

        loadDetail(id);

    }
});

