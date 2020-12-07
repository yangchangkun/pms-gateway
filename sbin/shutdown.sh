#!/usr/bin/env bash


#JDK路径
#JAVA_HOME=""

#利用pwd命令获取当前工程目录，实际获取到的是该shell脚本的目录。再利用sed命令将/bin替换为空
Project_HOME=$(echo `pwd` | sed 's/\/sbin//')

LOG_DIR=/data/logs/pms/gateway
APPLICATION_MAIN=com.pms.PmsGateWayApplication
CLASSPATH=$Project_HOME/classes

#JVM启动参数
#-server:一定要作为第一个参数,在多个CPU时性能佳
#-Xloggc:记录GC日志,这里建议写成绝对路径,如此便可在任意目录下执行该shell脚本
#JAVA_OPTS="-ms512m -mx512m -Xmn256m -Djava.awt.headless=true -XX:MaxPermSize=128m"
JAVA_OPTS="-Duser.timezone=GMT+8 -server -Xms512m -Xmx2048m -Xloggc:${LOG_DIR}/gc.log"




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

shutdown(){
    getPID
    echo "================================================================================================================"
    if [ $TPID -ne 0 ]; then
        echo -n "Stopping $APPLICATION_MAIN(PID=$TPID)..."
        kill $TPID
        sleep 3
        if [ $? -eq 0 ]; then
            echo "[Success]"
            echo "================================================================================================================"
        else
            echo "[Failed]"
            echo "================================================================================================================"
        fi
        getPID
        if [ $TPID -ne 0 ]; then
            shutdown
        fi
    else
        echo "$APPLICATION_MAIN is not running"
        echo "================================================================================================================"
    fi
}

shutdown