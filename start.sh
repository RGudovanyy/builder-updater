#!/bin/sh

CURRENT_DIR=`pwd`

usage()
{
    echo "Скрипт позволяющий запускать сборку прикладного патча и деплой на WebLogic стенд"
    echo ""
    echo "\t-h --help"
    echo "\t Синтаксис:"
	echo "\t ./start.sh -b=[build_name] --deploy=[deploy_name] -c=[file_name_with_configuration]" 
    echo "\t Допустимые параметры:"
    echo "\t -b --build Номер или название билдплана для сборки прикладного патча"
    echo "\t -d --deploy Номер или название билдплана для деплоя на WebLogic стенд"
    echo "\t -s --server Название сервера приложения на WebLogic стенд. По-умолчанию 'WC_Portlet'"
    echo "\t -c --config Название файла с конфигурацией, без расширения. Файл должен находится в той же директории, что и запускающий скрипт"
    echo ""
}

while [ "$1" != "" ]; do
    PARAM=`echo $1 | awk -F= '{print $1}'`
    VALUE=`echo $1 | awk -F= '{print $2}'`
    case $PARAM in
        -h|--help)
            usage
            exit
            ;;
        -b|--build)
            BUILD="build=$VALUE"
            ;;
        -d|--deploy)
            DEPLOY="deploy=$VALUE"
            ;;
        -s|--server)
            SERVER="server=$VALUE"
            ;;
        -c|--config)
	    CONFIG="$CURRENT_DIR/$VALUE.properties"
	    ;;
        *)
            echo "ERROR: unknown parameter \"$PARAM\""
            usage
            exit 1
            ;;
    esac
    shift
done

if [ -z $CONFIG ];
	then echo "Не указан обязательный параметр --config - название файла с конфигурацией"
	exit 1
fi

if [ -f builder-updater*.jar ];
	then java -jar builder-updater*.jar $BUILD $DEPLOY $SERVER $CONFIG
	exit 0
else 
	echo "Не найден файл <lmdywfy>. Он должен находиться в той же директории, что и запускающий скрипт"
	exit 1
fi
