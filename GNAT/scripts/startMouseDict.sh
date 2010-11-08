JAVA="java -XX:ThreadStackSize=256k -XX:+UseCompressedOops -XX:+UseParallelGC"
CP="lib/gnat.jar.jar"
DICT="dictionaries"

#human
nohup ${JAVA} -cp ${CP} -Xmx2500M gnat.server.dictionary.DictionaryServer 56002 ${DICT}/10090/ &

