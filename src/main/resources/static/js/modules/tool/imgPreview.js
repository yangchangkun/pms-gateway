

var vm = new Vue({
    el: '#vueApp',
    data: {

        dialogIndex: null,

        title: null,

        imgUrl:'',

    },
    methods: {

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
        var imgUrl = decodeURIComponent(urlParams.imgUrl);
        printLog("imgUrl="+imgUrl);

        this.imgUrl = imgUrl;

    }
});

