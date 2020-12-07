#!/usr/bin/env bash

#定义启动的环境变量
DEV_ENV=(dev pro test local)

#用户输入的环境变量是否在定义范围内，如果不是退出
EVN=""
for i in ${DEV_ENV[@]}
do
   if [[ "$1" = "$i"  ]]
   then
       EVN=$i;
       break;
   fi
done

if [[ $EVN = "" ]]
then
   echo "enter env param [ ${DEV_ENV[@]} ]"
   exit 0
fi

#JDK路径
#JAVA_HOME=""

#利用pwd命令获取当前工程目录，实际获取到的是该shell脚本的目录。再利用sed命令将/bin替换为空
Project_HOME=$(echo `pwd` | sed 's/\/sbin//')

LOG_DIR=/data/logs/pms/gateway
APPLICATION_MAIN=com.pms.PmsGateWayApplication
CLASSPATH=$Project_HOME/classes

#GC日志参数
#-XX:+PrintGC 输出GC日志
#-XX:+PrintGCDetails 输出GC的详细日志
#-XX:+PrintGCTimeStamps 输出GC的时间戳（以基准时间的形式）
#-XX:+PrintGCDateStamps 输出GC的时间戳（以日期的形式，如 2013-05-04T21:53:59.234+0800）
#-XX:+PrintHeapAtGC 在进行GC的前后打印出堆的信息
#-Xloggc:../logs/gc.log 日志文件的输出路径
#GC_OPTS=""
GC_OPTS=" -XX:+PrintGCDetails -XX:+PrintGCDateStamps  -Xloggc:${LOG_DIR}/gc.log "

#内存溢出记录dump文件
#HEAP_DUMP=""
HEAP_DUMP=" -XX:+HeapDumpOnOutOfMemoryError  -XX:HeapDumpPath=${LOG_DIR}/heap_dump.bin "



#JVM启动参数
#-server:一定要作为第一个参数,在多个CPU时性能佳
#-Xloggc:记录GC日志,这里建议写成绝对路径,如此便可在任意目录下执行该shell脚本
#JAVA_OPTS="-ms512m -mx512m -Xmn256m -Djava.awt.headless=true -XX:MaxPermSize=128m"
JAVA_OPTS="-Dspring.profiles.active=$EVN -Duser.timezone=GMT+8 -server -Xms512m -Xmx1024m ${GC_OPTS} ${HEAP_DUMP} "


for jarfile in "$Project_HOME"/lib/*.jar
do
   CLASSPATH="$CLASSPATH":"$jarfile"
done



#-------------------------------------------------------------------------------------------------------------
#getPID()-->获取Java应用的PID
#说明:通过JDK自带的JPS命令及grep命令,准确查找Java应用的PID
#其中:[jps -l]表示显示Java主程序的完整包路径
#     awk命令可以分割出PID($1部分)及Java主程序名称($2部分)
#例子:[$jps -l | grep $APPLICATION_MAIN]-->>[5775 jrx.anytxn.cms.App]
#另外:用这个命令也可以直接取到程序的PID-->>[ps aux|grep java|grep $APPLICATION_MAIN|grep -v grep|awk '{print $2}']
#-------------------------------------------------------------------------------------------------------------
#初始化全局变量tradePortalPID,用于标识交易前置系统的PID,0表示未启动
TPID=0

getPID(){
    javaps=`jps -l | grep $APPLICATION_MAIN`
    if [ -n "$javaps" ]; then
        TPID=`echo $javaps | awk '{print $1}'`
    else
        TPID=0
    fi
}

genDir(){
    #创建文件夹
    if [ ! -d "$LOG_DIR" ]; then
        mkdir "$LOG_DIR"
    fi
}
startup(){
    getPID
    genDir
    echo "================================================================================================================"
    if [ $TPID -ne 0 ]; then
        echo "$APP_MAIN already started(PID=$TPID)"
        echo "================================================================================================================"
    else
        echo -n "Starting $APPLICATION_MAIN"
        nohup java $JAVA_OPTS -classpath $CLASSPATH $APPLICATION_MAIN >> /dev/null 2>&1 &
        getPID
        if [ $TPID -ne 0 ]; then
            echo "(PID=$TPID)...[Success]"
            echo "================================================================================================================"
        else
            echo "[Failed]"
            echo "================================================================================================================"
        fi
    fi
}

startup

# 检测到tomcat启动完成以后退出tail命令
tail -f $LOG_DIR/nohup.log | sed '/app server run end/Q'

# 检查TPID, 并且输出到日志文件中
getPID
if [ $TPID -ne 0 ]; then
    echo "$APPLICATION_MAIN (PID=$TPID)...[Success]" 2>&1 | tee -a $LOG_DIR/nohup.log
    echo "================================================================================================================"
else
    echo "$APPLICATION_MAIN ...[Failed]" 2>&1 | tee -a $LOG_DIR/nohup.log
    echo "================================================================================================================"
fi