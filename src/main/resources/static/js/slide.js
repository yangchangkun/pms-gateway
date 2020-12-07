$(document).ready(function() {
    let hero = document.getElementById('hero-slides');
    //let menu = document.getElementById('menu');
    let slides = document.getElementById('slides');
    //let next = [ 'next', 'next-catch' ].map(n => document.getElementById(n));
    //let prev = [ 'prev', 'prev-catch' ].map(n => document.getElementById(n));
    let slideChildren = slides.children;
    let slideCount = slides.children.length;
    let currentlyDemoing = false;
    let currentPage = 0;
    let slidesPerPage = () => window.innerWidth > 1700 ? 4 : window.innerWidth > 1200 ? 3 : 2;
    let maxPageCount = () => slideCount / slidesPerPage() - 1;



    function goToPage(pageNumber) {
        //currentPage = Math.min(maxPageCount(), Math.max(0, pageNumber));
        //alert(slidesPerPage)
        //alert(maxPageCount);
        //alert(currentPage);
        pageNumber=pageNumber<0?0:pageNumber;
        pageNumber=pageNumber>slideCount-1?slideCount-1:pageNumber;

        currentPage=pageNumber;

        let currentPageMove=(1.1245*8/9)*pageNumber;
        hero.style.setProperty('--page', currentPageMove);
    }

    function sleep(time) {
        return new Promise(res => setTimeout(res, time));
    }

    function hoverSlide(index) {
        index in slideChildren &&
        slideChildren[index].classList.add('hover');
    }

    function unhoverSlide(index) {
        index in slideChildren &&
        slideChildren[index].classList.remove('hover');
    }

    async function demo() {
        if(currentlyDemoing) {
            return;
        }
        currentlyDemoing = true;
        if(currentPage !== 0) {
            goToPage(0);
            await sleep(800);
        }
        goToPage(0);
        //unhoverSlide(slideSeq[2]);
        currentlyDemoing = false;
        //alert();
    }

   // next.forEach(n => n.addEventListener('click', () => !currentlyDemoing && goToPage(currentPage + 1)));
   // prev.forEach(n => n.addEventListener('click', () => !currentlyDemoing && goToPage(currentPage - 1)));
    //menu.addEventListener('click', demo);
    sleep(50).then(demo);
    var hm = new Hammer(hero);
    hm.on("swipeleft", function (event) {
        if(!currentlyDemoing){
            goToPage(currentPage + 1);
        }
    });
    hm.on("swiperight", function (event) {
        if(!currentlyDemoing){
            goToPage(currentPage - 1);
        }
    });

});
