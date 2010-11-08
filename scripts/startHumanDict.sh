JAVA="java -XX:ThreadStackSize=256k -XX:+UseCompressedOops -XX:+UseParallelGC"
CP="lib/gnats.jar"
DICT="dictionaries"

#human
nohup ${JAVA} -cp ${CP} -Xmx1500M gnat.server.dictionary.DictionaryServer 56001 ${DICT}/9606/ &

