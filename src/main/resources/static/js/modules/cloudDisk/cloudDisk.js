var fileUploadLayerIndex;

function initFileUpload(){
    new AjaxUpload('#uploadFile', {
        action: baseURL + 'app/common/upload?token=' + globalToken ,
        name: 'file',
        data:{
            "resType":"document",
            "resKey":""
        },
        autoSubmit:true,
        responseType:"json",
        onSubmit:function(file, extension){
            /*if (!(extension && /^(jpg|jpeg|png|gif)$/.test(extension.toLowerCase()))){
                layer.open({
                    content: '只支持jpg、png、gif格式的图片',
                    btn: '我知道了'
                });
                return false;
            }*/
            fileUploadLayerIndex = layer.open({
                type: 2
                ,content: '文件上传中...'
            });
        },
        onComplete : function(file, r){
            layer.close(fileUploadLayerIndex);
            if(r.code == 0){
                vm.addFile(r.oss);
            }else{
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        }
    });
}


function loadDiskData(dirId) {
    var layerIndex = layer.open({
        type: 2
        ,content: '加载中'
    });

    $.ajax({
        type: "POST",
        url: baseURL + "app/clouddisk/disk/disk",
        data: {
            dirId:dirId
        },
        success: function (r) {
            layer.close(layerIndex);
            if (r.code == 0) {
                vm.currDirId = dirId;

                vm.dirEntity = r.dirEntity;
                vm.ownerList = r.ownerList;
                vm.childDirList = r.childDirList;
                vm.attachmentList = r.attachmentList;
            } else {
                layer.open({
                    content: r.msg,
                    btn: '我知道了'
                });
            }
        }
    });
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

        /**
         * 当前目录ID
         */
        currDirId : '0',

        /**
         * 当前目录对象
         */
        dirEntity: {
            id : '',
            parentId : '',
            name : '',
            category : '',
            type : '',
            state : '',
            createBy : '',
            createTime : '',
            updateBy : '',
            updateTime : '',
            userName : ''
        },

        /**
         * 添加或修改时使用的目录对象
         */
        dirBean:{
            dirId : '',
            parentDirId : '0',
            dirName : '',
            dirType : 0
        },

        /**
         * 当前目录管理员
         */
        ownerList: [],

        /**
         * 下级目录列表
         */
        childDirList:[],

        /**
         * 文件列表
         */
        attachmentList:[]

    },
    methods: {
        /**
         * 返回上级目录
         */
        toParentDir: function () {
            loadDiskData(vm.dirEntity.parentId);
        },

        /**
         * 跳转到下级目录
         */
        toChildDir: function (dirId) {
            loadDiskData(dirId);
        },

        /**
         * 刷新目录下的文件
         */
        refreshDir:function(){
            if (vm.ajaxLock) {
                return;
            }
            vm.ajaxLock = true;
            /*var layerIndex = layer.open({
                type: 2
                ,content: '加载中...'
            });*/

            var param = {
                dirId : vm.currDirId,
            };

            var url = "app/clouddisk/disk/childDirList";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/x-www-form-urlencoded",
                data: param,
                success: function (r) {
                    vm.ajaxLock = false;
                    //layer.close(layerIndex);

                    if (r.code === 0) {
                        vm.childDirList = r.childDirList;
                    } else {
                        layer.open({
                            content: r.msg,
                            btn: '我知道了'
                        });
                    }
                },
                error: function (data) {
                    vm.ajaxLock = false;
                    //layer.close(layerIndex);
                    layer.open({
                        content: '请求出错，请稍后再试！',
                        btn: '我知道了'
                    });
                }
            });
        },

        /**
         * 添加目录
         */
        toAddDir(){
            vm.dirBean = {
                dirId : '',
                parentDirId : vm.currDirId,
                dirName : '',
                dirType : 0
            };

            vm.title = "新增目录";

            $('#dirModal').modal('show');
        },

        /**
         * 编辑目录
         */
        toEditDir:function(obj){
            vm.dirBean = {
                dirId : obj.id,
                parentDirId : obj.parentId,
                dirName : obj.name,
                dirType : obj.type
            }

            vm.title = "修改目录";

            $('#dirModal').modal('show');
        },

        editDir:function(){
            if (vm.ajaxLock) {
                return;
            }
            vm.ajaxLock = true;
            var layerIndex = layer.open({
                type: 2
                ,content: '正在提交...'
            });

            var url = isBlank(vm.dirBean.dirId) ? "app/clouddisk/disk/addDir" : "app/clouddisk/disk/editDir";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/x-www-form-urlencoded",
                data: vm.dirBean,
                success: function (r) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);

                    if (r.code === 0) {
                        layer.open({
                            content: '操作成功'
                            ,skin: 'msg'
                            ,time: 2 //2秒后自动关闭
                        });
                        $('#dirModal').modal('hide');
                        vm.refreshDir();
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

        /**
         * 目录删除提示框
         * @param dirId
         */
        toDelDirDialog:function(dirId){
            layer.open({
                content: '您确定要删除该目录吗？'
                ,btn: ['确定', '取消']
                ,yes: function(index){
                    vm.delDir(dirId);
                }
            });
        },

        /**
         * 删除目录
         */
        delDir: function (dirId) {
            if (vm.ajaxLock) {
                return;
            }
            vm.ajaxLock = true;
            var layerIndex = layer.open({
                type: 2
                ,content: '正在提交...'
            });

            var param = {
                dirId : dirId,
            };

            var url = "app/clouddisk/disk/removeDir";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/x-www-form-urlencoded",
                data: param,
                success: function (r) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);

                    if (r.code === 0) {
                        layer.open({
                            content: '目录删除成功'
                            ,skin: 'msg'
                            ,time: 2 //2秒后自动关闭
                        });
                        vm.refreshDir();
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




        /**
         * 刷新目录下的文件
         */
        refreshDirFile:function(){
            if (vm.ajaxLock) {
                return;
            }
            vm.ajaxLock = true;
            /*var layerIndex = layer.open({
                type: 2
                ,content: '加载中...'
            });*/

            var param = {
                dirId : vm.currDirId,
            };

            var url = "app/clouddisk/attachment/list";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/x-www-form-urlencoded",
                data: param,
                success: function (r) {
                    vm.ajaxLock = false;
                    //layer.close(layerIndex);

                    if (r.code === 0) {
                        vm.attachmentList = r.attachmentList;
                    } else {
                        layer.open({
                            content: r.msg,
                            btn: '我知道了'
                        });
                    }
                },
                error: function (data) {
                    vm.ajaxLock = false;
                    //layer.close(layerIndex);
                    layer.open({
                        content: '请求出错，请稍后再试！',
                        btn: '我知道了'
                    });
                }
            });
        },

        /**
         * 添加文件
         */
        addFile:function(oss){
            if (vm.ajaxLock) {
                return;
            }
            vm.ajaxLock = true;
            var layerIndex = layer.open({
                type: 2
                ,content: '正在提交...'
            });

            var param = {
                dirId : vm.currDirId,
                accessory : JSON.stringify(oss)
            };

            var url = "app/clouddisk/attachment/addFile";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/x-www-form-urlencoded",
                data: param,
                success: function (r) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);

                    if (r.code === 0) {
                        layer.open({
                            content: '文件上传成功'
                            ,skin: 'msg'
                            ,time: 2 //2秒后自动关闭
                        });
                        vm.refreshDirFile();
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

        /**
         * 删除文件
         */
        toDelFileDialog:function(fileId){
            layer.open({
                content: '您确定要删除该文件吗？'
                ,btn: ['确定', '取消']
                ,yes: function(index){
                    layer.close(index);
                    vm.delFile(fileId);
                }
            });
        },
        delFile:function(fileId){
            if (vm.ajaxLock) {
                return;
            }
            vm.ajaxLock = true;
            var layerIndex = layer.open({
                type: 2
                ,content: '正在提交...'
            });

            var param = {
                dirId : vm.currDirId,
                fileId : fileId
            };

            var url = "app/clouddisk/attachment/delFile";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/x-www-form-urlencoded",
                data: param,
                success: function (r) {
                    vm.ajaxLock = false;
                    layer.close(layerIndex);

                    if (r.code === 0) {
                        layer.open({
                            content: '文件删除成功'
                            ,skin: 'msg'
                            ,time: 2 //2秒后自动关闭
                        });
                        vm.refreshDirFile();
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

        /**
         * 分配管理员
         */
        allocationOwner:function(){

        },

        goBack:function(){
            if(isBlank(vm.dirEntity.parentId)){
                onBackClick();
            } else {
                vm.toParentDir();
            }
        },


        /**
         * 下载文件
         * @param ossUrl
         */
        downLoadFile: function(ossUrl){
            openImgPreviewWin(ossUrl);
        },

    },
    created: function () {

    },
    updated: function () {

    },
    mounted: function () {
        this.currDirId = "0";

        loadDiskData("0");
    }
});

