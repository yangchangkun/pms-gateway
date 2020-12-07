

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

        password: '',
        newPassword: '',
        confirmPass: '',

    },
    methods: {

        updatePass: function () {
            if(vm.validator()){
                return ;
            }

            var layerIndex = layer.open({
                type: 2
                ,content: '加载中'
            });

            $.ajax({
                type: "POST",
                url: baseURL + "app/uc/updatePass",
                data: {
                    "password": vm.password,
                    "newPassword": vm.newPassword,
                    "confirmPass": vm.confirmPass
                },
                success: function (r) {
                    layer.close(layerIndex);
                    if (r.code == 0) {
                        layer.open({
                            content: '密码修改成功,请重新登录'
                            ,btn: ['我知道了']
                            ,yes: function(index){
                                layer.close(index);
                                vm.logOut();
                            }
                        });
                        vm.logOut();
                    } else {
                        layer.open({
                            content: r.msg,
                            btn: '我知道了'
                        });
                    }
                },
                error: function (data) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);
                    layer.open({
                        content: '请求出错，请稍后再试！',
                        btn: '我知道了'
                    });
                }
            });

        },

        logOut:function(){
            $.ajax({
                type: "POST",
                url: baseURL + "app/logout",
                dataType: "json",
                success: function(r){
                    exitSystem();
                }
            });
        },

        validator: function () {
            if(isBlank(vm.password)){
                layer.open({
                    content: '请输入原密码'
                    ,skin: 'msg'
                    ,time: 2 //2秒后自动关闭
                });
                return true;
            }
            if(isBlank(vm.newPassword)){
                layer.open({
                    content: '请输入新密码'
                    ,skin: 'msg'
                    ,time: 2 //2秒后自动关闭
                });
                return true;
            }
            if(isBlank(vm.confirmPass)){
                layer.open({
                    content: '请再次输入新密码'
                    ,skin: 'msg'
                    ,time: 2 //2秒后自动关闭
                });
                return true;
            }
            if(vm.newPassword != vm.confirmPass){
                layer.open({
                    content: '两次新密码输入不一致'
                    ,skin: 'msg'
                    ,time: 2 //2秒后自动关闭
                });
                return true;
            }

            var reg=/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[\s\S]{6,16}$/;
            if(!reg.test(vm.newPassword)){
                layer.open({
                    content: '密码必须由6-16位含大小写字母、数字的字符串组成！'
                    ,skin: 'msg'
                    ,time: 2 //2秒后自动关闭
                });
                return true
            }

            return false;
        },

    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {

    }
});

