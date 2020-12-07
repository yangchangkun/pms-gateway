

var vm = new Vue({
    el: '#vueApp',
    data: {

        dialogIndex: null,
        flag : 0,//判断是否显示提交按钮的标志
        title: null,

        /**
         * 定义操作锁
         * 用于判断ajax重复请求
         * 默认为不锁定，只要已进入post请求就设置为true，直到一次请求完成
         */
        ajaxLock: false,

        day:'', //当前选中的日期

        //创建工时的列表信息
        dailyGridDatas:[],

    },
    methods: {
        getCreateInfoList: function(day){
            var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });

            var params = {
                "day": day
            }

            var url = "app/daily/apply/dayDailyInit";
            $.ajax({
                url: baseURL + url,
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: params,
                success: function(r){
                    layer.close(layerIndex);

                    if(r.code == 0){
                        vm.dailyGridDatas = r.dailyList;
                        //判断是否全部审批通过
                        if(vm.dailyGridDatas!=null && vm.dailyGridDatas.length>0){
                            for(var j=0;j<vm.dailyGridDatas.length;j++){
                                if(vm.dailyGridDatas[j].status=='2'){
                                    vm.flag = 1;
                                }else if(vm.dailyGridDatas[j].status!='2'&& vm.dailyGridDatas[j].status!='-1'){
                                    vm.flag = 0;
                                    break;
                                }
                            }
                        }
                    }else{
                        layer.open({
                            content: r.msg
                            ,btn: ['我知道了']
                            ,yes: function(index){
                                layer.close(index);
                                onBackClick();
                            }
                        });

                    }
                }
            });
        },

        setDailySwitch:function(idx){
            var oldSwitch = vm.dailyGridDatas[idx].dailySwitch;
            if(oldSwitch==0){
                vm.dailyGridDatas[idx].dailySwitch = 1;
            } else {
                vm.dailyGridDatas[idx].dailySwitch = 0;
            }
        },


        //提交工时
        saveDaily: function(){
            if(vm.dailyGridDatas.length<0){
                layer.open({
                    content: '请填写工时',
                    btn: '我知道了'
                });
                return true;
            }
            if(vm.dailyGridDatas!=null && vm.dailyGridDatas.length>0) {
                for (var i = 0; i < vm.dailyGridDatas.length; i++) {
                    if(!vm.dailyGridDatas[i].dailySwitch && (vm.dailyGridDatas[i].jobContent=="")){
                        layer.open({
                            content: "第"+(i+1)+"项报工内容不可以为空",
                            btn: '我知道了'
                        });
                        return true;
                    }
                }
            }

            if(vm.ajaxLock){
                return ;
            }
            vm.ajaxLock = true;

            var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });

            var url = "app/daily/apply/saveDaily";
            $.ajax({
                url: baseURL + url,
                type: "POST",
                contentType: "application/x-www-form-urlencoded",
                data: {
                    "day": vm.day,
                    "dailyJArr":JSON.stringify(vm.dailyGridDatas)
                },
                success: function(r){
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    if(r.code == 0){
                        layer.open({
                            content: '操作成功'
                            ,btn: ['我知道了']
                            ,yes: function(index){
                                layer.close(index);
                                vm.getCreateInfoList(vm.day);
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


    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        var urlParams = getQueryString();
        printLog("urlParams="+JSON.stringify(urlParams));
        var day = urlParams.day;
        printLog("day="+day);

        this.day = day;

        this.getCreateInfoList(day);
    }
});

